package com.example.sales_expense_system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name= "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotNull
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotNull
    @Column(nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Builder.Default
    @Column(nullable = false)
    private Integer inactivityTimeout = 300; //5mins (default)

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 10)
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_2fa_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    public enum UserStatus {
        ACTIVE,
        INACTIVE
    }
}
