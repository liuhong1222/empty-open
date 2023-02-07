package com.zhongzhi.empty.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;

import com.zhongzhi.empty.util.PackageUtil;

/**
 * httpclient工厂
 * @author liuh
 * @date 2021年10月28日
 */
public class ClientFactory {
	
	private static ClientFactory clientFactory = new ClientFactory();
	
	private Map<String,Client> clientMap;
	
	public static ClientFactory getInstance() {
        return clientFactory;
    }
	
	/**
     * 把所有支付服务对象放进去缓存Map，这里的思路是通过包名及父类class对象Client.class获取到包下所有我们需要的class对象，
     * 得到class对象后可以通过类对象从而得到支付服务对象;要注意防止多线程向map中重复存放元素 ;如果是使用@PostConstruct
     * 注解或者其他方法来让该方法只在项目初始化时执行一次的话，则可以不用考虑这里的多线程并发的问题了
     */
    private synchronized void setClientMap() {
    	if (MapUtils.isNotEmpty(clientMap)) {
            return;
        }
        
        clientMap = new HashMap<String, Client>();
        // 通过包名及目标类对象获取到该包下所有的支付服务类对象，注意下面这步只是获取到了该包下的Client以及Client的子类，但是我们只需要Client的子类，因此还要做过滤
        Set<Class<Client>> clazzs = PackageUtil.getPackageClasses("com.zhongzhi.empty.http", Client.class);
        for (Class<Client> clazz : clazzs) {
            ClientService clientService = clazz.getAnnotation(ClientService.class);
            if (clientService == null) {
                // 过滤掉没有注解的
                continue;
            }
            String[] productNames = clientService.interfaces();
            for (String name : productNames) {
                try {
                    // 放到缓存去
                	Client client = clazz.getConstructor().newInstance();
                	client.initClient();
                	clientMap.put(name, client);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public Client getClient(String interfaces) {
    	if (MapUtils.isEmpty(clientMap)) {
            this.setClientMap();
        }
        return clientMap.get(interfaces);
    }
}
