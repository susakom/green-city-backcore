CREATE DATABASE greencity;
CREATE DATABASE greencityubs;

create user greencity with password 'greencity';
alter user greencity with superuser;
GRANT ALL PRIVILEGES ON DATABASE greencity TO greencity;
