# LanguagePropertyTranslator

The LanguagePropertyTranslator takes a fileset of property files that represent the source language, and a set of translators. and generates a sister set of language property files.

Also it can create a LoremIpsum translation for generic typesetting when the a second target language has not been defined.

Maven Configuration

```xml
	<plugin>
                <groupId>com.peterfranza</groupId>
		<artifactId>PropertyTranslator</artifactId>
		<version>1.3.9</version>
		<executions>
                    <execution>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>generate-languages</goal>
                        </goals>
                    </execution>
                </executions>
        	<configuration>
        		<sourceLanguage>en</sourceLanguage>
        		<fileset>
        			<directory>${basedir}/src/main/java</directory>
        			<includes>
              			<include>**/*_en.properties</include>
            		</includes>
        		</fileset>
        		<translators>
        			<translator>
        				<targetLanguage>zz</targetLanguage>
        				<type>LOREM</type>
        			</translator>
        		</translators>
        	</configuration>
        </plugin>
```

You can create a | delimited file to ease translation by running 

```
mvn -DdeltaOutputFile=project_es.diff.csv -DdeltaTargetLanguage=es  PropertyTranslator:export-language-delta
```

Then once that file is translated you can reimport it into the dictionary using 

```
 mvn -DdeltaInputFile=project_es.diff.translated.csv -DdeltaTargetLanguage=es  PropertyTranslator:import-language-delta
```
