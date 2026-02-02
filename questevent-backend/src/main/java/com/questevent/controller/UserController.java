package com.questevent.controller;

import com.questevent.dto.CompleteProfileRequest;
import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @PostMapping
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user (Platform Owner only)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Platform Owner only"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody User user) {

        log.info("Creating new user");

        User savedUser = userService.addUser(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.convertToDto(savedUser));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(
            summary = "Get all users",
            description = "Fetch all users (Platform Owner only)"
    )
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user by ID",
            description = "Fetch a user by user ID (Platform Owner only)"
    )
    public ResponseEntity<UserResponseDto> getUserById(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userService.getUserById(userId)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Fetch details of the currently logged-in user"
    )
    public ResponseEntity<UserResponseDto> getCurrentUser() {

        return ResponseEntity.ok(
                userService.getCurrentUser()
        );
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")
    @Operation(
            summary = "Update current user",
            description = "Updates logged-in user's profile"
    )
    public ResponseEntity<UserResponseDto> updateCurrentUser(
            @Valid @RequestBody User user) {

        log.info("Updating current logged-in user");

        User updatedUser =
                userService.updateCurrentUser(user);

        return ResponseEntity.ok(
                userService.convertToDto(updatedUser)
        );
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/complete-profile")
    @Operation(
            summary = "Complete user profile",
            description = "Completes logged-in user's profile"
    )
    public ResponseEntity<UserResponseDto> completeProfile(
            @Valid @RequestBody CompleteProfileRequest request) {

        log.info("Completing profile for current logged-in user");

        User updatedUser =
                userService.completeProfile(request);

        return ResponseEntity.ok(
                userService.convertToDto(updatedUser)
        );
    }

    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Delete user by ID",
            description = "Deletes a user by user ID (Platform Owner only)"
    )
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId) {

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}
