package uk.nhs.adaptors.gpc.consumer.sds.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gpc.consumer.sds.configuration.SdsConfiguration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SdsConfigurationValidator implements ConstraintValidator<ValidSdsConfiguration, SdsConfiguration> {

    private static final String SDS_CONFIGURATION_VIOLATION_MESSAGE =
        "The environment variable(s) %s must be provided.";

    @Override
    public boolean isValid(SdsConfiguration config, ConstraintValidatorContext context) {
        List<String> missingSdsProperties = new ArrayList<>();

        if (StringUtils.isEmpty(config.getUrl())) {
            missingSdsProperties.add("GPC_CONSUMER_SDS_URL");
        }

        if (StringUtils.isEmpty(config.getApiKey())) {
            missingSdsProperties.add("GPC_CONSUMER_SDS_APIKEY");
        }

        if (missingSdsProperties.isEmpty()) {
            return true;
        }

        var message = String.format(SDS_CONFIGURATION_VIOLATION_MESSAGE, String.join(", ", missingSdsProperties));
        setConstraintViolation(context, message);
        return false;

    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String message) {
        LOGGER.error(message);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
