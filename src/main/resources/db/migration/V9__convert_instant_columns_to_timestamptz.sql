ALTER TABLE public.localization_message
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC',
    ALTER COLUMN updated_date TYPE timestamp(6) with time zone USING updated_date AT TIME ZONE 'UTC';

ALTER TABLE public.message_job
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC',
    ALTER COLUMN send_after_date TYPE timestamp(6) with time zone USING send_after_date AT TIME ZONE 'UTC',
    ALTER COLUMN updated_date TYPE timestamp(6) with time zone USING updated_date AT TIME ZONE 'UTC';

ALTER TABLE public.registered_client
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC';

ALTER TABLE public.user_meta_change
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC',
    ALTER COLUMN expiration_date TYPE timestamp(6) with time zone USING expiration_date AT TIME ZONE 'UTC',
    ALTER COLUMN updated_date TYPE timestamp(6) with time zone USING updated_date AT TIME ZONE 'UTC';

ALTER TABLE public.user_role
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC';

ALTER TABLE public.user_setting
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC';

ALTER TABLE public.user_token
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC',
    ALTER COLUMN expiration_date TYPE timestamp(6) with time zone USING expiration_date AT TIME ZONE 'UTC',
    ALTER COLUMN refresh_token_exp TYPE timestamp(6) with time zone USING refresh_token_exp AT TIME ZONE 'UTC';

ALTER TABLE public.user_token_block
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC',
    ALTER COLUMN expiration_date TYPE timestamp(6) with time zone USING expiration_date AT TIME ZONE 'UTC';

ALTER TABLE public.users
    ALTER COLUMN birthdate TYPE timestamp(6) with time zone USING birthdate AT TIME ZONE 'UTC',
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC',
    ALTER COLUMN last_login_attempt TYPE timestamp(6) with time zone USING last_login_attempt AT TIME ZONE 'UTC';

ALTER TABLE public.localization_supported_countries
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC',
    ALTER COLUMN updated_date TYPE timestamp(6) with time zone USING updated_date AT TIME ZONE 'UTC';

ALTER TABLE public.localization_supported_langs
    ALTER COLUMN created_date TYPE timestamp(6) with time zone USING created_date AT TIME ZONE 'UTC',
    ALTER COLUMN updated_date TYPE timestamp(6) with time zone USING updated_date AT TIME ZONE 'UTC';
