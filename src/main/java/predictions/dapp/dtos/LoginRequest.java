package predictions.dapp.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for user authentication")
public class LoginRequest {

    @Schema(
            description = "Email address of the user",
            example = "johndoe@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @Schema(
            description = "Password for authentication",
            example = "SecurePassword123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}