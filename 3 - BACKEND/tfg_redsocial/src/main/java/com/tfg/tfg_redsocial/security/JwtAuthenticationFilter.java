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
 *   1. Leer la cabecera Authorization de la petición
 *   2. Extraer el token JWT (quitar "Bearer ")
 *   3. Extraer el email del token con JwtUtil
 *   4. Buscar al usuario en la base de datos
 *   5. Validar el token
 *   6. Si no hay problemas y OK → registrar al usuario en el contexto de seguridad de Spring
 *   7. Continuar con la petición hacia el controller
 */


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; //Inyecto dependencias necesarias
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
     *
     * @param request     La petición HTTP que llega (contiene cabeceras, body, etc.)
     * @param response    La respuesta HTTP que vamos a devolver
     * @param filterChain La cadena de filtros de Spring (hay que llamar a doFilter al final, para que después de todos los filtros pase)
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
        // Si no hay cabecera o no empieza por "Bearer ", dejamos pasar la petición
        // sin autenticar. Spring Security decidirá si el endpoint requiere auth o no.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // continuar sin autenticar
            return;
        }

        // ------------ PASO 3: Extraer el token quitando "Bearer " (7 caracteres)----------
        // authHeader = "Bearer eyJhbGci..."
        // token      =         "eyJhbGci..."
        //Hay que usar el método substring para que empiece directamente desde el 7º caracter

        final String token = authHeader.substring(7);

        //---------- PASO 4: Extraer el email del interior del token -----------------
        // Si el token está manipulado, extractEmail() lanzará una excepción
        // y la petición llegará al controller sin autenticación.
        String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            // El token ha sido manipulado seguro asíque continua pero, sin autenticar, ya me encargaré de decir que no puede pasar
            filterChain.doFilter(request, response);
            return;
        }

        //-------------- PASO 5: Comprobar que no hay ya una autenticación activa -----------------
        // SecurityContextHolder es el lugar donde Spring guarda quién está autenticado
        // durante el ciclo de vida de una petición.
        // Si ya hay alguien autenticado, no hace falta repetir el proceso.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            //--------------PASO 6: Buscar al usuario en la base de datos --------------------
            // Usamos el email extraído del token para recuperar el User completo.
            // Si el usuario fue eliminado de la BBDD, no lo encontraremos → sin acceso.
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null && jwtUtil.validateToken(token, email)) {

                //----------PASO 7: Registrar al usuario en el contexto de Spring -----------
                // Convertimos el rol del usuario (USER o ADMIN) en un formato
                // que Spring Security entiende: "ROLE_USER" o "ROLE_ADMIN"
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                );

                // UsernamePasswordAuthenticationToken es el objeto que Spring usa
                // para representar a un usuario autenticado.
                // Parámetros: (quien es, credenciales, sus permisos)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);

                // Añadimos detalles de la petición (IP, session, etc.)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Guardamos la autenticación en el contexto de Spring.
                // A partir de aquí, cualquier controller puede saber quién es el usuario.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        //--------------PASO 8: Continuar con la cadena de filtros--------------------
        // Independientemente de si autenticamos o no, dejamos pasar la petición.
        // Si el endpoint requiere autenticación y no la hay,
        // Spring Security devolverá un 401 automáticamente.
        filterChain.doFilter(request, response);
    }
}