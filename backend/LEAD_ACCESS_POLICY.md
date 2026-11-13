# Phạm vi đọc Lead (RBAC-07)

**Quyết định ngày 2026-11-13:** Counselor chỉ được xem Lead được giao cho chính mình. Dự án phục vụ một tổ chức, nên không có điều kiện lọc theo `organization_id`.

## Quy tắc cho API đọc Lead

1. Người gọi phải đăng nhập và có authority `LEADS.READ`.
2. `SYSTEM_ADMIN` và `MANAGER` được đọc toàn bộ Lead trong tổ chức duy nhất. Người dùng có role khác, kể cả `COUNSELOR` hoặc role tùy chỉnh, chỉ được đọc Lead có `leads.assigned_to = currentUser.id`. Mặc định giới hạn theo bản ghi được giao để role tùy chỉnh có `LEADS.READ` không tự mở rộng phạm vi.
3. Lead chưa được giao (`assigned_to IS NULL`) chỉ hiện với `SYSTEM_ADMIN` hoặc `MANAGER`. Khi đổi người phụ trách, quyền đọc của counselor cũ kết thúc ở request tiếp theo.
4. API danh sách phải đưa điều kiện phạm vi vào truy vấn DB **trước** phân trang, đếm tổng và ánh xạ DTO. Các điều kiện search/status/stage/source được kết hợp với phạm vi này bằng `AND`.
5. API chi tiết phải kiểm tra cùng điều kiện phạm vi. Nếu ID không tồn tại hoặc nằm ngoài phạm vi, trả cùng lỗi `LEAD_NOT_FOUND` (HTTP 404) để không tiết lộ bản ghi cho người không có quyền.
6. Nếu response Lead chứa thông tin Person/Contact, không được tạo endpoint khác trả các thông tin đó mà bỏ qua kiểm tra phạm vi. Phạm vi của API Person độc lập cần được thiết kế riêng trước khi mở API.

`@PreAuthorize("hasAuthority('LEADS.READ')")` là bước kiểm tra **chức năng**. Điều kiện `assigned_to` trong service/repository là bước kiểm tra **bản ghi**; hai bước đều cần có.

## Chưa chốt trong quyết định này

- Ai được tạo, sửa, gán, chuyển stage hoặc chuyển Lead thành Customer; authority của thao tác ghi không tự xác định phạm vi ghi.
- Phạm vi đọc Customer, Student Profile, Activity và Document. Cần quyết định cho từng module khi triển khai, không suy ra chỉ từ việc counselor được đọc Lead được giao.

## Ca kiểm thử khi làm module Lead

- Counselor A có `LEADS.READ`: danh sách chỉ gồm Lead gán A, tổng bản ghi và số trang chỉ tính các Lead đó.
- A gọi ID Lead gán B hoặc chưa giao: 404. A gọi ID Lead gán A: 200.
- Manager/Admin có `LEADS.READ`: thấy Lead gán A, gán B và chưa giao.
- User không có `LEADS.READ`: 403 dù Lead được gán cho họ.
- Sau khi Manager chuyển Lead từ A sang B, A nhận 404 và B nhận 200 ở request kế tiếp.
- Role tùy chỉnh có `LEADS.READ` nhưng không phải Manager/Admin vẫn chỉ thấy Lead được giao.
