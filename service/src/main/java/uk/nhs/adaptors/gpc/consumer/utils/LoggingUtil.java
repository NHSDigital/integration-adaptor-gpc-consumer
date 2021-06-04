package uk.nhs.adaptors.gpc.consumer.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_TRACE_ID;

public class LoggingUtil {
    private static final String REQUEST_ID = "RequestId";
    private static final String REQUEST_ID_PATTERN = REQUEST_ID + "=%s ";
    private static final String TRACE_ID_PATTERN = SSP_TRACE_ID + "=%s";

    public static void warn(Logger logger, ServerWebExchange exchange, String msg, Object... args) {
        logger.warn(extractHeaders(exchange) + msg, args);
    }

    public static void info(Logger logger, ServerWebExchange exchange, String msg, Object... args) {
        logger.info(extractHeaders(exchange) + msg, args);
    }

    private static String extractHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String traceId = headers.getFirst(SSP_TRACE_ID);
        if (StringUtils.isNotEmpty(traceId)) {
            return String.format(REQUEST_ID_PATTERN + TRACE_ID_PATTERN, exchange.getLogPrefix(), traceId);
        }
        return String.format(REQUEST_ID_PATTERN, exchange.getLogPrefix());
    }
}
