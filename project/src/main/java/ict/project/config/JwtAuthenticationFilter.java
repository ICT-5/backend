package ict.project.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ict.project.user.service.JwtUtil;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/api/users/login") || uri.equals("/api/users/signup");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        String header = request.getHeader("Authorization");
        log.debug("📌 Incoming request: {} {}", request.getMethod(), request.getRequestURI());
        log.debug("📌 Authorization header: {}", header);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            log.debug("📌 Extracted JWT: {}", token);

            String email = jwtUtil.validateToken(token);
            log.debug("📌 validateToken result (email): {}", email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails user = userDetailsService.loadUserByUsername(email);
                    log.debug("📌 Loaded UserDetails: {}", user.getUsername());

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("✅ SecurityContextHolder updated with authentication for {}", email);
                } catch (Exception e) {
                    log.error("❌ Failed to set authentication for {}: {}", email, e.getMessage());
                }
            }
        } else {
            log.debug("📌 No Bearer token found, skipping authentication");
        }

        filterChain.doFilter(request, response);
    }
}
