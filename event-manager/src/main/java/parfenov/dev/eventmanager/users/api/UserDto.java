package parfenov.dev.eventmanager.users.api;

import parfenov.dev.eventmanager.users.domain.UserRole;

public record UserDto(
        Long id,
        String login,
        Integer age,
        UserRole role
) {
}
