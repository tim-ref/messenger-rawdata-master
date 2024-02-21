CREATE TABLE IF NOT EXISTS raw_data(
    "uuid" uuid NOT NULL,
    "start" timestamp NOT NULL,
    duration_in_ms INT NOT NULL,
    "finish" timestamp NOT NULL,
    operation VARCHAR(255) NOT NULL,
    status VARCHAR(5) NOT NULL,
    message text NOT NULL,
    PRIMARY KEY ("uuid")
);

CREATE TABLE IF NOT EXISTS raw_data_report(
    "uuid" uuid NOT NULL,
    "start" timestamp NOT NULL,
    "finish" timestamp NOT NULL,
    "delivered" boolean NOT NULL,
    PRIMARY KEY ("uuid")
);

CREATE TABLE IF NOT EXISTS statistics(
    reference_date timestamp NOT NULL,
    messenger_services INT NOT NULL,
    registered_users INT NOT NULL,
    active_users INT NOT NULL,
    PRIMARY KEY (reference_date)
);
