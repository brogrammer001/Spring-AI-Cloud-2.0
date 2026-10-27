package com.mall.chatmcp.sevice.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.chatmcp.bo.BaseConfirmBo;
import com.mall.chatmcp.sevice.BaseToolService;
import com.mall.common.core.web.domain.AjaxResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.lang.reflect.Field;
import java.util.*;

public abstract class BaseToolServiceImpl implements BaseToolService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Validator validator;

    /** 表单响应码：前端据此渲染输入框 */
    public static final int FORM_CODE = 8002;

    /** 确认响应码：前端据此渲染同意/取消按钮 */
    public static final int CONFIRM_CODE = 8003;

    /** 常规增删改操作选项 */
    protected static final String[][] OPS_CRUD = {
        {"add", "新增"}, {"update", "修改"}, {"delete", "删除"}
    };

    /** 操作类型 -> 中文标签 */
    private static final Map<String, String> OP_LABELS = Map.of(
        "add", "新增", "update", "修改", "delete", "删除",
        "changestatus", "状态修改", "run", "立即执行", "auth", "分配");

    /** 动态推导补充字段时排除的控制/敏感字段（非查询条件或不适合用户补充） */
    private static final Set<String> EXCLUDED_FORM_FIELDS = Set.of(
        "confirmed", "operationType", "password", "status", "delFlag");

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
            List<Map<String, Object>> fields = new ArrayList<>();
            bindingResult.getFieldErrors().forEach(error -> {
                Map<String, Object> field = buildField(obj, error.getField(), error.getDefaultMessage());
                if (!fields.contains(field)) {
                    fields.add(field);
                }
            });
            logger.warn("参数校验失败: {}", bindingResult.getAllErrors());
            return formResult("请补充操作信息", "以下必填信息不完整，请补充后提交", fields, toMap(obj));
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
                for (String field : requiredFields) {
                    if (isEmptyValue(fieldValue(bo, field))) {
                        return formMissing(opLabel(operationType) + "操作必须传入"
                            + resolveLabel(bo, field) + "，请补充", bo, field);
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
            Map<String, Object> raw = objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
            raw.remove("confirmed");
            raw.values().removeIf(Objects::isNull);
            return raw;
        } catch (Exception e) {
            logger.warn("BO转Map失败: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    protected String opLabel(String operationType) {
        if (operationType == null) {
            return "操作";
        }
        return OP_LABELS.getOrDefault(operationType.toLowerCase(), operationType);
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
     * 反射读取 BO 字段的 @JsonPropertyDescription，例如 "用户账号，例如 zhangsan" -> "用户账号"
     */
    private String resolveLabel(Object bo, String name) {
        if (bo != null) {
            for (Class<?> c = bo.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    JsonPropertyDescription desc = c.getDeclaredField(name)
                        .getAnnotation(JsonPropertyDescription.class);
                    if (desc != null && !desc.value().isBlank()) {
                        return shorten(desc.value());
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
     * 多条匹配需补充区分信息：字段不写死，按实体类动态推导——优先取必填校验
     * （@NotBlank/@NotNull/@NotEmpty）且当前值为空的字段，其次取值为空的字符串字段，
     * 补充后这些字段会作为查询条件缩小范围实现精确定位
     */
    protected AjaxResult formMissing(String message, Object bo) {
        List<String> mandatory = new ArrayList<>();
        List<String> candidates = new ArrayList<>();
        for (Class<?> c = bo.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                String name = f.getName();
                if (EXCLUDED_FORM_FIELDS.contains(name) || !isEmptyValue(fieldValue(bo, name))) {
                    continue;
                }
                boolean mandatoryField = f.getAnnotation(NotBlank.class) != null
                    || f.getAnnotation(NotNull.class) != null
                    || f.getAnnotation(NotEmpty.class) != null;
                if (mandatoryField) {
                    mandatory.add(name);
                } else if (isTextField(f.getType()) && candidates.size() < 3) {
                    candidates.add(name);
                }
            }
        }
        List<String> fields = mandatory.isEmpty() ? candidates : mandatory;
        if (fields.isEmpty()) {
            return AjaxResult.error(message);
        }
        return formMissing(message, bo, fields.toArray(new String[0]));
    }

    private boolean isTextField(Class<?> type) {
        return type == String.class || type == String[].class;
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

    private String shorten(String description) {
        int end = description.length();
        for (char sep : new char[]{'，', '：', ',', ':'}) {
            int idx = description.indexOf(sep);
            if (idx >= 0 && idx < end) {
                end = idx;
            }
        }
        return end > 0 ? description.substring(0, end) : description;
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
