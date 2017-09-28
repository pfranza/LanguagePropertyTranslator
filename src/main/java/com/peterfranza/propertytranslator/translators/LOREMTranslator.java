package com.peterfranza.propertytranslator.translators;

import com.thedeanda.lorem.LoremIpsum;

public class LOREMTranslator implements Translator {

	private LoremIpsum generator = new LoremIpsum(0l);
	
	public String translate(String source) {
		
		if(source == null)
			return null;
		
		source = source.trim();
		
		if(source.length() == 0)
			return "";
		
		String target = generator.getWords(countWords(source));
		if(isCapitalize(source))
			target = capitalize(target);
		
		return target;
	}
	
	private boolean isCapitalize(String source) {
		return Character.isUpperCase(source.charAt(0));
	}

	private String capitalize(final String line) {
		   return Character.toUpperCase(line.charAt(0)) + line.substring(1);
	}

	private static int countWords(String s){

	    int wordCount = 0;

	    boolean word = false;
	    int endOfLine = s.length() - 1;

	    for (int i = 0; i < s.length(); i++) {
	        // if the char is a letter, word = true.
	        if (Character.isLetter(s.charAt(i)) && i != endOfLine) {
	            word = true;
	            // if char isn't a letter and there have been letters before,
	            // counter goes up.
	        } else if (!Character.isLetter(s.charAt(i)) && word) {
	            wordCount++;
	            word = false;
	            // last word of String; if it doesn't end with a non letter, it
	            // wouldn't count without this.
	        } else if (Character.isLetter(s.charAt(i)) && i == endOfLine) {
	            wordCount++;
	        }
	    }
	    return wordCount;
	}
	
}
