-- apply changes
create table rcregions_regions (
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
  constraint ck_rcregions_regions_region_type check ( region_type in ('SELL','RENT','CONTRACT','HOTEL')),
  constraint ck_rcregions_regions_price_type check ( price_type in ('FREE','STATIC','DYNAMIC')),
  constraint ck_rcregions_regions_status check ( status in ('FREE','OCCUPIED','ABADONED')),
  constraint pk_rcregions_regions primary key (id)
);

create table rcregions_acl (
  id                            uuid not null,
  region_id                     uuid,
  player_id                     uuid,
  access_level                  varchar(6),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_rcregions_acl_access_level check ( access_level in ('OWNER','MEMBER','GUEST')),
  constraint pk_rcregions_acl primary key (id)
);

create table rcregions_region_groups (
  identifier                    varchar(255) not null,
  name                          varchar(255),
  description                   varchar(255),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_rcregions_region_groups primary key (identifier)
);

create table rcregions_players (
  id                            uuid not null,
  name                          varchar(255),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_rcregions_players primary key (id)
);

create table rcregions_region_signs (
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
  constraint pk_rcregions_region_signs primary key (id)
);

create table rcregions_transactions (
  id                            uuid not null,
  region_id                     uuid,
  player_id                     uuid,
  action                        varchar(12),
  data                          clob,
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_rcregions_transactions_action check ( action in ('SELL','BUY','CHANGE_OWNER')),
  constraint pk_rcregions_transactions primary key (id)
);

create index ix_rcregions_regions_group_identifier on rcregions_regions (group_identifier);
alter table rcregions_regions add constraint fk_rcregions_regions_group_identifier foreign key (group_identifier) references rcregions_region_groups (identifier) on delete restrict on update restrict;

create index ix_rcregions_regions_owner_id on rcregions_regions (owner_id);
alter table rcregions_regions add constraint fk_rcregions_regions_owner_id foreign key (owner_id) references rcregions_players (id) on delete restrict on update restrict;

create index ix_rcregions_acl_region_id on rcregions_acl (region_id);
alter table rcregions_acl add constraint fk_rcregions_acl_region_id foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict;

create index ix_rcregions_acl_player_id on rcregions_acl (player_id);
alter table rcregions_acl add constraint fk_rcregions_acl_player_id foreign key (player_id) references rcregions_players (id) on delete restrict on update restrict;

create index ix_rcregions_region_signs_region_id on rcregions_region_signs (region_id);
alter table rcregions_region_signs add constraint fk_rcregions_region_signs_region_id foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict;

create index ix_rcregions_transactions_region_id on rcregions_transactions (region_id);
alter table rcregions_transactions add constraint fk_rcregions_transactions_region_id foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict;

create index ix_rcregions_transactions_player_id on rcregions_transactions (player_id);
alter table rcregions_transactions add constraint fk_rcregions_transactions_player_id foreign key (player_id) references rcregions_players (id) on delete restrict on update restrict;

