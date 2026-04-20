package com.kurtuba.auth.error.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BusinessLogicExceptionTest {

    @Test
    void toString_includesMessageFromRuntimeException() {
        BusinessLogicException exception = new BusinessLogicException(1400, "Unable to send mail: connection refused");

        assertEquals(
                "com.kurtuba.auth.error.exception.BusinessLogicException: Unable to send mail: connection refused",
                exception.toString());
    }

    @Test
    void constructorWithCause_preservesOriginalCause() {
        RuntimeException cause = new RuntimeException("smtp timeout");

        BusinessLogicException exception = new BusinessLogicException(1400, "Unable to send mail: smtp timeout", cause);

        assertSame(cause, exception.getCause());
    }
}
