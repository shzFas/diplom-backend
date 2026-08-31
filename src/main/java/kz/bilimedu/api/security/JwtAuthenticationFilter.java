package kz.bilimedu.api.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import kz.bilimedu.api.common.ErrorCode;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Разбирает Bearer-токен и кладёт principal в SecurityContext.
 *
 * <p>Фильтр не отвечает 401 сам: он лишь помечает причину в атрибуте запроса,
 * а ответ формирует {@link RestAuthenticationEntryPoint} — так тело ошибки
 * собирается тем же кодом, что и все остальные ошибки API.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String ERROR_ATTRIBUTE = "bilimedu.authError";

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length()).trim();
        try {
            AuthenticatedUser user = jwtService.parseAccessToken(token);
            var authority = new SimpleGrantedAuthority("ROLE_" + user.role().name());
            var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException e) {
            request.setAttribute(ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute(ERROR_ATTRIBUTE, ErrorCode.TOKEN_INVALID);
        }

        chain.doFilter(request, response);
    }
}
