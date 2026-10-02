package com.princekumar.itams.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads {@code Authorization: Bearer <jwt>}, verifies the token, loads
 * {@link AppUserDetails} for the subject, and populates the
 * {@link SecurityContextHolder}.
 *
 * <p>Loading {@code AppUserDetails} costs one DB round-trip per
 * authenticated request — the standard trade-off. It's what gives
 * controllers a rich {@code @AuthenticationPrincipal} they can pull
 * ids from, and it also picks up a live {@code enabled=false} on the
 * user account without waiting for the JWT to expire.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length());
        try {
            Claims claims = jwtService.parseAccessToken(token);
            String username = claims.getSubject();

            AppUserDetails userDetails = (AppUserDetails) userDetailsService.loadUserByUsername(username);
            if (!userDetails.isEnabled()) {
                // Account disabled after token issue — refuse without waiting for TTL.
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            log.debug("Rejecting request with invalid JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }
}
