alter table behandling drop constraint behandling_bakrom_person_id_fkey;

alter table ident
    alter column bakrom_person_id type char(5);
alter table behandling
    alter column bakrom_person_id type char(5);

alter table ident
rename column bakrom_person_id to spillerom_id;
alter table behandling
rename column bakrom_person_id to spillerom_id;

alter table behandling add constraint behandling_bakrom_person_id_fkey
    foreign key (spillerom_id) references ident(spillerom_id)


