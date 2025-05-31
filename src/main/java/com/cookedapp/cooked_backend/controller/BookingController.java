package com.cookedapp.cooked_backend.controller;

import com.cookedapp.cooked_backend.dto.BookingRequestDTO;
import com.cookedapp.cooked_backend.dto.BookingResponseDTO;
import com.cookedapp.cooked_backend.dto.BookingStatusUpdateRequestDTO;
import com.cookedapp.cooked_backend.dto.MessageResponse;
import com.cookedapp.cooked_backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequestDTO bookingRequestDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        try {
            BookingResponseDTO booking = bookingService.createBooking(bookingRequestDTO, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create booking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating booking: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not create booking."));
        }
    }

    @PutMapping("/{bookingId}/status")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingStatusUpdateRequestDTO statusUpdateDTO,
            @AuthenticationPrincipal UserDetails currentCook) {
        if (currentCook == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        try {
            BookingResponseDTO updatedBooking = bookingService.updateBookingStatus(bookingId, statusUpdateDTO.getNewStatus(), currentCook);
            return ResponseEntity.ok(updatedBooking);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to update booking {} status: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating booking {} status: ", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not update booking status."));
        }
    }


    @GetMapping("/cook/me")
    public ResponseEntity<?> getMyCookBookings(@AuthenticationPrincipal UserDetails currentCook) {
        if (currentCook == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        try {
            List<BookingResponseDTO> bookings = bookingService.getCookBookings(currentCook);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error fetching cook bookings for {}: ", currentCook.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not fetch cook bookings."));
        }
    }


    @GetMapping("/user/me")
    public ResponseEntity<?> getMyUserBookings(@AuthenticationPrincipal UserDetails currentCustomer) {
        if (currentCustomer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        try {
            List<BookingResponseDTO> bookings = bookingService.getCustomerBookings(currentCustomer);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error fetching customer bookings for {}: ", currentCustomer.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not fetch customer bookings."));
        }
    }
    @PutMapping("/{bookingId}/complete-service")
    public ResponseEntity<?> completeService(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails currentCook) {
        if (currentCook == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        try {
            BookingResponseDTO updatedBooking = bookingService.markServiceAsComplete(bookingId, currentCook);
            return ResponseEntity.ok(updatedBooking);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to mark service complete for booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error marking service complete for booking {}: ", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not mark service as complete."));
        }
    }
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> deleteBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        try {
            bookingService.deleteBooking(bookingId, currentUser);
            return ResponseEntity.ok(new MessageResponse("Booking deleted successfully."));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Failed to delete booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting booking {}: ", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not delete booking."));
        }
    }
    @PutMapping("/{bookingId}/receive-payment")
    public ResponseEntity<?> receivePayment(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails currentCook) {
        if (currentCook == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("User not authenticated."));
        }
        try {
            BookingResponseDTO updatedBooking = bookingService.markPaymentAsReceived(bookingId, currentCook);
            return ResponseEntity.ok(updatedBooking);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to mark payment received for booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error marking payment received for booking {}: ", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Could not mark payment as received."));
        }
    }
}