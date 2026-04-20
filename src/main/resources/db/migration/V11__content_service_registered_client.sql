insert into public.registered_client (access_token_ttl_minutes, cookie_max_age_seconds, refresh_token_enabled,
                                      refresh_token_ttl_minutes, scope_enabled, send_token_in_cookie, created_date,
                                      client_id, client_name, client_secret, client_type, id, post_logout_redirect_urls,
                                      redirect_urls, scopes, auds, cookie_http_only, cookie_secure)
values (15, 0, false, 0, true, false, CURRENT_TIMESTAMP, 'e02659a9-faa4-4135-ad01-cd455a76a193',
        'content-service-auth-client', '$2a$10$Tq/5pjHurjhSZJMaXTaZIuS/NF5VLAo19ocPFao8nF4.AfzeDVm8K', 'SERVICE', '8f02babf-effa-40cf-8339-9daaa891b6d6', '', '', 'SERVICE',
        'https://api.kurtuba.app/content', false, false);
