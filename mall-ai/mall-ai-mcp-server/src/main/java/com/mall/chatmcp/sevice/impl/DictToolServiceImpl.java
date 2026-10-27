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

    @Tool(description = "字典类型的新增、修改、删除。参数包含 operationType(add/update/delete)和字典类型实体。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
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


    @Tool(description = "字典数据的新增、修改、删除。参数包含 operationType(add/update/delete)和字典数据实体。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
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
        if (dictTypeBo.getDictType() == null || dictTypeBo.getDictType().isEmpty()) {
            return formMissing("修改操作必须传入字典类型编码，请补充", dictTypeBo, "dictType");
        }
        List<SysDictType> dictTypes = getDictTypesByConditions(dictTypeBo);
        if (dictTypes.isEmpty()) {
            return AjaxResult.error("字典类型不存在：" + dictTypeBo.getDictType());
        }
        if (dictTypes.size() > 1) {
            return formMissing("查询到多个字典类型，请补充以下信息以精确定位：" + formatDictTypeList(dictTypes), dictTypeBo);
        }
        SysDictType sysDictType = new SysDictType();
        BeanUtils.copyProperties(dictTypeBo, sysDictType);
        sysDictType.setDictId(dictTypes.getFirst().getDictId());
        R<Boolean> result = remoteDictService.updateDictType(sysDictType);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictTypeDelete(SysDictTypeBo dictTypeBo) {
        if (dictTypeBo.getDictType() == null || dictTypeBo.getDictType().isEmpty()) {
            return formMissing("删除操作必须传入字典类型编码，请补充", dictTypeBo, "dictType");
        }
        List<SysDictType> dictTypes = getDictTypesByConditions(dictTypeBo);
        if (dictTypes.isEmpty()) {
            return AjaxResult.error("字典类型不存在：" + dictTypeBo.getDictType());
        }
        if (dictTypes.size() > 1) {
            return formMissing("查询到多个字典类型，请补充以下信息以精确定位：" + formatDictTypeList(dictTypes), dictTypeBo);
        }
        R<Boolean> result = remoteDictService.deleteDictType(dictTypes.getFirst().getDictId());
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
            return formMissing("查询到多个字典数据，请补充以下信息以精确定位：" + formatDictDataList(dictDatas), dictDataBo);
        }
        SysDictData sysDictData = new SysDictData();
        BeanUtils.copyProperties(dictDataBo, sysDictData);
        sysDictData.setDictCode(dictDatas.getFirst().getDictCode());
        R<Boolean> result = remoteDictService.updateDictData(sysDictData);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleDictDataDelete(SysDictDataBo dictDataBo) {
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
            return formMissing("查询到多个字典数据，请补充以下信息以精确定位：" + formatDictDataList(dictDatas), dictDataBo);
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
