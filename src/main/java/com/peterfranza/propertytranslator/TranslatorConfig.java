package com.peterfranza.propertytranslator;

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

public class TranslatorConfig {

	@Parameter
	public TranslatorGeneratorType type;

	@Parameter
	public String targetLanguage;

	@Parameter(required = false)
	public File dictionary;

	@Parameter(alias = "evolution", required = false)
	public TranslatorDictionaryEvolutionConfiguration[] evolutions;

	@Parameter(required = false, defaultValue = "true")
	public boolean omitMissingKeys = true;

	@Parameter(required = false, defaultValue = "false")
	public MissingKeyBackFillType missingKeyDefault = MissingKeyBackFillType.NONE;

	@Override
	public String toString() {
		return "TranslatorConfig [type=" + type + ", targetLanguage=" + targetLanguage + ", dictionary=" + dictionary
				+ ", omitMissingKeys=" + omitMissingKeys + ", missingKeyDefault=" + missingKeyDefault + "]";
	}

}
