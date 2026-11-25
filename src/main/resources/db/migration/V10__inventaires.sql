-- =====================================================================
-- V10 : inventaires physiques (INV-AAAA-NNNN)
--
-- Cycle :
--   EN_COMPTAGE --(compteur soumet)--> A_VALIDER --(CHECKER valide)--> VALIDE
--        ^                                  |
--        +--------(CHECKER rejette)---------+      (recomptage demande)
--   EN_COMPTAGE / A_VALIDER --(annulation)--> ANNULE
--
-- Bonnes pratiques appliquees :
--  - stock theorique et couts FIGES a l'ouverture (photo du stock)
--  - comptage A L'AVEUGLE : le compteur ne voit pas la quantite theorique
--  - articles GELES tant que l'inventaire est ouvert (aucune entree / sortie)
--  - chaque ecart doit etre justifie (motif) avant validation
--  - principe des 4 yeux : le validateur n'a ni cree ni compte l'inventaire
--  - la validation genere des mouvements AJUSTEMENT_POSITIF / NEGATIF au CMUP,
--    sans modifier le CMUP (un ecart d'inventaire n'est pas un achat)
-- =====================================================================

CREATE TABLE inventaire (
    id                BIGSERIAL    PRIMARY KEY,
    numero            VARCHAR(20)  NOT NULL UNIQUE,
    -- COMPLET (tous les articles) ou TOURNANT (selection d'articles)
    type              VARCHAR(20)  NOT NULL,
    date_inventaire   DATE         NOT NULL,
    statut            VARCHAR(20)  NOT NULL,
    observations      TEXT,
    cree_par_id       BIGINT       NOT NULL REFERENCES utilisateur (id),
    date_creation     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- dernier utilisateur ayant saisi des quantites, et date de soumission
    compte_par_id     BIGINT       REFERENCES utilisateur (id),
    date_soumission   TIMESTAMP,
    -- dernier rejet (recomptage demande)
    motif_rejet       TEXT,
    rejete_par_id     BIGINT       REFERENCES utilisateur (id),
    date_rejet        TIMESTAMP,
    valide_par_id     BIGINT       REFERENCES utilisateur (id),
    date_validation   TIMESTAMP,
    motif_annulation  TEXT,
    annule_par_id     BIGINT       REFERENCES utilisateur (id),
    date_annulation   TIMESTAMP,
    version           BIGINT       NOT NULL DEFAULT 0,
    -- principe des 4 yeux, garanti aussi par la base
    CONSTRAINT ck_inventaire_validateur CHECK (
        valide_par_id IS NULL
        OR (valide_par_id <> cree_par_id AND valide_par_id IS DISTINCT FROM compte_par_id))
);

CREATE INDEX idx_inventaire_statut ON inventaire (statut);

CREATE TABLE ligne_inventaire (
    id                 BIGSERIAL      PRIMARY KEY,
    inventaire_id      BIGINT         NOT NULL REFERENCES inventaire (id) ON DELETE CASCADE,
    article_id         BIGINT         NOT NULL REFERENCES article (id),
    -- photo du stock a l'ouverture
    quantite_theorique NUMERIC(15, 3) NOT NULL,
    cmup               NUMERIC(18, 4) NOT NULL,
    cmup_revient       NUMERIC(18, 4) NOT NULL,
    -- NULL tant que l'article n'a pas ete compte
    quantite_comptee   NUMERIC(15, 3) CHECK (quantite_comptee >= 0),
    motif_ecart        VARCHAR(30),
    commentaire        VARCHAR(255),
    CONSTRAINT uk_ligne_inventaire_article UNIQUE (inventaire_id, article_id)
);

CREATE INDEX idx_ligne_inventaire_article ON ligne_inventaire (article_id);

-- Les ajustements de stock referencent l'inventaire
ALTER TABLE mouvement_stock ADD COLUMN inventaire_id BIGINT REFERENCES inventaire (id);
