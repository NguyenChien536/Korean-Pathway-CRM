package com.crm.korean_pathway_crm.auth.config;


import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.enums.SystemRoleCode;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationInitConfig {
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    @Bean
    @Profile("dev")
    ApplicationRunner applicationRunner (UserRepository userRepository){
        return args ->{
            boolean adminExists = userRepository.findByUsernameIgnoreCase("admin").isPresent()
                    || userRepository.existsByEmailIgnoreCase("admin@crmkorean.vn");

            if (!adminExists){
                Role role = roleRepository.findByCodeIgnoreCase(SystemRoleCode.SYSTEM_ADMIN.name())
                        .orElseThrow(() -> new IllegalStateException("Thiếu vai trò SYSTEM_ADMIN trong cơ sở dữ liệu"));
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                User user = User.builder()
                        .username("admin")
                        .email("admin@crmkorean.vn")
                        .fullName("CRM System Administrator")
                        .passwordHash(passwordEncoder.encode("Admin123!"))
                        .role(role)
                        .passwordChangedAt(now)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                userRepository.save(user);
                log.warn("Đã tạo tài khoản demo System Admin (admin). Chỉ dùng local; hãy đổi mật khẩu trước khi dùng dữ liệu thật.");
            } else {
                log.info("Tài khoản admin khởi tạo đã tồn tại, bỏ qua bước tạo mới");
            }
        };

    }
}

