package com.example.World.External.Payments;

import jakarta.validation.constraints.NotEmpty;

public record BillingDTO(
        @NotEmpty
        String fullName,
        @NotEmpty
        String city,
        @NotEmpty
        String country,
        @NotEmpty
        String line1,
        String line2,
        @NotEmpty
        String postalCode,
        @NotEmpty
        String expMonth,
        @NotEmpty
        String expYear
) {
}
