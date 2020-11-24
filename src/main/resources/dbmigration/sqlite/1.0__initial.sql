-- apply changes
create table rcregions_regions (
  id                            varchar(40) not null,
  name                          varchar(255),
  world                         varchar(40),
  world_name                    varchar(255),
  region_type                   varchar(8),
  price_type                    varchar(7),
  status                        varchar(8),
  price                         double not null,
  price_multiplier              double not null,
  volume                        integer not null,
  size                          integer not null,
  group_identifier              varchar(255),
  owner_id                      varchar(40),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_rcregions_regions_region_type check ( region_type in ('SELL','RENT','CONTRACT','HOTEL')),
  constraint ck_rcregions_regions_price_type check ( price_type in ('FREE','STATIC','DYNAMIC')),
  constraint ck_rcregions_regions_status check ( status in ('FREE','OCCUPIED','ABADONED')),
  constraint pk_rcregions_regions primary key (id),
  foreign key (group_identifier) references rcregions_region_groups (identifier) on delete restrict on update restrict,
  foreign key (owner_id) references rcregions_players (id) on delete restrict on update restrict
);

create table rcregions_acl (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  player_id                     varchar(40),
  access_level                  varchar(6),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_rcregions_acl_access_level check ( access_level in ('OWNER','MEMBER','GUEST')),
  constraint pk_rcregions_acl primary key (id),
  foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict,
  foreign key (player_id) references rcregions_players (id) on delete restrict on update restrict
);

create table rcregions_region_groups (
  identifier                    varchar(255) not null,
  name                          varchar(255),
  description                   varchar(255),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_rcregions_region_groups primary key (identifier)
);

create table rcregions_players (
  id                            varchar(40) not null,
  name                          varchar(255),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_rcregions_players primary key (id)
);

create table rcregions_region_signs (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  x                             integer not null,
  y                             integer not null,
  z                             integer not null,
  world_id                      varchar(40),
  world                         varchar(255),
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint pk_rcregions_region_signs primary key (id),
  foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict
);

create table rcregions_transactions (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  player_id                     varchar(40),
  action                        varchar(12),
  data                          clob,
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_rcregions_transactions_action check ( action in ('SELL','BUY','CHANGE_OWNER')),
  constraint pk_rcregions_transactions primary key (id),
  foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict,
  foreign key (player_id) references rcregions_players (id) on delete restrict on update restrict
);

