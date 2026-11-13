# Quy ước dữ liệu, lỗi và log của Korean Pathway CRM

Tài liệu này mô tả **phần API đã được triển khai**. Các bảng Lead, Customer, hồ sơ, thanh toán... đã có trong schema nhưng chưa đồng nghĩa với việc API của chúng đã hoàn tất. Khi làm module mới, áp dụng cùng nguyên tắc rồi ghi rõ ngoại lệ theo nghiệp vụ.

## 1. Kiểu dữ liệu và giá trị rỗng

- `null` trong request cập nhật User nghĩa là **không đổi** trường đó. Vì vậy `PUT /api/v1/users/{userId}` hiện hoạt động như cập nhật từng phần, dù tên HTTP method là `PUT`. `PATCH /api/v1/users/me` cũng cập nhật từng phần. Không tự suy diễn quy tắc này cho mọi endpoint mới.
- `username`, `email`, `fullName` của User không được rỗng hoặc chỉ gồm khoảng trắng. Sau khi `trim`, tên đăng nhập và họ tên phải còn ít nhất 2 ký tự; email phải còn ít nhất 1 ký tự và đúng định dạng. Giới hạn độ dài cũng được kiểm tra tại DTO.
- `phone` của User là tùy chọn: không gửi/`null` lúc tạo thì lưu `NULL`; gửi `""` lúc cập nhật thì xóa số đã lưu (`NULL`); không gửi trường này lúc cập nhật thì giữ số cũ. Số có giá trị phải khớp định dạng được khai báo ở DTO. Không lưu chuỗi rỗng như một số điện thoại.
- `password` và `confirmPassword` chỉ dùng để xác nhận/đổi mật khẩu, không trả lại trong DTO. Mật khẩu được BCrypt hash. API cập nhật của quản trị viên chỉ đổi mật khẩu khi gửi mật khẩu mới hợp lệ; API đổi mật khẩu cá nhân có luồng riêng.
- `roleId` dùng UUID; role phải tồn tại, còn hoạt động và người thao tác phải có quyền gán. ID người thao tác, `createdBy`, `updatedBy` và các mốc audit do server lấy từ người đăng nhập, không lấy từ request.

Ví dụ cập nhật `{"phone":""}` là xóa số; `{"email":"new@example.com"}` chỉ đổi email. Nếu gửi `{"username":" a "}`, dữ liệu sẽ bị từ chối sau khi chuẩn hóa vì chỉ còn một ký tự.

## 2. Trùng dữ liệu và giao dịch

- `users.username` và `users.email` phải duy nhất **không phân biệt chữ hoa/thường**; PostgreSQL có unique index trên `lower(...)`.
- `users.phone` phải duy nhất khi có giá trị; các bản ghi không có số dùng `NULL`. Service kiểm tra trước để trả mã lỗi cụ thể (`USERNAME_ALREADY_EXISTS`, `EMAIL_ALREADY_EXISTS`, `USER_PHONE_EXISTED`). Unique index của DB vẫn là ranh giới cuối khi hai request chạy đồng thời. Xung đột DB chung trả `DATA_CONFLICT` nếu không xác định được trường cụ thể.
- Thao tác ghi nhiều bước nằm trong transaction; lỗi trước khi commit khiến thay đổi dữ liệu của transaction bị rollback. Kiểm tra trước ở service không thay thế constraint DB.
- Đây là **chuẩn hóa giá trị đầu vào và hợp đồng API**. Việc chuẩn hóa quan hệ trong CSDL (ví dụ 1NF/2NF/3NF) là vấn đề khác, xem mục 6.

## 3. Tìm kiếm và phân trang

- Danh sách User nhận query parameter qua `@Valid @ModelAttribute UserFilterRequest`.
- `page` bắt đầu từ 0, mặc định 0; `size` mặc định 20 và phải trong khoảng 1–100. `search` tối đa 100 ký tự. Sai giới hạn hoặc sai kiểu trả HTTP 400.
- `search` được `trim`, tìm không phân biệt chữ hoa/thường trên username, họ tên, email, số điện thoại. Ký tự `%`, `_`, `\` được escape để là ký tự thường trong `LIKE`.
- `status` và `roleId` là bộ lọc tùy chọn, kết hợp bằng `AND` với điều kiện tìm kiếm. `status` là tên public của thuộc tính `isActive` trong entity.
- User mặc định sắp xếp `username ASC`; chỉ cho sort theo `username`, `fullName`, `email`, `createdAt`, `status`. `sortDir` là `ASC` hoặc `DESC`; luôn thêm `id ASC` để thứ tự ổn định khi trùng giá trị sort chính. Sort không hợp lệ trả HTTP 400, không truyền nguyên tên cột tùy ý từ client vào truy vấn.
- Repository trả `Page<User>`; service map thành `Page<UserResponse>`; controller bọc bằng `PageResponse` với `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

## 4. Thời gian

- Các trường nghiệp vụ/audit hiện dùng `LocalDateTime` và cột PostgreSQL `timestamp without time zone`. Quy ước ứng dụng là **giá trị UTC**: tạo bằng `LocalDateTime.now(ZoneOffset.UTC)` và cấu hình kết nối/JPA UTC. `LocalDateTime` tự nó không mang múi giờ; client phải hiểu đây là UTC theo hợp đồng API.
- Thời điểm phát hành/hết hạn JWT là thời điểm tuyệt đối (`Instant`); không lấy trực tiếp `Date` hay `LocalDateTime` làm thời hạn token mà không xác định múi giờ.
- Với module mới, phân biệt thời điểm (timestamp UTC) với ngày nghiệp vụ (ví dụ hạn thanh toán chỉ theo ngày). Quyết định kiểu dữ liệu dựa vào ý nghĩa của field.

## 5. HTTP, mã lỗi và log

- API thành công dùng `ApiResponse<T>` với `code = 1000` và `result` nếu có. Danh sách dùng `PageResponse` trong `result`.
- Lỗi trả HTTP status phù hợp và `ApiResponse` gồm `code`, `message`. Ví dụ validation là 400, chưa đăng nhập/token sai là 401, không đủ quyền là 403, không tìm thấy là 404, trùng dữ liệu là 409. Mã nghiệp vụ nằm ở `ErrorCode`; không trả stack trace, SQL hay thông báo nội bộ của exception cho client.
- Không log mật khẩu, mã OTP, access/refresh token, Authorization header, body xác thực, số giấy tờ, số điện thoại hoặc email. Chỉ log sự kiện cần thiết; ID bản ghi dạng UUID có thể dùng để tra cứu nội bộ. Cần cân nhắc thêm trước khi bật SQL bind logging, HTTP body logging hoặc log của thư viện xác thực.
- `RequestIdFilter` tạo UUID mới cho mỗi request, trả trong header `X-Request-Id` và đặt vào MDC; pattern log hiển thị `requestId`. Server tự sinh ID, không dùng giá trị client gửi, để tránh chèn dữ liệu vào log. MDC được xóa trong `finally` vì servlet thread được tái sử dụng.
- Khi gặp HTTP 500, có thể gửi **request ID** cho người vận hành để tìm log tương ứng. Exception không biết trước hiện chỉ ghi tên lớp lỗi để tránh lộ dữ liệu; cần bổ sung log an toàn theo từng tình huống khi điều tra, không bật ghi toàn bộ request hoặc token.

## 6. Chuẩn hóa quan hệ trong CSDL

- **1NF**: mỗi ô chứa một giá trị có ý nghĩa riêng; dữ liệu lặp như số liên hệ của một `person` được lưu trong `person_contacts`, không tạo `phone_1`, `phone_2`, `phone_3`.
- **2NF**: thuộc tính của bảng quan hệ nhiều-nhiều phải phụ thuộc vào cả khóa của bản ghi quan hệ. `role_permissions` dùng cặp `(role_id, permission_id)` làm khóa chính, không lưu danh sách mã quyền trong một ô của `roles`.
- **3NF**: dữ liệu mô tả một đối tượng nằm ở bảng của đối tượng đó. `users` tham chiếu `roles` bằng `role_id`; tên/mã role nằm ở `roles`. `applications` tham chiếu hồ sơ và trường học thay vì chép toàn bộ thông tin trường vào từng đơn.
- Những quy tắc này giảm trùng lặp và tránh cập nhật lệch giữa nhiều bản ghi. Không có nghĩa phải tách mọi field ra bảng riêng: tách theo thực thể, quan hệ và vòng đời nghiệp vụ; kiểm chứng bằng constraint, query thực tế và migration.
