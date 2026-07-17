package it.gov.pagopa.common.utils;

import it.gov.pagopa.common.config.JsonConfig;
import it.gov.pagopa.utils.CommonConstants;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.TimeZone;

public final class TestUtils {
    private TestUtils() {
    }

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(CommonConstants.ZONEID));
    }

    /**
     * applications's objectMapper
     */
    public static ObjectMapper objectMapper = new JsonConfig().objectMapper();

    /**
     * To serialize an object as a JSON handling Exception
     */
    public static String jsonSerializer(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}
