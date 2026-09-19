package com.mall.aichat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;


@Component
@ConfigurationProperties(prefix = "spring.ai.nacos.prompt")
public class PromptProperties {
    private String serverAddr;
    private String namespaceId;
    private String username;
    private String password;
    private String transportMode;
    /** Prompt 缓存更新轮询间隔（毫秒），默认 60000（60秒），避免频繁请求 Nacos */
    private long promptCacheUpdateInterval = 60000L;
    private Map<String, Binding> bindings = new LinkedHashMap<>();

    public static class Binding {
        /** Nacos Prompt Key */
        private String key;
        /** 固定版本号，与 label 互斥 */
        private String version;
        /** 版本标签（如 latest / production） */
        private String label;
        /** 启动时必须加载成功，默认 true */
        private boolean required = true;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public long getPromptCacheUpdateInterval() {
        return promptCacheUpdateInterval;
    }

    public void setPromptCacheUpdateInterval(long promptCacheUpdateInterval) {
        this.promptCacheUpdateInterval = promptCacheUpdateInterval;
    }

    public Map<String, Binding> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, Binding> bindings) {
        this.bindings = bindings;
    }
}
