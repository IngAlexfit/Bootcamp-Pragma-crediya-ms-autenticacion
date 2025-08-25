package co.crediya.msautenticacion.model.usuario;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Modelo de usuario para el sistema de autenticación.
 * @author IngPuello
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class Usuario {

    private UUID userId;
    private String nombre;
    private String apellido;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private BigDecimal salarioBase;

}
