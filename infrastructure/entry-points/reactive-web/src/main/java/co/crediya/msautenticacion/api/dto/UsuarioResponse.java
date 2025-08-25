package co.crediya.msautenticacion.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UsuarioResponse {
    private UUID userId;
    private String nombre;
    private String apellido;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private BigDecimal salarioBase;
}