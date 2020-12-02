-- apply changes
alter table rcregions_region_groups add column sell_modifier double default 1.0 not null;

alter table rcregions_transactions modify action varchar(14);
