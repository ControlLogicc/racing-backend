package com.solofounder.horseracing.dto.admin;

import com.solofounder.horseracing.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInternalAccountRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    private String staffCode;
    private String department;
    private String position;

    private BigDecimal weight;
    private Short experienceYears;
    private BigDecimal height;
    private String nationality;
    private String achievements;
    @Size(max = 2048, message = "Image URL must be at most 2048 characters")
    private String imageUrl;
    private LocalDate dateOfBirth;

    private String licenseNumber;
    private String licenseNo;
    private String status;
}
