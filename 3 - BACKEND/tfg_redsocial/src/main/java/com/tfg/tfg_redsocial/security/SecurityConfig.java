package com.tfg.tfg_redsocial.security;

import com.tfg.tfg_redsocial.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig - Director de orquesta de la seguridad
 *
 * @Configuration   → Esta clase contiene configuración de Spring
 * @EnableWebSecurity → Activa Spring Security con nuestra configuración personalizada
 * @EnableMethodSecurity → Permite usar @PreAuthorize en los controllers
 *                         (por ejemplo para endpoints solo de ADMIN)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserRepository userRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserRepository userRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userRepository = userRepository;
    }

    // =============================================================
    // BEAN 1: SecurityFilterChain
    // La configuración principal de seguridad HTTP
    // =============================================================

    /**
     * Define las reglas de seguridad HTTP de la aplicación.
     *
     * Un SecurityFilterChain es básicamente la lista de reglas que Spring
     * aplica a cada petición HTTP que recibe el servidor.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ============================CORS=================================
                // Permite que el frontend (en otro origen/puerto) llame a la API.
                // Sin esto, el navegador bloquea las peticiones del frontend al backend.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // =============================CRFS================================
                // CSRF (Cross-Site Request Forgery) es una protección para formularios HTML.
                // En APIs REST con JWT no se necesita porque el token ya protege las peticiones, asique la desabilito para que Spring lo sepa.
                .csrf(csrf -> csrf.disable())

                // ==========================SESIONES===================================
                // Con JWT no hay sesiones en el servidor. Cada petición se autentica
                // sola con su token. STATELESS significa que el servidor no guarda
                // ningún estado de sesión entre peticiones.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ==========================AUTORIZACIÓN===================================
                // Aquí definimos qué rutas son públicas y cuáles requieren token.
                .authorizeHttpRequests(auth -> auth

                        // Rutas públicas: no necesitan token
                        // El usuario aún no tiene token cuando se registra o hace login
                        .requestMatchers("/api/auth/**").permitAll()

                        // Rutas de prueba (TestController): públicas durante desarrollo
                        // IMPORTANTE: eliminar o proteger antes del despliegue en producción
                        .requestMatchers("/test/**").permitAll()

                        // Cualquier otra ruta requiere estar autenticado (tener token válido)
                        .anyRequest().authenticated()
                )

                // =========================PROVEEDOR====================================
                // Le decimos a Spring qué AuthenticationProvider usar.
                // El nuestro usa UserDetailsService + BCrypt (definidos abajo).
                .authenticationProvider(authenticationProvider())

                // ========================FILTRO JWT=====================================
                // Añadimos nuestro filtro ANTES del filtro estándar de Spring.
                // Así, cuando Spring intenta autenticar, ya encontrará al usuario
                // registrado en el SecurityContext por nuestro filtro.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =============================================================
    // BEAN 2: UserDetailsService
    // Cómo Spring Security carga un usuario por su identificador
    // =============================================================

    /**
     * UserDetailsService le dice a Spring Security cómo buscar un usuario.
     *
     * Spring Security necesita saber cómo cargar un usuario dado su identificador
     * (en nuestro caso, el email). Usamos una lambda que busca en UserRepository.
     *
     * Si el usuario no existe, lanzamos UsernameNotFoundException,
     * que Spring Security captura y convierte en un error 401.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            var user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Usuario no encontrado con email: " + email));

            // Convertimos nuestro User en un UserDetails que Spring Security entiende.
            // Le pasamos: email, password hasheado, y rol con prefijo "ROLE_"
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())) //Se devuelve una lista porque SpringSecuruty espera varios
                                                                                            //roles siempre, no solo uno, aunque yo vaya a usar solo 1
            );
        };
    }

    // =============================================================
    // BEAN 3: PasswordEncoder (BCrypt)
    // El hasheador de contraseñas
    // =============================================================

    /**
     * BCryptPasswordEncoder es el algoritmo que hashea las contraseñas.
     *
     * Lo definimos como @Bean para poder inyectarlo en AuthService
     * cuando necesitemos hashear la contraseña al registrar un usuario,
     * o verificarla al hacer login.
     *
     * BCrypt añade automáticamente un "salt" aleatorio a cada hash,
     * por lo que dos usuarios con la misma contraseña tendrán hashes distintos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =============================================================
    // BEAN 4: AuthenticationProvider
    // Conecta UserDetailsService + BCrypt
    // =============================================================

    /**
     * DaoAuthenticationProvider conecta el UserDetailsService con el PasswordEncoder.
     *
     * Cuando alguien intenta hacer login:
     *   1. Usa UserDetailsService para cargar el usuario por email
     *   2. Usa BCrypt para comparar la contraseña introducida con el hash guardado
     *   3. Si coinciden → autenticación exitosa
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // =============================================================
    // BEAN 5: AuthenticationManager
    // El componente que gestiona el proceso de autenticación
    // =============================================================

    /**
     * AuthenticationManager es el que ejecuta la autenticación completa.
     * Lo inyectaremos en AuthService para usarlo durante el login.
     *
     * Spring lo construye automáticamente a partir de AuthenticationConfiguration,
     * que ya conoce nuestro AuthenticationProvider.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // =============================================================
    // BEAN 6: CORS Configuration
    // Permite que el frontend acceda a la API
    // =============================================================

    /**
     * Configuración CORS (Cross-Origin Resource Sharing).
     *
     * El navegador bloquea por seguridad las peticiones entre orígenes distintos.
     * Por ejemplo, si el frontend está en localhost:5500 y el backend en localhost:8080,
     * el navegador bloqueará las peticiones sin esta configuración.
     *
     * En desarrollo permito cualquier origen ("*").
     * En producción cambiaré "*" por la URL real del frontend, EL CUAL espero desplegar en Vercel.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos: en producción poner la URL de Vercel
        config.setAllowedOriginPatterns(List.of("*"));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Cabeceras permitidas: incluimos Authorization para los tokens JWT
        config.setAllowedHeaders(List.of("*"));

        // Permitir cookies y cabeceras de autenticación
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplicar esta configuración a todas las rutas
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}