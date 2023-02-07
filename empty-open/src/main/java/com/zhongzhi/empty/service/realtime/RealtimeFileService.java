package com.zhongzhi.empty.service.realtime;

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
import com.zhongzhi.empty.constants.RealtimeRedisKeyConstant;
import com.zhongzhi.empty.entity.Agent;
import com.zhongzhi.empty.entity.Customer;
import com.zhongzhi.empty.entity.MobileColor;
import com.zhongzhi.empty.entity.RealtimeCvsFilePath;
import com.zhongzhi.empty.entity.RealtimeResultData;
import com.zhongzhi.empty.entity.TxtFileContent;
import com.zhongzhi.empty.enums.RealtimeMobileGroupEnum;
import com.zhongzhi.empty.enums.RealtimeReportGroupEnum;
import com.zhongzhi.empty.enums.RealtimeReportGroupEnum.FileDetection;
import com.zhongzhi.empty.enums.RealtimeResultEnum;
import com.zhongzhi.empty.enums.RealtimeTxtSuffixEnum;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.RealtimeResult;
import com.zhongzhi.empty.service.AgentService;
import com.zhongzhi.empty.service.customer.CustomerService;
import com.zhongzhi.empty.util.CommonUtils;
import com.zhongzhi.empty.util.DateUtils;
import com.zhongzhi.empty.util.DingDingMessage;
import com.zhongzhi.empty.util.EncodingDetect;
import com.zhongzhi.empty.util.FileUtil;
import com.zhongzhi.empty.util.ThreadLocalContainer;
import com.zhongzhi.empty.util.TxtFileUtil;
import com.zhongzhi.empty.util.ZipUtil;
import com.zhongzhi.empty.vo.CustomerInfoVo;

import lombok.extern.slf4j.Slf4j;

/**
 * 实时检测文件处理实现类
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
@Service
public class RealtimeFileService {

	private final static String DEFAULT_CHARSET = "utf-8";
	
	private final static Integer BIG_DATA_API_REQUEST_SIZE = 2000;
	
	@Autowired
	private RedisClient redisClient;
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private AgentService agentService;
	
	@Autowired
	private RealtimeApiService realtimeApiService;
	
	@Value("${realtime.file.upload.path}")
	private String fileUploadPath;
	
	@Value("${realtime.file.resource.path}")
	private String fileResourcePath;
	
	public TxtFileContent getValidMobileListByTxt(String fileUrl) {
		TxtFileContent result =  new TxtFileContent();
		List<String> mobileList = new ArrayList<String>();
		HashSet<String> temeSet = new HashSet<String>();
        File file = new File(fileUrl);        
        BufferedReader br = null;
        Integer errorCounts = 0;
        try {
	        if (file.isFile() && file.exists()) {
	        	//获取文件编码格式
	        	String fileCode = EncodingDetect.getJavaEncode(fileUrl);
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
        
        mobileList.addAll(temeSet);
        temeSet.clear();
        result.setMobileList(mobileList);
        result.setErrorCounts(errorCounts);
		return result;
	}
	
	public void saveTempFileByAll(String fileUrl, Long customerId, List<String> mobileList,Long emptyId) throws IOException {
		//保存72个号码以便进度页面显示为空
        saveDefaultMobileToRedis(customerId,emptyId,mobileList.subList(0, mobileList.size() <= 72?mobileList.size():72));
    	//保存全部的号码到缓存
		TxtFileUtil.saveTxt(mobileList, getTxtPath(fileUrl, RealtimeTxtSuffixEnum.ALL), EncodingDetect
		        .getJavaEncode(fileUrl), false);
		
	}
	
	public void saveTestResultData(Long customerId, String fileUrl,Long emptyId,
			ListMultimap<RealtimeReportGroupEnum, String> group) throws IOException {
			saveGroupList(fileUrl,group);
			saveDateToRedis(customerId,emptyId,group);
	}
	
	public void saveMobileToTxtFile(String fileUrl, List<String> mobileList, RealtimeTxtSuffixEnum txtSuffixEnum) throws IOException {
		TxtFileUtil.saveTxt(mobileList, getTxtPath(fileUrl, txtSuffixEnum), DEFAULT_CHARSET, true);
	}
	
	public List<String> readTxtFileContent(String fileUrl, String fileEncoding, int fromIndex) throws IOException {
		return TxtFileUtil.readTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.ALL), fileEncoding, fromIndex, BIG_DATA_API_REQUEST_SIZE);
	}
	
	public RealtimeResultData getTestResultByTxtFile(String fileUrl, Long customerId,Long realtimeId,String sourceFileName) throws Exception {
		RealtimeCvsFilePath realtimeCvsFilePath = new RealtimeCvsFilePath();
		int totalCount = 0;    		
        List<String> list = new ArrayList<String>();
        String subFilePath = DateUtils.getDate() + "/" + customerId + "/" + realtimeId + "/";
        String filePath = fileUploadPath + subFilePath;
        //生成正常报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.NORMAL), filePath + CommonConstant.NORMAL_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countNormal = TxtFileUtil.countLines(filePath + CommonConstant.NORMAL_FILE_NAME, DEFAULT_CHARSET);
        if (countNormal > 0) {
            log.info("----------正常总条数：" + countNormal);
            totalCount += countNormal;
            list.add(filePath + CommonConstant.NORMAL_FILE_NAME);
            realtimeCvsFilePath.setNormalNumber(countNormal);
            realtimeCvsFilePath.setNormalFilePath(subFilePath + CommonConstant.NORMAL_FILE_NAME);
            realtimeCvsFilePath.setNormalFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.NORMAL_FILE_NAME))));
        }
        // 生成空号报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.EMPTY), filePath + CommonConstant.REALTIME_EMPTY_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countEmpty = TxtFileUtil.countLines(filePath + CommonConstant.REALTIME_EMPTY_FILE_NAME, DEFAULT_CHARSET);
        if (countEmpty > 0) {
            log.info("----------空号总条数：" + countEmpty);
            totalCount += countEmpty;
            list.add(filePath + CommonConstant.REALTIME_EMPTY_FILE_NAME);
            realtimeCvsFilePath.setEmptyNumber(countEmpty);
            realtimeCvsFilePath.setEmptyFilePath(subFilePath + CommonConstant.REALTIME_EMPTY_FILE_NAME);
            realtimeCvsFilePath.setEmptyFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.REALTIME_EMPTY_FILE_NAME))));
        }
        // 生成通话中报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.ON_CALL), filePath + CommonConstant.ONCALL_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countOncall = TxtFileUtil.countLines(filePath + CommonConstant.ONCALL_FILE_NAME, DEFAULT_CHARSET);
        if (countOncall > 0) {
            log.info("----------通话中总条数：" + countOncall);
            totalCount += countOncall;
            list.add(filePath + CommonConstant.ONCALL_FILE_NAME);
            realtimeCvsFilePath.setOncallNumber(countOncall);
            realtimeCvsFilePath.setOncallFilePath(subFilePath + CommonConstant.ONCALL_FILE_NAME);
            realtimeCvsFilePath.setOncallFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.ONCALL_FILE_NAME))));
        }
        // 生成不在网报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.NOT_ONLINE), filePath + CommonConstant.NOT_ONLINE_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countNotOnline = TxtFileUtil.countLines(filePath + CommonConstant.NOT_ONLINE_FILE_NAME, DEFAULT_CHARSET);
        if (countNotOnline > 0) {
            log.info("----------不在网总条数：" + countNotOnline);
            totalCount += countNotOnline;
            list.add(filePath + CommonConstant.NOT_ONLINE_FILE_NAME);
            realtimeCvsFilePath.setNotOnlineNumber(countNotOnline);
            realtimeCvsFilePath.setNotOnlineFilePath(subFilePath + CommonConstant.NOT_ONLINE_FILE_NAME);
            realtimeCvsFilePath.setNotOnlineFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.NOT_ONLINE_FILE_NAME))));
        }
        // 生成关机报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.GUANJI), filePath + CommonConstant.SHUTDOWN_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countShutdown = TxtFileUtil.countLines(filePath + CommonConstant.SHUTDOWN_FILE_NAME, DEFAULT_CHARSET);
        if (countShutdown > 0) {
            log.info("----------关机总条数：" + countShutdown);
            totalCount += countShutdown;
            list.add(filePath + CommonConstant.SHUTDOWN_FILE_NAME);
            realtimeCvsFilePath.setShutdownNumber(countShutdown);
            realtimeCvsFilePath.setShutdownFilePath(subFilePath + CommonConstant.SHUTDOWN_FILE_NAME);
            realtimeCvsFilePath.setShutdownFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.SHUTDOWN_FILE_NAME))));
        }
        // 生成疑似关机报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.LIKE_GUANJI), filePath + CommonConstant.LIKE_SHUTDOWN_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countLikeShutdown = TxtFileUtil.countLines(filePath + CommonConstant.LIKE_SHUTDOWN_FILE_NAME, DEFAULT_CHARSET);
        if (countLikeShutdown > 0) {
            log.info("----------疑似关机总条数：" + countLikeShutdown);
            totalCount += countLikeShutdown;
            list.add(filePath + CommonConstant.LIKE_SHUTDOWN_FILE_NAME);
            realtimeCvsFilePath.setLikeShutdownNumber(countLikeShutdown);
            realtimeCvsFilePath.setLikeShutdownFilePath(subFilePath + CommonConstant.LIKE_SHUTDOWN_FILE_NAME);
            realtimeCvsFilePath.setLikeShutdownFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.LIKE_SHUTDOWN_FILE_NAME))));
        }
        // 生成停机报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.TINGJI), filePath + CommonConstant.TINGJI_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countTingji = TxtFileUtil.countLines(filePath + CommonConstant.TINGJI_FILE_NAME, DEFAULT_CHARSET);
        if (countTingji > 0) {
            log.info("----------停机总条数：" + countTingji);
            totalCount += countTingji;
            list.add(filePath + CommonConstant.TINGJI_FILE_NAME);
            realtimeCvsFilePath.setTingjiNumber(countTingji);
            realtimeCvsFilePath.setTingjiFilePath(subFilePath + CommonConstant.TINGJI_FILE_NAME);
            realtimeCvsFilePath.setTingjiFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.TINGJI_FILE_NAME))));
        }
        // 生成携号转网报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.MNP), filePath + CommonConstant.MNP_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countMnp = TxtFileUtil.countLines(filePath + CommonConstant.MNP_FILE_NAME, DEFAULT_CHARSET);
        if (countMnp > 0) {
            log.info("----------携号转网总条数：" + countMnp);
            totalCount += countMnp;
            list.add(filePath + CommonConstant.MNP_FILE_NAME);
            realtimeCvsFilePath.setMnpNumber(countMnp);
            realtimeCvsFilePath.setMnpFilePath(subFilePath + CommonConstant.MNP_FILE_NAME);
            realtimeCvsFilePath.setMnpFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.MNP_FILE_NAME))));
        }
        // 生成号码错误报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.MOBILE_ERROR), filePath + CommonConstant.MOBILE_ERROR_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countMoberr = TxtFileUtil.countLines(filePath + CommonConstant.MOBILE_ERROR_FILE_NAME, DEFAULT_CHARSET);
        if (countMoberr > 0) {
            log.info("----------号码错误总条数：" + countMoberr);
            totalCount += countMoberr;
            list.add(filePath + CommonConstant.MOBILE_ERROR_FILE_NAME);
            realtimeCvsFilePath.setMoberrNumber(countMoberr);
            realtimeCvsFilePath.setMoberrFilePath(subFilePath + CommonConstant.MOBILE_ERROR_FILE_NAME);
            realtimeCvsFilePath.setMoberrFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.MOBILE_ERROR_FILE_NAME))));
        }
        // 生成未知报表
        TxtFileUtil.appendTxt(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.UNKNOWN), filePath + CommonConstant.UNKNOWN_FILE_NAME, filePath, DEFAULT_CHARSET, true);
        int countUnknown = TxtFileUtil.countLines(filePath + CommonConstant.UNKNOWN_FILE_NAME, DEFAULT_CHARSET);
        if (countUnknown > 0) {
            log.info("----------未知总条数：" + countUnknown);
            totalCount += countUnknown;
            list.add(filePath + CommonConstant.UNKNOWN_FILE_NAME);
            realtimeCvsFilePath.setUnknownNumber(countUnknown);
            realtimeCvsFilePath.setUnknownFilePath(subFilePath + CommonConstant.UNKNOWN_FILE_NAME);
            realtimeCvsFilePath.setUnknownFileSize(String.valueOf(FileUtil.getFileSize(new File(filePath + CommonConstant.UNKNOWN_FILE_NAME))));
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
            
            realtimeCvsFilePath.setZipName(zipName);
            realtimeCvsFilePath.setZipPath((subFilePath + zipName));
            realtimeCvsFilePath.setZipSize(String.valueOf(FileUtil.getFileSize(new File(filePath + zipName))));
            realtimeCvsFilePath.setTotalNumber(totalCount);
        }
        
        realtimeCvsFilePath.setCustomerId(customerId);
        realtimeCvsFilePath.setCreateTime(new Date());
        realtimeCvsFilePath.setRealtimeId(realtimeId);
        realtimeCvsFilePath.setCreateDate(DateUtils.getNowDate());
        //删除临时文件
        deleteTempFileByEnd(fileUrl);
		return new RealtimeResultData(totalCount,realtimeCvsFilePath);
	}
	
	public void deleteTempFileByEnd(String fileUrl) {
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.ALL));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.NORMAL));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.EMPTY));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.NOT_ONLINE));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.ON_CALL));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.GUANJI));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.LIKE_GUANJI));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.TINGJI));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.MNP));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.MOBILE_ERROR));
		TxtFileUtil.deleteFile(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.UNKNOWN));
	}
	
	public ApiResult realtimeCheckBySingle(Long customerId,String mobile,String ip) {
		// 查询用户信息
		Customer customer = customerService.getCustomerById(customerId);
		if(customer == null) {
			return ApiResult.fail("用户不存在");
		}
		
		// 查询代理商信息
		Agent agent = agentService.getAgentById(customer.getAgentId());
		if(agent == null) {
			return ApiResult.fail("所属代理商不存在或已被禁用");
		}
		
		CustomerInfoVo customerInfoVo = new CustomerInfoVo();
		customerInfoVo.setAgentId(agent.getId());
		customerInfoVo.setCompanyName(agent.getCompanyName());
		customerInfoVo.setCustomerId(customerId);
		customerInfoVo.setCustomerName(customer.getName());
		customerInfoVo.setPhone(mobile);
		ThreadLocalContainer.setCustomerInfoVo(customerInfoVo);
		
		ApiResult<RealtimeResult> result = realtimeApiService.mobileStatusStaticQueryNew(mobile, ip);
		if(result == null) {
			return ApiResult.fail("接口调用失败");
		}
		
		if(!result.isSuccess()) {
			return result;
		}
		
		return ApiResult.ok(RealtimeResultEnum.getDesc(result.getData().getStatus()));
	}
	
	/**
     * 保存部分检测好的结果存到redis用于前端展示
     */
    private void saveDateToRedis(Long customerId,Long realtimeId,ListMultimap<RealtimeReportGroupEnum, String> group){
    	List<MobileColor> resultList = new ArrayList<MobileColor>();
    	FileDetection[] mobileReportGroupEnumList = RealtimeReportGroupEnum.FileDetection.values();
    	for(FileDetection mrge : mobileReportGroupEnumList) {
    		String color = RealtimeMobileGroupEnum.getColor(mrge.getGroupCode());
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
    		redisClient.set(String.format(RealtimeRedisKeyConstant.MOBILE_DISPLAY_KEY, customerId,realtimeId), JSON.toJSONString(resultList), 60 * 60 * 1000);
    	}    	
    }
	
	/**
     * 数据分组存入文本
     */
    private void saveGroupList(String fileUrl, ListMultimap<RealtimeReportGroupEnum, String> group) throws IOException {
        //正常
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.NORMAL), group, RealtimeReportGroupEnum.FileDetection.NORMAL);
        //空号
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.EMPTY), group, RealtimeReportGroupEnum.FileDetection.EMPTY);
        //通话中
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.ON_CALL), group, RealtimeReportGroupEnum.FileDetection.ON_CALL);
        //不在网
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.NOT_ONLINE), group, RealtimeReportGroupEnum.FileDetection.NOT_ONLINE);
        //关机
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.GUANJI), group, RealtimeReportGroupEnum.FileDetection.GUANJI);
        //疑似关机
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.LIKE_GUANJI), group, RealtimeReportGroupEnum.FileDetection.LIKE_GUANJI);
        //停机
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.TINGJI), group, RealtimeReportGroupEnum.FileDetection.TINGJI);
        //携号转网
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.MNP), group, RealtimeReportGroupEnum.FileDetection.MNP);
        //号码错误
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.MOBILE_ERROR), group, RealtimeReportGroupEnum.FileDetection.MOBILE_ERROR);
        //未知
        saveOneGroupList(getTxtPath(fileUrl, RealtimeTxtSuffixEnum.UNKNOWN), group, RealtimeReportGroupEnum.FileDetection.UNKNOWN);
    }
    
    /**
     * 数据分组存入对应的文本
     */
    private void saveOneGroupList(String filePath,
                                  ListMultimap<RealtimeReportGroupEnum, String> group,
                                  RealtimeReportGroupEnum.FileDetection oneFileDetectionEnum) throws IOException {
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
    	redisClient.set(String.format(RealtimeRedisKeyConstant.DEFAULT_MOBILE_DISPLAY_KEY,customerId, emptyId), JSON.toJSONString(resultList), 60 * 60 * 1000);
    }
	
	private String getTxtPath(String fileUrl, RealtimeTxtSuffixEnum txtSuffixEnum) {
        return fileUrl + "_" + txtSuffixEnum.getTxtSuffix();
    }
}
