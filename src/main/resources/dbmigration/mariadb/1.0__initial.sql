-- apply changes
create table sregions_regions (
  id                            varchar(40) not null,
  name                          varchar(255),
  world                         varchar(40),
  world_name                    varchar(255),
  region_type                   varchar(8),
  price_type                    varchar(7),
  status                        varchar(8),
  price                         double not null,
  price_multiplier              double not null,
  volume                        bigint not null,
  size                          bigint not null,
  group_identifier              varchar(255),
  owner_id                      varchar(40),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint pk_sregions_regions primary key (id)
);

create table sregions_acl (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  player_id                     varchar(40),
  access_level                  varchar(6),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint pk_sregions_acl primary key (id)
);

create table sregions_region_groups (
  identifier                    varchar(255) not null,
  name                          varchar(255),
  description                   varchar(255),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint pk_sregions_region_groups primary key (identifier)
);

create table sregions_players (
  id                            varchar(40) not null,
  name                          varchar(255),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint pk_sregions_players primary key (id)
);

create table sregions_region_signs (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  world_id                      varchar(40),
  world                         varchar(255),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint pk_sregions_region_signs primary key (id)
);

create table sregions_transactions (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  player_id                     varchar(40),
  action                        varchar(12),
  data                          json,
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
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

