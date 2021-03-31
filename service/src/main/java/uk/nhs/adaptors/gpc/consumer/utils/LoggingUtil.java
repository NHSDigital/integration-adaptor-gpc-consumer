package uk.nhs.adaptors.gpc.consumer.utils;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public class LoggingUtil {
    private static final String REQUEST_ID = "RequestId=%s ";
    private static final String TRACE_ID = "Ssp-TraceID";

    public static void info(Logger logger, ServerWebExchange exchange, String msg, Object... args) {
        logger.info(extractHeaders(exchange) + msg, args);
    }

    private static String extractHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        List<String> traceId = headers.get(TRACE_ID);
        if (traceId != null && !traceId.isEmpty()) {
            return String.format(REQUEST_ID + TRACE_ID + "=%s ", exchange.getLogPrefix(), traceId.get(0));
        }
        return String.format("RequestId=%s", exchange.getLogPrefix());
    }
}
