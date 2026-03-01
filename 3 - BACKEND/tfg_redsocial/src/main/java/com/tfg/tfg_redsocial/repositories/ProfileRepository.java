package com.tfg.tfg_redsocial.repositories;


import com.tfg.tfg_redsocial.models.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    // Para ver el perfil público por URL (/profiles/username)
    Optional<Profile> findByUsername(String username);

    // Esto es importante, por que si alguien se registra y ya existe el usuario, quiero que se le comunique: Oye! El user está pillado
    boolean existsByUsername(String username);

    // Para el buscador de usuarios (busca por username O display_name)
    List<Profile> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username, String displayName);
}