package com.example.mstemplateredis.v1.model;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @NotBlank(message = "IBAN is required")
    @Pattern(regexp = "[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}", message = "Invalid IBAN format")
    private String iban;       // IBAN of the account

    @NotNull(message = "Customer ID is required")
    @NotBlank(message = "Customer must not be blank")
    private String customerId; // ID of the customer

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    private BigDecimal balance;     // Account balance

    private String createdAt = Instant.now().toString();   // Timestamp for record creation

    private String updatedAt = Instant.now().toString();
}
