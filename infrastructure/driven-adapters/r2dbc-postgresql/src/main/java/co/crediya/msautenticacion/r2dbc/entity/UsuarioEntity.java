package co.crediya.msautenticacion.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column; // <- usar Spring Data, no JPA

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("usuario")
public class UsuarioEntity {

    @Id
    @Column("user_id")
    private UUID userId;

    @Column("nombre")
    private String nombre;

    @Column("apellido")
    private String apellido;

    @Column("fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column("email")
    private String email;

    @Column("documento_identidad")
    private String documentoIdentidad;

    @Column("telefono")
    private String telefono;

    @Column("salario_base")
    private BigDecimal salarioBase;
}