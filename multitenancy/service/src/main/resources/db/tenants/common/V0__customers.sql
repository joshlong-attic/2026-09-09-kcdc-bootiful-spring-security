create table customer
(
    id                serial primary key,
    name              text not null,
    tenant_identifier text not null
);