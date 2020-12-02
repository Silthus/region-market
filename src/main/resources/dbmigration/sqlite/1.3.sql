-- apply changes
alter table rcregions_regions add column owner_id varchar(40);

alter table rcregions_region_groups add column world varchar(255);
alter table rcregions_region_groups add column world_guard_region varchar(255);

create index ix_rcregions_regions_owner_id on rcregions_regions (owner_id);

