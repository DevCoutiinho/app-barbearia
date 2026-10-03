package dev.barbershop.api.user.repository;

import dev.barbershop.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    @Query("""
                 SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email
             """)
    Optional<User> findByEmailWitchRoles(@Param("email") String email);

    @Override
    @EntityGraph(attributePaths = {"barberProfile", "clientProfile"})
     Page<User> findAll(Specification<User> spec, Pageable pageable);
}
