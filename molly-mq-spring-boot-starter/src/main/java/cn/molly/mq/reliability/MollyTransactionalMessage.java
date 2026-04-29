package cn.molly.mq.reliability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事务化发送标记注解（编程式 API 语义糖）
 * <p>
 * 标注在业务方法上，由使用方自行结合 {@code @Transactional} 使用；该注解不改变行为，仅作语义提示与 APM 索引
 *
 * @author Ht7_Sincerity
 * @since 2026/04/28
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MollyTransactionalMessage {

    /**
     * 业务语义描述，方便追踪
     */
    String value() default "";
}
