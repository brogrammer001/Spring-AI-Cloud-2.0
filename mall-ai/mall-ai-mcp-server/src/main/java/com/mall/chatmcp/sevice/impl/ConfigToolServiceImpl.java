package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysConfigBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteConfigService;
import com.mall.system.api.domain.SysConfig;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class ConfigToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteConfigService remoteConfigService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "系统参数配置的新增、修改、删除。参数包含 operationType(add/update/delete)和参数配置实体。")
    public AjaxResult configCrud(SysConfigBo configBo) {
        String operationType = configBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }

        return executeWithErrorHandling(() -> switch (operationType.toLowerCase()) {
            case "add" -> handleConfigAdd(configBo);
            case "update" -> handleConfigUpdate(configBo);
            case "delete" -> handleConfigDelete(configBo);
            default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
        }, "参数配置操作");
    }

    @Tool(description = "查询系统参数配置。参数：参数名称(configName)或参数键名(configKey)，可选。")
    public AjaxResult configQuery(
            @ToolParam(description = "参数名称，可选") String configName,
            @ToolParam(description = "参数键名，可选") String configKey) {
        return executeWithErrorHandling(() -> {
            SysConfig query = new SysConfig();
            if (configName != null && !configName.isEmpty()) {
                query.setConfigName(configName);
            }
            if (configKey != null && !configKey.isEmpty()) {
                query.setConfigKey(configKey);
            }
            R<List<SysConfig>> result = remoteConfigService.getConfigList(query);
            if (result.getCode() == 200 && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    return AjaxResult.error("未查询到相关参数配置");
                }
                StringBuilder sb = new StringBuilder("查询到 " + result.getData().size() + " 个参数配置：\n");
                for (SysConfig c : result.getData()) {
                    sb.append("- 参数名称：").append(c.getConfigName())
                      .append("，键名：").append(c.getConfigKey())
                      .append("，键值：").append(c.getConfigValue())
                      .append("，内置：").append("Y".equals(c.getConfigType()) ? "是" : "否")
                      .append("\n");
                }
                return AjaxResult.success(sb.toString());
            }
            return AjaxResult.error(result.getMsg());
        }, "查询参数配置");
    }

    private AjaxResult handleConfigAdd(SysConfigBo configBo) {
        AjaxResult validateResult = validate(configBo, "sysConfigBo");
        if (validateResult != null) {
            return validateResult;
        }
        SysConfig sysConfig = new SysConfig();
        BeanUtils.copyProperties(configBo, sysConfig);
        if (sysConfig.getConfigType() == null || sysConfig.getConfigType().isEmpty()) {
            sysConfig.setConfigType("N");
        }
        R<Boolean> result = remoteConfigService.addConfig(sysConfig);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleConfigUpdate(SysConfigBo configBo) {
        if (configBo.getConfigKey() == null || configBo.getConfigKey().isEmpty()) {
            return AjaxResult.error("修改操作必须传入参数键名");
        }
        List<SysConfig> configs = getConfigsByConditions(configBo);
        if (configs.isEmpty()) {
            return AjaxResult.error("参数配置不存在：" + configBo.getConfigKey());
        }
        if (configs.size() > 1) {
            return AjaxResult.error("查询到多个参数配置，请补充更多信息后重试：" + formatConfigList(configs));
        }
        SysConfig sysConfig = new SysConfig();
        BeanUtils.copyProperties(configBo, sysConfig);
        sysConfig.setConfigId(configs.getFirst().getConfigId());
        R<Boolean> result = remoteConfigService.updateConfig(sysConfig);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleConfigDelete(SysConfigBo configBo) {
        if (configBo.getConfigKey() == null || configBo.getConfigKey().isEmpty()) {
            return AjaxResult.error("删除操作必须传入参数键名");
        }
        List<SysConfig> configs = getConfigsByConditions(configBo);
        if (configs.isEmpty()) {
            return AjaxResult.error("参数配置不存在：" + configBo.getConfigKey());
        }
        if (configs.size() > 1) {
            return AjaxResult.error("查询到多个参数配置，请补充更多信息后重试：" + formatConfigList(configs));
        }
        R<Boolean> result = remoteConfigService.deleteConfig(configs.getFirst().getConfigId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private List<SysConfig> getConfigsByConditions(SysConfigBo configBo) {
        SysConfig query = new SysConfig();
        if (configBo.getConfigKey() != null && !configBo.getConfigKey().isEmpty()) {
            query.setConfigKey(configBo.getConfigKey());
        }
        if (configBo.getConfigName() != null && !configBo.getConfigName().isEmpty()) {
            query.setConfigName(configBo.getConfigName());
        }
        R<List<SysConfig>> result = remoteConfigService.getConfigList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private String formatConfigList(List<SysConfig> configs) {
        StringBuilder sb = new StringBuilder();
        for (SysConfig c : configs) {
            sb.append("\n- 参数名称：").append(c.getConfigName())
              .append("，键名：").append(c.getConfigKey())
              .append("，键值：").append(c.getConfigValue());
        }
        return sb.toString();
    }
}