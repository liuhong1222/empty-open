package com.zhongzhi.empty.service.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ListMultimap;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.constants.EmptyRedisKeyConstant;
import com.zhongzhi.empty.entity.Customer;
import com.zhongzhi.empty.entity.CvsFilePath;
import com.zhongzhi.empty.entity.MobileColor;
import com.zhongzhi.empty.entity.TestResultData;
import com.zhongzhi.empty.entity.TxtFileContent;
import com.zhongzhi.empty.enums.InternationalTxtSuffixEnum;
import com.zhongzhi.empty.enums.MobileGroupEnum;
import com.zhongzhi.empty.enums.MobileReportGroupEnum;
import com.zhongzhi.empty.enums.MobileReportGroupEnum.FileDetection;
import com.zhongzhi.empty.enums.TxtSuffixEnum;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.service.customer.CustomerService;
import com.zhongzhi.empty.util.CommonUtils;
import com.zhongzhi.empty.util.DateUtils;
import com.zhongzhi.empty.util.DingDingMessage;
import com.zhongzhi.empty.util.EncodingDetect;
import com.zhongzhi.empty.util.FileUtil;
import com.zhongzhi.empty.util.Snowflake;
import com.zhongzhi.empty.util.TxtFileUtil;
import com.zhongzhi.empty.util.ZipUtil;
import com.zhongzhi.empty.vo.SplitFileVo;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件处理实现类
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
@Service
public class FileService {

	private final static String DEFAULT_CHARSET = "utf-8";
	
	private final static Integer BIG_DATA_API_REQUEST_SIZE = 2000;
	
	@Autowired
	private RedisClient redisClient;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private Snowflake snowflake;
	
	@Value("${empty.file.upload.path}")
	private String fileUploadPath;
	
	@Value("${empty.file.resource.path}")
	private String fileResourcePath;
	
	public TxtFileContent getValidMobileListByTxt(String fileUrl) {
		File file = new File(fileUrl); 
		if(file.length() <= 1048576 * 6) {
			return getValidMobileListBySmallTxt(fileUrl);
		}
		
		// 分割文件
		Long tempOrders = snowflake.nextId();
        SplitFileVo splitFileVo = TxtFileUtil.splitFileByError(file, (int)(((file.length() / (1048576 * 10)) + 1) * 8),tempOrders);
        // 合并文件且计算文件号码数量
    	int fileCount = TxtFileUtil.distinctNew(splitFileVo.getFileList(), getTxtPath(fileUrl, TxtSuffixEnum.ALL), (int)(((file.length() / (1048576 * 10)) + 1) * 8));
        
    	//获取文件编码格式
    	String fileCode = EncodingDetect.getJavaEncode(fileUrl);
    	TxtFileContent result =  new TxtFileContent();
    	result.setFileCode(fileCode);
        result.setMobileCounts(fileCount);
        result.setErrorCounts(splitFileVo.getErrorCounts());
        result.setMobileList(splitFileVo.getMobileList());
		return result;
	}
	
	public TxtFileContent getValidMobileListBySmallTxt(String fileUrl) {
		TxtFileContent result =  new TxtFileContent();
        File file = new File(fileUrl);        
        BufferedReader br = null;
        Integer errorCounts = 0;
        List<String> tempList = new ArrayList<String>();
		HashSet<String> temeSet = new HashSet<String>();
		//获取文件编码格式
    	String fileCode = EncodingDetect.getJavaEncode(fileUrl);
        try {
	        if (file.isFile() && file.exists()) {
	        	result.setFileCode(fileCode);
	        	//读取文件
	            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), fileCode));				
	            String lineTxt = null;	
	            while ((lineTxt = br.readLine()) != null) {	
	                if (StringUtils.isBlank(lineTxt)) {
	                    continue;
	                }
	                // 去掉字符串中的所有空格
	                lineTxt = lineTxt.trim().replace(" ", "").replace("　", "");
	                // 验证是否为正常的１１位有效数字
	                if (!CommonUtils.isMobile(lineTxt)) {
	                	errorCounts++;
	                    continue;
	                }
	                
	                temeSet.add(lineTxt);
	            }	 
	            
	            //保存全部的号码到缓存
	            tempList = new ArrayList<String>(temeSet);
	            TxtFileUtil.saveTxt(tempList, getTxtPath(fileUrl, TxtSuffixEnum.ALL), fileCode, false);
	        }
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			log.error(fileUrl+"----------文件编码格式转换异常：", e);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			log.error(fileUrl+"----------文件未找到：", e);
		} catch (IOException e) {
			e.printStackTrace();
			log.error(fileUrl+"----------文件流读取异常：", e);
		}finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                log.error(fileUrl+"----------文件流关闭异常：", e);
            }

        }

        result.setMobileList(tempList.subList(0, tempList.size() <= 72?tempList.size():72));
        result.setMobileCounts(tempList.size());
        result.setErrorCounts(errorCounts);
		return result;
	}
	
	public void saveTempFileByAll(String fileUrl, Long customerId, List<String> mobileList,Long emptyId) throws IOException {
		//保存72个号码以便进度页面显示为空
        saveDefaultMobileToRedis(customerId,emptyId,mobileList.subList(0, mobileList.size() <= 72?mobileList.size():72));
    	//保存全部的号码到缓存
//		TxtFileUtil.saveTxt(mobileList, getTxtPath(fileUrl, TxtSuffixEnum.ALL), EncodingDetect
//		        .getJavaEncode(fileUrl), false);
		
	}
	
	public void saveTestResultData(Long customerId, String fileUrl,Long emptyId,
			ListMultimap<MobileReportGroupEnum, String> group) throws IOException {
			saveGroupList(fileUrl,group);
			saveDateToRedis(customerId,emptyId,group);
	}
	
	public void saveMobileToTxtFile(String fileUrl, List<String> mobileList, TxtSuffixEnum txtSuffixEnum) throws IOException {
		TxtFileUtil.saveTxt(mobileList, getTxtPath(fileUrl, txtSuffixEnum), DEFAULT_CHARSET, true);
	}
	
	public List<String> readTxtFileContent(String fileUrl, String fileEncoding, int fromIndex) throws IOException {
		return TxtFileUtil.readTxt(getTxtPath(fileUrl, TxtSuffixEnum.ALL), fileEncoding, fromIndex, BIG_DATA_API_REQUEST_SIZE);
	}
	
	public TestResultData getTestResultByTxtFile(String fileUrl, Long customerId,Long emptyId,String sourceFileName) throws Exception {
		CvsFilePath cvsFilePath = new CvsFilePath();
		int totalCount = 0;    		
        List<String> list = new ArrayList<String>();
        String subFilePath = DateUtils.getDate() + "/" + customerId + "/" + emptyId + "/";
        String filePath = fileUploadPath + subFilePath;
        //生成实号报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, TxtSuffixEnum.REAL), filePath + CommonConstant.ACTIVE_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countReal = TxtFileUtil.countLines(filePath + CommonConstant.ACTIVE_FILE_NAME, DEFAULT_CHARSET);
        if (countReal > 0) {
            log.info("----------实号总条数：" + countReal);
            totalCount += countReal;
            list.add(filePath + CommonConstant.ACTIVE_FILE_NAME);
            cvsFilePath.setRealNumber(countReal);
            cvsFilePath.setRealFilePath(subFilePath + CommonConstant.ACTIVE_FILE_NAME);
            cvsFilePath.setRealFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.ACTIVE_FILE_NAME))));
        }
        // 生成空号报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, TxtSuffixEnum.KONG), filePath + CommonConstant.EMPTY_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countEmpty = TxtFileUtil.countLines(filePath + CommonConstant.EMPTY_FILE_NAME, DEFAULT_CHARSET);
        if (countEmpty > 0) {
            log.info("----------空号总条数：" + countEmpty);
            totalCount += countEmpty;
            list.add(filePath + CommonConstant.EMPTY_FILE_NAME);
            cvsFilePath.setEmptyNumber(countEmpty);
            cvsFilePath.setEmptyFilePath(subFilePath + CommonConstant.EMPTY_FILE_NAME);
            cvsFilePath.setEmptyFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.EMPTY_FILE_NAME))));
        }
        // 生成沉默号报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, TxtSuffixEnum.SILENCE), filePath + CommonConstant.SILENT_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countSilence = TxtFileUtil.countLines(filePath + CommonConstant.SILENT_FILE_NAME, DEFAULT_CHARSET);
        if (countSilence > 0) {
            log.info("----------沉默号总条数：" + countSilence);
            totalCount += countSilence;
            list.add(filePath + CommonConstant.SILENT_FILE_NAME);
            cvsFilePath.setSilentNumber(countSilence);
            cvsFilePath.setSilentFilePath(subFilePath + CommonConstant.SILENT_FILE_NAME);
            cvsFilePath.setSilentFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.SILENT_FILE_NAME))));
        }
        // 生成风险号报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, TxtSuffixEnum.SHUTDOWN), filePath + CommonConstant.RISK_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countShutdown = TxtFileUtil.countLines(filePath + CommonConstant.RISK_FILE_NAME, DEFAULT_CHARSET);
        if (countShutdown > 0) {
            log.info("----------风险号总条数：" + countShutdown);
            totalCount += countShutdown;
            list.add(filePath + CommonConstant.RISK_FILE_NAME);
            cvsFilePath.setRiskNumber(countShutdown);
            cvsFilePath.setRiskFilePath(subFilePath + CommonConstant.RISK_FILE_NAME);
            cvsFilePath.setRiskFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.RISK_FILE_NAME))));
        }
        // 报表文件打包
        if (!CollectionUtils.isEmpty(list)) {
            String zipName = sourceFileName + ".zip";
            //获取压缩包加密的密码
            Customer customer = customerService.getCustomerById(customerId);
            if (customer == null || StringUtils.isBlank(customer.getUnzipPassword())) {
                ZipUtil.zip(list.toArray(new String[list.size()]), filePath+zipName);
            } else {
                ZipUtil.zip(list.toArray(new String[list.size()]), filePath+zipName, customer.getUnzipPassword());
            }
            
            cvsFilePath.setZipName(zipName);
            cvsFilePath.setZipPath((subFilePath + zipName));
            cvsFilePath.setZipSize(String.valueOf(FileUtil.getFileSize(new File(filePath + zipName))));
            cvsFilePath.setTotalNumber(totalCount);
        }
        
        cvsFilePath.setCustomerId(customerId);
        cvsFilePath.setCreateTime(new Date());
        cvsFilePath.setEmptyId(emptyId);
        cvsFilePath.setCreateDate(DateUtils.getNowDate());
        //删除临时文件
        deleteTempFileByEnd(fileUrl);
		return new TestResultData(totalCount,cvsFilePath);
	}
	
	public void deleteTempFileByEnd(String fileUrl) {
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, TxtSuffixEnum.ALL));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, TxtSuffixEnum.REAL));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, TxtSuffixEnum.KONG));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, TxtSuffixEnum.SILENCE));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, TxtSuffixEnum.SHUTDOWN));		
	}
	
	/**
     * 保存部分检测好的结果存到redis用于前端展示
     */
    private void saveDateToRedis(Long customerId,Long emptyId,ListMultimap<MobileReportGroupEnum, String> group){
    	List<MobileColor> resultList = new ArrayList<MobileColor>();
    	FileDetection[] mobileReportGroupEnumList = MobileReportGroupEnum.FileDetection.values();
    	for(FileDetection mrge : mobileReportGroupEnumList) {
    		String color = MobileGroupEnum.getColor(mrge.getGroupCode());
    		List<String> mobileList = group.get(mrge);
    		if(!CollectionUtils.isEmpty(mobileList)) {
    			for(String mobile : mobileList) {
        			resultList.add(new MobileColor(mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"), color));
        		}
    		}
    	}
    	//打乱顺序
    	Collections.sort(resultList, new Comparator<MobileColor>() {
			@Override
			public int compare(MobileColor o1, MobileColor o2) {
				return o1.getMobile().compareTo(o2.getMobile());
			}
		});
    	if(!CollectionUtils.isEmpty(resultList)) {
    		//放入redis缓存
    		redisClient.set(String.format(EmptyRedisKeyConstant.MOBILE_DISPLAY_KEY, customerId,emptyId), JSON.toJSONString(resultList), 60 * 60 * 1000);
    	}    	
    }
	
	/**
     * 数据分组存入文本
     */
    private void saveGroupList(String fileUrl, ListMultimap<MobileReportGroupEnum, String> group) throws IOException {
        //获取实号1组
        saveOneGroupList(getTxtPath(fileUrl, TxtSuffixEnum.REAL), group, MobileReportGroupEnum.FileDetection.REAL);
        //获取空号1组
        saveOneGroupList(getTxtPath(fileUrl, TxtSuffixEnum.KONG), group, MobileReportGroupEnum.FileDetection.EMPTY);
        //沉默号
        saveOneGroupList(getTxtPath(fileUrl, TxtSuffixEnum.SILENCE), group, MobileReportGroupEnum.FileDetection.SILENCE);
        //停机号
        saveOneGroupList(getTxtPath(fileUrl, TxtSuffixEnum.KONG), group, MobileReportGroupEnum.FileDetection.OUT_SERVICE);
        //未知状态 为沉默号
        saveOneGroupList(getTxtPath(fileUrl, TxtSuffixEnum.SILENCE), group, MobileReportGroupEnum.FileDetection.UNKNOWN);
        //关机号
        saveOneGroupList(getTxtPath(fileUrl, TxtSuffixEnum.SHUTDOWN), group, MobileReportGroupEnum.FileDetection.SHUT);
    }
    
    /**
     * 数据分组存入对应的文本
     */
    private void saveOneGroupList(String filePath,
                                  ListMultimap<MobileReportGroupEnum, String> group,
                                  MobileReportGroupEnum.FileDetection oneFileDetectionEnum) throws IOException {
        List<String> oneDelivrdMobileList = group.get(oneFileDetectionEnum);
        TxtFileUtil.saveTxt(oneDelivrdMobileList, filePath, DEFAULT_CHARSET, true);
    }
	
	private void saveDefaultMobileToRedis(Long customerId,Long emptyId,List<String> mobileList){
    	List<MobileColor> resultList = new ArrayList<MobileColor>();
    	for(String mobile: mobileList){
    		String colorString = (Long.parseLong(mobile))%6==0?
    				CommonConstant.TESTPROCESS_MOBILECOLOR_YELLOW:CommonConstant.TESTPROCESS_MOBILECOLOR_BLUE;
    		resultList.add(new MobileColor(mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"), colorString));
    	}
    	//数据保存到redis
    	redisClient.set(String.format(EmptyRedisKeyConstant.DEFAULT_MOBILE_DISPLAY_KEY,customerId, emptyId), JSON.toJSONString(resultList), 60 * 60 * 1000);
    }
	
	private String getTxtPath(String fileUrl, TxtSuffixEnum txtSuffixEnum) {
        return fileUrl + "_" + txtSuffixEnum.getTxtSuffix();
    }
	
	private String getTxtPath(String fileUrl, InternationalTxtSuffixEnum txtSuffixEnum) {
        return fileUrl + "_" + txtSuffixEnum.getTxtSuffix();
    }
}
