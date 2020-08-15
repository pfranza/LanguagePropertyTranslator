package com.peterfranza.propertytranslator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.peterfranza.propertytranslator.TranslationDeltaExporter.DetectHtml;
import com.peterfranza.propertytranslator.translators.Translator;

public abstract class AbstractMachineTranslationMojo extends AbstractMojo {

	@Parameter(required = true)
	String sourceLanguage;

	@Parameter(property = "deltaTargetLanguage", alias = "deltaTargetLanguage", required = true)
	String deltaTargetLanguage;

	@Parameter(alias = "translator", required = true)
	TranslatorConfig[] translators;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Arrays.asList(translators).parallelStream().forEach(PropertyTranslationGenerator.throwingConsumerWrapper(t -> {
			if (t.type == TranslatorGeneratorType.DICTIONARY
					&& t.targetLanguage.equalsIgnoreCase(deltaTargetLanguage)) {
				t.type.getTranslator().reconfigure(t, sourceLanguage);
				t.type.getTranslator().open();
				t.type.getTranslator().withMissingKeys((key, phrase) -> {

					if (!DetectHtml.isHtml(phrase)) {
						try {
							Optional<String> p = translate(t, phrase);
							if (p.isPresent()) {

								Optional<String> sourcePhrase = t.type.getTranslator().getSourcePhrase(key);
								if (sourcePhrase.isPresent()) {
									List<String> vars = Utils.extractVaribleExpressions(sourcePhrase.get());
									if (!Utils.expressionContainsVariableDefinitions(p.get(), vars)) {
										getLog().error("Translation Key: " + key
												+ " does not contain all the variables " + vars + " ... skipping");
										return;
									} else {
										t.type.getTranslator().setKey(key, p.get(), Translator.TranslationType.MACHINE);
									}
								}

							}
						} catch (IOException e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}

				});

				t.type.getTranslator().close();
			}
		}));
	}

	public abstract Optional<String> translate(TranslatorConfig config, String sourcePhrase) throws IOException;

}
