package crypto.middleware.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class ApiKey {
    @Id
    private String key;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public void setKey(String key) {
        this.key = key;
    }

    public void setUser(User user) {
        this.user = user;
    }
}