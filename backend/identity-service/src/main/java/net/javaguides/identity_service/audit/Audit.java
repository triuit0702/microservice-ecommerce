package net.javaguides.identity_service.audit;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
    String action();          // USER_UPDATE, LOGIN, DELETE_PRODUCT

    String resource() default ""; // user:{#id}, product:{#productId}
    Class<?> resourceClass() default void.class; // DTO class
    String idField() default "id"; // tên field để lấy id
}
