package com.zhongzhi.empty.util;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ZipUtil {
    /**
     * 压缩指定文件到当前文件夹
     *
     * @param srcFiles 要压缩的指定文件
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String[] srcFiles) {
        return zip(srcFiles, null);
    }

    /**
     * 使用给定密码压缩指定文件或文件夹到当前目录
     *
     * @param srcFiles 要压缩的文件数组
     * @param destFile   zip路径
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String[] srcFiles, String destFile) {
        return zip(srcFiles, destFile, null);
    }

    /**
     * 使用给定密码压缩指定文件或文件夹到当前目录
     *
     * @param srcFiles 要压缩的文件数组
     * @param destFile 目标文件名
     * @param passwd   压缩使用的密码
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String[] srcFiles, String destFile, String passwd) {
        if (srcFiles == null || srcFiles.length == 0) {
            return "";
        }
        List<File> filesToAdd = Arrays.stream(srcFiles).map(src -> new File(src)).collect(Collectors.toList());
        for (File file : filesToAdd) {
            if (file.isDirectory()) {
                return "";
            }
        }

        String zipFilePath;
        if (StringUtils.isNotBlank(destFile)) {
            zipFilePath = destFile;
        } else {
            zipFilePath = filesToAdd.get(0).getParent() + File.separator + filesToAdd.get(0).getParentFile().getName() + ".zip";
        }

        if (StringUtils.isNotBlank(passwd)) {
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
            ZipFile zipFile = new ZipFile(zipFilePath, passwd.toCharArray());
            try {
                zipFile.addFiles(filesToAdd, zipParameters);
            } catch (ZipException e) {
                log.error("压缩加密文件发生异常", e);
                return "";
            }
        } else {
            ZipFile zipFile = new ZipFile(zipFilePath);
            try {
                zipFile.addFiles(filesToAdd);
            } catch (ZipException e) {
                log.error("压缩文件发生异常", e);
                return "";
            }
        }
        return zipFilePath;
    }
}
