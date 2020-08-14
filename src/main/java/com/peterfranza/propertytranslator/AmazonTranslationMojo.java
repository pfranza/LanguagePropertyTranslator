package com.peterfranza.propertytranslator;

import java.io.IOException;
import java.util.Optional;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.amazonaws.services.translate.AmazonTranslateAsyncClientBuilder;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;

@Mojo(name = "aws-translation-missing-keys", defaultPhase = LifecyclePhase.NONE)
public class AmazonTranslationMojo extends AbstractMachineTranslationMojo {

	@Override
	public Optional<String> translate(TranslatorConfig config, String sourcePhrase) throws IOException {
		
		TranslateTextRequest translateTextRequest = new TranslateTextRequest()
	               .withText(sourcePhrase)
	               .withSourceLanguageCode(config.type.getTranslator().getSourceLanguage())
	               .withTargetLanguageCode(deltaTargetLanguage);
		
		TranslateTextResult response = AmazonTranslateAsyncClientBuilder.standard()
				.build().translateText(translateTextRequest);
		
		return Optional.ofNullable(response.getTranslatedText());
	}

}
