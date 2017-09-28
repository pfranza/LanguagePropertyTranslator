package com.peterfranza.propertytranslator;

import com.peterfranza.propertytranslator.translators.LOREMTranslator;
import com.peterfranza.propertytranslator.translators.Translator;
import com.peterfranza.propertytranslator.translators.XLIFFTranslator;

public enum TranslatorGeneratorType {
	LOREM(new LOREMTranslator()), XLIFF(new XLIFFTranslator());
	
	private Translator translator;

	TranslatorGeneratorType(Translator clazz) {
		this.translator = clazz;
	}
	
	public Translator getTranslator() {
		return translator;
	}
}
