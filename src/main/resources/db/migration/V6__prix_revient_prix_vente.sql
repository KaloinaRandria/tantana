-- =====================================================================
-- V6 : phase 1 du module Ventes
--   1. Prix de revient : les frais du transitaire sont desormais repartis
--      sur les articles recus, au prorata de leur valeur.
--        prix d'achat  = prix fournisseur x taux du jour     -> stock.cmup
--        prix revient  = prix d'achat + part des frais       -> stock.cmup_revient
--   2. Prix de vente catalogue (HT) par article + historique des changements
--   3. Parametres : TVA, remise maximale vendeur, identite de la societe
-- =====================================================================

-- ----- 1. Prix de revient -----
ALTER TABLE stock ADD COLUMN cmup_revient NUMERIC(18, 4) NOT NULL DEFAULT 0;
-- Stock existant : pas d'historique de repartition, le revient part du prix d'achat
UPDATE stock SET cmup_revient = cmup;

ALTER TABLE mouvement_stock ADD COLUMN cout_revient_unitaire NUMERIC(18, 4) NOT NULL DEFAULT 0;
ALTER TABLE mouvement_stock ADD COLUMN cmup_revient_apres    NUMERIC(18, 4) NOT NULL DEFAULT 0;
UPDATE mouvement_stock SET cout_revient_unitaire = cout_unitaire, cmup_revient_apres = cmup_apres;

-- Part des frais par unite, figee a la validation de la reception
ALTER TABLE ligne_reception ADD COLUMN frais_unitaire NUMERIC(18, 4) NOT NULL DEFAULT 0;

-- ----- 2. Prix de vente -----
-- NULL = prix pas encore fixe : l'article ne pourra pas etre vendu
ALTER TABLE article ADD COLUMN prix_vente_ht NUMERIC(18, 2) CHECK (prix_vente_ht >= 0);

CREATE TABLE historique_prix_vente (
    id                BIGSERIAL      PRIMARY KEY,
    article_id        BIGINT         NOT NULL REFERENCES article (id),
    ancien_prix       NUMERIC(18, 2),
    nouveau_prix      NUMERIC(18, 2) NOT NULL,
    -- cout de revient au moment du changement, pour comprendre la decision
    cmup_revient      NUMERIC(18, 4),
    motif             VARCHAR(255),
    modifie_par_id    BIGINT         NOT NULL REFERENCES utilisateur (id),
    date_modification TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_historique_prix_article ON historique_prix_vente (article_id, date_modification);

-- ----- 3. Parametres (une seule ligne, id = 1) -----
CREATE TABLE parametre (
    id                 INTEGER       PRIMARY KEY CHECK (id = 1),
    taux_tva           NUMERIC(5, 2) NOT NULL DEFAULT 20 CHECK (taux_tva >= 0 AND taux_tva <= 100),
    remise_max_vendeur NUMERIC(5, 2) NOT NULL DEFAULT 10 CHECK (remise_max_vendeur >= 0 AND remise_max_vendeur <= 100),
    raison_sociale     VARCHAR(150)  NOT NULL,
    adresse            VARCHAR(255),
    telephone          VARCHAR(50),
    email              VARCHAR(150),
    nif                VARCHAR(50),
    stat               VARCHAR(50),
    rcs                VARCHAR(50),
    mentions_facture   TEXT,
    date_modification  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version            BIGINT        NOT NULL DEFAULT 0
);

INSERT INTO parametre (id, taux_tva, remise_max_vendeur, raison_sociale, mentions_facture)
VALUES (1, 20, 10, 'Tantana', 'Merci de votre confiance. Marchandises vendues non reprises sauf défaut constaté à la livraison.');
