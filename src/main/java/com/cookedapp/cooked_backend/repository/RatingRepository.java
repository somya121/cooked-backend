package com.cookedapp.cooked_backend.repository;

import com.cookedapp.cooked_backend.entity.Rating;
import com.cookedapp.cooked_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByBookingIdAndRatedByUserId(Long bookingId, Long userId);
    List<Rating> findByRatedCook(User ratedCook);

    @Query("SELECT AVG(r.ratingValue) FROM Rating r WHERE r.ratedCook.id = :cookId")
    Optional<Double> findAverageRatingByCookId(@Param("cookId") Long cookId);

    Long countByRatedCook(User ratedCook);
}