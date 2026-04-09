package it.gov.pagopa.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommonUtilsTest {

    @Test
    void sanitizeString() {
        assertNull(CommonUtils.sanitizeString(null));
        assertEquals("hello", CommonUtils.sanitizeString("\nhe!!llo\r"));
        assertEquals("helloWorld", CommonUtils.sanitizeString("hello@World!!!"));

    }
}