package dev.barbershop.api.client.repository;

import dev.barbershop.api.client.entity.Client;
import dev.barbershop.api.user.entity.User;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    default Optional<Client> findClientByUserId(UUID userId) {

        User userExample = User.builder()
                .id(userId)
                .password("QBE_PROBE")
                .build();

        Client clientExample = Client.builder()
                .user(userExample)
                .schedulings(null)
                .build();

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnorePaths(
                        "id",
                        "loyaltyPoints",
                        "schedulings",
                        "user.password",
                        "user.name",
                        "user.email",
                        "user.avatar",
                        "user.telephone",
                        "user.active",
                        "user.inventoryMovements",
                        "user.roles",
                        "user.notifications",
                        "user.barberProfile",
                        "user.clientProfile"
                );

        return findOne(Example.of(clientExample, matcher));
    }
}
