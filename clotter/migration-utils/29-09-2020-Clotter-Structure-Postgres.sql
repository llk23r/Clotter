CREATE SEQUENCE IF NOT EXISTS tweets_id_seq;

ALTER SEQUENCE tweets_id_seq RESTART WITH 1;

-- Table Definition
CREATE TABLE "public"."tweets" (
    "id" int4 NOT NULL DEFAULT nextval('tweets_id_seq'::regclass),
    "tweet_text" text NOT NULL,
    "tweet_id" varchar NOT NULL,
    "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "user_name" varchar NOT NULL,
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX tweet_id ON tweets (tweet_id);