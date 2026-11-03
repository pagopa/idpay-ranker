package it.gov.pagopa.utils;


import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

public class CommonUtils {
    private CommonUtils() {}

    public static String sanitizeString(String str){
        return str == null? null: str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }

    public static String getSpanContextTraceId() {
        SpanContext spanContext = Span.current().getSpanContext();

        return spanContext.isValid()
                ? spanContext.getTraceId()
                : null;
    }
}
