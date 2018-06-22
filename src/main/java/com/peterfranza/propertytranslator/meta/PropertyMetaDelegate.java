package com.peterfranza.propertytranslator.meta;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Optional;
import java.util.Properties;

import com.peterfranza.propertytranslator.PropertyFileMetaContainer;

public class PropertyMetaDelegate {

	private Optional<PropertyMetaDelegate> parent = Optional.empty();

	private Properties properties;

	private String packageName;
	private String className;

	public PropertyMetaDelegate(String packageName, String className, InputStream input) throws IOException {

		this.packageName = packageName;
		this.className = className;

		this.properties = new Properties();
		this.properties.load(input);
	}

	public PropertyMetaDelegate setParent(PropertyMetaDelegate parent) {
		this.parent = Optional.ofNullable(parent);
		return this;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getClassName() {
		return className;
	}

	public String getFQCN() {
		return getPackageName() + "." + getClassName();
	}

	public void write(Writer writer) throws IOException {
		BufferedWriter bw = new BufferedWriter(writer);
		bw.write("package " + getPackageName() + ";");
		bw.newLine();
		bw.newLine();

		bw.write("@SuppressWarnings(\"all\")");
		bw.newLine();
		bw.write("@javax.annotation.Generated(\"PropertyTranslator\")");
		bw.newLine();

		bw.write("public class " + getClassName());

		parent.ifPresent(p -> {
			try {
				bw.write(" extends " + p.getFQCN());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		bw.write(" {");
		bw.newLine();

		properties.keySet().stream().map(r -> {
			return r.toString();
		}).sorted().forEach(key -> {
			try {
				bw.write("\tpublic static final String " + PropertyFileMetaContainer.getPropertyKeyFor(key) + " = \""
						+ key + "\";");
				bw.newLine();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		bw.newLine();
		bw.write("}");

		bw.flush();
		bw.close();
	}

}
