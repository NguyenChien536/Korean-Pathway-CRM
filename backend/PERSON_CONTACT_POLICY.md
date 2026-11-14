# Thông tin liên hệ tối thiểu khi tạo Lead (PER-01)

**Quyết định ngày 2026-11-13:** Luồng tạo Lead mới phải nhận họ tên Person và **số điện thoại**. Email và Zalo là tùy chọn. Vì phone đã bắt buộc, điều kiện “có ít nhất một trong phone/Zalo/email” được đáp ứng tự nhiên; không cần thêm một validation riêng cho điều kiện đó.

## Phạm vi áp dụng

- Áp dụng cho use case tạo Person cùng Lead qua API `POST /leads` sẽ xây dựng sau này. API tạo Person độc lập, import dữ liệu hoặc cập nhật/xóa contact cần quy tắc riêng trước khi mở.
- `persons.full_name` lưu tên người; `person_contacts` lưu mỗi cách liên hệ trong một dòng (`contact_type`, `value`, `normalized_value`). Số điện thoại bắt buộc được tạo thành một contact `PHONE` cùng transaction tạo Person và Lead.
- Service cần từ chối request thiếu phone hoặc phone không hợp lệ trước khi lưu. Nếu một bước tạo Person, Contact hoặc Lead thất bại, transaction phải rollback toàn bộ.
- Schema hiện chỉ `UNIQUE (person_id, contact_type, normalized_value)` và unique contact chính theo từng loại trong một Person. Nó **không** buộc mọi Person có dòng `PHONE` và **không** buộc phone duy nhất giữa hai Person. Vì vậy validation phone bắt buộc thuộc use case tạo Lead; không được mô tả là DB đã bảo đảm điều này.
- Demo seed hiện tạo Person và Lead mà không có dòng `person_contacts`; cần bổ sung contact giả khi triển khai luồng Lead để dữ liệu demo phản ánh quy tắc mới.

## Xử lý khi số điện thoại đã có trong CRM

**Quyết định ngày 2026-11-13:** Cho phép nhiều Person dùng chung một số điện thoại. Trước khi tạo Lead, tìm contact `PHONE` theo `normalized_value`; so sánh thêm tên đã chuẩn hóa (bỏ khoảng trắng thừa, không phân biệt hoa/thường) để chọn luồng. Không bỏ dấu tiếng Việt hoặc tự coi hai cách viết gần giống là cùng tên khi chưa chốt quy tắc đối chiếu.

**Quyết định ngày 2026-11-14 về phone:** `PhoneNormalizer` lưu số Việt Nam có đầu `0` (10 hoặc 11 chữ số) thành `+84` và phần sau số `0`; số `+84` tương ứng giữ nguyên. Ví dụ `0912345678` và `+84912345678` cùng có `normalized_value = +84912345678`. Số quốc tế khác phải bắt đầu bằng `+`, gồm 9–15 chữ số theo dạng E.164 đơn giản và được giữ nguyên. Không nhận `84...` thiếu dấu `+`, dấu cách ở giữa, dấu gạch nối hoặc `+840...`. `PersonContactService` gọi cùng normalizer khi tìm và khi tạo PHONE; `value` giữ cách nhập ban đầu sau khi bỏ khoảng trắng đầu/cuối. Đây là quy tắc cho dữ liệu mới; nếu đã có contact lưu theo cách khác thì cần migration làm sạch trước khi trông chờ tra cứu khớp.

### Lead đến từ webhook

- Nếu **đúng một Person** có cả tên và phone khớp, hệ thống tự dùng lại Person và xử lý Lead cũ theo trạng thái bên dưới; không cần nhân viên xác nhận trong đường xử lý webhook. Đây là quy tắc tự động theo quyết định sản phẩm, vẫn có rủi ro hiếm gặp hai người trùng tên và cùng số.
- Nếu phone khớp nhưng tên khác, tạo Person/Lead mới theo dữ liệu nhận được và ghi cảnh báo *có thể dùng chung số liên hệ* để nhân viên rà soát. Không tự xác nhận quan hệ gia đình.
- Nếu có nhiều Person cùng khớp cả tên lẫn phone, không chọn ngẫu nhiên một Person; đưa sự kiện vào luồng rà soát vì kết quả không còn duy nhất.
- Cùng một sự kiện webhook có thể được gửi lại. Trước khi tự xử lý cần có khóa idempotency theo nhà cung cấp + mã sự kiện để một lần gửi lại không tạo thêm Lead, lịch sử hoặc cảnh báo. Schema hiện chưa có nơi lưu khóa này; thiết kế migration khi bắt đầu tích hợp webhook.

### Nhân viên tạo Lead trên giao diện web

- Nếu tên và phone cùng khớp Person cũ, hiển thị modal cảnh báo. Nhân viên hỏi khách và chọn **dùng Person cũ** hoặc **tạo Person mới**. Backend phải nhận lựa chọn rõ ràng và kiểm tra lại trên server; đóng modal không được âm thầm tạo hoặc gộp.
- Nếu phone khớp nhưng tên khác, cho phép tạo Person mới và hiển thị cảnh báo số dùng chung. Nhân viên có thể kiểm tra thêm nếu nghi nhập sai tên.
- Counselor chỉ xem Lead được giao: modal/cảnh báo không được lộ tên, ID hoặc Lead của người khác ngoài phạm vi. Nếu ứng viên cũ nằm ngoài phạm vi, cần Manager xử lý việc dùng lại/gán Lead thay vì cho Counselor vượt quyền qua modal.

### Khi đã chọn đúng Person cũ

- Kiểm tra Person đã là Customer hay chưa trước khi quyết định mở Lead. Một Person có thể có nhiều Lead đã đóng; unique index hiện chỉ giới hạn số Lead `OPEN`, không chọn hộ Lead cũ nào cần mở lại.
- Lead `OPEN` đã tồn tại: dùng lại Lead đó; ghi nhận lần liên hệ mới theo Activity/Audit khi triển khai.
- Lead cũ `LOST` hoặc `ARCHIVED`: mở lại chính Lead đó, ghi `lead_status_history` từ trạng thái cũ sang `OPEN` và `stage_history` nếu chuyển từ stage kết thúc về stage đang mở. Giữ lịch sử và `created_at` ban đầu. Quy tắc chọn stage mở lại cần chốt trước khi code.
- Lead `CONVERTED`/Person đã là Customer: giữ Lead chuyển đổi và `lead_conversions`; chuyển yêu cầu mới sang luồng Customer hoặc nhu cầu mới, không đổi ngược Lead đã chuyển đổi thành `OPEN`.

Schema hiện có index tìm theo `(contact_type, normalized_value)` và chỉ unique trong từng Person; nó chưa đảm bảo chống tạo trùng Person trong hai request đồng thời. Khi triển khai cần thiết kế khóa/điều phối cho bước tìm và tạo, cộng với unique partial index một Lead `OPEN` trên một Person. Một lần webhook mới từ nguồn khác không nên âm thầm ghi đè nguồn gốc ban đầu của Lead; ghi nhận lần tương tác mới trong lịch sử.

## Quyết định còn mở trước khi code

- Cách xử lý tên viết khác/đổi tên và trường hợp nhiều Person cùng khớp; webhook tự động chỉ áp dụng cho kết quả khớp duy nhất.
- Khi mở lại Lead `LOST`/`ARCHIVED`, chọn stage đang mở nào và ai được phép thao tác.
- Nếu Person có nhiều Lead `LOST`/`ARCHIVED`, chọn Lead nào để mở lại; nếu Person đã là Customer thì ưu tiên luồng Customer.
- Khi mở lại từ `LOST`, chỉ xóa `leads.lost_reason` sau khi lý do cũ đã được ghi trong lịch sử. `lead_status_history` không tự sao chép trường này.
- Cách giữ nguồn/chiến dịch của từng lần khách gửi form: chỉ ghi đè `leads.campaign_name` sẽ làm mất giá trị cũ; bảng `activities` hiện không có cột campaign/source riêng.
- Thông báo WebSocket/Kanban là tính năng sau này. Khi triển khai, chỉ phát thông báo sau khi transaction lưu Lead thành công; cần quy tắc ưu tiên riêng thay vì mặc định mọi lần đổi `updated_at` đều đẩy Lead lên đầu.
- Hình dạng DTO cho lựa chọn của modal và luồng Manager xử lý ứng viên nằm ngoài phạm vi Counselor.
- Idempotency key và lưu sự kiện webhook; V1 chưa có bảng tương ứng.
- Nếu cần lưu quan hệ gia đình **đã xác nhận**, phải thiết kế quan hệ Person–Person riêng; cảnh báo trùng số hiện chỉ là gợi ý, không phải quan hệ được lưu.
- Khi cập nhật Person đã gắn Lead, có cho xóa contact `PHONE` cuối cùng hay yêu cầu số thay thế.
