package com.tfg.tfg_redsocial.services;

import com.tfg.tfg_redsocial.dtos.AuthResponse;
import com.tfg.tfg_redsocial.dtos.LoginRequest;
import com.tfg.tfg_redsocial.dtos.RegisterRequest;
import com.tfg.tfg_redsocial.models.Profile;
import com.tfg.tfg_redsocial.models.Role;
import com.tfg.tfg_redsocial.models.User;
import com.tfg.tfg_redsocial.repositories.ProfileRepository;
import com.tfg.tfg_redsocial.repositories.UserRepository;
import com.tfg.tfg_redsocial.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService - Lógica de negocio del registro y login
 *
 * @Service → Spring gestiona esta clase como un componente de servicio.
 * Aquí vive toda la lógica: validaciones, BCrypt, creación de entidades, JWT.
 * El Controller solo llama a estos métodos y devuelve el resultado.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;   // BCrypt, definido en SecurityConfig
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Inyección de dependencias por constructor.
     * Spring pone automaticamente las dependencias necesarias.
     */
    public AuthService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // ==============================================================
    // MÉTODO 1: REGISTRO
    // ==============================================================

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @Transactional garantiza que si algo falla a mitad del proceso
     * (por ejemplo, al crear el perfil), toda la operación se deshace.
     * No queremos usuarios sin perfil ni perfiles sin usuario en la BBDD.
     *
     * Es como un RollBack si sale algo más básicamente
     *
     * Flujo:
     *   1. Validar que email y username no existan ya
     *   2. Hashear la contraseña con BCrypt
     *   3. Crear y guardar el User en la base de datos
     *   4. Crear y guardar el Profile asociado
     *   5. Generar token JWT
     *   6. Devolver AuthResponse con el token y datos del usuario
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // ====================PASO 1A: Validar que el email no esté en uso===========================
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("El email ya está registrado: " + request.email());
        }

        // ================PASO 1B: Validar que el username no esté en uso =============================
        if (profileRepository.existsByUsername(request.username())) {
            throw new RuntimeException("El username ya está en uso: " + request.username());
        }

        // =============== PASO 2: Hashear la contraseña con BCrypt ========================
        // passwordEncoder.encode() aplica BCrypt a la contraseña en texto plano.
        // El resultado es algo como: "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
        // Esto es lo que tengo que guardar en la base de datos, NUNCA la contraseña original.
        String hashedPassword = passwordEncoder.encode(request.password());

        // ======================== PASO 3: Crear y guardar el User    ====================
        // Usamos el patrón Builder de Lombok para construir el objeto de forma legible.
        // El rol por defecto es USER, como definimos en la entidad con @Builder.Default.
        User createduser = User.builder()
                .email(request.email())
                .password(hashedPassword)   // contraseña hasheada, nunca la original
                .role(Role.USER)
                .isActive(true)
                .build();

        // save() guarda el User en la tabla users y devuelve el User con el ID asignado.
        // Necesitamos ese ID para crear el Profile asociado.
        createduser = userRepository.save(createduser);

        // ================ PASO 4: Crear y guardar el Profile asociado ======================
        // Cada User tiene exactamente un Profile (relación 1:1).
        // El Profile contiene la información pública: username, displayName, bio...
        Profile profile = Profile.builder()
                .user(createduser)                         // FK hacia el User recién creado
                .username(request.username())
                .displayName(request.displayName())
                .build();

        profileRepository.save(profile);

        // ======================= PASO 5: Generar token JWT ==========================
        // El token contiene el email del usuario y expira en 24h (según properties).
        String token = jwtUtil.generateToken(createduser.getEmail());

        // ============= PASO 6: Construir y devolver la respuesta ==================================
        // AuthResponse.of() es el método de conveniencia que creamos en el DTO.
        // Devolvemos el token y los datos básicos que el frontend necesita.
        return AuthResponse.of(
                token,
                createduser.getId(),
                createduser.getEmail(),
                profile.getUsername(),
                profile.getDisplayName(),
                createduser.getRole().name()
        );
    }

    // ==============================================================
    // MÉTODO 2: LOGIN
    // ==============================================================

    /**
     * Autentica a un usuario existente y devuelve un token JWT.
     *
     * Flujo:
     *   1. Usar AuthenticationManager para verificar email + contraseña
     *   2. Si las credenciales son incorrectas, AuthenticationManager lanza una excepción
     *   3. Buscar el usuario en la base de datos
     *   4. Buscar el perfil para obtener username y displayName
     *   5. Generar un nuevo token JWT
     *   6. Devolver AuthResponse
     */
    public AuthResponse login(LoginRequest request) {

        // ======================== PASO 1 y 2: Verificar credenciales ========================
        // AuthenticationManager usa BCrypt internamente para comparar la contraseña
        // introducida con el hash guardado en la base de datos.
        // Si son incorrectas, lanza BadCredentialsException automáticamente
        // y Spring devuelve un 401 sin que tengamos que hacer nada más.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // ======================== PASO 3: Buscar el usuario ========================
        // Si llegamos aquí, las credenciales son correctas.
        // Recuperamos el User completo de la base de datos.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ======================== PASO 4: Buscar el perfil ========================
        // Necesitamos el username y displayName para incluirlos en la respuesta.
        // El frontend los necesita para mostrar quién está logueado.
        Profile profile = profileRepository.findByUsername(
                user.getProfile().getUsername()
        ).orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        // ======================== PASO 5: Generar nuevo token JWT ========================
        String token = jwtUtil.generateToken(user.getEmail());

        // ======================== PASO 6: Devolver la respuesta ==================
        return AuthResponse.of(
                token,
                user.getId(),
                user.getEmail(),
                profile.getUsername(),
                profile.getDisplayName(),
                user.getRole().name()
        );
    }
}