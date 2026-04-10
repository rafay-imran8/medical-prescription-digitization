package com.prescription.controller;

import com.prescription.dto.ApiResponse;
import com.prescription.dto.UserDTO;
import com.prescription.dto.CreateAdminUserRequest;
import com.prescription.service.UserService;
import com.prescription.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.prescription.dto.UserLogResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        try {
            List<UserDTO> users = userService.getAllUsersDTO();
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch users: " + e.getMessage()));
        }
    }


    @GetMapping("/users/logs")
    public ResponseEntity<ApiResponse<List<UserLogResponse>>> getUserLogs() {
        try {
            List<UserLogResponse> logs = userService.getUserLogs();  // ← was adminService
            return ResponseEntity.ok(ApiResponse.success(logs));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean isActive = request.get("isActive");
            UserDTO user = userService.updateUserStatus(userId, isActive);
            return ResponseEntity.ok(ApiResponse.success("User status updated", user));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    /**
     * Create admin or analyst user (only accessible by admin)
     */
    @PostMapping("/users/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> createAdminUser(
            @RequestBody CreateAdminUserRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UserDTO user = userService.createAdminOrAnalyst(
                    request,
                    userDetails.getUserId()
            );
            return ResponseEntity.ok(ApiResponse.success(
                    "User created successfully",
                    user
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
//
//    @PutMapping("/users/{userId}/verify")
//    public ResponseEntity<ApiResponse<UserDTO>> verifyUser(@PathVariable Long userId) {
//        try {
//            UserDTO user = userService.verifyUser(userId);
//            return ResponseEntity.ok(ApiResponse.success("User verified successfully", user));
//        } catch (Exception e) {
//            return ResponseEntity
//                    .status(HttpStatus.BAD_REQUEST)
//                    .body(ApiResponse.error(e.getMessage()));
//        }
//    }
}