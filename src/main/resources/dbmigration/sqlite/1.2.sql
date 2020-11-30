-- apply changes
create table rcregions_owned_regions (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  player_id                     varchar(40),
  end                           timestamp,
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  start                         timestamp not null,
  constraint pk_rcregions_owned_regions primary key (id),
  foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict,
  foreign key (player_id) references rcregions_players (id) on delete restrict on update restrict
);

alter table rcregions_players add column last_online timestamp;

