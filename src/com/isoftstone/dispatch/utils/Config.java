package com.isoftstone.dispatch.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Config {
	public Config() {
	}

	private static Properties props = new Properties();
	static {
		String jarFile = Config.class.getProtectionDomain().getCodeSource()
				.getLocation().getFile();
		File f = new File(jarFile);
		String configPath = f.getParent() + File.separator + "config.properties";
		try {
//			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
			props.load(new FileInputStream(new File(configPath)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getValue(String key) {
		return props.getProperty(key);
	}

	public static void updateProperties(String key, String value) {
		props.setProperty(key, value);
	}
}
