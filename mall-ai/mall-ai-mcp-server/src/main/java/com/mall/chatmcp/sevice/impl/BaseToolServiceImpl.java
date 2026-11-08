package com.mall.chatmcp.sevice.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.chatmcp.bo.BaseConfirmBo;
import com.mall.chatmcp.sevice.BaseToolService;
import com.mall.common.core.annotation.FieldRemark;
import com.mall.common.core.web.domain.AjaxResult;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public abstract class BaseToolServiceImpl implements BaseToolService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Validator validator;

    /** 表单响应码：前端据此渲染输入框 */
    public static final int FORM_CODE = 8002;

    public static final String CONFIRMED_MSG = "首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。";

    /** 确认响应码：前端据此渲染同意/取消按钮 */
    public static final int CONFIRM_CODE = 8003;

    /** 批量操作单次最多处理条数，防止超大列表导致长时间执行 */
    protected static final int MAX_BATCH_SIZE = 100;

    /** 常规增删改操作选项 */
    protected static final String[][] OPS_CRUD = {
        {"add", "新增"}, {"update", "修改"}, {"delete", "删除"}
    };

    /** 操作类型枚举：操作码 -> 中文标签 */
    private enum OperationType {
        ADD("add", "新增"),
        UPDATE("update", "修改"),
        DELETE("delete", "删除"),
        CHANGE_STATUS("changestatus", "状态修改"),
        RUN("run", "立即执行"),
        AUTH("auth", "分配");

        private final String code;
        private final String label;

        OperationType(String code, String label) {
            this.code = code;
            this.label = label;
        }

        /** 按操作码获取中文标签，未匹配时返回原操作码 */
        public static String labelOf(String code) {
            if (code == null) {
                return null;
            }
            for (OperationType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type.label;
                }
            }
            return code;
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * 参数校验：失败时返回 type=form 的结构化响应（前端按 fields 渲染输入框），成功返回 null
     */
    protected <T> AjaxResult validate(T obj) {
        BindingResult bindingResult = new BeanPropertyBindingResult(obj, obj.getClass().getSimpleName());
        validator.validate(obj, bindingResult);
        if (bindingResult.hasErrors()) {
            Map<String, Map<String, Object>> fieldMap = new LinkedHashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, Object> field = fieldMap.get(error.getField());
                if (field == null) {
                    field = buildField(obj, error.getField(), error.getDefaultMessage());
                    fieldMap.put(error.getField(), field);
                }
                if (Arrays.asList(error.getCodes()).contains("Size")) {
                    addSizeLimits(field, obj, error.getField());
                }
            }
            logger.warn("参数校验失败: {}", bindingResult.getAllErrors());
            return formResult("请补充操作信息", "以下必填信息不完整，请补充后提交",
                new ArrayList<>(fieldMap.values()), toMap(obj));
        }
        return null;
    }

    /**
     * 增删改通用流程：操作类型选择（表单）→ 必填字段补齐（表单）→ 用户确认（confirm）。
     * 返回 null 表示已确认且字段齐全，可执行具体操作。
     *
     * @param ops            支持的操作选项（用于选择表单与非法操作提示）
     * @param requiredFields update/delete 操作的定位必填字段（add 的必填由 Bean Validation 校验）
     */
    protected AjaxResult crudGate(Object bo, String tool, String entity, String[][] ops, String... requiredFields) {
        String operationType = (String) fieldValue(bo, "operationType");
        if (operationType == null || operationType.isEmpty()) {
            return operationForm(bo, ops);
        }
        switch (operationType.toLowerCase()) {
            case "add" -> {
                AjaxResult validateResult = validate(bo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            case "update", "delete" -> {
                if (!hasRecordId(bo)) {
                    for (String field : requiredFields) {
                        if (isEmptyValue(fieldValue(bo, field))) {
                            return formMissing(opLabel(operationType) + "操作必须传入"
                                + resolveLabel(bo, field) + "，请补充", bo, field);
                        }
                    }
                }
            }
            default -> {
                return AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：" + joinOpValues(ops));
            }
        }
        if (!isConfirmed(bo)) {
            return confirmCrud(tool, entity, operationType, toMap(bo));
        }
        return null;
    }

    /**
     * 表单响应：前端按 fields 渲染输入框，提交时将用户填写值与 values/arguments 合并后重新调用工具
     */
    protected AjaxResult formResult(String title, String message, List<Map<String, Object>> fields, Map<String, Object> values) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "form");
        data.put("title", title);
        data.put("message", message);
        data.put("fields", fields);
        data.put("values", values != null ? values : new LinkedHashMap<>());
        data.put("submitText", "提交");
        return new AjaxResult(FORM_CODE, "请补充操作信息", data);
    }

    protected AjaxResult formResult(String title, String message, List<Map<String, Object>> fields, Object bo) {
        return formResult(title, message, fields, toMap(bo));
    }

    /**
     * 缺字段表单：按字段名列表生成输入框，values 取 BO 已有值（前端回填）
     */
    protected AjaxResult formMissing(String message, Object bo, String... fieldNames) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (String name : fieldNames) {
            fields.add(buildField(bo, name, null));
        }
        return formResult("请补充操作信息", message, fields, bo);
    }

    protected AjaxResult formMissing(String message, Map<String, Object> values, String... fieldNames) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (String name : fieldNames) {
            fields.add(buildField(null, name, null));
        }
        return formResult("请补充操作信息", message, fields, values);
    }

    /**
     * 单选表单：查询到多条匹配记录时，前端渲染单选框让用户选择一条。
     * option.value 为记录主键，option.label 为记录的可读描述。
     * 用户选择后，前端将 {selectFieldName: selectedId} 合并进 arguments 重新调用工具。
     */
    protected <T> AjaxResult formSelect(String message, Object bo, List<T> records,
                                        Function<T, Object> idGetter,
                                        Function<T, String> labelGetter,
                                        String selectFieldName,
                                        String selectFieldLabel) {
        if (records == null || records.isEmpty()) {
            return AjaxResult.error(message);
        }
        List<Map<String, Object>> opts = new ArrayList<>();
        for (T record : records) {
            Map<String, Object> opt = new LinkedHashMap<>();
            opt.put("value", idGetter.apply(record));
            opt.put("label", labelGetter.apply(record));
            opts.add(opt);
        }
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", selectFieldName);
        field.put("label", selectFieldLabel);
        field.put("inputType", "radio");
        field.put("required", true);
        field.put("options", opts);
        field.put("message", message);
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field);
        return formResult("请选择要操作的记录", message, fields, bo);
    }

    /**
     * 是否已携带记录主键（单选回传），携带时跳过名称必填校验与重复查询
     */
    protected boolean hasRecordId(Object bo) {
        if (bo == null) {
            return false;
        }
        for (Class<?> c = bo.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                String name = f.getName();
                if ((name.endsWith("Id") || "dictCode".equals(name)) && !isEmptyValue(fieldValue(bo, name))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 操作类型选择表单（operationType 缺失时使用）
     */
    protected AjaxResult operationForm(Object bo, String[][] options) {
        List<Map<String, Object>> opts = new ArrayList<>();
        for (String[] option : options) {
            Map<String, Object> opt = new LinkedHashMap<>();
            opt.put("value", option[0]);
            opt.put("label", option[1]);
            opts.add(opt);
        }
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", "operationType");
        field.put("label", "操作类型");
        field.put("inputType", "select");
        field.put("required", true);
        field.put("options", opts);
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field);
        return formResult("请选择操作类型", "请选择要执行的操作类型", fields, bo);
    }

    /**
     * 确认响应：前端渲染同意/取消按钮，用户点击同意后携带 arguments（含 confirmed=true）重新调用工具
     */
    protected AjaxResult confirmResult(String tool, String operationType, String title, String message, Map<String, Object> arguments) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (arguments != null) {
            args.putAll(arguments);
        }
        args.put("confirmed", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "confirm");
        data.put("tool", tool);
        data.put("operationType", operationType);
        data.put("title", title);
        data.put("message", message);
        data.put("arguments", args);
        data.put("confirmText", "同意");
        data.put("cancelText", "取消");
        return new AjaxResult(CONFIRM_CODE, "请确认操作", data);
    }

    /**
     * 增删改确认（自动生成标题与提示语）
     */
    protected AjaxResult confirmCrud(String tool, String entity, String operationType, Map<String, Object> arguments) {
        String op = opLabel(operationType);
        return confirmResult(tool, operationType, "确认" + op + entity,
            "即将对" + entity + "执行「" + op + "」操作，请确认是否继续。", arguments);
    }

    /**
     * 判断用户是否已确认（前端点击同意按钮后 confirmed=true）
     */
    protected boolean isConfirmed(Object bo) {
        return bo instanceof BaseConfirmBo base && Boolean.TRUE.equals(base.getConfirmed());
    }

    /**
     * BO 转 Map（剔除 null 值与 confirmed），用于表单 values 回填与确认 arguments 重放
     */
    protected Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> raw = objectMapper.convertValue(obj, new TypeReference<>() {});
            raw.remove("confirmed");
            raw.values().removeIf(Objects::isNull);
            return raw;
        } catch (Exception e) {
            logger.warn("BO转Map失败: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    protected String opLabel(String operationType) {
        String label = OperationType.labelOf(operationType);
        return label == null ? "操作" : label;
    }

    /**
     * 构建表单字段：label 动态取 BO 字段上的 @JsonPropertyDescription 描述（首个分隔符前的短语），取不到则用字段名
     */
    private Map<String, Object> buildField(Object bo, String name, String message) {
        String label = resolveLabel(bo, name);
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("label", label);
        field.put("inputType", "text");
        field.put("required", true);
        field.put("placeholder", "请输入" + label);
        if (message != null) {
            field.put("message", message);
        }
        return field;
    }

    /**
     * 沿父类链查找 BO 字段定义
     */
    private Field findField(Object bo, String name) {
        if (bo == null) {
            return null;
        }
        for (Class<?> c = bo.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 字段定义在父类中，继续向上查找
            }
        }
        return null;
    }

    /**
     * 反射读取 BO 字段的 @JsonPropertyDescription，例如 "用户账号，例如 zhangsan" -> "用户账号"
     */
    private String resolveLabel(Object bo, String name) {
        if (bo != null) {
            for (Class<?> c = bo.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    FieldRemark desc = c.getDeclaredField(name).getAnnotation(FieldRemark.class);
                    if (desc != null && !desc.name().isBlank()) {
                        return desc.name();
                    }
                    break;
                } catch (NoSuchFieldException ignored) {
                    // 字段定义在父类中，继续向上查找
                }
            }
        }
        return name;
    }

    /**
     * 在候选记录中匹配前端单选回传的主键，匹配不到返回 null
     */
    protected <T> Long matchSelectedId(List<T> records, Long selectedId, Function<T, Long> idGetter) {
        if (selectedId == null || records == null) {
            return null;
        }
        for (T record : records) {
            if (selectedId.equals(idGetter.apply(record))) {
                return selectedId;
            }
        }
        return null;
    }

    /**
     * 为校验失败的字段补充 @Size 长度限制（仅校验不通过时返回）
     */
    private void addSizeLimits(Map<String, Object> field, Object bo, String name) {
        Field fieldDef = findField(bo, name);
        Size size = fieldDef != null ? fieldDef.getAnnotation(Size.class) : null;
        if (size == null) {
            return;
        }
        if (size.min() > 0) {
            field.put("minLength", size.min());
        }
        if (size.max() < Integer.MAX_VALUE) {
            field.put("maxLength", size.max());
        }
    }

    /** 反射读取 BO 字段值（沿父类链查找），取不到返回 null */
    private Object fieldValue(Object bo, String name) {
        if (bo == null) {
            return null;
        }
        for (Class<?> c = bo.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                var field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(bo);
            } catch (NoSuchFieldException ignored) {
                // 字段定义在父类中，继续向上查找
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private boolean isEmptyValue(Object value) {
        return value == null
            || (value instanceof String s && s.isEmpty())
            || (value instanceof String[] arr && arr.length == 0);
    }

    private String joinOpValues(String[][] ops) {
        StringJoiner joiner = new StringJoiner("、");
        for (String[] op : ops) {
            joiner.add(op[0]);
        }
        return joiner.toString();
    }

    /**
     * 批量列表校验：为空或超过上限时返回错误，通过返回 null
     */
    protected AjaxResult checkBatchSize(List<?> list, String entity) {
        if (list == null || list.isEmpty()) {
            return AjaxResult.error(entity + "列表不能为空");
        }
        if (list.size() > MAX_BATCH_SIZE) {
            return AjaxResult.error("单次批量操作最多支持 " + MAX_BATCH_SIZE + " 条" + entity + "，当前为 " + list.size() + " 条");
        }
        return null;
    }

    /**
     * 名称列表拼接，用于确认文案展示操作规模
     */
    protected String joinNames(List<String> names) {
        return String.join("、", names);
    }

    /**
     * 批量操作结果汇总：逐条记录成功与失败明细。
     * 全部成功返回 success，存在失败返回 error（已成功项写入成功信息，失败项写明原因）。
     */
    protected static class BatchResult {
        private final String successTitle;
        private final String failTitle;
        private final StringBuilder successItems = new StringBuilder();
        private final StringBuilder failItems = new StringBuilder();

        protected BatchResult(String successTitle, String failTitle) {
            this.successTitle = successTitle;
            this.failTitle = failTitle;
        }

        protected void addSuccess(String item) {
            append(successItems, item);
        }

        protected void addFail(String item, String reason) {
            append(failItems, item + "：" + (reason == null ? "执行失败" : reason));
        }

        private void append(StringBuilder sb, String item) {
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append(item);
        }

        protected AjaxResult toResult() {
            String success = successItems.isEmpty() ? "" : successTitle + successItems;
            if (failItems.isEmpty()) {
                return AjaxResult.success(success);
            }
            String fail = failTitle + failItems;
            return AjaxResult.error(success.isEmpty() ? fail : success + "。" + fail);
        }
    }

    protected AjaxResult executeWithErrorHandling(ToolCallback callback, String operationDesc) {
        try {
            logger.info("开始执行: {}", operationDesc);
            long startTime = System.currentTimeMillis();
            AjaxResult result = callback.execute();
            long endTime = System.currentTimeMillis();
            logger.info("执行完成: {}, 耗时: {}ms", operationDesc, endTime - startTime);
            return result;
        } catch (Exception e) {
            logger.error("执行失败: {}, 错误: {}", operationDesc, e.getMessage(), e);
            return AjaxResult.error("系统内部异常，" + operationDesc + "失败，请稍后再试。");
        }
    }

    @FunctionalInterface
    protected interface ToolCallback {
        AjaxResult execute() throws Exception;
    }

    protected void logOperation(String operationType, String entityName, Object result) {
        logger.info("{} {} 操作完成", operationType, entityName);
    }
}
