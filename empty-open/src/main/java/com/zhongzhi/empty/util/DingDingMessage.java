package com.zhongzhi.empty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 钉钉报警消息
 * @author liuh
 * @date 2021年10月26日
 */
@Component
public class DingDingMessage {

    private static final Logger logger = LoggerFactory.getLogger(DingDingMessage.class);

    @Value("${empty.dingding.url}")
    private String dingUrl;

    private final static String DING_MESSAGE_TEMPLATE=new String("{ \"msgtype\": \"text\", \"text\": {\"content\": \"%s\"} ,\"at\":{\"isAtAll\":false}}");

    //异步发送
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * 发送钉钉通知消息
     * @param content 消息内容
     */
    public void sendMessage(String content){
        //异步发送
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String message=String.format(DING_MESSAGE_TEMPLATE,content);
                String result=sendSmsByPost(dingUrl,message);
                logger.info("钉钉消息发送，内容：{}，返回：{}",message,result);
            }
        });
    }

    private String sendSmsByPost(String path, String postContent) {
        return HttpUtils.postJson(path, postContent);
    }
}
