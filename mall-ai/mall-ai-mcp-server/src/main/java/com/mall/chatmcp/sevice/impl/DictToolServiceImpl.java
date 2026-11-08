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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DictToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteDictService remoteDictService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "字典类型的新增、修改、删除。参数包含 operationType(add/update/delete)和字典类型实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult dictTypeCrud(SysDictTypeBo dictTypeBo) {
        AjaxResult gate = crudGate(dictTypeBo, "dictTypeCrud", "字典类型", OPS_CRUD, "dictType");
        if (gate != null) {
            return gate;
        }
        return executeWithErrorHandling(() -> switch (dictTypeBo.getOperationType().toLowerCase()) {
            case "add" -> handleDictTypeAdd(dictTypeBo);
            case "update" -> handleDictTypeUpdate(dictTypeBo);
            case "delete" -> handleDictTypeDelete(dictTypeBo);
            default -> AjaxResult.error("不支持的操作类型：" + dictTypeBo.getOperationType() + "，请使用：add、update、delete");
        }, "字典类型操作");
    }


    @Tool(description = "字典数据的新增、修改、删除。参数包含 operationType(add/update/delete)和字典数据实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult dictDataCrud(SysDictDataBo dictDataBo) {
        AjaxResult gate = crudGate(dictDataBo, "dictDataCrud", "字典数据", OPS_CRUD, "dictType", "dictLabel");
        if (gate != null) {
            return gate;
        }
        return executeWithErrorHandling(() -> switch (dictDataBo.getOperationType().toLowerCase()) {
            case "add" -> handleDictDataAdd(dictDataBo);
            case "update" -> handleDictDataUpdate(dictDataBo);
            case "delete" -> handleDictDataDelete(dictDataBo);
            default -> AjaxResult.error("不支持的操作类型：" + dictDataBo.getOperationType() + "，请使用：add、update、delete");
        }, "字典数据操作");
    }

    private AjaxResult handleDictTypeAdd(SysDictTypeBo dictTypeBo) {
        SysDictType sysDictType = new SysDictType();
        BeanUtils.copyProperties(dictTypeBo, sysDictType);
        if (sysDictType.getStatus() == null || sysDictType.getStatus().isEmpty()) {
            sysDictType.setStatus("0");
        }
        R<Boolean> result = remoteDictService.addDictType(sysDictType);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictTypeUpdate(SysDictTypeBo dictTypeBo) {
        Long dictId = dictTypeBo.getDictId();
        if (dictId == null) {
            if (dictTypeBo.getDictType() == null || dictTypeBo.getDictType().isEmpty()) {
                return formMissing("修改操作必须传入字典类型编码，请补充", dictTypeBo, "dictType");
            }
            List<SysDictType> dictTypes = getDictTypesByConditions(dictTypeBo);
            if (dictTypes.isEmpty()) {
                return AjaxResult.error("字典类型不存在：" + dictTypeBo.getDictType());
            }
            if (dictTypes.size() > 1) {
                return formSelect("查询到 " + dictTypes.size() + " 个字典类型，请选择要修改的字典类型：",
                    dictTypeBo, dictTypes, SysDictType::getDictId, this::dictTypeLabel, "dictId", "字典类型");
            }
            dictId = dictTypes.getFirst().getDictId();
        }
        SysDictType sysDictType = new SysDictType();
        BeanUtils.copyProperties(dictTypeBo, sysDictType);
        sysDictType.setDictId(dictId);
        R<Boolean> result = remoteDictService.updateDictType(sysDictType);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictTypeDelete(SysDictTypeBo dictTypeBo) {
        Long dictId = dictTypeBo.getDictId();
        if (dictId == null) {
            if (dictTypeBo.getDictType() == null || dictTypeBo.getDictType().isEmpty()) {
                return formMissing("删除操作必须传入字典类型编码，请补充", dictTypeBo, "dictType");
            }
            List<SysDictType> dictTypes = getDictTypesByConditions(dictTypeBo);
            if (dictTypes.isEmpty()) {
                return AjaxResult.error("字典类型不存在：" + dictTypeBo.getDictType());
            }
            if (dictTypes.size() > 1) {
                return formSelect("查询到 " + dictTypes.size() + " 个字典类型，请选择要删除的字典类型：",
                    dictTypeBo, dictTypes, SysDictType::getDictId, this::dictTypeLabel, "dictId", "字典类型");
            }
            dictId = dictTypes.getFirst().getDictId();
        }
        R<Boolean> result = remoteDictService.deleteDictType(dictId);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictDataAdd(SysDictDataBo dictDataBo) {
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
        Long dictCode = dictDataBo.getDictCode();
        if (dictCode == null) {
            if (dictDataBo.getDictType() == null || dictDataBo.getDictType().isEmpty()) {
                return formMissing("修改操作必须传入字典类型编码，请补充", dictDataBo, "dictType");
            }
            if (dictDataBo.getDictLabel() == null || dictDataBo.getDictLabel().isEmpty()) {
                return formMissing("修改操作必须传入字典标签，请补充", dictDataBo, "dictLabel");
            }
            List<SysDictData> dictDatas = getDictDatasByConditions(dictDataBo);
            if (dictDatas.isEmpty()) {
                return AjaxResult.error("字典数据不存在");
            }
            if (dictDatas.size() > 1) {
                return formSelect("查询到 " + dictDatas.size() + " 条字典数据，请选择要修改的字典数据：",
                    dictDataBo, dictDatas, SysDictData::getDictCode, this::dictDataLabel, "dictCode", "字典数据");
            }
            dictCode = dictDatas.getFirst().getDictCode();
        }
        SysDictData sysDictData = new SysDictData();
        BeanUtils.copyProperties(dictDataBo, sysDictData);
        sysDictData.setDictCode(dictCode);
        R<Boolean> result = remoteDictService.updateDictData(sysDictData);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictDataDelete(SysDictDataBo dictDataBo) {
        Long dictCode = dictDataBo.getDictCode();
        if (dictCode == null) {
            if (dictDataBo.getDictType() == null || dictDataBo.getDictType().isEmpty()) {
                return formMissing("删除操作必须传入字典类型编码，请补充", dictDataBo, "dictType");
            }
            if (dictDataBo.getDictLabel() == null || dictDataBo.getDictLabel().isEmpty()) {
                return formMissing("删除操作必须传入字典标签，请补充", dictDataBo, "dictLabel");
            }
            List<SysDictData> dictDatas = getDictDatasByConditions(dictDataBo);
            if (dictDatas.isEmpty()) {
                return AjaxResult.error("字典数据不存在");
            }
            if (dictDatas.size() > 1) {
                return formSelect("查询到 " + dictDatas.size() + " 条字典数据，请选择要删除的字典数据：",
                    dictDataBo, dictDatas, SysDictData::getDictCode, this::dictDataLabel, "dictCode", "字典数据");
            }
            dictCode = dictDatas.getFirst().getDictCode();
        }
        R<Boolean> result = remoteDictService.deleteDictData(dictCode);
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

    private String dictTypeLabel(SysDictType d) {
        return "字典名称：" + d.getDictName()
            + "，字典类型：" + d.getDictType()
            + "，状态：" + ("0".equals(d.getStatus()) ? "正常" : "停用");
    }

    private String dictDataLabel(SysDictData d) {
        return "标签：" + d.getDictLabel()
            + "，值：" + d.getDictValue()
            + "，类型：" + d.getDictType();
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

    @Tool(description = "批量新增字典类型。参数：字典类型列表(dictTypeList)，每个元素需包含字典名称、字典类型编码等必填信息，可填写状态、备注。未填状态默认为正常。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddDictTypes(
            @ToolParam(description = "字典类型信息列表") List<SysDictTypeBo> dictTypeList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(dictTypeList, "字典类型");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysDictTypeBo dictTypeBo : dictTypeList) {
                AjaxResult validateResult = validate(dictTypeBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("dictTypeList", dictTypeList.stream().map(this::toMap).toList());
                return confirmResult("batchAddDictTypes", "add", "确认批量新增字典类型",
                    "即将批量新增 " + dictTypeList.size() + " 个字典类型，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下字典类型新增失败：");
            for (SysDictTypeBo dictTypeBo : dictTypeList) {
                SysDictType sysDictType = new SysDictType();
                BeanUtils.copyProperties(dictTypeBo, sysDictType);
                if (sysDictType.getStatus() == null || sysDictType.getStatus().isEmpty()) {
                    sysDictType.setStatus("0");
                }
                R<Boolean> result = remoteDictService.addDictType(sysDictType);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(dictTypeBo.getDictType());
                } else {
                    batch.addFail(dictTypeBo.getDictType(), result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增字典类型");
    }

    @Tool(description = "批量删除字典类型。参数：字典类型编码列表(dictTypes)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeleteDictTypes(
            @ToolParam(description = "字典类型编码列表，例如 [\"sys_user_sex\",\"sys_job_status\"]") List<String> dictTypes,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(dictTypes, "字典类型");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("dictTypes", dictTypes);
                return confirmResult("batchDeleteDictTypes", "delete", "确认批量删除字典类型",
                    "即将批量删除 " + dictTypes.size() + " 个字典类型：" + joinNames(dictTypes) + "，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下字典类型删除失败：");
            for (String dictType : dictTypes) {
                List<SysDictType> dictTypeList = getDictTypesByConditions(buildDictTypeQuery(dictType));
                if (dictTypeList.isEmpty()) {
                    batch.addFail(dictType, "字典类型不存在");
                    continue;
                }
                if (dictTypeList.size() > 1) {
                    batch.addFail(dictType, "匹配到多条记录，请使用更精确的字典类型编码");
                    continue;
                }
                R<Boolean> result = remoteDictService.deleteDictType(dictTypeList.getFirst().getDictId());
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(dictType);
                } else {
                    batch.addFail(dictType, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除字典类型");
    }

    @Tool(description = "批量修改字典类型。参数：字典类型列表(dictTypeList)，每个元素需包含字典类型编码(dictType)或字典ID(dictId)用于定位，另可填写字典名称、状态、备注等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdateDictTypes(
            @ToolParam(description = "字典类型信息列表") List<SysDictTypeBo> dictTypeList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(dictTypeList, "字典类型");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("dictTypeList", dictTypeList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdateDictTypes", "update", "确认批量修改字典类型",
                    "即将批量修改 " + dictTypeList.size() + " 个字典类型，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下字典类型修改失败：");
            for (SysDictTypeBo dictTypeBo : dictTypeList) {
                String label = dictTypeBo.getDictType() != null && !dictTypeBo.getDictType().isEmpty()
                    ? dictTypeBo.getDictType() : "ID=" + dictTypeBo.getDictId();
                Long dictId = dictTypeBo.getDictId();
                if (dictId == null) {
                    if (dictTypeBo.getDictType() == null || dictTypeBo.getDictType().isEmpty()) {
                        batch.addFail(label, "缺少字典类型编码或字典ID");
                        continue;
                    }
                    List<SysDictType> dictTypes = getDictTypesByConditions(dictTypeBo);
                    if (dictTypes.isEmpty()) {
                        batch.addFail(label, "字典类型不存在");
                        continue;
                    }
                    if (dictTypes.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带字典ID");
                        continue;
                    }
                    dictId = dictTypes.getFirst().getDictId();
                }

                SysDictType sysDictType = new SysDictType();
                BeanUtils.copyProperties(dictTypeBo, sysDictType);
                sysDictType.setDictId(dictId);
                R<Boolean> result = remoteDictService.updateDictType(sysDictType);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改字典类型");
    }

    @Tool(description = "批量新增字典数据。参数：字典数据列表(dictDataList)，每个元素需包含字典类型编码(dictType)、字典标签、字典键值等必填信息，可填写排序、状态、备注。未填排序默认0、状态默认正常。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddDictDatas(
            @ToolParam(description = "字典数据信息列表") List<SysDictDataBo> dictDataList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(dictDataList, "字典数据");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysDictDataBo dictDataBo : dictDataList) {
                AjaxResult validateResult = validate(dictDataBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("dictDataList", dictDataList.stream().map(this::toMap).toList());
                return confirmResult("batchAddDictDatas", "add", "确认批量新增字典数据",
                    "即将批量新增 " + dictDataList.size() + " 条字典数据，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下字典数据新增失败：");
            for (SysDictDataBo dictDataBo : dictDataList) {
                SysDictData sysDictData = new SysDictData();
                BeanUtils.copyProperties(dictDataBo, sysDictData);
                if (sysDictData.getStatus() == null || sysDictData.getStatus().isEmpty()) {
                    sysDictData.setStatus("0");
                }
                if (sysDictData.getDictSort() == null) {
                    sysDictData.setDictSort(0L);
                }
                String label = (dictDataBo.getDictType() != null ? dictDataBo.getDictType() : "未知类型")
                    + "/" + dictDataBo.getDictLabel();
                R<Boolean> result = remoteDictService.addDictData(sysDictData);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增字典数据");
    }

    @Tool(description = "批量删除字典数据。参数：字典数据列表(dictDataList)，每个元素需包含字典类型编码(dictType)和字典标签(dictLabel)，或直接携带字典编码(dictCode)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeleteDictDatas(
            @ToolParam(description = "字典数据信息列表") List<SysDictDataBo> dictDataList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(dictDataList, "字典数据");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("dictDataList", dictDataList.stream().map(this::toMap).toList());
                return confirmResult("batchDeleteDictDatas", "delete", "确认批量删除字典数据",
                    "即将批量删除 " + dictDataList.size() + " 条字典数据，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下字典数据删除失败：");
            for (SysDictDataBo dataBo : dictDataList) {
                String label = (dataBo.getDictType() != null ? dataBo.getDictType() : "未知类型")
                    + "/" + (dataBo.getDictLabel() != null ? dataBo.getDictLabel() : "ID=" + dataBo.getDictCode());
                Long dictCode = dataBo.getDictCode();
                if (dictCode == null) {
                    if (dataBo.getDictType() == null || dataBo.getDictType().isEmpty()
                        || dataBo.getDictLabel() == null || dataBo.getDictLabel().isEmpty()) {
                        batch.addFail(label, "缺少字典类型编码和字典标签");
                        continue;
                    }
                    List<SysDictData> dictDatas = getDictDatasByConditions(dataBo);
                    if (dictDatas.isEmpty()) {
                        batch.addFail(label, "字典数据不存在");
                        continue;
                    }
                    if (dictDatas.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带字典编码");
                        continue;
                    }
                    dictCode = dictDatas.getFirst().getDictCode();
                }
                R<Boolean> result = remoteDictService.deleteDictData(dictCode);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除字典数据");
    }

    @Tool(description = "批量修改字典数据。参数：字典数据列表(dictDataList)，每个元素需包含字典编码(dictCode)，或字典类型编码(dictType)加字典标签(dictLabel)用于定位，另可填写键值、排序、状态、备注等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdateDictDatas(
            @ToolParam(description = "字典数据信息列表") List<SysDictDataBo> dictDataList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(dictDataList, "字典数据");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("dictDataList", dictDataList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdateDictDatas", "update", "确认批量修改字典数据",
                    "即将批量修改 " + dictDataList.size() + " 条字典数据，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下字典数据修改失败：");
            for (SysDictDataBo dataBo : dictDataList) {
                String label = (dataBo.getDictType() != null ? dataBo.getDictType() : "未知类型")
                    + "/" + (dataBo.getDictLabel() != null ? dataBo.getDictLabel() : "ID=" + dataBo.getDictCode());
                Long dictCode = dataBo.getDictCode();
                if (dictCode == null) {
                    if (dataBo.getDictType() == null || dataBo.getDictType().isEmpty()
                        || dataBo.getDictLabel() == null || dataBo.getDictLabel().isEmpty()) {
                        batch.addFail(label, "缺少字典类型编码和字典标签");
                        continue;
                    }
                    List<SysDictData> dictDatas = getDictDatasByConditions(dataBo);
                    if (dictDatas.isEmpty()) {
                        batch.addFail(label, "字典数据不存在");
                        continue;
                    }
                    if (dictDatas.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带字典编码");
                        continue;
                    }
                    dictCode = dictDatas.getFirst().getDictCode();
                }

                SysDictData sysDictData = new SysDictData();
                BeanUtils.copyProperties(dataBo, sysDictData);
                sysDictData.setDictCode(dictCode);
                R<Boolean> result = remoteDictService.updateDictData(sysDictData);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改字典数据");
    }

    private SysDictTypeBo buildDictTypeQuery(String dictType) {
        SysDictTypeBo query = new SysDictTypeBo();
        query.setDictType(dictType);
        return query;
    }
}
