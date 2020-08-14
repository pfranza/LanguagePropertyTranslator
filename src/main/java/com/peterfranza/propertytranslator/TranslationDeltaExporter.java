package com.peterfranza.propertytranslator;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "export-language-delta", defaultPhase = LifecyclePhase.NONE)
public class TranslationDeltaExporter extends AbstractMojo {

	public enum OutputMode {
		TRUNCATE, APPEND
	}

	public enum HTMLDetection {
		SKIP, PROCESS
	}

	@Parameter(required = true)
	String sourceLanguage;

	@Parameter(alias = "translator", required = true)
	TranslatorConfig[] translators;

	@Parameter(property = "deltaOutputFile", alias = "deltaOutputFile", required = true)
	String deltaOutputFile;

	@Parameter(property = "deltaTargetLanguage", alias = "deltaTargetLanguage", required = true)
	String deltaTargetLanguage;

	@Parameter(property = "delimiter", alias = "delimiter", required = true, defaultValue = "|")
	String delimiter;

	@Parameter(property = "outputMode", alias = "outputMode", required = true, defaultValue = "TRUNCATE")
	OutputMode outputMode;

	@Parameter(property = "htmlDetection", alias = "htmlDetection", required = true, defaultValue = "PROCESS")
	HTMLDetection htmlDetection;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			PrintStream writetoOuput = new PrintStream(
					new FileOutputStream(deltaOutputFile, outputMode == OutputMode.APPEND));

			Arrays.asList(translators).parallelStream()
					.forEach(PropertyTranslationGenerator.throwingConsumerWrapper(t -> {
						if (t.type == TranslatorGeneratorType.DICTIONARY
								&& t.targetLanguage.equalsIgnoreCase(deltaTargetLanguage)) {
							t.type.getTranslator().reconfigure(t, sourceLanguage);
							t.type.getTranslator().open();

							t.type.getTranslator().printStats(getLog());
							System.out.println("Exporting " + t.targetLanguage + " to " + deltaOutputFile);

							t.type.getTranslator().withMissingKeys((key, phrase) -> {
								boolean isHtml = DetectHtml.isHtml(phrase);

								if (isHtml && htmlDetection == HTMLDetection.SKIP)
									return;

								writetoOuput.println(key + delimiter + phrase);
							});

							t.type.getTranslator().close();
						}
					}));

			writetoOuput.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	public static class DetectHtml {
		public final static String tagStart = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>";
		public final static String tagEnd = "\\</\\w+\\>";
		public final static String tagSelfClosing = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>";
		public final static String htmlEntity = "&[a-zA-Z][a-zA-Z0-9]+;";
		public final static Pattern htmlPattern = Pattern.compile(
				"(" + tagStart + ".*" + tagEnd + ")|(" + tagSelfClosing + ")|(" + htmlEntity + ")", Pattern.DOTALL);

		/**
		 * Will return true if s contains HTML markup tags or entities.
		 *
		 * @param s String to test
		 * @return true if string contains HTML
		 */
		public static boolean isHtml(String s) {
			boolean ret = false;
			if (s != null) {
				ret = htmlPattern.matcher(s).find();
			}
			return ret;
		}

	}

}
