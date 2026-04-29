package cn.molly.document.excel.annotation;

import cn.molly.document.excel.converter.CellConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 绑定一个自定义的 {@link CellConverter} 实现。
 * <p>
 * 被注解的字段在导入导出时将使用该转换器，优先级高于类型默认转换器。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelConverter {

    /**
     * 转换器实现类，必须具备无参构造。
     */
    Class<? extends CellConverter<?>> value();
}
