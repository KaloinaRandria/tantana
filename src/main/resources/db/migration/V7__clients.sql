-- =====================================================================
-- V7 : clients (phase 2 du module Ventes)
--   type PARTICULIER  : NIF / STAT facultatifs
--   type PROFESSIONNEL: NIF et STAT obligatoires (mentions des factures)
--   plafond_credit    : montant TTC maximal restant du par le client
--                       (0 = paiement comptant uniquement). Utilise en phase 4.
--   comptoir = TRUE   : client generique des ventes anonymes (un seul,
--                       ni desactivable ni transformable).
-- =====================================================================

CREATE TABLE client (
    id             BIGSERIAL      PRIMARY KEY,
    nom            VARCHAR(150)   NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    telephone      VARCHAR(50),
    email          VARCHAR(150),
    adresse        VARCHAR(255),
    nif            VARCHAR(50),
    stat           VARCHAR(50),
    plafond_credit NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (plafond_credit >= 0),
    observations   TEXT,
    comptoir       BOOLEAN        NOT NULL DEFAULT FALSE,
    actif          BOOLEAN        NOT NULL DEFAULT TRUE,
    date_creation  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_client_pro CHECK (type <> 'PROFESSIONNEL' OR (nif IS NOT NULL AND stat IS NOT NULL)),
    CONSTRAINT ck_client_comptoir_actif CHECK (NOT comptoir OR actif)
);

-- Un NIF ne peut appartenir qu'a un seul client
CREATE UNIQUE INDEX uk_client_nif ON client (nif) WHERE nif IS NOT NULL;
-- Un seul client comptoir
CREATE UNIQUE INDEX uk_client_comptoir ON client (comptoir) WHERE comptoir;
CREATE INDEX idx_client_nom ON client (lower(nom));

INSERT INTO client (nom, type, comptoir, observations)
VALUES ('Client comptoir', 'PARTICULIER', TRUE, 'Client générique pour les ventes au comptoir sans nom.');
