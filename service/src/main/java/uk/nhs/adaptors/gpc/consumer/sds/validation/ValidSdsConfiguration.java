package uk.nhs.adaptors.gpc.consumer.sds.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = SdsConfigurationValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSdsConfiguration {
    String message() default "Invalid SDS Configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}