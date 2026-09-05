CREATE TABLE user
(
    user_id     UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    avatar      VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    telephone   VARCHAR(11) NOT NULL,
    created_on  TIMESTAMPTZ NOT NULL,
    updated_on  TIMESTAMPTZ NOT NULL
);

CREATE TABLE role
(
    role_id     UUID PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE CHECK(name IN('USER', 'ADMIN', 'BARBER')),
    description VARCHAR(255)
);

CREATE TABLE permission
(
    permission_id   UUID PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    description     VARCHAR(255)
);

CREATE TABLE user_role
(
    user_id     UUID NOT NULL,
    role_id     UUID NOT NULL,

    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
            REFERENCES user (user_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id)
            REFERENCES role (role_id)
            ON DELETE CASCADE
);

CREATE TABLE role_permission
(
    permission_id   UUID NOT NULL,
    role_id         UUID NOT NULL,

    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id)
            REFERENCES role (role_id)
            ON DELETE CASCADE,

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id)
            REFERENCES permission (permission_id)
            ON DELETE CASCADE
);

CREATE TABLE notification
(
    notification_id     UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    title               VARCHAR(50) NOT NULL,
    message            VARCHAR(255) NOT NULL,
    type                ENUM('agendamento_novo', 'agendamento_cancelado', 'agendamento_lembrete', 'estoque_baixo', 'sistema'),
    read                BOOLEAN NOT NULL,
    created_on          TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_user_notification
        FOREIGN KEY (user_id)
            REFERENCES permission (permission_id)
            ON DELETE CASCADE
);

CREATE TABLE service
(
    service_id      UUID PRIMARY KEY,
    barber_id       UUID NOT NULL,
    description     VARCHAR(255) NOT NULL,
    price           NUMERIC(10,2) NOT NULL,
    active          BOOLEAN NOT NULL,
    updated_on      TIMESTAMPTZ NOT NULL,
    created_on      TIMESTAMPTZ NOT NULL,
    time_service    INT NOT NULL,

    CONSTRAINT fk_service_user
        FOREIGN KEY (barber_id)
            REFERENCES user (user_id)
            ON DELETE RESTRICT
);

CREATE TABLE scheduling
(
    scheduling_id   UUID PRIMARY KEY,
    barber_id       UUID NOT NULL,
    client_id       UUID NOT NULL,
    service_id      UUID NOT NULL,
    data_time       TIMESTAMPTZ NOT NULL,
    created_on      TIMESTAMPTZ NOT NULL,
    updated_on      TIMESTAMPTZ NOT NULL,
    status          ENUM('Agendamento_cancelado', 'Agendameto_concluido', 'Agendado','Pendente_confirmacao'),
    final_price     NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_scheduling_barber
        FOREIGN KEY (barber_id)
            REFERENCES user (user_id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_scheduling_client
        FOREIGN KEY (client_id)
            REFERENCES user (user_id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_scheduling_barber
        FOREIGN KEY (service_id)
            REFERENCES service (servce_id)
            ON DELETE RESTRICT
);

CREATE TABLE barber_availability
(
    availability_id     UUID PRIMARY KEY,
    barber_id           UUDI NOT NULL,
    day_week            INT NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    active              BOOEAN NOT NULL,
    start_pause         TIMESTAMPTZ NOT NULL,
    end_pause           TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_availability_barber
        FOREIGN KEY (barber_id)
            REFERENCES user (user_id)
            ON DELETE CASCADE
);

CREATE TABLE product
(
    product_id          UUID PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(255),
    minimum_quantity    INT NOT NULL,
    current_quantity    INT NOT NULL,
    price_cost          NUMERIC(10,2) NOT NULL,
    updated_on          TIMESTAMPTZ NOT NUL
);

CREATE TABLE inventory_movement
(
    inventory_movement  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    product_id          UUID NOT NULL,
    quantity            INT NOT NULL,
    created_on          TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_inventory_movement_user
        FOREIGN KEY (user_id)
            REFERENCES user (user_id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_movement_product
        FOREIGN KEY (product_id)
            REFERENCES product (product_id)
            ON DELETE RESTRICT
);