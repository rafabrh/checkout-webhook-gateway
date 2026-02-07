create table if not exists orders (
                                      id bigserial primary key,
                                      order_id varchar(64) not null unique,
    plan varchar(64) not null,
    channel varchar(32) not null,
    instance varchar(64),
    remote_jid varchar(64) not null,
    customer_name varchar(140),
    status varchar(32) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
    );

create table if not exists payments (
                                        id bigserial primary key,
                                        payment_id varchar(64) not null unique,
    order_id varchar(64) not null,
    status varchar(32) not null,
    amount numeric(18,2) not null,
    created_at timestamptz not null
    );

create index if not exists ix_payments_order_id on payments(order_id);

create table if not exists pairing_sessions (
                                                id bigserial primary key,
                                                order_id varchar(64) not null unique,
    token varchar(64) not null unique,
    status varchar(32) not null,
    qr_url text,
    qr_base64 text,
    instance_name varchar(120),
    created_at timestamptz not null,
    updated_at timestamptz not null
    );