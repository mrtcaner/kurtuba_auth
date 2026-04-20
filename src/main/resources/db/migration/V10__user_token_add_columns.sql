ALTER TABLE public.user_token
    ADD COLUMN updated_date TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE public.user_token
    ADD COLUMN used_date TIMESTAMP(6) WITH TIME ZONE;