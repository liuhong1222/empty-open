package com.zhongzhi.empty.service;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.exception.BusinessException;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.LuaByDeductFeeResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * lua表达式-redis
 * @author liuh
 * @date 2021年10月28日
 */
@Slf4j
@Service
public class LuaExpressionService {
	
	@Autowired
	private RedisClient redisClient;
	
	/**
	 * 已发送号码批量存入redis缓存
	 * @param keyList
	 * @param expires
	 * @return
	 */
	public boolean msetBySendFrequency(List<String> keyList,String value,int expires) {
		//封装lua表达式
		StringBuffer luaString  =  new StringBuffer();
		for (int i = 0; i < keyList.size(); i++) {
			luaString.append(String.format("redis.call('set', KEYS[%s], ARGV[1]); ",i+1));
			luaString.append(String.format("redis.call('EXPIRE', KEYS[%s], ARGV[2]); ",i+1));
		}
		
		luaString.append("return 'true' ");
		//参数list
		List<String> paramList = new ArrayList<String>();
		paramList.add(value);
		paramList.add(String.valueOf(expires));
		//使用eval命令执行lua表达式
		Object balanceObj = redisClient.eval(luaString.toString(),keyList,paramList);
		log.info("lua msetBySendFrequency result:{}",balanceObj);
		if(ObjectUtils.isEmpty(balanceObj)) {
			return false;
		}
		
		return Boolean.parseBoolean(balanceObj.toString());
	}
	
	public boolean luaByPreDeductFee(List<String> keyList,List<String> paramList) {
		//封装lua表达式
		StringBuffer luaString  =  new StringBuffer();	
		//redis余额扣减本次预扣费用且返回扣减后的redis余额
		luaString.append("local balance = redis.call('decrBy', KEYS[1], ARGV[1]); "); 
		//扣减后的redis余额小于0，则
		//1. redis余额加上本次预扣金额
		//2. 返回null
		luaString.append("if (balance < 0) then ");
		luaString.append("   redis.call('incrby', KEYS[1], ARGV[1]); ");
		luaString.append("   return nil; ");
		luaString.append("end; ");
		//扣件后的redis余额大于等于0，则
		//1. 冻结金额加上本次预扣金额
		//2. 返回扣减后的余额
		luaString.append("if (balance >= 0) then ");
		luaString.append("   redis.call('incrby', KEYS[2], ARGV[1]); ");
		luaString.append("   return balance; ");
		luaString.append("end; ");
		
		//使用eval命令执行lua表达式
		Object balanceObj = redisClient.eval(luaString.toString(), keyList,paramList);
		if(ObjectUtils.isEmpty(balanceObj)) {
			return false;
		}
		
		return true;
	}
	
	public void luaByBackDeductFee(List<String> keyList,List<String> paramList) {
		//封装lua表达式
		StringBuffer luaString  =  new StringBuffer(); 
		//冻结金额扣除本次费用
		luaString.append("redis.call('decrBy', KEYS[2], ARGV[1]); ");
		//redis余额加上本次费用
		luaString.append("redis.call('incrby', KEYS[1], ARGV[1]); ");
		
		//使用eval命令执行lua表达式
		redisClient.eval(luaString.toString(), keyList,paramList);
	}
	
	public LuaByDeductFeeResponse luaByDeductFee(long diffFee,List<String> keyList,List<String> paramList) {
		//封装lua表达式
		StringBuffer luaString = new StringBuffer();
		//如果预扣费用和实际费用之间的差值为0，则
		//1. 冻结金额扣减预扣费用
		//2. 实际扣款金额加上实际费用
		//3。 获取redis当前时间，用于流水对账，单位：微秒
		//4. 返回redis余额和redis时间
		if(diffFee == 0) {
			luaString.append("   redis.call('decrBy', KEYS[2], ARGV[2]); ");
			luaString.append("   redis.call('incrby', KEYS[3], ARGV[3]); ");	
			luaString.append("   local a = redis.call('time');  ");
			luaString.append("   return {redis.call('get', KEYS[1]),a[1]*1000000+a[2]}; ");
		}else if(diffFee > 0) {
			//如果预扣费用和实际费用之间的差值大于0，则
			//1. redis余额加上预扣费用和实际费用之间的差值
			//2. 冻结金额扣减预扣费用
			//3. 实际扣款金额加上实际费用
			//4。 获取redis当前时间，用于流水对账，单位：微秒
			//5. 返回redis余额和redis时间
			luaString.append("   redis.call('incrby', KEYS[1], ARGV[1]); ");
			luaString.append("   redis.call('decrBy', KEYS[2], ARGV[2]); ");
			luaString.append("   redis.call('incrby', KEYS[3], ARGV[3]); ");
			luaString.append("   local a = redis.call('time');  ");
			luaString.append("   return {redis.call('get', KEYS[1]),a[1]*1000000+a[2]}; ");
		}else {
			//如果预扣费用和实际费用之间的差值小于0，则直接返回且抛出异常
			throw new BusinessException(ApiCode.BUSINESS_EXCEPTION,"实际费用不能大于预扣费用");
		}
		
		//使用eval命令执行lua表达式
		Object balanceObj = redisClient.eval(luaString.toString(), keyList,paramList);
		if(ObjectUtils.isEmpty(balanceObj)) {
			throw new BusinessException(ApiCode.BUSINESS_EXCEPTION,"调用异常，实际费用大于预扣费用");
		}
		
		List<Object> balanceObjList = (List<Object>)balanceObj;
		if(CollectionUtils.isEmpty(balanceObjList) ) {
			throw new BusinessException(ApiCode.BUSINESS_EXCEPTION,"调用异常，用户无任何调用记录");
        }
		
		//返回redis当前余额以及redis时间(单位:毫秒)
		return new LuaByDeductFeeResponse(Long.valueOf(balanceObjList.get(0).toString()),(Long)(Long.valueOf(balanceObjList.get(1).toString())/1000));
	}
	
	public Object luaByGetCustomerRedisData(List<String> keyList) {
		//封装lua表达式
        String getRedisStr = new StringBuffer()
                .append("local emptyFreezed = redis.call('get',KEYS[1]);  ")
                .append("local emptyReal = redis.call('get',KEYS[2]);  ")
                .append("local emptyBalance = redis.call('get',KEYS[3]);  ")
                
                .append("local realtimeFreezed = redis.call('get',KEYS[4]);  ")
                .append("local realtimeReal = redis.call('get',KEYS[5]);  ")
                .append("local realtimeBalance = redis.call('get',KEYS[6]);  ")
                
                .append("local internationalFreezed = redis.call('get',KEYS[7]);  ")
                .append("local internationalReal = redis.call('get',KEYS[8]);  ")
                .append("local internationalBalance = redis.call('get',KEYS[9]);  ")
                
                .append("local directCommonFreezed = redis.call('get',KEYS[10]);  ")
                .append("local directCommonReal = redis.call('get',KEYS[11]);  ")
                .append("local directCommonBalance = redis.call('get',KEYS[12]);  ")
                
                .append("local lineDirectFreezed = redis.call('get',KEYS[13]);  ")
                .append("local lineDirectReal = redis.call('get',KEYS[14]);  ")
                .append("local lineDirectBalance = redis.call('get',KEYS[15]);  ")
                
                .append("local a = redis.call('time');  ")
                .append("return {emptyFreezed,emptyReal,emptyBalance,"
                		+ "realtimeFreezed,realtimeReal,realtimeBalance,"
                		+ "internationalFreezed,internationalReal,internationalBalance,"
						+ "directCommonFreezed,directCommonReal,directCommonBalance,"
						+ "lineDirectFreezed,lineDirectReal,lineDirectBalance,"
                		+ "a[1]*1000000+a[2]}")
                .toString();
        
        //使用eval命令执行lua表达式
        return redisClient.eval(getRedisStr,15,keyList.get(0),keyList.get(1),keyList.get(2),
        									   keyList.get(3),keyList.get(4),keyList.get(5),
        									   keyList.get(6),keyList.get(7),keyList.get(8),
        									   keyList.get(9),keyList.get(10),keyList.get(11),
        									   keyList.get(12),keyList.get(13),keyList.get(14));
	}
	
	public void subRealDeductBalance(List<String> keyList,List<String> paramList) {
		//封装lua表达式
		StringBuffer luaString  =  new StringBuffer(); 
		luaString.append("redis.call('decrBy', KEYS[5], ARGV[5]); ");
		luaString.append("redis.call('decrBy', KEYS[4], ARGV[4]); ");
		luaString.append("redis.call('decrBy', KEYS[3], ARGV[3]); ");
		luaString.append("redis.call('decrBy', KEYS[2], ARGV[2]); ");
		luaString.append("redis.call('decrBy', KEYS[1], ARGV[1]); ");
		
		//使用eval命令执行lua表达式
		redisClient.eval(luaString.toString(), keyList,paramList);
	}
}
