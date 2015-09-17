/*
 * @(#)CrawlerMachinePool.java 2015-3-30 上午9:49:48 dispatch Copyright 2015
 * Isoftstone, Inc. All rights reserved. ISOFTSTONE PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 */
package com.isoftstone.dispatch.crawlerpool;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.isoftstone.dispatch.consts.DispatchConstant;
import com.isoftstone.dispatch.utils.Config;

/**
 * CrawlerMachinePool
 * 
 * @author danhb
 * @date 2015-3-30
 * @version 1.0
 *
 */
public class CrawlerMachinePool {

	private static final Log LOG = LogFactory.getLog(CrawlerMachinePool.class);

	private BlockingQueue<CrawlerMachineBean> crawlerMachineQueue = new ArrayBlockingQueue<CrawlerMachineBean>(Integer.valueOf(Config.getValue(DispatchConstant.MAX_CRAWLER_MACHINE_SIZE)));

	private static CrawlerMachinePool crawlerMachinePool;

	private CrawlerMachinePool() {
	}

	public static CrawlerMachinePool getInstance() {
		if (crawlerMachinePool == null) {
			crawlerMachinePool = new CrawlerMachinePool();
		}
		return crawlerMachinePool;
	}

	/**
	 * 向代理池中加入爬虫机器列表.
	 * 
	 * @param crawlerMachineList
	 *            爬虫机器列表.
	 */
	public void insertCrawlerMachineList(List<CrawlerMachineBean> crawlerMachineList) {
		if (CollectionUtils.isEmpty(crawlerMachineList)) {
			return;
		}
		try {
			for (CrawlerMachineBean crawlerMachine : crawlerMachineList) {
				this.crawlerMachineQueue.offer(crawlerMachine, 1, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			LOG.error("InterruptedException", e);
		}
	}

	/**
	 * 向爬虫机器池中添加一个机器.
	 * 
	 * @param proxy
	 *            代理.
	 */
	public void addCrawlerMachine(CrawlerMachineBean crawlerMachine) {
		try {
			this.crawlerMachineQueue.put(crawlerMachine);
		} catch (InterruptedException e) {
			LOG.error("InterruptedException", e);
		}
	}

	/**
	 * 获取一个爬虫机器,如果池中没有，则阻塞.
	 * 
	 * @return 爬虫机器.
	 */
	public CrawlerMachineBean getCrawlerMachine() {
		CrawlerMachineBean crawlerMachineBean = null;
		try {
			crawlerMachineBean = this.crawlerMachineQueue.take();
		} catch (InterruptedException e) {
			LOG.error("InterruptedException", e);
		}
		return crawlerMachineBean;
	}

}
