-- apply changes
create table rcregions_sales (
  id                            uuid not null,
  region_id                     uuid,
  seller_id                     uuid,
  price                         float not null,
  type                          varchar(7),
  buyer_id                      uuid,
  start                         timestamptz,
  expires                       timestamptz,
  end                           timestamptz,
  acknowledged                  timestamptz,
  version                       bigint not null,
  when_created                  timestamptz not null,
  when_modified                 timestamptz not null,
  constraint ck_rcregions_sales_type check ( type in ('DIRECT','AUCTION','SERVER')),
  constraint pk_rcregions_sales primary key (id)
);

alter table rcregions_regions drop constraint if exists ck_rcregions_regions_status;
alter table rcregions_regions add constraint ck_rcregions_regions_status check ( status in ('FREE','OCCUPIED','ABADONED','FOR_SALE'));
alter table rcregions_transactions drop constraint if exists ck_rcregions_transactions_action;
alter table rcregions_transactions add constraint ck_rcregions_transactions_action check ( action in ('SELL_TO_SERVER','SELL_TO_PLAYER','BUY','CHANGE_OWNER','SAVE_SCHEMATIC'));
create index ix_rcregions_sales_region_id on rcregions_sales (region_id);
alter table rcregions_sales add constraint fk_rcregions_sales_region_id foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict;

create index ix_rcregions_sales_seller_id on rcregions_sales (seller_id);
alter table rcregions_sales add constraint fk_rcregions_sales_seller_id foreign key (seller_id) references rcregions_players (id) on delete restrict on update restrict;

create index ix_rcregions_sales_buyer_id on rcregions_sales (buyer_id);
alter table rcregions_sales add constraint fk_rcregions_sales_buyer_id foreign key (buyer_id) references rcregions_players (id) on delete restrict on update restrict;

