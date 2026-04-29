--
-- Name: localization_message; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.localization_message (
                                             id character varying(255) NOT NULL,
                                             created_date timestamp(6) with time zone NOT NULL,
                                             message_key character varying(255) NOT NULL,
                                             language_code character varying(255) NOT NULL,
                                             message character varying(255) NOT NULL,
                                             updated_date timestamp(6) with time zone
);


ALTER TABLE public.localization_message OWNER TO kurtuba_auth_migrator;

--
-- Name: localization_supported_countries; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.localization_supported_countries (
                                                         id character varying(255) NOT NULL,
                                                         country_code character varying(255) NOT NULL,
                                                         created_date timestamp(6) with time zone NOT NULL,
                                                         updated_date timestamp(6) with time zone
);


ALTER TABLE public.localization_supported_countries OWNER TO kurtuba_auth_migrator;

--
-- Name: localization_supported_langs; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.localization_supported_langs (
                                                     id character varying(255) NOT NULL,
                                                     language_code character varying(255) NOT NULL,
                                                     created_date timestamp(6) with time zone NOT NULL,
                                                     updated_date timestamp(6) with time zone
);


ALTER TABLE public.localization_supported_langs OWNER TO kurtuba_auth_migrator;

--
-- Name: message_job; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.message_job (
                                    max_try_count integer NOT NULL,
                                    try_count integer NOT NULL,
                                    created_date timestamp(6) with time zone NOT NULL,
                                    send_after_date timestamp(6) with time zone NOT NULL,
                                    updated_date timestamp(6) with time zone,
                                    contact_type character varying(255) NOT NULL,
                                    error character varying(10485760),
                                    id character varying(255) NOT NULL,
                                    message character varying(10485760) NOT NULL,
                                    recipient character varying(255) NOT NULL,
                                    sender character varying(255) NOT NULL,
                                    state character varying(255) NOT NULL,
                                    subject character varying(255),
                                    service_provider character varying(255),
                                    sid character varying(255),
                                    user_meta_change_id character varying(255)
);


ALTER TABLE public.message_job OWNER TO kurtuba_auth_migrator;

--
-- Name: registered_client; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.registered_client (
                                          access_token_ttl_minutes integer NOT NULL,
                                          cookie_max_age_seconds integer NOT NULL,
                                          refresh_token_enabled boolean NOT NULL,
                                          refresh_token_ttl_minutes integer NOT NULL,
                                          scope_enabled boolean NOT NULL,
                                          send_token_in_cookie boolean NOT NULL,
                                          created_date timestamp(6) with time zone,
                                          client_id character varying(255) NOT NULL,
                                          client_name character varying(255) NOT NULL,
                                          client_secret character varying(255),
                                          client_type character varying(255) NOT NULL,
                                          id character varying(255) NOT NULL,
                                          post_logout_redirect_urls character varying(255),
                                          redirect_urls character varying(255),
                                          scopes character varying(255),
                                          auds character varying(255),
                                          cookie_http_only boolean DEFAULT false NOT NULL,
                                          cookie_secure boolean DEFAULT false NOT NULL,
                                          CONSTRAINT registered_client_client_type_check CHECK (((client_type)::text = ANY (ARRAY[('DEFAULT'::character varying)::text, ('MOBILE'::character varying)::text, ('WEB'::character varying)::text, ('SERVICE'::character varying)::text])))
);


ALTER TABLE public.registered_client OWNER TO kurtuba_auth_migrator;

--
-- Name: role; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.role (
                             id character varying(255) NOT NULL,
                             name character varying(255)
);


ALTER TABLE public.role OWNER TO kurtuba_auth_migrator;

--
-- Name: spring_session; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.spring_session (
                                       primary_id character varying(36) NOT NULL,
                                       session_id character varying(36) NOT NULL,
                                       creation_time bigint NOT NULL,
                                       last_access_time bigint NOT NULL,
                                       max_inactive_interval integer NOT NULL,
                                       expiry_time bigint NOT NULL,
                                       principal_name character varying(100)
);


ALTER TABLE public.spring_session OWNER TO kurtuba_auth_migrator;

--
-- Name: spring_session_attributes; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.spring_session_attributes (
                                                  session_primary_id character varying(255) NOT NULL,
                                                  attribute_name character varying(200) NOT NULL,
                                                  attribute_bytes bytea NOT NULL
);


ALTER TABLE public.spring_session_attributes OWNER TO kurtuba_auth_migrator;

--
-- Name: user_fcm_token; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.user_fcm_token (
                                       id character varying(255) NOT NULL,
                                       fcm_token character varying(255) NOT NULL,
                                       registered_client_id character varying(255) NOT NULL,
                                       updated_at timestamp(6) with time zone NOT NULL,
                                       user_id character varying(255) NOT NULL,
                                       firebase_installation_id character varying(255) NOT NULL
);


ALTER TABLE public.user_fcm_token OWNER TO kurtuba_auth_migrator;

--
-- Name: user_meta_change; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.user_meta_change (
                                         executed boolean NOT NULL,
                                         max_try_count integer,
                                         try_count integer,
                                         created_date timestamp(6) with time zone,
                                         expiration_date timestamp(6) with time zone,
                                         updated_date timestamp(6) with time zone,
                                         code character varying(255),
                                         contact_type character varying(255) NOT NULL,
                                         id character varying(255) NOT NULL,
                                         link_param character varying(255),
                                         meta character varying(255),
                                         meta_operation_type character varying(255) NOT NULL,
                                         user_id character varying(255) NOT NULL,
                                         CONSTRAINT user_meta_change_contact_type_check CHECK (((contact_type)::text = ANY (ARRAY[('EMAIL'::character varying)::text, ('MOBILE'::character varying)::text]))),
    CONSTRAINT user_meta_change_meta_operation_type_check CHECK (((meta_operation_type)::text = ANY (ARRAY[('ACCOUNT_ACTIVATION'::character varying)::text, ('PASSWORD_CHANGE'::character varying)::text, ('PASSWORD_RESET'::character varying)::text, ('EMAIL_CHANGE'::character varying)::text, ('MOBILE_CHANGE'::character varying)::text])))
);


ALTER TABLE public.user_meta_change OWNER TO kurtuba_auth_migrator;

--
-- Name: user_role; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.user_role (
                                  created_date timestamp(6) with time zone NOT NULL,
                                  id character varying(255) NOT NULL,
                                  user_id character varying(255) NOT NULL,
                                  role_id character varying(255)
);


ALTER TABLE public.user_role OWNER TO kurtuba_auth_migrator;

--
-- Name: user_setting; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.user_setting (
                                     id character varying(255) NOT NULL,
                                     bio character varying(255),
                                     can_change_username boolean NOT NULL,
                                     created_date timestamp(6) with time zone NOT NULL,
                                     profile_cover character varying(255),
                                     profile_pic character varying(255),
                                     user_id character varying(255),
                                     language_code character varying(255),
                                     country_code character varying(255)
);


ALTER TABLE public.user_setting OWNER TO kurtuba_auth_migrator;

--
-- Name: user_token; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.user_token (
                                   blocked boolean NOT NULL,
                                   created_date timestamp(6) with time zone NOT NULL,
                                   expiration_date timestamp(6) with time zone NOT NULL,
                                   refresh_token_exp timestamp(6) with time zone NOT NULL,
                                   auds character varying(255) NOT NULL,
                                   client_id character varying(255) NOT NULL,
                                   id character varying(255) NOT NULL,
                                   jti character varying(255) NOT NULL,
                                   refresh_token character varying(255) NOT NULL,
                                   scopes character varying(255),
                                   user_id character varying(255) NOT NULL,
                                   refresh_token_used boolean DEFAULT false NOT NULL,
                                   updated_date timestamp(6) with time zone,
                                   used_date timestamp(6) with time zone
);


ALTER TABLE public.user_token OWNER TO kurtuba_auth_migrator;

--
-- Name: user_token_block; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.user_token_block (
                                         created_date timestamp(6) with time zone NOT NULL,
                                         expiration_date timestamp(6) with time zone,
                                         id character varying(255) NOT NULL,
                                         jti character varying(255) NOT NULL
);


ALTER TABLE public.user_token_block OWNER TO kurtuba_auth_migrator;

--
-- Name: users; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.users (
                              activated boolean NOT NULL,
                              email_verified boolean NOT NULL,
                              failed_login_count integer NOT NULL,
                              locked boolean NOT NULL,
                              mobile_verified boolean NOT NULL,
                              show_captcha boolean NOT NULL,
                              birthdate timestamp(6) with time zone,
                              created_date timestamp(6) with time zone NOT NULL,
                              last_login_attempt timestamp(6) with time zone NOT NULL,
                              name character varying(100) NOT NULL,
                              auth_provider character varying(255) NOT NULL,
                              email character varying(255),
                              id character varying(255) NOT NULL,
                              mobile character varying(255),
                              password character varying(255) NOT NULL,
                              surname character varying(255),
                              username character varying(255),
                              gender character varying(255),
                              blocked boolean DEFAULT false NOT NULL,
                              CONSTRAINT user_auth_provider_check CHECK (((auth_provider)::text = ANY (ARRAY[('KURTUBA'::character varying)::text, ('GOOGLE'::character varying)::text, ('FACEBOOK'::character varying)::text, ('GITHUB'::character varying)::text]))),
    CONSTRAINT user_gender_check CHECK (((gender)::text = ANY (ARRAY[('FEMALE'::character varying)::text, ('MALE'::character varying)::text])))
);


ALTER TABLE public.users OWNER TO kurtuba_auth_migrator;

--
-- Data for Name: localization_message; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('4f903c7e-6c4e-4a25-80c2-759a359b6104', '2025-01-08 19:32:52.85976+00', 'mail.account.activation.subject', 'en', 'Kurtuba Account Activation', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('535260e0-105f-45fa-9f8a-99cf7b6fa1da', '2025-01-08 19:32:52.90406+00', 'mail.account.activation.content.title', 'en', 'THANKS FOR SIGNING UP!', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('771dfc52-d4a2-4fef-bc2e-189672350bc0', '2025-01-08 19:32:52.926255+00', 'mail.account.activation.content.greet', 'en', 'Hi', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('0dadd35c-24b5-4824-88ff-904c3005ab7d', '2025-01-08 19:32:52.946453+00', 'mail.account.activation.content.code.prologue', 'en', 'You''re almost ready to get started. Here is your activation code', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('a5628617-6aa7-4dab-8568-144838dbf0eb', '2025-01-08 19:32:52.967914+00', 'mail.account.activation.content.code.epilogue', 'en', 'You can login to Kurtuba with your existing credentials to enter the code', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('6f3a38a2-ed5b-4eb7-8a9d-434f0c78e72c', '2025-01-08 19:32:52.990761+00', 'mail.account.activation.content.link.prologue', 'en', 'You''re almost ready to get started. Click below to activate your account', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('6a8b1f41-e541-40b9-ab76-69bc1701a862', '2025-01-08 19:32:53.01337+00', 'mail.account.activation.content.link.button.label', 'en', 'ACTIVATE ACCOUNT', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('2be6bfc3-8759-41f0-b769-9b072cb75aab', '2025-01-08 19:32:53.03383+00', 'mail.account.activation.content.closing', 'en', 'Cheers', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('fe3f59d2-a8ab-409e-9287-393f61418512', '2025-01-08 19:32:53.057796+00', 'mail.account.activation.content.closing.subject', 'en', 'Kurtuba Team', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('a48eb4d3-55db-4fc5-8cdc-d3d731e63c4c', '2025-01-08 19:32:53.079973+00', 'mail.account.activation.content.get-in-touch', 'en', 'Get In Touch', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('a8d749c9-8ed6-4823-925c-e14149345a9a', '2025-01-08 19:32:53.101256+00', 'mail.password.reset.subject', 'en', 'Kurtuba Password Reset', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('e4bc8991-c84a-4015-9f53-8cd053322a1f', '2025-01-08 19:32:53.120757+00', 'mail.password.reset.content.code.prologue', 'en', 'We received a request to reset your Kurtuba password. Here is you code', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('f0931f5d-cb80-42f2-8f75-04bac18c9f8a', '2025-01-08 19:32:53.141815+00', 'mail.password.reset.content.link.prologue', 'en', 'We received a request to reset your Kurtuba password. Click the link below to reset your password', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('3c5720e3-b82a-4b6c-8518-2a14e0106089', '2025-01-08 19:32:53.163738+00', 'mail.password.reset.content.epilogue', 'en', 'If you didn''t request to reset your password, ignore this email', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('2f774f16-faaf-4307-8eda-5b3191b5efc0', '2025-01-08 19:32:53.185099+00', 'mail.password.reset.content.closing', 'en', 'Cheers', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('171993c1-1285-4552-847d-cdb88be2a194', '2025-01-08 19:32:53.205741+00', 'mail.password.reset.content.closing.subject', 'en', 'Kurtuba Team', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('16ca555e-fe83-4c49-8449-5c458b9419f5', '2025-01-08 19:32:53.225075+00', 'mail.password.reset.content.get-in-touch', 'en', 'Get In Touch', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('6f559d81-dde0-40a7-be5c-fc624b31e0ba', '2025-01-08 19:32:53.24607+00', 'mail.email.verification.subject', 'en', 'Kurtuba Email Verification', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('a282b479-8387-4341-9e9e-eed266671d69', '2025-01-08 19:32:53.268032+00', 'mail.email.verification.content.title', 'en', 'Verify Your E-mail Address!', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('6c8a7930-cbd9-49de-bf97-73867ac3e937', '2025-01-08 19:32:53.290293+00', 'mail.email.verification.content.greet', 'en', 'Hi', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('cdd84012-d515-466a-9577-b41f743819e2', '2025-01-08 19:32:53.316667+00', 'mail.email.verification.content.code.prologue', 'en', 'Here is your verification code', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('325f2ece-af57-4926-aaf3-1895ba73a194', '2025-01-08 19:32:53.338632+00', 'mail.email.verification.content.link.prologue', 'en', 'Click below to verify your email address', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('e5f53dac-2d77-4c0a-908f-37dcfa451070', '2025-01-08 19:32:53.361783+00', 'mail.email.verification.content.link.button.label', 'en', 'VERIFY YOUR EMAIL', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('d7cd84bd-d6e4-48a4-9ec6-300fe4fa3b74', '2025-01-08 19:32:53.385365+00', 'mail.email.verification.content.closing', 'en', 'Cheers', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('082bac06-3e77-4316-8bc7-d3d9f1475710', '2025-01-08 19:32:53.408142+00', 'mail.email.verification.content.closing.subject', 'en', 'Kurtuba Team', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('282fbabb-a526-46a0-a2df-c66a1d28837d', '2025-01-08 19:32:53.430264+00', 'mail.email.verification.content.get-in-touch', 'en', 'Get In Touch', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('65a7d251-590c-414c-8c21-4b3f57ef08e9', '2025-01-08 19:32:53.454116+00', 'mail.account.modification.subject', 'en', 'Kurtuba Account Modification', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('658ebdbd-834d-440e-8c31-1fb071edfe5d', '2025-01-08 19:32:53.480707+00', 'mail.account.modification.content.greet', 'en', 'Hi', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('edeeccd0-7df8-43b6-98b6-8108e7c4d8ad', '2025-01-08 19:32:53.50656+00', 'mail.account.modification.content.prologue', 'en', 'Your Kurtuba account metaName has changed.', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('3e9ce236-0af2-405e-af8b-dafb9cfdfcff', '2025-01-08 19:32:53.529333+00', 'mail.account.modification.content.context', 'en', 'Remember to use your new metaName the next time you want to log in to Kurtuba.', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('5395facf-e02f-4d70-973f-7541f6f6b0f7', '2025-01-08 19:32:53.5551+00', 'mail.account.modification.content.epilogue', 'en', 'If you didn’t make this change, please get in touch straight away.', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('ac7ffa18-9ff3-445e-b248-40df0a3f7a39', '2025-01-08 19:32:53.581992+00', 'mail.account.modification.content.closing', 'en', 'Cheers', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('1fb7eea1-0fe0-4293-abe3-299ac76a1657', '2025-01-08 19:32:53.609221+00', 'mail.account.modification.content.closing.subject', 'en', 'Kurtuba Team', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('7d47eac9-5668-4a80-ad4c-a44304bfc3ca', '2025-01-08 19:32:53.636318+00', 'mail.account.modification.content.get-in-touch', 'en', 'Get In Touch', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('97ce2265-06ab-4d18-9773-c9c07fa51d30', '2025-01-18 13:07:16.034195+00', 'sms.account.activation.message', 'en', 'Kurtuba activation code: ', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('36c796fa-51a4-48c4-ab13-69ac388dfdee', '2025-01-18 13:07:16.092947+00', 'sms.account.activation.sender', 'en', 'KURTUBA', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('1e74fb7d-3591-49d9-b633-1b6bb21b7fa3', '2026-03-06 03:21:30.761847+00', 'mail.account.activation.subject', 'tr', 'Kurtuba Hesap Aktivasyonu', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('51fe07c2-a785-4b3a-82a8-fa0a0446eda1', '2026-03-06 03:23:04.296176+00', 'mail.account.activation.content.title', 'tr', 'KAYIT OLDUĞUNUZ İÇİN TEŞEKKÜRLER!', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('7e4a2a67-be41-4e52-8ec6-f9e7acd5aa5e', '2026-03-06 03:23:04.334618+00', 'mail.account.activation.content.greet', 'tr', 'Merhaba', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('0035b0d7-c434-4f65-aca7-d8f901f6cdd6', '2026-03-06 03:23:04.369676+00', 'mail.account.activation.content.code.prologue', 'tr', 'Başlamak için neredeyse hazırsınız. Aktivasyon kodunuz', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('4a943a42-0229-49e9-ae13-164014ed903f', '2026-03-06 03:23:04.407166+00', 'mail.account.activation.content.code.epilogue', 'tr', 'Kodu girmek için mevcut Kurtuba kullanıcı adı ve şifrenizi kullanabilirisiniz', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('b08afe01-343e-40a4-bdd0-9557afde0fd6', '2026-03-06 03:23:04.437188+00', 'mail.account.activation.content.link.prologue', 'tr', 'Başlamak için neredeyse hazırsınız. Hesabınızı aktifleştirmek için aşağıdaki linke tıklayın', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('961b9de7-2560-4253-9294-3cf1e5ce5847', '2026-03-06 03:23:04.470036+00', 'mail.account.activation.content.link.button.label', 'tr', 'HESABI AKTİFLEŞTİR', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('779f7498-3500-4da7-8da8-fb0a83fe3692', '2026-03-06 03:23:04.507032+00', 'mail.account.activation.content.closing', 'tr', 'Saygılarımızla', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('3811df2f-27da-481f-92c6-6a83d4ff9792', '2026-03-06 03:23:04.536531+00', 'mail.account.activation.content.closing.subject', 'tr', 'Kurtuba Team', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('7e225944-d681-4a86-880c-89bed160fb4b', '2026-03-06 03:23:04.578197+00', 'mail.account.activation.content.get-in-touch', 'tr', 'İletişime geç', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('c95094c4-5cfe-4280-9221-a2373b9f631a', '2026-03-06 03:23:04.612338+00', 'mail.password.reset.subject', 'tr', 'Kurtuba Şifre Sıfırlama', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('fb722c14-1427-458d-8f42-018a79e8e176', '2026-03-06 03:23:04.644015+00', 'mail.password.reset.content.code.prologue', 'tr', 'Kurtuba şifrenizi yenileme isteğinizi aldık. Yenileme Kodunuz', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('7619dea5-f179-4287-a1f6-45715c12c817', '2026-03-06 03:23:04.676962+00', 'mail.password.reset.content.link.prologue', 'tr', 'Kurtuba şifrenizi yenileme isteğinizi aldık. Şifrenizi yenilemek için aşağıdaki linke tılayın', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('fc823b07-e4c6-420e-b112-bc5477fc26f4', '2026-03-06 03:23:04.711389+00', 'mail.password.reset.content.epilogue', 'tr', 'Eğer şifre yenileme isteğini siz başlatmadıysanız bu e-postayı dikkate almayın', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('9f0c2e12-18bc-4b7f-8885-d29f56d67c9f', '2026-03-06 03:23:04.745482+00', 'mail.password.reset.content.closing', 'tr', 'Saygılarımızla', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('f2864ca9-fd25-4f0c-8802-cc8bb3498ced', '2026-03-06 03:23:04.787677+00', 'mail.password.reset.content.closing.subject', 'tr', 'Kurtuba Team', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('ef8f09e5-c10d-4396-9c89-7394b37c1f7b', '2026-03-06 03:23:04.888521+00', 'mail.password.reset.content.get-in-touch', 'tr', 'İletişime Geç', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('3574b62c-21ed-4015-aa05-b762f634bb94', '2026-03-06 03:23:04.922432+00', 'mail.email.verification.subject', 'tr', 'Kurtuba E-Posta Onaylama', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('c2bad5c4-8076-44b2-ad6c-3ce2658e62c2', '2026-03-06 03:23:04.954318+00', 'mail.email.verification.content.title', 'tr', 'E-Posta Adresinizi Onaylayın!', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('ff70807b-1bed-483a-ad7d-1592c5123e11', '2026-03-06 03:23:04.992662+00', 'mail.email.verification.content.greet', 'tr', 'Merhaba', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('3945cff4-3e2e-4144-9057-7500ad799259', '2026-03-06 03:23:05.025325+00', 'mail.email.verification.content.code.prologue', 'tr', 'Onaylama Kodunuz', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('9605069a-76ea-4910-b7f6-876f50ac17af', '2026-03-06 03:23:05.05965+00', 'mail.email.verification.content.link.prologue', 'tr', 'E-Posta adresinizi onaylamak için aşağıdaki linke tıklayın', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('90047b2b-c004-477c-8820-d42ec107df9f', '2026-03-06 03:23:05.094227+00', 'mail.email.verification.content.link.button.label', 'tr', 'E-POSTA ADRESİNİZİ ONAYLAYIN', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('40eb572d-51c3-417a-9859-ec25d3a3fa17', '2026-03-06 03:23:05.131324+00', 'mail.email.verification.content.closing', 'tr', 'Saygılarımızla', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('ce5e8434-d02b-4811-8f6c-14f185d80037', '2026-03-06 03:23:05.168287+00', 'mail.email.verification.content.closing.subject', 'tr', 'Kurtuba Team', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('7d530b68-0c74-4947-a71e-4b981497399a', '2026-03-06 03:23:05.20296+00', 'mail.email.verification.content.get-in-touch', 'tr', 'İletişime Geç', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('dd52aa2a-6f2f-40dc-986c-9f1ef41fce84', '2026-03-06 03:23:05.237533+00', 'mail.account.modification.subject', 'tr', 'Kurtuba Hesap Değişikliği', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('5438662f-0e0a-486d-83bf-19e581a20582', '2026-03-06 03:23:05.269918+00', 'mail.account.modification.content.greet', 'tr', 'Merhaba', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('ab8d6ec1-0ac3-4e05-aa92-d73ff79c88f0', '2026-03-06 03:23:05.332057+00', 'mail.account.modification.content.context', 'tr', 'Krtuba hesabızına giriş yaparken yeni metaName kullanmayı unutmayın.', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('0a95c67f-0c3b-4c05-9974-f1a9f347f689', '2026-03-06 03:23:05.363532+00', 'mail.account.modification.content.epilogue', 'tr', 'Eğer bu değişikliği siz yapmadıysanız lütfen hemen! iletişime geçin.', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('349cc385-a258-4933-81e4-7f928eeae4ac', '2026-03-06 03:23:05.396446+00', 'mail.account.modification.content.closing', 'tr', 'Saygılarımızla', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('0b6e2324-476a-4b60-b51c-54d5833e96c2', '2026-03-06 03:23:05.4274+00', 'mail.account.modification.content.closing.subject', 'tr', 'Kurtuba Team', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('2b43a201-efc4-47ab-ab4e-517452732d9a', '2026-03-06 03:23:05.460614+00', 'mail.account.modification.content.get-in-touch', 'tr', 'İletişime Geç', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('9fcff111-a197-40cb-b799-069edf72bf9a', '2026-03-06 03:23:05.495315+00', 'sms.account.activation.message', 'tr', 'Kurtuba aktivasyon kodu: ', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('2a68dc01-7921-4da2-bc9a-e8fedbd40b40', '2026-03-06 03:23:05.528476+00', 'sms.account.activation.sender', 'tr', 'KURTUBA', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('fc2a750c-9b2b-4b96-9bad-9c1181f8aff2', '2026-03-06 03:23:05.300358+00', 'mail.account.modification.content.prologue', 'tr', 'Kurtuba hesabınızda metaName değişti.', '2026-04-29 19:21:20.839116+00');
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('65207b5f-457a-4531-aed9-7fd977b600ca', '2026-04-29 19:21:20.839116+00', 'mail.account.modification.content.metaname.password', 'tr', 'şifre', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('40050cf5-9a54-402f-9998-621e0708b95e', '2026-04-29 19:21:20.839116+00', 'mail.account.modification.content.metaname.phonenumber', 'tr', 'telefon numarası', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('95743a83-9c8d-4f38-b313-eb0f6349a062', '2026-04-29 19:21:20.839116+00', 'mail.account.modification.content.metaname.emailaddress', 'tr', 'email adresi', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('75ad8dc2-095f-423e-87a8-fdb5262c2fc9', '2026-04-29 19:21:20.839116+00', 'mail.account.modification.content.metaname.password', 'en', 'password', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('c8f7bc1b-8fee-4543-9e7d-ec479a011792', '2026-04-29 19:21:20.839116+00', 'mail.account.modification.content.metaname.phonenumber', 'en', 'mobile number', NULL);
INSERT INTO public.localization_message (id, created_date, message_key, language_code, message, updated_date) VALUES ('f81cc2a7-2360-4114-9faa-121e1b53ce78', '2026-04-29 19:21:20.839116+00', 'mail.account.modification.content.metaname.emailaddress', 'en', 'email address', NULL);


--
-- Data for Name: localization_supported_countries; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ad', 'ad', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ae', 'ae', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-af', 'af', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ag', 'ag', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ai', 'ai', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-al', 'al', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-am', 'am', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ao', 'ao', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-aq', 'aq', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ar', 'ar', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-as', 'as', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-at', 'at', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-au', 'au', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-aw', 'aw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ax', 'ax', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-az', 'az', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ba', 'ba', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bb', 'bb', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bd', 'bd', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-be', 'be', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bf', 'bf', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bg', 'bg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bh', 'bh', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bi', 'bi', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bj', 'bj', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bl', 'bl', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bm', 'bm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bn', 'bn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bo', 'bo', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bq', 'bq', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-br', 'br', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bs', 'bs', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bt', 'bt', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bv', 'bv', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bw', 'bw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-by', 'by', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-bz', 'bz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ca', 'ca', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cc', 'cc', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cd', 'cd', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cf', 'cf', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cg', 'cg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ch', 'ch', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ci', 'ci', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ck', 'ck', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cl', 'cl', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cm', 'cm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cn', 'cn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-co', 'co', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cr', 'cr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cu', 'cu', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cv', 'cv', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cw', 'cw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cx', 'cx', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cy', 'cy', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-cz', 'cz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-de', 'de', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-dj', 'dj', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-dk', 'dk', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-dm', 'dm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-do', 'do', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-dz', 'dz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ec', 'ec', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ee', 'ee', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-eg', 'eg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-eh', 'eh', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-er', 'er', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-es', 'es', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-et', 'et', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-fi', 'fi', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-fj', 'fj', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-fk', 'fk', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-fm', 'fm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-fo', 'fo', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-fr', 'fr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ga', 'ga', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gb', 'gb', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gd', 'gd', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ge', 'ge', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gf', 'gf', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gg', 'gg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gh', 'gh', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gi', 'gi', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gl', 'gl', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gm', 'gm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gn', 'gn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gp', 'gp', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gq', 'gq', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gr', 'gr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gs', 'gs', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gt', 'gt', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gu', 'gu', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gw', 'gw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-gy', 'gy', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-hk', 'hk', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-hm', 'hm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-hn', 'hn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-hr', 'hr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ht', 'ht', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-hu', 'hu', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-id', 'id', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ie', 'ie', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-il', 'il', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-im', 'im', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-in', 'in', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-io', 'io', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-iq', 'iq', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ir', 'ir', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-is', 'is', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-it', 'it', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-je', 'je', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-jm', 'jm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-jo', 'jo', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-jp', 'jp', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ke', 'ke', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-kg', 'kg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-kh', 'kh', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ki', 'ki', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-km', 'km', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-kn', 'kn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-kp', 'kp', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-kr', 'kr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-kw', 'kw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ky', 'ky', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-kz', 'kz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-la', 'la', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-lb', 'lb', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-lc', 'lc', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-li', 'li', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-lk', 'lk', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-lr', 'lr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ls', 'ls', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-lt', 'lt', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-lu', 'lu', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-lv', 'lv', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ly', 'ly', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ma', 'ma', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mc', 'mc', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-md', 'md', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-me', 'me', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mf', 'mf', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mg', 'mg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mh', 'mh', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mk', 'mk', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ml', 'ml', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mm', 'mm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mn', 'mn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mo', 'mo', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mp', 'mp', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mq', 'mq', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mr', 'mr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ms', 'ms', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mt', 'mt', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mu', 'mu', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mv', 'mv', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mw', 'mw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mx', 'mx', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-my', 'my', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-mz', 'mz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-na', 'na', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-nc', 'nc', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ne', 'ne', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-nf', 'nf', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ng', 'ng', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ni', 'ni', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-nl', 'nl', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-no', 'no', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-np', 'np', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-nr', 'nr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-nu', 'nu', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-nz', 'nz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-om', 'om', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pa', 'pa', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pe', 'pe', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pf', 'pf', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pg', 'pg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ph', 'ph', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pk', 'pk', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pl', 'pl', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pm', 'pm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pn', 'pn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pr', 'pr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ps', 'ps', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pt', 'pt', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-pw', 'pw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-py', 'py', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-qa', 'qa', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-re', 're', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ro', 'ro', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-rs', 'rs', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ru', 'ru', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-rw', 'rw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sa', 'sa', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sb', 'sb', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sc', 'sc', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sd', 'sd', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-se', 'se', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sg', 'sg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sh', 'sh', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-si', 'si', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sj', 'sj', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sk', 'sk', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sl', 'sl', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sm', 'sm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sn', 'sn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-so', 'so', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sr', 'sr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ss', 'ss', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-st', 'st', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sv', 'sv', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sx', 'sx', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sy', 'sy', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-sz', 'sz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tc', 'tc', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-td', 'td', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tf', 'tf', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tg', 'tg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-th', 'th', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tj', 'tj', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tk', 'tk', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tl', 'tl', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tm', 'tm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tn', 'tn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-to', 'to', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tr', 'tr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tt', 'tt', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tv', 'tv', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tw', 'tw', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-tz', 'tz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ua', 'ua', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ug', 'ug', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-um', 'um', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-us', 'us', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-uy', 'uy', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-uz', 'uz', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-va', 'va', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-vc', 'vc', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ve', 've', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-vg', 'vg', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-vi', 'vi', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-vn', 'vn', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-vu', 'vu', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-wf', 'wf', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ws', 'ws', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-ye', 'ye', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-yt', 'yt', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-za', 'za', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-zm', 'zm', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_countries (id, country_code, created_date, updated_date) VALUES ('country-zw', 'zw', '2026-04-29 19:21:20.839116+00', NULL);


--
-- Data for Name: localization_supported_langs; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

INSERT INTO public.localization_supported_langs (id, language_code, created_date, updated_date) VALUES ('lang-tr', 'tr', '2026-04-29 19:21:20.839116+00', NULL);
INSERT INTO public.localization_supported_langs (id, language_code, created_date, updated_date) VALUES ('lang-en', 'en', '2026-04-29 19:21:20.839116+00', NULL);


--
-- Data for Name: message_job; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--



--
-- Data for Name: registered_client; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

INSERT INTO public.registered_client (access_token_ttl_minutes, cookie_max_age_seconds, refresh_token_enabled, refresh_token_ttl_minutes, scope_enabled, send_token_in_cookie, created_date, client_id, client_name, client_secret, client_type, id, post_logout_redirect_urls, redirect_urls, scopes, auds, cookie_http_only, cookie_secure) VALUES (100, 0, false, 0, true, false, '2025-01-11 17:22:40.629165+00', 'e6ce1ca4-b94d-40e5-815b-803d1ac48d8e', 'adm-service-client', '$2a$10$wTdfgE6jNa0GAnaqzUA.tuKzqWbj2YfZEfG/5gZmlL8u1KRYNAJbK', 'SERVICE', '646383c4-467c-4b82-9ee7-9416fe89314b', '', '', 'SERVICE', 'https://api.kurtuba.app', false, false);
INSERT INTO public.registered_client (access_token_ttl_minutes, cookie_max_age_seconds, refresh_token_enabled, refresh_token_ttl_minutes, scope_enabled, send_token_in_cookie, created_date, client_id, client_name, client_secret, client_type, id, post_logout_redirect_urls, redirect_urls, scopes, auds, cookie_http_only, cookie_secure) VALUES (1, 0, true, 129600, false, false, '2025-01-03 19:04:50.253145+00', 'b4e547f9-2c80-4cf0-b46a-d87a918a2280', 'default-client', NULL, 'DEFAULT', 'ff123e66-d288-43ae-8032-bd380cfe08d5', '', '', '', 'https://api.kurtuba.app', false, false);
INSERT INTO public.registered_client (access_token_ttl_minutes, cookie_max_age_seconds, refresh_token_enabled, refresh_token_ttl_minutes, scope_enabled, send_token_in_cookie, created_date, client_id, client_name, client_secret, client_type, id, post_logout_redirect_urls, redirect_urls, scopes, auds, cookie_http_only, cookie_secure) VALUES (1, 0, true, 129600, true, false, '2025-01-11 17:22:40.540395+00', '96939331-32b9-4089-a121-934de609f5df', 'kurtuba-mobile-client', NULL, 'MOBILE', '5be04a9d-5d46-43f8-aa40-4ffff196e7fc', '', '', 'TEST,ADMIN,USER', 'https://api.kurtuba.app', false, false);
INSERT INTO public.registered_client (access_token_ttl_minutes, cookie_max_age_seconds, refresh_token_enabled, refresh_token_ttl_minutes, scope_enabled, send_token_in_cookie, created_date, client_id, client_name, client_secret, client_type, id, post_logout_redirect_urls, redirect_urls, scopes, auds, cookie_http_only, cookie_secure) VALUES (300, 7776000, true, 129600, true, true, '2025-01-11 17:22:40.615211+00', '34ff7c95-ac55-4e7c-817e-6aa9333e21f6', 'kurtuba-web-client', NULL, 'WEB', 'c742b61b-e6a7-482c-9d70-7116f2050e43', '', '', 'TEST,USER', 'https://api.kurtuba.app', true, true);
INSERT INTO public.registered_client (access_token_ttl_minutes, cookie_max_age_seconds, refresh_token_enabled, refresh_token_ttl_minutes, scope_enabled, send_token_in_cookie, created_date, client_id, client_name, client_secret, client_type, id, post_logout_redirect_urls, redirect_urls, scopes, auds, cookie_http_only, cookie_secure) VALUES (300, 7776000, true, 500, true, true, '2025-01-11 17:22:40.600476+00', '419afca3-e02a-4978-a12a-28797ef29f5a', 'adm-web-client', NULL, 'WEB', '493695d5-7e5e-4e5d-a950-2d1eb0c9eb4e', '', '', 'TEST,ADMIN,USER', 'https://api.kurtuba.app', true, true);
INSERT INTO public.registered_client (access_token_ttl_minutes, cookie_max_age_seconds, refresh_token_enabled, refresh_token_ttl_minutes, scope_enabled, send_token_in_cookie, created_date, client_id, client_name, client_secret, client_type, id, post_logout_redirect_urls, redirect_urls, scopes, auds, cookie_http_only, cookie_secure) VALUES (15, 0, false, 0, true, false, '2026-04-29 16:21:20.839116+00', 'e02659a9-faa4-p8ty-ad01-cd455a76a193', 'another-service-auth-client', '$2a$10$Tq/5pjHurjhSZJMaXTaZIuS/NF5VLAo19ocPFao8nF4.AklerDVm8K', 'SERVICE', '8f02b123-hkjf-40cf-8339-9daaa891b6d6', '', '', 'SERVICE', 'https://api.kurtuba.app/another-service', false, false);


--
-- Data for Name: role; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

INSERT INTO public.role (id, name) VALUES ('a4df29d2-4fc1-4a64-8055-11021d46489d', 'USER');
INSERT INTO public.role (id, name) VALUES ('c2699abe-7bd8-4c71-a771-8f457e808216', 'SERVICE');
INSERT INTO public.role (id, name) VALUES ('4b7ba947-b752-484d-9087-0d81ee7f4c1a', 'ADMIN');


--
-- Data for Name: spring_session; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--



--
-- Data for Name: spring_session_attributes; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--



--
-- Data for Name: user_fcm_token; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--



--
-- Data for Name: user_meta_change; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--



--
-- Data for Name: user_role; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

INSERT INTO public.user_role (created_date, id, user_id, role_id) VALUES ('2026-01-22 17:12:26+00', '067d8hfe-0kf2-4ad5-b88b-b72d9a52e521', 'b5a47277-bd17-4883-8cd8-b915bc6e852f', '4b7ba947-b752-484d-9087-0d81ee7f4c1a');
INSERT INTO public.user_role (created_date, id, user_id, role_id) VALUES ('2025-01-05 20:31:45.377913+00', '037ffdb0-7d25-4a86-b7df-ed11919db65c', 'b5a47277-bd17-4883-8cd8-b915bc6e852f', 'a4df29d2-4fc1-4a64-8055-11021d46489d');


--
-- Data for Name: user_setting; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

INSERT INTO public.user_setting (id, bio, can_change_username, created_date, profile_cover, profile_pic, user_id, language_code, country_code) VALUES ('57236c19-b438-4cfd-9451-edcc727e0290', NULL, false, '2025-01-09 02:45:54.403529+00', NULL, NULL, 'b5a47277-bd17-4883-8cd8-b915bc6e852f', 'tr', 'tr');


--
-- Data for Name: user_token; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--



--
-- Data for Name: user_token_block; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--



--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

INSERT INTO public.users (activated, email_verified, failed_login_count, locked, mobile_verified, show_captcha, birthdate, created_date, last_login_attempt, name, auth_provider, email, id, mobile, password, surname, username, gender, blocked) VALUES (true, true, 0, false, true, false, '1999-01-01 00:00:00+00', '2025-01-05 20:31:45.349792+00', '2026-03-24 10:49:36.932103+00', 'muhlis', 'KURTUBA', 'user@user.com', 'b5a47277-bd17-4883-8cd8-b915bc6e852f', '+905555555555', '$2a$10$Bb95AqMp1rtSmpYVso4RUOchrkihiVFO/4XJ6xdWxoznfkPucp4Ce', 'muhlisson', 'muhlis', 'MALE', false);


--
-- Name: localization_message localization_message_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.localization_message
    ADD CONSTRAINT localization_message_pkey PRIMARY KEY (id);


--
-- Name: localization_supported_countries localization_supported_countries_country_code_key; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.localization_supported_countries
    ADD CONSTRAINT localization_supported_countries_country_code_key UNIQUE (country_code);


--
-- Name: localization_supported_countries localization_supported_countries_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.localization_supported_countries
    ADD CONSTRAINT localization_supported_countries_pkey PRIMARY KEY (id);


--
-- Name: localization_supported_langs localization_supported_langs_language_code_key; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.localization_supported_langs
    ADD CONSTRAINT localization_supported_langs_language_code_key UNIQUE (language_code);


--
-- Name: localization_supported_langs localization_supported_langs_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.localization_supported_langs
    ADD CONSTRAINT localization_supported_langs_pkey PRIMARY KEY (id);


--
-- Name: message_job message_job_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.message_job
    ADD CONSTRAINT message_job_pkey PRIMARY KEY (id);


--
-- Name: registered_client registered_client_client_id_key; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.registered_client
    ADD CONSTRAINT registered_client_client_id_key UNIQUE (client_id);


--
-- Name: registered_client registered_client_client_name_key; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.registered_client
    ADD CONSTRAINT registered_client_client_name_key UNIQUE (client_name);


--
-- Name: registered_client registered_client_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.registered_client
    ADD CONSTRAINT registered_client_pkey PRIMARY KEY (id);


--
-- Name: role role_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);


--
-- Name: spring_session_attributes spring_session_attributes_pk; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.spring_session_attributes
    ADD CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name);


--
-- Name: spring_session spring_session_pk; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.spring_session
    ADD CONSTRAINT spring_session_pk PRIMARY KEY (primary_id);


--
-- Name: user_setting ukaq5q998x4b33hm6c0m6h6c6ox; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_setting
    ADD CONSTRAINT ukaq5q998x4b33hm6c0m6h6c6ox UNIQUE (user_id);


--
-- Name: user_fcm_token ukijr0c33lsdy2v8agmtd1p403q; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_fcm_token
    ADD CONSTRAINT ukijr0c33lsdy2v8agmtd1p403q UNIQUE (firebase_installation_id);


--
-- Name: user_fcm_token ukoe9weiugkg634vg8hacqeb9h3; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_fcm_token
    ADD CONSTRAINT ukoe9weiugkg634vg8hacqeb9h3 UNIQUE (fcm_token);


--
-- Name: user_fcm_token user_fcm_token_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_fcm_token
    ADD CONSTRAINT user_fcm_token_pkey PRIMARY KEY (id);


--
-- Name: user_meta_change user_meta_change_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_meta_change
    ADD CONSTRAINT user_meta_change_pkey PRIMARY KEY (id);


--
-- Name: users user_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);


--
-- Name: user_role user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (id);


--
-- Name: user_setting user_setting_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_setting
    ADD CONSTRAINT user_setting_pkey PRIMARY KEY (id);


--
-- Name: user_token_block user_token_block_jti_key; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_token_block
    ADD CONSTRAINT user_token_block_jti_key UNIQUE (jti);


--
-- Name: user_token_block user_token_block_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_token_block
    ADD CONSTRAINT user_token_block_pkey PRIMARY KEY (id);


--
-- Name: user_token user_token_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_token
    ADD CONSTRAINT user_token_pkey PRIMARY KEY (id);


--
-- Name: spring_session_ix1; Type: INDEX; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE UNIQUE INDEX spring_session_ix1 ON public.spring_session USING btree (session_id);


--
-- Name: spring_session_ix2; Type: INDEX; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE INDEX spring_session_ix2 ON public.spring_session USING btree (expiry_time);


--
-- Name: spring_session_ix3; Type: INDEX; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE INDEX spring_session_ix3 ON public.spring_session USING btree (principal_name);


--
-- Name: users_username_unique_idx; Type: INDEX; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE UNIQUE INDEX users_username_unique_idx ON public.users USING btree (username) WHERE ((username IS NOT NULL) AND ((username)::text <> ''::text));


--
-- Name: user_role fk859n2jvi8ivhui0rl0esws6o; Type: FK CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT fk859n2jvi8ivhui0rl0esws6o FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: user_role fka68196081fvovjhkek5m97n3y; Type: FK CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_role
    ADD CONSTRAINT fka68196081fvovjhkek5m97n3y FOREIGN KEY (role_id) REFERENCES public.role(id);


--
-- Name: user_setting fkg5ckmir2a8ejjtq4b0pcdewil; Type: FK CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_setting
    ADD CONSTRAINT fkg5ckmir2a8ejjtq4b0pcdewil FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: user_token_block fkgxmr26jabfodgbs3r7pllsuws; Type: FK CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_token_block
    ADD CONSTRAINT fkgxmr26jabfodgbs3r7pllsuws FOREIGN KEY (jti) REFERENCES public.user_token(id);


--
-- Name: spring_session_attributes spring_session_attributes_fk; Type: FK CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.spring_session_attributes
    ADD CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id) REFERENCES public.spring_session(primary_id) ON DELETE CASCADE;


--
-- Name: TABLE localization_message; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.localization_message TO kurtuba_auth_user;


--
-- Name: TABLE localization_supported_countries; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.localization_supported_countries TO kurtuba_auth_user;


--
-- Name: TABLE localization_supported_langs; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.localization_supported_langs TO kurtuba_auth_user;


--
-- Name: TABLE message_job; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.message_job TO kurtuba_auth_user;


--
-- Name: TABLE registered_client; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.registered_client TO kurtuba_auth_user;


--
-- Name: TABLE role; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.role TO kurtuba_auth_user;


--
-- Name: TABLE spring_session; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.spring_session TO kurtuba_auth_user;


--
-- Name: TABLE spring_session_attributes; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.spring_session_attributes TO kurtuba_auth_user;


--
-- Name: TABLE user_meta_change; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.user_meta_change TO kurtuba_auth_user;


--
-- Name: TABLE user_role; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.user_role TO kurtuba_auth_user;


--
-- Name: TABLE user_setting; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.user_setting TO kurtuba_auth_user;


--
-- Name: TABLE user_token; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.user_token TO kurtuba_auth_user;


--
-- Name: TABLE user_token_block; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.user_token_block TO kurtuba_auth_user;


--
-- Name: TABLE users; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.users TO kurtuba_auth_user;
