package com.zhongzhi.empty.enums;

import com.zhongzhi.empty.constants.RedisKeyConstant;

/**
 * 产品枚举
 * @author liuh
 * @date 2021年10月28日
 */
public enum ProductEnum {

	EMPTY(0, "空号检测",RedisKeyConstant.EMPTY_BALANCE_KEY,RedisKeyConstant.EMPTY_FREEZE_AMOUNT_KEY,RedisKeyConstant.EMPTY_REAL_DEDUCT_FEE_KEY),
	REALTIME(1, "实时检测",RedisKeyConstant.REALTIME_BALANCE_KEY,RedisKeyConstant.REALTIME_FREEZE_AMOUNT_KEY,RedisKeyConstant.REALTIME_REAL_DEDUCT_FEE_KEY),
	REALTIME_STARDARD(2, "实时检测标准版",RedisKeyConstant.REALTIME_BALANCE_KEY,RedisKeyConstant.REALTIME_FREEZE_AMOUNT_KEY,RedisKeyConstant.REALTIME_REAL_DEDUCT_FEE_KEY),
	INTERNATIONAL(3, "国际检测",RedisKeyConstant.INTERNATIONAL_BALANCE_KEY,RedisKeyConstant.INTERNATIONAL_FREEZE_AMOUNT_KEY,RedisKeyConstant.INTERNATIONAL_REAL_DEDUCT_FEE_KEY),
	
	DIRECT_COMMON(4, "定向通用检测",RedisKeyConstant.DIRECT_COMMON_BALANCE_KEY,RedisKeyConstant.DIRECT_COMMON_FREEZE_AMOUNT_KEY,RedisKeyConstant.DIRECT_COMMON_REAL_DEDUCT_FEE_KEY),
	LINE_DIRECT(5, "line定向检测",RedisKeyConstant.LINE_DIRECT_BALANCE_KEY,RedisKeyConstant.LINE_DIRECT_FREEZE_AMOUNT_KEY,RedisKeyConstant.LINE_DIRECT_REAL_DEDUCT_FEE_KEY),
	;
	
	private int productCode;
	
	private String productName;
	
	private String balanceKey;
	
	private String freezeKey;
	
	private String deductKey;

	ProductEnum() {}
	
	ProductEnum(int productCode,String productName,String balanceKey,String freezeKey,String deductKey) {
		this.productCode = productCode;
		this.productName = productName;
		this.balanceKey = balanceKey;
		this.freezeKey = freezeKey;
		this.deductKey = deductKey;
	}
	
	public static ProductEnum getProductEnum(int productCode) {
		ProductEnum[] pes = ProductEnum.values();
        for (ProductEnum pe : pes) {
            if (pe.getProductCode() == productCode) {
                return pe;
            }
        }
        return null;
    }
	
	public static String getProductBalanceKey(int productCode,Long customerId) {
		ProductEnum[] pes = ProductEnum.values();
        for (ProductEnum pe : pes) {
            if (pe.getProductCode() == productCode) {
                return pe.getBalanceKey() + customerId;
            }
        }
        return null;
    }
	
	public static String getProductFreezeKey(int productCode,Long customerId) {
		ProductEnum[] pes = ProductEnum.values();
        for (ProductEnum pe : pes) {
            if (pe.getProductCode() == productCode) {
                return pe.getFreezeKey() + customerId;
            }
        }
        return null;
    }
	
	public static String getProductDeductKey(int productCode,Long customerId) {
		ProductEnum[] pes = ProductEnum.values();
        for (ProductEnum pe : pes) {
            if (pe.getProductCode() == productCode) {
                return pe.getDeductKey() + customerId;
            }
        }
        return null;
    }
	
	public int getProductCode() {
		return productCode;
	}

	public void setProductCode(int productCode) {
		this.productCode = productCode;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getBalanceKey() {
		return balanceKey;
	}

	public void setBalanceKey(String balanceKey) {
		this.balanceKey = balanceKey;
	}

	public String getFreezeKey() {
		return freezeKey;
	}

	public void setFreezeKey(String freezeKey) {
		this.freezeKey = freezeKey;
	}

	public String getDeductKey() {
		return deductKey;
	}

	public void setDeductKey(String deductKey) {
		this.deductKey = deductKey;
	}
}
