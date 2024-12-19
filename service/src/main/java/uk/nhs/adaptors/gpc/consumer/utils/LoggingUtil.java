package uk.nhs.adaptors.gpc.consumer.utils;

import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_TRACE_ID;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingUtil {

    private static final String MESSAGE_PREFIX = "RequestId=%s SspTraceId=%s ";

    public static void warn(Logger logger, ServerWebExchange exchange, String msg, Object... args) {
        logger.warn(extractHeaders(exchange) + msg, args);
    }

    private static String extractHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String traceId = StringUtils.defaultIfBlank(headers.getFirst(SSP_TRACE_ID), StringUtils.EMPTY);
        return String.format(MESSAGE_PREFIX, exchange.getLogPrefix(), traceId);
    }

    public static void info(Logger logger, ServerWebExchange exchange, String msg, Object... args) {
        logger.info(extractHeaders(exchange) + msg, args);
    }

    public static void debug(Logger logger, ServerWebExchange exchange, String msg, Object... args) {
        logger.debug(extractHeaders(exchange) + msg, args);
    }
}
