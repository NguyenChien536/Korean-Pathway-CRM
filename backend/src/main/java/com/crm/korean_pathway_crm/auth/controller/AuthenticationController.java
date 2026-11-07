package com.crm.korean_pathway_crm.auth.controller;

import com.crm.korean_pathway_crm.auth.dto.AuthenticationRequest;
import com.crm.korean_pathway_crm.auth.dto.AuthenticationResponse;
import com.crm.korean_pathway_crm.auth.dto.IntrospectRequest;
import com.crm.korean_pathway_crm.auth.dto.IntrospectResponse;
import com.crm.korean_pathway_crm.auth.service.AuthenticationService;
import com.crm.korean_pathway_crm.user.dto.response.ApiResponse;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;
    public record RefreshRequest(
            @NotBlank(message = "Refresh token là bắt buộc")
            @Size(max = 255, message = "Refresh token tối đa 255 ký tự") String refreshToken) {}
    public record LogoutRequest(String refreshToken) {}

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        return tokenResponse(authenticationService.authenticate(request));
    }
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect( @RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return tokenResponse(authenticationService.refreshToken(request.refreshToken()));
    }
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request) {

        String refreshToken = request != null ? request.refreshToken() : null;
        authenticationService.logout(authHeader, refreshToken);

        return ApiResponse.<Void>builder()
                .message("Logged out successfully")
                .build();
    }

    private ResponseEntity<ApiResponse<AuthenticationResponse>> tokenResponse(AuthenticationResponse result) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.<AuthenticationResponse>builder().result(result).build());
    }
}
