import cn.hutool.core.annotation.MirrorFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author chenguowei
 * @date 2024/7/23
 * @description
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface TestAnnotation {

    @MirrorFor(attribute = "name")
    // 等同于 @Link(attribute = "name", type = RelationType.MIRROR_FOR)
    String value() default "";
    // 等同于 @Link(attribute = "value"", type = RelationType.MIRROR_FOR)
    @MirrorFor(attribute = "value")
    String name() default "";
}
