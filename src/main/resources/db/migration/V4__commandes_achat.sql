-- =====================================================================
-- V4 : commandes d'achat
--   numerotation          : compteurs de numeros par prefixe et par annee
--                           (CA-2026-0001, plus tard BR-..., FV-...)
--   commande_achat        : en-tete (fournisseur, dates, devise, taux)
--   ligne_commande_achat  : articles commandes ; quantite_recue sera
--                           alimentee par les receptions validees (etape 3)
-- =====================================================================

CREATE TABLE numerotation (
    prefixe VARCHAR(10) NOT NULL,
    annee   INTEGER     NOT NULL,
    dernier INTEGER     NOT NULL,
    PRIMARY KEY (prefixe, annee)
);

CREATE TABLE commande_achat (
    id                  BIGSERIAL      PRIMARY KEY,
    numero              VARCHAR(20)    NOT NULL UNIQUE,
    fournisseur_id      BIGINT         NOT NULL REFERENCES fournisseur (id),
    date_commande       DATE           NOT NULL,
    date_arrivee_prevue DATE           NOT NULL,
    devise              VARCHAR(3)     NOT NULL,
    -- nombre d'Ariary pour 1 unite de la devise (ex. 1 USD = 4 500 MGA)
    taux_change         NUMERIC(18, 6) NOT NULL CHECK (taux_change > 0),
    -- total des lignes en devise, recalcule a chaque enregistrement
    montant_total       NUMERIC(20, 2) NOT NULL DEFAULT 0,
    statut              VARCHAR(30)    NOT NULL,
    observations        TEXT,
    cree_par_id         BIGINT         NOT NULL REFERENCES utilisateur (id),
    date_creation       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification   TIMESTAMP,
    version             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_commande_dates CHECK (date_arrivee_prevue >= date_commande)
);

CREATE INDEX idx_commande_achat_fournisseur ON commande_achat (fournisseur_id);
CREATE INDEX idx_commande_achat_statut      ON commande_achat (statut);

CREATE TABLE ligne_commande_achat (
    id                 BIGSERIAL      PRIMARY KEY,
    commande_id        BIGINT         NOT NULL REFERENCES commande_achat (id) ON DELETE CASCADE,
    article_id         BIGINT         NOT NULL REFERENCES article (id),
    quantite_commandee NUMERIC(15, 3) NOT NULL CHECK (quantite_commandee > 0),
    prix_unitaire      NUMERIC(18, 4) NOT NULL CHECK (prix_unitaire >= 0),
    quantite_recue     NUMERIC(15, 3) NOT NULL DEFAULT 0 CHECK (quantite_recue >= 0),
    CONSTRAINT uk_ligne_commande_article UNIQUE (commande_id, article_id)
);

CREATE INDEX idx_ligne_commande_article ON ligne_commande_achat (article_id);
