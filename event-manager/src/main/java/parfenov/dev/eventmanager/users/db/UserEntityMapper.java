package parfenov.dev.eventmanager.users.db;

import org.springframework.stereotype.Component;
import parfenov.dev.eventmanager.users.domain.User;
import parfenov.dev.eventmanager.users.domain.UserRole;

@Component
public class UserEntityMapper {

    public UserEntity toEntity(User user) {
        return new UserEntity(
                user.id(),
                user.login(),
                user.age(),
                user.role().name(),
                user.passwordHash()
        );
    }

    public User toDomain(UserEntity userEntity) {
        return new User(
                userEntity.getId(),
                userEntity.getLogin(),
                userEntity.getAge(),
                UserRole.valueOf(userEntity.getRole()),
                userEntity.getPasswordHash()
        );
    }

}
