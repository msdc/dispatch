package com.isoftstone.dispatch.quartzjob;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.isoftstone.dispatch.consts.DispatchConstant;
import com.isoftstone.dispatch.crawlerpool.CrawlerMachineBean;
import com.isoftstone.dispatch.crawlerpool.CrawlerMachinePool;
import com.isoftstone.dispatch.utils.Config;
import com.isoftstone.dispatch.utils.RedisUtils;
import com.isoftstone.dispatch.utils.ShellUtils;
import com.isoftstone.dispatch.vo.DispatchVo;
import com.isoftstone.dispatch.vo.Runmanager;

public class NutchJob implements Job {

	private static final Log LOG = LogFactory.getLog(NutchJob.class);

	private static final JobPool jobPool = JobPool.getInstance();

	private static final CrawlerMachinePool crawlerMachinePool = CrawlerMachinePool.getInstance();

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String rootFolder = Config.getValue(DispatchConstant.KEY_LOCAL_ROOT_FOLDER);
		String folderWithoutSequence = (String) context.getJobDetail().getJobDataMap().get(DispatchConstant.KEY_FOLDER_WITHOUT_SEQUENCE);
		LOG.info("开始调度：" + folderWithoutSequence);
		String redisKey;
		boolean isIncrement = DispatchConstant.INCREMENT_TRUE.equals(Config.getValue(DispatchConstant.KEY_INCREMENT));
		if(isIncrement) {
			redisKey = folderWithoutSequence + "*" + "_dispatch(increment)";
		} else {
			redisKey = folderWithoutSequence + "*" + "_dispatch";			
		}
		List<String> judgeFolderNameList = RedisUtils.getResultList(redisKey, DispatchConstant.DISPATCH_REDIS_DBINDEX);
		// --如果是增量，则需要判断，当前任务是否增在运行.
		if (isIncrement) {
			for (Iterator<String> it = judgeFolderNameList.iterator(); it.hasNext();) {
				String key = it.next();
				DispatchVo dispatchVo = RedisUtils.getDispatchResult(key, DispatchConstant.DISPATCH_REDIS_DBINDEX);
				if (DispatchConstant.DISPATCH_STATIS_RUNNING.equals(dispatchVo.getStatus())) {
					return;
				}
			}
		}

		// --上次调度已经完成，开始本次调度.
		Runmanager runmanager = new Runmanager();
		String shDir = Config.getValue("shDir");
		String crawlDir = Config.getValue("crawlDir");
		String solr = Config.getValue("solrURL");
		runmanager.setUsername(Config.getValue("username"));
		runmanager.setPassword(Config.getValue("password"));
		// -- 多机器，本地模式，从爬虫机器池中，获取机器.
		// runmanager.setHostIp(Config.getValue("hostip"));

		int port = DispatchConstant.HOST_PORT;
		if (StringUtils.isNotBlank(Config.getValue("hostport"))) {
			port = Integer.valueOf(Config.getValue("hostport"));
		}
		runmanager.setPort(port);
		List<String> dispatchFolderNameList = new ArrayList<String>();
		dispatchFolderNameList.addAll(judgeFolderNameList);
		Collections.sort(dispatchFolderNameList, new Comparator<String>() {

			@Override
			public int compare(String arg1, String arg2) {
				int seq1 = getSequence(arg1);
				int seq2 = getSequence(arg2);
				return seq1 - seq2;
			}

		});

		for (Iterator<String> it = dispatchFolderNameList.iterator(); it.hasNext();) {
			String dispatchFolderName = it.next();

			// --irm.cnstock.com_1hour_1
			String folderNameSeed = dispatchFolderName.substring(0, dispatchFolderName.lastIndexOf("_"));
			String folderNameData = folderNameSeed.substring(0, folderNameSeed.lastIndexOf("_"));

			if (isIncrement) {
				String[] folderNameStrs = folderNameSeed.split("_");
				folderNameSeed = folderNameStrs[0] + "_" + folderNameStrs[1] + "_" + DispatchConstant.INCREMENT_FILENAME_SIGN + "_" + folderNameStrs[2];
				folderNameData = folderNameData.substring(0, folderNameData.lastIndexOf("_")) + "_" + DispatchConstant.INCREMENT_FILENAME_SIGN;
			}
			String depth = Config.getValue("depth");

			// -- 生成种子目录.
			String seedFolder = rootFolder + File.separator + folderNameSeed;
			// -- 如果是集群模式.
			if (DispatchConstant.MODEL_DEPLOY.equals(Config.getValue(DispatchConstant.KEY_MODEL))) {
				seedFolder = Config.getValue(DispatchConstant.KEY_HDFS_ROOT_PREFIX) + folderNameSeed;
			}
			DispatchVo dispatchVo = RedisUtils.getDispatchResult(dispatchFolderName, DispatchConstant.DISPATCH_REDIS_DBINDEX);
			boolean userProxy = dispatchVo.isUserProxy();
			if (userProxy) {
				shDir = Config.getValue(DispatchConstant.KEY_USE_PROXY);
			}
			
			runmanager.setDispatchFolderName(dispatchFolderName);
			if (DispatchConstant.MODEL_LOCAL.equals(Config.getValue(DispatchConstant.KEY_MODEL))) {

				// LOG.info("指令加入队列完成:" + command);
				LOG.info("开始从爬虫机器池中，获取爬虫机器." + seedFolder);
				CrawlerMachineBean crawlerMachineBean = crawlerMachinePool.getCrawlerMachine();
				String hostIP = crawlerMachineBean.getHostIp();
				runmanager.setHostIp(hostIP);
				String crawlerData = crawlDir + folderNameData + "_data_" + runmanager.getHostIp() + "_" + System.currentTimeMillis();
	            String command = shDir + " " + seedFolder + " " + crawlerData + " " + solr + " " + depth;
	            runmanager.setCrawlerData(crawlerData);
	            runmanager.setCommand(command);
				runmanager.setCrawlerMachineBean(crawlerMachineBean);
				LOG.info("指令:" + command);
				LOG.info("获取到爬虫机器，IP：" + hostIP + "开始执行." + command);
				jobPool.setRunmanager(runmanager);
				new Thread(new ExecJob()).start();
			} else if (DispatchConstant.MODEL_DEPLOY.equals(Config.getValue(DispatchConstant.KEY_MODEL))) {
				// dispatchVo.setStatus(DispatchConstant.DISPATCH_STATIS_RUNNING);
				// RedisUtils.setResult(dispatchVo, dispatchFolderName,
				// DispatchConstant.DISPATCH_REDIS_DBINDEX);
			    runmanager.setHostIp(Config.getValue("hostip"));
			    String command = shDir + " " + seedFolder + " " + crawlDir + folderNameData + "_data" + " " + solr + " " + depth;
			    runmanager.setCommand(command);
			    LOG.info("指令开始执行:" + command);
			    new ShellUtils().execCmd(runmanager);
			    LOG.info("指令执行完成:" + command);
				// dispatchVo.setStatus(DispatchConstant.DISPATCH_STATIS_COMPLETE);
				// RedisUtils.setResult(dispatchVo, dispatchFolderName,
				// DispatchConstant.DISPATCH_REDIS_DBINDEX);
			} else {
				LOG.info("请配置运行模式.config.properties#model");
			}
		}

	}

	/**
	 * 获取时序.
	 * 
	 * @param str
	 * @return
	 */
	private int getSequence(String str) {
		if (StringUtils.isBlank(str)) {
			return 0;
		}
		String folderName = str.substring(0, str.lastIndexOf("_"));
		String sequence = folderName.substring(folderName.lastIndexOf("_") + 1, folderName.length());
		return Integer.valueOf(sequence);
	}

	// private List<String> sortFolderNameList(List<String> judgeFolderNameList)
	// {
	// Map<Integer, String> tempMap = new TreeMap<Integer, String>();
	// for(Iterator<String> it = judgeFolderNameList.iterator(); it.hasNext();)
	// {
	// String temp = it.next();
	// String folderName = temp.substring(0, temp.lastIndexOf("_"));
	// String sequence = folderName.substring(folderName.lastIndexOf("_"),
	// folderName.length());
	// tempMap.put(Integer.valueOf(sequence), temp);
	// }
	// List<String> resultList = new ArrayList<String>();
	//
	// }
}
