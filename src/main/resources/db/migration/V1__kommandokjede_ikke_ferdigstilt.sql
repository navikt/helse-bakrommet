CREATE TABLE kommandokjede_ikke_ferdigstilt
(
    command_context_id uuid PRIMARY KEY,
    melding_id         uuid      NOT NULL,
    command            VARCHAR   NOT NULL,
    sti                INT[]     NOT NULL,
    opprettet          timestamp NOT NULL
);