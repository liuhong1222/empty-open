package com.zhongzhi.empty.service.direct;

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
import com.zhongzhi.empty.entity.Customer;
import com.zhongzhi.empty.entity.IntDirectCvsFilePath;
import com.zhongzhi.empty.entity.InternationalCvsFilePath;
import com.zhongzhi.empty.entity.MobileColor;
import com.zhongzhi.empty.entity.TxtFileContent;
import com.zhongzhi.empty.enums.IntDirectTxtSuffixEnum;
import com.zhongzhi.empty.enums.InternationalMobileReportGroupEnum;
import com.zhongzhi.empty.enums.InternationalMobileReportGroupEnum.FileDetection;
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
 * 定向国际检测文件处理实现类
 * @author liuh
 * @date 2022年10月18日
 */
@Slf4j
@Service
public class IntDirectFileService {

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
	
	@Value("${direct.file.upload.path}")
	private String fileUploadPath;
	
	public TxtFileContent getValidMobileListByIntDirectTxt(String fileUrl, String countryCode) {
		File file = new File(fileUrl); 
		if(file.length() <= 1048576 * 6) {
			return getValidMobileListBySmallIntDirectTxt(fileUrl,countryCode);
		}
		
		// 分割文件
		Long tempOrders = snowflake.nextId();
        SplitFileVo splitFileVo = TxtFileUtil.splitIntDirectFileByError(file, (int)(((file.length() / (1048576 * 10)) + 1) * 8),tempOrders,countryCode);
        // 合并文件且计算文件号码数量
    	int fileCount = TxtFileUtil.distinctNew(splitFileVo.getFileList(), getTxtPath(fileUrl, IntDirectTxtSuffixEnum.ALL), (int)(((file.length() / (1048576 * 10)) + 1) * 8));
        
    	//获取文件编码格式
    	String fileCode = EncodingDetect.getJavaEncode(fileUrl);
    	TxtFileContent result =  new TxtFileContent();
    	result.setFileCode(fileCode);
        result.setMobileCounts(fileCount);
        result.setErrorCounts(splitFileVo.getErrorCounts());
        result.setMobileList(splitFileVo.getMobileList());
		return result;
	}
	
	public TxtFileContent getValidMobileListBySmallIntDirectTxt(String fileUrl, String countryCode) {
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
	                // 验证是否为正常的有效数字
	                if (!CommonUtils.isNumericByInternational(lineTxt)) {
	                	errorCounts++;
	                    continue;
	                }
	                
	                if("91".equals(countryCode)) {
	                	if(lineTxt.length() < 12) {
	                		lineTxt = countryCode + lineTxt;
	                	}
	                }else {
	                	if(!lineTxt.startsWith(countryCode)) {
	                		lineTxt = countryCode + lineTxt;
	                	}
	                }
	                
	                temeSet.add(lineTxt);
	            }	 
	            
	            //保存全部的号码到缓存
	            tempList = new ArrayList<String>(temeSet);
	            TxtFileUtil.saveTxt(tempList, getTxtPath(fileUrl, IntDirectTxtSuffixEnum.ALL), fileCode, false);
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
//		TxtFileUtil.saveTxt(mobileList, getTxtPath(fileUrl, IntDirectTxtSuffixEnum.ALL), EncodingDetect
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
        saveOneGroupList(getTxtPath(fileUrl, IntDirectTxtSuffixEnum.ACTIVATE), group, InternationalMobileReportGroupEnum.FileDetection.ACTIVATE);
        //获取未激活
        saveOneGroupList(getTxtPath(fileUrl, IntDirectTxtSuffixEnum.NOREGISTER), group, InternationalMobileReportGroupEnum.FileDetection.NO_ACTIVE);
        //获取未知
        saveOneGroupList(getTxtPath(fileUrl, IntDirectTxtSuffixEnum.NOREGISTER), group, InternationalMobileReportGroupEnum.FileDetection.NO_RESULT);
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
	
	public void saveMobileToTxtFile(String fileUrl, List<String> mobileList, IntDirectTxtSuffixEnum txtSuffixEnum) throws IOException {
		TxtFileUtil.saveTxt(mobileList, getTxtPath(fileUrl, txtSuffixEnum), DEFAULT_CHARSET, true);
	}
	
	public List<String> readTxtFileContent(String fileUrl, String fileEncoding, int fromIndex) throws IOException {
		return TxtFileUtil.readTxt(getTxtPath(fileUrl, IntDirectTxtSuffixEnum.ALL), fileEncoding, fromIndex, BIG_DATA_API_REQUEST_SIZE);
	}
	
	public String getTxtPath(String fileUrl, IntDirectTxtSuffixEnum txtSuffixEnum) {
        return fileUrl + "_" + txtSuffixEnum.getTxtSuffix();
    }
	
	public IntDirectCvsFilePath getTestResultByTxtFile(QueryResponse queryResponse, ProgressTaskInfo progressTaskInfo) throws Exception{
		IntDirectCvsFilePath intDirectCvsFilePath = new IntDirectCvsFilePath();
		intDirectCvsFilePath.setCustomerId(progressTaskInfo.getCustomerId());
		intDirectCvsFilePath.setIntDirectId(progressTaskInfo.getIntDirectId());
		intDirectCvsFilePath.setCreateDate(DateUtils.getNowDate1());
		intDirectCvsFilePath.setCountryCode(progressTaskInfo.getCountryCode());
		intDirectCvsFilePath.setProductType(progressTaskInfo.getProductType());
		
		String subFilePath = DateUtils.getDate() + "/" + progressTaskInfo.getCustomerId() + "/" + progressTaskInfo.getIntDirectId() + "/";
        String filePath = fileUploadPath + subFilePath;
        int totalCount = 0;
        
        List<String> list = new ArrayList<String>();
        // 调用上游接口下载已激活文件包
		Boolean flag = internationalService.download(progressTaskInfo.getCustomerId(), progressTaskInfo.getExternFileId(), "2", filePath + CommonConstant.ACTIVE_IN_FILE_NAME);
		if(!flag) {
			log.error("{}, 调用上游接口下载已激活包失败，intDirectId:{},sendID:{}",progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId());
			dingDingMessage.sendMessage(String.format("警告：%s, 调用上游接口下载已激活包失败，intDirectId:%s,sendID:%s", progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId()));
			return null;
		}
		
		int countActivate = TxtFileUtil.countLines(filePath + CommonConstant.ACTIVE_IN_FILE_NAME, DEFAULT_CHARSET);
		log.info("----------已激活总条数：" + countActivate);
		intDirectCvsFilePath.setActiveFilePath(subFilePath + CommonConstant.ACTIVE_IN_FILE_NAME);
		intDirectCvsFilePath.setActiveFileSize(String.valueOf(new File(filePath + CommonConstant.ACTIVE_IN_FILE_NAME).length()));
		intDirectCvsFilePath.setActiveNumber(countActivate);
		list.add(filePath + CommonConstant.ACTIVE_IN_FILE_NAME);
		totalCount = totalCount + countActivate;
        
		// 调用上游接口下载未注册文件包
		flag = internationalService.download(progressTaskInfo.getCustomerId(), progressTaskInfo.getExternFileId(), "3", filePath + CommonConstant.NO_REGISTER_FILE_NAME);
		if(!flag) {
			log.error("{}, 调用上游接口下载未注册包失败，intDirectId:{},sendID:{}",progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId());
			dingDingMessage.sendMessage(String.format("警告：%s, 调用上游接口下载未注册包失败，intDirectId:%s,sendID:%s", progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId()));
			return null;
		}

		int countNoRister = TxtFileUtil.countLines(filePath + CommonConstant.NO_REGISTER_FILE_NAME, DEFAULT_CHARSET);
		log.info("----------未注册总条数：" + countNoRister);
		intDirectCvsFilePath.setNoRegisterFilePath(subFilePath + CommonConstant.NO_REGISTER_FILE_NAME);
		intDirectCvsFilePath.setNoRegisterFileSize(String.valueOf(new File(filePath + CommonConstant.NO_REGISTER_FILE_NAME).length()));
		intDirectCvsFilePath.setNoRegisterNumber(countNoRister);
		list.add(filePath + CommonConstant.NO_REGISTER_FILE_NAME);
		totalCount = totalCount + countNoRister;
		        
        // 报表文件打包
        if (!CollectionUtils.isEmpty(list)) {
            String zipName = progressTaskInfo.getSourceFileName() + ".zip";
            //获取压缩包加密的密码
            Customer customer = customerService.getCustomerById(progressTaskInfo.getCustomerId());
            if (customer == null || StringUtils.isBlank(customer.getUnzipPassword())) {
                ZipUtil.zip(list.toArray(new String[list.size()]), filePath+zipName);
            } else {
                ZipUtil.zip(list.toArray(new String[list.size()]), filePath+zipName, customer.getUnzipPassword());
            }
            
            intDirectCvsFilePath.setZipName(zipName);
            intDirectCvsFilePath.setZipPath((subFilePath + zipName));
            intDirectCvsFilePath.setZipSize(String.valueOf(FileUtil.getFileSize(new File(filePath + zipName))));
            intDirectCvsFilePath.setTotalNumber(totalCount);
        }

        // 删除临时文件
        deleteTempFileByEnd(progressTaskInfo.getFileUrl());
		return intDirectCvsFilePath;
	}
	
	public void deleteTempFileByEnd(String fileUrl) {
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, IntDirectTxtSuffixEnum.ALL));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, IntDirectTxtSuffixEnum.ACTIVATE));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, IntDirectTxtSuffixEnum.NOREGISTER));
	}
}
