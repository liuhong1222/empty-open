package com.zhongzhi.empty.util;

import java.util.UUID;

import com.zhongzhi.empty.vo.CustomerInfoVo;

/**
 * 当前线程变量容器
 * @author liuh
 * @date 2021年10月26日
 */
public final class ThreadLocalContainer {
    /**
     * 存储当前线程uuid
     */
    private static final ThreadLocal<String> uuidLocal = new XThreadLocal<>();

    /**
     * 存储当前线程账号id
     */
    private static final ThreadLocal<CustomerInfoVo> customerInfoVoLocal = new XThreadLocal<>();

    public static CustomerInfoVo getCustomerInfoVo() {
        return customerInfoVoLocal.get();
    }

    public static void setCustomerInfoVo(CustomerInfoVo customerInfoVo) {
    	customerInfoVoLocal.set(customerInfoVo);
    }

    public static String getUUID() {
        return uuidLocal.get();
    }

    public static void setUUID() {
        uuidLocal.set(UUID.randomUUID().toString().replace("-", ""));
    }


    /**
     * clear all thread local object
     */
    public static void clearAll() {
    	customerInfoVoLocal.remove();
        uuidLocal.remove();
    }

}
