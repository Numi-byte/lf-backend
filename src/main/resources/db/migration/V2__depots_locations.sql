-- depots
create table if not exists depots (
                                      id   uuid primary key,
                                      name text not null
);

-- locations (digital grid)
create table if not exists locations (
                                         id       uuid primary key,
                                         depot_id uuid not null references depots(id) on delete cascade,
    zone     text not null,
    row_no   int  not null,
    col_no   int  not null,
    bin      text,
    type     text not null check (type in ('shelf','locker'))
    );

-- make the grid unique per depot (zone,row,col,bin with bin treated as '')
create unique index if not exists uk_locations_grid
    on locations (depot_id, zone, row_no, col_no, (coalesce(bin, '')));

-- items: add state + current_location
alter table items add column if not exists state text not null default 'in_intake';
alter table items add column if not exists current_location_id uuid;
alter table items add constraint fk_items_current_location
    foreign key (current_location_id) references locations(id);

create index if not exists idx_items_state on items(state);
