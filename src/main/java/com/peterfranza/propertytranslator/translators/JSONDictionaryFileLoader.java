package com.peterfranza.propertytranslator.translators;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.peterfranza.propertytranslator.translators.JSONDictionaryTranslator.Dictionary;
import com.peterfranza.propertytranslator.translators.JSONDictionaryTranslator.TranslationObject;

public class JSONDictionaryFileLoader {

	public static void process(File masterDictionary, Consumer<TranslationObject> consumer,
			Consumer<String> infoLogConsumer, Consumer<String> errorLogConsumer) throws IOException {
		if (masterDictionary != null && masterDictionary.exists()) {
			infoLogConsumer.accept("Reading dictionary from " + masterDictionary.getAbsolutePath());
			try (Reader reader = new FileReader(masterDictionary)) {
				Dictionary d = new Gson().fromJson(reader, Dictionary.class);
				for (TranslationObject obj : d.objects) {
					consumer.accept(obj);
				}
			}
		}
	}

}
