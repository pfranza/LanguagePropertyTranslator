package com.peterfranza.propertytranslator.translators;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.maven.plugin.logging.Log;

import com.peterfranza.propertytranslator.TranslatorConfig;
import com.thedeanda.lorem.LoremIpsum;

public class LOREMTranslator implements Translator {

	private LoremIpsum generator = new LoremIpsum(0l);
	private String sourceLanguage;

	@Override
	public String translate(String source) {

		if (source == null)
			return null;

		source = source.trim();

		if (source.length() == 0)
			return "";

		String target = generator.getWords(countWords(source));
		if (isCapitalize(source))
			target = capitalize(target);

		return target;
	}

	private boolean isCapitalize(String source) {
		return Character.isUpperCase(source.charAt(0));
	}

	private String capitalize(final String line) {
		return Character.toUpperCase(line.charAt(0)) + line.substring(1);
	}

	private static int countWords(String s) {

		int wordCount = 0;

		boolean word = false;
		int endOfLine = s.length() - 1;

		for (int i = 0; i < s.length(); i++) {
			// if the char is a letter, word = true.
			if (Character.isLetter(s.charAt(i)) && i != endOfLine) {
				word = true;
				// if char isn't a letter and there have been letters before,
				// counter goes up.
			} else if (!Character.isLetter(s.charAt(i)) && word) {
				wordCount++;
				word = false;
				// last word of String; if it doesn't end with a non letter, it
				// wouldn't count without this.
			} else if (Character.isLetter(s.charAt(i)) && i == endOfLine) {
				wordCount++;
			}
		}
		return wordCount;
	}

	@Override
	public void reconfigure(TranslatorConfig config, String sourceLanguage, Consumer<String> infoLogConsumer,
			Consumer<String> errorLogConsumer) {
		this.sourceLanguage = sourceLanguage;
	}

	@Override
	public void open() {
		// NO-OP
	}

	@Override
	public void close() {
		// NO-OP
	}

	@Override
	public void printStats(Log log) {
		// NO-OP
	}

	@Override
	public void withMissingKeys(BiConsumer<String, String> consumer) {
		// NO-OP
	}

	@Override
	public void setKey(String key, String value, TranslationType type) {
		// NO-OP
	}

	@Override
	public Optional<TranslationStatusSummary> getSummary() {
		return Optional.empty();
	}

	@Override
	public boolean hasKey(String key) {
		return true;
	}

	@Override
	public String getSourceLanguage() {
		return this.sourceLanguage;
	}

	@Override
	public Optional<String> getSourcePhrase(String key) {
		return Optional.empty();
	}

}
