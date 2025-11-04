package predictions.dapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "consultas")
public class Consultas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Lob
    private String rendimiento;

    @Lob
    private String predicciones;

    public Consultas() {}

    public Consultas(Long userId, String rendimiento, String predicciones) {
        this.userId = userId;
        this.rendimiento = rendimiento;
        this.predicciones = predicciones;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRendimiento() { return rendimiento; }
    public void setRendimiento(String rendimiento) { this.rendimiento = rendimiento; }

    public String getPredicciones() { return predicciones; }
    public void setPredicciones(String predicciones) { this.predicciones = predicciones; }
}