package com.isoftstone.dispatch.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Progressable;

import com.isoftstone.dispatch.consts.DispatchConstant;

public class HdfsCommon {

    private static final Log LOG = LogFactory.getLog(HdfsCommon.class);

    public static void upAllFileToHdfs() {
        // 将本地文件上传到hdfs。
        // "hdfs://192.168.100.231:8020/user/hdfs/tmp1/"
        String target = Config.getValue(DispatchConstant.KEY_HDFS_ROOT_FOLDER);
        if (StringUtils.isBlank(target)) {
            return;
        }
        String rootFolder = Config
                .getValue(DispatchConstant.KEY_LOCAL_ROOT_FOLDER);
        List<String> folderNameList = RedisUtils.getResultList("*(increment)",
            DispatchConstant.DISPATCH_REDIS_DBINDEX);
        List<String> normalFolderNameList = RedisUtils.getResultList(
            "*_dispatch", DispatchConstant.DISPATCH_REDIS_DBINDEX);
        folderNameList.addAll(normalFolderNameList);
        for (Iterator<String> it = folderNameList.iterator(); it.hasNext();) {
            String folderNameTemp = it.next();
            String folderName = folderNameTemp.substring(0,
                folderNameTemp.lastIndexOf("_"));
            String[] folderTemp = folderName.split("_");
            String sequence = folderTemp[folderTemp.length - 1];
            String incrementFolderName = folderName.substring(0,
                folderName.lastIndexOf("_"))
                    + "_increment_" + sequence;
            Configuration config = new Configuration();
            FileSystem fs = null;
            FileInputStream fis = null;
            OutputStream os = null;
            try {
                fs = FileSystem.get(URI.create(target), config);
                // --拷贝全量种子文件夹.
                String folderPath = rootFolder + File.separator + folderName
                        + File.separator + DispatchConstant.SEED_FILE_NAME;
                fis = new FileInputStream(new File(folderPath));
                os = fs.create(new Path(target + folderPath));
                // copy
                IOUtils.copyBytes(fis, os, 4096, true);

                //--拷贝增量种子文件夹.
                String incrementFolderPath = rootFolder + File.separator
                        + incrementFolderName + File.separator
                        + DispatchConstant.SEED_FILE_NAME;
                fis = new FileInputStream(new File(incrementFolderPath));
                os = fs.create(new Path(target + incrementFolderPath));
                // copy
                IOUtils.copyBytes(fis, os, 4096, true);
            } catch (IOException e) {
                LOG.error("上传到Hdfs时，出现异常.", e);
            } finally {
                try {
                    if (fs != null) {
                        fs.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                    if (os != null) {
                        os.close();
                    }

                } catch (IOException e) {
                    LOG.error("关闭流异常.", e);
                }

            }
        }

    }

    /**
     * 针对linux文件系统，将其本地文件上传到Hdfs上.
     * @param fileName 本地文件目录.
     * @throws IOException
     */
    public static void upFileToHdfs(String fileName) {
        // 将本地文件上传到hdfs。
        //"hdfs://192.168.100.231:8020/user/hdfs/tmp1/"
        LOG.info("上传文件到hdfs：" + fileName);
        String target = Config.getValue(DispatchConstant.KEY_HDFS_ROOT_FOLDER);
        if (StringUtils.isBlank(target)) {
            return;
        }
        Configuration config = new Configuration();
        FileSystem fs = null;
        FileInputStream fis = null;
        OutputStream os = null;
        try {
            fs = FileSystem.get(URI.create(target), config);
            fis = new FileInputStream(new File(fileName));
            os = fs.create(new Path(target + fileName));
            // copy
            IOUtils.copyBytes(fis, os, 4096, true);
        } catch (IOException e) {
            LOG.error("", e);
        } finally {
            try {
                if (fs != null) {
                    fs.close();
                }
                if (fis != null) {
                    fis.close();
                }
                if (os != null) {
                    os.close();
                }

            } catch (IOException e) {
                LOG.error("关闭流异常.", e);
            }

        }
    }

    public static boolean copyFile(String src, String dst, Configuration conf) throws Exception {
        FileSystem fs = FileSystem.get(conf);
        fs.exists(new Path(dst));
        //FileStatus status = fs.getFileStatus(new Path(dst));
        File file = new File(src);
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        /**
        * FieSystem的create方法可以为文件不存在的父目录进行创建，
        */
        OutputStream out = fs.create(new Path(dst), new Progressable() {
            public void progress() {
                System.out.print(".");
            }
        });
        IOUtils.copyBytes(in, out, 4096, true);
        return true;
    }

    public static boolean copyDirectory(String src, String dst,
            Configuration conf) throws Exception {
        FileSystem fs = FileSystem.get(conf);
        if (!fs.exists(new Path(dst))) {
            fs.mkdirs(new Path(dst));
        }
        System.out.println("copyDirectory:" + dst);
        FileStatus status = fs.getFileStatus(new Path(dst));
        File file = new File(src);

        if (status.isDir()) {
            dst = cutDir(dst);
        } else {
            LOG.info("You put in the " + dst + "is file !");
            return false;
        }
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                copyDirectory(f.getPath(), dst, conf);
            } else {
                upFileToHdfs(f.getPath());
            }
        }
        return true;
    }

    public static String cutDir(String str) {
        String[] strs = str.split(File.pathSeparator);
        String result = "";
        if ("hdfs" == strs[0]) {
            result += "hdfs://";
            for (int i = 1; i < strs.length; i++) {
                result += strs[i] + File.separator;
            }
        } else {
            for (int i = 0; i < strs.length; i++) {
                result += strs[i] + File.separator;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            LOG.info("请输入正确的参数：文件名.");
            return;
        }
        String localSrc = args[0];
        String dst = "/";
        Configuration conf = new Configuration();
        File srcFile = new File(localSrc);
        if (srcFile.isDirectory()) {
            try {
                copyDirectory(localSrc, dst, conf);
            } catch (Exception e) {
                LOG.error("上传Hdfs中，出现错误.", e);
            }
        }
    }

}
