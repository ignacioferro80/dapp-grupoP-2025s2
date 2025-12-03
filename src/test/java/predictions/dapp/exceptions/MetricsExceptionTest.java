package predictions.dapp.exceptions;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MetricsExceptionTest {

    @Tag("unit")
    @Test
    void testMetricsException_WithMessageAndCause() {
        // Arrange
        String expectedMessage = "Error obtaining metrics data";
        Throwable cause = new RuntimeException("Root cause");

        // Act
        MetricsException exception = new MetricsException(expectedMessage, cause);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_WithOnlyMessage() {
        // Arrange
        String expectedMessage = "Error obtaining metrics data";

        // Act
        MetricsException exception = new MetricsException(expectedMessage, null);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_WithNullMessageAndCause() {
        // Act
        MetricsException exception = new MetricsException(null, null);

        // Assert
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_WithIOExceptionCause() {
        // Arrange
        String expectedMessage = "Failed to fetch matches";
        IOException ioException = new IOException("IO error");

        // Act
        MetricsException exception = new MetricsException(expectedMessage, ioException);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(ioException, exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_WithInterruptedExceptionCause() {
        // Arrange
        String expectedMessage = "Thread interrupted";
        InterruptedException interruptedException = new InterruptedException("Thread interrupted");

        // Act
        MetricsException exception = new MetricsException(expectedMessage, interruptedException);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(interruptedException, exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_CanBeThrown() {
        // Arrange & Act & Assert
        RuntimeException cause = new RuntimeException("Root cause");
        MetricsException exception = assertThrows(MetricsException.class, () -> {
            throw new MetricsException("Test error", cause);
        });

        assertEquals("Test error", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_WithNullCause() {
        // Arrange
        String expectedMessage = "Test error with null cause";

        // Act
        MetricsException exception = new MetricsException(expectedMessage, null);

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_PreserveCauseMessage() {
        // Arrange
        IOException ioException = new IOException("Original IO error");

        // Act
        MetricsException exception = new MetricsException("Wrapped IO error", ioException);

        // Assert
        assertEquals("Wrapped IO error", exception.getMessage());
        assertEquals("Original IO error", exception.getCause().getMessage());
    }

    @Tag("unit")
    @Test
    void testMetricsException_InCatchBlock() {
        // Simulate real usage in service layer
        try {
            // Simulate an IOException that would be caught
            throw new IOException("Failed to fetch standings");
        } catch (IOException e) {
            MetricsException wrapped = new MetricsException("Error obteniendo standings para: La Liga", e);

            assertEquals("Error obteniendo standings para: La Liga", wrapped.getMessage());
            assertEquals(IOException.class, wrapped.getCause().getClass());
        }
    }
}
