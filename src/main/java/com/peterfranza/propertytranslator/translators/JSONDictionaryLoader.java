package com.peterfranza.propertytranslator.translators;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.peterfranza.propertytranslator.PropertyTranslationGenerator;
import com.peterfranza.propertytranslator.TranslatorConfig;
import com.peterfranza.propertytranslator.translators.JSONDictionary.TranslationObject;
import com.peterfranza.propertytranslator.translators.JSONDictionaryTranslator.DictionaryLoader;

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
	public JSONDictionary loadDictionary(TranslatorConfig config) throws IOException {
		JSONDictionary d = new JSONDictionary();
		
		if (config.dictionary != null) {
			d = JSONDictionaryFileLoader.process(config.dictionary, infoLogConsumer, errorLogConsumer);
		}

		loadEvolutions(config, d);
		
		d.sourceLanguage = sourceLanguage;
		d.targetLanguage = config.targetLanguage;
		return d;
	}

	private void loadEvolutions(TranslatorConfig config, JSONDictionary d) throws IOException {
		if (config.evolutions != null && config.evolutions.length > 0) {
			JSONDictionaryEvolutionProcessor.process(Arrays.asList(config.evolutions), (key) -> {
				TranslationObject v = d.get(key);
				if (v != null) {
					return Optional.ofNullable(v.sourcePhrase);
				}

				return Optional.empty();
			}, (obj) -> {
				d.add(obj);
			}, (e) -> {return !d.containsSource(e);},
			d.sources::add, infoLogConsumer, errorLogConsumer);
		}
	}

	@Override
	public void saveDictionary(TranslatorConfig config, JSONDictionary dictionary) throws IOException {
		if (config.dictionary != null) {
			config.dictionary.getParentFile().mkdirs();		 
			try (Writer writer = new OutputStreamWriter(new FileOutputStream(config.dictionary), PropertyTranslationGenerator.UTF8)) {
				JSONDictionary d = new JSONDictionary();
				d.targetLanguage = config.targetLanguage;
				d.sourceLanguage = sourceLanguage;
				d.objects = new TreeSet<>(dictionary.objects.stream().filter(JSONDictionaryLoader::isCompleteRecord).collect(Collectors.toList()));
				d.sources = dictionary.sources;

				JSONDictionaryFileLoader.createGson().toJson(d,writer);
			}
		}
	}

	private static boolean isCompleteRecord(TranslationObject rec) {
		//return rec.sourcePhrase != null && rec.sourcePhrase.trim().length() > 0 && ((rec.targetPhrase != null && rec.targetPhrase.trim().length() > 0) || rec.timesUsed > 0)  ;
		return true;
	}
	
}