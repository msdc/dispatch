package com.isoftstone.dispatch.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.isoftstone.dispatch.consts.DispatchConstant;
import com.isoftstone.dispatch.vo.StrategyVo;

public class QuartzUtils {

	private static final Log LOG = LogFactory.getLog(QuartzUtils.class);

	private static SchedulerFactory sf = new StdSchedulerFactory();

	private static final String groupName = "NutchGroup";

	private static List<String> jobNameList = new ArrayList<String>();

	@SuppressWarnings("rawtypes")
	public static void addJob(String jobName, Class<? extends Job> clazz,
			StrategyVo strategy, Map paramMap) {
		try {
			Scheduler sched = sf.getScheduler();
			JobDetail jobDetail = JobBuilder.newJob(clazz)
					.withIdentity(jobName, groupName).build();
			if (paramMap != null) {
				for (Iterator it = paramMap.entrySet().iterator(); it.hasNext();) {
					Entry entry = (Entry) it.next();
					jobDetail.getJobDataMap().put((String) entry.getKey(),
							entry.getValue());
				}
			}
			SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder
					.simpleSchedule();
			// -- 全量模式 只执行一次.
			if (DispatchConstant.INCREMENT_TRUE.equals(Config
					.getValue(DispatchConstant.KEY_INCREMENT))) {
				simpleScheduleBuilder.repeatForever();
			} else {
				jobNameList.add(jobName);
				simpleScheduleBuilder.withRepeatCount(0);
			}
			Integer[] result = getScheduler(strategy);
			Integer type = result[0];
			Integer num = result[1];
			if (DispatchConstant.SCHEDULER_TYPE_SECOND == type) {
				simpleScheduleBuilder.withIntervalInSeconds(num);
			} else if (DispatchConstant.SCHEDULER_TYPE_MINUTE == type) {
				simpleScheduleBuilder.withIntervalInMinutes(num);
			} else if (DispatchConstant.SCHEDULER_TYPE_HOUR == type) {
				simpleScheduleBuilder.withIntervalInHours(num);
			} else {
				// -- 其他情况 按分钟级别执行.
				simpleScheduleBuilder.withIntervalInMinutes(num);
			}
			TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder
					.newTrigger().withIdentity(jobName, groupName);
			triggerBuilder.withSchedule(simpleScheduleBuilder);
			triggerBuilder.startAt(new Date());
			Trigger trigger = triggerBuilder.build();
			sched.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			LOG.error("添加job异常.", e);
		}
	}

	public static boolean containsJob(String jobName) {
		try {
			if (DispatchConstant.INCREMENT_TRUE.equals(Config
					.getValue(DispatchConstant.KEY_INCREMENT))) {
				Scheduler sched = sf.getScheduler();
				JobKey jobKey = new JobKey(jobName, groupName);
				return sched.checkExists(jobKey);
			}
			return jobNameList.contains(jobName);
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 通过strategy生成调度策略.
	 * 
	 * @param strategyVo
	 * @return
	 */
	public static Integer[] getScheduler(StrategyVo strategy) {
		if (strategy == null) {
			return null;
		}
		Integer[] result = new Integer[2];
		if (strategy.getSecond() != 0) {
			result[0] = DispatchConstant.SCHEDULER_TYPE_SECOND;
			result[1] = strategy.getSecond();
		}
		if (strategy.getMinute() != 0) {
			result[0] = DispatchConstant.SCHEDULER_TYPE_MINUTE;
			result[1] = strategy.getMinute();
		}
		if (strategy.getHour() != 0) {
			result[0] = DispatchConstant.SCHEDULER_TYPE_HOUR;
			result[1] = strategy.getHour();
		}
		if (strategy.getDay() != 0) {
			result[0] = DispatchConstant.SCHEDULER_TYPE_DAY;
			result[1] = strategy.getDay();
		}
		if (strategy.getWeek() != 0) {
			result[0] = DispatchConstant.SCHEDULER_TYPE_WEEK;
			result[1] = strategy.getWeek();
		}
		return result;

	}
}
