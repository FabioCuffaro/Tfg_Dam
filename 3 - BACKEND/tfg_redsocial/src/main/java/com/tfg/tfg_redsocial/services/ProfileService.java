package com.tfg.tfg_redsocial.services;

import com.tfg.tfg_redsocial.dtos.ProfileResponse;
import com.tfg.tfg_redsocial.dtos.UpdateProfileRequest;
import com.tfg.tfg_redsocial.exception.ResourceNotFoundException;
import com.tfg.tfg_redsocial.models.Profile;
import com.tfg.tfg_redsocial.models.User;
import com.tfg.tfg_redsocial.repositories.FollowRepository;
import com.tfg.tfg_redsocial.repositories.PostRepository;
import com.tfg.tfg_redsocial.repositories.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ProfileService - Lógica de negocio para perfiles
 *
 * Gestiona:
 *   - Ver el perfil de cualquier usuario por username
 *   - Editar el propio perfil
 *   - Buscar usuarios por username o nombre
 */
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    public ProfileService(ProfileRepository profileRepository,
                          PostRepository postRepository,
                          FollowRepository followRepository) {
        this.profileRepository = profileRepository;
        this.postRepository = postRepository;
        this.followRepository = followRepository;
    }

    // =================================================================
    // GET /api/profiles/{username} → Ver perfil público
    // =================================================================

    /**
     * Devuelve el perfil completo de un usuario por su username.
     * Incluye estadísticas calculadas (posts, seguidores, siguiendo)
     * y si el usuario actual ya le sigue.
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUsername(String username, User currentUser) {
        Profile profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado: @" + username));

        return toProfileResponse(profile, currentUser);
    }


    // =================================================================
    // PUT /api/profiles/me → Editar propio perfil
    // =================================================================

    /**
     * Actualiza los campos del perfil del usuario autenticado.
     *
     * Solo actualizamos los campos que llegan no-nulos en el request.
     * Así el frontend puede enviar solo los campos que cambió.
     */
    @Transactional
    public ProfileResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUsername(currentUser.getProfile().getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        if (request.displayName() != null && !request.displayName().isBlank()) {
            profile.setDisplayName(request.displayName());
        }
        // Bio, location y website pueden ser strings vacíos (para poder borrarlos si quieres :) )
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.location() != null) {
            profile.setLocation(request.location());
        }
        if (request.website() != null) {
            profile.setWebsite(request.website());
        }

        Profile saved = profileRepository.save(profile);
        return toProfileResponse(saved, currentUser);
    }



    // =================================================================
    // GET /api/profiles/{username}/followers → Lista de seguidores
    // =================================================================

    /**
     * Devuelve la lista de usuarios que siguen al usuario con ese username.
     *
     * Flujo:
     *   1. Busca el perfil por username para obtener el userId
     *   2. Ejecuta la query que devuelve los User del campo "follower"
     *      (es decir, todos los que tienen un Follow donde following.id = userId)
     *   3. Para cada User, buscamos su Profile y construimos un ProfileResponse
     *
     * @param username    El username del perfil que se está visitando
     * @param currentUser El usuario logueado (para saber si ya le sigue a cada uno)
     */
    @Transactional(readOnly = true)
    public List<ProfileResponse> getFollowers(String username, User currentUser) {
        Profile target = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado: @" + username));

        // La query devuelve Profile directamente → no hay riesgo de NullPointer
        // al acceder a User.profile (lado mappedBy que Hibernate no inicializa)
        return followRepository
                .findFollowerProfilesByUserId(target.getUser().getId())
                .stream()
                .map(p -> toProfileResponse(p, currentUser))
                .toList();
    }



    // =================================================================
    // GET /api/profiles/{username}/following → Lista de a quién sigue
    // =================================================================

        // Indica que este método es transaccional y solo de lectura (no modifica datos en BD)
        @Transactional(readOnly = true)
        public List<ProfileResponse> getFollowing(String username, User currentUser) {

        // Busca el perfil por username en la base de datos
        // Si no existe, lanza una excepción personalizada
        Profile target = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado: @" + username));

        // Llama al repositorio de Follow para obtener los perfiles que sigue ese usuario
        return followRepository
                // Busca los perfiles a los que sigue el usuario target (por su userId)
                .findFollowingProfilesByUserId(target.getUser().getId())
                // Convierte la lista en un stream para poder transformarla
                .stream()
                // Por cada Profile, lo transforma en un ProfileResponse (DTO)
                .map(p -> toProfileResponse(p, currentUser))
                // Convierte el stream de nuevo en lista
                .toList();
    }




    // =================================================================
    // GET /api/profiles/search?q=query → Buscar usuarios
    // =================================================================

        /**
         * Busca usuarios cuyo username o displayName contenga el término de búsqueda.
         * Devuelve máximo 10 resultados para no sobrecargar la UI del buscador.
         */

    // Transacción de solo lectura porque solo consulta datos
        @Transactional(readOnly = true)
        public List<ProfileResponse> searchProfiles(String query, User currentUser) {

            return profileRepository
                    // Busca perfiles donde username o displayName contengan el texto (ignorando mayúsculas/minúsculas)
                    .findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query)
                    // Convierte el resultado en stream para procesarlo
                    .stream()
                    // Limita los resultados a 10 (optimización para UI/autocomplete)
                    .limit(10)
                    // Convierte cada Profile en ProfileResponse
                    .map(p -> toProfileResponse(p, currentUser))
                    // Vuelve a convertir el stream en lista
                    .toList();
        }


    // =================================================================
    // MÉTODO PRIVADO: Profile → ProfileResponse
    // =================================================================

        // Método auxiliar que transforma la entidad Profile en un DTO ProfileResponse
        private ProfileResponse toProfileResponse(Profile profile, User currentUser) {

            // Cuenta cuántos posts ha publicado este usuario
            long postCount = postRepository.countByUserId(profile.getUser().getId());

            // Cuenta cuántos seguidores tiene este usuario
            long followerCount = followRepository.countByFollowingId(profile.getUser().getId());

            // Cuenta a cuántos usuarios sigue este usuario
            long followingCount = followRepository.countByFollowerId(profile.getUser().getId());

            // Comprueba si el usuario actual (logueado) sigue a este perfil
            boolean isFollowed = followRepository.existsByFollowerIdAndFollowingId(
                    currentUser.getId(),                      // ID del usuario actual
                    profile.getUser().getId()                // ID del perfil que se está evaluando
            );

            // Construye y devuelve el DTO con todos los datos necesarios para el frontend
            return new ProfileResponse(
                    profile.getId(),                         // ID del perfil
                    profile.getUser().getId(),              // ID del usuario
                    profile.getUsername(),                  // username (@handle)
                    profile.getDisplayName(),               // nombre visible
                    profile.getBio(),                       // biografía
                    profile.getAvatarUrl(),                 // URL del avatar
                    profile.getLocation(),                  // ubicación
                    profile.getWebsite(),                   // web personal
                    postCount,                              // número de posts
                    followerCount,                          // número de seguidores
                    followingCount,                         // número de seguidos
                    isFollowed                              // si el usuario actual lo sigue o no
            );
        }
}
