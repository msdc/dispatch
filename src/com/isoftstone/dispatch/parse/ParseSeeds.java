package com.isoftstone.dispatch.parse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.isoftstone.dispatch.consts.DispatchConstant;
import com.isoftstone.dispatch.vo.StrategyVo;

public class ParseSeeds {

	/**
	 * 获取所有文件名列表.
	 * 
	 * @return 文件名列表.
	 */
	public static List<String> getFolderNameList(String rootFolder) {
		List<String> folderNameList = new ArrayList<String>();
		File file = new File(rootFolder);
		if (!file.isDirectory()) {
			return folderNameList;
		}
		File[] fileList = file.listFiles();
		if (fileList == null || fileList.length == 0) {
			return folderNameList;
		}
		for (int i = 0; i < fileList.length; i++) {
			if (fileList[i].isDirectory()) {
				folderNameList.add(fileList[i].getName());
			}
		}
		return folderNameList;
	}

	/**
	 * 解析文件夹的处理策略.
	 * 
	 * @return 文件夹处理策略，key是文件夹名称，value是策略实例.
	 */
	public static Map<String, StrategyVo> parseStrategy(Map<String, List<String>> folderNameListMap) {
		Map<String, StrategyVo> resultMap = new HashMap<String, StrategyVo>();
		for (Iterator<Entry<String, List<String>>> it = folderNameListMap.entrySet().iterator(); it.hasNext();) {
			Entry<String, List<String>> entry = it.next();
			String folderName = entry.getKey();
			String[] parts = folderName.split(DispatchConstant.FOLDER_SEPARATE);
			String strategyStr = parts[1];
			Pattern p = Pattern.compile("^\\d{1,2}");
			Matcher m = p.matcher(strategyStr);
			String time = "";
			String timeUnit = "";
			if (m.find()) {
				time = m.group();
			}
			p = Pattern.compile("[a-z]+$");
			m = p.matcher(strategyStr);
			if (m.find()) {
				timeUnit = m.group();
			}
			if (StringUtils.isNotBlank(timeUnit)) {
				StrategyVo strategy = new StrategyVo();
				int value = Integer.valueOf(time);
				switch (timeUnit) {
				case "hour":
					strategy.setHour(value);
					break;
				case "day":
					strategy.setDay(value);
					break;
				case "week":
					strategy.setWeek(value);
					break;

				}
				resultMap.put(folderName, strategy);
			}

		}
		return resultMap;
	}
}
