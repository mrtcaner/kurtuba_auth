ALTER TABLE public.registered_client
    ADD COLUMN cookie_http_only boolean NOT NULL DEFAULT false,
    ADD COLUMN cookie_secure boolean NOT NULL DEFAULT false;

UPDATE public.registered_client
SET cookie_http_only = true,
    cookie_secure = true
WHERE client_type = 'WEB';
