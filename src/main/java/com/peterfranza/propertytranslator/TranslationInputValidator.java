package com.peterfranza.propertytranslator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class TranslationInputValidator {

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
