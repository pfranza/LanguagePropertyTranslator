package com.peterfranza.propertytranslator;

import org.apache.maven.plugins.annotations.Parameter;

public class TranslatorConfig {

	@Parameter
	public TranslatorGeneratorType type;
	
	@Parameter(alias="target_suffix")
	public String targetLanguage;
	
}
