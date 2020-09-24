-- -------------------------------------------------------------
-- Database: clotter
-- Generation Time: 2020-09-24 11:17:09.4740
-- -------------------------------------------------------------

CREATE TABLE "tweets" (
    "id" integer auto increment,
    "tweet_text" varchar(255) NOT NULL,
    "tweet_id" uuid NOT NULL,
    "created_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX tweet_id ON tweets (tweet_id);
