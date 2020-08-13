package com.peterfranza.propertytranslator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "export-language-delta", defaultPhase = LifecyclePhase.NONE)
public class TranslationDeltaExporter extends AbstractMojo {

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

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			PrintStream writetoOuput = new PrintStream(new FileOutputStream(deltaOutputFile, true));

			Arrays.asList(translators).parallelStream()
					.forEach(PropertyTranslationGenerator.throwingConsumerWrapper(t -> {
						if (t.type == TranslatorGeneratorType.DICTIONARY
								&& t.targetLanguage.equalsIgnoreCase(deltaTargetLanguage)) {

							t.type.getTranslator().printStats(getLog());
							System.out.println("Exporting " + t.targetLanguage + " to " + deltaOutputFile);

							t.type.getTranslator().reconfigure(t, sourceLanguage);
							t.type.getTranslator().open();

							t.type.getTranslator().withMissingKeys((key, phrase) -> {
								writetoOuput.println(key + delimiter + phrase);
							});

							t.type.getTranslator().close();
						}
					}));

			writetoOuput.close();

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

	}

}
