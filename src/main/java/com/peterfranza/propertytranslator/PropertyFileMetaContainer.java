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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;

public class PropertyFileMetaContainer {

	private String packageName;
	private String metaName;

	private boolean isWebApplication = false;
	private boolean isClassCompanion = false;

	private Properties source = new Properties();

	private Optional<PropertyFileMetaContainer> parent = Optional.empty();

	public PropertyFileMetaContainer(File inputRoot, String filename, String metaSuffix, String sourceLanguage)
			throws FileNotFoundException, IOException {
		this.packageName = getPackageNameFor(inputRoot, filename);

		File inputFile = new File(inputRoot, filename);
		this.metaName = getMetaNameFor(inputFile.getName(), metaSuffix, sourceLanguage);
		try (FileInputStream fis = new FileInputStream(inputFile)) {
			source.load(fis);
		}

		String baseName = inputFile.getName().toLowerCase().replace(".properties", "").replace("_" + sourceLanguage,
				"");
		Arrays.asList(inputFile.getParentFile().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().startsWith(baseName) && name.toLowerCase().endsWith(".java");
			}
		})).stream().findFirst().ifPresent(file -> {
			try {
				isClassCompanion = true;
				new VoidVisitorAdapter<Object>() {
					@Override
					public void visit(ClassOrInterfaceDeclaration n, Object arg) {
						super.visit(n, arg);
						isWebApplication = n.getExtendedTypes().stream().filter(f -> {
							return f.getName().asString().equalsIgnoreCase("WebApplication");
						}).count() > 0;
					}
				}.visit(JavaParser.parse(file), null);

			} catch (Exception e) {

			}
		});
	}

	public void attachParent(Collection<PropertyFileMetaContainer> metaContainers) {
		if (!isWebApplication && isClassCompanion) {
			Optional.ofNullable(metaContainers.stream().filter(r -> {
				return !r.isClassCompanion && r.packageName.equalsIgnoreCase(packageName);
			}).findFirst().orElseGet(() -> {
				return metaContainers.stream().filter(r -> {
					return r.isWebApplication;
				}).findFirst().orElse(null);
			})).ifPresent(r -> {
				parent = Optional.of(r);
			});
		} else if (!isWebApplication && !isClassCompanion) {
			metaContainers.stream().filter(r -> {
				return r.isWebApplication;
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

	private static String getPropertyKeyFor(String key) {
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
