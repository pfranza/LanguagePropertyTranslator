package com.peterfranza.propertytranslator.translators;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.maven.plugin.logging.Log;

import com.peterfranza.propertytranslator.TranslatorConfig;

public interface Translator {

	public enum TranslationType {MACHINE, SERVICE}
	
	
	String translate(String string) throws Exception;

	void reconfigure(TranslatorConfig config, String sourceLanguage);

	void open() throws Exception;

	void close() throws Exception;

	void printStats(Log log);

	Optional<TranslationStatusSummary> getSummary();

	void withMissingKeys(BiConsumer<String, String> consumer);

	void setKey(String key, String value, TranslationType type);

	boolean hasKey(String key);

}
