package co.crediya.msautenticacion.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;


/**
 * DTO para la creación de usuarios.
 * <p>
 * Valida los datos recibidos en la petición de registro de usuario.
 */
@Data
public class UsuarioRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe tener un formato válido")
    private String email;

    @NotNull(message = "El salario base es obligatorio")
    @DecimalMin(value = "0", message = "El salario base debe ser mayor o igual a 0")
    @DecimalMax(value = "15000000", message = "El salario base debe ser menor o igual a 15.000.000")
    private BigDecimal salarioBase;
}