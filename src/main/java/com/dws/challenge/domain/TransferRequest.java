package com.dws.challenge.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

    @NotEmpty(message = "Account from must not be null or empty")
    private String accountFromId;

    @NotEmpty(message = "Account to must not be null or empty")
    private String accountToId;

    @NotNull(message = "Amount to transfer cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;


}
