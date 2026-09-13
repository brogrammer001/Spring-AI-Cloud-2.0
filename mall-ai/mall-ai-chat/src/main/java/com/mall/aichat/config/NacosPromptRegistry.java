package com.mall.aichat.config;

import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.ai.listener.AbstractNacosPromptListener;
import com.alibaba.nacos.api.ai.listener.NacosPromptEvent;
import com.alibaba.nacos.api.ai.model.prompt.Prompt;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nacos Prompt Registry：Prompt 拉取与订阅服务。
 * <p>渲染直接委托给 SDK 的 {@link Prompt#render(Map)}（defaultValue 合并 + 变量覆盖），
 * 本类只补充两件事：漏传变量告警、Object 类型变量转换。</p>
 */
@Service
public class NacosPromptRegistry {

    private static final Logger log = LoggerFactory.getLogger(NacosPromptRegistry.class);

    /** 仅用于渲染后检查残留占位符（不参与替换） */
    private static final Pattern LEFTOVER = Pattern.compile("\\{\\{\\s*[\\w.\\-]+\\s*}}");

    private final AiService aiService;
    private final PromptProperties properties;
    private final ConcurrentHashMap<String, PromptSnapshot> prompts = new ConcurrentHashMap<>();

    /**
     * 不可变快照。prompt 字段直接持有 SDK 对象，渲染走 prompt.render()；
     * 其余字段用于日志、链路追踪和管控台展示。
     */
    public record PromptSnapshot(String promptKey, String version, String md5,
                                 String template, Prompt prompt) { }

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
            var current = aiService.subscribePrompt(
                binding.getKey(), binding.getVersion(), binding.getLabel(),
                new AbstractNacosPromptListener() {
                    @Override
                    public void onEvent(NacosPromptEvent event) {
                        if (event == null || event.getPrompt() == null) {
                            // Last-Known-Good 策略：保留最近一次有效 Prompt，仅告警
                            log.warn("Nacos Prompt [{}] 当前 version/label 无法解析到在线版本，沿用旧内容", name);
                            return;
                        }
                        update(name, event.getPrompt());
                        log.info("Nacos Prompt [{}] 热更新完成: key={}, version={}, md5={}",
                            name, event.getPrompt().getPromptKey(),
                            event.getPrompt().getVersion(), event.getPrompt().getMd5());
                    }
                });

            update(name, current);
            log.info("Nacos Prompt [{}] 加载成功, key={}, version={}",
                name, current.getPromptKey(), current.getVersion());
        } catch (Exception e) {
            if (binding.isRequired()) {
                throw new IllegalStateException("启动加载 Nacos Prompt 失败: " + binding.getKey(), e);
            }
            log.warn("Nacos Prompt [{}] 非必需，加载失败忽略: {}", name, e.getMessage());
        }
    }

    /** 通过 MD5 去重，避免重复替换 */
    private void update(String name, Prompt prompt) {
        var previous = prompts.get(name);
        if (previous != null && Objects.equals(previous.md5(), prompt.getMd5())) {
            return;
        }
        prompts.put(name, new PromptSnapshot(
            prompt.getPromptKey(), prompt.getVersion(), prompt.getMd5(),
            prompt.getTemplate(), prompt));
    }

    /** 按 Nacos 别名取 prompt 原文（未渲染） */
    public String get(String name) {
        return getSnapshot(name).template();
    }

    /** 取快照元数据（版本 / md5 / 原始 Prompt 对象），可用于链路追踪 */
    public PromptSnapshot getSnapshot(String name) {
        var snapshot = prompts.get(name);
        if (snapshot == null) {
            throw new IllegalStateException("Prompt 未加载: " + name);
        }
        return snapshot;
    }

    /**
     * 业务侧统一渲染入口：委托给 SDK 的 prompt.render(variables)。
     * <p>渲染优先级（SDK 内部实现）：调用方变量 &gt; Nacos 控制台 defaultValue。</p>
     *
     * @param name      业务别名（bindings 里配置的名字）
     * @param variables 变量名 → 变量值；可为 null 或空（此时仅使用 defaultValue）
     * @return 渲染后的最终 Prompt 文本
     */
    public String render(String name, Map<String, String> variables) {
        var snapshot = getSnapshot(name);
        // SDK render：template == null 返回 null；merged 为空时返回原 template
        String rendered = snapshot.prompt().render(variables);
        checkLeftover(name, rendered);
        return rendered;
    }

    /** 便捷重载：变量值为任意 Object（数字、List 等）时自动转 String */
    public String render(String name, Map<String, Object> variables, boolean objectValues) {
        if (variables == null || variables.isEmpty()) {
            return render(name, (Map<String, String>) null);
        }
        Map<String, String> stringVars = new HashMap<>(variables.size());
        variables.forEach((k, v) -> stringVars.put(k, v == null ? null : String.valueOf(v)));
        return render(name, stringVars);
    }

    /** SDK render 对漏传变量静默保留，这里补一层告警，方便排查漏传 */
    private void checkLeftover(String name, String rendered) {
        if (rendered == null) {
            return;
        }
        Matcher m = LEFTOVER.matcher(rendered);
        if (m.find()) {
            StringBuilder keys = new StringBuilder();
            m.reset();
            while (m.find()) {
                if (keys.length() > 0) {
                    keys.append(", ");
                }
                keys.append(m.group());
            }
            log.warn("Nacos Prompt [{}] 渲染后仍有未替换占位符（可能漏传变量或模板含空白）: {}", name, keys);
        }
    }
}
