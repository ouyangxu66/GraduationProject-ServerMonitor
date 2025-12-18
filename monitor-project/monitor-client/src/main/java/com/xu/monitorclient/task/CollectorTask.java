package com.xu.monitorclient.task;

import com.xu.monitorclient.core.AgentIdentity;
import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorcommon.utils.SystemMonitorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class CollectorTask {

    //1.手动声明Logger常量,用来获取日志信息
    private static final Logger logger = LoggerFactory.getLogger(CollectorTask.class);

    private final AgentIdentity agentIdentity;
    public CollectorTask(AgentIdentity agentIdentity){
        this.agentIdentity = agentIdentity;
    }

    //2.从配置文件中获取服务端地址,机器id,通信密钥
    @Value("${monitor.server-url}")
    private String serverUrl;

    @Value("${monitor.app-id}")
    private String appId;

    @Value("${monitor.app-secret}")
    private String appSecret;


    //3.创建RestTemplate对象,用来发送数据
    private final RestTemplate restTemplate = new RestTemplate();

    //4.定时任务
    @Scheduled(fixedRateString="${monitor.interval}")
    public void run() throws InterruptedException {
        try {
            //4.1调用采集数据工具类
            BaseMonitorModel data = SystemMonitorUtil.collect();

            //4.2打印日志,并调用方法上传数据到服务端
            logger.info("[数据采集成功]CPU利用率: {}% 内存已使用: {}G",
                    data.getCpuLoad(),data.getMemoryUsed());

            //4.3注入唯一标识
            data.setAgentId(agentIdentity.getAgentId());
            sendDataToServer(data);
        } catch (InterruptedException e) {
            logger.error("采集或上报数据失败",e);
        }

    }

    /**
     * 上报数据
     * @param data
     */
    private void sendDataToServer(BaseMonitorModel data) {
        logger.info(">>>正在向服务端[{}] 上报数据...",serverUrl);

        try {
            //发送数据
            // 参数1: URL
            // 参数2: 要发送的对象 (Spring 会自动把它转换成 JSON 格式，例如 {"cpuLoad":15.5})
            // 参数3: 服务端返回的数据类型 (服务端返回 "success"，所以是 String.class)
            String response = restTemplate.postForObject(serverUrl, data, String.class);
            logger.info("服务器响应: {}",response);
        } catch (RestClientException e) {
            logger.error("发送失败,服务端可能未启动: {}",e.getMessage());
        }

    }
}
