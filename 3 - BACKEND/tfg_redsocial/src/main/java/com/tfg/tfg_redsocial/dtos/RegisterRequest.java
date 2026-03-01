package com.tfg.tfg_redsocial.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequest - DTO para el registro de nuevos usuarios
 *
 * Representa los datos que llegan del formulario de registro del frontend.
 * Usamos record porque es inmutable: una vez creado, sus datos no cambian.
 * Perfecto para transportar datos de entrada.
 *
 * Las anotaciones de validación (@NotBlank, @Email, @Size...) se activan
 * automáticamente gracias a spring-boot-starter-validation en el pom.xml.
 * Si algún campo no cumple la validación, Spring devuelve un 400 Bad Request
 * antes de que el código llegue al Service.
 */
public record RegisterRequest(

        /**
         * Username único del perfil público.
         * Solo letras, números y guion bajo. Entre 3 y 20 caracteres.
         * Será visible públicamente en la URL: /profiles/{username}
         */
        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 20, message = "El username debe tener entre 3 y 20 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9_]+$", //expresión regular (regular expression)
                message = "El username solo puede contener letras, números y guion bajo"
        )
        String username,

        /**
         * Nombre para mostrar en el perfil (puede tener espacios, acentos, etc.)
         * Es diferente del username: más libre y descriptivo.
         */
        @NotBlank(message = "El nombre para mostrar es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String displayName,

        /**
         * Email único. Sirve como identificador para el login.
         * @Email valida automáticamente que tenga formato correcto.
         */
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        /**
         * Contraseña en texto plano. Solo viaja del frontend al backend.
         * NUNCA se guarda en la base de datos así.
         * AuthService la hasheará con BCrypt antes de guardarla.
         */
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener mínimo 6 caracteres")
        String password

) {}