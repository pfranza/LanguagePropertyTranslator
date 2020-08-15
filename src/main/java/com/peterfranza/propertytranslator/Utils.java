package com.peterfranza.propertytranslator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

	public static List<String> extractVaribleExpressions(String source) {
		List<String> newList = new ArrayList<>();

		StringBuffer buffer = new StringBuffer();

		boolean insideVariable = false;
		// preserves {0} and ${foo} variables
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '}') {
				insideVariable = false;
				buffer.append(c);
				newList.add(buffer.toString());
				buffer.setLength(0);
			} else if (c == '{') {
				insideVariable = true;
				buffer.append(c);
			} else if (insideVariable) {
				buffer.append(c);
			} else if (Character.isAlphabetic(c) || Character.isIdeographic(c)) {

			} else {

			}
		}

		return newList;
	}

	public static boolean expressionContainsVariableDefinitions(String expression, List<String> variables) {
		for (String var : variables) {
			if (!expression.contains(var)) {
				return false;
			}
		}
		return true;
	}

}
