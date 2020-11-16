package com.peterfranza.propertytranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class TranslationPropertyFileReader {

	public static void read(File deltaInputFile, String delimiter, Consumer<Entry<String, String>> entryConsumer)
			throws IOException {
		try (Stream<String> stream = Files.lines(deltaInputFile.toPath())) {
			stream.forEach((line) -> {

				if (!line.isEmpty()) {
					int idx = line.indexOf(delimiter);

					final String key = line.substring(0, idx).trim();
					final String value = line.substring(idx + delimiter.length()).trim();

					entryConsumer.accept(new Entry<String, String>() {

						@Override
						public String setValue(String value) {
							throw new RuntimeException("Not supported");
						}

						@Override
						public String getValue() {
							return value;
						}

						@Override
						public String getKey() {
							return key;
						}
					});
				}

			});
		}
	}

}
