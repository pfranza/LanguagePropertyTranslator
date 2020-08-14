package com.peterfranza.propertytranslator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.peterfranza.propertytranslator.translators.DictionaryTranslator.CleanProperties;

@Mojo(name = "generate-languages", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class PropertyTranslationGenerator extends AbstractMojo {

	@Parameter(required = true)
	private FileSet fileset;

	@Parameter(alias = "translator", required = true)
	TranslatorConfig[] translators;

	@Parameter(required = true)
	String sourceLanguage;

	@Parameter(required = false, defaultValue = ".*\\_en.properties")
	String dependencyMatcher;

	private FileSetManager fileSetManager = new FileSetManager();

	@Parameter(defaultValue = "${project.build.directory}/generated-sources/properties", required = true)
	public File targetFolder;

	@Parameter(defaultValue = "${project.build.sourceDirectory}", required = true)
	public File sourceFolder;

	@Parameter(defaultValue = "${project.build.directory}/generated-sources/properties/translation_summary.properties", required = true)
	public File targetSummaryFile;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// This is a bag of work items
		List<TranslatableWorkSourceInput> translatableItems = new ArrayList<TranslatableWorkSourceInput>();

		try {
			File root = new File(fileset.getDirectory());

			Reflections reflections = new Reflections(new ResourcesScanner());
			Set<String> properties = reflections.getResources(Pattern.compile(dependencyMatcher));

			// Process classpath items
			properties.stream().forEach(str -> {

				try {
					Properties packageProperties = new Properties();
					packageProperties.load(PropertyTranslationGenerator.class.getResourceAsStream("/" + str));

					TranslatableWorkSourceInput input = new TranslatableWorkSourceInput(
							TranslatableWorkSourceInputType.DEPENDENCY, Utils.getPackageNameFor(str), str,
							packageProperties, null);

					translatableItems.add(input);

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			List<String> includedFiles = Arrays.asList(fileSetManager.getIncludedFiles(fileset));
			includedFiles.parallelStream().forEach(throwingConsumerWrapper(f -> {

				File inputFile = new File(root, f);
				try (FileInputStream fis = new FileInputStream(inputFile)) {
					Properties sourceProperties = new Properties();
					sourceProperties.load(fis);

					TranslatableWorkSourceInput input = new TranslatableWorkSourceInput(
							TranslatableWorkSourceInputType.JAVA_PACKAGE_SOURCE,
							Utils.getPackageNameFor(root, f, getLog()), f, sourceProperties, inputFile);

					translatableItems.add(input);
				}
			}));

			List<TranslatableWorkItem> workItems = merge(translatableItems);

			List<TranslationStatusSummary> summary = new ArrayList<>();

			Arrays.asList(translators).parallelStream().forEach(throwingConsumerWrapper(t -> {
				t.type.getTranslator().reconfigure(t, sourceLanguage);
				t.type.getTranslator().open();

				workItems.parallelStream().forEach(throwingConsumerWrapper(workItem -> {
					getLog().info("Processing: " + workItem.getName() + " for " + t.targetLanguage);
					generateLanguagePropertyFiles(root, getOutputRoot(root), t, workItem);
				}));

				t.type.getTranslator().getSummary().ifPresent(summary::add);
				t.type.getTranslator().printStats(getLog());
				t.type.getTranslator().close();
			}));

			if (targetSummaryFile != null) {
				try (Writer w = Files.newWriter(targetSummaryFile, Charset.defaultCharset())) {
					w.write(new Gson().toJson(summary));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("Error processing translations", e);
		}

	}

	private void generateLanguagePropertyFiles(File inputRoot, File outputRoot, TranslatorConfig config,
			TranslatableWorkItem workItem) throws FileNotFoundException, IOException, Exception {
		outputRoot.mkdirs();
		File outFile = workItem.getOutputFile(outputRoot, config);
		outFile.getParentFile().mkdirs();

		try (FileOutputStream fout = new FileOutputStream(outFile)) {
			generateLanguagePropertyFiles(config, workItem.getProperties(), fout);
		}
	}

	private void generateLanguagePropertyFiles(TranslatorConfig config, Properties source, OutputStream output)
			throws Exception {
		Properties target = new CleanProperties();
		for (Entry<Object, Object> entry : source.entrySet()) {
			String v = config.type.getTranslator().translate(entry.getValue().toString());
			if (v != null) {
				target.setProperty(entry.getKey().toString(), v);
			} else {
				getLog().debug("Skipping key: " + entry.getKey().toString());
			}
		}

		target.store(output, null);
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

	public static <T> Consumer<T> throwingConsumerWrapper(ThrowingConsumer<T, Exception> throwingConsumer) {

		return i -> {
			try {
				throwingConsumer.accept(i);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		};
	}

	@FunctionalInterface
	public interface ThrowingConsumer<T, E extends Exception> {
		void accept(T t) throws E;
	}

	private enum TranslatableWorkSourceInputType {
		DEPENDENCY, JAVA_PACKAGE_SOURCE, RAW_FILE
	}

	private List<TranslatableWorkItem> merge(List<TranslatableWorkSourceInput> input) {
		ArrayList<TranslatableWorkItem> work = new ArrayList<PropertyTranslationGenerator.TranslatableWorkItem>();

		Consumer<TranslatableWorkSourceInput> itemProcessor = (item) -> {
			Optional<TranslatableWorkItem> existingWorkItem = work.parallelStream().filter(w -> w.isFor(item))
					.findFirst();

			if (existingWorkItem.isPresent()) {
				existingWorkItem.get().add(item);
			} else {
				work.add(new TranslatableWorkItem().add(item));
			}
		};

		input.stream().filter(s -> s.type == TranslatableWorkSourceInputType.DEPENDENCY).forEach(itemProcessor);
		input.stream().filter(s -> s.type != TranslatableWorkSourceInputType.DEPENDENCY).forEach(itemProcessor);

		return work;
	}

	private static class TranslatableWorkSourceInput {
		private String sourcePath;
		private String packagePath;

		private Properties input;
		private File inputFile;
		private TranslatableWorkSourceInputType type;

		public TranslatableWorkSourceInput(TranslatableWorkSourceInputType type, String packagePath, String sourcePath,
				Properties input, File inputFile) {
			super();
			this.type = type;
			this.sourcePath = sourcePath;
			this.input = input;
			this.packagePath = packagePath;
			this.inputFile = inputFile;
		}

		public String getUnitName() {
			if (sourcePath.lastIndexOf("/") != -1) {
				return sourcePath.substring(sourcePath.lastIndexOf("/"));
			}

			return sourcePath;
		}

		@Override
		public String toString() {
			return "TranslatableWorkSourceInput [type=" + type + ", packagePath=" + packagePath + ", sourcePath="
					+ sourcePath
					// + ", input=" + input
					+ "]";
		}

	}

	private class TranslatableWorkItem {

		private List<TranslatableWorkSourceInput> sources = new ArrayList<TranslatableWorkSourceInput>();

		public TranslatableWorkItem add(TranslatableWorkSourceInput item) {
			this.sources.add(item);
			return this;
		}

		public String getName() {

			Optional<TranslatableWorkSourceInput> src = sources.stream()
					.filter(s -> s.type != TranslatableWorkSourceInputType.DEPENDENCY).findFirst();
			if (src.isPresent()) {
				return src.get().sourcePath;
			}

			src = sources.stream().filter(s -> s.type == TranslatableWorkSourceInputType.DEPENDENCY).findFirst();
			if (src.isPresent()) {
				return src.get().sourcePath;
			}

			return "Unknown";
		}

		public Properties getProperties() {
			Properties target = new CleanProperties();

			sources.stream().filter(s -> s.type == TranslatableWorkSourceInputType.DEPENDENCY).forEach(s -> {
				target.putAll(s.input);
			});

			sources.stream().filter(s -> s.type != TranslatableWorkSourceInputType.DEPENDENCY).forEach(s -> {
				target.putAll(s.input);
			});

			return target;
		}

		public File getOutputFile(File outputRoot, TranslatorConfig config) {

			for (TranslatableWorkSourceInput source : sources) {
				if (source.inputFile != null) {
					if (isFileInSourceDirectory(source.inputFile)) {
						Path first = Paths.get(sourceFolder.toURI());
						Path second = Paths.get(source.inputFile.getParentFile().toURI());
						return new File(outputRoot, first.relativize(second).toString() + File.separator
								+ replaceLast(source.inputFile.getName(), sourceLanguage, config.targetLanguage));
					} else {
						return new File(new File(fileset.getDirectory()),
								replaceLast(source.sourcePath, sourceLanguage, config.targetLanguage));
					}
				}
			}

			for (TranslatableWorkSourceInput source : sources) {
				return new File(targetFolder, replaceLast(source.sourcePath, sourceLanguage, config.targetLanguage));
			}

			throw new RuntimeException("Unable to determine output file");
		}

		public boolean isFor(TranslatableWorkSourceInput item) {

			if (!Strings.isNullOrEmpty(item.packagePath) && sources.size() > 0) {
				for (TranslatableWorkSourceInput source : sources) {
					if (source.packagePath.equalsIgnoreCase(item.packagePath)) {
						// They are in same package .. but are they the same source
						return source.getUnitName().equalsIgnoreCase(item.getUnitName());
					}
				}
			}

			return false;
		}

	}

}
