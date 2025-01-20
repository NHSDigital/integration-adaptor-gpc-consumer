package uk.nhs.adaptors.gpc.consumer.gpc.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;
import uk.nhs.adaptors.gpc.consumer.utils.PemFormatter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GpcConfigurationValidator implements ConstraintValidator<ValidGpcConfiguration, GpcConfiguration> {

    private static final String INVALID_FORMAT_VIOLATION_MESSAGE =
        "One or more of the GPC_CONSUMER_SPINE_ variables is in an invalid PEM format. Invalid variables: %s";
    private static final String MISSING_SSL_PROPERTIES_VIOLATION_MESSAGE =
        "Either all or none of the GPC_CONSUMER_SPINE_ variables must be defined. Missing variables: %s";
    private static final int NUMBER_OF_SSL_PROPERTIES = 4;

    @Override
    public boolean isValid(GpcConfiguration config, ConstraintValidatorContext context) {

        validateSspUrl(config);

        var missingSslProperties = checkForMissingSslProperties(config);
        if (missingSslProperties.size() == NUMBER_OF_SSL_PROPERTIES) {
            config.setSslEnabled(false);
            return true;
        }
        if (!missingSslProperties.isEmpty()) {
            setConstraintViolation(context, MISSING_SSL_PROPERTIES_VIOLATION_MESSAGE, missingSslProperties);
            return false;
        }

        var invalidSslProperties = checkSslPropertiesAreValidPemFormat(config);
        if (!invalidSslProperties.isEmpty()) {
            setConstraintViolation(context, INVALID_FORMAT_VIOLATION_MESSAGE, invalidSslProperties);
            return false;
        }

        config.setSslEnabled(true);
        return true;
    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String message, List<String> violations) {
        String violationMessage = String.format(message, String.join(", ", violations));

        LOGGER.error(violationMessage);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(violationMessage).addConstraintViolation();
    }

    private List<String> checkForMissingSslProperties(GpcConfiguration config) {
        var missingSslProperty = new ArrayList<String>();

        if (StringUtils.isBlank(config.getClientCert())) {
            missingSslProperty.add("GPC_CONSUMER_SPINE_CLIENT_CERT");
        }
        if (StringUtils.isBlank(config.getClientKey())) {
            missingSslProperty.add("GPC_CONSUMER_SPINE_CLIENT_KEY");
        }
        if (StringUtils.isBlank(config.getRootCA())) {
            missingSslProperty.add("GPC_CONSUMER_SPINE_ROOT_CA_CERT");
        }
        if (StringUtils.isBlank(config.getSubCA())) {
            missingSslProperty.add("GPC_CONSUMER_SPINE_SUB_CA_CERT");
        }

        return missingSslProperty;
    }

    private List<String> checkSslPropertiesAreValidPemFormat(GpcConfiguration config) {
        var invalidSslProperties = new ArrayList<String>();

        config.setClientCert(tryGetPemFormatedProperty(config.getClientCert()));
        config.setClientKey(tryGetPemFormatedProperty(config.getClientKey()));
        config.setRootCA(tryGetPemFormatedProperty(config.getRootCA()));
        config.setSubCA(tryGetPemFormatedProperty(config.getSubCA()));

        if (config.getClientCert().isEmpty()) {
            invalidSslProperties.add("GPC_CONSUMER_SPINE_CLIENT_CERT");
        }
        if (config.getClientKey().isEmpty()) {
            invalidSslProperties.add("GPC_CONSUMER_SPINE_CLIENT_KEY");
        }
        if (config.getRootCA().isEmpty()) {
            invalidSslProperties.add("GPC_CONSUMER_SPINE_ROOT_CA_CERT");
        }
        if (config.getSubCA().isEmpty()) {
            invalidSslProperties.add("GPC_CONSUMER_SPINE_SUB_CA_CERT");
        }

        return invalidSslProperties;
    }

    private String tryGetPemFormatedProperty(String sslProperty) {
        try {
            return PemFormatter.format(sslProperty);

        } catch (Exception e) {
            return "";
        }
    }

    private void validateSspUrl(GpcConfiguration config) {
        var baseUrl = config.getSspUrl();

        if (StringUtils.isBlank(baseUrl)) {
            config.setSspEnabled(false);
            return;
        }

        config.setSspUrl(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        config.setSspEnabled(true);
    }
}
