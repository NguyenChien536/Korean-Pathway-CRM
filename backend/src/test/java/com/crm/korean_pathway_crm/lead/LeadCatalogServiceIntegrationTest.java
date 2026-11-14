package com.crm.korean_pathway_crm.lead;

import com.crm.korean_pathway_crm.exception.AppException;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import com.crm.korean_pathway_crm.lead.service.LeadCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeadCatalogServiceIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LeadCatalogService leadCatalogService;

    @Test
    void resolvesTheActiveLeadPipelineItsInitialStageAndActiveSource() {
        UUID leadPipelineId = UUID.randomUUID();
        UUID studentPipelineId = UUID.randomUUID();
        UUID initialLeadStageId = UUID.randomUUID();
        UUID inactiveStageId = UUID.randomUUID();
        UUID studentStageId = UUID.randomUUID();
        UUID activeSourceId = UUID.randomUUID();
        UUID inactiveSourceId = UUID.randomUUID();

        insertPipeline(leadPipelineId, "LEAD_TEST", "LEAD");
        insertPipeline(studentPipelineId, "STUDENT_TEST", "STUDENT_PROFILE");
        insertStage(inactiveStageId, leadPipelineId, "OLD_NEW", "INACTIVE", 5);
        insertStage(initialLeadStageId, leadPipelineId, "NEW", "ACTIVE", 10);
        insertStage(studentStageId, studentPipelineId, "DOCUMENT_PREPARATION", "ACTIVE", 10);
        insertSource(activeSourceId, "ZALO_TEST", "ACTIVE");
        insertSource(inactiveSourceId, "OLD_SOURCE", "INACTIVE");

        assertThat(leadCatalogService.resolveInitialLeadStage())
                .isEqualTo(new LeadCatalogService.InitialLeadStage(leadPipelineId, initialLeadStageId));
        assertThat(leadCatalogService.resolveActiveSourceId(activeSourceId)).isEqualTo(activeSourceId);
        assertThat(leadCatalogService.resolveActiveSourceId(null)).isNull();
        assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> leadCatalogService.resolveActiveSourceId(inactiveSourceId))
                .satisfies(error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LEAD_SOURCE_NOT_FOUND));
    }

    private void insertPipeline(UUID id, String code, String entityType) {
        jdbcTemplate.update("""
                INSERT INTO pipelines (id, code, name, entity_type, is_default, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, code, code, entityType);
    }

    private void insertStage(UUID id, UUID pipelineId, String code, String status, int orderIndex) {
        jdbcTemplate.update("""
                INSERT INTO pipeline_stages
                    (id, pipeline_id, code, name, order_index, category, is_initial,
                     is_terminal, status, created_at)
                VALUES (?, ?, ?, ?, ?, 'OPEN', true, false, ?, CURRENT_TIMESTAMP)
                """, id, pipelineId, code, code, orderIndex, status);
    }

    private void insertSource(UUID id, String code, String status) {
        jdbcTemplate.update("""
                INSERT INTO lead_sources (id, code, name, status, created_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, id, code, code, status);
    }
}
