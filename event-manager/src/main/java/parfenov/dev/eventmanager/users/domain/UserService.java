package parfenov.dev.eventmanager.users.domain;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import parfenov.dev.eventmanager.users.db.UserEntityMapper;
import parfenov.dev.eventmanager.users.db.UserRepository;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserEntityMapper userEntityMapper;

    public UserService(UserRepository userRepository, UserEntityMapper userEntityMapper) {
        this.userRepository = userRepository;
        this.userEntityMapper = userEntityMapper;
    }

    public User saveUser(User user) {
        log.info("Saving user id={} login={}", user.id(), user.login());
        var entity = userEntityMapper.toEntity(user);
        var savedUser = userRepository.save(entity);
        return userEntityMapper.toDomain(savedUser);
    }

    public boolean isUserExistsByLogin(String login) {
        return userRepository.findByLogin(login).isPresent();
    }

    public User getUserByLogin(String login) {
        return userRepository
                .findByLogin(login)
                .map(userEntityMapper::toDomain)
                .orElseThrow(
                () -> new EntityNotFoundException("User wasn't fount login=%s".formatted(login))
        );
    }

    public User getUserById(Long userId) {
        return userRepository
                .findById(userId)
                .map(userEntityMapper::toDomain)
                .orElseThrow(
                        () -> new EntityNotFoundException("User wasn't fount id=%s".formatted(userId))
                );
    }

}
