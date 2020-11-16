package com.peterfranza.propertytranslator.translators;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.maven.plugin.logging.Log;

import com.peterfranza.propertytranslator.TranslatorConfig;

public class QuestionMarkTranslator implements Translator {

	private String sourceLanguage;

	@Override
	public String translate(String source) {

		if (source == null)
			return null;

		source = source.trim();

		if (source.length() == 0)
			return "";

		StringBuilder translated = new StringBuilder();
		boolean insideVariable = false;

		// preserves {0} and ${foo} variables

		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '}') {
				translated.append(c);
				insideVariable = false;
			} else if (c == '{') {
				translated.append(c);
				insideVariable = true;
			} else if (insideVariable) {
				translated.append(c);
			} else if (Character.isAlphabetic(c) || Character.isIdeographic(c)) {
				translated.append('?');
			} else {
				translated.append(c);
			}
		}
		return translated.toString();
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
