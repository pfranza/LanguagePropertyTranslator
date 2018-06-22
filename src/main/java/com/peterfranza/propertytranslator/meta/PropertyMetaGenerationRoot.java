package com.peterfranza.propertytranslator.meta;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target({ TYPE, PACKAGE })
public @interface PropertyMetaGenerationRoot {

	String packagePropertyFileName() default "package.properties";

	String propertyFileName() default "${className}.properties";

	String generatedSuffix() default "PropertyMeta";

	String packageOutputName() default "Package";

}
