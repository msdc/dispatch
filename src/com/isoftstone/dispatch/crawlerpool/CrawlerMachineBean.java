/*
 * @(#)CrawlerMachineBean.java 2015-3-30 上午9:52:06 dispatch Copyright 2015
 * Isoftstone, Inc. All rights reserved. ISOFTSTONE PROPRIETARY/CONFIDENTIAL.
 * Use is subject to license terms.
 */
package com.isoftstone.dispatch.crawlerpool;

/**
 * CrawlerMachineBean
 * @author danhb
 * @date  2015-3-30
 * @version 1.0
 *
 */
public class CrawlerMachineBean {
    private String hostIp;

    /**
     * 
     */
    public CrawlerMachineBean() {
        super();
    }

    /**
     * @param hostIp
     */
    public CrawlerMachineBean(String hostIp) {
        super();
        this.hostIp = hostIp;
    }

    /**
     * @return the hostIp
     */
    public String getHostIp() {
        return hostIp;
    }

    /**
     * @param hostIp the hostIp to set
     */
    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

}
