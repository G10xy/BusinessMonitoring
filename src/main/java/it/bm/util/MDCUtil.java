package it.bm.util;

import org.slf4j.MDC;

import java.util.Map;

import static it.bm.util.Constant.CORRELATION_ID_HEADER_NAME;

public class MDCUtil {

    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_HEADER_NAME);
    }

    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_HEADER_NAME, correlationId);
        }
    }

    public static Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }

    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }

    public static void clearContext() {
        MDC.clear();
    }
}