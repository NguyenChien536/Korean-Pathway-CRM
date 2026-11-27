package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.dto.request.LeadStatusUpdateRequest;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.lead.service.LeadStatusService;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.repository.PersonRepository;
import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.role.repository.RoleRepository;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:lead_status;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class LeadStatusIntegrationTest {
    @Autowired LeadStatusService service;
    @Autowired LeadRepository leads;
    @Autowired PersonRepository persons;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired JdbcTemplate jdbc;

    private UUID pipelineId;
    private UUID initialStageId;
    private UUID targetStageId;
    private User counselor;
    private Lead lead;

    @BeforeEach
    void setUp() {
        Role role = roles.findByCodeIgnoreCase("COUNSELOR").orElseGet(() -> roles.saveAndFlush(
                Role.builder().code("COUNSELOR").name("Counselor").build()));
        String username = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        counselor = users.saveAndFlush(User.builder().username(username).email(username + "@test.local")
                .fullName("Counselor").passwordHash("test-only").role(role)
                .createdAt(now).updatedAt(now).build());
        pipelineId = pipeline("LEAD", "ACTIVE");
        initialStageId = stage(pipelineId, "OPEN", "ACTIVE", false, 10);
        targetStageId = stage(pipelineId, "LOST", "ACTIVE", true, 20);
        jdbc.update("update pipeline_stages set code = 'LOST' where id = ?", targetStageId);
        Person person = persons.saveAndFlush(Person.builder().fullName("Nguyễn Văn An").build());
        lead = leads.saveAndFlush(Lead.builder().personId(person.getId()).pipelineId(pipelineId)
                .stageId(initialStageId).assignedTo(counselor.getId()).build());
        login("LEADS.READ", "LEADS.UPDATE");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void recordsLostReasonAndBothHistoriesUsingTheOldStatus() {
        var response = service.markLost(lead.getId(), request("  Chưa đủ tài chính  "));
        assertThat(response.getStatus()).isEqualTo(LeadStatus.LOST);
        assertThat(response.getStageId()).isEqualTo(targetStageId);
        assertThat(response.getLostReason()).isEqualTo("Chưa đủ tài chính");
        var history = jdbc.queryForMap("select * from lead_status_history where lead_id = ?", lead.getId());
        assertThat(history.get("from_status")).isEqualTo("OPEN");
        assertThat(history.get("to_status")).isEqualTo("LOST");
        assertThat(history.get("changed_by")).isEqualTo(counselor.getId());
        assertThat(history.get("changed_at")).isNotNull();
        assertThat(history.get("reason")).isEqualTo("Chưa đủ tài chính");
        var stageHistory = jdbc.queryForMap("select * from stage_history where lead_id = ?", lead.getId());
        assertThat(stageHistory.get("from_stage_id")).isEqualTo(initialStageId);
        assertThat(stageHistory.get("to_stage_id")).isEqualTo(targetStageId);
        assertThat(stageHistory.get("changed_by")).isEqualTo(counselor.getId());
        assertThat(jdbc.queryForObject("select updated_by from leads where id = ?", UUID.class, lead.getId()))
                .isEqualTo(counselor.getId());

        assertError(() -> service.markLost(lead.getId(), request("Lặp lại")), ErrorCode.LEAD_NOT_MODIFIABLE);
        assertThat(historyCount()).isEqualTo(1);
        assertThat(statusHistoryCount()).isEqualTo(1);
    }

    @Test
    void counselorCannotCloseAnotherCounselorsOrUnassignedLead() {
        lead.setAssignedTo(UUID.randomUUID());
        leads.saveAndFlush(lead);
        assertError(() -> service.markLost(lead.getId(), request("Không còn nhu cầu")), ErrorCode.LEAD_NOT_FOUND);
        lead.setAssignedTo(null);
        leads.saveAndFlush(lead);
        assertError(() -> service.markLost(lead.getId(), request("Không còn nhu cầu")), ErrorCode.LEAD_NOT_FOUND);
        assertNoChanges();
    }

    @Test
    void managerCanCloseAnotherCounselorsLead() {
        Role manager = roles.findByCodeIgnoreCase("MANAGER").orElseGet(() ->
                roles.saveAndFlush(Role.builder().code("MANAGER").name("Manager").build()));
        counselor.setRole(manager);
        users.saveAndFlush(counselor);
        lead.setAssignedTo(UUID.randomUUID());
        leads.saveAndFlush(lead);
        assertThat(service.markLost(lead.getId(), request("Không còn nhu cầu")).getStatus())
                .isEqualTo(LeadStatus.LOST);
    }

    @Test
    void rejectsMissingDeletedAndClosedLeads() {
        assertError(() -> service.markLost(UUID.randomUUID(), request("Lý do")), ErrorCode.LEAD_NOT_FOUND);
        lead.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        leads.saveAndFlush(lead);
        assertError(() -> service.markLost(lead.getId(), request("Lý do")), ErrorCode.LEAD_NOT_FOUND);
        lead.setDeletedAt(null);
        for (LeadStatus status : new LeadStatus[]{LeadStatus.LOST, LeadStatus.ARCHIVED, LeadStatus.CONVERTED}) {
            lead.setStatus(status);
            lead.setLostReason("Lý do cũ");
            lead.setConvertedAt(status == LeadStatus.CONVERTED ? LocalDateTime.now(ZoneOffset.UTC) : null);
            leads.saveAndFlush(lead);
            assertError(() -> service.markLost(lead.getId(), request("Lý do mới")), ErrorCode.LEAD_NOT_MODIFIABLE);
        }
        assertThat(historyCount()).isZero();
        assertThat(statusHistoryCount()).isZero();
    }

    @Test
    void rejectsInvalidReasonsBeforeWriting() {
        for (String reason : new String[]{null, "", "   ", "a".repeat(501)}) {
            assertError(() -> service.markLost(lead.getId(), request(reason)), ErrorCode.INVALID_REQUEST);
        }
        assertNoChanges();
    }

    @Test
    void rejectsUnavailableLostStageAndInactivePipeline() {
        // Các cấu hình dưới đây đều khiến stage không đủ điều kiện đánh dấu thất bại.
        String[] updates = {
                "update pipeline_stages set status = 'INACTIVE' where id = ?",
                "update pipeline_stages set status = 'ACTIVE', is_terminal = false where id = ?",
                "update pipeline_stages set is_terminal = true, category = 'OPEN' where id = ?",
                "update pipeline_stages set category = 'LOST', code = 'OTHER' where id = ?"
        };
        for (String update : updates) {
            jdbc.update(update, targetStageId);
            assertError(() -> service.markLost(lead.getId(), request("Lý do")), ErrorCode.LEAD_STAGE_UNAVAILABLE);
        }
        jdbc.update("update pipeline_stages set code = 'LOST' where id = ?", targetStageId);
        jdbc.update("update pipelines set status = 'INACTIVE' where id = ?", pipelineId);
        assertError(() -> service.markLost(lead.getId(), request("Lý do")), ErrorCode.LEAD_PIPELINE_UNAVAILABLE);
        assertNoChanges();
    }

    @Test
    void missingUpdateAuthorityIsRejectedBySpring() {
        login("LEADS.READ");
        assertThatThrownBy(() -> service.markLost(lead.getId(), request("Lý do")))
                .isInstanceOf(AccessDeniedException.class);
        assertNoChanges();
    }

    @Test
    void responseFailureRollsBackLeadAndBothHistoriesAfterFlush() {
        // Giả lập Person đã xóa mềm: bước dựng response lỗi sau khi service đã flush.
        // Test không có transaction bên ngoài để kiểm tra rollback thật của service.
        Person person = persons.findById(lead.getPersonId()).orElseThrow();
        person.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        persons.saveAndFlush(person);
        assertError(() -> service.markLost(lead.getId(), request("Lý do")), ErrorCode.PERSON_NOT_FOUND);
        assertNoChanges();
    }

    private void assertNoChanges() {
        Lead stored = leads.findById(lead.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LeadStatus.OPEN);
        assertThat(stored.getStageId()).isEqualTo(initialStageId);
        assertThat(stored.getLostReason()).isNull();
        assertThat(historyCount()).isZero();
        assertThat(statusHistoryCount()).isZero();
    }

    private int statusHistoryCount() {
        return jdbc.queryForObject("select count(*) from lead_status_history where lead_id = ?", Integer.class, lead.getId());
    }

    private void login(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(counselor.getId().toString(), null,
                        Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private int historyCount() {
        return jdbc.queryForObject("select count(*) from stage_history where lead_id = ?", Integer.class, lead.getId());
    }

    private UUID pipeline(String type, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into pipelines (id, code, name, entity_type, is_default, status, created_at, updated_at)
                values (?, ?, 'Test', ?, false, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, id.toString(), type, status);
        return id;
    }

    private UUID stage(UUID pipelineId, String category, String status, boolean terminal, int order) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into pipeline_stages (id, pipeline_id, code, name, order_index, category,
                    is_initial, is_terminal, status, created_at)
                values (?, ?, ?, 'Test', ?, ?, false, ?, ?, CURRENT_TIMESTAMP)
                """, id, pipelineId, id.toString(), order, category, terminal, status);
        return id;
    }

    private LeadStatusUpdateRequest request(String reason) {
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest();
        request.setReason(reason);
        return request;
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }
}
