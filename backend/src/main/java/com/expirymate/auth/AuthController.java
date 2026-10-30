package com.expirymate.auth;

import com.expirymate.user.User;
import com.expirymate.user.UserRepository;
import com.expirymate.shared.SharedListService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final SharedListService lists;
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public AuthController(UserRepository users, PasswordEncoder passwords,
                          AuthenticationManager authenticationManager, SharedListService lists) {
        this.users = users;
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.lists = lists;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册");
        }
        User user = users.saveAndFlush(new User(UUID.randomUUID().toString(), email,
                passwords.encode(request.password()), request.displayName().trim()));
        lists.create(user.getId(), "我的清单");
        return authenticate(email, request.password(), servletRequest, servletResponse);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        return authenticate(request.email().trim(), request.password(), servletRequest, servletResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal) {
        return principal == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
                : ResponseEntity.ok(AuthResponse.from(principal));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    private AuthResponse authenticate(String email, String password,
                                      HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password));
            if (request.getSession(false) != null) request.changeSessionId();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            return AuthResponse.from((UserPrincipal) authentication.getPrincipal());
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
    }
}
