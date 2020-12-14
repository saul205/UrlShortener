-- Clean database

DROP TABLE CLICK IF EXISTS;
DROP TABLE HISTORIAL IF EXISTS;
DROP TABLE SHORTURL IF EXISTS;
DROP TABLE CONTADOR IF EXISTS;
DROP TABLE MOSTVISITED IF EXISTS;

-- ShortURL

CREATE TABLE SHORTURL
(
    HASH    VARCHAR(30) PRIMARY KEY, -- Key
    TARGET  VARCHAR(1024),           -- Original URLcc
    SPONSOR VARCHAR(1024),           -- Sponsor URL
    CREATED TIMESTAMP,               -- Creation date
    OWNER   VARCHAR(255),            -- User id
    MODE    INTEGER,                 -- Redirect mode
    SAFE    INTEGER,                 -- Safe target
    QR      VARCHAR(255),            -- QR resource
    ALCANZABLE INTEGER,               -- Alcanzable
    IP      VARCHAR(20),             -- IP
    COUNTRY VARCHAR(50),             -- Country
    URI     VARCHAR(1024)
);

-- Click

CREATE TABLE CLICK
(
    ID       BIGINT IDENTITY,                                             -- KEY
    HASH     VARCHAR(10) NOT NULL FOREIGN KEY REFERENCES SHORTURL (HASH), -- Foreing key
    CREATED  TIMESTAMP,                                                   -- Creation date
    REFERRER VARCHAR(1024),                                               -- Traffic origin
    BROWSER  VARCHAR(50),                                                 -- Browser
    PLATFORM VARCHAR(50),                                                 -- Platform
    IP       VARCHAR(20),                                                 -- IP
    COUNTRY  VARCHAR(50)                                                  -- Country
);

-- History

CREATE TABLE HISTORIAL
(
    ID      BIGINT IDENTITY,
    HASH    VARCHAR(10) NOT NULL FOREIGN KEY REFERENCES SHORTURL (HASH),
    TARGET  VARCHAR(1024),
    CREATED TIMESTAMP
);

CREATE TABLE CONTADOR
(
    HASH VARCHAR(10) PRIMARY KEY,
    TARGET VARCHAR(1024),
    COUNTS BIGINT
);

CREATE TABLE MOSTVISITED
(
    HASH VARCHAR(10) PRIMARY KEY,
    TARGET VARCHAR(1024),
    COUNTS BIGINT
);


