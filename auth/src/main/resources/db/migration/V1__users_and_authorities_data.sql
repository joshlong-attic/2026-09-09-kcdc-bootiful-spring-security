delete
from authorities;
delete
from users;

insert into users (username, password, enabled)
values ('josh', '{bcrypt}$2a$10$i4a5jw4y9LnKS//8DNDVYe2VfC6Jr2m0Ds1j5un5JNhnkJxg6jAT.', true);

insert into users (username, password, enabled)
values ('rob', '{sha256}0a598ce65440a7c4ad18b48fdc2cf28c4e58aec803780f59548535696a23b16624e47ff4f59e6f39', true);


insert into authorities (username, authority)
values ('josh', 'ROLE_USER');

insert into authorities (username, authority)
values ('josh', 'ROLE_ADMIN');

insert into authorities (username, authority)
values ('rob', 'ROLE_EDITOR');

insert into authorities (username, authority)
values ('rob', 'ROLE_USER');