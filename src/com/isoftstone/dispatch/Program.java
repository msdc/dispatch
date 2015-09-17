package com.isoftstone.dispatch;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.isoftstone.dispatch.consts.DispatchConstant;
import com.isoftstone.dispatch.crawlerpool.CrawlerMachineBean;
import com.isoftstone.dispatch.crawlerpool.CrawlerMachinePool;
import com.isoftstone.dispatch.quartzjob.CheckFileJob;
import com.isoftstone.dispatch.utils.Config;
import com.isoftstone.dispatch.utils.HdfsCommon;

public class Program {
	private static final Log LOG = LogFactory.getLog(Program.class);

	private static final Integer INTERVAL_IN_MINUTES = 60;

	public static void main(String[] args) {
		// --1. 如果是deploy模式,将文件系统中的文件拷贝到hdfs上.
		// --2. 如果是local模式，则启动队列执行.
		if (DispatchConstant.MODEL_DEPLOY.equals(Config.getValue(DispatchConstant.KEY_MODEL))) {
			HdfsCommon.upAllFileToHdfs();
		} else if (DispatchConstant.MODEL_LOCAL.equals(Config.getValue(DispatchConstant.KEY_MODEL))) {
			// new Thread(new ExecJob()).start();
			// -- 启动爬虫机器池.
			CrawlerMachinePool pool = CrawlerMachinePool.getInstance();
			String nutchHostIp = Config.getValue(DispatchConstant.NUTCH_HOST_IP);
			String[] nutchHostIps = nutchHostIp.split(DispatchConstant.NUTCH_HOST_IP_SPLIT);
			for (int i = 0; i < nutchHostIps.length; i++) {
				CrawlerMachineBean bean = new CrawlerMachineBean(nutchHostIps[i]);
				pool.addCrawlerMachine(bean);
			}
		}

		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched;
		try {
			sched = sf.getScheduler();
			String job_name = "检查文件夹任务";
			JobDetail check_file = JobBuilder.newJob(CheckFileJob.class).withIdentity(job_name, "Group").build();
			Trigger checkTrigger = TriggerBuilder.newTrigger().withIdentity(job_name, "Group").withSchedule(SimpleScheduleBuilder.simpleSchedule().repeatForever().withIntervalInMinutes(INTERVAL_IN_MINUTES)).startAt(new Date()).build();
			sched.scheduleJob(check_file, checkTrigger);

			sched.start();
		} catch (SchedulerException e) {
			LOG.error("检查文件Job异常", e);
		}
	}

}
