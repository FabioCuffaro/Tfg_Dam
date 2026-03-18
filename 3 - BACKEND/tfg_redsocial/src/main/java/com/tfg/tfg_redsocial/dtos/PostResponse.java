package com.tfg.tfg_redsocial.dtos;

import java.time.LocalDateTime;

/**
 * PostResponse - DTO de respuesta para un post
 *
 * Contiene toda la info que el frontend necesita para pintar una tarjeta de post:
 * datos del post + datos del autor + conteo de likes + si el usuario actual ya dio like.
 */
public record PostResponse(
        Long id,
        String content,
        String imageUrl,
        LocalDateTime createdAt,

        // Datos del autor (evita que el frontend haga una segunda petición)
        Long authorId,
        String authorUsername,
        String authorDisplayName,
        String authorAvatarUrl,

        // Likes
        long likeCount,
        boolean likedByCurrentUser
) {}
