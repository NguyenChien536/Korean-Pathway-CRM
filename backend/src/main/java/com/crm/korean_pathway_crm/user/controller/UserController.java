package com.crm.korean_pathway_crm.user.controller;

import com.crm.korean_pathway_crm.user.dto.ApiResponse;
import com.crm.korean_pathway_crm.user.dto.ChangePasswordRequest;
import com.crm.korean_pathway_crm.user.dto.MyInfoUpdateRequest;
import com.crm.korean_pathway_crm.user.dto.UserCreationRequest;
import com.crm.korean_pathway_crm.user.dto.UserResponse;
import com.crm.korean_pathway_crm.user.dto.UserUpdateRequest;
import com.crm.korean_pathway_crm.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreationRequest request) {
        log.info("REST request to create user: {}", request.getUsername());
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        log.info("REST request to retrieve all users with pagination and filters");
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID userId) {
        log.info("REST request to get user by ID: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(userId))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyInfo(@Valid @RequestBody MyInfoUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
         log.info("REST request to update user with ID: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("REST request to change password");
        userService.changePassword(request);
        return ApiResponse.<Void>builder()
                .message("Đổi mật khẩu thành công. Vui lòng đăng nhập lại")
                .build();
    }
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
        log.info("REST request to delete user ID: {}", userId);
        userService.deleteUser(userId);
        return ApiResponse.<Void>builder()
                .message("Account has been deactivated")
                .build();
    }
}
