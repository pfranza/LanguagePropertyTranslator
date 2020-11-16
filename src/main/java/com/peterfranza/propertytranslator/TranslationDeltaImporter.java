package com.peterfranza.propertytranslator;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.peterfranza.propertytranslator.translators.TranslationType;

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

	@Parameter(property = "missingKey", alias = "missingKey", required = true, defaultValue = "HALT")
	OnMissingKey missingKey;

	@Parameter(property = "errorKey", alias = "errorKey", required = true, defaultValue = "SKIP")
	OnErrorKey errorKey;

	@Parameter(property = "translationType", alias = "translationType", required = true)
	TranslationType translationType;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {

			Arrays.asList(translators).stream().forEach(PropertyTranslationGenerator.throwingConsumerWrapper(t -> {
				if (t.type == TranslatorGeneratorType.DICTIONARY
						&& t.targetLanguage.equalsIgnoreCase(deltaTargetLanguage)) {
					getLog().info(t.toString());
					t.type.getTranslator().reconfigure(t, sourceLanguage, getLog()::info, getLog()::error);
					t.type.getTranslator().open();

					getLog().info("Importing " + t.targetLanguage + " from " + deltaInputFile);

					TranslationPropertyFileReader.read(new File(deltaInputFile), delimiter, (e) -> {
						String key = e.getKey();
						String value = e.getValue();

						if (missingKey == OnMissingKey.SKIP && !t.type.getTranslator().hasKey(key))
							return;

						Optional<String> sourcePhrase = t.type.getTranslator().getSourcePhrase(key);
						if (sourcePhrase.isPresent()) {
							boolean valid = TranslationInputValidator.checkValidity(key, sourcePhrase.get(), value,
									getLog()::error);
							if (!valid && errorKey == OnErrorKey.SKIP) {
								return;
							}
						}

						t.type.getTranslator().setKey(key, value, translationType);
					});

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
