package co.crediya.msautenticacion.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.persistence.Column;

import java.util.UUID;
import java.math.BigDecimal;

/**
 * Clase que representa la entidad Usuario en la base de datos.
 * 
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("usuario") 
public class UsuarioEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "fecha_nacimiento")
    private String fechaNacimiento;

    @Column(name = "email")
    private String email;

    @Column(name = "documento_identidad")
    private String documentoIdentidad;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "salario_base")
    private BigDecimal salarioBase;

 
}