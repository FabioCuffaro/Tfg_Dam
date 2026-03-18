package com.tfg.tfg_redsocial.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PostRequest - DTO para crear un nuevo post
 *
 * record es una clase Java moderna (Java 16+) para datos inmutables.
 * Genera automáticamente: constructor, getters, equals, hashCode, toString.
 */
public record PostRequest(

        @NotBlank(message = "El contenido no puede estar vacío")
        @Size(max = 500, message = "El post no puede superar los 500 caracteres")
        String content

) {}
