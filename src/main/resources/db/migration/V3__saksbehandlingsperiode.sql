drop table behandling;

create table saksbehandlingsperiode (
    id uuid not null primary key,
    spillerom_personid text not null references ident (spillerom_id),
    opprettet timestamp with time zone not null,
    opprettet_av_nav_ident text not null,
    opprettet_av_navn text not null,
    fom date not null,
    tom date not null
);
