-- apply changes
alter table rcregions_regions add column owner_id varchar(40);

alter table rcregions_region_groups add column world varchar(255);
alter table rcregions_region_groups add column world_guard_region varchar(255);

create index ix_rcregions_regions_owner_id on rcregions_regions (owner_id);
alter table rcregions_regions add constraint fk_rcregions_regions_owner_id foreign key (owner_id) references rcregions_players (id) on delete restrict on update restrict;

