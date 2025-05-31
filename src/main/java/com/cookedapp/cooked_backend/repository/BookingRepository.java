package com.cookedapp.cooked_backend.repository;

import com.cookedapp.cooked_backend.entity.Booking;
import com.cookedapp.cooked_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomer(User customer);
    List<Booking> findByCook(User cook);
    List<Booking> findByCookAndBookingStatus(User cook, String status);
    List<Booking> findByCustomerAndBookingStatus(User customer, String status);
}