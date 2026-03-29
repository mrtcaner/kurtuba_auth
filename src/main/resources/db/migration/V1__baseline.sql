

--
-- Name: localization_available_locale; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.localization_available_locale (
    id character varying(255) NOT NULL,
    country_code character varying(255) NOT NULL,
    created_date timestamp(6) without time zone NOT NULL,
    language_code character varying(255) NOT NULL,
    updated_date timestamp(6) without time zone
);


ALTER TABLE public.localization_available_locale OWNER TO kurtuba_auth_migrator;

--
-- Name: localization_message; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.localization_message (
    id character varying(255) NOT NULL,
    created_date timestamp(6) without time zone NOT NULL,
    message_key character varying(255) NOT NULL,
    language_code character varying(255) NOT NULL,
    message character varying(255) NOT NULL,
    updated_date timestamp(6) without time zone
);


ALTER TABLE public.localization_message OWNER TO kurtuba_auth_migrator;

--
-- Name: message_job; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.message_job (
    max_try_count integer NOT NULL,
    try_count integer NOT NULL,
    created_date timestamp(6) without time zone NOT NULL,
    send_after_date timestamp(6) without time zone NOT NULL,
    updated_date timestamp(6) without time zone,
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
    created_date timestamp(6) without time zone,
    client_id character varying(255) NOT NULL,
    client_name character varying(255) NOT NULL,
    client_secret character varying(255),
    client_type character varying(255) NOT NULL,
    id character varying(255) NOT NULL,
    post_logout_redirect_urls character varying(255),
    redirect_urls character varying(255),
    scopes character varying(255),
    auds character varying(255),
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
    created_date timestamp(6) without time zone,
    expiration_date timestamp(6) without time zone,
    updated_date timestamp(6) without time zone,
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
    created_date timestamp(6) without time zone NOT NULL,
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
    created_date timestamp(6) without time zone NOT NULL,
    profile_cover character varying(255),
    profile_pic character varying(255),
    localization_available_locale_id character varying(255),
    user_id character varying(255)
);


ALTER TABLE public.user_setting OWNER TO kurtuba_auth_migrator;

--
-- Name: user_token; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.user_token (
    blocked boolean NOT NULL,
    created_date timestamp(6) without time zone NOT NULL,
    expiration_date timestamp(6) without time zone NOT NULL,
    refresh_token_exp timestamp(6) without time zone NOT NULL,
    auds character varying(255) NOT NULL,
    client_id character varying(255) NOT NULL,
    id character varying(255) NOT NULL,
    jti character varying(255) NOT NULL,
    refresh_token character varying(255) NOT NULL,
    scopes character varying(255),
    user_id character varying(255) NOT NULL,
    refresh_token_used boolean DEFAULT false NOT NULL
);


ALTER TABLE public.user_token OWNER TO kurtuba_auth_migrator;

--
-- Name: user_token_block; Type: TABLE; Schema: public; Owner: kurtuba_auth_migrator
--

CREATE TABLE public.user_token_block (
    created_date timestamp(6) without time zone NOT NULL,
    expiration_date timestamp(6) without time zone,
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
    birthdate timestamp(6) without time zone,
    created_date timestamp(6) without time zone NOT NULL,
    last_login_attempt timestamp(6) without time zone,
    name character varying(100) NOT NULL,
    auth_provider character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    id character varying(255) NOT NULL,
    mobile character varying(255),
    password character varying(255) NOT NULL,
    surname character varying(255),
    username character varying(255),
    gender character varying(255),
    CONSTRAINT user_auth_provider_check CHECK (((auth_provider)::text = ANY (ARRAY[('KURTUBA'::character varying)::text, ('GOOGLE'::character varying)::text, ('FACEBOOK'::character varying)::text, ('GITHUB'::character varying)::text]))),
    CONSTRAINT user_gender_check CHECK (((gender)::text = ANY (ARRAY[('FEMALE'::character varying)::text, ('MALE'::character varying)::text])))
);


ALTER TABLE public.users OWNER TO kurtuba_auth_migrator;


--
-- Data for Name: localization_available_locale; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.localization_available_locale (id, country_code, created_date, language_code, updated_date) FROM stdin;
8c649e91-2402-4451-a921-cda38c90e0c7	tr	2025-01-08 19:39:54.237323	tr	\N
4a145e91-2656-4911-r522-cda48c90e0c7	tr	2025-01-08 19:39:54.237323	en	\N
126c1461-7518-4287-b411-ddcb97f04218	us	2025-01-08 19:39:54.2543	en	\N
2b6a78f6-0a18-4f07-8f3a-29117e281a02	us	2025-01-08 19:39:54.266335	tr	\N
\.


--
-- Data for Name: localization_message; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.localization_message (id, created_date, message_key, language_code, message, updated_date) FROM stdin;
4f903c7e-6c4e-4a25-80c2-759a359b6104	2025-01-08 19:32:52.85976	mail.account.activation.subject	en	Kurtuba Account Activation	\N
535260e0-105f-45fa-9f8a-99cf7b6fa1da	2025-01-08 19:32:52.90406	mail.account.activation.content.title	en	THANKS FOR SIGNING UP!	\N
771dfc52-d4a2-4fef-bc2e-189672350bc0	2025-01-08 19:32:52.926255	mail.account.activation.content.greet	en	Hi	\N
0dadd35c-24b5-4824-88ff-904c3005ab7d	2025-01-08 19:32:52.946453	mail.account.activation.content.code.prologue	en	You're almost ready to get started. Here is your activation code	\N
a5628617-6aa7-4dab-8568-144838dbf0eb	2025-01-08 19:32:52.967914	mail.account.activation.content.code.epilogue	en	You can login to Kurtuba with your existing credentials to enter the code	\N
6f3a38a2-ed5b-4eb7-8a9d-434f0c78e72c	2025-01-08 19:32:52.990761	mail.account.activation.content.link.prologue	en	You're almost ready to get started. Click below to activate your account	\N
6a8b1f41-e541-40b9-ab76-69bc1701a862	2025-01-08 19:32:53.01337	mail.account.activation.content.link.button.label	en	ACTIVATE ACCOUNT	\N
2be6bfc3-8759-41f0-b769-9b072cb75aab	2025-01-08 19:32:53.03383	mail.account.activation.content.closing	en	Cheers	\N
fe3f59d2-a8ab-409e-9287-393f61418512	2025-01-08 19:32:53.057796	mail.account.activation.content.closing.subject	en	Kurtuba Team	\N
a48eb4d3-55db-4fc5-8cdc-d3d731e63c4c	2025-01-08 19:32:53.079973	mail.account.activation.content.get-in-touch	en	Get In Touch	\N
a8d749c9-8ed6-4823-925c-e14149345a9a	2025-01-08 19:32:53.101256	mail.password.reset.subject	en	Kurtuba Password Reset	\N
e4bc8991-c84a-4015-9f53-8cd053322a1f	2025-01-08 19:32:53.120757	mail.password.reset.content.code.prologue	en	We received a request to reset your Kurtuba password. Here is you code	\N
f0931f5d-cb80-42f2-8f75-04bac18c9f8a	2025-01-08 19:32:53.141815	mail.password.reset.content.link.prologue	en	We received a request to reset your Kurtuba password. Click the link below to reset your password	\N
3c5720e3-b82a-4b6c-8518-2a14e0106089	2025-01-08 19:32:53.163738	mail.password.reset.content.epilogue	en	If you didn't request to reset your password, ignore this email	\N
2f774f16-faaf-4307-8eda-5b3191b5efc0	2025-01-08 19:32:53.185099	mail.password.reset.content.closing	en	Cheers	\N
171993c1-1285-4552-847d-cdb88be2a194	2025-01-08 19:32:53.205741	mail.password.reset.content.closing.subject	en	Kurtuba Team	\N
16ca555e-fe83-4c49-8449-5c458b9419f5	2025-01-08 19:32:53.225075	mail.password.reset.content.get-in-touch	en	Get In Touch	\N
6f559d81-dde0-40a7-be5c-fc624b31e0ba	2025-01-08 19:32:53.24607	mail.email.verification.subject	en	Kurtuba Email Verification	\N
a282b479-8387-4341-9e9e-eed266671d69	2025-01-08 19:32:53.268032	mail.email.verification.content.title	en	Verify Your E-mail Address!	\N
6c8a7930-cbd9-49de-bf97-73867ac3e937	2025-01-08 19:32:53.290293	mail.email.verification.content.greet	en	Hi	\N
cdd84012-d515-466a-9577-b41f743819e2	2025-01-08 19:32:53.316667	mail.email.verification.content.code.prologue	en	Here is your verification code	\N
325f2ece-af57-4926-aaf3-1895ba73a194	2025-01-08 19:32:53.338632	mail.email.verification.content.link.prologue	en	Click below to verify your email address	\N
e5f53dac-2d77-4c0a-908f-37dcfa451070	2025-01-08 19:32:53.361783	mail.email.verification.content.link.button.label	en	VERIFY YOUR EMAIL	\N
d7cd84bd-d6e4-48a4-9ec6-300fe4fa3b74	2025-01-08 19:32:53.385365	mail.email.verification.content.closing	en	Cheers	\N
082bac06-3e77-4316-8bc7-d3d9f1475710	2025-01-08 19:32:53.408142	mail.email.verification.content.closing.subject	en	Kurtuba Team	\N
282fbabb-a526-46a0-a2df-c66a1d28837d	2025-01-08 19:32:53.430264	mail.email.verification.content.get-in-touch	en	Get In Touch	\N
65a7d251-590c-414c-8c21-4b3f57ef08e9	2025-01-08 19:32:53.454116	mail.account.modification.subject	en	Kurtuba Account Modification	\N
658ebdbd-834d-440e-8c31-1fb071edfe5d	2025-01-08 19:32:53.480707	mail.account.modification.content.greet	en	Hi	\N
edeeccd0-7df8-43b6-98b6-8108e7c4d8ad	2025-01-08 19:32:53.50656	mail.account.modification.content.prologue	en	Your Kurtuba account metaName has changed.	\N
3e9ce236-0af2-405e-af8b-dafb9cfdfcff	2025-01-08 19:32:53.529333	mail.account.modification.content.context	en	Remember to use your new metaName the next time you want to log in to Kurtuba.	\N
5395facf-e02f-4d70-973f-7541f6f6b0f7	2025-01-08 19:32:53.5551	mail.account.modification.content.epilogue	en	If you didn’t make this change, please get in touch straight away.	\N
ac7ffa18-9ff3-445e-b248-40df0a3f7a39	2025-01-08 19:32:53.581992	mail.account.modification.content.closing	en	Cheers	\N
1fb7eea1-0fe0-4293-abe3-299ac76a1657	2025-01-08 19:32:53.609221	mail.account.modification.content.closing.subject	en	Kurtuba Team	\N
7d47eac9-5668-4a80-ad4c-a44304bfc3ca	2025-01-08 19:32:53.636318	mail.account.modification.content.get-in-touch	en	Get In Touch	\N
97ce2265-06ab-4d18-9773-c9c07fa51d30	2025-01-18 13:07:16.034195	sms.account.activation.message	en	Kurtuba activation code: 	\N
36c796fa-51a4-48c4-ab13-69ac388dfdee	2025-01-18 13:07:16.092947	sms.account.activation.sender	en	KURTUBA	\N
1e74fb7d-3591-49d9-b633-1b6bb21b7fa3	2026-03-06 03:21:30.761847	mail.account.activation.subject	tr	Kurtuba Hesap Aktivasyonu	\N
51fe07c2-a785-4b3a-82a8-fa0a0446eda1	2026-03-06 03:23:04.296176	mail.account.activation.content.title	tr	KAYIT OLDUĞUNUZ İÇİN TEŞEKKÜRLER!	\N
7e4a2a67-be41-4e52-8ec6-f9e7acd5aa5e	2026-03-06 03:23:04.334618	mail.account.activation.content.greet	tr	Merhaba	\N
0035b0d7-c434-4f65-aca7-d8f901f6cdd6	2026-03-06 03:23:04.369676	mail.account.activation.content.code.prologue	tr	Başlamak için neredeyse hazırsınız. Aktivasyon kodunuz	\N
4a943a42-0229-49e9-ae13-164014ed903f	2026-03-06 03:23:04.407166	mail.account.activation.content.code.epilogue	tr	Kodu girmek için mevcut Kurtuba kullanıcı adı ve şifrenizi kullanabilirisiniz	\N
b08afe01-343e-40a4-bdd0-9557afde0fd6	2026-03-06 03:23:04.437188	mail.account.activation.content.link.prologue	tr	Başlamak için neredeyse hazırsınız. Hesabınızı aktifleştirmek için aşağıdaki linke tıklayın	\N
961b9de7-2560-4253-9294-3cf1e5ce5847	2026-03-06 03:23:04.470036	mail.account.activation.content.link.button.label	tr	HESABI AKTİFLEŞTİR	\N
779f7498-3500-4da7-8da8-fb0a83fe3692	2026-03-06 03:23:04.507032	mail.account.activation.content.closing	tr	Saygılarımızla	\N
3811df2f-27da-481f-92c6-6a83d4ff9792	2026-03-06 03:23:04.536531	mail.account.activation.content.closing.subject	tr	Kurtuba Team	\N
7e225944-d681-4a86-880c-89bed160fb4b	2026-03-06 03:23:04.578197	mail.account.activation.content.get-in-touch	tr	İletişime geç	\N
c95094c4-5cfe-4280-9221-a2373b9f631a	2026-03-06 03:23:04.612338	mail.password.reset.subject	tr	Kurtuba Şifre Sıfırlama	\N
fb722c14-1427-458d-8f42-018a79e8e176	2026-03-06 03:23:04.644015	mail.password.reset.content.code.prologue	tr	Kurtuba şifrenizi yenileme isteğinizi aldık. Yenileme Kodunuz	\N
7619dea5-f179-4287-a1f6-45715c12c817	2026-03-06 03:23:04.676962	mail.password.reset.content.link.prologue	tr	Kurtuba şifrenizi yenileme isteğinizi aldık. Şifrenizi yenilemek için aşağıdaki linke tılayın	\N
fc823b07-e4c6-420e-b112-bc5477fc26f4	2026-03-06 03:23:04.711389	mail.password.reset.content.epilogue	tr	Eğer şifre yenileme isteğini siz başlatmadıysanız bu e-postayı dikkate almayın	\N
9f0c2e12-18bc-4b7f-8885-d29f56d67c9f	2026-03-06 03:23:04.745482	mail.password.reset.content.closing	tr	Saygılarımızla	\N
f2864ca9-fd25-4f0c-8802-cc8bb3498ced	2026-03-06 03:23:04.787677	mail.password.reset.content.closing.subject	tr	Kurtuba Team	\N
ef8f09e5-c10d-4396-9c89-7394b37c1f7b	2026-03-06 03:23:04.888521	mail.password.reset.content.get-in-touch	tr	İletişime Geç	\N
3574b62c-21ed-4015-aa05-b762f634bb94	2026-03-06 03:23:04.922432	mail.email.verification.subject	tr	Kurtuba E-Posta Onaylama	\N
c2bad5c4-8076-44b2-ad6c-3ce2658e62c2	2026-03-06 03:23:04.954318	mail.email.verification.content.title	tr	E-Posta Adresinizi Onaylayın!	\N
ff70807b-1bed-483a-ad7d-1592c5123e11	2026-03-06 03:23:04.992662	mail.email.verification.content.greet	tr	Merhaba	\N
3945cff4-3e2e-4144-9057-7500ad799259	2026-03-06 03:23:05.025325	mail.email.verification.content.code.prologue	tr	Onaylama Kodunuz	\N
9605069a-76ea-4910-b7f6-876f50ac17af	2026-03-06 03:23:05.05965	mail.email.verification.content.link.prologue	tr	E-Posta adresinizi onaylamak için aşağıdaki linke tıklayın	\N
90047b2b-c004-477c-8820-d42ec107df9f	2026-03-06 03:23:05.094227	mail.email.verification.content.link.button.label	tr	E-POSTA ADRESİNİZİ ONAYLAYIN	\N
40eb572d-51c3-417a-9859-ec25d3a3fa17	2026-03-06 03:23:05.131324	mail.email.verification.content.closing	tr	Saygılarımızla	\N
ce5e8434-d02b-4811-8f6c-14f185d80037	2026-03-06 03:23:05.168287	mail.email.verification.content.closing.subject	tr	Kurtuba Team	\N
7d530b68-0c74-4947-a71e-4b981497399a	2026-03-06 03:23:05.20296	mail.email.verification.content.get-in-touch	tr	İletişime Geç	\N
dd52aa2a-6f2f-40dc-986c-9f1ef41fce84	2026-03-06 03:23:05.237533	mail.account.modification.subject	tr	Kurtuba Hesap Değişikliği	\N
5438662f-0e0a-486d-83bf-19e581a20582	2026-03-06 03:23:05.269918	mail.account.modification.content.greet	tr	Merhaba	\N
fc2a750c-9b2b-4b96-9bad-9c1181f8aff2	2026-03-06 03:23:05.300358	mail.account.modification.content.prologue	tr	Kurtuba hesabınızın metaName değişti.	\N
ab8d6ec1-0ac3-4e05-aa92-d73ff79c88f0	2026-03-06 03:23:05.332057	mail.account.modification.content.context	tr	Krtuba hesabızına giriş yaparken yeni metaName kullanmayı unutmayın.	\N
0a95c67f-0c3b-4c05-9974-f1a9f347f689	2026-03-06 03:23:05.363532	mail.account.modification.content.epilogue	tr	Eğer bu değişikliği siz yapmadıysanız lütfen hemen! iletişime geçin.	\N
349cc385-a258-4933-81e4-7f928eeae4ac	2026-03-06 03:23:05.396446	mail.account.modification.content.closing	tr	Saygılarımızla	\N
0b6e2324-476a-4b60-b51c-54d5833e96c2	2026-03-06 03:23:05.4274	mail.account.modification.content.closing.subject	tr	Kurtuba Team	\N
2b43a201-efc4-47ab-ab4e-517452732d9a	2026-03-06 03:23:05.460614	mail.account.modification.content.get-in-touch	tr	İletişime Geç	\N
9fcff111-a197-40cb-b799-069edf72bf9a	2026-03-06 03:23:05.495315	sms.account.activation.message	tr	Kurtuba aktivasyon kodu: 	\N
2a68dc01-7921-4da2-bc9a-e8fedbd40b40	2026-03-06 03:23:05.528476	sms.account.activation.sender	tr	KURTUBA	\N
\.


--
-- Data for Name: registered_client; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.registered_client (access_token_ttl_minutes, cookie_max_age_seconds, refresh_token_enabled, refresh_token_ttl_minutes, scope_enabled, send_token_in_cookie, created_date, client_id, client_name, client_secret, client_type, id, post_logout_redirect_urls, redirect_urls, scopes, auds) FROM stdin;
100	0	f	0	t	f	2025-01-11 17:22:40.629165	e6ce1ca4-b94d-40e5-815b-803d1ac48d8e	adm-service-client	$2a$10$wTdfgE6jNa0GAnaqzUA.tuKzqWbj2YfZEfG/5gZmlL8u1KRYNAJbK	SERVICE	646383c4-467c-4b82-9ee7-9416fe89314b			SERVICE	https://api.kurtuba.app
1	0	t	129600	f	f	2025-01-03 19:04:50.253145	b4e547f9-2c80-4cf0-b46a-d87a918a2280	default-client	\N	DEFAULT	ff123e66-d288-43ae-8032-bd380cfe08d5				https://api.kurtuba.app
300	7776000	t	500	t	t	2025-01-11 17:22:40.600476	419afca3-e02a-4978-a12a-28797ef29f5a	adm-web-client	$2a$10$d47bE0Ij3YWAUcon8IbFmOdgFgJh8.43ZpK4Fdl9m0V0sesnYaLcO	WEB	493695d5-7e5e-4e5d-a950-2d1eb0c9eb4e			TEST,ADMIN,USER	https://api.kurtuba.app
1	0	t	129600	t	f	2025-01-11 17:22:40.540395	96939331-32b9-4089-a121-934de609f5df	kurtuba-mobile-client	\N	MOBILE	5be04a9d-5d46-43f8-aa40-4ffff196e7fc			TEST,ADMIN,USER	https://api.kurtuba.app
300	7776000	t	129600	t	t	2025-01-11 17:22:40.615211	34ff7c95-ac55-4e7c-817e-6aa9333e21f6	kurtuba-web-client	\N	WEB	c742b61b-e6a7-482c-9d70-7116f2050e43			TEST,USER	https://api.kurtuba.app
\.


--
-- Data for Name: role; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.role (id, name) FROM stdin;
a4df29d2-4fc1-4a64-8055-11021d46489d	USER
c2699abe-7bd8-4c71-a771-8f457e808216	SERVICE
4b7ba947-b752-484d-9087-0d81ee7f4c1a	ADMIN
\.


--
-- Data for Name: spring_session; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.spring_session (primary_id, session_id, creation_time, last_access_time, max_inactive_interval, expiry_time, principal_name) FROM stdin;
bb64710e-fcfe-4581-a424-9007af2ee030	3f51a8bb-f36e-4a54-99b5-1be7e6fdb4ea	1774348948748	1774348948748	1800	1774350748748	\N
39eb221b-2f55-4b80-adbd-109b1bb277b6	d1449efd-f56e-4b3e-b627-6117d2ccfc35	1774348948748	1774348948748	1800	1774350748748	\N
d4052e06-6b99-4c66-b6e3-a7915095dff6	38d273fd-34e4-44fd-81fa-138b71ddf329	1774349171413	1774349171414	1800	1774350971414	\N
5bf6b1ef-c821-44e3-b789-01f2df2870a9	a9a08c7a-76da-4ebe-bf58-41594cb1f99a	1774349171413	1774349171414	1800	1774350971414	\N
0bf7de0b-5535-436f-8a2f-1a7cbb8d499c	983197b7-a9ec-44a1-bb5e-f9b094d9462b	1774349377579	1774349377579	1800	1774351177579	\N
7269575d-f581-4b80-8d3d-89878810e8af	0600bda9-5683-46fa-a0c6-985fa34db918	1774349377628	1774349377628	1800	1774351177628	\N
\.


--
-- Data for Name: spring_session_attributes; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.spring_session_attributes (session_primary_id, attribute_name, attribute_bytes) FROM stdin;
\.


--
-- Data for Name: user_fcm_token; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.user_fcm_token (id, fcm_token, registered_client_id, updated_at, user_id, firebase_installation_id) FROM stdin;
\.


--
-- Data for Name: user_meta_change; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.user_meta_change (executed, max_try_count, try_count, created_date, expiration_date, updated_date, code, contact_type, id, link_param, meta, meta_operation_type, user_id) FROM stdin;
\.


--
-- Data for Name: user_role; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.user_role (created_date, id, user_id, role_id) FROM stdin;
2026-01-22 17:12:26	067d8hfe-0kf2-4ad5-b88b-b72d9a52e521	b5a47277-bd17-4883-8cd8-b915bc6e852f	4b7ba947-b752-484d-9087-0d81ee7f4c1a
2025-01-05 20:31:45.377913	037ffdb0-7d25-4a86-b7df-ed11919db65c	b5a47277-bd17-4883-8cd8-b915bc6e852f	a4df29d2-4fc1-4a64-8055-11021d46489d
\.


--
-- Data for Name: user_setting; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.user_setting (id, bio, can_change_username, created_date, profile_cover, profile_pic, localization_available_locale_id, user_id) FROM stdin;
57236c19-b438-4cfd-9451-edcc727e0290	\N	f	2025-01-09 02:45:54.403529	\N	\N	8c649e91-2402-4451-a921-cda38c90e0c7	b5a47277-bd17-4883-8cd8-b915bc6e852f
\.


--
-- Data for Name: user_token; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.user_token (blocked, created_date, expiration_date, refresh_token_exp, auds, client_id, id, jti, refresh_token, scopes, user_id, refresh_token_used) FROM stdin;
\.


--
-- Data for Name: user_token_block; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.user_token_block (created_date, expiration_date, id, jti) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: kurtuba_auth_migrator
--

COPY public.users (activated, email_verified, failed_login_count, locked, mobile_verified, show_captcha, birthdate, created_date, last_login_attempt, name, auth_provider, email, id, mobile, password, surname, username, gender) FROM stdin;
t	t	0	f	t	f	1985-08-25 00:00:00	2025-01-05 20:31:45.349792	2026-03-24 10:49:36.932103	muhlis	KURTUBA	user@user.com	b5a47277-bd17-4883-8cd8-b915bc6e852f	+905366568898	$2a$10$Bb95AqMp1rtSmpYVso4RUOchrkihiVFO/4XJ6xdWxoznfkPucp4Ce	muhlisson	muhlis	MALE
\.


--
-- Name: localization_available_locale localization_available_locale_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.localization_available_locale
    ADD CONSTRAINT localization_available_locale_pkey PRIMARY KEY (id);


--
-- Name: localization_message localization_message_pkey; Type: CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.localization_message
    ADD CONSTRAINT localization_message_pkey PRIMARY KEY (id);


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
-- Name: user_setting fke5l3m1e4mhqe2f2d1ywqe3wko; Type: FK CONSTRAINT; Schema: public; Owner: kurtuba_auth_migrator
--

ALTER TABLE ONLY public.user_setting
    ADD CONSTRAINT fke5l3m1e4mhqe2f2d1ywqe3wko FOREIGN KEY (localization_available_locale_id) REFERENCES public.localization_available_locale(id);


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
-- Name: TABLE localization_available_locale; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.localization_available_locale TO kurtuba_auth_user;


--
-- Name: TABLE localization_message; Type: ACL; Schema: public; Owner: kurtuba_auth_migrator
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.localization_message TO kurtuba_auth_user;


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



