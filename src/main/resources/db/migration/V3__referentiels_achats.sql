-- =====================================================================
-- V3 : referentiels du module Achats
--   article      : catalogue des produits (partage avec Stock et Ventes)
--   fournisseur  : fournisseurs a l'etranger, avec leur devise habituelle
--   transitaire  : transitaires externes ou l'on recupere les arrivages
-- Pas de suppression physique : on desactive (actif = FALSE), car ces
-- lignes seront referencees par les commandes et les receptions.
-- =====================================================================

CREATE TABLE article (
    id            BIGSERIAL    PRIMARY KEY,
    reference     VARCHAR(50)  NOT NULL UNIQUE,
    designation   VARCHAR(200) NOT NULL,
    unite         VARCHAR(20)  NOT NULL DEFAULT 'PIECE',
    description   TEXT,
    actif         BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fournisseur (
    id            BIGSERIAL    PRIMARY KEY,
    nom           VARCHAR(150) NOT NULL UNIQUE,
    pays          VARCHAR(100) NOT NULL,
    devise        VARCHAR(3)   NOT NULL,
    contact       VARCHAR(150),
    telephone     VARCHAR(50),
    email         VARCHAR(150),
    adresse       VARCHAR(255),
    actif         BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transitaire (
    id            BIGSERIAL    PRIMARY KEY,
    nom           VARCHAR(150) NOT NULL UNIQUE,
    contact       VARCHAR(150),
    telephone     VARCHAR(50),
    email         VARCHAR(150),
    adresse       VARCHAR(255),
    actif         BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
