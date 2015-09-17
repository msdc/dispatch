package com.isoftstone.dispatch.quartzjob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.isoftstone.dispatch.consts.DispatchConstant;
import com.isoftstone.dispatch.parse.ParseSeeds;
import com.isoftstone.dispatch.utils.Config;
import com.isoftstone.dispatch.utils.FileUtils;
import com.isoftstone.dispatch.utils.QuartzUtils;
import com.isoftstone.dispatch.utils.RedisUtils;
import com.isoftstone.dispatch.vo.DispatchVo;
import com.isoftstone.dispatch.vo.Seed;
import com.isoftstone.dispatch.vo.StrategyVo;

public class CheckFileJob implements Job {

	private static final Log LOG = LogFactory.getLog(CheckFileJob.class);

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LOG.info("开始扫描.");
		boolean isIncrement = DispatchConstant.INCREMENT_TRUE.equals(Config.getValue(DispatchConstant.KEY_INCREMENT));
		List<String> folderNameList;
		if (isIncrement) {
			folderNameList = RedisUtils.getResultList("*(increment)", DispatchConstant.DISPATCH_REDIS_DBINDEX);
		} else {
			folderNameList = RedisUtils.getResultList("*_dispatch", DispatchConstant.DISPATCH_REDIS_DBINDEX);
		}
		if (CollectionUtils.isEmpty(folderNameList)) {
			LOG.info("扫描未发现目标文件.");
			return;
		}
		
		List<String> redisKeys = new ArrayList<String>();
		for (Iterator<String> it = folderNameList.iterator(); it.hasNext();) {
            String redisKey = it.next();
            redisKeys.add(redisKey);
        }
		//TODO:
        List<DispatchVo> dispatchVos = RedisUtils.getDispatchListResult(redisKeys, DispatchConstant.DISPATCH_REDIS_DBINDEX);
		
        //-- 如果是全量调度，则需要查看种子种的内容，是否有需要爬取的内容,如果没有，则直接返回.
        List<String> resultFolderNameList = new ArrayList<String>();
        for (Iterator<DispatchVo> it = dispatchVos.iterator(); it.hasNext();) {
            DispatchVo dispatchVo = it.next();
            if (dispatchVo != null && dispatchVo.getSeed() != null) {
                List<Seed> seeds = dispatchVo.getSeed();
                for (Iterator<Seed> seedIt = seeds.iterator(); seedIt.hasNext();) {
                    Seed seed = seedIt.next();
                    boolean isEnabled = "true".equals(seed.getIsEnabled());
                    if (isEnabled) {
                        if (isIncrement) {
                            resultFolderNameList.add(dispatchVo.getRedisKey());
                            break;
                        } else {
                            if (DispatchConstant.DISPATCH_STATIS_START.equals(seed.getStatus())) {
                                resultFolderNameList.add(dispatchVo.getRedisKey());
                                break;
                            }
                        }
                    }
                }
            }
		}
        
        if(resultFolderNameList.isEmpty()) {
            LOG.info("扫描未发现目标文件.");
            return;
        } else {
            folderNameList = resultFolderNameList;
        }
        
		Map<String, List<String>> folderNameListMap = classifyFolder(folderNameList);

		Map<String, StrategyVo> strategyMap = ParseSeeds.parseStrategy(folderNameListMap);
		if (strategyMap.isEmpty()) {
			LOG.info("扫描未发现目标文件.");
			return;
		}
		for (Iterator<Entry<String, StrategyVo>> it = strategyMap.entrySet().iterator(); it.hasNext();) {
			Entry<String, StrategyVo> entry = it.next();
			String key = entry.getKey();
			StrategyVo strategyVo = entry.getValue();
			String job_name = "动态任务调度" + key;
			if(DispatchConstant.MODEL_DEPLOY.equals(Config.getValue(DispatchConstant.KEY_MODEL))) {
			    if (QuartzUtils.containsJob(job_name)) {
			        continue;
			    }
			} else {
			    if (isIncrement && QuartzUtils.containsJob(job_name)) {
			        continue;
			    }
			}
			LOG.info(job_name + "加入调度");
			Map<String, String> paramMap = new HashMap<String, String>();
			paramMap.put(DispatchConstant.KEY_FOLDER_WITHOUT_SEQUENCE, key);
			QuartzUtils.addJob(job_name, NutchJob.class, strategyVo, paramMap);
		}
		LOG.info("扫描完成.");
	}

	private Map<String, List<String>> classifyFolder(List<String> folderNameList) {
		Map<String, List<String>> folderNameListMap = new HashMap<String, List<String>>();
		for (Iterator<String> it = folderNameList.iterator(); it.hasNext();) {
			String dispatchFolderName = it.next();
			if(StringUtils.isEmpty(dispatchFolderName)) {
			    LOG.info("出现为空的redisKey，请检查.");
			    continue;
			}
			String folderName = dispatchFolderName.substring(0, dispatchFolderName.lastIndexOf("_"));
			String key = FileUtils.getFolderNameWithoutSequence(folderName);
			List<String> value = folderNameListMap.get(key);
			if (value == null) {
				value = new ArrayList<String>();
				value.add(folderName);
				folderNameListMap.put(key, value);
			} else {
				value.add(folderName);
			}
		}
		return folderNameListMap;
	}

}
