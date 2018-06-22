package com.peterfranza.propertytranslator.meta;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PropertyMetaGenerationProcessor extends AbstractProcessor {

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> annotataions = new LinkedHashSet<String>();
		annotataions.add(PropertyMetaGenerationRoot.class.getCanonicalName());
		annotataions.add(PropertyMetaGeneration.class.getCanonicalName());
		return annotataions;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		String defaultPackageName = "package.properties";
		String defaultPropertyName = "${className}.properties";
		String defaultGeneratedSuffix = "PropertyMeta";
		String defaultPackageOutputName = "Package";

		Optional<? extends Element> root = roundEnv.getElementsAnnotatedWith(PropertyMetaGenerationRoot.class).stream()
				.findFirst();

		Optional<PropertyMetaDelegate> rootPackage = Optional.empty();
		if (root.isPresent()) {
			defaultPackageName = root.get().getAnnotation(PropertyMetaGenerationRoot.class).packagePropertyFileName();
			defaultPropertyName = root.get().getAnnotation(PropertyMetaGenerationRoot.class).propertyFileName();
			defaultGeneratedSuffix = root.get().getAnnotation(PropertyMetaGenerationRoot.class).generatedSuffix();
			defaultPackageOutputName = root.get().getAnnotation(PropertyMetaGenerationRoot.class).packageOutputName();
			rootPackage = processElement(Optional.empty(), defaultPackageName, defaultPropertyName,
					defaultGeneratedSuffix, defaultPackageOutputName, root.get());
		}

		for (TypeElement procAnnotation : annotations) {
			for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(procAnnotation)) {
				processElement(rootPackage, defaultPackageName, defaultPropertyName, defaultGeneratedSuffix,
						defaultPackageOutputName, annotatedElement);
			}
		}

		return false;
	}

	private Optional<PropertyMetaDelegate> processElement(Optional<PropertyMetaDelegate> root,
			String defaultPackageName, String defaultPropertyName, String defaultGeneratedSuffix,
			String defaultPackageOutputName, Element annotatedElement) {

		PropertyMetaDelegate packageMeta = null;

		try {
			FileObject file = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH,
					processingEnv.getElementUtils().getPackageOf(annotatedElement).getQualifiedName(),
					defaultPackageName);

			packageMeta = new PropertyMetaDelegate(
					processingEnv.getElementUtils().getPackageOf(annotatedElement).getQualifiedName().toString(),
					defaultPackageOutputName + defaultGeneratedSuffix, file.openInputStream());

			packageMeta.setParent(root.orElse(null));

			JavaFileObject out = processingEnv.getFiler().createSourceFile(packageMeta.getFQCN(), annotatedElement);
			packageMeta.write(out.openWriter());

		} catch (IOException e) {

		}

		try {

			String propertyName = defaultPropertyName.replace("${className}", annotatedElement.getSimpleName());

			FileObject file = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH,
					processingEnv.getElementUtils().getPackageOf(annotatedElement).getQualifiedName(), propertyName);

			PropertyMetaDelegate meta = new PropertyMetaDelegate(
					processingEnv.getElementUtils().getPackageOf(annotatedElement).getQualifiedName().toString(),
					annotatedElement.getSimpleName().toString() + defaultGeneratedSuffix, file.openInputStream());

			meta.setParent(Optional.ofNullable(packageMeta).orElse(root.orElse(null)));

			JavaFileObject out = processingEnv.getFiler().createSourceFile(meta.getFQCN(), annotatedElement);
			meta.write(out.openWriter());

			return Optional.of(meta);
		} catch (IOException e) {

		}

		return Optional.empty();
	}

	private void error(Element e, String msg, Object... args) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
	}

}
