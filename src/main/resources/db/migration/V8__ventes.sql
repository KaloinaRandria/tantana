-- =====================================================================
-- V8 : ventes (phase 3 du module Ventes)
--
-- Cycle : BROUILLON --(vendeur valide)--> VALIDEE (facture FV-AAAA-NNNN)
--                    \--> A_VALIDER si remise > plafond ou prix < revient
--                              --(CHECKER approuve)--> VALIDEE
--                              --(CHECKER refuse)----> BROUILLON
--
-- Bonnes pratiques appliquees :
--  - numero de facture attribue a la validation seulement (suite continue)
--  - prix et couts figes sur chaque ligne au moment de la vente
--  - une facture validee ne se modifie ni ne se supprime (avoir en phase 4)
-- =====================================================================

CREATE TABLE vente (
    id                   BIGSERIAL      PRIMARY KEY,
    -- NULL tant que la vente n'est pas validee
    numero               VARCHAR(20)    UNIQUE,
    client_id            BIGINT         NOT NULL REFERENCES client (id),
    date_vente           DATE           NOT NULL DEFAULT CURRENT_DATE,
    statut               VARCHAR(20)    NOT NULL,
    taux_tva             NUMERIC(5, 2)  NOT NULL,
    total_brut_ht        NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_remise         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_ht             NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_tva            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_ttc            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    -- figes a la validation
    cout_revient_total   NUMERIC(18, 2),
    marge_totale         NUMERIC(18, 2),
    -- paiements (phase 4)
    montant_paye         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    observations         TEXT,
    -- pourquoi la vente demande un controle, et la reponse du controleur
    motif_controle       TEXT,
    commentaire_controle TEXT,
    vendeur_id           BIGINT         NOT NULL REFERENCES utilisateur (id),
    controleur_id        BIGINT         REFERENCES utilisateur (id),
    date_creation        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_controle        TIMESTAMP,
    date_validation      TIMESTAMP,
    version              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_vente_numero CHECK ((statut = 'VALIDEE') = (numero IS NOT NULL)),
    CONSTRAINT ck_vente_controleur CHECK (controleur_id IS NULL OR controleur_id <> vendeur_id)
);

CREATE INDEX idx_vente_client ON vente (client_id);
CREATE INDEX idx_vente_statut ON vente (statut);
CREATE INDEX idx_vente_date   ON vente (date_vente);

CREATE TABLE ligne_vente (
    id                    BIGSERIAL      PRIMARY KEY,
    vente_id              BIGINT         NOT NULL REFERENCES vente (id) ON DELETE CASCADE,
    article_id            BIGINT         NOT NULL REFERENCES article (id),
    quantite              NUMERIC(15, 3) NOT NULL CHECK (quantite > 0),
    -- prix catalogue HT au moment de la saisie
    prix_catalogue_ht     NUMERIC(18, 2) NOT NULL CHECK (prix_catalogue_ht >= 0),
    remise_pct            NUMERIC(5, 2)  NOT NULL DEFAULT 0 CHECK (remise_pct >= 0 AND remise_pct <= 100),
    prix_unitaire_ht      NUMERIC(18, 2) NOT NULL,
    montant_ht            NUMERIC(18, 2) NOT NULL,
    -- couts figes a la validation (CMUP du moment)
    cout_achat_unitaire   NUMERIC(18, 4),
    cout_revient_unitaire NUMERIC(18, 4),
    CONSTRAINT uk_ligne_vente_article UNIQUE (vente_id, article_id)
);

CREATE INDEX idx_ligne_vente_article ON ligne_vente (article_id);

-- Les sorties de stock referencent la vente
ALTER TABLE mouvement_stock ADD COLUMN vente_id BIGINT REFERENCES vente (id);
