package com.tfg.tfg_redsocial.models;

/**
 * Enum Role - Define los roles disponibles en el sistema
 *
 * USER  → Usuario estándar registrado
 * ADMIN → Superusuario con permisos totales (solo tú)
 *
 * Se usa junto a Spring Security para proteger los endpoints /api/admin/**
 */

public enum Role {
    USER,
    ADMIN
}
