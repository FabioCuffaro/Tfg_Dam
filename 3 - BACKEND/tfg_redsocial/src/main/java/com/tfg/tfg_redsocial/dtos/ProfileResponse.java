package com.tfg.tfg_redsocial.dtos;

/**
 * ProfileResponse - DTO de respuesta para un perfil
 *
 * Incluye estadísticas calculadas: postCount, followerCount, followingCount.
 * También incluye isFollowedByCurrentUser para que el frontend sepa
 * si mostrar "Seguir" o "Dejar de seguir".
 */
public record ProfileResponse(
        Long id,
        Long userId,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String location,
        String website,

        // Estadísticas calculadas en el servicio
        long postCount,
        long followerCount,
        long followingCount,

        // Estado de seguimiento para el usuario actual
        boolean isFollowedByCurrentUser
) {}
