package com.peterfranza.propertytranslator.translators;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JSONDictionaryFileLoader {

	public static JSONDictionary process(File masterDictionary,
			Consumer<String> infoLogConsumer, Consumer<String> errorLogConsumer) throws IOException {
		if (masterDictionary != null && masterDictionary.exists()) {
			infoLogConsumer.accept("Reading dictionary from " + masterDictionary.getAbsolutePath());
			try (Reader reader = new FileReader(masterDictionary)) {
				JSONDictionary d = createGson().fromJson(reader, JSONDictionary.class);
				return d;
			}
		}
		
		return new JSONDictionary();
	}

	public static Gson createGson() {
		return new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation()
				.setPrettyPrinting().create();
	}

}
