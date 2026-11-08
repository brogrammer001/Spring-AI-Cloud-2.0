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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfigToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteConfigService remoteConfigService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "系统参数配置的新增、修改、删除。参数包含 operationType(add/update/delete)和参数配置实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult configCrud(SysConfigBo configBo) {
        AjaxResult gate = crudGate(configBo, "configCrud", "参数配置", OPS_CRUD, "configName");
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
        if (configBo.getConfigId() != null) {
            SysConfig sysConfig = new SysConfig();
            BeanUtils.copyProperties(configBo, sysConfig);
            sysConfig.setConfigId(configBo.getConfigId());
            R<Boolean> result = remoteConfigService.updateConfig(sysConfig);
            return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
        }
        if (configBo.getConfigName() == null || configBo.getConfigName().isEmpty()) {
            return formMissing("修改操作必须传入参数键名，请补充", configBo, "configName");
        }
        List<SysConfig> configs = getConfigsByConditions(configBo);
        if (configs.isEmpty()) {
            return AjaxResult.error("参数配置不存在：" + configBo.getConfigName());
        }
        if (configs.size() > 1) {
            return formSelect("查询到 " + configs.size() + " 个参数配置，请选择要修改的配置：",
                configBo, configs,
                SysConfig::getConfigId,
                c -> "参数名称：" + c.getConfigName()
                    + "，键名：" + c.getConfigKey()
                    + "，键值：" + c.getConfigValue(),
                "configId", "参数配置");
        }
        SysConfig sysConfig = new SysConfig();
        BeanUtils.copyProperties(configBo, sysConfig);
        sysConfig.setConfigId(configs.getFirst().getConfigId());
        R<Boolean> result = remoteConfigService.updateConfig(sysConfig);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleConfigDelete(SysConfigBo configBo) {
        if (configBo.getConfigId() != null) {
            R<Boolean> result = remoteConfigService.deleteConfig(configBo.getConfigId());
            return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
        }
        if (configBo.getConfigName() == null || configBo.getConfigName().isEmpty()) {
            return formMissing("删除操作必须传入参数键名，请补充", configBo, "configName");
        }
        List<SysConfig> configs = getConfigsByConditions(configBo);
        if (configs.isEmpty()) {
            return AjaxResult.error("参数配置不存在：" + configBo.getConfigName());
        }
        if (configs.size() > 1) {
            return formSelect("查询到 " + configs.size() + " 个参数配置，请选择要删除的配置：",
                configBo, configs,
                SysConfig::getConfigId,
                c -> "参数名称：" + c.getConfigName()
                    + "，键名：" + c.getConfigKey()
                    + "，键值：" + c.getConfigValue(),
                "configId", "参数配置");
        }
        R<Boolean> result = remoteConfigService.deleteConfig(configs.getFirst().getConfigId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private List<SysConfig> getConfigsByConditions(SysConfigBo configBo) {
        SysConfig query = new SysConfig();
        if (configBo.getConfigName() != null && !configBo.getConfigName().isEmpty()) {
            query.setConfigName(configBo.getConfigName());
        }
        R<List<SysConfig>> result = remoteConfigService.getConfigList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    @Tool(description = "批量新增参数配置。参数：参数配置列表(configList)，每个元素需包含参数名称、参数键名、参数键值等必填信息，可填写系统内置、备注。未填系统内置默认否。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddConfigs(
            @ToolParam(description = "参数配置信息列表") List<SysConfigBo> configList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(configList, "参数配置");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysConfigBo configBo : configList) {
                AjaxResult validateResult = validate(configBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("configList", configList.stream().map(this::toMap).toList());
                return confirmResult("batchAddConfigs", "add", "确认批量新增参数配置",
                    "即将批量新增 " + configList.size() + " 个参数配置，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下参数配置新增失败：");
            for (SysConfigBo configBo : configList) {
                SysConfig sysConfig = new SysConfig();
                BeanUtils.copyProperties(configBo, sysConfig);
                if (sysConfig.getConfigType() == null || sysConfig.getConfigType().isEmpty()) {
                    sysConfig.setConfigType("N");
                }
                R<Boolean> result = remoteConfigService.addConfig(sysConfig);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(configBo.getConfigName());
                } else {
                    batch.addFail(configBo.getConfigName(), result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增参数配置");
    }

    @Tool(description = "批量删除参数配置。参数：参数名称列表(configNames)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeleteConfigs(
            @ToolParam(description = "参数名称列表，例如 [\"用户管理-账号初始密码\"]") List<String> configNames,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(configNames, "参数配置");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("configNames", configNames);
                return confirmResult("batchDeleteConfigs", "delete", "确认批量删除参数配置",
                    "即将批量删除 " + configNames.size() + " 个参数配置：" + joinNames(configNames) + "，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下参数配置删除失败：");
            for (String configName : configNames) {
                SysConfigBo query = new SysConfigBo();
                query.setConfigName(configName);
                List<SysConfig> configs = getConfigsByConditions(query);
                if (configs.isEmpty()) {
                    batch.addFail(configName, "参数配置不存在");
                    continue;
                }
                if (configs.size() > 1) {
                    batch.addFail(configName, "匹配到多条记录，请使用更精确的参数名称或先查询配置ID");
                    continue;
                }
                R<Boolean> result = remoteConfigService.deleteConfig(configs.getFirst().getConfigId());
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(configName);
                } else {
                    batch.addFail(configName, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除参数配置");
    }

    @Tool(description = "批量修改参数配置。参数：参数配置列表(configList)，每个元素需包含参数名称(configName)或配置ID(configId)用于定位，另可填写参数键名、键值、系统内置、备注等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdateConfigs(
            @ToolParam(description = "参数配置信息列表") List<SysConfigBo> configList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(configList, "参数配置");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("configList", configList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdateConfigs", "update", "确认批量修改参数配置",
                    "即将批量修改 " + configList.size() + " 个参数配置，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下参数配置修改失败：");
            for (SysConfigBo configBo : configList) {
                String label = configBo.getConfigName() != null && !configBo.getConfigName().isEmpty()
                    ? configBo.getConfigName() : "ID=" + configBo.getConfigId();
                Long configId = configBo.getConfigId();
                if (configId == null) {
                    if (configBo.getConfigName() == null || configBo.getConfigName().isEmpty()) {
                        batch.addFail(label, "缺少参数名称或配置ID");
                        continue;
                    }
                    List<SysConfig> configs = getConfigsByConditions(configBo);
                    if (configs.isEmpty()) {
                        batch.addFail(label, "参数配置不存在");
                        continue;
                    }
                    if (configs.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带配置ID");
                        continue;
                    }
                    configId = configs.getFirst().getConfigId();
                }

                SysConfig sysConfig = new SysConfig();
                BeanUtils.copyProperties(configBo, sysConfig);
                sysConfig.setConfigId(configId);
                R<Boolean> result = remoteConfigService.updateConfig(sysConfig);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改参数配置");
    }
}
