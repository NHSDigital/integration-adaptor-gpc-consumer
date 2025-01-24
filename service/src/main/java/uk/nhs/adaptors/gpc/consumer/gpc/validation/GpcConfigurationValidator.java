package uk.nhs.adaptors.gpc.consumer.gpc.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;
import uk.nhs.adaptors.gpc.consumer.utils.PemFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Slf4j
public class GpcConfigurationValidator implements ConstraintValidator<ValidGpcConfiguration, GpcConfiguration> {

    private static final String PEM_FORMAT_VIOLATION_MESSAGE =
        "The environment variable(s) %s are not in a valid PEM format";
    private static final String SSL_PROPERTIES_VIOLATION_MESSAGE =
        """
            You must either use mutual TLS or decide to disable it.
            To enable mutual TLS you must provide %s environment variable(s).
            To disable mutual TLS you must remove %s environment variable(s).""";

    @Override
    public boolean isValid(GpcConfiguration config, ConstraintValidatorContext context) {
        TreeMap<String, String> environmentVariables = new TreeMap<>();
        environmentVariables.put("GPC_CONSUMER_SPINE_CLIENT_CERT", config.getClientCert());
        environmentVariables.put("GPC_CONSUMER_SPINE_CLIENT_KEY", config.getClientKey());
        environmentVariables.put("GPC_CONSUMER_SPINE_ROOT_CA_CERT", config.getRootCA());
        environmentVariables.put("GPC_CONSUMER_SPINE_SUB_CA_CERT", config.getSubCA());
        List<String> missingSslProperties = new ArrayList<>();
        List<String> invalidSslProperties = new ArrayList<>();

        validateSspUrl(config);

        for (var variable : environmentVariables.entrySet()) {
            if (StringUtils.isBlank(variable.getValue())) {
                missingSslProperties.add(variable.getKey());
                invalidSslProperties.add(variable.getKey());
            } else if (isInvalidPemFormat(variable.getValue())) {
                invalidSslProperties.add(variable.getKey());
            }
        }

        var presentSslProperties = environmentVariables.keySet().stream()
            .filter(key -> !missingSslProperties.contains(key))
            .sorted().toList();

        if (presentSslProperties.isEmpty()) {
            config.setSslEnabled(false);
            return true;
        }

        if (!missingSslProperties.isEmpty()) {
            var message = String.format(
                SSL_PROPERTIES_VIOLATION_MESSAGE,
                String.join(", ", missingSslProperties),
                String.join(", ", presentSslProperties));

            setConstraintViolation(context, message);
        }

        if (!invalidSslProperties.isEmpty()) {
            var message = String.format(PEM_FORMAT_VIOLATION_MESSAGE, String.join(", ", invalidSslProperties));
            setConstraintViolation(context, message);
        }

        if (!missingSslProperties.isEmpty() || !invalidSslProperties.isEmpty()) {
            config.setSslEnabled(false);
            return false;
        }

        config.setSslEnabled(true);
        return true;
    }

    private static void setConstraintViolation(ConstraintValidatorContext context, String message) {
        LOGGER.error(message);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    private boolean isInvalidPemFormat(String sslProperty) {
        try {
            PemFormatter.format(sslProperty);
            return false;
        } catch (Exception e) {
            return true;
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
