package com.peterfranza.propertytranslator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.shared.utils.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.CaseFormat;
import com.peterfranza.propertytranslator.meta.PropertyMetaGenerationRoot;

public class PropertyFileMetaContainer {

	private String packageName;
	private String metaName;

	private boolean isRootProperty = false;
	private boolean isClassCompanion = false;

	private Properties source = new Properties();

	private Optional<PropertyFileMetaContainer> parent = Optional.empty();

	public PropertyFileMetaContainer(File inputRoot, String filename, String metaSuffix, String packageOutputName, String rootPropertyClass, String sourceLanguage)
			throws FileNotFoundException, IOException {
		this.packageName = getPackageNameFor(inputRoot, filename);

		File inputFile = new File(inputRoot, filename);
		
		try (FileInputStream fis = new FileInputStream(inputFile)) {
			source.load(fis);
		}
		
		String baseName = inputFile.getName().toLowerCase().replace(".properties", "").replace("_" + sourceLanguage, "");
		Arrays.asList(inputFile.getParentFile().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().startsWith(baseName) && name.toLowerCase().endsWith(".java");
			}
		})).stream().findFirst().ifPresent(file -> {
			try {
				isClassCompanion = true;
				
				CompilationUnit parsedFile = JavaParser.parse(file);
				
				if(rootPropertyClass != null && parsedFile.getClassByName(rootPropertyClass).isPresent()) {
					isRootProperty = true;
				} else {
					parsedFile.accept(new VoidVisitorAdapter<String>() {
						@Override
						public void visit(NormalAnnotationExpr n, String arg) {
							super.visit(n.getPairs(), arg);

							if(n.getName().toString().equalsIgnoreCase(PropertyMetaGenerationRoot.class.getSimpleName())) {
								isRootProperty = true;
							} 

						}
					}, new String());
				}
				

			} catch (Exception e) {

			}
		});
		
		if(isClassCompanion) {
			this.metaName = getMetaNameFor(inputFile.getName(), metaSuffix, sourceLanguage);
		} else {
			this.metaName = packageOutputName + metaSuffix;
		}
	}

	public void attachParent(Collection<PropertyFileMetaContainer> metaContainers) {
		if (!isRootProperty && isClassCompanion) {
			Optional.ofNullable(metaContainers.stream().filter(r -> {
				return !r.isClassCompanion && r.packageName.equalsIgnoreCase(packageName);
			}).findFirst().orElseGet(() -> {
				return metaContainers.stream().filter(r -> {
					return r.isRootProperty;
				}).findFirst().orElse(null);
			})).ifPresent(r -> {
				parent = Optional.of(r);
			});
		} else if (!isRootProperty && !isClassCompanion) {
			metaContainers.stream().filter(r -> {
				return r.isRootProperty;
			}).findFirst().ifPresent(r -> {
				parent = Optional.of(r);
			});
		}
	}

	public void generate(File outputRoot) throws Exception {

		File outFolder = new File(outputRoot, packageName.replace(".", File.separator));
		outFolder.mkdirs();

		File outFile = new File(outFolder, metaName + ".java");

		FileOutputStream fout = new FileOutputStream(outFile);

		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout))) {

			bw.write("package " + packageName + ";");
			bw.newLine();
			bw.newLine();

			bw.write("@SuppressWarnings(\"all\")");
			bw.newLine();
			bw.write("@javax.annotation.Generated(\"PropertyTranslator\")");
			bw.newLine();

			bw.write("public class " + metaName);

			parent.ifPresent(p -> {
				try {
					bw.write(" extends " + p.getPackageName() + "." + p.getName());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			bw.write(" {");
			bw.newLine();

			source.keySet().stream().map(r -> {
				return r.toString();
			}).sorted().forEach(key -> {
				try {
					bw.write("\tpublic static final String " + getPropertyKeyFor(key) + " = \"" + key + "\";");
					bw.newLine();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			bw.newLine();
			bw.write("}");

			bw.flush();
		}
	}

	public String getName() {
		return metaName;
	}

	public String getPackageName() {
		return packageName;
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((metaName == null) ? 0 : metaName.hashCode());
		result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertyFileMetaContainer other = (PropertyFileMetaContainer) obj;
		if (metaName == null) {
			if (other.metaName != null)
				return false;
		} else if (!metaName.equals(other.metaName))
			return false;
		if (packageName == null) {
			if (other.packageName != null)
				return false;
		} else if (!packageName.equals(other.packageName))
			return false;
		return true;
	}

	private static String getMetaNameFor(String filename, String metaSuffix, String sourceLanguage) {
		return StringUtils.capitalise(
				filename.replace("_" + sourceLanguage, "").replace("-", "").replace(".properties", metaSuffix));
	}

	private static String getPackageNameFor(File inputRoot, String filename) throws FileNotFoundException, IOException {
		File parentFolder = new File(inputRoot, filename).getParentFile();
		Optional<File> file = Arrays.asList(parentFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".java");
			}
		})).stream().findFirst();
		if (file.isPresent()) {
			CompilationUnit unit = JavaParser.parse(file.get());
			if (unit.getPackageDeclaration().isPresent()) {
				return unit.getPackageDeclaration().get().getNameAsString();
			}
		}
		return filename.replace(".properties", "").replace("/", ".");
	}

	public static String getPropertyKeyFor(String key) {
		ArrayList<String> parts = new ArrayList<>();
		for (String part : key.split("\\.")) {
			part = part.replace("-", "_");
			if (isAllUpper(part)) {
				parts.add(part);
			} else {
				parts.add(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, part));
			}
		}

		return StringUtils.join(parts.iterator(), "_").replace("__", "_");
	}

	private static boolean isAllUpper(String s) {
		for (char c : s.toCharArray()) {
			if (Character.isLetter(c) && Character.isLowerCase(c)) {
				return false;
			}
		}
		return true;
	}
	
	

}
