-- apply changes
alter table rcregions_region_groups add column price_type varchar(7);
alter table rcregions_region_groups add constraint ck_rcregions_region_groups_price_type check ( price_type in ('FREE','STATIC','DYNAMIC'));

