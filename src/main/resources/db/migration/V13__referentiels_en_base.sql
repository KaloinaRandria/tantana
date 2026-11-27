-- =====================================================================
-- V13 : tous les referentiels en base, references par un identifiant numerique
--
-- 1. Statuts et types SYSTEME (valeurs fixes, pilotent les regles du code) :
--    id SMALLINT fixe + code lisible + libelle (+ couleur). Libelles et couleurs
--    modifiables par l'ADMIN ; on n'ajoute pas de valeur (aucune regle ne la traiterait).
-- 2. Listes METIER modifiables par l'ADMIN (creation, modification, desactivation) :
--    unites, modes de paiement, motifs d'ecart, types de frais, devises.
-- 3. Les colonnes texte des tables metier (statut, type, mode, devise, unite...)
--    deviennent des colonnes SMALLINT <xxx>_id avec cle etrangere.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Referentiels systeme
-- ---------------------------------------------------------------------
CREATE TABLE statut_commande (
    id      SMALLINT    PRIMARY KEY,
    code    VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(60) NOT NULL,
    couleur VARCHAR(20) NOT NULL
);
INSERT INTO statut_commande (id, code, libelle, couleur) VALUES
    (1, 'BROUILLON',           'Brouillon',           'secondary'),
    (2, 'COMMANDEE',           'Commandée',           'primary'),
    (3, 'PARTIELLEMENT_RECUE', 'Partiellement reçue', 'warning'),
    (4, 'RECUE',               'Reçue',               'success'),
    (5, 'CLOTUREE',            'Clôturée',            'dark'),
    (6, 'ANNULEE',             'Annulée',             'danger');

CREATE TABLE statut_reception (
    id      SMALLINT    PRIMARY KEY,
    code    VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(60) NOT NULL,
    couleur VARCHAR(20) NOT NULL
);
INSERT INTO statut_reception (id, code, libelle, couleur) VALUES
    (1, 'SAISIE',  'À valider', 'warning'),
    (2, 'VALIDEE', 'Validée',   'success'),
    (3, 'REJETEE', 'Rejetée',   'danger');

CREATE TABLE statut_vente (
    id      SMALLINT    PRIMARY KEY,
    code    VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(60) NOT NULL,
    couleur VARCHAR(20) NOT NULL
);
INSERT INTO statut_vente (id, code, libelle, couleur) VALUES
    (1, 'BROUILLON', 'Brouillon', 'secondary'),
    (2, 'A_VALIDER', 'À valider', 'warning'),
    (3, 'VALIDEE',   'Facturée',  'success');

-- Calcule (jamais stocke sur la vente) : la table ne sert qu'aux libelles et couleurs
CREATE TABLE statut_paiement (
    id      SMALLINT    PRIMARY KEY,
    code    VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(60) NOT NULL,
    couleur VARCHAR(20) NOT NULL
);
INSERT INTO statut_paiement (id, code, libelle, couleur) VALUES
    (1, 'NON_PAYEE',        'Non payée',           'danger'),
    (2, 'PARTIELLE',        'Partiellement payée', 'warning'),
    (3, 'PAYEE',            'Payée',               'success'),
    (4, 'SOLDEE_PAR_AVOIR', 'Soldée par avoir',    'secondary');

CREATE TABLE statut_inventaire (
    id      SMALLINT    PRIMARY KEY,
    code    VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(60) NOT NULL,
    couleur VARCHAR(20) NOT NULL
);
INSERT INTO statut_inventaire (id, code, libelle, couleur) VALUES
    (1, 'EN_COMPTAGE', 'En comptage', 'info'),
    (2, 'A_VALIDER',   'À valider',   'warning'),
    (3, 'VALIDE',      'Validé',      'success'),
    (4, 'ANNULE',      'Annulé',      'secondary');

-- sens : +1 entree, -1 sortie (fige : utilise par les calculs de stock)
CREATE TABLE type_mouvement (
    id      SMALLINT    PRIMARY KEY,
    code    VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(60) NOT NULL,
    couleur VARCHAR(20) NOT NULL,
    sens    SMALLINT    NOT NULL CHECK (sens IN (-1, 1))
);
INSERT INTO type_mouvement (id, code, libelle, couleur, sens) VALUES
    (1, 'ENTREE_ACHAT',       'Entrée (réception achat)', 'success',   1),
    (2, 'SORTIE_VENTE',       'Sortie (vente)',           'primary',  -1),
    (3, 'RETOUR_VENTE',       'Retour client (avoir)',    'info',      1),
    (4, 'AJUSTEMENT_POSITIF', 'Ajustement +',             'secondary', 1),
    (5, 'AJUSTEMENT_NEGATIF', 'Ajustement −',             'warning',  -1);

CREATE TABLE type_inventaire (
    id          SMALLINT     PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    libelle     VARCHAR(60)  NOT NULL,
    couleur     VARCHAR(20)  NOT NULL,
    description VARCHAR(200)
);
INSERT INTO type_inventaire (id, code, libelle, couleur, description) VALUES
    (1, 'COMPLET',  'Complet',  'primary',   'Tous les articles — inventaire annuel de clôture'),
    (2, 'TOURNANT', 'Tournant', 'secondary', 'Une sélection d''articles — comptage régulier sans fermer le magasin');

CREATE TABLE type_client (
    id      SMALLINT    PRIMARY KEY,
    code    VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(60) NOT NULL,
    couleur VARCHAR(20) NOT NULL
);
INSERT INTO type_client (id, code, libelle, couleur) VALUES
    (1, 'PARTICULIER',   'Particulier',   'secondary'),
    (2, 'PROFESSIONNEL', 'Professionnel', 'primary');

-- ---------------------------------------------------------------------
-- 2. Listes metier modifiables par l'ADMIN
-- ---------------------------------------------------------------------
CREATE TABLE unite (
    id                SMALLINT     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code              VARCHAR(20)  NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9_]+$'),
    libelle           VARCHAR(50)  NOT NULL,
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP
);
CREATE UNIQUE INDEX ux_unite_libelle ON unite (LOWER(libelle));
INSERT INTO unite (code, libelle) VALUES
    ('PIECE', 'Pièce'), ('CARTON', 'Carton'), ('SAC', 'Sac'), ('LOT', 'Lot'),
    ('KG', 'Kilogramme'), ('LITRE', 'Litre'), ('METRE', 'Mètre');

CREATE TABLE mode_paiement (
    id                    SMALLINT     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code                  VARCHAR(20)  NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9_]+$'),
    libelle               VARCHAR(50)  NOT NULL,
    icone                 VARCHAR(40)  NOT NULL DEFAULT 'bi-wallet2',
    reference_obligatoire BOOLEAN      NOT NULL DEFAULT TRUE,
    actif                 BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification     TIMESTAMP
);
CREATE UNIQUE INDEX ux_mode_paiement_libelle ON mode_paiement (LOWER(libelle));
INSERT INTO mode_paiement (code, libelle, icone, reference_obligatoire) VALUES
    ('ESPECES',      'Espèces',           'bi-cash',          FALSE),
    ('MVOLA',        'MVola',             'bi-phone',         TRUE),
    ('ORANGE_MONEY', 'Orange Money',      'bi-phone',         TRUE),
    ('AIRTEL_MONEY', 'Airtel Money',      'bi-phone',         TRUE),
    ('VIREMENT',     'Virement bancaire', 'bi-bank',          TRUE),
    ('CHEQUE',       'Chèque',            'bi-journal-check', TRUE);

CREATE TABLE motif_ecart (
    id                      SMALLINT     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code                    VARCHAR(20)  NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9_]+$'),
    libelle                 VARCHAR(60)  NOT NULL,
    commentaire_obligatoire BOOLEAN      NOT NULL DEFAULT FALSE,
    actif                   BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification       TIMESTAMP
);
CREATE UNIQUE INDEX ux_motif_ecart_libelle ON motif_ecart (LOWER(libelle));
INSERT INTO motif_ecart (code, libelle, commentaire_obligatoire) VALUES
    ('CASSE',            'Casse / détérioration',                 FALSE),
    ('PEREMPTION',       'Produit périmé',                        FALSE),
    ('VOL_PERTE',        'Vol ou perte inexpliquée',              FALSE),
    ('ERREUR_RECEPTION', 'Erreur de réception (quantité saisie)', FALSE),
    ('ERREUR_VENTE',     'Erreur de vente / livraison',           FALSE),
    ('ECHANTILLON',      'Échantillon / usage interne',           FALSE),
    ('AUTRE',            'Autre (préciser en commentaire)',       TRUE);

CREATE TABLE type_frais (
    id                SMALLINT     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code              VARCHAR(20)  NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9_]+$'),
    libelle           VARCHAR(60)  NOT NULL,
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP
);
CREATE UNIQUE INDEX ux_type_frais_libelle ON type_frais (LOWER(libelle));
INSERT INTO type_frais (code, libelle) VALUES
    ('FRET',        'Fret / transport'),
    ('DOUANE',      'Droits et taxes de douane'),
    ('TRANSIT',     'Honoraires de transit'),
    ('MANUTENTION', 'Manutention'),
    ('STOCKAGE',    'Magasinage / stockage'),
    ('AUTRE',       'Autre');

-- monnaie_locale : la devise de valorisation du stock (taux force a 1) ; une seule
CREATE TABLE devise (
    id                SMALLINT     GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code              VARCHAR(3)   NOT NULL UNIQUE CHECK (code ~ '^[A-Z]{3}$'),
    libelle           VARCHAR(50)  NOT NULL,
    monnaie_locale    BOOLEAN      NOT NULL DEFAULT FALSE,
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP,
    CONSTRAINT ck_devise_locale_active CHECK (NOT monnaie_locale OR actif)
);
CREATE UNIQUE INDEX ux_devise_libelle ON devise (LOWER(libelle));
CREATE UNIQUE INDEX ux_devise_locale ON devise (monnaie_locale) WHERE monnaie_locale;
INSERT INTO devise (code, libelle, monnaie_locale) VALUES
    ('USD', 'Dollar américain',   FALSE),
    ('EUR', 'Euro',               FALSE),
    ('CNY', 'Yuan chinois',       FALSE),
    ('AED', 'Dirham des Émirats', FALSE),
    ('INR', 'Roupie indienne',    FALSE),
    ('JPY', 'Yen japonais',       FALSE),
    ('ZAR', 'Rand sud-africain',  FALSE),
    ('MGA', 'Ariary malgache',    TRUE);

-- Securite : toute valeur deja utilisee et absente des listes ci-dessus devient une entree
INSERT INTO unite (code, libelle)
SELECT DISTINCT a.unite, a.unite FROM article a WHERE NOT EXISTS (SELECT 1 FROM unite u WHERE u.code = a.unite);
INSERT INTO devise (code, libelle)
SELECT DISTINCT x.devise, x.devise
FROM (SELECT devise FROM fournisseur UNION SELECT devise FROM commande_achat) x
WHERE NOT EXISTS (SELECT 1 FROM devise d WHERE d.code = x.devise);
INSERT INTO mode_paiement (code, libelle)
SELECT DISTINCT p.mode, p.mode FROM paiement p WHERE NOT EXISTS (SELECT 1 FROM mode_paiement m WHERE m.code = p.mode);
INSERT INTO motif_ecart (code, libelle)
SELECT DISTINCT l.motif_ecart, l.motif_ecart FROM ligne_inventaire l
WHERE l.motif_ecart IS NOT NULL AND NOT EXISTS (SELECT 1 FROM motif_ecart m WHERE m.code = l.motif_ecart);
INSERT INTO type_frais (code, libelle)
SELECT DISTINCT f.type, f.type FROM frais_reception f WHERE NOT EXISTS (SELECT 1 FROM type_frais t WHERE t.code = f.type);

-- ---------------------------------------------------------------------
-- 3. Conversion des colonnes texte en identifiants numeriques
--    (ajout de la colonne, reprise par le code, contrainte, suppression de l'ancienne)
-- ---------------------------------------------------------------------

-- article.unite -> unite_id
ALTER TABLE article ADD COLUMN unite_id SMALLINT;
UPDATE article t SET unite_id = r.id FROM unite r WHERE r.code = t.unite;
ALTER TABLE article ALTER COLUMN unite_id SET NOT NULL,
    ADD CONSTRAINT fk_article_unite FOREIGN KEY (unite_id) REFERENCES unite (id);
ALTER TABLE article DROP COLUMN unite;
CREATE INDEX idx_article_unite ON article (unite_id);

-- fournisseur.devise -> devise_id
ALTER TABLE fournisseur ADD COLUMN devise_id SMALLINT;
UPDATE fournisseur t SET devise_id = r.id FROM devise r WHERE r.code = t.devise;
ALTER TABLE fournisseur ALTER COLUMN devise_id SET NOT NULL,
    ADD CONSTRAINT fk_fournisseur_devise FOREIGN KEY (devise_id) REFERENCES devise (id);
ALTER TABLE fournisseur DROP COLUMN devise;

-- commande_achat.devise -> devise_id ; statut -> statut_id
ALTER TABLE commande_achat ADD COLUMN devise_id SMALLINT, ADD COLUMN statut_id SMALLINT;
UPDATE commande_achat t SET devise_id = r.id FROM devise r WHERE r.code = t.devise;
UPDATE commande_achat t SET statut_id = r.id FROM statut_commande r WHERE r.code = t.statut;
ALTER TABLE commande_achat ALTER COLUMN devise_id SET NOT NULL, ALTER COLUMN statut_id SET NOT NULL,
    ADD CONSTRAINT fk_commande_achat_devise FOREIGN KEY (devise_id) REFERENCES devise (id),
    ADD CONSTRAINT fk_commande_achat_statut FOREIGN KEY (statut_id) REFERENCES statut_commande (id);
ALTER TABLE commande_achat DROP COLUMN devise, DROP COLUMN statut;
CREATE INDEX idx_commande_achat_statut ON commande_achat (statut_id);

-- reception.statut -> statut_id
ALTER TABLE reception ADD COLUMN statut_id SMALLINT;
UPDATE reception t SET statut_id = r.id FROM statut_reception r WHERE r.code = t.statut;
ALTER TABLE reception ALTER COLUMN statut_id SET NOT NULL,
    ADD CONSTRAINT fk_reception_statut FOREIGN KEY (statut_id) REFERENCES statut_reception (id);
ALTER TABLE reception DROP COLUMN statut;
CREATE INDEX idx_reception_statut ON reception (statut_id);

-- frais_reception.type -> type_frais_id
ALTER TABLE frais_reception ADD COLUMN type_frais_id SMALLINT;
UPDATE frais_reception t SET type_frais_id = r.id FROM type_frais r WHERE r.code = t.type;
ALTER TABLE frais_reception ALTER COLUMN type_frais_id SET NOT NULL,
    ADD CONSTRAINT fk_frais_reception_type FOREIGN KEY (type_frais_id) REFERENCES type_frais (id);
ALTER TABLE frais_reception DROP COLUMN type;

-- mouvement_stock.type -> type_mouvement_id
ALTER TABLE mouvement_stock ADD COLUMN type_mouvement_id SMALLINT;
UPDATE mouvement_stock t SET type_mouvement_id = r.id FROM type_mouvement r WHERE r.code = t.type;
ALTER TABLE mouvement_stock ALTER COLUMN type_mouvement_id SET NOT NULL,
    ADD CONSTRAINT fk_mouvement_stock_type FOREIGN KEY (type_mouvement_id) REFERENCES type_mouvement (id);
ALTER TABLE mouvement_stock DROP COLUMN type;

-- client.type -> type_client_id (la contrainte NIF/STAT des professionnels est recreee)
ALTER TABLE client ADD COLUMN type_client_id SMALLINT;
UPDATE client t SET type_client_id = r.id FROM type_client r WHERE r.code = t.type;
ALTER TABLE client ALTER COLUMN type_client_id SET NOT NULL,
    ADD CONSTRAINT fk_client_type FOREIGN KEY (type_client_id) REFERENCES type_client (id);
ALTER TABLE client DROP COLUMN type;
ALTER TABLE client ADD CONSTRAINT ck_client_pro CHECK (type_client_id <> 2 OR (nif IS NOT NULL AND stat IS NOT NULL));

-- vente.statut -> statut_id (« facturee <=> numero renseigne » est recree)
ALTER TABLE vente ADD COLUMN statut_id SMALLINT;
UPDATE vente t SET statut_id = r.id FROM statut_vente r WHERE r.code = t.statut;
ALTER TABLE vente ALTER COLUMN statut_id SET NOT NULL,
    ADD CONSTRAINT fk_vente_statut FOREIGN KEY (statut_id) REFERENCES statut_vente (id);
ALTER TABLE vente DROP COLUMN statut;
ALTER TABLE vente ADD CONSTRAINT ck_vente_numero CHECK ((statut_id = 3) = (numero IS NOT NULL));
CREATE INDEX idx_vente_statut ON vente (statut_id);

-- paiement.mode -> mode_paiement_id
ALTER TABLE paiement ADD COLUMN mode_paiement_id SMALLINT;
UPDATE paiement t SET mode_paiement_id = r.id FROM mode_paiement r WHERE r.code = t.mode;
ALTER TABLE paiement ALTER COLUMN mode_paiement_id SET NOT NULL,
    ADD CONSTRAINT fk_paiement_mode FOREIGN KEY (mode_paiement_id) REFERENCES mode_paiement (id);
ALTER TABLE paiement DROP COLUMN mode;

-- inventaire.type -> type_inventaire_id ; statut -> statut_id
ALTER TABLE inventaire ADD COLUMN type_inventaire_id SMALLINT, ADD COLUMN statut_id SMALLINT;
UPDATE inventaire t SET type_inventaire_id = r.id FROM type_inventaire r WHERE r.code = t.type;
UPDATE inventaire t SET statut_id = r.id FROM statut_inventaire r WHERE r.code = t.statut;
ALTER TABLE inventaire ALTER COLUMN type_inventaire_id SET NOT NULL, ALTER COLUMN statut_id SET NOT NULL,
    ADD CONSTRAINT fk_inventaire_type FOREIGN KEY (type_inventaire_id) REFERENCES type_inventaire (id),
    ADD CONSTRAINT fk_inventaire_statut FOREIGN KEY (statut_id) REFERENCES statut_inventaire (id);
ALTER TABLE inventaire DROP COLUMN type, DROP COLUMN statut;
CREATE INDEX idx_inventaire_statut ON inventaire (statut_id);

-- ligne_inventaire.motif_ecart -> motif_ecart_id (facultatif : seulement pour les ecarts)
ALTER TABLE ligne_inventaire ADD COLUMN motif_ecart_id SMALLINT
    CONSTRAINT fk_ligne_inventaire_motif REFERENCES motif_ecart (id);
UPDATE ligne_inventaire t SET motif_ecart_id = r.id FROM motif_ecart r WHERE r.code = t.motif_ecart;
ALTER TABLE ligne_inventaire DROP COLUMN motif_ecart;
