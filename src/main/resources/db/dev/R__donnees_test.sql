-- =====================================================================
-- Donnees de test : DEV UNIQUEMENT (dossier db/dev, charge seulement
-- par le profil dev). Script "repeatable" (R__) : Flyway le rejoue
-- automatiquement a chaque modification de ce fichier.
--
-- Mot de passe de TOUS les comptes de test : Tantana@2026
-- (hash BCrypt ci-dessous, genere avec le PasswordEncoder de Spring)
-- =====================================================================

INSERT INTO utilisateur (username, password, nom_complet, email, actif) VALUES
    ('admin',    '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Administrateur Test',   'admin@tantana.test',    TRUE),
    ('stock',    '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Gestionnaire Stock',    'stock@tantana.test',    TRUE),
    ('vendeur',  '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Vendeur Test',          'vendeur@tantana.test',  TRUE),
    ('acheteur', '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Acheteur Test',         'acheteur@tantana.test', TRUE),
    -- plusieurs roles sur un meme compte
    ('polyvalent','{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6','Vendeur et Magasinier', 'poly@tantana.test',     TRUE),
    -- double controle : l'un cree, l'autre valide
    ('maker',    '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Initiateur Test',       'maker@tantana.test',    TRUE),
    ('checker',  '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Validateur Test',       'checker@tantana.test',  TRUE),
    -- compte desactive : la connexion doit etre refusee
    ('inactif',  '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Compte Desactive',      'inactif@tantana.test',  FALSE)
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
