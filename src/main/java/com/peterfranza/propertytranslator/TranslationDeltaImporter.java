package com.peterfranza.propertytranslator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.peterfranza.propertytranslator.translators.Translator.TranslationType;

@Mojo(name = "import-language-delta", defaultPhase = LifecyclePhase.NONE)
public class TranslationDeltaImporter extends AbstractMojo {

	public enum OnMissingKey {
		SKIP, HALT
	}

	@Parameter(required = true)
	String sourceLanguage;

	@Parameter(alias = "translator", required = true)
	TranslatorConfig[] translators;

	@Parameter(property = "deltaInputFile", alias = "deltaInputFile", required = true)
	String deltaInputFile;

	@Parameter(property = "deltaTargetLanguage", alias = "deltaTargetLanguage", required = true)
	String deltaTargetLanguage;

	@Parameter(property = "delimiter", alias = "delimiter", required = true, defaultValue = "|")
	String delimiter;

	@Parameter(property = "missingKey", alias = "missingKey", required = true, defaultValue = "HALT")
	OnMissingKey missingKey;

	@Parameter(property = "translationType", alias = "translationType", required = true)
	TranslationType translationType;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			
			List<ImportIntegrityChecker> checkers = Arrays.asList(
					TranslationDeltaImporter::matchesVariableExpressions,
					TranslationDeltaImporter::matchesVariableCounting,
					TranslationDeltaImporter::matchesVariableSpacing);

			Arrays.asList(translators).stream()
					.forEach(PropertyTranslationGenerator.throwingConsumerWrapper(t -> {
						if (t.type == TranslatorGeneratorType.DICTIONARY
								&& t.targetLanguage.equalsIgnoreCase(deltaTargetLanguage)) {
							getLog().info(t.toString());
							t.type.getTranslator().reconfigure(t, sourceLanguage);
							t.type.getTranslator().open();

							System.out.println("Importing " + t.targetLanguage + " from " + deltaInputFile);

							try (Stream<String> stream = Files.lines(Paths.get(deltaInputFile))) {
								stream.forEach((line) -> {

									if (!line.isEmpty()) {
										int idx = line.indexOf(delimiter);

										String key = line.substring(0, idx).trim();
										String value = line.substring(idx + delimiter.length()).trim();

										if (missingKey == OnMissingKey.SKIP && !t.type.getTranslator().hasKey(key))
											return;

										Optional<String> sourcePhrase = t.type.getTranslator().getSourcePhrase(key);
										if (sourcePhrase.isPresent()) {
											AtomicBoolean validFlag = new AtomicBoolean(true);
											checkers.forEach(c -> {
												if(!c.validate(key, sourcePhrase.get(), value, getLog()::error)) {
													validFlag.set(false);
												}
											});
											
											if(!validFlag.get()) {
												return;
											}											
										}

										t.type.getTranslator().setKey(key, value, translationType);
									}

								});
							}

							t.type.getTranslator().printStats(getLog());
							t.type.getTranslator().close();
						}
					}));

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}
	
	/**
	 * 
	 * This check is to confirm that the target string contains all the same variables as the source string
	 * 
	 * @param key
	 * @param sourceString
	 * @param targetString
	 * @param errorLogConsumer
	 * @return
	 */
	private static boolean matchesVariableExpressions(String key, String sourceString, String targetString, Consumer<String> errorLogConsumer) {
		List<String> vars = Utils.extractVaribleExpressions(sourceString);
		if (!Utils.expressionContainsVariableDefinitions(targetString, vars)) {
			errorLogConsumer.accept("Translation Key: " 
					+ key
					+ " does not contain all the variables " + vars
					+ " ... skipping");
			return false;
		}
		return true;
	}
	
	/**
	 * This check is to confirm that during the translation process no spaces were removed surrounding the edges of a variable
	 * @param key
	 * @param sourceString
	 * @param targetString
	 * @param errorLogConsumer
	 * @return
	 */
	private static boolean matchesVariableSpacing(String key, String sourceString, String targetString, Consumer<String> errorLogConsumer) {
		List<String> vars = Utils.extractVaribleExpressions(sourceString);
		boolean flag = true;
		for(String var: vars) {
			if(hasWhitespaceBefore(sourceString, var) != hasWhitespaceBefore(targetString, var)) {
				errorLogConsumer.accept("Pre-token whitespace mismatch for key: " + key + "  " + targetString);
				flag = false;
			}
			
			if(hasWhitespaceAfter(sourceString, var) != hasWhitespaceAfter(targetString, var)) {
				errorLogConsumer.accept("Post-token whitespace mismatch for key: " + key + "  " + targetString);
				flag = false;
			}
		}
		
		return flag;
	}
	
	private static boolean matchesVariableCounting(String key, String sourceString, String targetString, Consumer<String> errorLogConsumer) {
		List<String> vars = Utils.extractVaribleExpressions(sourceString);
		boolean flag = true;
		for(String var: vars) {
			if(getIndexesOf(sourceString, var).size() != getIndexesOf(targetString, var).size()) {
				errorLogConsumer.accept("Variable count mismatch for key: " + key + " for token " + var);
				flag = false;
			}
		}
		
		return flag;
	}
	
	private static List<Integer> getIndexesOf(String sourceString, String token) {
		List<Integer> list = new ArrayList<Integer>();
		
		int index = sourceString.indexOf(token);
		while (index >= 0) {
		    list.add(index);
		    index = sourceString.indexOf(token, index + 1);
		}
		
		return list;
	}
	
	private static boolean hasWhitespaceBefore(String sourceString, String token) {
		for(Integer index: getIndexesOf(sourceString, token)) {
			if(index == 0) {
				return false;
			}
			
			char charBefore = sourceString.charAt(index-1);
			return charBefore == ' ';
		}
		
		return false;
	}
	
	private static boolean hasWhitespaceAfter(String sourceString, String token) {
		for(Integer index: getIndexesOf(sourceString, token)) {
			if(index+token.length() >= sourceString.length()) {
				return false;
			}
			
			char charAfter = sourceString.charAt(index+token.length());
			return charAfter == ' ';
		}
		
		return false;
	}
	
	@FunctionalInterface
	private interface ImportIntegrityChecker {		
		boolean validate(String key, String sourceString, String targetString, Consumer<String> errorLogConsumer);
	}

}
