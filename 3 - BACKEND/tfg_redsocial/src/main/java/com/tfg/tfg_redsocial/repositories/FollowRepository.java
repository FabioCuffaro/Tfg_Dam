package com.tfg.tfg_redsocial.repositories;

import com.tfg.tfg_redsocial.models.Follow;
import com.tfg.tfg_redsocial.models.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // ¿Está el usuario A siguiendo al usuario B?
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // Eliminar el follow cuando se deja de seguir a alguien
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // Contar cuántos seguidores tiene un usuario (cuántos le siguen)
    long countByFollowingId(Long followingId);

    // Contar cuántos usuarios sigue alguien (a cuántos sigue)
    long countByFollowerId(Long followerId);

    // ======================================================
    // Devuelve Profile directamente en vez de User.
    //
    // ¿Por qué Profile y no User?
    // Cuando JPQL devuelve entidades User desde una proyección de asociación
    // (SELECT f.follower FROM Follow...), Hibernate NO inicializa el lado
    // mappedBy del @OneToOne (User.profile), dejándolo null aunque sea EAGER.
    // Acceder a u.getProfile().getUsername() lanzaría NullPointerException.
    // Al hacer la query directamente sobre Profile evitamos ese problema:
    // Profile.user sí es el lado dueño (@JoinColumn) y siempre se carga.
    // ======================================================

    // Perfiles de los usuarios que siguen a userId (sus seguidores)
    @Query("SELECT p FROM Profile p WHERE p.user.id IN " +
            "(SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId)")
    List<Profile> findFollowerProfilesByUserId(@Param("userId") Long userId);

    // Perfiles de los usuarios a los que sigue userId (a quiénes sigue)
    @Query("SELECT p FROM Profile p WHERE p.user.id IN " +
            "(SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId)")
    List<Profile> findFollowingProfilesByUserId(@Param("userId") Long userId);
}
