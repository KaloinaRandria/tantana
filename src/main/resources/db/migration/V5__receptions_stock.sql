-- =====================================================================
-- V5 : receptions (arrivages chez le transitaire) et stock
--
--   reception        : un arrivage d'une commande. Saisi par un MAKER,
--                      valide ou rejete par un CHECKER (personne differente).
--   ligne_reception  : quantites reellement recues (bon etat) et refusees
--                      (abimees / manquantes) par ligne de commande.
--   frais_reception  : frais du transitaire en Ariary, NON repartis sur le
--                      cout des articles (choix metier).
--   stock            : quantite et cout moyen pondere (CMUP) par article.
--   mouvement_stock  : historique de chaque entree / sortie (tracabilite).
--
-- Le stock ne bouge QU'A LA VALIDATION d'une reception.
-- =====================================================================

CREATE TABLE reception (
    id              BIGSERIAL      PRIMARY KEY,
    numero          VARCHAR(20)    NOT NULL UNIQUE,
    commande_id     BIGINT         NOT NULL REFERENCES commande_achat (id),
    transitaire_id  BIGINT         NOT NULL REFERENCES transitaire (id),
    date_reception  DATE           NOT NULL,
    -- taux reel du jour de reception : sert au cout d'entree en stock
    taux_change     NUMERIC(18, 6) NOT NULL CHECK (taux_change > 0),
    statut          VARCHAR(20)    NOT NULL,
    observations    TEXT,
    motif_rejet     TEXT,
    saisi_par_id    BIGINT         NOT NULL REFERENCES utilisateur (id),
    date_saisie     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valide_par_id   BIGINT         REFERENCES utilisateur (id),
    date_validation TIMESTAMP,
    version         BIGINT         NOT NULL DEFAULT 0,
    -- principe des 4 yeux, garanti aussi par la base
    CONSTRAINT ck_reception_validateur CHECK (valide_par_id IS NULL OR valide_par_id <> saisi_par_id)
);

CREATE INDEX idx_reception_commande ON reception (commande_id);
CREATE INDEX idx_reception_statut   ON reception (statut);

CREATE TABLE ligne_reception (
    id                BIGSERIAL      PRIMARY KEY,
    reception_id      BIGINT         NOT NULL REFERENCES reception (id) ON DELETE CASCADE,
    ligne_commande_id BIGINT         NOT NULL REFERENCES ligne_commande_achat (id),
    quantite_recue    NUMERIC(15, 3) NOT NULL DEFAULT 0 CHECK (quantite_recue >= 0),
    quantite_refusee  NUMERIC(15, 3) NOT NULL DEFAULT 0 CHECK (quantite_refusee >= 0),
    commentaire       VARCHAR(255),
    CONSTRAINT uk_ligne_reception UNIQUE (reception_id, ligne_commande_id)
);

CREATE TABLE frais_reception (
    id                BIGSERIAL      PRIMARY KEY,
    reception_id      BIGINT         NOT NULL REFERENCES reception (id) ON DELETE CASCADE,
    type              VARCHAR(20)    NOT NULL,
    montant           NUMERIC(18, 2) NOT NULL CHECK (montant >= 0),
    reference_facture VARCHAR(100),
    commentaire       VARCHAR(255)
);

CREATE INDEX idx_frais_reception ON frais_reception (reception_id);

CREATE TABLE stock (
    id         BIGSERIAL      PRIMARY KEY,
    article_id BIGINT         NOT NULL UNIQUE REFERENCES article (id),
    quantite   NUMERIC(15, 3) NOT NULL DEFAULT 0,
    -- cout moyen unitaire pondere, en Ariary
    cmup       NUMERIC(18, 4) NOT NULL DEFAULT 0,
    date_maj   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version    BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE mouvement_stock (
    id             BIGSERIAL      PRIMARY KEY,
    article_id     BIGINT         NOT NULL REFERENCES article (id),
    type           VARCHAR(30)    NOT NULL,
    -- toujours positive ; le sens est donne par le type
    quantite       NUMERIC(15, 3) NOT NULL CHECK (quantite > 0),
    cout_unitaire  NUMERIC(18, 4) NOT NULL,
    quantite_apres NUMERIC(15, 3) NOT NULL,
    cmup_apres     NUMERIC(18, 4) NOT NULL,
    reference      VARCHAR(50)    NOT NULL,
    reception_id   BIGINT         REFERENCES reception (id),
    utilisateur_id BIGINT         NOT NULL REFERENCES utilisateur (id),
    date_mouvement TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mouvement_article ON mouvement_stock (article_id, date_mouvement);
