package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.dto.request.LeadStageUpdateRequest;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.lead.enums.LeadStatus;
import com.crm.korean_pathway_crm.lead.repository.LeadRepository;
import com.crm.korean_pathway_crm.lead.service.LeadStageService;
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
        "spring.datasource.url=jdbc:h2:mem:lead_stages;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class LeadStageIntegrationTest {
    @Autowired LeadStageService service;
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
        targetStageId = stage(pipelineId, "OPEN", "ACTIVE", false, 20);
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
    void changesStageAndWritesHistoryWithoutChangingStatus() {
        var response = service.updateStage(lead.getId(), request(targetStageId));
        assertThat(response.getStageId()).isEqualTo(targetStageId);
        Lead stored = leads.findById(lead.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LeadStatus.OPEN);
        assertThat(jdbc.queryForObject("select updated_by from leads where id = ?", UUID.class, lead.getId()))
                .isEqualTo(counselor.getId());
        var history = jdbc.queryForMap("select * from stage_history where lead_id = ?", lead.getId());
        assertThat(history.get("from_stage_id")).isEqualTo(initialStageId);
        assertThat(history.get("to_stage_id")).isEqualTo(targetStageId);
        assertThat(history.get("changed_by")).isEqualTo(counselor.getId());

        service.updateStage(lead.getId(), request(targetStageId));
        assertThat(historyCount()).isEqualTo(1);
    }

    @Test
    void rejectsWrongPipelineInactiveTerminalOrMissingStage() {
        UUID studentPipeline = pipeline("STUDENT_PROFILE", "ACTIVE");
        UUID wrongPipeline = stage(studentPipeline, "OPEN", "ACTIVE", false, 10);
        UUID inactive = stage(pipelineId, "OPEN", "INACTIVE", false, 30);
        UUID won = stage(pipelineId, "WON", "ACTIVE", true, 40);
        UUID lost = stage(pipelineId, "LOST", "ACTIVE", true, 50);
        for (UUID stageId : new UUID[]{wrongPipeline, inactive, won, lost, UUID.randomUUID()}) {
            assertError(() -> service.updateStage(lead.getId(), request(stageId)), ErrorCode.LEAD_STAGE_UNAVAILABLE);
        }
        assertThat(leads.findById(lead.getId()).orElseThrow().getStageId()).isEqualTo(initialStageId);
        assertThat(historyCount()).isZero();
    }

    @Test
    void counselorCannotReadOrChangeAnotherCounselorsLead() {
        lead.setAssignedTo(UUID.randomUUID());
        leads.saveAndFlush(lead);
        assertError(() -> service.updateStage(lead.getId(), request(targetStageId)), ErrorCode.LEAD_NOT_FOUND);
        assertError(() -> service.getAvailableStages(lead.getId()), ErrorCode.LEAD_NOT_FOUND);
        assertThat(historyCount()).isZero();
    }

    @Test
    void rejectsClosedDeletedLeadAndInactivePipeline() {
        lead.setStatus(LeadStatus.LOST);
        leads.saveAndFlush(lead);
        assertError(() -> service.updateStage(lead.getId(), request(targetStageId)), ErrorCode.LEAD_NOT_MODIFIABLE);
        lead.setStatus(LeadStatus.OPEN);
        lead.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        leads.saveAndFlush(lead);
        assertError(() -> service.updateStage(lead.getId(), request(targetStageId)), ErrorCode.LEAD_NOT_FOUND);
        lead.setDeletedAt(null);
        leads.saveAndFlush(lead);
        jdbc.update("update pipelines set status = 'INACTIVE' where id = ?", pipelineId);
        assertError(() -> service.updateStage(lead.getId(), request(targetStageId)), ErrorCode.LEAD_PIPELINE_UNAVAILABLE);
        assertThat(historyCount()).isZero();
    }

    @Test
    void listsOnlyActiveOpenStagesOfTheLeadsPipelineInOrder() {
        stage(pipelineId, "OPEN", "INACTIVE", false, 5);
        stage(pipelineId, "WON", "ACTIVE", true, 30);
        stage(pipeline("STUDENT_PROFILE", "ACTIVE"), "OPEN", "ACTIVE", false, 10);
        assertThat(service.getAvailableStages(lead.getId()))
                .extracting(item -> item.getId()).containsExactly(initialStageId, targetStageId);
    }

    @Test
    void missingAuthorityIsRejectedBySpringBeforeUpdating() {
        login("LEADS.READ");
        assertThatThrownBy(() -> service.updateStage(lead.getId(), request(targetStageId)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(historyCount()).isZero();
    }

    @Test
    void failureAfterFlushRollsBackBothLeadAndHistory() {
        // Person không tồn tại làm bước tạo response lỗi sau khi đã flush Lead.
        // Test không có transaction bên ngoài, nên kiểm tra được rollback thật của service.
        lead.setPersonId(UUID.randomUUID());
        leads.saveAndFlush(lead);
        assertError(() -> service.updateStage(lead.getId(), request(targetStageId)), ErrorCode.PERSON_NOT_FOUND);
        assertThat(leads.findById(lead.getId()).orElseThrow().getStageId()).isEqualTo(initialStageId);
        assertThat(historyCount()).isZero();
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

    private LeadStageUpdateRequest request(UUID stageId) {
        LeadStageUpdateRequest request = new LeadStageUpdateRequest();
        request.setStageId(stageId);
        return request;
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }
}
