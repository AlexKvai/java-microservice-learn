package parfenov.dev.eventnotificator.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class NotificationJwtFilter extends OncePerRequestFilter {
    private final SecretKey key;
    public NotificationJwtFilter(@Value("${jwt.sign-key}") String secret){
        key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,
                                               FilterChain chain) throws ServletException,IOException {
        String header=request.getHeader(HttpHeaders.AUTHORIZATION);
        if(header!=null && header.startsWith("Bearer ")) {
            try {
                var claims=Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(header.substring(7)).getPayload();
                Number id=claims.get("userId",Number.class);
                String role=claims.get("role",String.class);
                if(id!=null && role!=null && claims.getSubject()!=null) {
                    var principal=new JwtUser(id.longValue(),claims.getSubject(),role);
                    var authentication=new UsernamePasswordAuthenticationToken(principal,null,
                        List.of(new SimpleGrantedAuthority(role)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException invalid) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request,response);
    }
}
