package com.isoftstone.dispatch.quartzjob;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.isoftstone.dispatch.vo.Runmanager;

public class JobPool {
	private static final Log LOG = LogFactory.getLog(JobPool.class);

	private int maxProxySize = 200;

	private BlockingQueue<Runmanager> jobQueue = new ArrayBlockingQueue<Runmanager>(
			maxProxySize);
	private static JobPool jobPool;

	private JobPool() {
	}

	public static JobPool getInstance() {
		if (jobPool == null) {
			jobPool = new JobPool();
		}
		return jobPool;
	}

	public Runmanager getRunmanager() {
		Runmanager runmanager = null;
		try {
			runmanager = jobQueue.take();
		} catch (InterruptedException e) {
			LOG.error("取元素被中断.", e);
		}
		return runmanager;
	}

	public void setRunmanager(Runmanager runmanager) {
		try {
			jobQueue.put(runmanager);
		} catch (InterruptedException e) {
			LOG.error("取元素被中断.", e);
		}
	}
	
	public int getJobSize() {
		return jobQueue.size();
	}

}
