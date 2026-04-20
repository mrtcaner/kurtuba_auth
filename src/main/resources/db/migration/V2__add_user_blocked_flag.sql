ALTER TABLE public.users
    ADD COLUMN blocked boolean NOT NULL DEFAULT false;
