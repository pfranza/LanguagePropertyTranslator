package com.peterfranza.propertytranslator.translators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.maven.shared.model.fileset.util.FileSetManager;

import com.peterfranza.propertytranslator.OnErrorKey;
import com.peterfranza.propertytranslator.OnMissingKey;
import com.peterfranza.propertytranslator.TranslationInputValidator;
import com.peterfranza.propertytranslator.TranslationPropertyFileReader;
import com.peterfranza.propertytranslator.TranslatorDictionaryEvolutionConfiguration;
import com.peterfranza.propertytranslator.translators.JSONDictionaryTranslator.TranslationObject;

public class JSONDictionaryEvolutionProcessor {

	public static void process(List<TranslatorDictionaryEvolutionConfiguration> configs,
			Function<String, Optional<String>> sourcePhraseLookup, Consumer<TranslationObject> consumer,
			Consumer<String> infoLogConsumer, Consumer<String> errorLogConsumer) throws IOException {
		
		File root = new File(config.fileset.getDirectory());
		for(String f: Arrays.asList( new FileSetManager().getIncludedFiles(config.fileset))) {
			File inputFile = new File(root, f);
			infoLogConsumer.accept("Evolving '" + inputFile.getAbsolutePath() + "' " + config);
			TranslationPropertyFileReader.read(inputFile, config.delimiter, (e) -> {
				String key = e.getKey();
				String value = e.getValue();

				TranslationObject obj = new TranslationObject();
				obj.sourcePhrase = sourcePhraseLookup.apply(key).orElse(null);
				obj.calculatedKey = key;
				obj.targetPhrase = value;
				obj.type = config.translationType;

				if (config.missingKey == OnMissingKey.SKIP && obj.sourcePhrase != null) {
					return;
				}

				Optional<String> sourcePhrase = sourcePhraseLookup.apply(key);
				if (sourcePhrase.isPresent()) {
					boolean valid = TranslationInputValidator.checkValidity(key, sourcePhrase.get(), value,
							errorLogConsumer);
					if (!valid && config.errorKey == OnErrorKey.SKIP) {
						errorLogConsumer.accept("Errors found skipping key " + key);
						return;
					}
				}
				consumer.accept(obj);
			});
		}
		
			
		
	}

}
