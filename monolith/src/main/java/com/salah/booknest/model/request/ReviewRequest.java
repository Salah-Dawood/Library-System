package com.salah.booknest.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(@NotNull(message = "Rating can not be empty") Integer rating,
                            String comment) {
}
