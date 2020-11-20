package com.peterfranza.propertytranslator.translators;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gson.GsonBuilder;
import com.peterfranza.propertytranslator.TranslatorConfig;
import com.peterfranza.propertytranslator.translators.JSONDictionaryTranslator.Dictionary;
import com.peterfranza.propertytranslator.translators.JSONDictionaryTranslator.DictionaryLoader;
import com.peterfranza.propertytranslator.translators.JSONDictionaryTranslator.TranslationObject;

public class JSONDictionaryLoader implements DictionaryLoader {

	private String sourceLanguage;
	private Consumer<String> errorLogConsumer;
	public JSONDictionaryLoader(String sourceLanguage, Consumer<String> infoLogConsumer,
			Consumer<String> errorLogConsumer) {
		this.sourceLanguage = sourceLanguage;
		this.infoLogConsumer = infoLogConsumer;
		this.errorLogConsumer = errorLogConsumer;
	}

	private Consumer<String> infoLogConsumer;

	@Override
	public Map<String, TranslationObject> loadDictionary(TranslatorConfig config) throws IOException {
		HashMap<String, TranslationObject> md = new HashMap<>();
		if (config.dictionary != null) {
			JSONDictionaryFileLoader.process(config.dictionary, (obj) -> {
				md.put(obj.calculatedKey, obj);
			}, infoLogConsumer, errorLogConsumer);
		}

		if (config.evolutions != null && config.evolutions.length > 0) {
			JSONDictionaryEvolutionProcessor.process(Arrays.asList(config.evolutions), (key) -> {
				TranslationObject v = md.get(key);
				if (v != null) {
					return Optional.ofNullable(v.sourcePhrase);
				}

				return Optional.empty();
			}, (obj) -> {
				md.put(obj.calculatedKey, obj);
			}, infoLogConsumer, errorLogConsumer);
		}
		return md;
	}

	@Override
	public void saveDictionary(TranslatorConfig config, Map<String, TranslationObject> dictionary) throws IOException {
		if (config.dictionary != null) {
			config.dictionary.getParentFile().mkdirs();
			try (Writer writer = new FileWriter(config.dictionary)) {
				Dictionary d = new Dictionary();
				d.targetLanguage = config.targetLanguage;
				d.sourceLanguage = sourceLanguage;
				d.objects = new ArrayList<>(dictionary.values().stream().filter(JSONDictionaryLoader::isCompleteRecord).collect(Collectors.toList()));
				Collections.sort(d.objects);

				new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
					.setPrettyPrinting().create().toJson(d, writer);
			}
		}
	}

	private static boolean isCompleteRecord(TranslationObject rec) {
		return rec.sourcePhrase != null && rec.targetPhrase != null && rec.sourcePhrase.trim().length() > 0 && rec.targetPhrase.trim().length() > 0 ;
	}
	
}