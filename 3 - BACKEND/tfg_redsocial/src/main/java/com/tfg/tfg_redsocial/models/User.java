package com.tfg.tfg_redsocial.models;

import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad User - Maneja autenticación y credenciales
 */

/*

Aquí explico por qué he usado Lombok en mi proyecto, ya que agiliza bastante el desarrollo:

@Entity - Le dice a Hibernate: "Esta clase es una tabla"
@Table(name = "users")  // Nombre de la tabla en PostgreSQL

@Data  // Lombok genera getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Constructor vacío: new User()
@AllArgsConstructor  // Constructor con todos los campos
@Builder  // Este es el que más voy a usar al principio para probar si funciona lo básico de las clases.

User.builder().email("...").password("...").build()

@Id  // Esta columna es la clave primaria
@GeneratedValue(strategy = GenerationType.IDENTITY)  // Autoincremental

@Column(nullable = false, unique = true)  // No puede ser NULL, debe ser único

 */


@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password; // Hasheado con BCrypt

    // ── CAMPO AÑADIDO: rol del usuario ───────────────────
    // @Enumerated(EnumType.STRING) guarda "USER" o "ADMIN"
    // en texto en la BBDD, no como número. Más legible en PGAdmin.
    //
    // @Builder.Default es OBLIGATORIO cuando usas @Builder
    // y quieres un valor por defecto. Sin él, el Builder
    // ignora el = Role.USER y deja el campo null.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;
    // ─────────────────────────────────────────────────────

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relación 1:1 con Profile
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profile profile;

    // Relación 1:N con Posts
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;

    // Excluir password de toString por seguridad
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}