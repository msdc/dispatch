package com.isoftstone.dispatch.consts;

public class DispatchConstant {

    public static final String KEY_FOLDER_NAME_LIST = "folderNameList";

    public static final String KEY_FOLDER_WITHOUT_SEQUENCE = "folderWithoutSequence";

    public static final String FOLDER_SEPARATE = "_";

    public static final String REDIS_IP = "127.0.0.1";

    public static final int REDIS_PORT = 6379;

    public static final int JOB_RUNNING = 1;

    public static final int JOB_END = 0;

    public static final int HOST_PORT = 22;

    public static final String DISPATCH_STATIS_START = "start";

    public static final String DISPATCH_STATIS_RUNNING = "running";

    public static final String DISPATCH_STATIS_COMPLETE = "complete";

    public static final String REDIS_KEY_DISPATCH_FOLDERNAME = "*_dispatch";

    public static final Integer SCHEDULER_TYPE_SECOND = 0;

    public static final Integer SCHEDULER_TYPE_MINUTE = 1;

    public static final Integer SCHEDULER_TYPE_HOUR = 2;

    public static final Integer SCHEDULER_TYPE_DAY = 3;

    public static final Integer SCHEDULER_TYPE_WEEK = 4;

    public static final Integer DISPATCH_REDIS_DBINDEX = 2;

    public static final String KEY_INCREMENT = "increment";

    public static final String INCREMENT_TRUE = "true";

    public static final String KEY_MODEL = "model";

    public static final String MODEL_LOCAL = "local";

    public static final String MODEL_DEPLOY = "deploy";

    public static final String KEY_USE_PROXY = "proxyShDir";

    public static final String KEY_HDFS_ROOT_FOLDER = "hdfsRootFolder";

    public static final String KEY_HDFS_ROOT_PREFIX = "hdfsRootPrefix";

    public static final String KEY_LOCAL_ROOT_FOLDER = "localRootFolder";

    public static final String SEED_FILE_NAME = "seed.txt";

    // -- 增量文件夹命名标识.
    public static final String INCREMENT_FILENAME_SIGN = "increment";

    public static final String MAX_CRAWLER_MACHINE_SIZE = "maxCrawlerMachineSize";

    public static final String NUTCH_HOST_IP = "nutchHostIp";
    
    public static final String NUTCH_HOST_IP_SPLIT = ";";
    
    public static final String KEY_IS_COPYFOLDER = "isCopyFile";

}