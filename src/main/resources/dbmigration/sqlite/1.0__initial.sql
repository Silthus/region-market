-- apply changes
create table sregions_regions (
  id                            varchar(40) not null,
  group_id                      varchar(40),
  world_guard_region            varchar(255),
  owner_id                      varchar(40),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_regions primary key (id),
  foreign key (group_id) references sregions_region_groups (id) on delete restrict on update restrict,
  foreign key (owner_id) references sregions_players (id) on delete restrict on update restrict
);

create table sregions_acl (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  player_id                     varchar(40),
  access_level                  varchar(6),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_sregions_acl_access_level check ( access_level in ('OWNER','MEMBER','GUEST')),
  constraint pk_sregions_acl primary key (id),
  foreign key (region_id) references sregions_regions (id) on delete restrict on update restrict,
  foreign key (player_id) references sregions_players (id) on delete restrict on update restrict
);

create table sregions_region_groups (
  id                            varchar(40) not null,
  identifier                    varchar(255),
  name                          varchar(255),
  description                   varchar(255),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_region_groups primary key (id)
);

create table sregions_players (
  id                            varchar(40) not null,
  name                          varchar(255),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_players primary key (id)
);

create table sregions_transactions (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_sregions_transactions primary key (id),
  foreign key (region_id) references sregions_regions (id) on delete restrict on update restrict
);

