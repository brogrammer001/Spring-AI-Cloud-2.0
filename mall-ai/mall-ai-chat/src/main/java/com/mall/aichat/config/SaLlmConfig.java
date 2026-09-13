package com.mall.aichat.config;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.ai.constant.AiConstants;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 会话记忆配置
 */
@Configuration
public class SaLlmConfig {

    @Value("${mineru.base-url}")
    private String baseUrl;

    @Value("${mineru.token}")
    private String token;

    /**
     * 配置 MinerU请求接口
     * @return
     */
    @Bean
    public RestClient mineruRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
            .requestFactory(factory)
            .build();
    }

    @Bean(destroyMethod = "shutdown")
    public AiService nacosAiService(PromptProperties props) throws NacosException {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, props.getServerAddr());
        properties.setProperty(PropertyKeyConst.NAMESPACE, props.getNamespaceId());
        // 开源自建 Nacos 的账号密码鉴权；未开启鉴权可注释掉
        if (StringUtils.hasText(props.getUsername())) {
            properties.setProperty(PropertyKeyConst.USERNAME, props.getUsername());
            properties.setProperty(PropertyKeyConst.PASSWORD, props.getPassword());
        }
        // 可选：强制 HTTP 传输模式，与直接调 REST 接口行为一致
        if ("http".equalsIgnoreCase(props.getTransportMode())) {
            properties.setProperty(AiConstants.AI_TRANSPORT_MODE, AiConstants.AI_TRANSPORT_MODE_HTTP);
        }
        return AiFactory.createAiService(properties);
    }

    /**
     * 配置线程池
     * @return
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 2. 核心线程数：线程池创建时候初始化的线程数
        // 建议设置为 CPU 核心数 + 1，或者根据你的并发量调整（例如 5）
        executor.setCorePoolSize(5);

        // 3. 最大线程数：线程池最大的线程数，只有在队列满了之后才会创建超过核心线程数的线程
        // 压缩任务比较耗时（调用LLM），建议稍微大一点
        executor.setMaxPoolSize(10);

        // 4. 队列容量：用来缓冲执行任务的队列
        // 如果并发量大，队列可以设大一点，比如 100-200
        executor.setQueueCapacity(100);

        // 5. 线程名称前缀：方便在日志中排查问题
        executor.setThreadNamePrefix("async-compress-");

        // 6. 拒绝策略：当队列满了且线程数达到最大值时的处理策略
        // CallerRunsPolicy：由调用者所在的线程来执行任务（即不进线程池，在主线程跑）。
        // 这样可以防止任务丢失，但会稍微阻塞主线程。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 7. 等待所有任务结束后再关闭线程池（保证应用关闭时数据不丢失）
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

}
