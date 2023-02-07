package com.zhongzhi.empty.service.international;

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
import com.zhongzhi.empty.constants.InternationalRedisKeyConstant;
import com.zhongzhi.empty.constants.RealtimeRedisKeyConstant;
import com.zhongzhi.empty.entity.Customer;
import com.zhongzhi.empty.entity.InternationalCvsFilePath;
import com.zhongzhi.empty.entity.InternationalTxtFileContent;
import com.zhongzhi.empty.entity.MobileColor;
import com.zhongzhi.empty.entity.TxtFileContent;
import com.zhongzhi.empty.enums.InternationalMobileReportGroupEnum;
import com.zhongzhi.empty.enums.InternationalMobileReportGroupEnum.FileDetection;
import com.zhongzhi.empty.enums.InternationalTxtSuffixEnum;
import com.zhongzhi.empty.enums.RealtimeTxtSuffixEnum;
import com.zhongzhi.empty.http.international.QueryResponse;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.service.customer.CustomerService;
import com.zhongzhi.empty.service.gateway.InternationalService;
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
 * 国际检测文件处理实现类
 * @author liuh
 * @date 2022年6月9日
 */
@Slf4j
@Service
public class InternationalFileService {

	private final static String DEFAULT_CHARSET = "utf-8";
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private InternationalService internationalService;
	
	@Autowired
	private RedisClient redisClient;
	
	@Autowired
	private Snowflake snowflake;
	
	private final static Integer BIG_DATA_API_REQUEST_SIZE = 2000;
	
	@Value("${international.file.upload.path}")
	private String fileUploadPath;
	
	public TxtFileContent getValidMobileListByInternationalTxt(String fileUrl) {
		File file = new File(fileUrl); 
		if(file.length() <= 1048576 * 6) {
			return getValidMobileListBySmallInternationalTxt(fileUrl);
		}
		
		// 分割文件
		Long tempOrders = snowflake.nextId();
        SplitFileVo splitFileVo = TxtFileUtil.splitInternationalFileByError(file, (int)(((file.length() / (1048576 * 10)) + 1) * 8),tempOrders);
        // 合并文件且计算文件号码数量
    	int fileCount = TxtFileUtil.distinctNew(splitFileVo.getFileList(), getTxtPath(fileUrl, InternationalTxtSuffixEnum.ALL), (int)(((file.length() / (1048576 * 10)) + 1) * 8));
        
    	//获取文件编码格式
    	String fileCode = EncodingDetect.getJavaEncode(fileUrl);
    	TxtFileContent result =  new TxtFileContent();
    	result.setFileCode(fileCode);
        result.setMobileCounts(fileCount);
        result.setErrorCounts(splitFileVo.getErrorCounts());
        result.setMobileList(splitFileVo.getMobileList());
		return result;
	}
	
	public TxtFileContent getValidMobileListBySmallInternationalTxt(String fileUrl) {
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
	                if (!CommonUtils.isNumericByInternational(lineTxt)) {
	                	errorCounts++;
	                    continue;
	                }
	                
	                temeSet.add(lineTxt);
	            }	 
	            
	            //保存全部的号码到缓存
	            tempList = new ArrayList<String>(temeSet);
	            TxtFileUtil.saveTxt(tempList, getTxtPath(fileUrl, InternationalTxtSuffixEnum.ALL), fileCode, false);
	        }
        } catch (UnsupportedEncodingException e) {
			log.error(fileUrl+"----------文件编码格式转换异常：", e);
		} catch (FileNotFoundException e) {
			log.error(fileUrl+"----------文件未找到：", e);
		} catch (IOException e) {
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

        result.setMobileList(tempList);
        result.setMobileCounts(tempList.size());
        result.setErrorCounts(errorCounts);
		return result;
	}
	
	public void saveTempFileByAll(String fileUrl, Long customerId, List<String> mobileList,Long internationalId) throws IOException {
		//保存72个号码以便进度页面显示为空
        saveDefaultMobileToRedis(customerId,internationalId,mobileList.subList(0, mobileList.size() <= 72?mobileList.size():72));
    	//保存全部的号码到缓存
//		TxtFileUtil.saveTxt(mobileList, getTxtPath(fileUrl, InternationalTxtSuffixEnum.ALL), EncodingDetect
//		        .getJavaEncode(fileUrl), false);
		
	}
	
	public void saveTestResultData(Long customerId, String fileUrl,Long internationalId,
			ListMultimap<InternationalMobileReportGroupEnum, String> group) throws IOException {
			saveGroupList(fileUrl,group);
			saveDateToRedis(customerId,internationalId,group);
	}
	
	private void saveDateToRedis(Long customerId,Long internationalId,ListMultimap<InternationalMobileReportGroupEnum, String> group){
    	List<MobileColor> resultList = new ArrayList<MobileColor>();
    	FileDetection[] mobileReportGroupEnumList = InternationalMobileReportGroupEnum.FileDetection.values();
    	for(FileDetection mrge : mobileReportGroupEnumList) {
    		String color = InternationalMobileReportGroupEnum.FileDetection.getColor(mrge.getGroupCode());
    		List<String> mobileList = group.get(mrge);
    		if(!CollectionUtils.isEmpty(mobileList)) {
    			for(String mobile : mobileList) {
        			resultList.add(new MobileColor(mobile, color));
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
    		redisClient.set(String.format(InternationalRedisKeyConstant.MOBILE_DISPLAY_KEY, customerId,internationalId), JSON.toJSONString(resultList), 60 * 60 * 1000);
    	}    	
    }
	
	private void saveGroupList(String fileUrl, ListMultimap<InternationalMobileReportGroupEnum, String> group) throws IOException {        
        //获取激活
        saveOneGroupList(getTxtPath(fileUrl, InternationalTxtSuffixEnum.ACTIVATE), group, InternationalMobileReportGroupEnum.FileDetection.ACTIVATE);
        //获取未激活
        saveOneGroupList(getTxtPath(fileUrl, InternationalTxtSuffixEnum.NOACTIVE), group, InternationalMobileReportGroupEnum.FileDetection.NO_ACTIVE);
        //获取未知
        saveOneGroupList(getTxtPath(fileUrl, InternationalTxtSuffixEnum.UNKNOWN), group, InternationalMobileReportGroupEnum.FileDetection.UNKNOWN);
        //获取未知
        saveOneGroupList(getTxtPath(fileUrl, InternationalTxtSuffixEnum.UNKNOWN), group, InternationalMobileReportGroupEnum.FileDetection.NO_RESULT);
    }
    
    /**
     * 数据分组存入对应的文本
     */
    private void saveOneGroupList(String filePath,
                                  ListMultimap<InternationalMobileReportGroupEnum, String> group,
                                  InternationalMobileReportGroupEnum.FileDetection oneFileDetectionEnum) throws IOException {
        List<String> oneDelivrdMobileList = group.get(oneFileDetectionEnum);
        TxtFileUtil.saveTxt(oneDelivrdMobileList, filePath, DEFAULT_CHARSET, true);
    }
	
	private void saveDefaultMobileToRedis(Long customerId,Long internationalId,List<String> mobileList){
    	List<MobileColor> resultList = new ArrayList<MobileColor>();
    	for(String mobile: mobileList){
    		String colorString = (Long.parseLong(mobile))%6==0?
    				CommonConstant.TESTPROCESS_MOBILECOLOR_YELLOW:CommonConstant.TESTPROCESS_MOBILECOLOR_BLUE;
    		resultList.add(new MobileColor(mobile, colorString));
    	}
    	//数据保存到redis
    	redisClient.set(String.format(InternationalRedisKeyConstant.DEFAULT_MOBILE_DISPLAY_KEY,customerId, internationalId), JSON.toJSONString(resultList), 60 * 60 * 1000);
    }
	
	public void saveMobileToTxtFile(String fileUrl, List<String> mobileList, InternationalTxtSuffixEnum txtSuffixEnum) throws IOException {
		TxtFileUtil.saveTxt(mobileList, getTxtPath(fileUrl, txtSuffixEnum), DEFAULT_CHARSET, true);
	}
	
	public List<String> readTxtFileContent(String fileUrl, String fileEncoding, int fromIndex) throws IOException {
		return TxtFileUtil.readTxt(getTxtPath(fileUrl, InternationalTxtSuffixEnum.ALL), fileEncoding, fromIndex, BIG_DATA_API_REQUEST_SIZE);
	}
	
	private String getTxtPath(String fileUrl, InternationalTxtSuffixEnum txtSuffixEnum) {
        return fileUrl + "_" + txtSuffixEnum.getTxtSuffix();
    }
	
	public InternationalCvsFilePath getTestResultByTxtFile(String fileUrl,  Long customerId,Long internationalId,String sourceFileName) throws Exception{
		InternationalCvsFilePath internationalCvsFilePath = new InternationalCvsFilePath();
		internationalCvsFilePath.setCustomerId(customerId);
		internationalCvsFilePath.setInternationalId(internationalId);
		
		String subFilePath = DateUtils.getDate() + "/" + customerId + "/" + internationalId + "/";
        String filePath = fileUploadPath + subFilePath;
        
        List<String> list = new ArrayList<String>();
        int totalCount = 0;    		
        //生成激活报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, InternationalTxtSuffixEnum.ACTIVATE), filePath + CommonConstant.ACTIVE_IN_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countActivate = TxtFileUtil.countLines(filePath + CommonConstant.ACTIVE_IN_FILE_NAME, DEFAULT_CHARSET);
        if (countActivate > 0) {
            log.info("----------激活总条数：" + countActivate);
            totalCount += countActivate;
            list.add(filePath + CommonConstant.ACTIVE_IN_FILE_NAME);
            internationalCvsFilePath.setActiveNumber(countActivate);
            internationalCvsFilePath.setActiveFilePath(subFilePath + CommonConstant.ACTIVE_IN_FILE_NAME);
            internationalCvsFilePath.setActiveFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.ACTIVE_IN_FILE_NAME))));
        }
        // 生成未激活报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, InternationalTxtSuffixEnum.NOACTIVE), filePath + CommonConstant.NO_ACTIVE_IN_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countNoActive = TxtFileUtil.countLines(filePath + CommonConstant.NO_ACTIVE_IN_FILE_NAME, DEFAULT_CHARSET);
        if (countNoActive > 0) {
            log.info("----------未激活总条数：" + countNoActive);
            totalCount += countNoActive;
            list.add(filePath + CommonConstant.NO_ACTIVE_IN_FILE_NAME);
            internationalCvsFilePath.setNoRegisterNumber(countNoActive);
            internationalCvsFilePath.setNoRegisterFilePath(subFilePath + CommonConstant.NO_ACTIVE_IN_FILE_NAME);
            internationalCvsFilePath.setNoRegisterFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.NO_ACTIVE_IN_FILE_NAME))));
        }
        
        // 生成未知报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, InternationalTxtSuffixEnum.UNKNOWN), filePath + CommonConstant.NO_REGISTER_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countUnknown = TxtFileUtil.countLines(filePath + CommonConstant.NO_REGISTER_FILE_NAME, DEFAULT_CHARSET);
        if (countUnknown > 0) {
            log.info("----------未知总条数：" + countUnknown);
            totalCount += countUnknown;
            list.add(filePath + CommonConstant.NO_REGISTER_FILE_NAME);
            internationalCvsFilePath.setUnknownNumber(countUnknown);
            internationalCvsFilePath.setUnknownFilePath(subFilePath + CommonConstant.NO_REGISTER_FILE_NAME);
            internationalCvsFilePath.setUnknownFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.NO_REGISTER_FILE_NAME))));
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
            
            internationalCvsFilePath.setZipName(zipName);
            internationalCvsFilePath.setZipPath((subFilePath + zipName));
            internationalCvsFilePath.setZipSize(String.valueOf(FileUtil.getFileSize(new File(filePath + zipName))));
            internationalCvsFilePath.setTotalNumber(totalCount);
        }

        //删除临时文件
        deleteTempFileByEnd(fileUrl);
		return internationalCvsFilePath;
	}
	
	public void deleteTempFileByEnd(String fileUrl) {
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, InternationalTxtSuffixEnum.ALL));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, InternationalTxtSuffixEnum.ACTIVATE));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, InternationalTxtSuffixEnum.NOACTIVE));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, InternationalTxtSuffixEnum.UNKNOWN));
	}
}
