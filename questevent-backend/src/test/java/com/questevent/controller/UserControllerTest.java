package com.questevent.controller;

import com.questevent.dto.UserResponseDto;
import com.questevent.entity.User;
import com.questevent.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User user;
    private UserResponseDto dto;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId(1L);
        user.setName("Test");
        user.setEmail("User@test.com");
        user.setGender("Male");

        dto = new UserResponseDto();
        dto.setUserId(1L);
        dto.setName("Test");
        dto.setEmail("User@test.com");
        dto.setGender("Male");
    }

    @Test
    void createUser_success() {

        Mockito.when(userService.addUser(user)).thenReturn(user);
        Mockito.when(userService.convertToDto(user)).thenReturn(dto);

        ResponseEntity<UserResponseDto> response =
                userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Test", response.getBody().getName());
    }

    @Test
    void getAllUsers_success() {

        Mockito.when(userService.getAllUsers()).thenReturn(List.of(user));
        Mockito.when(userService.convertToDto(user)).thenReturn(dto);

        ResponseEntity<List<UserResponseDto>> response =
                userController.getAllUsers();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getUserById_success() {

        Mockito.when(userService.getUserById(1L)).thenReturn(user);
        Mockito.when(userService.convertToDto(user)).thenReturn(dto);

        ResponseEntity<UserResponseDto> response =
                userController.getUser(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("User@test.com", response.getBody().getEmail());
    }

    @Test
    void updateUser_success() {

        Mockito.when(userService.updateUser(1L, user)).thenReturn(user);
        Mockito.when(userService.convertToDto(user)).thenReturn(dto);

        ResponseEntity<UserResponseDto> response =
                userController.updateUser(1L, user);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Test", response.getBody().getName());
    }

    @Test
    void deleteUser_success() {

        Mockito.doNothing().when(userService).deleteUser(1L);

        ResponseEntity<Void> response =
                userController.deleteUser(1L);

        assertEquals(204, response.getStatusCode().value());
    }
}
