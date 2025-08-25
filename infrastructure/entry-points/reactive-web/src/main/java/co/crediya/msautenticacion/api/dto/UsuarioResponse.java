package co.crediya.msautenticacion.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Data
public class UsuarioResponse {
    private UUID userId;
    private String nombre;
    private String apellido;
    private Timestamp fechaNacimiento;
    private String telefono;
    private String email;
    private BigDecimal salarioBase;
}