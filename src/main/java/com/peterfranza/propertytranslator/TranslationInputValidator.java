package com.peterfranza.propertytranslator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.peterfranza.propertytranslator.translators.TranslationType;

@Mojo(name = "validate-language-delta", defaultPhase = LifecyclePhase.NONE)
public class TranslationInputValidator extends AbstractMojo {

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

			Arrays.asList(translators).stream().forEach(PropertyTranslationGenerator.throwingConsumerWrapper(t -> {
				if (t.type == TranslatorGeneratorType.DICTIONARY
						&& t.targetLanguage.equalsIgnoreCase(deltaTargetLanguage)) {
					getLog().info(t.toString());
					t.type.getTranslator().reconfigure(t, sourceLanguage, getLog()::info, getLog()::error);
					t.type.getTranslator().open();

					getLog().info("Validating " + t.targetLanguage + " from " + deltaInputFile);

					TranslationPropertyFileReader.read(new File(deltaInputFile), delimiter, (e) -> {
						String key = e.getKey();
						String value = e.getValue();


						Optional<String> sourcePhrase = t.type.getTranslator().getSourcePhrase(key);
						if (sourcePhrase.isPresent()) {
							boolean valid = TranslationInputValidator.checkValidity(key, sourcePhrase.get(), value,
									getLog()::error);
							if (!valid) {
								throw new RuntimeException("Error processing key: "+key+" input: " + sourcePhrase.get());
							}
						}

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
	
	private static List<ImportIntegrityChecker> CHECKS = Arrays.asList(
			TranslationInputValidator::matchesVariableExpressions, TranslationInputValidator::matchesVariableCounting,
			TranslationInputValidator::matchesVariableSpacing, TranslationInputValidator::matchesHtmlTags);

	public static boolean checkValidity(String key, String sourcePhrase, String targetPhrase,
			Consumer<String> errorLogConsumer) {
		AtomicBoolean validFlag = new AtomicBoolean(true);
		CHECKS.forEach(c -> {
			if (!c.validate(key, sourcePhrase, targetPhrase, errorLogConsumer)) {
				validFlag.set(false);
			}
		});
		return validFlag.get();
	}

	@FunctionalInterface
	private interface ImportIntegrityChecker {
		boolean validate(String key, String sourceString, String targetString, Consumer<String> errorLogConsumer);
	}

	/**
	 * 
	 * This check is to confirm that the target string contains all the same
	 * variables as the source string
	 * 
	 * @param key
	 * @param sourceString
	 * @param targetString
	 * @param errorLogConsumer
	 * @return
	 */
	private static boolean matchesVariableExpressions(String key, String sourceString, String targetString,
			Consumer<String> errorLogConsumer) {
		List<String> vars = Utils.extractVaribleExpressions(sourceString);
		if (!Utils.expressionContainsVariableDefinitions(targetString, vars)) {
			errorLogConsumer.accept("Translation Key: " + key + " (" + targetString + ") "
					+ " does not contain all the variables " + vars);
			return false;
		}
		return true;
	}

	/**
	 * This check is to confirm that during the translation process no spaces were
	 * removed surrounding the edges of a variable
	 * 
	 * @param key
	 * @param sourceString
	 * @param targetString
	 * @param errorLogConsumer
	 * @return
	 */
	private static boolean matchesVariableSpacing(String key, String sourceString, String targetString,
			Consumer<String> errorLogConsumer) {
		List<String> vars = Utils.extractVaribleExpressions(sourceString);
		boolean flag = true;
		for (String var : vars) {
			if (hasWhitespaceBefore(sourceString.trim(), var) != hasWhitespaceBefore(targetString.trim(), var)) {
				errorLogConsumer.accept("Pre-token whitespace mismatch for key: " + key + "  " + sourceString
						+ "  ===>>  " + targetString);
				flag = false;
			}

			if (hasWhitespaceAfter(sourceString.trim(), var) != hasWhitespaceAfter(targetString.trim(), var)) {
				errorLogConsumer.accept("Post-token whitespace mismatch for key: " + key + "  " + sourceString
						+ "  ===>>  " + targetString);
				flag = false;
			}
		}

		return flag;
	}

	public static boolean matchesHtmlTags(String key, String sourceString, String targetString,
			Consumer<String> errorLogConsumer) {
		if (TranslationDeltaExporter.DetectHtml.isHtml(sourceString)) {
			Document sourceDoc = Jsoup.parse(sourceString);
			Document targetDoc = Jsoup.parse(targetString);

			// Tag Count Match
			if (sourceDoc.getAllElements().size() != targetDoc.getAllElements().size()) {
				errorLogConsumer.accept(
						"Html tag count mismatch for key: " + key + "  " + sourceString + "  ===>>  " + targetString);
				return false;
			}

			for (int i = 0; i < sourceDoc.getAllElements().size(); i++) {
				Element sourceElement = sourceDoc.getAllElements().get(i);
				Element targetElement = targetDoc.getAllElements().get(i);

				for (Attribute sourceAttr : sourceElement.attributes()) {
					String targetAttr = targetElement.attr(sourceAttr.getKey());
					if (!sourceAttr.getValue().equals(targetAttr)) {
						errorLogConsumer.accept(
								"Html tag attribute mismatch (" + sourceAttr.getKey() + "=" + sourceAttr.getValue()
										+ ") for key: " + key + "  " + sourceString + "  ===>>  " + targetString);
						return false;
					}
				}

			}

		}

		return true;
	}

	private static boolean matchesVariableCounting(String key, String sourceString, String targetString,
			Consumer<String> errorLogConsumer) {
		List<String> vars = Utils.extractVaribleExpressions(sourceString);
		boolean flag = true;
		for (String var : vars) {
			if (getIndexesOf(sourceString, var).size() != getIndexesOf(targetString, var).size()) {
				errorLogConsumer.accept("Variable count mismatch for key: " + key + " for token " + var);
				flag = false;
			}
		}

		return flag;
	}

	private static List<Integer> getIndexesOf(String sourceString, String token) {
		List<Integer> list = new ArrayList<Integer>();

		int index = sourceString.indexOf(token);
		while (index >= 0) {
			list.add(index);
			index = sourceString.indexOf(token, index + 1);
		}

		return list;
	}

	private static boolean hasWhitespaceBefore(String sourceString, String token) {
		for (Integer index : getIndexesOf(sourceString, token)) {
			if (index == 0) {
				return false;
			}

			char charBefore = sourceString.charAt(index - 1);
			return Character.isWhitespace(charBefore);
		}

		return false;
	}

	private static boolean hasWhitespaceAfter(String sourceString, String token) {
		for (Integer index : getIndexesOf(sourceString, token)) {
			if (index + token.length() >= sourceString.length()) {
				return false;
			}

			char charAfter = sourceString.charAt(index + token.length());
			return Character.isWhitespace(charAfter);
		}

		return false;
	}

}
