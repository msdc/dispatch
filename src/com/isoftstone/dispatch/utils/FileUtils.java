package com.isoftstone.dispatch.utils;

public class FileUtils {
	public static String getFolderNameWithoutSequence(String folderName) {
		String strategyStr = folderName.substring(0,
				folderName.lastIndexOf("_"));
		return strategyStr;
	}
}
