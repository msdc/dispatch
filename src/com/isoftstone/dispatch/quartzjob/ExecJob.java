package com.isoftstone.dispatch.quartzjob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.isoftstone.dispatch.consts.DispatchConstant;
import com.isoftstone.dispatch.crawlerpool.CrawlerMachinePool;
import com.isoftstone.dispatch.utils.Config;
import com.isoftstone.dispatch.utils.HdfsCommon;
import com.isoftstone.dispatch.utils.RedisUtils;
import com.isoftstone.dispatch.utils.ShellUtils;
import com.isoftstone.dispatch.vo.DispatchVo;
import com.isoftstone.dispatch.vo.Runmanager;
import com.isoftstone.dispatch.vo.Seed;

public class ExecJob implements Runnable {
	public static final JobPool jobPool = JobPool.getInstance();

	private static final CrawlerMachinePool crawlerMachinePool = CrawlerMachinePool.getInstance();
	
	private static final Map<String, Integer> domainCrawlerCount = new HashMap<String, Integer>(); 
	
	private static final Log LOG = LogFactory.getLog(ExecJob.class);
	
	private static final Integer crawlcount = 2;

	@Override
	public void run() {
//		while (true) {
			LOG.info("队列中的剩余任务数目：" + jobPool.getJobSize());
			Runmanager runmanager = jobPool.getRunmanager();
			String dispatchFolderName = runmanager.getDispatchFolderName();
			boolean isIncrement = DispatchConstant.INCREMENT_TRUE.equals(Config.getValue(DispatchConstant.KEY_INCREMENT));
			DispatchVo dispatchVo = RedisUtils.getDispatchResult(dispatchFolderName, DispatchConstant.DISPATCH_REDIS_DBINDEX);
			
			//--准备拷贝目录相关内容.
			String folderNameSeed = dispatchFolderName.substring(0, dispatchFolderName.lastIndexOf("_"));
			String folderNameData = folderNameSeed.substring(0, folderNameSeed.lastIndexOf("_"));
			if (isIncrement) {
				folderNameData = folderNameData.substring(0, folderNameData.lastIndexOf("_")) + "_" + DispatchConstant.INCREMENT_FILENAME_SIGN;
			}
			String folderData = runmanager.getCrawlerData();
			String desFolderDataName = Config.getValue("reParseFolderData");
			String command = "scp -r " + folderData + " " + desFolderDataName;
			if(isIncrement) {
				dispatchVo.setStatus(DispatchConstant.DISPATCH_STATIS_RUNNING);
			} else {
				List<Seed> seedList = dispatchVo.getSeed();
				List<String> seeds = new ArrayList<String>();
				synchronized (dispatchVo) {
					for(Iterator<Seed> it = seedList.iterator(); it.hasNext();) {
						Seed seed = it.next();
						seed.setStatus(DispatchConstant.DISPATCH_STATIS_RUNNING);
						seeds.add(seed.getUrl());
					}
				}
				contentToTxt(folderNameSeed, seeds, "false");
			}
			RedisUtils.setResult(dispatchVo, dispatchFolderName, DispatchConstant.DISPATCH_REDIS_DBINDEX);

			try {
				LOG.info("开始执行：" + Thread.currentThread().getName() + runmanager.getCommand());
				new ShellUtils().execCmd(runmanager);
				LOG.info("执行完成：" + Thread.currentThread().getName() + runmanager.getCommand());
				LOG.info("拷贝目录到目标机器.");
				runmanager.setCommand(command);
				new ShellUtils().execCmd(runmanager);
				LOG.info("拷贝目录到目标机器完成." + command);
				command = "rm -rf " + folderData;
				runmanager.setCommand(command);
				new ShellUtils().execCmd(runmanager);
				LOG.info("删除data目录" + command);
				
				LOG.info("备份data目录到hdfs");
				Runmanager r = new Runmanager();
				r.setHostIp(Config.getValue("hostip"));
				r.setPort(22);
				r.setUsername(Config.getValue("username"));
				r.setPassword(Config.getValue("password"));
				r.setCommand("java -jar /hdfsCommon.jar " + folderData);
				new ShellUtils().execCmd(r);
				LOG.info("备份data目录到hdfs完成");
				synchronized (domainCrawlerCount) {
				    String domain = folderNameSeed.split("_")[0];
				    Integer count = domainCrawlerCount.get(domain);
				    if(count == null) {
				        count = 0;
				    }
				    if (count + 1 == crawlcount) {
				         String nutch_root = "/nutch_run/local_incremental/bin/nutch";
				         String output_folder = " /home/nutch_final_data/";
				         String data_folder = "/nutch_data/";
				         String mergeCommand = "java -jar /merge_nutch_data.jar " + nutch_root + " " + output_folder + " " + data_folder + " " + domain;
				         r.setCommand(mergeCommand);
				         LOG.info("merge指令：" + mergeCommand);
				         new ShellUtils().execCmd(r);
				         LOG.info("merge指令执行完毕：" + mergeCommand);
				         domainCrawlerCount.put(domain, 0);
				    } else {
				        domainCrawlerCount.put(domain, count + 1);
				        LOG.info("爬取次数：" + count + 1);
				    }
                }
			} catch (Exception e) {
				LOG.error("执行任务中异常：" + runmanager.getCommand(), e);
			}

			crawlerMachinePool.addCrawlerMachine(runmanager.getCrawlerMachineBean());
			if(isIncrement) {
				dispatchVo.setStatus(DispatchConstant.DISPATCH_STATIS_COMPLETE);
			} else {
				List<Seed> seedList = dispatchVo.getSeed();
				synchronized (dispatchVo) {
					for(Iterator<Seed> it = seedList.iterator(); it.hasNext();) {
						Seed seed = it.next();
						seed.setStatus(DispatchConstant.DISPATCH_STATIS_COMPLETE);
					}
				}
			}
			RedisUtils.setResult(dispatchVo, dispatchFolderName, DispatchConstant.DISPATCH_REDIS_DBINDEX);
//		}

	}
	
	/**
	 * 
	 * 保存内容到文件
	 * */
	private void contentToTxt(String folderName, List<String> seeds, String status) {
		String folderRoot = Config.getValue(DispatchConstant.KEY_LOCAL_ROOT_FOLDER);
		String filePath = folderRoot + File.separator + folderName + File.separator + DispatchConstant.SEED_FILE_NAME;
		String str = null; // 原有txt内容
		StringBuffer strBuf = new StringBuffer();// 内容更新
		BufferedReader input = null;
		BufferedWriter output = null;
		try {
			File f = new File(filePath);
			if (!f.exists()) {
				return;
			} else {
				input = new BufferedReader(new FileReader(f));
				List<String> fileSeedList = new ArrayList<String>();
				while ((str = input.readLine()) != null) {
					fileSeedList.add(str);
				}
				input.close();

				// --写入未包含到本次种子中的历史数据.
				for (int i = 0; i < fileSeedList.size(); i++) {
					String tempStr = fileSeedList.get(i);
					String temp = tempStr;
					if (tempStr.startsWith("#")) {
						temp = tempStr.substring(1, tempStr.length());
					}
					if (!seeds.contains(temp)) {
						strBuf.append(tempStr + System.getProperty("line.separator"));
					}
				}

			}
			for (Iterator<String> it = seeds.iterator(); it.hasNext();) {
				String seedStr = it.next();
				if ("false".equals(status)) {
					strBuf.append("#");
				}
				strBuf.append(seedStr + System.getProperty("line.separator"));
			}
			output = new BufferedWriter(new FileWriter(f));
			output.write(strBuf.toString());
			output.close();
			String isCopy = Config.getValue(DispatchConstant.KEY_IS_COPYFOLDER);
			if ("true".equals(isCopy)) {
				putSeedsFolder(folderName, "local");
			}
			HdfsCommon.upFileToHdfs(filePath);
		} catch (Exception e) {
			LOG.error("生成文件错误.", e);
		} finally {
			try {
				if (input != null) {
					input.close();
				}
				if (output != null) {
					output.close();
				}
			} catch (IOException e) {
				LOG.error("关闭流异常.", e);
			}
		}
	}
	
	public void putSeedsFolder(String folderName, String type) {
		String hostIp = Config.getValue("seedHostip");
		String userName = Config.getValue("seedHostUserName");
		String password = Config.getValue("seedHostPassword");
		Runmanager runmanager = new Runmanager();
		runmanager.setHostIp(hostIp);
		runmanager.setUsername(userName);
		runmanager.setPassword(password);
		runmanager.setPort(22);
		String folderRoot = Config.getValue(DispatchConstant.KEY_LOCAL_ROOT_FOLDER);
		LOG.info("文件根目录" + folderRoot);

		String desCopyRootFolders = Config.getValue("desFolderNameIPs");
		String[] desCopyRootFolderStr = desCopyRootFolders.split(";");

		String command = "";
		if ("local".equals(type)) {
			for (int i = 0; i < desCopyRootFolderStr.length; i++) {
				String desCopyRootFolder = desCopyRootFolderStr[i];
				command = "scp -r " + folderRoot + File.separator + folderName + " " + desCopyRootFolder;
				LOG.info("命令：" + command);
				runmanager.setCommand(command);
				new ShellUtils().execCmd(runmanager);
			}
		}
	}
}
