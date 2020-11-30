-- apply changes
create table rcregions_owned_regions (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  player_id                     varchar(40),
  end                           datetime(6),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  start                         datetime(6) not null,
  constraint pk_rcregions_owned_regions primary key (id)
);

alter table rcregions_players add column last_online datetime(6);

create index ix_rcregions_owned_regions_region_id on rcregions_owned_regions (region_id);
alter table rcregions_owned_regions add constraint fk_rcregions_owned_regions_region_id foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict;

create index ix_rcregions_owned_regions_player_id on rcregions_owned_regions (player_id);
alter table rcregions_owned_regions add constraint fk_rcregions_owned_regions_player_id foreign key (player_id) references rcregions_players (id) on delete restrict on update restrict;

