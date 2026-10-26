package com.crm.korean_pathway_crm.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    USERNAME_ALREADY_EXISTS(1001, "Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(1002, "Email đã tồn tại", HttpStatus.CONFLICT),
    USER_NOT_FOUND(1003, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    PASSWORD_CONFIRMATION_MISMATCH(1004, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    AUTHENTICATED_USER_NOT_FOUND(1005, "Không xác định được người dùng đang đăng nhập", HttpStatus.UNAUTHORIZED),
    USER_PHONE_EXISTED(1007, "Số điện thoại đã tồn tại", HttpStatus.CONFLICT),
    ACCESS_DENIED(1006, "Bạn không có quyền", HttpStatus.FORBIDDEN),
    AUTHENTICATION_FAILED(1008, "Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1009, "Refresh token không hợp lệ, đã hết hạn hoặc đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    INVALID_REQUEST(1010, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_ACCESS_TOKEN(1011, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),

    ROLE_NOT_FOUND(2001, "Không tìm thấy vai trò", HttpStatus.NOT_FOUND),
    ROLE_INACTIVE(2002, "Không thể gán vai trò đang ngừng hoạt động", HttpStatus.CONFLICT),
    ROLE_IN_USE(2003, "Không thể xóa vai trò đang được gán cho người dùng", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
