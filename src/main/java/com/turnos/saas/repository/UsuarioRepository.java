package com.turnos.saas.repository;

import com.turnos.saas.model.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Page<Usuario> findByEmpresaId(UUID empresaId, Pageable pageable);

    Page<Usuario> findByEmpresaIdAndActivoTrue(UUID empresaId, Pageable pageable);

    Optional<Usuario> findByIdAndActivoTrue(UUID id);
}
