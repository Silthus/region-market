-- apply changes
alter table rcregions_regions drop constraint if exists ck_rcregions_regions_status;
alter table rcregions_regions alter column status type varchar(15) using status::varchar(15);
alter table rcregions_regions add constraint ck_rcregions_regions_status check ( status in ('FREE','OCCUPIED','ABADONED','FOR_DIRECT_SALE'));
