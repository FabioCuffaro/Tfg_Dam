package com.tfg.tfg_redsocial.dtos;

import jakarta.validation.constraints.Size;

/**
 * UpdateProfileRequest - DTO para actualizar el perfil del usuario
 *
 * Todos los campos son opcionales (pueden ser null si no se quieren cambiar).
 * Usamos @Size para validar longitud máxima de los campos de texto.
 */
public record UpdateProfileRequest(

        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String displayName,

        @Size(max = 300, message = "La bio no puede superar los 300 caracteres")
        String bio,

        @Size(max = 100, message = "La ubicación no puede superar los 100 caracteres")
        String location,

        @Size(max = 200, message = "El sitio web no puede superar los 200 caracteres")
        String website

) {}
