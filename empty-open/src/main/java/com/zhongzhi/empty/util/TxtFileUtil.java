package com.zhongzhi.empty.util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.zhongzhi.empty.vo.SplitFileVo;

import lombok.extern.slf4j.Slf4j;

/**
 * txt文件处理
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
public class TxtFileUtil {

	private final static String DEFAULT_CHARSET = "utf-8";
    /**
     * 读取指定行数据
     *
     * @param filePath    文件路径
     * @param charsetName 文件编码
     * @param lineStart   开始行 从第1行开始
     * @param lineSize    读取行数
     * @return
     * @throws IOException
     */
    public static List<String> readTxt(String filePath, String charsetName, long lineStart, long lineSize) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(new File(filePath))), charsetName))) {
            long perSplitLines = lineStart + lineSize - 1;
            String line = null;
            //逐行读取，逐行输出
            for (long lineCounter = 0; lineCounter < perSplitLines && (line = input.readLine()) != null; ++lineCounter) {
                if (lineCounter >= lineStart - 1) {
                    if (StringUtils.isNotBlank(line)) {
                        result.add(line);
                    }
                }
            }
        }
        return result;
    }


    /**
     * 保存数据到文本
     *
     * @param lines       数据
     * @param filePath    文件路径
     * @param charsetName 文件编码
     * @param append      是否追加
     * @throws IOException
     */
    public static void saveTxt(List<String> lines, String filePath, String charsetName, boolean append) throws IOException {
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filePath), append), charsetName))) {
            for (String line : lines) {
                output.append(line).append("\r\n");
            }
        }
    }
    
    /**
     * 保存数据到文本
     *
     * @param lines       数据
     * @param filePath    文件路径
     * @param charsetName 文件编码
     * @param append      是否追加
     * @throws IOException
     */
    public static void saveSetTxt(Set<String> lines, String filePath, String charsetName, boolean append) throws IOException {
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filePath), append), charsetName))) {
            for (String line : lines) {
                output.append(line).append("\r\n");
            }
        }
    }

    /**
     * 将A文本数据保存到B文本
     *
     * @param fromFile    A文本
     * @param toFile      B文本
     * @param charsetName 文件编码
     * @param append      是否追加
     * @return
     * @throws IOException
     */
    public static void appendTxt(String fromFile, String toFile, String toFileDir, String charsetName, boolean append) throws IOException {
        File file = new File(fromFile);
        File outFile = new File(toFileDir);
        if (!outFile.exists()) {
            outFile.mkdirs();
        }
        if (file.exists()) {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(new File(fromFile))), charsetName));
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(toFile), append), charsetName))) {
                String line = null;
                //逐行读取，逐行输出
                while ((line = input.readLine()) != null) {
                    output.append(line).append("\r\n");
                }
            }
        }
    }
    
    /**
     * 将A文本数据保存到B文本
     *
     * @param fromFile    A文本
     * @param toFile      B文本
     * @param charsetName 文件编码
     * @param append      是否追加
     * @return
     * @throws IOException
     */
    public static int appendTxt(File file, String toFile, String toFileDir, String charsetName, boolean append) throws IOException {
        int count = 0;
    	File outFile = new File(toFileDir);
        if (!outFile.exists()) {
            outFile.mkdirs();
        }
        if (file.exists()) {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), charsetName));
                 BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(toFile), append), charsetName))) {
                String line = null;
                //逐行读取，逐行输出
                while ((line = input.readLine()) != null) {
                	count++;
                    output.append(line).append("\r\n");
                }
                
                log.info("{}，文件合并完成，count:{}",file.getAbsolutePath(),count);
            }
        }
        
        return count;
    }
    
    /**
     * 将A文本数据保存到B文本
     *
     * @param fromFile    A文本
     * @param toFile      B文本
     * @param charsetName 文件编码
     * @param append      是否追加
     * @return
     * @throws IOException
     */
    public static int appendTxtNew(File file, String toFile, String toFileDir, String charsetName, boolean append) throws IOException {
        int count = 0;
    	File outFile = new File(toFileDir);
        if (!outFile.exists()) {
            outFile.mkdirs();
        }
        if (file.exists()) {
        	List<String> tempList = readTxt(file.getAbsolutePath(), charsetName);
        	saveTxt(tempList, toFile, charsetName, append);
        	count = tempList.size();
            log.info("{}，文件合并完成，count:{}",file.getAbsolutePath(),tempList.size());
        }
        
        return count;
    }

    /**
     * 统计文件行数
     *
     * @param filePath
     * @param charsetName
     * @return
     * @throws IOException
     */
    public static int countLines(String filePath, String charsetName) throws IOException {
        File file = new File(filePath);
        Map<String,String> resultList = new HashMap<String,String>();
        if (file.exists() && file.isFile() ) {
        	//文件大于8M则分成8个小文件进行去重
            if (file.length() >= 1048576 * 6) {
            	File[] littleFiles = splitFile(file, 16);
            	return distinct(littleFiles, filePath, 16);
			}
            
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(new File(filePath))), charsetName))) {
                	String line = null;	                
					//逐行读取，逐行输出
	                while ((line = input.readLine()) != null) {
	                	line = line.trim();
	                    if (line.length() > 1 && StringUtils.isNotBlank(line)) {
	                    	resultList.put(line,"");
	                    }
	                }
	                //删除去重前的文件
	                if(deleteFile(filePath)) {
	                	//保存去重后的文件到结果文件
	                	saveTxtBySetList(resultList, filePath, charsetName);
	                }
			}                
        }
        return resultList.keySet().size();
    }
    
    public static void main(String[] args) throws IOException {
		System.out.println(countLines("D:\\opt\\upload\\direct\\20221020\\911363331007455232\\1030862212417191936\\已激活.txt", "utf-8"));
	}
    
    private static List<String> readTxt(String filePath,String charsetName) throws UnsupportedEncodingException, FileNotFoundException, IOException{
    	Map<String,String> resultList = new HashMap<String,String>();
    	try (BufferedReader input = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(new File(filePath))), charsetName))) {
        	String line = null;	                
			//逐行读取，逐行输出
            while ((line = input.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                	resultList.put(line,"");
                }
            }
            
    	}
    	
    	return new ArrayList<String>(resultList.keySet());
    }
    
    /**
     * 统计文件行数-不需要去重
     *
     * @param filePath
     * @param charsetName
     * @return
     * @throws IOException
     */
    public static int countLinesByActive(String filePath, String charsetName) throws IOException {
        int count = 0;
        File file = new File(filePath);
        if (file.exists()) {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(new File(filePath))), charsetName))) {
                String line = null;
                //逐行读取，逐行输出
                while ((line = input.readLine()) != null) {
                    if (StringUtils.isNotBlank(line)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
    
    /**
     * 去重且合并
     *
     * @param littleFiles
     * @param distinctFilePath
     * @param splitSize
     * @return
     * @throws IOException
     */
    public static int distinct(File[] littleFiles,String distinctFilePath,int splitSize) {
    	int fileCount = 0;
    	File distinctedFile = new File(distinctFilePath);
        try {
            if(distinctedFile.exists()){
                distinctedFile.delete();
            }
           
            for(int i=0;i<splitSize;i++){
                if(littleFiles[i].exists()){
                	fileCount +=appendTxt(littleFiles[i], distinctFilePath, littleFiles[i].getParent(), DEFAULT_CHARSET, true);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1){
            e1.printStackTrace();
        } finally {
            for(int i=0;i<splitSize;i++){                
                //合并完成之后删除临时小文件
                if(littleFiles[i].exists()){
                    littleFiles[i].delete();
                }
            }            
        }
        
        return fileCount;
    }
    
    /**
     * 去重且合并
     *
     * @param littleFiles
     * @param distinctFilePath
     * @param splitSize
     * @return
     * @throws IOException
     */
    public static int distinctNew(File[] littleFiles,String distinctFilePath,int splitSize) {
    	int fileCount = 0;
    	File distinctedFile = new File(distinctFilePath);
        try {
            if(distinctedFile.exists()){
                distinctedFile.delete();
            }
           
            for(int i=0;i<splitSize;i++){
                if(littleFiles[i].exists()){
                	fileCount +=appendTxtNew(littleFiles[i], distinctFilePath, littleFiles[i].getParent(), DEFAULT_CHARSET, true);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1){
            e1.printStackTrace();
        } finally {
            for(int i=0;i<splitSize;i++){                
                //合并完成之后删除临时小文件
                if(littleFiles[i].exists()){
                    littleFiles[i].delete();
                }
            }            
        }
        
        return fileCount;
    }
    
    /**
     * 分割大文件成小文件
     *
     * @param filePath
     * @param splitSize
     * @return
     * @throws IOException
     */
    public static File[] splitFile(File file,int splitSize) {
    	BufferedReader reader = null;
        PrintWriter[] pws = new PrintWriter[splitSize];
        File[] littleFiles = new File[splitSize];
        String parentPath = file.getParent();
        File tempFolder = new File(parentPath + File.separator + "test");
        if(!tempFolder.exists()){
            tempFolder.mkdir();
        }
        for(int i=0;i<splitSize;i++){
            littleFiles[i] = new File(tempFolder.getAbsolutePath() + File.separator + (i+1) + ".txt");
            if(littleFiles[i].exists()){
                littleFiles[i].delete();
            }
            try {
                pws[i] = new PrintWriter(littleFiles[i]);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                if(tempString != ""){
                    //关键是将每行数据hash取模之后放到对应取模值的文件中，确保hash值相同的字符串都在同一个文件里面
                    int index = Math.abs(tempString.hashCode() % splitSize);
                    pws[index].println(tempString);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            for(int i=0;i<splitSize;i++){
                if(pws[i] != null){
                    pws[i].close();
                }
            }
        }
        return littleFiles;
    }
    
    /**
     * 分割大文件成小文件
     *
     * @param filePath
     * @param splitSize
     * @return
     * @throws IOException
     */
    public static SplitFileVo splitFileByError(File file,int splitSize,Long tempOrders) {
    	BufferedReader reader = null;
    	Integer errorCounts = 0;
    	List<String> mobileList = new ArrayList<String>();
        PrintWriter[] pws = new PrintWriter[splitSize];
        File[] littleFiles = new File[splitSize];
        String parentPath = file.getParent();
        File tempFolder = new File(parentPath + File.separator + tempOrders);
        if(!tempFolder.exists()){
            tempFolder.mkdir();
        }
        for(int i=0;i<splitSize;i++){
            littleFiles[i] = new File(tempFolder.getAbsolutePath() + File.separator + (i+1) + ".txt");
            if(littleFiles[i].exists()){
                littleFiles[i].delete();
            }
            try {
                pws[i] = new PrintWriter(littleFiles[i]);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                if(tempString != ""){
                	// 去掉字符串中的所有空格
                	tempString = tempString.trim().replace(" ", "").replace("　", "");
	                // 验证是否为正常的１１位有效数字
	                if (!CommonUtils.isMobile(tempString)) {
	                	errorCounts++;
	                    continue;
	                }
	                
	                if(mobileList.size() < 72) {
	                	mobileList.add(tempString);
	                }
	                
                    //关键是将每行数据hash取模之后放到对应取模值的文件中，确保hash值相同的字符串都在同一个文件里面
                    int index = Math.abs(tempString.hashCode() % splitSize);
                    pws[index].println(tempString);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            for(int i=0;i<splitSize;i++){
                if(pws[i] != null){
                    pws[i].close();
                }
            }
        }
        
        log.info("{}， 文件分割完成，数量：{}",file.getAbsolutePath(),littleFiles.length);
        return new SplitFileVo(littleFiles, errorCounts,mobileList);
    }
    
    public static SplitFileVo splitInternationalFileByError(File file,int splitSize,Long tempOrders) {
    	BufferedReader reader = null;
    	Integer errorCounts = 0;
    	List<String> mobileList = new ArrayList<String>();
        PrintWriter[] pws = new PrintWriter[splitSize];
        File[] littleFiles = new File[splitSize];
        String parentPath = file.getParent();
        File tempFolder = new File(parentPath + File.separator + tempOrders);
        if(!tempFolder.exists()){
            tempFolder.mkdir();
        }
        for(int i=0;i<splitSize;i++){
            littleFiles[i] = new File(tempFolder.getAbsolutePath() + File.separator + (i+1) + ".txt");
            if(littleFiles[i].exists()){
                littleFiles[i].delete();
            }
            try {
                pws[i] = new PrintWriter(littleFiles[i]);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                if(tempString != ""){
                	// 去掉字符串中的所有空格
                	tempString = tempString.trim().replace(" ", "").replace("　", "");
	                // 验证是否为正常的有效数字
	                if (!CommonUtils.isNumericByInternational(tempString)) {
	                	errorCounts++;
	                    continue;
	                }
	                
	                if(mobileList.size() < 72) {
	                	mobileList.add(tempString);
	                }
	                
                    //关键是将每行数据hash取模之后放到对应取模值的文件中，确保hash值相同的字符串都在同一个文件里面
                    int index = Math.abs(tempString.hashCode() % splitSize);
                    pws[index].println(tempString);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            for(int i=0;i<splitSize;i++){
                if(pws[i] != null){
                    pws[i].close();
                }
            }
        }
        
        log.info("{}， 国际文件分割完成，数量：{}",file.getAbsolutePath(),littleFiles.length);
        return new SplitFileVo(littleFiles, errorCounts,mobileList);
    }
    
    public static SplitFileVo splitIntDirectFileByError(File file,int splitSize,Long tempOrders,String countryCode) {
    	BufferedReader reader = null;
    	Integer errorCounts = 0;
    	List<String> mobileList = new ArrayList<String>();
        PrintWriter[] pws = new PrintWriter[splitSize];
        File[] littleFiles = new File[splitSize];
        String parentPath = file.getParent();
        File tempFolder = new File(parentPath + File.separator + tempOrders);
        if(!tempFolder.exists()){
            tempFolder.mkdir();
        }
        for(int i=0;i<splitSize;i++){
            littleFiles[i] = new File(tempFolder.getAbsolutePath() + File.separator + (i+1) + ".txt");
            if(littleFiles[i].exists()){
                littleFiles[i].delete();
            }
            try {
                pws[i] = new PrintWriter(littleFiles[i]);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                if(tempString != ""){
                	// 去掉字符串中的所有空格
                	tempString = tempString.trim().replace(" ", "").replace("　", "");
	                // 验证是否为正常的有效数字
	                if (!CommonUtils.isNumericByInternational(tempString)) {
	                	errorCounts++;
	                    continue;
	                }
	                
	                if("91".equals(countryCode)) {
	                	if(tempString.length() < 12) {
	                		tempString = countryCode + tempString;
	                	}
	                }else {
	                	if(!tempString.startsWith(countryCode)) {
	                		tempString = countryCode + tempString;
	                	}
	                }
	                
	                if(mobileList.size() < 72) {
	                	mobileList.add(tempString);
	                }
	                
                    //关键是将每行数据hash取模之后放到对应取模值的文件中，确保hash值相同的字符串都在同一个文件里面
                    int index = Math.abs(tempString.hashCode() % splitSize);
                    pws[index].println(tempString);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            for(int i=0;i<splitSize;i++){
                if(pws[i] != null){
                    pws[i].close();
                }
            }
        }
        
        log.info("{}， 定向国际文件分割完成，数量：{}",file.getAbsolutePath(),littleFiles.length);
        return new SplitFileVo(littleFiles, errorCounts,mobileList);
    }
    
    /**
     * 保存数据到文本-setlist
     *
     * @param lines       数据
     * @param filePath    文件路径
     * @param charsetName 文件编码
     * @param append      是否追加
     * @throws IOException
     */
    public static void saveTxtBySetList(Map<String,String> lines, String filePath, String charsetName) throws IOException {
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filePath), Boolean.FALSE), charsetName))) {
            for (String line : lines.keySet()) {
                output.append(line).append("\r\n");
            }
        }
    }
    
    /**
     * 删除文件
     */
    public static boolean deleteFile(String filePath) {
        boolean result = false;
        try {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                result=file.delete();
            }
        } catch (Exception ignored){
        }
        
        return result;
    }

}
