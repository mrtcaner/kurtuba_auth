UPDATE public.registered_client
SET client_secret = null
WHERE client_name = 'adm-web-client';