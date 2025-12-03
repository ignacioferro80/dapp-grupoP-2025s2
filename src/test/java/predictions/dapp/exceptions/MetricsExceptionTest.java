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
        IOException cause = new IOException("Network timeout");

        // Act
        MetricsException exception = new MetricsException(expectedMessage, cause);

        // Assert
        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }

    @Tag("unit")
    @Test
    void testMetricsException_CauseIsPreserved() {
        // Arrange
        InterruptedException cause = new InterruptedException("Thread interrupted");
        String message = "Error obteniendo standings para: Premier League";

        // Act
        MetricsException exception = new MetricsException(message, cause);

        // Assert
        assertNotNull(exception.getCause());
        assertEquals(InterruptedException.class, exception.getCause().getClass());
        assertEquals("Thread interrupted", exception.getCause().getMessage());
    }

    @Tag("unit")
    @Test
    void testMetricsException_CanBeThrown() {
        // Arrange & Act & Assert
        MetricsException exception = assertThrows(MetricsException.class, () -> {
            throw new MetricsException("Test error", new RuntimeException("Root cause"));
        });

        assertEquals("Test error", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_WithNullCause() {
        // Act
        MetricsException exception = new MetricsException("Error message", null);

        // Assert
        assertEquals("Error message", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Tag("unit")
    @Test
    void testMetricsException_ExtendsRuntimeException() {
        // Arrange
        MetricsException exception = new MetricsException("Test", new Exception());

        // Assert
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
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