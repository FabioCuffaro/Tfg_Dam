package com.tfg.tfg_redsocial.dtos;

/**
 * AuthResponse - DTO de respuesta tras registro o login
 *
 * Representa los datos que el backend devuelve al frontend
 * cuando el usuario se registra o hace login correctamente.
 *
 * El frontend guardará el token en localStorage y lo incluirá
 * en la cabecera Authorization de todas las peticiones siguientes:
 *   Authorization: Bearer {token}
 *
 * Incluimos datos básicos del usuario para que el frontend
 * pueda mostrar el nombre y username sin hacer otra petición.
 */
public record AuthResponse(

        /**
         * El token JWT generado.
         * El frontend lo guarda y lo envía en cada petición autenticada.
         */
        String token,

        /**
         * Tipo de token. Siempre "Bearer" en JWT.
         * El frontend lo usa para construir la cabecera Authorization.
         */
        String tokenType,

        /**
         * ID del usuario en la base de datos.
         * Útil para el frontend cuando necesite hacer peticiones
         * relacionadas con este usuario específico.
         */
        Long userId,

        /**
         * Email del usuario. Para mostrarlo en la interfaz si hace falta.
         */
        String email,

        /**
         * Username del perfil. Se usa en la URL pública del perfil.
         * El frontend puede construir: /profiles/{username}
         */
        String username,

        /**
         * Nombre para mostrar en la interfaz (header, perfil, posts...).
         */
        String displayName,

        /**
         * Rol del usuario: "USER" o "ADMIN".
         * El frontend puede usarlo para mostrar u ocultar opciones de administración.
         */
        String role

) {
    /**
     * Constructor de conveniencia que rellena automáticamente.
     * Así en AuthService solo tenemos que pasar el token y los datos del usuario,
     *
     * Así me dejo de tener que escribir "Bearer" cada vez.
     */
    public static AuthResponse of(String token, Long userId, String email,
                                  String username, String displayName, String role) {
        return new AuthResponse(token, "Bearer", userId, email, username, displayName, role);
    }
}