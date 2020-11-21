package com.peterfranza.propertytranslator.translators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.maven.shared.model.fileset.util.FileSetManager;

import com.peterfranza.propertytranslator.TranslationPropertyFileReader;
import com.peterfranza.propertytranslator.TranslatorDictionaryEvolutionConfiguration;
import com.peterfranza.propertytranslator.Utils;
import com.peterfranza.propertytranslator.translators.JSONDictionary.TranslationEvolutionSource;
import com.peterfranza.propertytranslator.translators.JSONDictionary.TranslationObject;

public class JSONDictionaryEvolutionProcessor {

	private static final class EvolutionFile implements Comparable<EvolutionFile> {
		
		private String filename;
		private TranslatorDictionaryEvolutionConfiguration config;
		
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((filename == null) ? 0 : filename.hashCode());
			return result;
		}



		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EvolutionFile other = (EvolutionFile) obj;
			if (filename == null) {
				if (other.filename != null)
					return false;
			} else if (!filename.equals(other.filename))
				return false;
			return true;
		}



		@Override
		public int compareTo(EvolutionFile o) {
			return filename.compareTo(o.filename);
		}
		
		private File getAsFile() {
			File root = new File(config.fileset.getDirectory());
			return new File(root, filename);
		}
		
	}
	
	public static void process(List<TranslatorDictionaryEvolutionConfiguration> configs,
			Function<String, Optional<String>> sourcePhraseLookup, Consumer<TranslationObject> consumer,
			Function<TranslationEvolutionSource, Boolean> allowProcessSource, 
			Consumer<TranslationEvolutionSource> fileConsumer,
			Consumer<String> infoLogConsumer, Consumer<String> errorLogConsumer) throws IOException {
		
		Set<EvolutionFile> files = new TreeSet<EvolutionFile>();
		
		for(TranslatorDictionaryEvolutionConfiguration c: configs) {
			for(String f: Arrays.asList( new FileSetManager().getIncludedFiles(c.fileset))) {
				EvolutionFile ef = new EvolutionFile();
				ef.config = c;
				ef.filename = f;
				files.add(ef);
			}
		}
		
		
		for(EvolutionFile f: files) {
			try {
				TranslationEvolutionSource src = new TranslationEvolutionSource();
				src.filename = f.filename;
				src.checksum = Utils.checksum(f.getAsFile());
				src.date = new Date();
				
				if(allowProcessSource.apply(src)) {
					infoLogConsumer.accept("Evolving '" + f.getAsFile().getAbsolutePath() + "' " + f.config);
					TranslationPropertyFileReader.read(f.getAsFile(), f.config.delimiter, (e) -> {
						String key = e.getKey();
						String value = e.getValue();

						TranslationObject obj = new TranslationObject();
						obj.sourcePhrase = sourcePhraseLookup.apply(key).orElse(null);
						obj.calculatedKey = key;
						obj.targetPhrase = value;
						obj.type = f.config.translationType;

						consumer.accept(obj);
					});

					fileConsumer.accept(src);
				} else {
					infoLogConsumer.accept("Skipping " + f.getAsFile().getAbsolutePath());
				}
			} catch(Exception e) {
				throw new RuntimeException(e);
			}

		}
			
		
	}

}
