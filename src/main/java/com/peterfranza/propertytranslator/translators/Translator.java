package com.peterfranza.propertytranslator.translators;

import java.io.IOException;

import org.apache.maven.plugin.logging.Log;

import com.peterfranza.propertytranslator.TranslatorConfig;

public interface Translator {

	String translate(String string) throws Exception;

	void reconfigure(TranslatorConfig config, String sourceLanguage);

	void open() throws Exception;

	void close() throws Exception;

	void printStats(Log log);
	
}
