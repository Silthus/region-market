-- apply changes
create table rcregions_owned_regions (
  id                            uuid not null,
  region_id                     uuid,
  player_id                     uuid,
  end                           timestamptz,
  version                       bigint not null,
  when_created                  timestamptz not null,
  when_modified                 timestamptz not null,
  start                         timestamptz not null,
  constraint pk_rcregions_owned_regions primary key (id)
);

alter table rcregions_players add column last_online timestamptz;

create index ix_rcregions_owned_regions_region_id on rcregions_owned_regions (region_id);
alter table rcregions_owned_regions add constraint fk_rcregions_owned_regions_region_id foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict;

create index ix_rcregions_owned_regions_player_id on rcregions_owned_regions (player_id);
alter table rcregions_owned_regions add constraint fk_rcregions_owned_regions_player_id foreign key (player_id) references rcregions_players (id) on delete restrict on update restrict;

