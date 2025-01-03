-- CREATE USER kurtubauser WITH PASSWORD '6kOroUVNmFqcp3I'
-- CREATE SCHEMA IF NOT EXISTS kurtuba AUTHORIZATION kurtubauser;
-- grant usage, create on schema kurtuba to kurtubauser;

create table kurtuba.spring_session
(
    primary_id            char(36) not null
        constraint spring_session_pk
            primary key,
    session_id            char(36) not null,
    creation_time         bigint   not null,
    last_access_time      bigint   not null,
    max_inactive_interval integer  not null,
    expiry_time           bigint   not null,
    principal_name        varchar(100)
);

alter table kurtuba.spring_session
    owner to kurtubauser;

create unique index spring_session_ix1
    on kurtuba.spring_session (session_id);

create index spring_session_ix2
    on kurtuba.spring_session (expiry_time);

create index spring_session_ix3
    on kurtuba.spring_session (principal_name);

create table kurtuba.spring_session_attributes
(
    session_primary_id char(36)     not null
        constraint spring_session_attributes_fk
            references kurtuba.spring_session
            on delete cascade,
    attribute_name     varchar(200) not null,
    attribute_bytes    bytea        not null,
    constraint spring_session_attributes_pk
        primary key (session_primary_id, attribute_name)
);

alter table kurtuba.spring_session_attributes
    owner to kurtubauser;

create table kurtuba.email_job
(
    id              varchar(255)      not null
        primary key,
    state           varchar(255)      not null,
    mail_type       varchar(255)      not null,
    send_after_date timestamp(6)      not null,
    max_try_count   integer           not null,
    try_count       integer           not null,
    error           varchar(255),
    sender          varchar(255)      not null,
    recipient       varchar(255)      not null,
    subject         varchar(255)      not null,
    message         varchar(10485760) not null,
    created_date    timestamp(6)      not null,
    updated_date    timestamp(6)
);

alter table kurtuba.email_job
    owner to kurtubauser;

create table kurtuba.registered_client
(
    id                        varchar(255) not null
        primary key,
    client_id                 varchar(255) not null
        unique,
    client_name               varchar(255) not null
        unique,
    client_secret             varchar(255),
    client_type               varchar(255) not null,
    access_token_ttl_minutes  integer      not null,
    refresh_token_enabled     boolean      not null,
    refresh_token_ttl_minutes integer      not null,
    send_token_in_cookie      boolean      not null,
    cookie_max_age_seconds    integer      not null,
    scope_enabled             boolean      not null,
    scopes                    varchar(255),
    post_logout_redirect_urls varchar(255),
    redirect_urls             varchar(255),
    created_date              timestamp(6)
);

alter table kurtuba.registered_client
    owner to kurtubauser;

create table kurtuba."user"
(
    id                  varchar(255) not null
        primary key,
    email               varchar(255) not null,
    username            varchar(255) not null,
    password            varchar(255) not null,
    email_verified      boolean      not null,
    phone               varchar(255),
    phone_verified      boolean      not null,
    activated           boolean      not null,
    failed_login_count  integer      not null,
    locked              boolean      not null,
    show_captcha        boolean      not null,
    last_login_attempt  timestamp(6),
    auth_provider       varchar(255) not null,
    can_change_username boolean      not null,
    name                varchar(100) not null,
    surname             varchar(255),
    birthdate           timestamp(6),
    bio                 varchar(255),
    profile_cover       varchar(255),
    profile_pic         varchar(255),
    created_date        timestamp(6) not null


);

alter table kurtuba."user"
    owner to kurtubauser;

create table kurtuba.user_meta_change
(
    id               varchar(255) not null
        primary key,
    user_id          varchar(255) not null,
    meta_change_type varchar(255) not null,
    meta             varchar(255),
    code             varchar(255),
    link_param       varchar(255),
    expiration_date  timestamp(6),
    executed         boolean      not null,
    max_try_count    integer,
    try_count        integer,
    created_date     timestamp(6),
    updated_date     timestamp(6)
);

alter table kurtuba.user_meta_change
    owner to kurtubauser;

create table kurtuba.user_role
(
    created_date timestamp(6) not null,
    id           varchar(255) not null
        primary key,
    role         varchar(255) not null,
    user_id      varchar(255) not null
        constraint fk859n2jvi8ivhui0rl0esws6o
            references kurtuba."user"
);

alter table kurtuba.user_role
    owner to kurtubauser;

create table kurtuba.user_token
(
    id                varchar(255) not null
        primary key,
    user_id           varchar(255) not null,
    jti               varchar(255) not null,
    expiration_date   timestamp(6) not null,
    refresh_token     varchar(255) not null,
    refresh_token_exp timestamp(6) not null,
    client_id         varchar(255) not null,
    blocked           boolean      not null,
    aud               varchar(255) not null,
    scopes            varchar(255),
    created_date      timestamp(6) not null
);

alter table kurtuba.user_token
    owner to kurtubauser;

create table kurtuba.user_token_block
(
    id              varchar(255) not null
        primary key,
    jti             varchar(255) not null
        unique
        constraint fkgxmr26jabfodgbs3r7pllsuws
            references kurtuba.user_token,
    expiration_date timestamp(6),
    created_date    timestamp(6) not null
);

alter table kurtuba.user_token_block
    owner to kurtubauser;

