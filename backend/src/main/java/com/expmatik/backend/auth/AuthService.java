package com.expmatik.backend.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.expmatik.backend.auth.dto.AuthResponse;
import com.expmatik.backend.auth.dto.LoginRequest;
import com.expmatik.backend.auth.dto.LogoutResponse;
import com.expmatik.backend.auth.dto.RegisterRequest;
import com.expmatik.backend.user.User;
import com.expmatik.backend.user.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAuthority(request.getAuthority());

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getFirstName(), user.getLastName());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getFirstName(), user.getLastName());
    }

    public LogoutResponse logout(String token) {
        try {
            System.out.println("=== Iniciando logout ===");
            System.out.println("Token recibido: " + token.substring(0, Math.min(20, token.length())) + "...");
            
            String email = jwtUtil.extractEmail(token);
            System.out.println("Email del token: " + email);
            
            java.util.Date expirationDate = jwtUtil.extractExpiration(token);
            System.out.println("Fecha de expiración: " + expirationDate);
            
            tokenBlacklistService.blacklistToken(token, expirationDate);
            
            System.out.println("=== Logout completado ===");
            return new LogoutResponse("Sesión cerrada correctamente", true, email);
        } catch (Exception e) {
            System.out.println("Error en logout: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al cerrar sesión: " + e.getMessage());
        }
    }
}
