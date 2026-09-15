package com.doctrip.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    private String email;

    @NotBlank(message = "Mobile number is required.")
    @Pattern(regexp = "^[0-9]{10}$", message = "Enter a 10-digit mobile number.")
    private String mobile;

    @NotBlank(message = "Password is required.")
    private String password;

    /** Frontend requires accepting Terms of Service/Privacy Policy before submitting registration. */
    @AssertTrue(message = "Please accept the Terms of Service to continue.")
    private boolean agreeToTerms;
}
