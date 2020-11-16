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

	@Parameter(property = "missingKey", alias = "missingKey", required = true, defaultValue = "HALT")
	public OnMissingKey missingKey = OnMissingKey.HALT;

	@Parameter(property = "errorKey", alias = "errorKey", required = true, defaultValue = "SKIP")
	public OnErrorKey errorKey = OnErrorKey.SKIP;

	@Override
	public String toString() {
		return "TranslatorDictionaryEvolutionConfiguration [translationType=" + translationType + ", delimiter="
				+ delimiter + ", missingKey=" + missingKey + ", errorKey=" + errorKey + "]";
	}

	
	
}
