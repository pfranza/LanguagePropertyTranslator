package com.peterfranza.propertytranslator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.apache.maven.plugin.logging.Log;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class Utils {

	public static String getPackageNameFor(String resource) {
		return resource.substring(0, resource.lastIndexOf("/")).replace("/", ".");
	}

	public static String getPackageNameFor(File inputRoot, String filename, Log logger)
			throws FileNotFoundException, IOException {
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

		logger.warn("Package not found for " + filename
				+ ", if this is a java property create a package-info.java sibling in same directory");
		return filename.replace(".properties", "").replace("/", ".");
	}

}
