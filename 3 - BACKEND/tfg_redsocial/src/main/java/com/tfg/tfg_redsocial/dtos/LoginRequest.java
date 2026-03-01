package com.tfg.tfg_redsocial.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest - DTO para el inicio de sesión
 *
 *
 * EXPLICACIÓN DE RECORD:      public record LoginRequest(String email, String password){}
 *
 * Genera automáticamente constructor
 * Genera getters (email(), password())
 * Genera equals, hashCode, toString
 * Es inmutable (no puedes cambiar los campos)
 *
 * No quiero crear una clase completa, porque solo voy a tener un contenedor de datos sencillos, sin lógica, solo algo que usar
 * en el transporte de capas.
 *
 * Representa los datos que llegan del formulario de login del frontend.
 * Solo necesita email y contraseña.
 *
 * Es más simple que RegisterRequest porque en el login
 * no necesitamos validar formato de username ni longitud de contraseña,
 * solo que los campos no estén vacíos.
 */
public record LoginRequest(

        /**
         * Email del usuario registrado.
         * AuthService lo usará para buscarlo en la base de datos.
         */
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        /**
         * Contraseña en texto plano.
         * AuthService la comparará con el hash guardado usando BCrypt.
         */
        @NotBlank(message = "La contraseña es obligatoria")
        String password

) {}