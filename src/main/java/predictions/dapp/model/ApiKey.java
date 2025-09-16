package predictions.dapp.model;

import jakarta.persistence.*;

@Entity
@Table (name = "api_key")
public class ApiKey {
    @Id
    @Column(name = "api_key_value")
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