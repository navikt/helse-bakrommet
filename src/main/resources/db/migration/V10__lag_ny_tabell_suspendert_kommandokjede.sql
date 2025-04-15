CREATE TABLE suspendert_kommandokjede
(
    command_context_id                          UUID PRIMARY KEY,
    command                                     VARCHAR   NOT NULL,
    første_tidspunkt                            TIMESTAMP NOT NULL,
    siste_tidspunkt                             TIMESTAMP NOT NULL,
    siste_melding_id                            UUID      NOT NULL,
    totalt_antall_ganger_påminnet               INTEGER   NOT NULL,

    sist_suspenderte_sti                        VARCHAR   NOT NULL,
    sist_suspenderte_sti_første_tidspunkt       TIMESTAMP NOT NULL,
    sist_suspenderte_sti_antall_ganger_påminnet INTEGER   NOT NULL
);
