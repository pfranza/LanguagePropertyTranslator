package com.peterfranza.propertytranslator;

public class TranslationStatusSummary {
	private String targetLanguage;
	private long totalKeys = 0;
	private long missingKeys = 0;

	public String getTargetLanguage() {
		return targetLanguage;
	}

	public void setTargetLanguage(String targetLanguage) {
		this.targetLanguage = targetLanguage;
	}

	public long getTotalKeys() {
		return totalKeys;
	}

	public void setTotalKeys(long totalKeys) {
		this.totalKeys = totalKeys;
	}

	public long getMissingKeys() {
		return missingKeys;
	}

	public void setMissingKeys(long missingKeys) {
		this.missingKeys = missingKeys;
	}

}
