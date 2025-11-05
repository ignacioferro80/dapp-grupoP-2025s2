package predictions.dapp.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for user registration")
public class RegisterRequest {

    @Schema(
            description = "Username for the new account",
            example = "johndoe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @Schema(
            description = "Email address for the new account (must be unique)",
            example = "johndoe@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @Schema(
            description = "Password for the new account (will be encrypted)",
            example = "SecurePassword123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}