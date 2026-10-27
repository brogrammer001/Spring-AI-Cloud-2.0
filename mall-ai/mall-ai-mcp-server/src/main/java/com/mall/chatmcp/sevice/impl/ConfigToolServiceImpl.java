package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysConfigBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteConfigService;
import com.mall.system.api.domain.SysConfig;
import org.springframework.ai.tool.annotation.Tool;
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

    @Tool(description = "系统参数配置的新增、修改、删除。参数包含 operationType(add/update/delete)和参数配置实体。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
    public AjaxResult configCrud(SysConfigBo configBo) {
        AjaxResult gate = crudGate(configBo, "configCrud", "参数配置", OPS_CRUD, "configKey");
        if (gate != null) {
            return gate;
        }
        return executeWithErrorHandling(() -> switch (configBo.getOperationType().toLowerCase()) {
            case "add" -> handleConfigAdd(configBo);
            case "update" -> handleConfigUpdate(configBo);
            case "delete" -> handleConfigDelete(configBo);
            default -> AjaxResult.error("不支持的操作类型：" + configBo.getOperationType() + "，请使用：add、update、delete");
        }, "参数配置操作");
    }

    private AjaxResult handleConfigAdd(SysConfigBo configBo) {
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
            return formMissing("修改操作必须传入参数键名，请补充", configBo, "configKey");
        }
        List<SysConfig> configs = getConfigsByConditions(configBo);
        if (configs.isEmpty()) {
            return AjaxResult.error("参数配置不存在：" + configBo.getConfigKey());
        }
        if (configs.size() > 1) {
            return formMissing("查询到多个参数配置，请补充以下信息以精确定位：" + formatConfigList(configs), configBo);
        }
        SysConfig sysConfig = new SysConfig();
        BeanUtils.copyProperties(configBo, sysConfig);
        sysConfig.setConfigId(configs.getFirst().getConfigId());
        R<Boolean> result = remoteConfigService.updateConfig(sysConfig);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleConfigDelete(SysConfigBo configBo) {
        if (configBo.getConfigKey() == null || configBo.getConfigKey().isEmpty()) {
            return formMissing("删除操作必须传入参数键名，请补充", configBo, "configKey");
        }
        List<SysConfig> configs = getConfigsByConditions(configBo);
        if (configs.isEmpty()) {
            return AjaxResult.error("参数配置不存在：" + configBo.getConfigKey());
        }
        if (configs.size() > 1) {
            return formMissing("查询到多个参数配置，请补充以下信息以精确定位：" + formatConfigList(configs), configBo);
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
