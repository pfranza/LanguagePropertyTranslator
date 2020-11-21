package com.peterfranza.propertytranslator.translators;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.google.gson.annotations.Expose;

public class JSONDictionary {
	@Expose
	String sourceLanguage;
	
	@Expose
	String targetLanguage;

	@Expose
	Set<TranslationEvolutionSource> sources = new TreeSet<>();
	
	@Expose
	Set<TranslationObject> objects = new TreeSet<>();
	

	public TranslationObject get(String key) {
		return objects.stream().filter(o->o.calculatedKey.equals(key)).findFirst().orElse(null);
	}

	public void add(TranslationObject target) {
		objects.add(target);
	}
	
	public boolean containsSource(TranslationEvolutionSource src) {
		Optional<TranslationEvolutionSource> f = sources.stream().filter( o -> o.filename.equals(src.filename)).findFirst();
		if(f.isPresent()) {
			if(!f.get().checksum.equals(src.checksum)) {
				throw new RuntimeException("File match found but checksum has changed - " + f.get().filename);
			}
		}
		return f.isPresent();
	}
	
	public static class TranslationEvolutionSource implements Comparable<TranslationEvolutionSource> {

		@Expose
		String filename;
		
		@Expose
		Date date;
		
		@Expose
		String checksum;
		
		@Override
		public int compareTo(TranslationEvolutionSource o) {
			return date.compareTo(o.date);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((checksum == null) ? 0 : checksum.hashCode());
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
			TranslationEvolutionSource other = (TranslationEvolutionSource) obj;
			if (checksum == null) {
				if (other.checksum != null)
					return false;
			} else if (!checksum.equals(other.checksum))
				return false;
			return true;
		}
		
		
		
	}
	
	public static class TranslationObject implements Comparable<TranslationObject> {
		@Expose
		String calculatedKey;
		
		@Expose
		String sourcePhrase;
		
		@Expose
		String targetPhrase = "";
		
		@Expose
		TranslationType type = TranslationType.MACHINE;
		
		int timesUsed = 0;

		@Override
		public int compareTo(TranslationObject o) {
			return calculatedKey.compareTo(o.calculatedKey);
		}
	}

	
}