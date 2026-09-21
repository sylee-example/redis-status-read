package com.example.redissample.dto;

import com.example.redissample.domain.StoreStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull StoreStatus status) {
}
