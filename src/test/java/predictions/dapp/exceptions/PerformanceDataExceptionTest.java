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
        Throwable cause = new RuntimeException("Root cause");

        // Act
        PerformanceDataException exception = new PerformanceDataException(expectedMessage, cause);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_WithOnlyMessage() {
        // Arrange
        String expectedMessage = "Error obtaining performance data";

        // Act
        PerformanceDataException exception = new PerformanceDataException(expectedMessage, null);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_WithNullMessageAndCause() {
        // Act
        PerformanceDataException exception = new PerformanceDataException(null, null);

        // Assert
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_WithIOExceptionCause() {
        // Arrange
        String expectedMessage = "Failed to fetch performance data";
        IOException ioException = new IOException("IO error");

        // Act
        PerformanceDataException exception = new PerformanceDataException(expectedMessage, ioException);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(ioException, exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_WithInterruptedExceptionCause() {
        // Arrange
        String expectedMessage = "Operation interrupted";
        InterruptedException interruptedException = new InterruptedException("Operation interrupted");

        // Act
        PerformanceDataException exception = new PerformanceDataException(expectedMessage, interruptedException);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(interruptedException, exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_CanBeThrown() {
        // Arrange & Act & Assert
        RuntimeException cause = new RuntimeException("Root cause");
        PerformanceDataException exception = assertThrows(PerformanceDataException.class, () -> {
            throw new PerformanceDataException("Test error", cause);
        });

        assertEquals("Test error", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_WithNullCause() {
        // Arrange
        String expectedMessage = "Test error with null cause";

        // Act
        PerformanceDataException exception = new PerformanceDataException(expectedMessage, null);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testPerformanceDataException_PreserveCauseMessage() {
        // Arrange
        IOException ioException = new IOException("Original IO error");

        // Act
        PerformanceDataException exception = new PerformanceDataException("Wrapped IO error", ioException);

        // Assert
        assertEquals("Wrapped IO error", exception.getMessage());
        assertEquals("Original IO error", exception.getCause().getMessage());
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
