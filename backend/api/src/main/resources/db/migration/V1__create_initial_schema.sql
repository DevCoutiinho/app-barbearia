CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    avatar     VARCHAR(255),
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    telephone  VARCHAR(11)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE roles
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE CHECK (name IN ('USER', 'ADMIN', 'BARBER')),
    description VARCHAR(255)
);

CREATE TABLE permissions
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE users_roles
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,

    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
            REFERENCES roles (id)
            ON DELETE CASCADE,

    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE roles_permissions
(
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,

    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id)
            REFERENCES roles (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id)
            REFERENCES permissions (id)
            ON DELETE CASCADE,

    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE notifications
(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL,
    title      VARCHAR(50)  NOT NULL,
    message    VARCHAR(255) NOT NULL,
    type       VARCHAR(50)  NOT NULL CHECK (type IN ('NEW_APPOINTMENT', 'APPOINTMENT_CANCELED', 'APPOINTMENT_REMINDER',
                                                     'LOW_STOCK', 'SYSTEM')),
    read       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_notification
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE TABLE service_categories
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE services
(
    id            UUID PRIMARY KEY,
    barber_id     UUID           NOT NULL,
    name          VARCHAR(255)   NOT NULL,
    category_id   UUID           NOT NULL,
    technique     VARCHAR(50) CHECK (technique IN
                                     ('MACHINE', 'SCISSORS', 'RAZOR', 'MIXED', 'TWEEZERS', 'WAX')),
    price         NUMERIC(10, 2) NOT NULL,
    active        BOOLEAN        NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_time INT            NOT NULL,

    CONSTRAINT fk_service_barber
        FOREIGN KEY (barber_id)
            REFERENCES users (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_service_category
        FOREIGN KEY (category_id)
            REFERENCES service_categories (id)
            ON DELETE RESTRICT
);

CREATE TABLE schedulings
(
    id           UUID PRIMARY KEY,
    barber_id    UUID           NOT NULL,
    client_id    UUID           NOT NULL,
    service_id   UUID           NOT NULL,
    scheduled_at TIMESTAMPTZ    NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status       VARCHAR(50)    NOT NULL CHECK (status IN
                                                ('CANCELED', 'COMPLETED', 'SCHEDULED', 'PENDING')),
    final_price  NUMERIC(10, 2) NOT NULL,

    CONSTRAINT fk_scheduling_barber
        FOREIGN KEY (barber_id)
            REFERENCES users (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_scheduling_client
        FOREIGN KEY (client_id)
            REFERENCES users (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_scheduling_service
        FOREIGN KEY (service_id)
            REFERENCES services (id)
            ON DELETE RESTRICT
);

CREATE TABLE availabilities
(
    id          UUID PRIMARY KEY,
    barber_id   UUID    NOT NULL,
    day_week    INT     NOT NULL,
    start_time  TIME    NOT NULL,
    end_time    TIME    NOT NULL,
    active      BOOLEAN NOT NULL,
    start_pause TIME    NOT NULL,
    end_pause   TIME    NOT NULL,

    CONSTRAINT fk_availability_barber
        FOREIGN KEY (barber_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE TABLE products
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(100)   NOT NULL,
    description      VARCHAR(255),
    minimum_quantity NUMERIC(10, 2) NOT NULL,
    current_quantity NUMERIC(10, 2) NOT NULL,
    unit_measurement VARCHAR(50)    NOT NULL CHECK (unit_measurement IN ('UN', 'G', 'ML', 'L')),
    price_cost       NUMERIC(10, 2) NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active           BOOLEAN        NOT NULL
);

CREATE TABLE inventory_movements
(
    id         UUID PRIMARY KEY,
    user_id    UUID           NOT NULL,
    product_id UUID           NOT NULL,
    quantity   NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type       VARCHAR(50)    NOT NULL CHECK (type IN ('CONSUMPTION', 'ENTRY', 'DISPOSE')),

    CONSTRAINT fk_inventory_movement_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_movement_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE RESTRICT
);

CREATE TABLE service_images
(
    id         UUID PRIMARY KEY,
    service_id UUID         NOT NULL,
    url        VARCHAR(512) NOT NULL,
    alt_text   VARCHAR(255),
    is_cover   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_image_service
        FOREIGN KEY (service_id) REFERENCES services (id) ON DELETE CASCADE
);