package com.tfg.tfg_redsocial.security;

import com.tfg.tfg_redsocial.repositories.UserRepository;
import com.tfg.tfg_redsocial.models.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthenticationFilter - El portero de la aplicación
 *
 * Se ejecuta UNA VEZ por cada petición HTTP, antes de que llegue a controller.
 * Hereda de OncePerRequestFilter para garantizar que solo se ejecuta UNA SOLA VEZ CADA PETICIÓN.
 *
 * Flujo completo:
 * 1. Leer la cabecera Authorization de la petición
 * 2. Extraer el token JWT (quitar "Bearer ")
 * 3. Extraer el email del token con JwtUtil
 * 4. Buscar al usuario en la base de datos
 * 5. Validar el token
 * 6. Si no hay problemas y OK → registrar al usuario en el contexto de seguridad de Spring
 * 7. Continuar con la petición hacia el controller
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // Inyecto dependencias necesarias
    private final UserRepository userRepository;

    /**
     * Inyección de dependencias por constructor.
     * Spring nos proporciona JwtUtil y UserRepository automáticamente.
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * Método principal del filtro. Se ejecuta en cada petición HTTP.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        //---------------PASO 1: Leer la cabecera Authorization ----------------
        // Todas las peticiones autenticadas llevan:
        // Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ...
        final String authHeader = request.getHeader("Authorization");

        // -----------PASO 2: Comprobar que existe y tiene el formato correcto -----------
        // CAMBIO AQUÍ: Si no hay token, el "return" es vital para que no se ejecute el resto
        // y Spring pueda mirar los "permitAll" de tu SecurityConfig.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // continuar sin autenticar
            return; // Salimos del filtro porque no hay token que validar
        }

        // ------------ PASO 3: Extraer el token quitando "Bearer " (7 caracteres)----------
        final String token = authHeader.substring(7);

        //---------- PASO 4: Extraer el email del interior del token -----------------
        String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            // Si el token es falso o ha expirado, dejamos que siga como "anónimo"
            filterChain.doFilter(request, response);
            return;
        }

        //-------------- PASO 5: Comprobar que no hay ya una autenticación activa -----------------
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            //--------------PASO 6: Buscar al usuario en la base de datos --------------------
            User user = userRepository.findByEmail(email).orElse(null);

            // Validamos que el usuario existe y que el token coincide con ese email
            if (user != null && jwtUtil.validateToken(token, email)) {

                //----------PASO 7: Registrar al usuario en el contexto de Spring -----------
                // Convertimos el rol (USER/ADMIN) al formato "ROLE_USER"
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                );

                // Creamos el objeto de autenticación con el usuario y sus permisos
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);

                // Añadimos detalles de la petición (IP, etc.)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Guardamos la autenticación. Ahora la app "sabe" quién eres.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        //--------------PASO 8: Continuar con la cadena de filtros--------------------
        // IMPORTANTE: Llamamos a doFilter para que la petición llegue al Controller.
        filterChain.doFilter(request, response);
    }
}