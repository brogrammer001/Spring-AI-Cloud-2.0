package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysDictDataBo;
import com.mall.chatmcp.bo.SysDictTypeBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteDictService;
import com.mall.system.api.domain.SysDictData;
import com.mall.system.api.domain.SysDictType;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class DictToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteDictService remoteDictService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "字典类型的新增、修改、删除。参数包含 operationType(add/update/delete)和字典类型实体。")
    public AjaxResult dictTypeCrud(SysDictTypeBo dictTypeBo) {
        String operationType = dictTypeBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }

        return executeWithErrorHandling(() -> switch (operationType.toLowerCase()) {
            case "add" -> handleDictTypeAdd(dictTypeBo);
            case "update" -> handleDictTypeUpdate(dictTypeBo);
            case "delete" -> handleDictTypeDelete(dictTypeBo);
            default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
        }, "字典类型操作");
    }

    @Tool(description = "查询字典类型列表。参数：字典名称(dictName)或字典类型编码(dictType)，可选。")
    public AjaxResult dictTypeQuery(
            @ToolParam(description = "字典名称，可选") String dictName,
            @ToolParam(description = "字典类型编码，可选") String dictType) {
        return executeWithErrorHandling(() -> {
            SysDictType query = new SysDictType();
            if (dictName != null && !dictName.isEmpty()) {
                query.setDictName(dictName);
            }
            if (dictType != null && !dictType.isEmpty()) {
                query.setDictType(dictType);
            }
            R<List<SysDictType>> result = remoteDictService.getDictTypeList(query);
            if (result.getCode() == 200 && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    return AjaxResult.error("未查询到相关字典类型");
                }
                StringBuilder sb = new StringBuilder("查询到 " + result.getData().size() + " 个字典类型：\n");
                for (SysDictType d : result.getData()) {
                    sb.append("- 字典名称：").append(d.getDictName())
                      .append("，字典类型：").append(d.getDictType())
                      .append("，状态：").append("0".equals(d.getStatus()) ? "正常" : "停用")
                      .append("\n");
                }
                return AjaxResult.success(sb.toString());
            }
            return AjaxResult.error(result.getMsg());
        }, "查询字典类型");
    }

    @Tool(description = "字典数据的新增、修改、删除。参数包含 operationType(add/update/delete)和字典数据实体。")
    public AjaxResult dictDataCrud(SysDictDataBo dictDataBo) {
        String operationType = dictDataBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }

        return executeWithErrorHandling(() -> switch (operationType.toLowerCase()) {
            case "add" -> handleDictDataAdd(dictDataBo);
            case "update" -> handleDictDataUpdate(dictDataBo);
            case "delete" -> handleDictDataDelete(dictDataBo);
            default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
        }, "字典数据操作");
    }

    @Tool(description = "查询字典数据列表。参数：字典类型编码(dictType)必填，字典标签(dictLabel)可选。")
    public AjaxResult dictDataQuery(
            @ToolParam(description = "字典类型编码，例如 sys_user_sex") String dictType,
            @ToolParam(description = "字典标签，可选") String dictLabel) {
        return executeWithErrorHandling(() -> {
            if (dictType == null || dictType.isEmpty()) {
                return AjaxResult.error("字典类型编码不能为空");
            }
            SysDictData query = new SysDictData();
            query.setDictType(dictType);
            if (dictLabel != null && !dictLabel.isEmpty()) {
                query.setDictLabel(dictLabel);
            }
            R<List<SysDictData>> result = remoteDictService.getDictDataList(query);
            if (result.getCode() == 200 && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    return AjaxResult.error("未查询到字典类型为 " + dictType + " 的字典数据");
                }
                StringBuilder sb = new StringBuilder("字典类型 " + dictType + " 的字典项（共 " + result.getData().size() + " 个）：\n");
                for (SysDictData d : result.getData()) {
                    sb.append("- 标签：").append(d.getDictLabel())
                      .append("，值：").append(d.getDictValue())
                      .append("，排序：").append(d.getDictSort() != null ? d.getDictSort() : 0)
                      .append("，状态：").append("0".equals(d.getStatus()) ? "正常" : "停用")
                      .append("\n");
                }
                return AjaxResult.success(sb.toString());
            }
            return AjaxResult.error(result.getMsg());
        }, "查询字典数据");
    }

    private AjaxResult handleDictTypeAdd(SysDictTypeBo dictTypeBo) {
        AjaxResult validateResult = validate(dictTypeBo, "sysDictTypeBo");
        if (validateResult != null) {
            return validateResult;
        }
        SysDictType sysDictType = new SysDictType();
        BeanUtils.copyProperties(dictTypeBo, sysDictType);
        if (sysDictType.getStatus() == null || sysDictType.getStatus().isEmpty()) {
            sysDictType.setStatus("0");
        }
        R<Boolean> result = remoteDictService.addDictType(sysDictType);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictTypeUpdate(SysDictTypeBo dictTypeBo) {
        if (dictTypeBo.getDictType() == null || dictTypeBo.getDictType().isEmpty()) {
            return AjaxResult.error("修改操作必须传入字典类型编码");
        }
        List<SysDictType> dictTypes = getDictTypesByConditions(dictTypeBo);
        if (dictTypes.isEmpty()) {
            return AjaxResult.error("字典类型不存在：" + dictTypeBo.getDictType());
        }
        if (dictTypes.size() > 1) {
            return AjaxResult.error("查询到多个字典类型，请补充更多信息后重试：" + formatDictTypeList(dictTypes));
        }
        SysDictType sysDictType = new SysDictType();
        BeanUtils.copyProperties(dictTypeBo, sysDictType);
        sysDictType.setDictId(dictTypes.getFirst().getDictId());
        R<Boolean> result = remoteDictService.updateDictType(sysDictType);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictTypeDelete(SysDictTypeBo dictTypeBo) {
        if (dictTypeBo.getDictType() == null || dictTypeBo.getDictType().isEmpty()) {
            return AjaxResult.error("删除操作必须传入字典类型编码");
        }
        List<SysDictType> dictTypes = getDictTypesByConditions(dictTypeBo);
        if (dictTypes.isEmpty()) {
            return AjaxResult.error("字典类型不存在：" + dictTypeBo.getDictType());
        }
        if (dictTypes.size() > 1) {
            return AjaxResult.error("查询到多个字典类型，请补充更多信息后重试：" + formatDictTypeList(dictTypes));
        }
        R<Boolean> result = remoteDictService.deleteDictType(dictTypes.getFirst().getDictId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictDataAdd(SysDictDataBo dictDataBo) {
        AjaxResult validateResult = validate(dictDataBo, "sysDictDataBo");
        if (validateResult != null) {
            return validateResult;
        }
        SysDictData sysDictData = new SysDictData();
        BeanUtils.copyProperties(dictDataBo, sysDictData);
        if (sysDictData.getStatus() == null || sysDictData.getStatus().isEmpty()) {
            sysDictData.setStatus("0");
        }
        if (sysDictData.getDictSort() == null) {
            sysDictData.setDictSort(0L);
        }
        R<Boolean> result = remoteDictService.addDictData(sysDictData);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictDataUpdate(SysDictDataBo dictDataBo) {
        if (dictDataBo.getDictType() == null || dictDataBo.getDictType().isEmpty()) {
            return AjaxResult.error("修改操作必须传入字典类型编码");
        }
        if (dictDataBo.getDictLabel() == null || dictDataBo.getDictLabel().isEmpty()) {
            return AjaxResult.error("修改操作必须传入字典标签");
        }
        List<SysDictData> dictDatas = getDictDatasByConditions(dictDataBo);
        if (dictDatas.isEmpty()) {
            return AjaxResult.error("字典数据不存在");
        }
        if (dictDatas.size() > 1) {
            return AjaxResult.error("查询到多个字典数据，请补充更多信息后重试：" + formatDictDataList(dictDatas));
        }
        SysDictData sysDictData = new SysDictData();
        BeanUtils.copyProperties(dictDataBo, sysDictData);
        sysDictData.setDictCode(dictDatas.getFirst().getDictCode());
        R<Boolean> result = remoteDictService.updateDictData(sysDictData);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictDataDelete(SysDictDataBo dictDataBo) {
        if (dictDataBo.getDictType() == null || dictDataBo.getDictType().isEmpty()) {
            return AjaxResult.error("删除操作必须传入字典类型编码");
        }
        if (dictDataBo.getDictLabel() == null || dictDataBo.getDictLabel().isEmpty()) {
            return AjaxResult.error("删除操作必须传入字典标签");
        }
        List<SysDictData> dictDatas = getDictDatasByConditions(dictDataBo);
        if (dictDatas.isEmpty()) {
            return AjaxResult.error("字典数据不存在");
        }
        if (dictDatas.size() > 1) {
            return AjaxResult.error("查询到多个字典数据，请补充更多信息后重试：" + formatDictDataList(dictDatas));
        }
        R<Boolean> result = remoteDictService.deleteDictData(dictDatas.getFirst().getDictCode());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private List<SysDictType> getDictTypesByConditions(SysDictTypeBo dictTypeBo) {
        SysDictType query = new SysDictType();
        if (dictTypeBo.getDictType() != null && !dictTypeBo.getDictType().isEmpty()) {
            query.setDictType(dictTypeBo.getDictType());
        }
        if (dictTypeBo.getDictName() != null && !dictTypeBo.getDictName().isEmpty()) {
            query.setDictName(dictTypeBo.getDictName());
        }
        R<List<SysDictType>> result = remoteDictService.getDictTypeList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<SysDictData> getDictDatasByConditions(SysDictDataBo dictDataBo) {
        SysDictData query = new SysDictData();
        if (dictDataBo.getDictType() != null && !dictDataBo.getDictType().isEmpty()) {
            query.setDictType(dictDataBo.getDictType());
        }
        if (dictDataBo.getDictLabel() != null && !dictDataBo.getDictLabel().isEmpty()) {
            query.setDictLabel(dictDataBo.getDictLabel());
        }
        if (dictDataBo.getDictValue() != null && !dictDataBo.getDictValue().isEmpty()) {
            query.setDictValue(dictDataBo.getDictValue());
        }
        R<List<SysDictData>> result = remoteDictService.getDictDataList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private String formatDictTypeList(List<SysDictType> dictTypes) {
        StringBuilder sb = new StringBuilder();
        for (SysDictType d : dictTypes) {
            sb.append("\n- 字典名称：").append(d.getDictName())
              .append("，字典类型：").append(d.getDictType())
              .append("，状态：").append("0".equals(d.getStatus()) ? "正常" : "停用");
        }
        return sb.toString();
    }

    private String formatDictDataList(List<SysDictData> dictDatas) {
        StringBuilder sb = new StringBuilder();
        for (SysDictData d : dictDatas) {
            sb.append("\n- 标签：").append(d.getDictLabel())
              .append("，值：").append(d.getDictValue())
              .append("，类型：").append(d.getDictType());
        }
        return sb.toString();
    }
}