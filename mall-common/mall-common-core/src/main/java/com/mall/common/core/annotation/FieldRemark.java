package com.mall.common.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldRemark {

    /**
     * 映射的实体类属性字段，驼峰
     * @return
     */
    String field() default "";

    /**
     * 数据库字段
     * @return
     */
    String column() default "";

    /**
     * 字段名称
     * @return
     */
    String name() default "";

    /**
     * 读取内容转表达式 (如: 0=男,1=女,2=未知)
     */
    public String readConverterExp() default "";

    /**
     * 动态推导补充表单时是否排除该字段（如主键、控制字段、状态等非定位字段）
     */
    boolean excludeForm() default false;

}
