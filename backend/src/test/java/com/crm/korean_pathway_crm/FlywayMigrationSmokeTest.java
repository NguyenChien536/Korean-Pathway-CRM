package com.crm.korean_pathway_crm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
//Kiểm thử migration tự động
//GitHub tự kiểm tra database mỗi khi push code hoặc mở Pull Request.
//B1: viết phần khởi động Spring với postgreSQL
//B2: bật Flyway và đặt Hibernate ở chế độ validate
//B3: Chạy bài test trên một PostgreSQL dùng riêng cho test. Không dùng database dev hay database có dữ liệu cần giữ. Cung cấp ba biến môi trường MIGRATION_TEST_DB_URL, MIGRATION_TEST_DB_USERNAME, MIGRATION_TEST_DB_PASSWORD.
// Khởi động toàn bộ Spring context để kiểm tra việc kết nối database và ánh xạ JPA.
// Thông tin kết nối được truyền từ môi trường chạy test.
@SpringBootTest(properties = {
        "spring.datasource.url=${MIGRATION_TEST_DB_URL}",
        "spring.datasource.username=${MIGRATION_TEST_DB_USERNAME}",
        "spring.datasource.password=${MIGRATION_TEST_DB_PASSWORD}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
// Dùng các cấu hình kiểm thử chung (ví dụ JWT), sau đó ghi đè datasource H2
// trong application-test.yml bằng PostgreSQL cho riêng bài smoke test này.
@ActiveProfiles("test")
// Maven test thông thường không cần PostgreSQL; CI đặt biến này để chạy smoke test.
@EnabledIfEnvironmentVariable(named = "MIGRATION_TEST_DB_URL", matches = ".+")
class FlywayMigrationSmokeTest {

    @Test
    void applicationStartsWithFlywayMigrations() {
        // Khi dựng Spring context, Flyway chạy các migration trước.
        // Sau đó Hibernate dùng ddl-auto=validate để kiểm tra schema trong PostgreSQL
        // có khớp với các Entity hay không. Nếu một trong hai bước lỗi, test sẽ thất bại.
    }
}
