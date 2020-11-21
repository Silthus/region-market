-- apply changes
create table sregions_regions (
  id                            uuid not null,
  name                          varchar(255),
  world                         uuid,
  world_name                    varchar(255),
  region_type                   varchar(8),
  price_type                    varchar(7),
  status                        varchar(8),
  price                         double not null,
  price_multiplier              double not null,
  volume                        bigint not null,
  size                          bigint not null,
  group_identifier              varchar(255),
  owner_id                      uuid,
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_sregions_regions_region_type check ( region_type in ('SELL','RENT','CONTRACT','HOTEL')),
  constraint ck_sregions_regions_price_type check ( price_type in ('FREE','STATIC','DYNAMIC')),
  constraint ck_sregions_regions_status check ( status in ('FREE','OCCUPIED','ABADONED')),
  constraint pk_sregions_regions primary key (id)
);

create table sregions_acl (
  id                            uuid not null,
  region_id                     uuid,
  player_id                     uuid,
  access_level                  varchar(6),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_sregions_acl_access_level check ( access_level in ('OWNER','MEMBER','GUEST')),
  constraint pk_sregions_acl primary key (id)
);

create table sregions_region_groups (
  identifier                    varchar(255) not null,
  name                          varchar(255),
  description                   varchar(255),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_region_groups primary key (identifier)
);

create table sregions_players (
  id                            uuid not null,
  name                          varchar(255),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_players primary key (id)
);

create table sregions_region_signs (
  id                            uuid not null,
  region_id                     uuid,
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  world_id                      uuid,
  world                         varchar(255),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_region_signs primary key (id)
);

create table sregions_transactions (
  id                            uuid not null,
  region_id                     uuid,
  player_id                     uuid,
  action                        varchar(12),
  data                          clob,
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_sregions_transactions_action check ( action in ('SELL','BUY','CHANGE_OWNER')),
  constraint pk_sregions_transactions primary key (id)
);

create index ix_sregions_regions_group_identifier on sregions_regions (group_identifier);
alter table sregions_regions add constraint fk_sregions_regions_group_identifier foreign key (group_identifier) references sregions_region_groups (identifier) on delete restrict on update restrict;

create index ix_sregions_regions_owner_id on sregions_regions (owner_id);
alter table sregions_regions add constraint fk_sregions_regions_owner_id foreign key (owner_id) references sregions_players (id) on delete restrict on update restrict;

create index ix_sregions_acl_region_id on sregions_acl (region_id);
alter table sregions_acl add constraint fk_sregions_acl_region_id foreign key (region_id) references sregions_regions (id) on delete restrict on update restrict;

create index ix_sregions_acl_player_id on sregions_acl (player_id);
alter table sregions_acl add constraint fk_sregions_acl_player_id foreign key (player_id) references sregions_players (id) on delete restrict on update restrict;

create index ix_sregions_region_signs_region_id on sregions_region_signs (region_id);
alter table sregions_region_signs add constraint fk_sregions_region_signs_region_id foreign key (region_id) references sregions_regions (id) on delete restrict on update restrict;

create index ix_sregions_transactions_region_id on sregions_transactions (region_id);
alter table sregions_transactions add constraint fk_sregions_transactions_region_id foreign key (region_id) references sregions_regions (id) on delete restrict on update restrict;

create index ix_sregions_transactions_player_id on sregions_transactions (player_id);
alter table sregions_transactions add constraint fk_sregions_transactions_player_id foreign key (player_id) references sregions_players (id) on delete restrict on update restrict;

