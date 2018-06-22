package com.peterfranza.propertytranslator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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

@Mojo(name = "generate-languages", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class PropertyTranslationGenerator extends AbstractMojo {

	@Parameter(required = true)
	private FileSet fileset;

	@Parameter(alias = "translator", required = true)
	TranslatorConfig[] translators;

	@Parameter(required = true)
	String sourceLanguage;

	@Parameter(defaultValue = "PropertyMeta", required = false)
	String metaSuffix = "PropertyMeta";
	
	@Parameter(required=false, defaultValue="false")
	public boolean generateMeta = false;

	private FileSetManager fileSetManager = new FileSetManager();

	@Parameter(defaultValue = "${project.build.directory}/generated-sources/annotations", required = true)
	public File targetFolder;
	
	@Parameter(defaultValue = "${project.build.sourceDirectory}", required = true)
	public File sourceFolder;

	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			File root = new File(fileset.getDirectory());

			for (TranslatorConfig t : translators) {
				t.type.getTranslator().reconfigure(t, sourceLanguage);
				t.type.getTranslator().open();

				for (String f : fileSetManager.getIncludedFiles(fileset)) {
					getLog().info("Processing: " + f + " for " + t.targetLanguage);
					generateLanguagePropertyFiles(root, getOutputRoot(root), t, f);
				}
				t.type.getTranslator().printStats(getLog());
				t.type.getTranslator().close();
			}

			if(generateMeta) {
				Collection<PropertyFileMetaContainer> metaContainers = new ArrayList<>();
				for (String f : fileSetManager.getIncludedFiles(fileset)) {
					if(isFileInSourceDirectory(new File(root, f))) {
						metaContainers.add(new PropertyFileMetaContainer(root, f, metaSuffix, sourceLanguage));
					}
				}

				metaContainers.stream().forEach(r -> {
					r.attachParent(metaContainers);
				});

				getOutputRoot(root).mkdirs();
				metaContainers.stream().forEach(r -> {
					getLog().info("Generating Property Meta For " + r.getName());
					try {
						r.generate(getOutputRoot(root));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("Error processing translations", e);
		}
	}

	private void generateLanguagePropertyFiles(File inputRoot, File outputRoot, TranslatorConfig config,
			String filename) throws FileNotFoundException, IOException, Exception {
		outputRoot.mkdirs();
		
		File inputFile = new File(inputRoot, filename);
		FileInputStream fis = new FileInputStream(inputFile);

		Properties source = new Properties();
		source.load(fis);

		Properties target = new CleanProperties();

		File outFile = getOutputFile(inputFile, inputRoot, outputRoot, config, filename);
		outFile.getParentFile().mkdirs();
		FileOutputStream fout = new FileOutputStream(outFile);
		for (Entry<Object, Object> entry : source.entrySet()) {
			String v = config.type.getTranslator().translate(entry.getValue().toString());
			if (v != null) {
				target.setProperty(entry.getKey().toString(), v);
			} else {
				getLog().debug("Skipping key: " + entry.getKey().toString());
			}
		}

		target.store(fout, null);
		fout.close();
		fis.close();
	}

	private File getOutputFile(File inputFile, File inputRoot, File outputRoot, TranslatorConfig config, String filename) {
		if(isFileInSourceDirectory(inputFile)) {			
			Path first = Paths.get(sourceFolder.toURI()); 
			Path second = Paths.get(inputFile.getParentFile().toURI());
			return new File(outputRoot, first.relativize(second).toString() + File.separator + replaceLast(inputFile.getName(), sourceLanguage, config.targetLanguage));
		} else {
			return new File(inputRoot, replaceLast(filename, sourceLanguage, config.targetLanguage));
		}
	}
	
	private boolean isFileInSourceDirectory(File inputFile) {
		return Paths.get(inputFile.toURI()).startsWith(Paths.get(sourceFolder.toURI()));
	}

	private File getOutputRoot(File mainRoot) {

		if (targetFolder != null)
			return targetFolder;

		return mainRoot;
	}

	private static String replaceLast(String string, String substring, String replacement) {
		int index = string.lastIndexOf(substring);
		if (index == -1)
			return string;

		return string.substring(0, index) + replacement + string.substring(index + substring.length());
	}

}
