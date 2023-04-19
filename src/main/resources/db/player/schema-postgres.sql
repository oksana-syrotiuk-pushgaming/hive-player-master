-----------------------------------------------
-- t_player
-- Player from a particular iGP
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_player (
	player_id      VARCHAR(250)  NOT NULL,
	igp_code       VARCHAR(12)   NOT NULL,
	guest          BOOLEAN       NOT NULL,
	ccy_code       VARCHAR(6)    NOT NULL,
	username       VARCHAR(64),
	alias          VARCHAR(64),
	country        VARCHAR(128),
	lang           VARCHAR(4)    NOT NULL,
	PRIMARY KEY(player_id,igp_code, guest)
);
CREATE INDEX IF NOT EXISTS t_player_idx_igp_code ON t_player(igp_code);

-----------------------------------------------
-- t_session
-- Player session
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_session (
	session_id          VARCHAR(64)   PRIMARY KEY,
	creation_time       BIGINT        NOT NULL,
	last_accessed_time  BIGINT        NOT NULL,
	player_id           VARCHAR(250),
	igp_code            VARCHAR(12),
	access_token        VARCHAR(256),
	game_code           VARCHAR(64),
	lang                VARCHAR(4),
	jurisdiction        VARCHAR(8),
	mode                VARCHAR(4),
	ccy_code            VARCHAR(6)    NOT NULL,
	balance             DECIMAL(16,2),
	ip_address          VARCHAR(15),
	user_agent          VARCHAR(256),
    status              VARCHAR(8),
    reason              VARCHAR(256),
    session_token       VARCHAR(64),
    auth_token          VARCHAR(256),
    guest_token         VARCHAR(256),
    launch_referrer     VARCHAR(256),
    client_type         VARCHAR(8),
    region              VARCHAR(8),
    country             VARCHAR(8),
	authenticated       BOOLEAN       NOT NULL,
	token_used          BOOLEAN
);
CREATE INDEX IF NOT EXISTS t_session_idx_accesstime ON t_session(last_accessed_time);
CREATE INDEX IF NOT EXISTS t_session_idx_status ON t_session(status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS t_session_idx_player_id_igp_code_status ON t_session(player_id,igp_code,status);

-----------------------------------------------
-- t_txn
-- Primary Gaming transactions
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_txn (
	txn_id          VARCHAR(128)             PRIMARY KEY,
	game_code       VARCHAR(64)              NOT NULL,
	play_id         VARCHAR(128)             NOT NULL,
	play_complete   BOOLEAN                  NOT NULL,
	play_complete_if_cancelled BOOLEAN       NOT NULL,
	round_id        VARCHAR(128)             NOT NULL,
	round_complete  BOOLEAN,
	round_complete_if_cancelled BOOLEAN      NOT NULL,
	player_id       VARCHAR(250)             NOT NULL,
	igp_code        VARCHAR(12)              NOT NULL,
	access_token    VARCHAR(256),
	session_id      VARCHAR(64)              NOT NULL,
	mode            VARCHAR(4)               NOT NULL,
	guest           BOOLEAN                  NOT NULL,
	ccy_code        VARCHAR(6)               NOT NULL,
	type            VARCHAR(8)               NOT NULL,
	amount          NUMERIC(16,2)            NOT NULL,
	jackpot_amount  NUMERIC(16,2)            		 ,
	txn_ts          TIMESTAMP WITH TIME ZONE NOT NULL,
	cancel_ts       TIMESTAMP WITH TIME ZONE,
	txn_ref         VARCHAR(128),
	play_ref        VARCHAR(128),
	status          VARCHAR(10)              NOT NULL,
	balance         DECIMAL(16,2),
	exception       VARCHAR(50),
	retry           INT                      NOT NULL DEFAULT 0,
	txn_events      TEXT,
	extra_info      TEXT,
	FOREIGN KEY(player_id,igp_code, guest) REFERENCES t_player,
	CONSTRAINT amount_c1 CHECK (amount >= 0.00),
	CONSTRAINT jackpot_amount_c1 CHECK (jackpot_amount >= 0.00)
);
CREATE INDEX IF NOT EXISTS t_txn_idx_txn_ts ON t_txn(txn_ts);
CREATE INDEX IF NOT EXISTS t_txn_idx_access_token ON t_txn(access_token);
CREATE INDEX IF NOT EXISTS t_txn_idx_fk2 ON t_txn(player_id, igp_code, guest);
CREATE INDEX IF NOT EXISTS t_txn_idx_status_active ON t_txn(status) WHERE status in ('PENDING','RECON','CANCELLING');
CREATE INDEX IF NOT EXISTS t_txn_idx_play_id ON t_txn(play_id);

-----------------------------------------------
-- t_txn_callback_q
-- Gaming transactions awaiting callback to game
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_txn_callback_q(
	txn_id      VARCHAR(128)             PRIMARY KEY,
	game_code   VARCHAR(64)              NOT NULL,
	txn_status  VARCHAR(9)               NOT NULL,
	retries     INT                      NOT NULL DEFAULT 0,
	exception   VARCHAR(50)
);

-----------------------------------------------
-- t_txn_audit
-- Audit trail of actions on txns performed by back office Administrator
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_txn_audit (
	txn_id      VARCHAR(128)                 PRIMARY KEY,
	action_ts   TIMESTAMP WITH TIME ZONE     NOT NULL,
	action      VARCHAR(20)                  NOT NULL
);

-----------------------------------------------
-- t_play
-- Audit trail of play actions at game level
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_play (
    play_id         VARCHAR(128)             PRIMARY KEY NOT NULL,
    player_id       VARCHAR(250)             NOT NULL,
    status          VARCHAR(12)              NOT NULL,
    mode            VARCHAR(4)               NOT NULL,
    game_code       VARCHAR(64)              NOT NULL,
    guest           BOOLEAN                  NOT NULL,
    auto_completed  BOOLEAN                  ,
    ccy_code        VARCHAR(6)               NOT NULL,
    igp_code        VARCHAR(12)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    stake           NUMERIC(16,2)            NOT NULL,
    win             NUMERIC(16,2)            NOT NULL,
    num_txns        INT                      NOT NULL,
    play_ref        VARCHAR(128),
    bonus_fund_type VARCHAR(8),
    session_id      VARCHAR(64),
    cleardown       NUMERIC(16,2),
    FOREIGN KEY(player_id,igp_code,guest) REFERENCES t_player
);
CREATE INDEX t_play_idx_created_at ON t_play(created_at);
CREATE INDEX t_play_idx_modified_at ON t_play(modified_at);
CREATE INDEX t_play_idx_status_active ON t_play(status) WHERE status = 'ACTIVE';
CREATE INDEX t_play_idx_game_code ON t_play(game_code);
CREATE INDEX t_play_idx_player_id ON t_play(player_id);


-----------------------------------------------
-- t_autocomplete_request_q
-- Autocomplete requests awaiting callback to game
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_autocomplete_request_q(
	play_id    VARCHAR(128)             NOT NULL PRIMARY KEY,
	game_code  VARCHAR(64)              NOT NULL,
	session_id VARCHAR(128)             NOT NULL,
	guest      BOOLEAN                  NOT NULL,
	retries    INT                      NOT NULL DEFAULT 0,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now() at time zone 'utc'),
	exception  VARCHAR(50)
);

-----------------------------------------------
-- t_bigwin
-- Lookup table for Big Win amounts (as min win amount) per ccy code
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_bigwin(
	ccy_code VARCHAR(6)    NOT NULL PRIMARY KEY,
	min_win  NUMERIC(16,2) NOT NULL DEFAULT 0
);

-----------------------------------------------
-- t_txn_cleardown
-- Transaction cleardown information
-----------------------------------------------
CREATE TABLE IF NOT EXISTS t_txn_cleardown (
     txn_id           VARCHAR(128) PRIMARY KEY NOT NULL,
     txn_ts           TIMESTAMP WITH TIME ZONE NOT NULL,
     cleardown_txn_id VARCHAR(128)             NOT NULL,
     amount           NUMERIC(16,2)            NOT NULL
);
CREATE INDEX IF NOT EXISTS t_txn_cleardown_idx_txn_ts ON t_txn_cleardown(txn_ts);
CREATE UNIQUE INDEX IF NOT EXISTS t_txn_cleardown_idx_cleardown_txn_id ON t_txn_cleardown(cleardown_txn_id);
