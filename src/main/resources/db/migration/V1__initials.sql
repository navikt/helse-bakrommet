create table ident (
    bakrom_person_id uuid not null unique,
    naturlig_ident varchar not null unique
);

create table behandling (
    bakrom_person_id uuid not null references ident (bakrom_person_id),
    påbegynt timestamptz not null,
    fom date not null,
    tom date not null
);
