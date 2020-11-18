-- apply changes
create table sregions_regions (
  id                            uuid not null,
  group_id                      uuid,
  world_guard_region            varchar(255),
  owner_id                      uuid,
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
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
  id                            uuid not null,
  identifier                    varchar(255),
  name                          varchar(255),
  description                   varchar(255),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_region_groups primary key (id)
);

create table sregions_players (
  id                            uuid not null,
  name                          varchar(255),
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_players primary key (id)
);

create table sregions_transactions (
  id                            uuid not null,
  region_id                     uuid,
  version                       bigint not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_transactions primary key (id)
);

create index ix_sregions_regions_group_id on sregions_regions (group_id);
alter table sregions_regions add constraint fk_sregions_regions_group_id foreign key (group_id) references sregions_region_groups (id) on delete restrict on update restrict;

create index ix_sregions_regions_owner_id on sregions_regions (owner_id);
alter table sregions_regions add constraint fk_sregions_regions_owner_id foreign key (owner_id) references sregions_players (id) on delete restrict on update restrict;

create index ix_sregions_acl_region_id on sregions_acl (region_id);
alter table sregions_acl add constraint fk_sregions_acl_region_id foreign key (region_id) references sregions_regions (id) on delete restrict on update restrict;

create index ix_sregions_acl_player_id on sregions_acl (player_id);
alter table sregions_acl add constraint fk_sregions_acl_player_id foreign key (player_id) references sregions_players (id) on delete restrict on update restrict;

create index ix_sregions_transactions_region_id on sregions_transactions (region_id);
alter table sregions_transactions add constraint fk_sregions_transactions_region_id foreign key (region_id) references sregions_regions (id) on delete restrict on update restrict;

