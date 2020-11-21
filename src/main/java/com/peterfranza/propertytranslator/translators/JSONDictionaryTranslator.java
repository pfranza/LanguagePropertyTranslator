package com.peterfranza.propertytranslator.translators;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.maven.plugin.logging.Log;

import com.peterfranza.propertytranslator.TranslatorConfig;
import com.peterfranza.propertytranslator.translators.JSONDictionary.TranslationObject;

public class JSONDictionaryTranslator implements Translator {

	private Consumer<String> errorLogConsumer;
	private Consumer<String> infoLogConsumer;
	private JSONDictionary dictionary = new JSONDictionary();

	private TranslatorConfig config;

	private String sourceLanguage;

	@Override
	public void reconfigure(TranslatorConfig config, String sourceLanguage, Consumer<String> infoLogConsumer,
			Consumer<String> errorLogConsumer) {
		this.config = config;
		this.sourceLanguage = sourceLanguage;
		this.infoLogConsumer = infoLogConsumer;
		this.errorLogConsumer = errorLogConsumer;
		dictionary = new JSONDictionary();
	}

	@Override
	public String translate(String sourcePhrase) throws Exception {

		if (sourcePhrase == null)
			return null;

		if (sourcePhrase.isEmpty())
			return "";

		String key = calculateKey(sourcePhrase);
		
		TranslationObject target = dictionary.get(key);
		if (target != null) {
			if(target.sourcePhrase == null || target.sourcePhrase.trim().isEmpty())
				target.sourcePhrase = sourcePhrase;

			if (target.targetPhrase != null && !target.targetPhrase.trim().isEmpty())
				target.timesUsed++;
				return target.targetPhrase;
		}

		if (target == null) {
			target = new TranslationObject();
			target.calculatedKey = key;
			target.sourcePhrase = sourcePhrase;
			dictionary.add(target);
		}

		if (config.omitMissingKeys)
			return null;
		else
			return fallback(sourcePhrase);
	}

	private String fallback(String sourcePhrase) {
		switch (config.missingKeyDefault) {

		case PRIMARYLANGUAGE:
			return sourcePhrase;

		case QUESTIONMARK:
			return generateQuestionMark(sourcePhrase);

		case NONE:
		default:
			return "";
		}
	}

	private String generateQuestionMark(String sourcePhrase) {
		if (sourcePhrase == null)
			return null;

		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < sourcePhrase.length(); i++) {
			buffer.append("?");
		}
		return buffer.toString();
	}

	@Override
	public void open() throws IOException {
		dictionary = getDictionaryLoaderFor().loadDictionary(config);
	}

	@Override
	public void close() throws IOException {
		if (dictionary.objects.size() > 0) {
			getDictionaryLoaderFor().saveDictionary(config, dictionary);
		}

		dictionary = null;
	}

	@Override
	public String getSourceLanguage() {
		return sourceLanguage;
	}

	private static String calculateKey(String sourcePhrase) throws Exception {
		sourcePhrase = sourcePhrase.toLowerCase().trim();
		MessageDigest crypt = MessageDigest.getInstance("SHA-1");
		crypt.reset();
		crypt.update(sourcePhrase.getBytes());
		return byteToHex(crypt.digest());
	}
	
	private static String calculateCaseKey(String sourcePhrase) throws Exception {
		sourcePhrase = sourcePhrase.trim();
		MessageDigest crypt = MessageDigest.getInstance("SHA-1");
		crypt.reset();
		crypt.update(sourcePhrase.getBytes());
		return byteToHex(crypt.digest());
	}

	private static String byteToHex(byte[] digest) {
		Formatter formatter = new Formatter();
		for (byte b : digest) {
			formatter.format("%02x", b);
		}
		String result = formatter.toString();
		formatter.close();
		return result;
	}

	

	private DictionaryLoader getDictionaryLoaderFor() {
		return new JSONDictionaryLoader(sourceLanguage, infoLogConsumer, errorLogConsumer);
	}

	@Override
	public void printStats(Log log) {

		if (log == null)
			return;

		long total = dictionary.objects.size();
		long missing = 0;

		for (TranslationObject t : dictionary.objects) {
			if (t.targetPhrase == null || t.targetPhrase.trim().isEmpty())
				missing += 1;
		}

		if (missing > 0) {
			log.error("Dictionary " + config.targetLanguage + " only has " + (total - missing) + " of " + total
					+ " phrases (Missing " + missing + ")");
		} else {
			log.info("Dictionary " + config.targetLanguage + " is complete");
		}
	}

	@Override
	public Optional<TranslationStatusSummary> getSummary() {

		long missing = 0;
		long machine = 0;

		for (TranslationObject t : dictionary.objects) {
			if (t.targetPhrase == null || t.targetPhrase.trim().isEmpty())
				missing += 1;

			if (t.type == TranslationType.MACHINE)
				machine += 1;
		}

		TranslationStatusSummary s = new TranslationStatusSummary();
		s.setTargetLanguage(config.targetLanguage);
		s.setTotalKeys(dictionary.objects.size());
		s.setMissingKeys(missing);
		s.setMachineKeys(machine);

		return Optional.of(s);
	}

	interface DictionaryLoader {

		JSONDictionary loadDictionary(TranslatorConfig masterDictionary) throws IOException;

		void saveDictionary(TranslatorConfig masterDictionary, JSONDictionary dictionary)
				throws IOException;

	}
	
	@Override
	public void withMissingKeys(BiConsumer<String, String> consumer) {
		for (TranslationObject t : dictionary.objects) {
			if (t.targetPhrase == null || t.targetPhrase.trim().isEmpty()) {
				consumer.accept(t.calculatedKey, t.sourcePhrase);
			}
		}
	}

	@Override
	public boolean hasKey(String sourceKey) {
		return dictionary.get(sourceKey) != null;
	}

	@Override
	public void setKey(String sourceKey, String targetValue, TranslationType type) {
		TranslationObject value = dictionary.get(sourceKey);
		if (value == null) {
			throw new RuntimeException("Unknown dictionary key: " + sourceKey);
		}

		value.targetPhrase = targetValue;
		value.type = type;
	}

	@Override
	public Optional<String> getSourcePhrase(String sourceKey) {
		TranslationObject value = dictionary.get(sourceKey);
		if (value == null) {
			return Optional.empty();
		}

		return Optional.of(value.sourcePhrase);
	}

	public static class CleanProperties extends Properties {
		private static class StripCommentsStream extends FilterOutputStream {

			StringBuffer buffer = new StringBuffer();
			private char eol;

			public StripCommentsStream(final OutputStream out) {
				super(out);
				eol = System.getProperty("line.separator").charAt(0);
			}

			@Override
			public void write(final int b) throws IOException {

				buffer.append((char) b);

				if (b == eol) {
					flush();
				}

			}

			@Override
			public void flush() throws IOException {
				super.flush();

				if (!buffer.toString().trim().startsWith("#")) {
					for (char c : buffer.toString().toCharArray()) {
						super.write(c);
					}
				}

				buffer.setLength(0);
			}

		}

		private static final long serialVersionUID = 7567765340218227372L;

		@Override
		public synchronized Enumeration<Object> keys() {
			return Collections.enumeration(new TreeSet<>(super.keySet()));
		}

		@Override
		public void store(final OutputStream out, final String comments) throws IOException {
			super.store(new StripCommentsStream(out), null);
		}
	}

}
