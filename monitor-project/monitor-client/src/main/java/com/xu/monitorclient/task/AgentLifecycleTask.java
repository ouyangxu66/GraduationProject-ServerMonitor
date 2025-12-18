package com.xu.monitorclient.task;

import com.xu.monitorclient.core.AgentIdentity;
import com.xu.monitorcommon.constant.AppConstants;
import com.xu.monitorcommon.dto.AgentDTO;
import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorcommon.utils.SystemMonitorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class AgentLifecycleTask {
    private static final Logger logger = LoggerFactory.getLogger(AgentLifecycleTask.class);

    private final AgentIdentity agentIdentity;
    
    public AgentLifecycleTask(AgentIdentity agentIdentity){
        this.agentIdentity=agentIdentity;
    }


    // 基准URL,用于拼接
    @Value("${monitor.server-base-url}")
    private String serverBaseUrl;

    // 注入配置的密钥,用于鉴权
    @Value("${monitor.app-secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // 标记是否已经注册
    private boolean isRegistered = false;

    /**
     * 定时任务，每30秒执行一次
     * 根据是否注册状态来判断执行哪一个方法
     */
    @Scheduled(fixedRate = 30000)
    public void maintainLifecycle(){
        if (!isRegistered){
            doRegister();
        }else {
            doHeartbeat();
        }
    }

    /**
     * 心跳监测
     */
    private void doHeartbeat() {
        try {
            AgentDTO.Heartbeat dto = new AgentDTO.Heartbeat();
            dto.setAgentId(agentIdentity.getAgentId());
            dto.setTimestamp(System.currentTimeMillis());

            String url = serverBaseUrl + "/api/agent/heartbeat";
            //携带Headers发送请求
            sendRequest(url, dto);
            logger.debug("💓 心跳发送成功");

        } catch (Exception e) {
            logger.warn("💔 心跳发送失败: {}", e.getMessage());
            // 如果心跳连续失败多次，可以考虑重置 isRegistered = false，触发重新注册逻辑(可选)
        }
    }

    /**
     * 服务器注册
     */
    private void doRegister() {
        logger.info("正在尝试向服务端注册 Agent...");
        try {
            AgentDTO.Register dto = new AgentDTO.Register();
            dto.setAgentId(agentIdentity.getAgentId());

            // 采集一次静态信息用于注册
            BaseMonitorModel model = SystemMonitorUtil.collect();
            dto.setHostname(model.getHostName());
            dto.setOsName(model.getOsName());
            dto.setIp(model.getIp());

            String url = serverBaseUrl + "/api/agent/register";
            // 携带Headers发送请求
            sendRequest(url,dto);
            // 注册成功，修改标记
            isRegistered = true;
            logger.info("✅ Agent 注册成功! AgentID: {}", dto.getAgentId());
        } catch (ResourceAccessException e) {
            logger.warn("❌ 注册失败: 无法连接服务端，将在下个周期重试。错误: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("❌ 注册过程发生未知异常", e);
        }
    }

    /**
     * 携带密钥请求头发送HTTP请求到指定URL
     * 
     * @param url 请求的目标URL
     * @param body 请求体内容
     */
    private void sendRequest(String url, Object body){
        HttpHeaders headers = new HttpHeaders();
        // 设置请求内容类型为JSON
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 请求头中添加密钥
        headers.add(AppConstants.MONITOR_APP_SECRET_HEADER,appSecret);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        restTemplate.postForObject(url,entity,String.class);
    }



}