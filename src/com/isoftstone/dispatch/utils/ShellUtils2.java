package com.isoftstone.dispatch.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ShellUtils2 {
	private static final Log LOG = LogFactory.getLog(ShellUtils2.class);

	public boolean runShell(String command) throws IOException {
		Process process = Runtime.getRuntime().exec(command);
		LOG.info("开始执行shell==" + command);
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = new String();
		while ((line = br.readLine()) != null) {
			LOG.info(line);
		}
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			LOG.error("processes was interrupted", e);
			return false;
		}
		br.close();
		int ret = process.exitValue();
		LOG.info(ret);
		return true;
	}
}
