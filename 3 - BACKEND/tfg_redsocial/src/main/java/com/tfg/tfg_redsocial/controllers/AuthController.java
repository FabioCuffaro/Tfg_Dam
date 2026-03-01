package com.tfg.tfg_redsocial.controllers;

import com.tfg.tfg_redsocial.dtos.AuthResponse;
import com.tfg.tfg_redsocial.dtos.LoginRequest;
import com.tfg.tfg_redsocial.dtos.RegisterRequest;
import com.tfg.tfg_redsocial.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - Endpoints públicos de registro y login
 *
 * @RestController → Combina @Controller + @ResponseBody.
 *                   Todos los métodos devuelven JSON automáticamente.
 *
 * @RequestMapping → Prefijo común para todos los endpoints de este controller.
 *                   Todos empezarán por /api/auth/
 *
 * Estos endpoints están marcados como públicos en SecurityConfig (.permitAll()),
 * por lo que no requieren token JWT para ser llamados.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Inyección de dependencias por constructor.
     * Spring proporciona el AuthService automáticamente.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ===============================================================
    // POST /api/auth/register
    // ===============================================================

    /**
     * Registra un nuevo usuario.
     *
     * @Valid activa las validaciones definidas en RegisterRequest
     * (@NotBlank, @Email, @Size, @Pattern...).
     * Si algún campo no es válido, Spring devuelve 400 Bad Request
     * automáticamente sin llegar al Service.
     *
     * Devuelve 201 Created con el token y datos del usuario.
     *
     * Ejemplo de body JSON:
     * {
     *   "username": "fabio",
     *   "displayName": "Fabio Cuffaro",
     *   "email": "fabio@gmail.com",
     *   "password": "mipassword123"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        // 201 Created es el código HTTP semánticamente correcto para creación de recursos
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ===============================================================
    // POST /api/auth/login
    // ===============================================================

    /**
     * Autentica a un usuario existente y devuelve un token JWT.
     *
     * Si las credenciales son incorrectas, AuthService lanza una excepción
     * que Spring convierte en 401 Unauthorized automáticamente.
     *
     * Devuelve 200 OK con el token y datos del usuario.
     *
     * Ejemplo de body JSON:
     * {
     *   "email": "fabio@gmail.com",
     *   "password": "mipassword123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        // 200 OK es el código correcto para login exitoso
        return ResponseEntity.ok(response);
    }
}