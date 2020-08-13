package com.peterfranza.propertytranslator.translators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import org.apache.maven.plugin.logging.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.peterfranza.propertytranslator.TranslationMasterDictionaryType;
import com.peterfranza.propertytranslator.TranslatorConfig;

public class DictionaryTranslator implements Translator {

	private Map<String, TranslationObject> dictionary = new HashMap<String, TranslationObject>();

	private TranslatorConfig config;

	private String sourceLanguage;

	public void reconfigure(TranslatorConfig config, String sourceLanguage) {
		this.config = config;
		this.sourceLanguage = sourceLanguage;
		dictionary.clear();
	}

	public String translate(String sourcePhrase) throws Exception {

		if (sourcePhrase == null)
			return null;

		if (sourcePhrase.isEmpty())
			return "";

		String key = calculateKey(sourcePhrase);
		TranslationObject target = dictionary.get(key);
		if (target != null) {
			if (target.targetPhrase != null && !target.targetPhrase.trim().isEmpty())
				return target.targetPhrase;
		}

		if (target == null) {
			target = new TranslationObject();
			target.calculatedKey = key;
			target.sourcePhrase = sourcePhrase;
			dictionary.put(key, target);
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

	public void open() throws IOException {
		dictionary.clear();
		dictionary.putAll(getDictionaryLoaderFor(config.dictionaryFormat).loadFile(config.dictionary));
	}

	public void close() throws IOException {
		if (dictionary.size() > 0) {
			getDictionaryLoaderFor(config.dictionaryFormat).saveFile(config.dictionary, dictionary);
			dictionary.clear();
		}
	}

	private static String calculateKey(String sourcePhrase) throws Exception {
		sourcePhrase = sourcePhrase.toLowerCase().trim();
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

	public static class TranslationObject implements Comparable<TranslationObject> {
		String calculatedKey;
		String sourcePhrase;
		String targetPhrase = "";

		@Override
		public int compareTo(TranslationObject o) {
			return calculatedKey.compareTo(o.calculatedKey);
		}
	}

	private DictionaryLoader getDictionaryLoaderFor(TranslationMasterDictionaryType type) {
		switch (type) {

		case PROPERTIES:
			return new PropertiesDictionaryLoader();

		default:
			return new JSONDictionaryLoader();
		}
	}

	@Override
	public void printStats(Log log) {
		long total = dictionary.size();
		long missing = 0;

		for (TranslationObject t : dictionary.values()) {
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

	private interface DictionaryLoader {

		Map<String, TranslationObject> loadFile(File masterDictionary) throws IOException;

		void saveFile(File masterDictionary, Map<String, TranslationObject> dictionary) throws IOException;

	}

	static class Dictionary {
		String sourceLanguage;
		String targetLanguage;
		List<TranslationObject> objects;
	}

	private class JSONDictionaryLoader implements DictionaryLoader {

		public Map<String, TranslationObject> loadFile(File masterDictionary) throws IOException {
			if (masterDictionary.exists()) {
				HashMap<String, TranslationObject> md = new HashMap<>();
				try (Reader reader = new FileReader(masterDictionary)) {
					Dictionary d = new Gson().fromJson(reader, Dictionary.class);
					for (TranslationObject obj : d.objects) {
						md.put(obj.calculatedKey, obj);
					}
				}
				return md;
			}
			return new HashMap<>();
		}

		public void saveFile(File masterDictionary, Map<String, TranslationObject> dictionary) throws IOException {
			masterDictionary.getParentFile().mkdirs();
			try (Writer writer = new FileWriter(masterDictionary)) {
				Dictionary d = new Dictionary();
				d.targetLanguage = config.targetLanguage;
				d.sourceLanguage = sourceLanguage;
				d.objects = new ArrayList<>(dictionary.values());
				Collections.sort(d.objects);

				new GsonBuilder().setPrettyPrinting().create().toJson(d, writer);
			}
		}

	}

	private class PropertiesDictionaryLoader implements DictionaryLoader {

		@Override
		public Map<String, TranslationObject> loadFile(File masterDictionary) throws IOException {
			if (masterDictionary.exists()) {
				HashMap<String, TranslationObject> md = new HashMap<>();
				try (Reader reader = new FileReader(masterDictionary)) {
					Properties p = new Properties();
					p.load(reader);

					for (Entry<Object, Object> es : p.entrySet()) {
						TranslationObject o = new TranslationObject();
						o.calculatedKey = es.getKey().toString();
						o.sourcePhrase = "";
						o.targetPhrase = es.getValue().toString();
						md.put(es.getKey().toString(), o);
					}
				}
				return md;
			}
			return new HashMap<>();
		}

		@Override
		public void saveFile(File masterDictionary, Map<String, TranslationObject> dictionary) throws IOException {
			try (FileOutputStream writer = new FileOutputStream(masterDictionary)) {
				CleanProperties p = new CleanProperties();
				for (TranslationObject o : dictionary.values()) {
					p.setProperty(o.calculatedKey, cascade(o.targetPhrase, o.sourcePhrase, ""));
				}
				p.store(writer, null);
			}
		}

		private String cascade(String... string) {
			for (String s : string) {
				if (s != null && !s.isEmpty())
					return s;
			}
			return "";
		}

	}

	@Override
	public void withMissingKeys(BiConsumer<String, String> consumer) {
		for (TranslationObject t : dictionary.values()) {
			if (t.targetPhrase == null || t.targetPhrase.trim().isEmpty()) {
				consumer.accept(t.calculatedKey, t.sourcePhrase);
			}
		}
	}

	@Override
	public void setKey(String sourceKey, String targetValue) {
		TranslationObject value = dictionary.get(sourceKey);
		if (value == null) {
			throw new RuntimeException("Unknown dictionary key: " + sourceKey);
		}

		value.targetPhrase = targetValue;
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
						super.write((int) c);
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

	private class XLIFF12DictionaryLoader implements DictionaryLoader {

		@Override
		public Map<String, TranslationObject> loadFile(File masterDictionary) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void saveFile(File masterDictionary, Map<String, TranslationObject> dictionary) throws IOException {
			// TODO Auto-generated method stub

		}

	}

}
