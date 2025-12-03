package predictions.dapp.dtos;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class RegisterRequestTest {

    @Test
    void getters_ShouldReturnValuesSetViaReflection() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();

        Field usernameField = RegisterRequest.class.getDeclaredField("username");
        usernameField.setAccessible(true);
        usernameField.set(request, "johndoe");

        Field emailField = RegisterRequest.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(request, "johndoe@example.com");

        Field passwordField = RegisterRequest.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(request, "SecurePassword123!");

        // Act & Assert
        assertEquals("johndoe", request.getUsername());
        assertEquals("johndoe@example.com", request.getEmail());
        assertEquals("SecurePassword123!", request.getPassword());
    }
}
