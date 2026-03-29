package com.tfg.tfg_redsocial.services;

import com.tfg.tfg_redsocial.exception.ResourceNotFoundException;
import com.tfg.tfg_redsocial.models.Follow;
import com.tfg.tfg_redsocial.models.User;
import com.tfg.tfg_redsocial.repositories.FollowRepository;
import com.tfg.tfg_redsocial.repositories.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FollowService - Lógica de seguir/dejar de seguir usuarios
 */
@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final ProfileRepository profileRepository;

    public FollowService(FollowRepository followRepository,
                         ProfileRepository profileRepository) {
        this.followRepository = followRepository;
        this.profileRepository = profileRepository;
    }

    // =================================================================
    // POST /api/follows/{username} → Seguir a un usuario
    // =================================================================

    /**
     * El usuario actual sigue al usuario con el username indicado.
     *
     * Reglas:
     *   - No puedes seguirte a ti mismo
     *   - No puedes seguir a alguien que ya sigues (idempotente)
     */
    @Transactional
    public void follow(User currentUser, String targetUsername) {
        var targetProfile = profileRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: @" + targetUsername));

        User targetUser = targetProfile.getUser();

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new RuntimeException("No puedes seguirte a ti mismo");
        }

        boolean alreadyFollowing = followRepository.existsByFollowerIdAndFollowingId(
                currentUser.getId(), targetUser.getId());

        if (!alreadyFollowing) {
            Follow follow = Follow.builder()
                    .follower(currentUser)
                    .following(targetUser)
                    .build();

            followRepository.save(follow);
        }
    }

    // =================================================================
    // DELETE /api/follows/{username} → Dejar de seguir
    // =================================================================

    /**
     * El usuario actual deja de seguir al usuario con el username indicado.
     */
    @Transactional
    public void unfollow(User currentUser, String targetUsername) {
        var targetProfile = profileRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: @" + targetUsername));

        User targetUser = targetProfile.getUser();

        followRepository.deleteByFollowerIdAndFollowingId(
                currentUser.getId(), targetUser.getId());
    }
}
