create table if not exists users
(
    username text    not null primary key,
    password text    not null,
    enabled  boolean not null
);

create table if not exists authorities
(
    username  text not null references users (username),
    authority text not null
);

create unique index if not exists index_auth_username ON authorities USING btree (username, authority);

create table if not exists tenant_details
(
    id         serial primary key,
    identifier text not null unique default pg_catalog.gen_random_uuid(),
    enabled    boolean              default true,
    created_at timestamp            default now()
);

create table if not exists tenant_details_attributes
(
    tenant_id       integer not null references tenant_details (id),
    attribute_name  text    not null,
    attribute_value text    not null,
    primary key (tenant_id, attribute_name)
);

create table if not exists users_tenant_details
(
    users_username            text not null references users (username),
    tenant_details_identifier text not null references tenant_details (identifier),
    primary key (users_username, tenant_details_identifier)
);

insert into tenant_details (identifier) values ('tenant2'), ('tenant1'), ('tenant0');

insert into tenant_details_attributes (tenant_id, attribute_name, attribute_value)
select id, 'tier', 'xl' from tenant_details
where identifier = 'tenant0' on conflict (tenant_id, attribute_name) do update set attribute_value = excluded.attribute_value;

insert into tenant_details_attributes (tenant_id, attribute_name, attribute_value)
select id, 'region', 'us-east-1' from tenant_details
where identifier = 'tenant1' on conflict (tenant_id, attribute_name) do update set attribute_value = excluded.attribute_value;

insert into tenant_details_attributes (tenant_id, attribute_name, attribute_value)
select id, 'tier', 's' from tenant_details
where identifier = 'tenant1' on conflict (tenant_id, attribute_name) do update set attribute_value = excluded.attribute_value;

insert into tenant_details_attributes (tenant_id, attribute_name, attribute_value)
select id, 'region', 'apj-west-2'
from tenant_details
where identifier = 'tenant0'
on conflict (tenant_id, attribute_name) do update set attribute_value = excluded.attribute_value;

insert into tenant_details_attributes (tenant_id, attribute_name, attribute_value)
select id, 'tier', 's'
from tenant_details
where identifier = 'tenant2'
on conflict (tenant_id, attribute_name) do update set attribute_value = excluded.attribute_value;

insert into tenant_details_attributes (tenant_id, attribute_name, attribute_value)
select id, 'region', 'eu-west-1'
from tenant_details
where identifier = 'tenant2'
on conflict (tenant_id, attribute_name) do update set attribute_value = excluded.attribute_value;



insert into users (username, password, enabled) values ('catherine', '{bcrypt}$2a$10$H20/wTYHzZDRkEMaXycDUOvITjbzdJ/ngwVPNQWsYA3QgbQl.j/6G', true);
insert into users (username, password, enabled) values ('josh', '{sha256}ed1a0e0cc973b58b6d5e2c04a646f213193943fc505aa3ffe50bde28f7cf20b4b67b1fbf6c911552', true);
insert into users (username, password, enabled) values ('mala', '{bcrypt}$2a$10$JSLA2uTlXq6kInaXVyuTV.bmZvz8zRLhGrSzoLOCmDTNDu5ZyA6jG', true);
insert into users (username, password, enabled) values ('trisha', '{sha256}4f7a8586bd3ad985420caeaf20c6c557322d517117efeff63528afbfb277243b5dec5997fba43bee', true);
insert into users (username, password, enabled) values ('james', '{sha256}a9934a3090622d5d72a3e8c5bf6e506684a91923f4609be83a7c1f73c37f8905964aac7bb5174093', true);
insert into users (username, password, enabled) values ('rob', '{bcrypt}$2a$10$P.ZwEJrwC3iCz79IMTL6V.m9AJt93YfqvzJZI24o9S7pYgCiqY0KK', true);
insert into users (username, password, enabled) values ('dashaun', '{bcrypt}$2a$10$emGMOOymUg/x0HAlgF2oA.nys/pSndIk89535xQQM0b/WT3TFl92S', true);
insert into authorities (username, authority) values ('catherine', 'ROLE_USER');
insert into authorities (username, authority) values ('josh', 'ROLE_USER');
insert into authorities (username, authority) values ('mala', 'ROLE_USER');
insert into authorities (username, authority) values ('trisha', 'ROLE_USER');
insert into authorities (username, authority) values ('james', 'ROLE_USER');
insert into authorities (username, authority) values ('rob', 'ROLE_USER');
insert into authorities (username, authority) values ('dashaun', 'ROLE_USER');


insert into users_tenant_details (users_username, tenant_details_identifier) values ('catherine', 'tenant0');
insert into users_tenant_details (users_username, tenant_details_identifier) values ('josh', 'tenant2');
insert into users_tenant_details (users_username, tenant_details_identifier) values ('mala', 'tenant1');
insert into users_tenant_details (users_username, tenant_details_identifier) values ('trisha', 'tenant0');
insert into users_tenant_details (users_username, tenant_details_identifier) values ('james', 'tenant1');
insert into users_tenant_details (users_username, tenant_details_identifier) values ('rob', 'tenant2');
insert into users_tenant_details (users_username, tenant_details_identifier) values ('dashaun', 'tenant0');