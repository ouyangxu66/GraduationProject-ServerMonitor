package com.xu.monitorcommon.dto;



public class AgentDTO {


    /**
     *  注册
     */
    public static class Register {
        private String agentId;
        private String hostname;
        private String osName;
        private String ip; // 这里的IP仅作为元数据展示，不作为唯一标识

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getOsName() {
            return osName;
        }

        public void setOsName(String osName) {
            this.osName = osName;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }
    }

    /**
     * 心跳
     */
    public static class Heartbeat {
        private String agentId;
        private long timestamp;

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}