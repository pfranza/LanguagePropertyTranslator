package com.peterfranza.propertytranslator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "import-language-delta", defaultPhase = LifecyclePhase.NONE)
public class TranslationDeltaImporter extends AbstractMojo {

	@Parameter(required = true)
	String sourceLanguage;

	@Parameter(alias = "translator", required = true)
	TranslatorConfig[] translators;

	@Parameter(property = "deltaInputFile", alias = "deltaInputFile", required = true)
	String deltaInputFile;

	@Parameter(property = "deltaTargetLanguage", alias = "deltaTargetLanguage", required = true)
	String deltaTargetLanguage;

	@Parameter(property = "delimiter", alias = "delimiter", required = true, defaultValue = "|")
	String delimiter;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			Arrays.asList(translators).parallelStream()
					.forEach(PropertyTranslationGenerator.throwingConsumerWrapper(t -> {
						if (t.type == TranslatorGeneratorType.DICTIONARY
								&& t.targetLanguage.equalsIgnoreCase(deltaTargetLanguage)) {

						
							t.type.getTranslator().reconfigure(t, sourceLanguage);
							t.type.getTranslator().open();
							
							System.out.println("Importing " + t.targetLanguage + " from " + deltaInputFile);


							try (Stream<String> stream = Files.lines(Paths.get(deltaInputFile))) {
								stream.forEach((line) -> {

									if (!line.isEmpty()) {
										int idx = line.indexOf(delimiter);

										String key = line.substring(0, idx);
										String value = line.substring(idx + delimiter.length());

										t.type.getTranslator().setKey(key, value);
									}

								});
							}

							t.type.getTranslator().printStats(getLog());
							t.type.getTranslator().close();
						}
					}));

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

}
