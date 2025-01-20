package uk.nhs.adaptors.gpc.consumer.gpc.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = GpcConfigurationValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGpcConfiguration {
    String message() default "Invalid GPC Configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}