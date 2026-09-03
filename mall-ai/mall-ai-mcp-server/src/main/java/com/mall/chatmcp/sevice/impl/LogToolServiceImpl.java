package com.mall.chatmcp.sevice.impl;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteLogQueryService;
import com.mall.system.api.domain.SysLogininfor;
import com.mall.system.api.domain.SysOperLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteLogQueryService remoteLogQueryService;

    @Tool(description = "查询操作日志。参数：操作人员(operName)、操作模块(title)、操作状态(status: 0正常 1异常)，可选。")
    public AjaxResult operLogQuery(
            @ToolParam(description = "操作人员，可选") String operName,
            @ToolParam(description = "操作模块，可选") String title,
            @ToolParam(description = "操作状态：0正常 1异常，可选") String status) {
        return executeWithErrorHandling(() -> {
            SysOperLog query = new SysOperLog();
            if (operName != null && !operName.isEmpty()) {
                query.setOperName(operName);
            }
            if (title != null && !title.isEmpty()) {
                query.setTitle(title);
            }
            if (status != null && !status.isEmpty()) {
                query.setStatus(Integer.valueOf(status));
            }
            R<List<SysOperLog>> result = remoteLogQueryService.getOperLogList(query);
            if (result.getCode() == 200 && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    return AjaxResult.error("未查询到相关操作日志");
                }
                List<SysOperLog> logs = result.getData();
                StringBuilder sb = new StringBuilder("查询到 " + logs.size() + " 条操作日志：\n");
                int limit = Math.min(10, logs.size());
                for (int i = 0; i < limit; i++) {
                    SysOperLog log = logs.get(i);
                    sb.append(i + 1).append(". 操作人：").append(log.getOperName())
                      .append("，模块：").append(log.getTitle())
                      .append("，类型：").append(formatBusinessType(log.getBusinessType()))
                      .append("，状态：").append(log.getStatus() != null && log.getStatus() == 0 ? "正常" : "异常")
                      .append("，耗时：").append(log.getCostTime() != null ? log.getCostTime() + "ms" : "未知")
                      .append("，时间：").append(log.getOperTime() != null ? log.getOperTime() : "未知")
                      .append("\n");
                }
                if (logs.size() > limit) {
                    sb.append("... 共 ").append(logs.size()).append(" 条");
                }
                return AjaxResult.success(sb.toString());
            }
            return AjaxResult.error(result.getMsg());
        }, "查询操作日志");
    }

    @Tool(description = "查询登录日志。参数：用户账号(userName)、登录状态(status: 0成功 1失败)，可选。")
    public AjaxResult loginLogQuery(
            @ToolParam(description = "用户账号，可选") String userName,
            @ToolParam(description = "登录状态：0成功 1失败，可选") String status) {
        return executeWithErrorHandling(() -> {
            SysLogininfor query = new SysLogininfor();
            if (userName != null && !userName.isEmpty()) {
                query.setUserName(userName);
            }
            if (status != null && !status.isEmpty()) {
                query.setStatus(status);
            }
            R<List<SysLogininfor>> result = remoteLogQueryService.getLogininforList(query);
            if (result.getCode() == 200 && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    return AjaxResult.error("未查询到相关登录日志");
                }
                List<SysLogininfor> logs = result.getData();
                StringBuilder sb = new StringBuilder("查询到 " + logs.size() + " 条登录日志：\n");
                int limit = Math.min(10, logs.size());
                for (int i = 0; i < limit; i++) {
                    SysLogininfor log = logs.get(i);
                    sb.append(i + 1).append(". 用户：").append(log.getUserName())
                      .append("，IP：").append(log.getIpaddr())
                      .append("，状态：").append("0".equals(log.getStatus()) ? "成功" : "失败")
                      .append("，信息：").append(log.getMsg())
                      .append("，时间：").append(log.getAccessTime() != null ? log.getAccessTime() : "未知")
                      .append("\n");
                }
                if (logs.size() > limit) {
                    sb.append("... 共 ").append(logs.size()).append(" 条");
                }
                return AjaxResult.success(sb.toString());
            }
            return AjaxResult.error(result.getMsg());
        }, "查询登录日志");
    }

    @Tool(description = "统计登录失败次数最多的用户。参数：无。")
    public AjaxResult loginFailStats() {
        return executeWithErrorHandling(() -> {
            SysLogininfor query = new SysLogininfor();
            query.setStatus("1");
            R<List<SysLogininfor>> result = remoteLogQueryService.getLogininforList(query);
            if (result.getCode() == 200 && result.getData() != null) {
                List<SysLogininfor> logs = result.getData();
                if (logs.isEmpty()) {
                    return AjaxResult.success("没有登录失败记录");
                }
                // 统计每个用户的失败次数
                java.util.Map<String, Integer> failCount = new java.util.HashMap<>();
                for (SysLogininfor log : logs) {
                    String name = log.getUserName() != null ? log.getUserName() : "未知";
                    failCount.merge(name, 1, Integer::sum);
                }
                // 按失败次数排序
                List<java.util.Map.Entry<String, Integer>> sorted = new java.util.ArrayList<>(failCount.entrySet());
                sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                StringBuilder sb = new StringBuilder("登录失败统计（共 " + logs.size() + " 次失败）：\n");
                int limit = Math.min(10, sorted.size());
                for (int i = 0; i < limit; i++) {
                    sb.append(i + 1).append(". ").append(sorted.get(i).getKey())
                      .append("：").append(sorted.get(i).getValue()).append(" 次\n");
                }
                return AjaxResult.success(sb.toString());
            }
            return AjaxResult.error(result.getMsg());
        }, "统计登录失败");
    }

    private String formatBusinessType(Integer businessType) {
        if (businessType == null) return "未知";
        return switch (businessType) {
            case 0 -> "其它";
            case 1 -> "新增";
            case 2 -> "修改";
            case 3 -> "删除";
            case 4 -> "授权";
            case 5 -> "导出";
            case 6 -> "导入";
            case 7 -> "强退";
            case 8 -> "生成代码";
            case 9 -> "清空数据";
            default -> "未知";
        };
    }
}