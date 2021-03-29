package uk.nhs.adaptors.gpc.consumer.utils;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public class MdcUtil {
    private static final String REQUEST_ID = "RequestId";
    private static final String TRACE_ID = "Ssp-TraceID";

    public static void applyHeadersToMdc(ServerWebExchange exchange) {
        MDC.put(REQUEST_ID, exchange.getLogPrefix());
        HttpHeaders headers = exchange.getRequest().getHeaders();
        List<String> traceId = headers.get(TRACE_ID);
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID, traceId.get(0));
        }
    }
}
