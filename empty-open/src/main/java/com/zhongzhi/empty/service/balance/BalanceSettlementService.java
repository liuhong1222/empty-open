package com.zhongzhi.empty.service.balance;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.util.StringUtil;
import com.zhongzhi.empty.constants.RedisKeyConstant;
import com.zhongzhi.empty.entity.BalanceFlow;
import com.zhongzhi.empty.entity.CustomerBalance;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.enums.OptTypeEnum;
import com.zhongzhi.empty.enums.ProductEnum;
import com.zhongzhi.empty.exception.BusinessException;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.service.customer.CustomerBalanceService;
import com.zhongzhi.empty.util.CommonUtils;
import com.zhongzhi.empty.util.DateUtils;
import com.zhongzhi.empty.util.DingDingMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消耗数据库余额结算实现类
 * @author liuh
 * @date 2021年11月9日
 */
@Slf4j
@Service
public class BalanceSettlementService {
    
    private ThreadPoolTaskScheduler threadPoolTaskScheduler = null;
	
	@Value("${task.customer.balance.mysql.settlement.cron}")
	private String customerBalanceSettlementCron;
	
	@Value("${verify.task.open}")
	private boolean verifyTaskOpen;
	
	@Autowired
	private BalanceService balanceService;
	
	@Autowired
	private RedisClient redisClient;
	
	@Autowired
	private DingDingMessage dingdingMessage;
	
	@Autowired
	private CustomerBalanceService customerBalanceService;
	
	@Autowired
	private BalanceFlowService balanceFlowService;

    private final static int poolSize = 5;
    
    @PostConstruct
	private void start() {
		if(verifyTaskOpen) {
			threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
			threadPoolTaskScheduler.setThreadNamePrefix("balance-settlement-");
			threadPoolTaskScheduler.setPoolSize(2);
			threadPoolTaskScheduler.initialize();
			threadPoolTaskScheduler.schedule(() -> {
				log.info("schedule task sync customer's balance mysql settlement starting...");
				doMysqlSettlementHandler();
				log.info("schedule task sync customer's balance mysql settlement ending...");
			}, new CronTrigger(customerBalanceSettlementCron));
		}
	}

	@PreDestroy
	private void stop() {
		threadPoolTaskScheduler.shutdown();
	}

    private void doMysqlSettlementHandler(){
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        //获取账号集合
        Set<String> customerIdList = getCustomerIdList();
        if(!CollectionUtils.isEmpty(customerIdList)){
            //批量提交任务
            for(String customerId:customerIdList){
            	if(StringUtil.isNotEmpty(customerId) && !"null".equals(customerId)) {
                    pool.submit(new Runnable() {
						
						@Override
						public void run() {
							subBalance(Long.valueOf(customerId));
						}
					});
            	}
            }
        }

        pool.shutdown();
    }
    
    @Transactional
    private boolean subBalance(Long customerId) {
    	try {
    		//空号余额流水变更记录
    		BalanceFlow emptyRecord = new BalanceFlow();
    		//实时余额流水变更记录
    		BalanceFlow realtimeRecord = new BalanceFlow();
    		//国际余额流水变更记录
    		BalanceFlow internationalRecord = new BalanceFlow();
    		
    		//定向通用余额流水变更记录
    		BalanceFlow directCommonRecord = new BalanceFlow();
    		//line定向余额流水变更记录
    		BalanceFlow lineDirectRecord = new BalanceFlow();
    		
    		//获取当前余额
        	CustomerBalance customerBalance = customerBalanceService.findOneByCustomerIdForUpdate(customerId);
            if(customerBalance == null) {
                log.error("{}，balance settlement failed, this customer is not balance record", customerId);
                dingdingMessage.sendMessage(String.format("警告：%s, balance settlement failed, this customer is not balance record。", customerId));
                return false;
            }
            
            //获取redis中的金额
            Object object = balanceService.getCustomerRedisData(customerId);
            if(ObjectUtils.isEmpty(object)) {
            	return true;
            }
            
            //各产品的实际扣款金额都为0或者为空的话则不记录该账户本次流水记录
            List<Object> cacheMoney = (List<Object>)object;
    		if(!CollectionUtils.isEmpty(cacheMoney) && 
            		((ObjectUtils.isEmpty(cacheMoney.get(1)) || "0".equals(cacheMoney.get(1))) && 
    				(ObjectUtils.isEmpty(cacheMoney.get(4)) || "0".equals(cacheMoney.get(4))) && 
    				(ObjectUtils.isEmpty(cacheMoney.get(7)) || "0".equals(cacheMoney.get(7))) && 
    				(ObjectUtils.isEmpty(cacheMoney.get(10)) || "0".equals(cacheMoney.get(10))) && 
    				(ObjectUtils.isEmpty(cacheMoney.get(13)) || "0".equals(cacheMoney.get(13)))
    				)) {
            	return true;
            }
    		
    		Long currTime = Long.valueOf(DateUtils.formatDate(
            		new Date(CommonUtils.stringObjectToLong2(cacheMoney.get(15))/1000),"yyyyMMddHHmmssSSS"));
    		emptyRecord.setCustomerId(customerId);
    		emptyRecord.setFreezdMoney(CommonUtils.stringObjectToLong(cacheMoney.get(0)));
    		emptyRecord.setRealMoney(CommonUtils.stringObjectToLong(cacheMoney.get(1)));
    		emptyRecord.setRedisMoney(CommonUtils.stringObjectToLong(cacheMoney.get(2)));
    		emptyRecord.setCurTime(currTime);
    		emptyRecord.setCategory(ProductEnum.EMPTY.getProductCode());
    		
    		realtimeRecord.setCustomerId(customerId);
    		realtimeRecord.setFreezdMoney(CommonUtils.stringObjectToLong(cacheMoney.get(3)));
    		realtimeRecord.setRealMoney(CommonUtils.stringObjectToLong(cacheMoney.get(4)));
    		realtimeRecord.setRedisMoney(CommonUtils.stringObjectToLong(cacheMoney.get(5)));
    		realtimeRecord.setCurTime(currTime);
    		realtimeRecord.setCategory(ProductEnum.REALTIME.getProductCode());
    		
    		internationalRecord.setCustomerId(customerId);
    		internationalRecord.setFreezdMoney(CommonUtils.stringObjectToLong(cacheMoney.get(6)));
    		internationalRecord.setRealMoney(CommonUtils.stringObjectToLong(cacheMoney.get(7)));
    		internationalRecord.setRedisMoney(CommonUtils.stringObjectToLong(cacheMoney.get(8)));
    		internationalRecord.setCurTime(currTime);
    		internationalRecord.setCategory(ProductEnum.INTERNATIONAL.getProductCode());
    		
    		directCommonRecord.setCustomerId(customerId);
    		directCommonRecord.setFreezdMoney(CommonUtils.stringObjectToLong(cacheMoney.get(9)));
    		directCommonRecord.setRealMoney(CommonUtils.stringObjectToLong(cacheMoney.get(10)));
    		directCommonRecord.setRedisMoney(CommonUtils.stringObjectToLong(cacheMoney.get(11)));
    		directCommonRecord.setCurTime(currTime);
    		directCommonRecord.setCategory(ProductEnum.DIRECT_COMMON.getProductCode());
    		
    		lineDirectRecord.setCustomerId(customerId);
    		lineDirectRecord.setFreezdMoney(CommonUtils.stringObjectToLong(cacheMoney.get(12)));
    		lineDirectRecord.setRealMoney(CommonUtils.stringObjectToLong(cacheMoney.get(13)));
    		lineDirectRecord.setRedisMoney(CommonUtils.stringObjectToLong(cacheMoney.get(14)));
    		lineDirectRecord.setCurTime(currTime);
    		lineDirectRecord.setCategory(ProductEnum.LINE_DIRECT.getProductCode());
    		
    		if((emptyRecord.getRealMoney() == null || emptyRecord.getRealMoney() == 0) &&
    				(realtimeRecord.getRealMoney() == null || realtimeRecord.getRealMoney() == 0) &&
    				(internationalRecord.getRealMoney() == null || internationalRecord.getRealMoney() == 0) &&
    				(directCommonRecord.getRealMoney() == null || directCommonRecord.getRealMoney() == 0) &&
    				(lineDirectRecord.getRealMoney() == null || lineDirectRecord.getRealMoney() == 0)) {
    			return true;
    		}
            
            Long lastTime = customerBalance.getLastTime();
            if(lastTime == null || lastTime == 0L) {
            	lastTime = currTime;
            }
            
            emptyRecord.setLastTime(lastTime);
            emptyRecord.setOldDbMoney(customerBalance.getEmptyCount());
            
            realtimeRecord.setLastTime(lastTime);
            realtimeRecord.setOldDbMoney(customerBalance.getRealtimeCount());
            
            internationalRecord.setLastTime(lastTime);
            internationalRecord.setOldDbMoney(customerBalance.getInternationalCount());
            
            directCommonRecord.setLastTime(lastTime);
            directCommonRecord.setOldDbMoney(customerBalance.getDirectCommonCount());
            
            lineDirectRecord.setLastTime(lastTime);
            lineDirectRecord.setOldDbMoney(customerBalance.getLineDirectCount());
            //设置扣减后的空号余额和判断余额是否足够
            if (customerBalance.getEmptyCount() - emptyRecord.getRealMoney() < 0) {
                log.info("{}，balance settlement failed, empty balance not enough,mysql balance:{}, record:{}", customerId,customerBalance.getEmptyCount(),JSON.toJSONString(emptyRecord));
                dingdingMessage.sendMessage(String.format("警告：%s, balance settlement failed, empty balance not enough,mysql balance:%s, "
                		+ "redis balance:%s，real deduct balance：%s", customerId,customerBalance.getEmptyCount(),emptyRecord.getRedisMoney(),emptyRecord.getRealMoney()));
            }
            
            if (customerBalance.getRealtimeCount() - realtimeRecord.getRealMoney() < 0) {
                log.info("{}，balance settlement failed, realtime balance not enough,mysql balance:{}, record:{}", customerId,customerBalance.getRealtimeCount(),JSON.toJSONString(realtimeRecord));
                dingdingMessage.sendMessage(String.format("警告：%s, balance settlement failed, realtime balance not enough,mysql balance:%s, "
                		+ "redis balance:%s，real deduct balance：%s", customerId,customerBalance.getRealtimeCount(),realtimeRecord.getRedisMoney(),realtimeRecord.getRealMoney()));
            }
            
            if (customerBalance.getInternationalCount() - internationalRecord.getRealMoney() < 0) {
                log.info("{}，balance settlement failed, international balance not enough,mysql balance:{}, record:{}", customerId,customerBalance.getInternationalCount(),JSON.toJSONString(internationalRecord));
                dingdingMessage.sendMessage(String.format("警告：%s, balance settlement failed, international balance not enough,mysql balance:%s, "
                		+ "redis balance:%s，real deduct balance：%s", customerId,customerBalance.getInternationalCount(),internationalRecord.getRedisMoney(),internationalRecord.getRealMoney()));
            }
            
            if (customerBalance.getDirectCommonCount() - directCommonRecord.getRealMoney() < 0) {
                log.info("{}，balance settlement failed, direct common balance not enough,mysql balance:{}, record:{}", customerId,customerBalance.getDirectCommonCount(),JSON.toJSONString(directCommonRecord));
                dingdingMessage.sendMessage(String.format("警告：%s, balance settlement failed, direct common balance not enough,mysql balance:%s, "
                		+ "redis balance:%s，real deduct balance：%s", customerId,customerBalance.getDirectCommonCount(),directCommonRecord.getRedisMoney(),directCommonRecord.getRealMoney()));
            }
            
            if (customerBalance.getLineDirectCount() - lineDirectRecord.getRealMoney() < 0) {
                log.info("{}，balance settlement failed, line direct balance not enough,mysql balance:{}, record:{}", customerId,customerBalance.getLineDirectCount(),JSON.toJSONString(lineDirectRecord));
                dingdingMessage.sendMessage(String.format("警告：%s, balance settlement failed, line direct balance not enough,mysql balance:%s, "
                		+ "redis balance:%s，real deduct balance：%s", customerId,customerBalance.getLineDirectCount(),lineDirectRecord.getRedisMoney(),lineDirectRecord.getRealMoney()));
            }

            //更新余额以及上次记录时间
            int successCount = customerBalanceService.balanceSettlement(customerId, emptyRecord.getRealMoney(),realtimeRecord.getRealMoney(),internationalRecord.getRealMoney(),
            		directCommonRecord.getRealMoney(),lineDirectRecord.getRealMoney(),currTime);
            if (successCount == 0) {
                log.info("{}, balance settlement failed，mysql record update failed,empty record:{},realtime record:{},international record:{},directCommon record:{},lineDirect record:{}", 
                		customerId,JSON.toJSONString(emptyRecord),JSON.toJSONString(realtimeRecord),JSON.toJSONString(internationalRecord),JSON.toJSONString(directCommonRecord),JSON.toJSONString(lineDirectRecord));
                dingdingMessage.sendMessage(String.format("警告：%s, balance settlement failed，mysql record update failed", customerId));
                throw new BusinessException(ApiCode.DAO_EXCEPTION);
            }          
            
            emptyRecord.setOptType(OptTypeEnum.SETTLE_SUB.getValue());             
            emptyRecord.setDbMoney(customerBalance.getEmptyCount()-emptyRecord.getRealMoney()); 
            
            realtimeRecord.setOptType(OptTypeEnum.SETTLE_SUB.getValue());             
            realtimeRecord.setDbMoney(customerBalance.getRealtimeCount()-realtimeRecord.getRealMoney());
            
            internationalRecord.setOptType(OptTypeEnum.SETTLE_SUB.getValue());             
            internationalRecord.setDbMoney(customerBalance.getInternationalCount()-internationalRecord.getRealMoney());
            
            directCommonRecord.setOptType(OptTypeEnum.SETTLE_SUB.getValue());             
            directCommonRecord.setDbMoney(customerBalance.getDirectCommonCount()-directCommonRecord.getRealMoney());
            
            lineDirectRecord.setOptType(OptTypeEnum.SETTLE_SUB.getValue());             
            lineDirectRecord.setDbMoney(customerBalance.getLineDirectCount()-lineDirectRecord.getRealMoney());
            
            //插入本次余额流水记录
            List<BalanceFlow> balanceFlowList = new ArrayList<BalanceFlow>();
            if(emptyRecord.getRealMoney() != null && emptyRecord.getRealMoney() != 0) {
            	//核对数据库空号余额与redis余额是否一致
                Long emptyRedisMoney = emptyRecord.getFreezdMoney()+emptyRecord.getRedisMoney()+emptyRecord.getRealMoney();
            	if(customerBalance.getEmptyCount().compareTo(emptyRedisMoney) != 0) {
            		log.info("{}，empty balance check failed,mysql balance not equal redis balance，info：{}"
                			,customerId,JSON.toJSONString(emptyRecord));
            		dingdingMessage.sendMessage(String.format("警告：%s, empty balance check failed,mysql balance not equal redis balance, curTime:%s"
            				, customerId,emptyRecord.getCurTime()));
            	}
            	
            	balanceFlowList.add(emptyRecord);
            }
            
            if(realtimeRecord.getRealMoney() != null && realtimeRecord.getRealMoney() != 0) {
            	//核对数据库实时余额与redis余额是否一致
                Long realtimeRedisMoney = realtimeRecord.getFreezdMoney()+realtimeRecord.getRedisMoney()+realtimeRecord.getRealMoney();
            	if(customerBalance.getRealtimeCount().compareTo(realtimeRedisMoney) != 0) {
            		log.info("{}，realtime balance check failed,mysql balance not equal redis balance，info：{}"
                			,customerId,JSON.toJSONString(realtimeRecord));
            		dingdingMessage.sendMessage(String.format("警告：%s, realtime balance check failed,mysql balance not equal redis balance, curTime:%s"
            				, customerId,realtimeRecord.getCurTime()));
            	}
            	
            	balanceFlowList.add(realtimeRecord);
            }
            
            if(internationalRecord.getRealMoney() != null && internationalRecord.getRealMoney() != 0) {
            	//核对数据库国际余额与redis余额是否一致
                Long internationalRedisMoney = internationalRecord.getFreezdMoney()+internationalRecord.getRedisMoney()+internationalRecord.getRealMoney();
            	if(customerBalance.getInternationalCount().compareTo(internationalRedisMoney) != 0) {
            		log.info("{}，international balance check failed,mysql balance not equal redis balance，info：{}"
                			,customerId,JSON.toJSONString(internationalRecord));
            		dingdingMessage.sendMessage(String.format("警告：%s, international balance check failed,mysql balance not equal redis balance, curTime:%s"
            				, customerId,internationalRecord.getCurTime()));
            	}
            	
            	balanceFlowList.add(internationalRecord);
            }
            
            if(directCommonRecord.getRealMoney() != null && directCommonRecord.getRealMoney() != 0) {
            	//核对数据库定向通用余额与redis余额是否一致
                Long directCommonRedisMoney = directCommonRecord.getFreezdMoney()+directCommonRecord.getRedisMoney()+directCommonRecord.getRealMoney();
            	if(customerBalance.getDirectCommonCount().compareTo(directCommonRedisMoney) != 0) {
            		log.info("{}，direct common balance check failed,mysql balance not equal redis balance，info：{}"
                			,customerId,JSON.toJSONString(directCommonRecord));
            		dingdingMessage.sendMessage(String.format("警告：%s, direct common balance check failed,mysql balance not equal redis balance, curTime:%s"
            				, customerId,directCommonRecord.getCurTime()));
            	}
            	
            	balanceFlowList.add(directCommonRecord);
            }
            
            if(lineDirectRecord.getRealMoney() != null && lineDirectRecord.getRealMoney() != 0) {
            	//核对数据库line定向余额与redis余额是否一致
                Long lineDirectRedisMoney = lineDirectRecord.getFreezdMoney()+lineDirectRecord.getRedisMoney()+lineDirectRecord.getRealMoney();
            	if(customerBalance.getLineDirectCount().compareTo(lineDirectRedisMoney) != 0) {
            		log.info("{}，line direct balance check failed,mysql balance not equal redis balance，info：{}"
                			,customerId,JSON.toJSONString(lineDirectRecord));
            		dingdingMessage.sendMessage(String.format("警告：%s, line direct balance check failed,mysql balance not equal redis balance, curTime:%s"
            				, customerId,lineDirectRecord.getCurTime()));
            	}
            	
            	balanceFlowList.add(lineDirectRecord);
            }
            
            successCount = balanceFlowService.saveList(balanceFlowList);
            if (successCount != balanceFlowList.size()) {
                log.info("{}, balance settlement failed,save balance flow record exception, list:{}", customerId,JSON.toJSONString(balanceFlowList));
                dingdingMessage.sendMessage(String.format("警告：%s, balance settlement failed,save balance flow record exception", customerId));
                throw new BusinessException(ApiCode.DAO_EXCEPTION);
            }
            
            //账号实际扣款金额扣减本次费用
            balanceService.subRealDeductBalance(customerId, emptyRecord.getRealMoney(),realtimeRecord.getRealMoney(),internationalRecord.getRealMoney(),
            		directCommonRecord.getRealMoney(),lineDirectRecord.getRealMoney());
            log.info("{}, balance settlement success,currtime:{},emptyRealMoney:{},realtimeRealMoney:{},internationalRealMoney:{},directCommonRealMoney:{},lineDirectRealMoney:{}", customerId,emptyRecord.getCurTime(),
            		 emptyRecord.getRealMoney(), realtimeRecord.getRealMoney(),internationalRecord.getRealMoney(), directCommonRecord.getRealMoney(),lineDirectRecord.getRealMoney());
            
            return true;  
        } catch (Exception ex) {
            log.error("{},balance settlement exception,info:", customerId, ex);
            dingdingMessage.sendMessage(String.format("警告：%s, balance settlement exception, Exception:%s", customerId,ex));
            throw ex;
        }   	
    }
    
    /**
     * redis查询需要结算的customerId
     * */
    private Set<String> getCustomerIdList(){
    	Set<String> resultSet = new HashSet<String>();
    	// 获取空号用户所有的余额keys
    	Set<String> emptySets = redisClient.keys(RedisKeyConstant.EMPTY_BALANCE_KEY + "*");
    	// 获取实时用户所有的余额keys
    	Set<String> realtimeSets = redisClient.keys(RedisKeyConstant.REALTIME_BALANCE_KEY + "*");
    	// 获取国际用户所有的余额keys
    	Set<String> internationalSets = redisClient.keys(RedisKeyConstant.INTERNATIONAL_BALANCE_KEY + "*");
    	// 获取定向通用用户所有的余额keys
    	Set<String> directCommonSets = redisClient.keys(RedisKeyConstant.DIRECT_COMMON_BALANCE_KEY + "*");
    	// 获取line定向用户所有的余额keys
    	Set<String> lineDirectSets = redisClient.keys(RedisKeyConstant.LINE_DIRECT_BALANCE_KEY + "*");
    	if(!CollectionUtils.isEmpty(emptySets)) {
    		emptySets.forEach(e -> {
        		resultSet.add(e.replace(RedisKeyConstant.EMPTY_BALANCE_KEY, ""));
        	});
    	}
    	
    	if(!CollectionUtils.isEmpty(realtimeSets)) {
    		realtimeSets.forEach(r -> {
        		resultSet.add(r.replace(RedisKeyConstant.REALTIME_BALANCE_KEY, ""));
        	});
    	}
    	
    	if(!CollectionUtils.isEmpty(internationalSets)) {
    		internationalSets.forEach(r -> {
        		resultSet.add(r.replace(RedisKeyConstant.INTERNATIONAL_BALANCE_KEY, ""));
        	});
    	}
    	
    	if(!CollectionUtils.isEmpty(directCommonSets)) {
    		directCommonSets.forEach(r -> {
        		resultSet.add(r.replace(RedisKeyConstant.DIRECT_COMMON_BALANCE_KEY, ""));
        	});
    	}
    	
    	if(!CollectionUtils.isEmpty(lineDirectSets)) {
    		lineDirectSets.forEach(r -> {
        		resultSet.add(r.replace(RedisKeyConstant.LINE_DIRECT_BALANCE_KEY, ""));
        	});
    	}
    	
        return resultSet;
    }
}