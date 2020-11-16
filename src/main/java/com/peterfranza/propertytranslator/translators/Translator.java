package com.peterfranza.propertytranslator.translators;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.maven.plugin.logging.Log;

import com.peterfranza.propertytranslator.TranslatorConfig;

public interface Translator {

	String getSourceLanguage();

	String translate(String string) throws Exception;

	void reconfigure(TranslatorConfig config, String sourceLanguage, Consumer<String> infoLogConsumer,
			Consumer<String> errorLogConsumer);

	void open() throws Exception;

	void close() throws Exception;

	void printStats(Log log);

	Optional<TranslationStatusSummary> getSummary();

	Optional<String> getSourcePhrase(String key);

	void withMissingKeys(BiConsumer<String, String> consumer);

	void setKey(String key, String value, TranslationType type);

	boolean hasKey(String key);

}
