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
    OLD_PASSWORD_INCORRECT(1013, "Mật khẩu cũ không chính xác", HttpStatus.BAD_REQUEST),
    AUTHENTICATED_USER_NOT_FOUND(1005, "Không xác định được người dùng đang đăng nhập", HttpStatus.UNAUTHORIZED),
    USER_PHONE_EXISTED(1007, "Số điện thoại đã tồn tại", HttpStatus.CONFLICT),
    ACCESS_DENIED(1006, "Bạn không có quyền", HttpStatus.FORBIDDEN),
    AUTHENTICATION_FAILED(1008, "Tên đăng nhập hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1009, "Refresh token không hợp lệ, đã hết hạn hoặc đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    INVALID_REQUEST(1010, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_ACCESS_TOKEN(1011, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    CANNOT_DEACTIVATE_SELF(1012, "Không thể tự vô hiệu hóa tài khoản đang đăng nhập", HttpStatus.CONFLICT),
    LAST_ACTIVE_ADMIN_REQUIRED(1014, "Phải duy trì ít nhất một quản trị viên hệ thống đang hoạt động", HttpStatus.CONFLICT),

    ROLE_NOT_FOUND(2001, "Không tìm thấy vai trò", HttpStatus.NOT_FOUND),
    ROLE_INACTIVE(2002, "Không thể gán vai trò đang ngừng hoạt động", HttpStatus.CONFLICT),
    ROLE_IN_USE(2003, "Không thể xóa vai trò đang được sử dụng hoặc có dữ liệu liên kết", HttpStatus.CONFLICT),
    ROLE_CODE_ALREADY_EXISTS(2004, "Mã vai trò đã tồn tại", HttpStatus.CONFLICT),
    SYSTEM_ROLE_PROTECTED(2005, "Không thể xóa hoặc ngừng hoạt động vai trò hệ thống", HttpStatus.CONFLICT),
    ROLE_CODE_IMMUTABLE(2006, "Không thể thay đổi mã vai trò đã tạo", HttpStatus.CONFLICT),
    SYSTEM_ADMIN_PERMISSIONS_PROTECTED(2007, "Không được thay đổi bộ quyền của vai trò quản trị hệ thống", HttpStatus.CONFLICT),

    PERMISSION_CODE_NOT_FOUND(3001, "Một hoặc nhiều mã quyền không tồn tại", HttpStatus.BAD_REQUEST),

    RESOURCE_NOT_FOUND(9000, "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(9001, "Phương thức HTTP không được hỗ trợ", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(9002, "Định dạng dữ liệu gửi lên không được hỗ trợ", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INVALID_PARAMETER(9003, "Tham số yêu cầu không đúng định dạng", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER(9004, "Thiếu tham số bắt buộc", HttpStatus.BAD_REQUEST),
    DATA_CONFLICT(9005, "Dữ liệu bị trùng hoặc vi phạm ràng buộc liên kết", HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION(9006, "Dữ liệu đang được thay đổi bởi yêu cầu khác, vui lòng thử lại", HttpStatus.CONFLICT),
    FILE_TOO_LARGE(9007, "Tệp tải lên vượt quá dung lượng cho phép", HttpStatus.PAYLOAD_TOO_LARGE),
    DATABASE_UNAVAILABLE(9008, "Tạm thời không thể truy cập dữ liệu, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),
    RESPONSE_TYPE_NOT_ACCEPTABLE(9009, "Không thể trả về định dạng dữ liệu được yêu cầu", HttpStatus.NOT_ACCEPTABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
