package com.zhongzhi.empty.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件操作工具类
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
public class FileUtil {

    /**
     * 合并文件
     *
     * @param filePaths
     * @param resultPath
     * @return
     */
    public static synchronized boolean mergeFiles(String[] filePaths, String resultPath) {
        if (filePaths == null || filePaths.length < 1 || StringUtils.isBlank(resultPath)) {
            log.info("待合并文件路径数组为空或结果文件路径为空");
            return false;
        }

        File[] files = new File[filePaths.length];
        for (int i = 0; i < filePaths.length; i++) {
            files[i] = new File(filePaths[i]);
            if (StringUtils.isBlank(filePaths[i]) || !files[i].exists() || !files[i].isFile()) {
                log.info("待合并文件不存在或者不是文件");
                return false;
            }
        }

        File resultFile = new File(resultPath);
        // 判断目录是否存在，不存在，则创建，如创建失败，则抛出异常
        if (!resultFile.getParentFile().exists()) {
            boolean flag = resultFile.getParentFile().mkdirs();
            if (!flag) {
                throw new RuntimeException("创建合并结果文件路径" + resultFile.getParentFile() + "目录失败！");
            }
        }
        if (resultFile.exists()) {
            resultFile.delete();
        }
        FileChannel resultFileChannel = null;
        try {
            resultFileChannel = new FileOutputStream(resultFile, true).getChannel();
            for (int i = 0; i < filePaths.length; i++) {
                FileChannel blk = new FileInputStream(files[i]).getChannel();
                resultFileChannel.transferFrom(blk, resultFileChannel.size(), blk.size());
                blk.close();
            }
        } catch (FileNotFoundException e) {
            log.error("合并文件发生文件找不到异常", e);
            return false;
        } catch (IOException e) {
            log.error("合并文件发生IO异常：", e);
            return false;
        } finally {
            IOUtils.close(resultFileChannel);
        }

        // 删除合并前文件
        // for (int i = 0; i < filePaths.length; i++) {
        //     files[i].delete();
        // }
        return true;
    }

    /**
     * 快速获取文件行数
     *
     * @param filePath
     * @return
     */
    public static int getFileLineNum(String filePath) {
        try (LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(filePath))) {
            lineNumberReader.skip(Long.MAX_VALUE);
            int lineNumber = lineNumberReader.getLineNumber();
            return lineNumber;//实际上是读取换行符数量 , 因为最后一行没有换行符所以需要+1,(这里读取的文件最后一行都是空白行，所以不需要+1)
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * 读取文件最后几行 <br>
     * 相当于Linux系统中的tail命令 读取大小限制是2GB
     *
     * @param filename 文件名
     * @param charset  文件编码格式,传null默认使用defaultCharset
     * @param rows     读取行数
     * @throws IOException
     */
    public static String readLastRows(String filename, Charset charset, int rows) throws IOException {
        charset = charset == null ? Charset.defaultCharset() : charset;
        String lineSeparator = System.getProperty("line.separator");
        try (RandomAccessFile rf = new RandomAccessFile(filename, "r")) {
            // 每次读取的字节数要和系统换行符大小一致
            byte[] c = new byte[lineSeparator.getBytes().length];
            // 在获取到指定行数和读完文档之前,从文档末尾向前移动指针,遍历文档每一个字节
            for (long pointer = rf.length(), lineSeparatorNum = 0; pointer >= 0 && lineSeparatorNum < rows; ) {
                // 移动指针
                rf.seek(pointer--);
                // 读取数据
                int readLength = rf.read(c);
                if (readLength != -1 && new String(c, 0, readLength).equals(lineSeparator)) {
                    lineSeparatorNum++;
                }
                //扫描完依然没有找到足够的行数,将指针归0
                if (pointer == -1 && lineSeparatorNum < rows) {
                    rf.seek(0);
                }
            }
            byte[] tempbytes = new byte[(int) (rf.length() - rf.getFilePointer())];
            rf.readFully(tempbytes);
            return new String(tempbytes, charset);
        }
    }

    public static boolean saveFileFromInputStream(InputStream inputStream, String saveFilePath) {
        File f = new File(saveFilePath);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                log.error("创建文件异常，文件路径：{}, {}", saveFilePath, e.getMessage());
                return false;
            }
        }
        int index;
        byte[] bytes = new byte[1024];
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            while ((index = inputStream.read(bytes)) != -1) {
                log.info(new String(bytes));
                out.write(new String(bytes).getBytes(StandardCharsets.UTF_8), 0, index);
                out.flush();
            }
            inputStream.close();
            out.close();
            return true;
        } catch (Exception e) {
            log.error("写入文件异常，文件路径：{}，{}", saveFilePath, e.getMessage());
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("关闭文件流异常，文件路径：{}, {}", saveFilePath, e.getMessage());
                }
            }
        }
    }

    public static File TXTHandler(File file) {
        //原本这里的是gb2312，为了兼容更多，选择了gbk
        String code = "gbk";
        byte[] head = new byte[3];
        try {
            InputStream inputStream = new FileInputStream(file);
            inputStream.read(head);
            if (head[0] == -1 && head[1] == -2) {
                code = "UTF-16";
            } else if (head[0] == -2 && head[1] == -1) {
                code = "Unicode";
            } else if (head[0] == -17 && head[1] == -69 && head[2] == -65) {
                code = "UTF-8";
            }
            inputStream.close();

            if (code.equals("UTF-8")) {
                return file;
            }
            String str = org.apache.commons.io.FileUtils.readFileToString(file, code);
            org.apache.commons.io.FileUtils.writeStringToFile(file, str, "UTF-8");

        } catch (FileNotFoundException e) {
            log.error("文件找不到，文件路径：{}，{}", file, e.getMessage());
        } catch (IOException e) {
            log.error("文件io异常，文件路径：{}，{}", file, e.getMessage());
        }

        return file;
    }

    /**
     * 获取文件夹大小
     *
     * @param f
     * @return
     * @throws Exception
     */
    public static long getFileSize(File f) throws Exception {
        long size = 0;
        if (f.isDirectory()) {
            File[] flist = f.listFiles();
            for (int i = 0; i < flist.length; i++) {
                if (flist[i].isDirectory()) {
                    size = size + getFileSize(flist[i]);
                } else {
                    size = size + flist[i].length();
                }
            }
        } else {
            size = size + f.length();
        }

        return size;
    }

    /**
     * 迭代删除文件夹
     *
     * @param dirPath 文件夹路径
     */
    public static void deleteDir(String dirPath) {
        File file = new File(dirPath);
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            if (files == null) {
                file.delete();
            } else {
                for (int i = 0; i < files.length; i++) {
                    deleteDir(files[i].getAbsolutePath());
                }
                file.delete();
            }
        }
    }

    /**
     * 根据一个文件名，读取完文件，干掉bom头。
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public static void trimBom(String fileName) throws IOException {

        FileInputStream fin = new FileInputStream(fileName);
        // 开始写临时文件
        InputStream in = getInputStream(fin);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[4096];

        int len = 0;
        while (in.available() > 0) {
            len = in.read(b, 0, 4096);
            //out.write(b, 0, len);
            bos.write(b, 0, len);
        }

        in.close();
        fin.close();
        bos.close();

        //临时文件写完，开始将临时文件写回本文件。
        FileOutputStream out = new FileOutputStream(fileName);
        out.write(bos.toByteArray());
        out.close();
    }

    /**
     * 读取流中前面的字符，看是否有bom，如果有bom，将bom头先读掉丢弃
     *
     * @param in
     * @return
     * @throws java.io.IOException
     */
    public static InputStream getInputStream(InputStream in) throws IOException {

        PushbackInputStream testin = new PushbackInputStream(in);
        int ch = testin.read();
        if (ch != 0xEF) {
            testin.unread(ch);
        } else if ((ch = testin.read()) != 0xBB) {
            testin.unread(ch);
            testin.unread(0xef);
        } else if ((ch = testin.read()) != 0xBF) {
            throw new IOException("错误的UTF-8格式文件");
        } else {
            // 不需要做，这里是bom头被读完了
            // System.out.println("still exist bom");
        }
        return testin;

    }


}
