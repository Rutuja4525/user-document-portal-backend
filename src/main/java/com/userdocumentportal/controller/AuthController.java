package com.userdocumentportal.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.userdocumentportal.dto.*;
import com.userdocumentportal.entity.AuthProvider;
import com.userdocumentportal.entity.Role;
import com.userdocumentportal.entity.User;
import com.userdocumentportal.repository.UserRepository;
import com.userdocumentportal.security.jwt.JwtUtils;
import com.userdocumentportal.security.services.UserDetailsImpl;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Value("${propertymanagement.googleClientId:}")
    private String googleClientId;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                role));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        Role userRole = Role.TENANT;
        if (signUpRequest.getRole() != null) {
            try {
                userRole = Role.valueOf(signUpRequest.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Invalid Role!"));
            }
        }

        User user = User.builder()
                .firstName(signUpRequest.getFirstName())
                .lastName(signUpRequest.getLastName())
                .email(signUpRequest.getEmail())
                .phone(signUpRequest.getPhone())
                .password(encoder.encode(signUpRequest.getPassword()))
                .role(userRole)
                .authProvider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest googleRequest) {
        try {
            // Check if googleClientId is configured; if empty, we provide a descriptive message or log
            if (googleClientId == null || googleClientId.trim().isEmpty() || googleClientId.equals("YOUR_GOOGLE_CLIENT_ID")) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Google Client ID is not configured on the backend application.properties. Please verify propertymanagement.googleClientId configuration."));
            }

            NetHttpTransport transport = new NetHttpTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleRequest.getCredential());
            if (idToken == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid Google Token!"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            if (firstName == null) firstName = "Google";
            if (lastName == null) lastName = "User";

            Optional<User> userOptional = userRepository.findByEmail(email);
            User user;

            if (userOptional.isPresent()) {
                user = userOptional.get();
                // Update Google ID and provider if not set
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                    user.setAuthProvider(AuthProvider.GOOGLE);
                    userRepository.save(user);
                }
            } else {
                // Register new user
                Role userRole = Role.TENANT;
                if (googleRequest.getRole() != null) {
                    try {
                        userRole = Role.valueOf(googleRequest.getRole().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // fallback to TENANT
                    }
                }

                user = User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .authProvider(AuthProvider.GOOGLE)
                        .googleId(googleId)
                        .role(userRole)
                        .build();

                userRepository.save(user);
            }

            // Generate application JWT token
            String jwt = jwtUtils.generateTokenFromEmail(email);

            return ResponseEntity.ok(new JwtResponse(jwt,
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Error verifying Google ID token: " + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(new MessageResponse("Error: Unauthorized!"));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User not found!"));

        // Check if the current password is correct (for LOCAL accounts)
        if (user.getAuthProvider() == AuthProvider.LOCAL && user.getPassword() != null) {
            if (!encoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Current password is incorrect!"));
            }
        }

        // Update password
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Password updated successfully!"));
    }
}
