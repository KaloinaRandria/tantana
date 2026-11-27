-- =====================================================================
-- Base TANTANA_TEST : utilisateurs et roles UNIQUEMENT.
--
-- Charge par Flyway avec le profil "test" (dossier db/test), apres les
-- migrations db/migration. Aucune donnee metier (articles, fournisseurs,
-- commandes, ventes...) : la base demarre vide.
--
-- Script "repeatable" (R__) : rejoue a chaque modification du fichier.
-- ON CONFLICT DO NOTHING : sans effet sur les comptes deja presents.
--
-- Execution manuelle possible :
--   psql -h localhost -U postgres -d TANTANA_TEST -f R__utilisateurs_test.sql
--
-- Mot de passe de TOUS les comptes : le meme que les donnees de dev
-- (voir db/dev/R__donnees_test.sql) ; a changer a la premiere connexion.
-- =====================================================================

-- Les 6 roles sont crees par les migrations V1 et V2 ; on les garantit ici
-- pour pouvoir executer ce script seul sur une base deja migree.
INSERT INTO role (code, libelle) VALUES
    ('ADMIN',              'Administrateur'),
    ('GESTIONNAIRE_STOCK', 'Gestionnaire de stock'),
    ('VENDEUR',            'Vendeur'),
    ('ACHETEUR',           'Acheteur'),
    ('MAKER',              'Initiateur (création des opérations)'),
    ('CHECKER',            'Validateur (validation des opérations)')
ON CONFLICT (code) DO NOTHING;

INSERT INTO utilisateur (username, password, nom_complet, email, actif) VALUES
    ('admin',     '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Administrateur Test',   'admin@tantana.test',    TRUE),
    ('stock',     '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Gestionnaire Stock',    'stock@tantana.test',    TRUE),
    ('vendeur',   '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Vendeur Test',          'vendeur@tantana.test',  TRUE),
    ('acheteur',  '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Acheteur Test',         'acheteur@tantana.test', TRUE),
    -- plusieurs roles sur un meme compte
    ('polyvalent','{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Vendeur et Magasinier', 'poly@tantana.test',     TRUE),
    -- double controle : l'un cree, l'autre valide
    ('maker',     '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Initiateur Test',       'maker@tantana.test',    TRUE),
    ('checker',   '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Validateur Test',       'checker@tantana.test',  TRUE),
    -- compte desactive : la connexion doit etre refusee
    ('inactif',   '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Compte Desactive',      'inactif@tantana.test',  FALSE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO utilisateur_role (utilisateur_id, role_id)
SELECT u.id, r.id
FROM (VALUES
        ('admin',      'ADMIN'),
        ('stock',      'GESTIONNAIRE_STOCK'),
        ('vendeur',    'VENDEUR'),
        ('acheteur',   'ACHETEUR'),
        ('polyvalent', 'VENDEUR'),
        ('polyvalent', 'GESTIONNAIRE_STOCK'),
        ('maker',      'VENDEUR'),
        ('maker',      'ACHETEUR'),
        ('maker',      'MAKER'),
        ('checker',    'VENDEUR'),
        ('checker',    'ACHETEUR'),
        ('checker',    'CHECKER'),
        ('inactif',    'VENDEUR')
     ) AS a (username, code)
JOIN utilisateur u ON u.username = a.username
JOIN role r        ON r.code     = a.code
ON CONFLICT DO NOTHING;
