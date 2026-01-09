package com.questevent.controller;

import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @PostMapping
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user (Platform Owner only)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Platform Owner only")
    })
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody User user) {

        User savedUser = userService.addUser(user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.convertToDto(savedUser));
    }


    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a list of all users (Platform Owner only)")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users")
    @ApiResponse(responseCode = "403", description = "Forbidden - Platform Owner only")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {

        List<UserResponseDto> users = userService.getAllUsers()
                .stream()
                .map(userService::convertToDto)
                .toList();

        return ResponseEntity.ok(users);
    }


    @PreAuthorize("@rbac.canAccessUserProfile(authentication, #id)")
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by ID (Owner or self)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<UserResponseDto> getUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id) {

        User user = userService.getUserById(id);
        return ResponseEntity.ok(userService.convertToDto(user));
    }


    @PreAuthorize("@rbac.canAccessUserProfile(authentication, #id)")
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates user information (Owner or self)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User user) {

        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(userService.convertToDto(updatedUser));
    }

    @PreAuthorize("@rbac.isPlatformOwner(authentication)")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user (Platform Owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Platform Owner only")
    })
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id) {

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
