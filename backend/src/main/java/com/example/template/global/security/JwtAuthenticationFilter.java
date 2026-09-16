package com.example.template.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization 헤더의 Bearer 토큰을 검사해 SecurityContext에 인증 정보를 넣는다.
 *
 * <p>토큰이 없거나 잘못되었으면 그냥 익명 상태로 통과시킨다. 실제 접근 거부는 인가 설정이 판단한다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            jwtProvider.parse(token).ifPresent(JwtAuthenticationFilter::authenticate);
        }
        chain.doFilter(request, response);
    }

    private static void authenticate(AuthPrincipal principal) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
