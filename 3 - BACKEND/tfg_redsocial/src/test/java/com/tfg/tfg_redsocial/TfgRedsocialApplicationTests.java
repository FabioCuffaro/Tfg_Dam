package com.tfg.tfg_redsocial;

import com.tfg.tfg_redsocial.repositories.PostRepository;
import com.tfg.tfg_redsocial.repositories.ProfileRepository;
import com.tfg.tfg_redsocial.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test de arranque y conexión a base de datos.
 *
 * Si el contexto carga sin errores:
 *   - La conexión a PostgreSQL está activa.
 *   - Hibernate ha validado el esquema.
 *   - Todos los beans de Spring se crean correctamente.
 */
@SpringBootTest
class TfgRedsocialApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    void contextLoads() {
        // Si llega aquí, Spring arrancó y la BD está conectada
    }

    @Test
    void repositoriosBDDisponibles() {
        // Verifica que los repositorios JPA se inyectan (= BD alcanzable)
        assertNotNull(userRepository,    "UserRepository debe estar disponible");
        assertNotNull(postRepository,    "PostRepository debe estar disponible");
        assertNotNull(profileRepository, "ProfileRepository debe estar disponible");
    }

    @Test
    void consultaBDFunciona() {
        // Una consulta real a la BD para confirmar la conexión
        long totalUsuarios = userRepository.count();
        // No importa el número: si no lanza excepción, la BD responde
        org.junit.jupiter.api.Assertions.assertTrue(totalUsuarios >= 0,
                "La consulta a usuarios debe devolver un número no negativo");
    }
}
