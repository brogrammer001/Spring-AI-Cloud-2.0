package com.mall.aichat.config;

import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.ai.listener.AbstractNacosPromptListener;
import com.alibaba.nacos.api.ai.listener.NacosPromptEvent;
import com.alibaba.nacos.api.ai.model.prompt.Prompt;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nacos Prompt Registry：Prompt 拉取与订阅服务
 * <p>启动时按 {@link PromptProperties.Binding} 逐个订阅 Nacos Prompt Registry（promptKey 维度），
 * 控制台发布新版本后自动推送热更新，无需重启应用。</p>
 * <ul>
 *   <li>MD5 去重：重复推送相同内容时跳过替换</li>
 *   <li>Last-Known-Good：某版本/标签被下线导致推送空事件时，保留最近一次有效 Prompt 仅告警</li>
 *   <li>不可变快照：缓存内容以 record 暴露，保证并发读安全</li>
 * </ul>
 */
@Service
public class NacosPromptRegistry {

    private static final Logger log = LoggerFactory.getLogger(NacosPromptRegistry.class);

    private final AiService aiService;
    private final PromptProperties properties;

    /** 业务别名 → 不可变 Prompt 快照，保证并发读安全 */
    private final ConcurrentHashMap<String, PromptSnapshot> prompts = new ConcurrentHashMap<>();

    /**
     * 每次更新生成不可变快照
     */
    public record PromptSnapshot(String promptKey, String version, String md5, String template) {
    }

    public NacosPromptRegistry(AiService aiService, PromptProperties properties) {
        this.aiService = aiService;
        this.properties = properties;
    }

    @PostConstruct
    void subscribeAll() {
        if (properties.getBindings().isEmpty()) {
            throw new IllegalStateException("未配置任何 Nacos Prompt 绑定 (spring.ai.nacos.prompt.bindings)");
        }
        properties.getBindings().forEach(this::subscribe);
    }

    private void subscribe(String name, PromptProperties.Binding binding) {
        try {
            // 参数顺序：subscribePrompt(promptKey, version, label, listener)
            // 按标签订阅时 version 传 null；两者都不传则跟随 latest
            var current = aiService.subscribePrompt(
                binding.getKey(),
                binding.getVersion(),
                binding.getLabel(),
                new AbstractNacosPromptListener() {
                    @Override
                    public void onEvent(NacosPromptEvent event) {
                        if (event == null || event.getPrompt() == null) {
                            // Last-Known-Good 策略：保留最近一次有效 Prompt，仅告警
                            log.warn("Nacos Prompt [{}] 当前 version/label 无法解析到在线版本，沿用旧内容", name);
                            return;
                        }
                        update(name, event.getPrompt());
                        log.info("Nacos Prompt [{}] 热更新完成: key={}, version={}, md5={}", name, event.getPrompt().getPromptKey(), event.getPrompt().getVersion(), event.getPrompt().getMd5());
                    }
                });
            // 订阅成功后立即用首次返回值初始化本地缓存
            update(name, current);
            log.info("Nacos Prompt [{}] 加载成功, key={}, version={}", name, current.getPromptKey(), current.getVersion());
        } catch (Exception e) {
            if (binding.isRequired()) {
                throw new IllegalStateException("启动加载 Nacos Prompt 失败: " + binding.getKey(), e);
            }
            log.warn("Nacos Prompt [{}] 非必需，加载失败忽略: {}", name, e.getMessage());
        }
    }

    /**
     * 通过 MD5 去重，避免重复替换
     */
    private void update(String name, Prompt prompt) {
        var previous = prompts.get(name);
        if (previous != null && Objects.equals(previous.md5(), prompt.getMd5())) {
            return;
        }
        prompts.put(name, new PromptSnapshot(
            prompt.getPromptKey(),
            prompt.getVersion(),
            prompt.getMd5(),
            prompt.getTemplate()));
    }

    /**
     * 业务侧统一入口：按别名取 prompt 原文
     */
    public String get(String name) {
        var snapshot = prompts.get(name);
        if (snapshot == null) {
            throw new IllegalStateException("Prompt 未加载: " + name);
        }
        return snapshot.template();
    }

    /**
     * 取快照元数据（版本 / md5），可用于链路追踪
     */
    public PromptSnapshot getSnapshot(String name) {
        var snapshot = prompts.get(name);
        if (snapshot == null) {
            throw new IllegalStateException("Prompt 未加载: " + name);
        }
        return snapshot;
    }

    /**
     * 如果模板里有 {{variable}} 占位符，按简单字符串替换渲染
     */
    public String render(String name, Map<String, String> variables) {
        String template = get(name);
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        for (var entry : variables.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }
}