package parfenov.dev.eventnotificator.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class NotificationSecurityConfig {
    private final ObjectMapper mapper=new ObjectMapper().registerModule(new JavaTimeModule());
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,NotificationJwtFilter filter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(sm->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex->ex
                .authenticationEntryPoint((request,response,error)->{
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(mapper.writeValueAsString(Map.of(
                        "message","Unauthorized","detailedMessage","Valid JWT is required", "dateTime",LocalDateTime.now().toString())));
                })
                .accessDeniedHandler((request,response,error)->{
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(mapper.writeValueAsString(Map.of(
                        "message","Forbidden","detailedMessage","Insufficient permissions", "dateTime",LocalDateTime.now().toString())));
                }))
            .authorizeHttpRequests(auth->auth
                .requestMatchers("/error","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
                .requestMatchers("/notifications/**","/notifications").authenticated()
                .anyRequest().denyAll())
            .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean public FilterRegistrationBean<NotificationJwtFilter> filterRegistration(NotificationJwtFilter filter) {
        var registration=new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
