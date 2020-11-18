package com.peterfranza.propertytranslator;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;

import com.peterfranza.propertytranslator.translators.TranslationType;

public class TranslatorDictionaryEvolutionConfiguration {

	@Parameter(property = "translationType", alias = "translationType", required = true)
	public TranslationType translationType;

	@Parameter(required = true)
	public FileSet fileset;

	@Parameter(property = "delimiter", alias = "delimiter", required = true, defaultValue = "|")
	public String delimiter = "|";

	@Override
	public String toString() {
		return "TranslatorDictionaryEvolutionConfiguration [translationType=" + translationType + ", delimiter="
				+ delimiter + "]";
	}

	
	
}
