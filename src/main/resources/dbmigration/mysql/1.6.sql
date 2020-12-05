-- apply changes
create table rcregions_sales (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  seller_id                     varchar(40),
  price                         double not null,
  type                          varchar(7),
  buyer_id                      varchar(40),
  start                         datetime(6),
  expires                       datetime(6),
  end                           datetime(6),
  acknowledged                  datetime(6),
  version                       bigint not null,
  when_created                  datetime(6) not null,
  when_modified                 datetime(6) not null,
  constraint pk_rcregions_sales primary key (id)
);

create index ix_rcregions_sales_region_id on rcregions_sales (region_id);
alter table rcregions_sales add constraint fk_rcregions_sales_region_id foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict;

create index ix_rcregions_sales_seller_id on rcregions_sales (seller_id);
alter table rcregions_sales add constraint fk_rcregions_sales_seller_id foreign key (seller_id) references rcregions_players (id) on delete restrict on update restrict;

create index ix_rcregions_sales_buyer_id on rcregions_sales (buyer_id);
alter table rcregions_sales add constraint fk_rcregions_sales_buyer_id foreign key (buyer_id) references rcregions_players (id) on delete restrict on update restrict;

