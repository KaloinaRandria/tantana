-- =====================================================================
-- V9 : phase 4 du module Ventes
--   paiement    : encaissements d'une facture (plusieurs possibles).
--                 Une erreur se corrige par annulation tracee, jamais par suppression.
--   avoir       : AV-AAAA-NNNN, annule tout ou partie d'une facture
--                 (retour de marchandise ou geste commercial).
--   ligne_avoir : quantites creditees par ligne de facture, avec ou sans retour en stock.
--
-- Reste a payer d'une facture = total TTC - avoirs TTC - paiements non annules.
-- =====================================================================

CREATE TABLE paiement (
    id             BIGSERIAL      PRIMARY KEY,
    vente_id       BIGINT         NOT NULL REFERENCES vente (id),
    date_paiement  DATE           NOT NULL,
    montant        NUMERIC(18, 2) NOT NULL CHECK (montant > 0),
    mode           VARCHAR(20)    NOT NULL,
    reference      VARCHAR(100),
    saisi_par_id   BIGINT         NOT NULL REFERENCES utilisateur (id),
    date_saisie    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    annule         BOOLEAN        NOT NULL DEFAULT FALSE,
    motif_annulation VARCHAR(255),
    annule_par_id  BIGINT         REFERENCES utilisateur (id),
    date_annulation TIMESTAMP,
    CONSTRAINT ck_paiement_annulation CHECK (NOT annule OR (motif_annulation IS NOT NULL AND annule_par_id IS NOT NULL))
);

CREATE INDEX idx_paiement_vente ON paiement (vente_id);
CREATE INDEX idx_paiement_date  ON paiement (date_paiement);

CREATE TABLE avoir (
    id                 BIGSERIAL      PRIMARY KEY,
    numero             VARCHAR(20)    NOT NULL UNIQUE,
    vente_id           BIGINT         NOT NULL REFERENCES vente (id),
    date_avoir         DATE           NOT NULL,
    motif              VARCHAR(255)   NOT NULL,
    taux_tva           NUMERIC(5, 2)  NOT NULL,
    total_ht           NUMERIC(18, 2) NOT NULL,
    total_tva          NUMERIC(18, 2) NOT NULL,
    total_ttc          NUMERIC(18, 2) NOT NULL,
    -- cout de revient des quantites creditees (pour corriger la marge)
    cout_revient_total NUMERIC(18, 2) NOT NULL,
    saisi_par_id       BIGINT         NOT NULL REFERENCES utilisateur (id),
    date_creation      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_avoir_vente ON avoir (vente_id);

CREATE TABLE ligne_avoir (
    id             BIGSERIAL      PRIMARY KEY,
    avoir_id       BIGINT         NOT NULL REFERENCES avoir (id) ON DELETE CASCADE,
    ligne_vente_id BIGINT         NOT NULL REFERENCES ligne_vente (id),
    quantite       NUMERIC(15, 3) NOT NULL CHECK (quantite > 0),
    montant_ht     NUMERIC(18, 2) NOT NULL,
    retour_stock   BOOLEAN        NOT NULL
);

-- Cumuls sur la facture (mis a jour par l'application, dans la meme transaction)
ALTER TABLE vente ADD COLUMN montant_avoirs NUMERIC(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE ligne_vente ADD COLUMN quantite_avoir NUMERIC(15, 3) NOT NULL DEFAULT 0;
ALTER TABLE ligne_vente ADD CONSTRAINT ck_ligne_vente_avoir CHECK (quantite_avoir >= 0 AND quantite_avoir <= quantite);

-- Les retours en stock referencent l'avoir
ALTER TABLE mouvement_stock ADD COLUMN avoir_id BIGINT REFERENCES avoir (id);
