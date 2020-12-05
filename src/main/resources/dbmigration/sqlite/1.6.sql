-- apply changes
create table rcregions_sales (
  id                            varchar(40) not null,
  region_id                     varchar(40),
  seller_id                     varchar(40),
  price                         double not null,
  type                          varchar(7),
  buyer_id                      varchar(40),
  start                         timestamp,
  expires                       timestamp,
  end                           timestamp,
  acknowledged                  timestamp,
  version                       integer not null,
  when_created                  timestamp not null,
  when_modified                 timestamp not null,
  constraint ck_rcregions_sales_type check ( type in ('DIRECT','AUCTION','SERVER')),
  constraint pk_rcregions_sales primary key (id),
  foreign key (region_id) references rcregions_regions (id) on delete restrict on update restrict,
  foreign key (seller_id) references rcregions_players (id) on delete restrict on update restrict,
  foreign key (buyer_id) references rcregions_players (id) on delete restrict on update restrict
);

alter table rcregions_regions drop constraint if exists ck_rcregions_regions_status;
alter table rcregions_regions add constraint ck_rcregions_regions_status check ( status in ('FREE','OCCUPIED','ABADONED','FOR_SALE'));
alter table rcregions_transactions drop constraint if exists ck_rcregions_transactions_action;
alter table rcregions_transactions add constraint ck_rcregions_transactions_action check ( action in ('SELL_TO_SERVER','SELL_TO_PLAYER','BUY','CHANGE_OWNER','SAVE_SCHEMATIC'));
