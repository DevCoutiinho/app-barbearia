CREATE TABLE user
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    avatar      VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    telephone   VARCHAR(11) NOT NULL,
    created_at  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE role
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE CHECK(name IN('USER', 'ADMIN', 'BARBER')),
    description VARCHAR(255)
);

CREATE TABLE permission
(
    id              UUID PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    description     VARCHAR(255)
);

CREATE TABLE user_role
(
    PRIMARY KEY (user_id, role_id),
    user_id     UUID NOT NULL,
    role_id     UUID NOT NULL,

    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
            REFERENCES user (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
            REFERENCES role (id)
            ON DELETE CASCADE
);

CREATE TABLE role_permission
(
    PRIMARY KEY (role_id, permission_id),
    permission_id   UUID NOT NULL,
    role_id         UUID NOT NULL,

    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id)
            REFERENCES role (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id)
            REFERENCES permission (id)
            ON DELETE CASCADE
);

CREATE TABLE notification
(
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    title               VARCHAR(50) NOT NULL,
    message             VARCHAR(255) NOT NULL,
    type                VARCHAR(50) NOT NULL CHECK(type IN('NEW_APPOINTMENT', 'APPOINTMENT_CANCELED', 'APPOINTMENT_REMINDER', 'LOW_STOCK', 'SYSTEM')),
    read                BOOLEAN NOT NULL,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_user_notification
        FOREIGN KEY (user_id)
            REFERENCES user (id)
            ON DELETE CASCADE
);

CREATE TABLE service
(
    id              UUID PRIMARY KEY,
    barber_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    type_haircut    VARCHAR(50) NOT NULL CHECK(type_haircut IN('HAIR_CLIPPERS', 'SCISSORS', 'MIXED')),
    price           NUMERIC(10,2) NOT NULL,
    active          BOOLEAN NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    time_service    INT NOT NULL,

    CONSTRAINT fk_service_user
        FOREIGN KEY (barber_id)
            REFERENCES user (id)
            ON DELETE RESTRICT
);

CREATE TABLE scheduling
(
    id              UUID PRIMARY KEY,
    barber_id       UUID NOT NULL,
    client_id       UUID NOT NULL,
    service_id      UUID NOT NULL,
    data_time       TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status          VARCHAR(50) NOT NULL CHECK(status IN('APPOINTMENT_CANCELED', 'APOINTMENT_SCHEDULED', 'SCHEDULED','PENDING_CONFIRMATION')),
    final_price     NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_scheduling_barber
        FOREIGN KEY (barber_id)
            REFERENCES user (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_scheduling_client
        FOREIGN KEY (client_id)
            REFERENCES user (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_scheduling_service
        FOREIGN KEY (service_id)
            REFERENCES service (id)
            ON DELETE RESTRICT
);

CREATE TABLE barber_availability
(
    id                  UUID PRIMARY KEY,
    barber_id           UUDI,
    day_week            INT NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    active              BOOEAN NOT NULL,
    start_pause         TIME NOT NULL,
    end_pause           TIME NOT NULL,

    CONSTRAINT fk_availability_barber
        FOREIGN KEY (barber_id)
            REFERENCES user (id)
            ON DELETE CASCADE
);

CREATE TABLE product
(
    id                  UUID PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(255),
    minimum_quantity    NUMERIC(10,2) NOT NULL,
    current_quantity    NUMERIC(10,2) NOT NULL,
    unit_measurement    VARCHAR(50) NOT NULL CHECK(unit_measurement IN('UN', 'G', 'ML', 'L')),
    price_cost          NUMERIC(10,2) NOT NULL,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    active              BOOLEAN NOT NULL
);

CREATE TABLE inventory_movement
(
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    product_id          UUID NOT NULL,
    quantity            NUMERIC(10,2) NOT NULL,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    type                VARCHAR(50) NOT NULL CHECK(type IN('CONSUMPTION', 'ENTRY', 'DISPOSAL')),

    CONSTRAINT fk_inventory_movement_user
        FOREIGN KEY (user_id)
            REFERENCES user (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_movement_product
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE RESTRICT
);