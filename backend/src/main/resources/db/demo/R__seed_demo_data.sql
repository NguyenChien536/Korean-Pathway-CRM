-- Chỉ được nạp khi chạy với Spring profile `dev`.
-- Seed dữ liệu CRM giả lập, không tạo tài khoản hay lưu PII có thật.
-- Có thể chạy lặp lại: dùng UUID ổn định và bỏ qua bản ghi đã tồn tại.

INSERT INTO persons (id, full_name)
VALUES
    ('00000000-0000-4000-8000-000000000010', 'Nguyen Van A'),
    ('00000000-0000-4000-8000-000000000011', 'Nguyen Van B'),
    ('00000000-0000-4000-8000-000000000012', 'Nguyen Van C')
ON CONFLICT (id) DO NOTHING;

INSERT INTO leads (
    id, person_id, source_id, pipeline_id, stage_id,
    status, campaign_name, currency_code
)
SELECT demo.lead_id, demo.person_id, lead_source.id, pipeline.id, stage.id,
       'OPEN', 'DEMO', 'VND'
FROM (VALUES
    ('00000000-0000-4000-8000-000000000020'::uuid, '00000000-0000-4000-8000-000000000010'::uuid, 'FACEBOOK', 'NEW'),
    ('00000000-0000-4000-8000-000000000021'::uuid, '00000000-0000-4000-8000-000000000011'::uuid, 'ZALO', 'CONTACTED'),
    ('00000000-0000-4000-8000-000000000022'::uuid, '00000000-0000-4000-8000-000000000012'::uuid, 'WEBSITE', 'CONSULTATION')
) AS demo(lead_id, person_id, source_code, stage_code)
JOIN lead_sources lead_source ON lead_source.code = demo.source_code
JOIN pipelines pipeline ON pipeline.code = 'LEAD_DEFAULT'
JOIN pipeline_stages stage ON stage.pipeline_id = pipeline.id AND stage.code = demo.stage_code
WHERE NOT EXISTS (SELECT 1 FROM leads l WHERE l.id = demo.lead_id)
  AND NOT EXISTS (
      SELECT 1 FROM leads l
      WHERE l.person_id = demo.person_id AND l.status = 'OPEN' AND l.deleted_at IS NULL
  );
