package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.sevice.BaseToolService;
import com.mall.common.core.web.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

public abstract class BaseToolServiceImpl implements BaseToolService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Validator validator;

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    protected <T> AjaxResult validate(T obj, String objName) {
        BindingResult bindingResult = new BeanPropertyBindingResult(obj, objName);
        validator.validate(obj, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("参数校验失败：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            logger.warn("参数校验失败: {}", errorMsg);
            return AjaxResult.error(errorMsg.toString());
        }
        return null;
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