-- User phone numbers are unique when provided. Blank values are normalized to NULL by UserService.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        WHERE phone IS NOT NULL AND btrim(phone) <> ''
        GROUP BY btrim(phone)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot create uq_users_phone: duplicate user phone numbers exist';
    END IF;
END;
$$;

CREATE UNIQUE INDEX uq_users_phone
    ON users (btrim(phone))
    WHERE phone IS NOT NULL AND btrim(phone) <> '';
