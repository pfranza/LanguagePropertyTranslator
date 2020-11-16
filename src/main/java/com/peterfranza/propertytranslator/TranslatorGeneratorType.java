package com.peterfranza.propertytranslator;

import com.peterfranza.propertytranslator.translators.JSONDictionaryTranslator;
import com.peterfranza.propertytranslator.translators.LOREMTranslator;
import com.peterfranza.propertytranslator.translators.QuestionMarkTranslator;
import com.peterfranza.propertytranslator.translators.Translator;

public enum TranslatorGeneratorType {
	LOREM(new LOREMTranslator()), DICTIONARY(new JSONDictionaryTranslator()),
	QUESTIONMARK(new QuestionMarkTranslator());

	private Translator translator;

	TranslatorGeneratorType(Translator clazz) {
		this.translator = clazz;
	}

	public Translator getTranslator() {
		return translator;
	}
}
