CREATE SEQUENCE IF NOT EXISTS tweets_id_seq;

-- Table Definition
CREATE TABLE "public"."tweets" (
    "id" int4 NOT NULL DEFAULT nextval('tweets_id_seq'::regclass),
    "tweet_text" varchar(255) NOT NULL,
    "tweet_id" uuid NOT NULL,
    "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX tweet_id ON tweets (tweet_id);
