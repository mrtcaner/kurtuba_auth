UPDATE public.users
SET last_login_attempt = CURRENT_TIMESTAMP
WHERE last_login_attempt IS NULL;

ALTER TABLE public.users
    ALTER COLUMN last_login_attempt SET NOT NULL;
