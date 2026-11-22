package parfenov.dev.eventmanager.users.api;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import parfenov.dev.eventmanager.users.domain.AuthenticationService;
import parfenov.dev.eventmanager.users.domain.UserRegistrationService;
import parfenov.dev.eventmanager.users.domain.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRegistrationService userRegistrationService;
    private final UserDtoMapper userDtoMapper;
    private final AuthenticationService authenticationService;

    public UserController(
            UserService userService,
            UserRegistrationService userRegistrationService,
            UserDtoMapper userDtoMapper,
            AuthenticationService authenticationService
    ) {
        this.userService = userService;
        this.userRegistrationService = userRegistrationService;
        this.userDtoMapper = userDtoMapper;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<UserDto> registerUser(
            @RequestBody @Valid SignUpRequest signUpRequest
    ) {
        log.info("Get request for sign-in: login={}", signUpRequest.login());
        var user = userRegistrationService.registerUser(signUpRequest);
        return ResponseEntity.status(201).body(userDtoMapper.toDto(user));
    }

    @PostMapping("/auth")
    public ResponseEntity<JwtTokenResponse> authenticate(
            @RequestBody @Valid SignInRequest signInRequest
    ) {
        log.info("Get request for sign-in: login={}", signInRequest.login());
        var token = authenticationService.authenticateUser(signInRequest);
        return ResponseEntity.ok(new JwtTokenResponse(token));


    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserInfo(
            @PathVariable Long userId
    ) {
        log.info("Get request for user: id={}", userId);
        var user = userService.getUserById(userId);
        return ResponseEntity.ok(userDtoMapper.toDto(user));

    }

}
