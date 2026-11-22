package parfenov.dev.eventmanager.events.api;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record EventCreateRequestDto(
        @NotBlank(message = "Name is mandatory")
        String name,

        @Positive(message = "Maximum places must be greater than zero")
        int maxPlaces,

        @Future(message = "Date must be in future")
        LocalDateTime date,

        @PositiveOrZero(message = "Cost must be non-negative")
        int cost,

        @Min(value = 30, message = "Duration must be greater than 30")
        int duration,

        @NotNull(message = "Location ID is mandatory")
        Long locationId
) {
}
