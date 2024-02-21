CREATE TABLE IF NOT EXISTS raw_statistics(
    homeserver VARCHAR(255) NOT NULL,
    "timestamp" timestamp NOT NULL,
    total_users INT NOT NULL,
    monthly_active_users INT NOT NULL,
    PRIMARY KEY ("homeserver")
);
