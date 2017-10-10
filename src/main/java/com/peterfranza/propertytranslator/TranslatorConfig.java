package com.peterfranza.propertytranslator;

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

public class TranslatorConfig {

	@Parameter
	public TranslatorGeneratorType type;
	
	@Parameter
	public String targetLanguage;
	
	@Parameter(required=false)
	public File dictionary;
	
	@Parameter(required=false, defaultValue="JSON")
	public TranslationMasterDictionaryType dictionaryFormat;
	
	@Parameter(required=false, defaultValue="true")
	public boolean omitMissingKeys = true;
	
	@Parameter(required=false, defaultValue="false")
	public MissingKeyBackFillType missingKeyDefault = MissingKeyBackFillType.NONE;
	
}
