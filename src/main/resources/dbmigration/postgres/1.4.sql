-- apply changes
alter table rcregions_region_groups add column sell_modifier float default 1.0 not null;

alter table rcregions_transactions drop constraint if exists ck_rcregions_transactions_action;
alter table rcregions_transactions alter column action type varchar(14) using action::varchar(14);
alter table rcregions_transactions add constraint ck_rcregions_transactions_action check ( action in ('SELL','BUY','CHANGE_OWNER','SAVE_SCHEMATIC'));
