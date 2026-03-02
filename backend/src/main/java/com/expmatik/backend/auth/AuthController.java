package com.expmatik.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expmatik.backend.auth.DTOs.AuthRequest;
import com.expmatik.backend.auth.DTOs.AuthRequestLogin;
import com.expmatik.backend.auth.DTOs.AuthResponse;
import com.expmatik.backend.auth.DTOs.AuthResult;
import com.expmatik.backend.auth.DTOs.RefreshTokenRequest;
import com.expmatik.backend.auth.DTOs.ValidateTokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints para autenticación y autorización")
public class AuthController {

    private final int secondsExpirationRt;
    private final AuthService authService;

    public AuthController(AuthService authService, @Value("${jwt.expiration-rt}") int expirationRt) {
        this.authService = authService;
        this.secondsExpirationRt = expirationRt/1000;

    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario", description = "Crea una nueva cuenta de usuario")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AuthResult registerRes = authService.register(request.email(), request.password(), request.deviceId(), request.role(), request.firstName(), request.lastName());
        Cookie cookie = new Cookie("refresh_token", registerRes.refreshToken());
        cookie.setMaxAge(secondsExpirationRt);
        cookie.setSecure(false);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        AuthResponse authResponse = new AuthResponse(registerRes.accessToken(), registerRes.role());
        response.addCookie(cookie);
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario con email y contraseña")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequestLogin request, HttpServletResponse response) {
        AuthResult loginRes = authService.login(request.email(), request.password(), request.deviceId());
        Cookie cookie = new Cookie("refresh_token", loginRes.refreshToken());
        cookie.setMaxAge(secondsExpirationRt);
        cookie.setSecure(false);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        AuthResponse authResponse = new AuthResponse(loginRes.accessToken(), loginRes.role());
        response.addCookie(cookie);
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Obtiene un nuevo access token usando el refresh token")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest,
                                                @CookieValue(value = "refresh_token", required = true) String token,
                                                HttpServletResponse response) {
        AuthResult refreshRes = authService.refreshToken(token, refreshTokenRequest.deviceId());
        Cookie cookie = new Cookie("refresh_token", refreshRes.refreshToken());
        cookie.setMaxAge(secondsExpirationRt);
        cookie.setSecure(false);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        AuthResponse authResponse = new AuthResponse(refreshRes.accessToken(), refreshRes.role());
        response.addCookie(cookie);
        return ResponseEntity.ok().body(authResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida el refresh token y cierra la sesión")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refresh_token", required = true) String token,
            @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletResponse response) {

        authService.logout(token, refreshTokenRequest.deviceId());

        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");

        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    @Operation(summary = "Validar token", description = "Verifica si un token JWT es válido y no está expirado")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ValidateTokenResponse> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(
                    new ValidateTokenResponse(false, "Missing token")
            );
        }

        String token = authHeader.substring(7);

        boolean valid = authService.validateAccessToken(token);

        if (!valid) {
            return ResponseEntity.ok(
                    new ValidateTokenResponse(false, "Invalid or expired token")
            );
        }

        return ResponseEntity.ok(new ValidateTokenResponse(true, "Token valid"));
    }

}



