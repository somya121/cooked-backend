package com.cookedapp.cooked_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String cookname;
    private String phone;
    private String availabilityStatus;
    @Column(name = "profile_picture")
    private String profilePicture;
    private Double latitude;
    private Double longitude;
    private Double chargesPerMeal;
    private String placeName;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_expertise", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "expertise")
    @Builder.Default
    private List<String> expertise = new ArrayList<>();

    @Builder.Default
    private String roles = "ROLE_USER";
    @Builder.Default
    private String status = "PENDING_COOK_PROFILE";
    @Column(unique = true, nullable = true)
    private String setupToken;
    private LocalDateTime setupTokenExpiry;
    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double averageRating = 0.0;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer numberOfRatings = 0;

    @OneToMany(mappedBy = "ratedCook", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Rating> ratingsReceived;

    @OneToMany(mappedBy = "ratedByUser", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Rating> ratingsGiven;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.roles == null || this.roles.trim().isEmpty()) {
            return List.of();
        }
        return List.of(this.roles.split(","))
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(this.status) || "PENDING_COOK_PROFILE".equals(this.status); }

}