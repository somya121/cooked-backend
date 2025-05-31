package com.cookedapp.cooked_backend.repository;

import com.cookedapp.cooked_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findBySetupToken(String setupToken);

    @Query(value = "SELECT u.*, calc.distance " +
            "FROM users u JOIN " +
            "     (SELECT id, (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
            "                               cos(radians(longitude) - radians(:lon)) + " +
            "                               sin(radians(:lat)) * sin(radians(latitude)))) AS distance " +
            "      FROM users) AS calc ON u.id = calc.id " +
            "WHERE u.roles LIKE '%ROLE_COOK%' " +
            "  AND u.status = 'ACTIVE' " +
            "  AND calc.distance < :radius " +
            "ORDER BY calc.distance", nativeQuery = true)
    List<User> findActiveCooksNearbyNativeWithDistance(
            @Param("lat") double latitude,
            @Param("lon") double longitude,
            @Param("radius") double radiusKm
    );
}