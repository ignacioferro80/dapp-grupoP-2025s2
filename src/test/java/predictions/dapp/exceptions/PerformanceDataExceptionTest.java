package predictions.dapp.exceptions;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceDataExceptionTest {

    @Tag("unit")
    @Test
    void testPerformanceDataException_WithMessageAndCause() {
        // Arrange
        String expectedMessage = "Error obtaining performance data";
        IOException cause = new IOException("Database connection failed");

        // Act
        PerformanceDataException exception = new PerformanceDataException(expectedMessage, cause);

        // Assert
        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_CauseIsPreserved() {
        // Arrange
        InterruptedException cause = new InterruptedException("Operation interrupted");
        String message = "Error fetching team performance data";

        // Act
        PerformanceDataException exception = new PerformanceDataException(message, cause);

        // Assert
        assertNotNull(exception.getCause());
        assertEquals(InterruptedException.class, exception.getCause().getClass());
        assertEquals("Operation interrupted", exception.getCause().getMessage());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_CanBeThrown() {
        // Arrange & Act & Assert
        PerformanceDataException exception = assertThrows(PerformanceDataException.class, () -> {
            throw new PerformanceDataException("Test error", new RuntimeException("Root cause"));
        });

        assertEquals("Test error", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_WithNullCause() {
        // Act
        PerformanceDataException exception = new PerformanceDataException("Error message", null);

        // Assert
        assertEquals("Error message", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_ExtendsRuntimeException() {
        // Arrange
        PerformanceDataException exception = new PerformanceDataException("Test", new Exception());

        // Assert
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_InCatchBlock() {
        // Simulate real usage in service layer
        try {
            // Simulate an InterruptedException that would be caught
            throw new InterruptedException("Thread was interrupted");
        } catch (InterruptedException e) {
            PerformanceDataException wrapped = new PerformanceDataException("Error fetching performance data", e);

            assertEquals("Error fetching performance data", wrapped.getMessage());
            assertEquals(InterruptedException.class, wrapped.getCause().getClass());
        }
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_DifferentExceptionTypes() {
        // Test with different exception types as causes
        RuntimeException runtimeCause = new RuntimeException("Runtime error");
        PerformanceDataException perfEx1 = new PerformanceDataException("Test 1", runtimeCause);

        IOException ioCause = new IOException("IO error");
        PerformanceDataException perfEx2 = new PerformanceDataException("Test 2", ioCause);

        // Assert
        assertEquals(RuntimeException.class, perfEx1.getCause().getClass());
        assertEquals(IOException.class, perfEx2.getCause().getClass());
    }
}