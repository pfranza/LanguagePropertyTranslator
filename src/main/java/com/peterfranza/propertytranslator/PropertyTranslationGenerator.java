package com.peterfranza.propertytranslator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import com.peterfranza.propertytranslator.translators.DictionaryTranslator.CleanProperties;

@Mojo(name = "generate-languages",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class PropertyTranslationGenerator extends AbstractMojo {

	@Parameter(required=true)
    private FileSet fileset;

	@Parameter(alias="translator", required=true)
	TranslatorConfig[] translators; 
	
	@Parameter(required=true)
	String sourceLanguage;
	
	private FileSetManager fileSetManager = new FileSetManager();
	
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			File root = new File(fileset.getDirectory());

			for(TranslatorConfig t: translators) {
				t.type.getTranslator().reconfigure(t, sourceLanguage);
				t.type.getTranslator().open();

				for(String f: fileSetManager.getIncludedFiles(fileset)) {
					FileInputStream fis = new FileInputStream(new File(root, f));

					getLog().info("Processing: " + f + " for " + t.targetLanguage);
					Properties source = new Properties();
					source.load(fis);

					Properties target = new CleanProperties();
					
					FileOutputStream fout = new FileOutputStream(new File(root, replaceLast(f, sourceLanguage, t.targetLanguage)));
					for(Entry<Object, Object> entry: source.entrySet()) {
						String v = t.type.getTranslator().translate(entry.getValue().toString());
						if(v != null) {
							target.setProperty(entry.getKey().toString(), v);
						} else {
							getLog().debug("Skipping key: " + entry.getKey().toString());
						}
					}

					target.store(fout, null);
					fout.close();
					fis.close();

				}
				t.type.getTranslator().printStats(getLog());
				t.type.getTranslator().close();
			}

		} catch(Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("Error processing translations", e);
		}
	}
	
	private static String replaceLast(String string, String substring, String replacement) {
		int index = string.lastIndexOf(substring);
		if(index == -1)
			return string;
		
		return string.substring(0, index) + replacement + string.substring(index+substring.length());
	}

}
