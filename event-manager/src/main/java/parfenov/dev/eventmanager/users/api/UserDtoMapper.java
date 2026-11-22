package parfenov.dev.eventmanager.users.api;

import org.springframework.stereotype.Component;
import parfenov.dev.eventmanager.users.domain.User;

@Component
public class UserDtoMapper {

    public UserDto toDto(User user) {
        return new UserDto(
                user.id(),
                user.login(),
                user.age(),
                user.role()
        );
    }
}
