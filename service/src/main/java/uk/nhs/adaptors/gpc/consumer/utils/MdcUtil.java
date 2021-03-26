package uk.nhs.adaptors.gpc.consumer.utils;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public class MdcUtil {
    private static final String REQUEST_ID = "RequestId";
    private static final String TRACE_ID = "Ssp-TraceID";

    public static void applyHeadersToMdc(ServerWebExchange exchange) {
        MDC.put(REQUEST_ID, exchange.getLogPrefix());
        HttpHeaders headers = exchange.getRequest().getHeaders();
        var traceId = headers.get(TRACE_ID);
        if (traceId != null && traceId.size() > 0) {
            MDC.put(TRACE_ID, traceId.get(0));
        }
    }
}
