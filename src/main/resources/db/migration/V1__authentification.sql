-- =====================================================================
-- V1 : authentification (utilisateurs, roles, liaison ManyToMany)
-- Execute dans TOUS les environnements (dev, prod).
-- =====================================================================

CREATE TABLE role (
    id      BIGSERIAL    PRIMARY KEY,
    code    VARCHAR(50)  NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE utilisateur (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    nom_complet   VARCHAR(150) NOT NULL,
    email         VARCHAR(150) UNIQUE,
    actif         BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Un utilisateur peut avoir plusieurs roles (ManyToMany)
CREATE TABLE utilisateur_role (
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur (id) ON DELETE CASCADE,
    role_id        BIGINT NOT NULL REFERENCES role (id)        ON DELETE RESTRICT,
    PRIMARY KEY (utilisateur_id, role_id)
);

CREATE INDEX idx_utilisateur_role_role ON utilisateur_role (role_id);

-- Roles de reference (necessaires en production aussi)
INSERT INTO role (code, libelle) VALUES
    ('ADMIN',              'Administrateur'),
    ('GESTIONNAIRE_STOCK', 'Gestionnaire de stock'),
    ('VENDEUR',            'Vendeur'),
    ('ACHETEUR',           'Acheteur');
