package com.mall.auth.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.constant.SecurityConstants;
import com.mall.common.core.utils.StringUtils;
import com.mall.common.core.utils.ip.IpUtils;
import com.mall.system.api.RemoteLogService;
import com.mall.system.api.domain.SysLogininfor;

/**
 * 记录日志方法
 *
 * @author mall
 */
@Component
public class SysRecordLogService
{
    private static final Logger log = LoggerFactory.getLogger(SysRecordLogService.class);

    @Autowired
    private RemoteLogService remoteLogService;

    /**
     * 日志写入专用线程池：登录/登出日志走远程 Feign + DB 插入，
     * 同步执行会拖慢接口响应（登出接口实测卡慢的根因）。
     * 有界队列 + 丢弃策略：日志是辅助数据，积压时宁可丢弃也不阻塞主流程。
     */
    private final ExecutorService logExecutor = new ThreadPoolExecutor(
        1, 2, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(200),
        r -> {
            Thread t = new Thread(r, "login-log-writer");
            t.setDaemon(true);
            return t;
        },
        new ThreadPoolExecutor.DiscardOldestPolicy());

    /**
     * 记录登录信息（异步写入，不阻塞调用方）
     *
     * @param username 用户名
     * @param status 状态
     * @param message 消息内容
     */
    public void recordLogininfor(String username, String status, String message)
    {
        SysLogininfor logininfor = new SysLogininfor();
        logininfor.setUserName(username);
        // IP 取自请求上下文（thread-local），必须在当前请求线程内取好，再提交异步任务
        logininfor.setIpaddr(IpUtils.getIpAddr());
        logininfor.setMsg(message);
        // 日志状态
        if (StringUtils.equalsAny(status, Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER))
        {
            logininfor.setStatus(Constants.LOGIN_SUCCESS_STATUS);
        }
        else if (Constants.LOGIN_FAIL.equals(status))
        {
            logininfor.setStatus(Constants.LOGIN_FAIL_STATUS);
        }
        logExecutor.execute(() -> {
            try
            {
                remoteLogService.saveLogininfor(logininfor, SecurityConstants.INNER);
            }
            catch (Exception e)
            {
                // 日志写失败不影响业务，仅记录告警
                log.warn("登录日志记录失败: username={}, status={}", username, status, e);
            }
        });
    }
}
