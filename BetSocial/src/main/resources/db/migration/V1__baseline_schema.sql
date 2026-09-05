-- Baseline schema for BetSocial.
-- Extracted from the live development database (PostgreSQL 17) with
--   pg_dump --schema-only --no-owner --no-privileges --schema=public
-- This is the authoritative starting point: it captures the real column types,
-- defaults, CHECK constraints, foreign keys and indexes the application relies
-- on, rather than a reconstruction from the entity records.
--
-- NOTE: pre-existing databases are baselined at this version via
-- spring.flyway.baseline-on-migrate=true, so V1 is NOT re-run against them.

-- pg_dump emits CREATE SCHEMA public / COMMENT ON SCHEMA here. Both are
-- omitted: PostgreSQL creates the public schema with every new database, so
-- CREATE SCHEMA fails with 42P06 on a fresh database, and the comment
-- requires ownership of the schema.
CREATE TABLE public.bet_ (
    bid bigint NOT NULL,
    tid bigint NOT NULL,
    status integer,
    outcome boolean,
    amount_for real DEFAULT 0 NOT NULL,
    amount_against real DEFAULT 0 NOT NULL,
    description text,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    deleted_at bigint,
    ends_at bigint,
    is_verified boolean DEFAULT false NOT NULL,
    king_mode boolean DEFAULT false NOT NULL,
    profit_mode boolean DEFAULT false NOT NULL,
    max_amount real DEFAULT 0 NOT NULL,
    min_amount real DEFAULT 0 NOT NULL,
    b_version integer DEFAULT 0,
    CONSTRAINT chk_description CHECK (((description IS NOT NULL) AND (description <> ''::text)))
);
CREATE SEQUENCE public.bet__bid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.bet__bid_seq OWNED BY public.bet_.bid;
CREATE TABLE public.betsave_ (
    bsid bigint NOT NULL,
    bid bigint NOT NULL,
    uid bigint NOT NULL
);
CREATE SEQUENCE public.betsave__bsid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.betsave__bsid_seq OWNED BY public.betsave_.bsid;
CREATE TABLE public.card_ (
    card_id character varying(255) NOT NULL,
    uid bigint NOT NULL,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL
);
CREATE TABLE public.comment_ (
    cid bigint NOT NULL,
    tid bigint NOT NULL,
    uid bigint NOT NULL,
    parent_cid bigint,
    description text,
    likes bigint,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    deleted_at bigint,
    c_version integer DEFAULT 0,
    CONSTRAINT chk_description CHECK (((description IS NOT NULL) AND (description <> ''::text)))
);
CREATE SEQUENCE public.comment__cid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.comment__cid_seq OWNED BY public.comment_.cid;
CREATE TABLE public.commentlike_ (
    clid bigint NOT NULL,
    cid bigint NOT NULL,
    tid bigint NOT NULL,
    uid bigint NOT NULL
);
CREATE SEQUENCE public.commentlike__clid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.commentlike__clid_seq OWNED BY public.commentlike_.clid;
CREATE TABLE public.follow_ (
    fid bigint NOT NULL,
    request_id bigint NOT NULL,
    receive_id bigint NOT NULL
);
CREATE SEQUENCE public.follow__fid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.follow__fid_seq OWNED BY public.follow_.fid;
CREATE TABLE public.group_ (
    gid bigint NOT NULL,
    group_name character varying(100) DEFAULT 'Unamed'::character varying,
    sort integer DEFAULT 0 NOT NULL,
    last_mid bigint,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    deleted_at bigint,
    g_version integer DEFAULT 0,
    CONSTRAINT chk_group_name CHECK (((group_name IS NOT NULL) AND ((group_name)::text <> ''::text)))
);
CREATE SEQUENCE public.group__gid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.group__gid_seq OWNED BY public.group_.gid;
CREATE TABLE public.groupuser_ (
    guid bigint NOT NULL,
    gid bigint NOT NULL,
    uid bigint NOT NULL,
    other_uid bigint,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    last_read_timestamp bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    administrator boolean NOT NULL
);
CREATE SEQUENCE public.groupuser__guid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.groupuser__guid_seq OWNED BY public.groupuser_.guid;
CREATE TABLE public.message_ (
    mid bigint NOT NULL,
    uid bigint NOT NULL,
    recipient_id bigint,
    description text NOT NULL,
    media_type integer DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    deleted_at bigint,
    gid bigint NOT NULL,
    is_read boolean NOT NULL,
    m_version integer DEFAULT 0,
    CONSTRAINT chk_description CHECK (((description IS NOT NULL) AND (description <> ''::text)))
);
CREATE SEQUENCE public.message__mid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.message__mid_seq OWNED BY public.message_.mid;
CREATE TABLE public.notification_ (
    nid bigint NOT NULL,
    uid bigint NOT NULL,
    actor_id bigint,
    notification_type character varying(50) NOT NULL,
    target_id bigint,
    target_type character varying(50),
    title text,
    body text,
    is_read boolean DEFAULT false,
    is_deleted boolean DEFAULT false,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL
);
CREATE SEQUENCE public.notification__nid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.notification__nid_seq OWNED BY public.notification_.nid;
CREATE TABLE public.prediction_ (
    pid bigint NOT NULL,
    bid bigint NOT NULL,
    uid bigint NOT NULL,
    prediction boolean NOT NULL,
    amount_bet real,
    amount_won real,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    deleted_at bigint,
    p_version integer DEFAULT 0
);
CREATE SEQUENCE public.prediction__pid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.prediction__pid_seq OWNED BY public.prediction_.pid;
CREATE TABLE public.thread_ (
    tid bigint NOT NULL,
    uid bigint NOT NULL,
    title character varying(255) NOT NULL,
    media text,
    media_type integer,
    category character varying(100),
    likes bigint DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    deleted_at bigint,
    is_private boolean DEFAULT false NOT NULL,
    t_version integer DEFAULT 0,
    CONSTRAINT chk_title CHECK (((title IS NOT NULL) AND ((title)::text <> ''::text)))
);
CREATE SEQUENCE public.thread__tid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.thread__tid_seq OWNED BY public.thread_.tid;
CREATE TABLE public.threadlike_ (
    tlid bigint NOT NULL,
    tid bigint NOT NULL,
    uid bigint NOT NULL
);
CREATE SEQUENCE public.threadlike__tlid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.threadlike__tlid_seq OWNED BY public.threadlike_.tlid;
CREATE TABLE public.user_ (
    uid bigint NOT NULL,
    user_name character varying(50) NOT NULL,
    email character varying(100) NOT NULL,
    pass_word text NOT NULL,
    phone_number character varying(15) NOT NULL,
    verification_token character varying(255),
    is_verified boolean NOT NULL,
    bio character varying(380) DEFAULT ''::character varying,
    profile_picture text,
    created_at bigint DEFAULT (EXTRACT(epoch FROM CURRENT_TIMESTAMP) * (1000)::numeric) NOT NULL,
    deleted_at bigint,
    user_role integer DEFAULT 0 NOT NULL,
    fb_notification_token text,
    status text DEFAULT 'offline'::text NOT NULL,
    wallet_address text,
    balance real DEFAULT 0 NOT NULL,
    u_version integer DEFAULT 0
);
CREATE SEQUENCE public.user__uid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.user__uid_seq OWNED BY public.user_.uid;
ALTER TABLE ONLY public.bet_ ALTER COLUMN bid SET DEFAULT nextval('public.bet__bid_seq'::regclass);
ALTER TABLE ONLY public.betsave_ ALTER COLUMN bsid SET DEFAULT nextval('public.betsave__bsid_seq'::regclass);
ALTER TABLE ONLY public.comment_ ALTER COLUMN cid SET DEFAULT nextval('public.comment__cid_seq'::regclass);
ALTER TABLE ONLY public.commentlike_ ALTER COLUMN clid SET DEFAULT nextval('public.commentlike__clid_seq'::regclass);
ALTER TABLE ONLY public.follow_ ALTER COLUMN fid SET DEFAULT nextval('public.follow__fid_seq'::regclass);
ALTER TABLE ONLY public.group_ ALTER COLUMN gid SET DEFAULT nextval('public.group__gid_seq'::regclass);
ALTER TABLE ONLY public.groupuser_ ALTER COLUMN guid SET DEFAULT nextval('public.groupuser__guid_seq'::regclass);
ALTER TABLE ONLY public.message_ ALTER COLUMN mid SET DEFAULT nextval('public.message__mid_seq'::regclass);
ALTER TABLE ONLY public.notification_ ALTER COLUMN nid SET DEFAULT nextval('public.notification__nid_seq'::regclass);
ALTER TABLE ONLY public.prediction_ ALTER COLUMN pid SET DEFAULT nextval('public.prediction__pid_seq'::regclass);
ALTER TABLE ONLY public.thread_ ALTER COLUMN tid SET DEFAULT nextval('public.thread__tid_seq'::regclass);
ALTER TABLE ONLY public.threadlike_ ALTER COLUMN tlid SET DEFAULT nextval('public.threadlike__tlid_seq'::regclass);
ALTER TABLE ONLY public.user_ ALTER COLUMN uid SET DEFAULT nextval('public.user__uid_seq'::regclass);
ALTER TABLE ONLY public.bet_
    ADD CONSTRAINT bet__pkey PRIMARY KEY (bid);
ALTER TABLE ONLY public.betsave_
    ADD CONSTRAINT betsave__pkey PRIMARY KEY (bsid);
ALTER TABLE ONLY public.card_
    ADD CONSTRAINT card__pkey PRIMARY KEY (card_id);
ALTER TABLE ONLY public.comment_
    ADD CONSTRAINT comment__pkey PRIMARY KEY (cid);
ALTER TABLE ONLY public.commentlike_
    ADD CONSTRAINT commentlike__pkey PRIMARY KEY (clid);
ALTER TABLE ONLY public.follow_
    ADD CONSTRAINT follow__pkey PRIMARY KEY (fid);
ALTER TABLE ONLY public.group_
    ADD CONSTRAINT group__pkey PRIMARY KEY (gid);
ALTER TABLE ONLY public.groupuser_
    ADD CONSTRAINT groupuser__pkey PRIMARY KEY (guid);
ALTER TABLE ONLY public.message_
    ADD CONSTRAINT message__pkey PRIMARY KEY (mid);
ALTER TABLE ONLY public.notification_
    ADD CONSTRAINT notification__pkey PRIMARY KEY (nid);
ALTER TABLE ONLY public.prediction_
    ADD CONSTRAINT prediction__pkey PRIMARY KEY (pid);
ALTER TABLE ONLY public.thread_
    ADD CONSTRAINT thread__pkey PRIMARY KEY (tid);
ALTER TABLE ONLY public.threadlike_
    ADD CONSTRAINT threadlike__pkey PRIMARY KEY (tlid);
ALTER TABLE ONLY public.user_
    ADD CONSTRAINT user__email_key UNIQUE (email);
ALTER TABLE ONLY public.user_
    ADD CONSTRAINT user__phone_number_key UNIQUE (phone_number);
ALTER TABLE ONLY public.user_
    ADD CONSTRAINT user__pkey PRIMARY KEY (uid);
ALTER TABLE ONLY public.user_
    ADD CONSTRAINT user__user_name_key UNIQUE (user_name);
CREATE INDEX bidx_description ON public.bet_ USING btree (description);
CREATE INDEX bidx_status ON public.bet_ USING btree (status);
CREATE INDEX bidx_tid ON public.bet_ USING btree (tid);
CREATE INDEX bsidx_bid ON public.betsave_ USING btree (bid);
CREATE INDEX bsidx_uid ON public.betsave_ USING btree (uid);
CREATE INDEX card_idx_uid ON public.card_ USING btree (uid);
CREATE INDEX cidx_description ON public.comment_ USING btree (description);
CREATE INDEX cidx_parent_cid ON public.comment_ USING btree (parent_cid);
CREATE INDEX cidx_tid ON public.comment_ USING btree (tid);
CREATE INDEX cidx_uid ON public.comment_ USING btree (uid);
CREATE INDEX clidx_cid ON public.commentlike_ USING btree (cid);
CREATE INDEX clidx_uid ON public.commentlike_ USING btree (uid);
CREATE INDEX fidx_recieve_id ON public.follow_ USING btree (receive_id);
CREATE INDEX fidx_request_id ON public.follow_ USING btree (request_id);
CREATE INDEX guidx_gid ON public.groupuser_ USING btree (gid);
CREATE INDEX guidx_uid ON public.groupuser_ USING btree (uid);
CREATE INDEX idx_notifications_target ON public.notification_ USING btree (target_id, target_type);
CREATE INDEX idx_notifications_type ON public.notification_ USING btree (notification_type);
CREATE INDEX idx_notifications_user_id ON public.notification_ USING btree (uid, is_read);
CREATE INDEX midx_description ON public.message_ USING btree (description);
CREATE INDEX midx_gid ON public.message_ USING btree (gid);
CREATE INDEX midx_recipient_id ON public.message_ USING btree (recipient_id);
CREATE INDEX midx_uid ON public.message_ USING btree (uid);
CREATE INDEX pidx_bid ON public.prediction_ USING btree (bid);
CREATE INDEX pidx_uid ON public.prediction_ USING btree (uid);
CREATE INDEX tidx_category ON public.thread_ USING btree (category);
CREATE INDEX tidx_title ON public.thread_ USING btree (title);
CREATE INDEX tidx_uid ON public.thread_ USING btree (uid);
CREATE INDEX tlidx_tid ON public.threadlike_ USING btree (tid);
CREATE INDEX tlidx_uid ON public.threadlike_ USING btree (uid);
CREATE INDEX uidx_user_name ON public.user_ USING btree (user_name);
CREATE INDEX uidx_verification_token ON public.user_ USING btree (verification_token);
ALTER TABLE ONLY public.notification_
    ADD CONSTRAINT fk_actor_id FOREIGN KEY (actor_id) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.prediction_
    ADD CONSTRAINT fk_bid FOREIGN KEY (bid) REFERENCES public.bet_(bid) ON DELETE CASCADE;
ALTER TABLE ONLY public.betsave_
    ADD CONSTRAINT fk_bid FOREIGN KEY (bid) REFERENCES public.bet_(bid) ON DELETE CASCADE;
ALTER TABLE ONLY public.commentlike_
    ADD CONSTRAINT fk_cid FOREIGN KEY (cid) REFERENCES public.comment_(cid) ON DELETE CASCADE;
ALTER TABLE ONLY public.groupuser_
    ADD CONSTRAINT fk_gid FOREIGN KEY (gid) REFERENCES public.group_(gid) ON DELETE CASCADE;
ALTER TABLE ONLY public.message_
    ADD CONSTRAINT fk_gid FOREIGN KEY (gid) REFERENCES public.group_(gid) ON DELETE CASCADE;
ALTER TABLE ONLY public.comment_
    ADD CONSTRAINT fk_parent_cid FOREIGN KEY (parent_cid) REFERENCES public.comment_(cid) ON DELETE CASCADE;
ALTER TABLE ONLY public.follow_
    ADD CONSTRAINT fk_receive_id FOREIGN KEY (receive_id) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.message_
    ADD CONSTRAINT fk_recipient_id FOREIGN KEY (recipient_id) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.follow_
    ADD CONSTRAINT fk_request_id FOREIGN KEY (request_id) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.bet_
    ADD CONSTRAINT fk_tid FOREIGN KEY (tid) REFERENCES public.thread_(tid) ON DELETE CASCADE;
ALTER TABLE ONLY public.comment_
    ADD CONSTRAINT fk_tid FOREIGN KEY (tid) REFERENCES public.thread_(tid) ON DELETE CASCADE;
ALTER TABLE ONLY public.threadlike_
    ADD CONSTRAINT fk_tid FOREIGN KEY (tid) REFERENCES public.thread_(tid) ON DELETE CASCADE;
ALTER TABLE ONLY public.commentlike_
    ADD CONSTRAINT fk_tid FOREIGN KEY (tid) REFERENCES public.thread_(tid) ON DELETE CASCADE;
ALTER TABLE ONLY public.thread_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.prediction_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.groupuser_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.message_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.comment_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.betsave_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.threadlike_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.commentlike_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.card_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
ALTER TABLE ONLY public.notification_
    ADD CONSTRAINT fk_uid FOREIGN KEY (uid) REFERENCES public.user_(uid) ON DELETE CASCADE;
