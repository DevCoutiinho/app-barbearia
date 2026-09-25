package dev.barbershop.api.user.repository;

import dev.barbershop.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByEmailContainingIgnoreCase(String email);

    List<User> findByTelephoneContainingIgnoreCase(String telephone);

    @Query("""
                 SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email
             """)
    Optional<User> findByEmailWitchRoles(@Param("email") String email);
}
