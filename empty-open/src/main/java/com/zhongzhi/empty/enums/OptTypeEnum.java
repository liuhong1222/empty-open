package com.zhongzhi.empty.enums;

/**
 * 操作类型枚举  
 * 扣除都是负数，加钱都是正数
 * @author liuh
 * @date 2021年11月09日
 */
public enum OptTypeEnum {

    /**
     * 结算扣款
     */
    SETTLE_SUB(-4)

    ;

    private Integer value;

    OptTypeEnum(int value){
        this.value=value;
    }

    public Integer getValue(){
        return  this.value;
    }
}
