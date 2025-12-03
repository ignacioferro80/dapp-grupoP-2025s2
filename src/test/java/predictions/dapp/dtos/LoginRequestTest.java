package predictions.dapp.dtos;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class LoginRequestTest {

    @Test
    void getters_ShouldReturnValuesSetViaReflection() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();

        Field emailField = LoginRequest.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(request, "johndoe@example.com");

        Field passwordField = LoginRequest.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(request, "SecurePassword123!");

        // Act & Assert
        assertEquals("johndoe@example.com", request.getEmail());
        assertEquals("SecurePassword123!", request.getPassword());
    }
}
