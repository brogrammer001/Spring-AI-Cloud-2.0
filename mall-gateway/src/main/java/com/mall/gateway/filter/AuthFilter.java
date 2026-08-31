package com.mall.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.mall.common.core.constant.CacheConstants;
import com.mall.common.core.constant.HttpStatus;
import com.mall.common.core.constant.SecurityConstants;
import com.mall.common.core.constant.TokenConstants;
import com.mall.common.core.utils.JwtUtils;
import com.mall.common.core.utils.ServletUtils;
import com.mall.common.core.utils.StringUtils;
import com.mall.common.redis.service.RedisService;
import com.mall.gateway.config.properties.IgnoreWhiteProperties;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * 网关鉴权
 * 
 * @author mall
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    // 排除过滤的 uri 地址，nacos自行添加
    @Autowired
    private IgnoreWhiteProperties ignoreWhite;

    @Autowired
    private RedisService redisService;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();

        String url = request.getURI().getPath();
        // 跳过不需要验证的路径
        if (StringUtils.matches(url, ignoreWhite.getWhites()))
        {
            return chain.filter(exchange);
        }
        String token = getToken(request);
        if (StringUtils.isEmpty(token))
        {
            return unauthorizedResponse(exchange, "令牌不能为空");
        }
        Claims claims = JwtUtils.parseToken(token);
        if (claims == null)
        {
            return unauthorizedResponse(exchange, "令牌已过期或验证不正确！");
        }
        String userkey = JwtUtils.getUserKey(claims);
        String userid = JwtUtils.getUserId(claims);
        String username = JwtUtils.getUserName(claims);
        if (StringUtils.isEmpty(userid) || StringUtils.isEmpty(username))
        {
            return unauthorizedResponse(exchange, "令牌验证失败");
        }

        // Redis 校验为阻塞调用，必须切到 boundedElastic 线程池执行：
        // 若直接在 Netty 事件循环线程上调用，会阻塞事件循环导致网关所有请求排队（前端表现为"一直卡着"），
        // 并发刷新时尤为明显。加 3 秒超时快速失败，避免 Redis 抖动时请求无限挂起。
        return Mono.fromCallable(() -> redisService.hasKey(getTokenKey(userkey)))
            .subscribeOn(Schedulers.boundedElastic())
            .timeout(Duration.ofSeconds(3))
            .flatMap(islogin -> {
                if (!islogin)
                {
                    return unauthorizedResponse(exchange, "登录状态已过期");
                }
                // 设置用户信息到请求
                addHeader(mutate, SecurityConstants.USER_KEY, userkey);
                addHeader(mutate, SecurityConstants.DETAILS_USER_ID, userid);
                addHeader(mutate, SecurityConstants.DETAILS_USERNAME, username);
                // 内部请求来源参数清除
                removeHeader(mutate, SecurityConstants.FROM_SOURCE);
                return chain.filter(exchange.mutate().request(mutate.build()).build());
            })
            // Redis 异常/超时 ≠ 未登录：按网关服务异常返回，
            // 避免把 Redis 抖动误判成"登录过期"导致前端误登出
            .onErrorResume(e -> {
                log.error("[鉴权异常处理]请求路径:{},Redis校验异常", url, e);
                return ServletUtils.webFluxResponseWriter(exchange.getResponse(),
                    "鉴权服务繁忙，请稍后重试", HttpStatus.ERROR);
            });
    }

    private void addHeader(ServerHttpRequest.Builder mutate, String name, Object value)
    {
        if (value == null)
        {
            return;
        }
        String valueStr = value.toString();
        String valueEncode = ServletUtils.urlEncode(valueStr);
        mutate.header(name, valueEncode);
    }

    private void removeHeader(ServerHttpRequest.Builder mutate, String name)
    {
        mutate.headers(httpHeaders -> httpHeaders.remove(name)).build();
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg)
    {
        log.error("[鉴权异常处理]请求路径:{},错误信息:{}", exchange.getRequest().getPath(), msg);
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), msg, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 获取缓存key
     */
    private String getTokenKey(String token)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + token;
    }

    /**
     * 获取请求token
     */
    private String getToken(ServerHttpRequest request)
    {
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
        // 如果前端设置了令牌前缀，则裁剪掉前缀
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX))
        {
            token = token.replaceFirst(TokenConstants.PREFIX, StringUtils.EMPTY);
        }
        return token;
    }

    @Override
    public int getOrder()
    {
        return -200;
    }
}