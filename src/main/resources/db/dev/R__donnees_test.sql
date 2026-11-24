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

-- ===== Referentiels Achats =====

INSERT INTO fournisseur (nom, pays, devise, contact, telephone, email, actif) VALUES
    ('Guangzhou Trading Co.', 'Chine',              'CNY', 'Li Wei',       '+86 20 0000 0000',  'sales@gz-trading.test', TRUE),
    ('Dubai General Supply',  'Emirats arabes unis', 'AED', 'Omar Haddad',  '+971 4 000 0000',   'contact@dgs.test',      TRUE),
    ('Euro Import SARL',      'France',             'EUR', 'Claire Martin', '+33 1 00 00 00 00', 'commandes@euroimp.test', TRUE),
    ('Ancien Fournisseur',    'Inde',               'USD', NULL,            NULL,                NULL,                    FALSE)
ON CONFLICT (nom) DO NOTHING;

INSERT INTO transitaire (nom, contact, telephone, email, adresse, actif) VALUES
    ('Transit Express Toamasina', 'Rakoto Jean', '+261 34 00 000 00', 'contact@transit-tmm.test', 'Port de Toamasina', TRUE),
    ('Ivato Fret Services',       'Rabe Hery',   '+261 33 00 000 00', 'fret@ivato.test',          'Aeroport Ivato',    TRUE)
ON CONFLICT (nom) DO NOTHING;

INSERT INTO article (reference, designation, unite, description, actif) VALUES
    ('ART-0001', 'Telephone portable 6.5 pouces', 'PIECE',  'Smartphone 128 Go',              TRUE),
    ('ART-0002', 'Chargeur USB-C 25W',            'PIECE',  NULL,                             TRUE),
    ('ART-0003', 'Ecouteurs sans fil',            'PIECE',  'Boitier de charge inclus',       TRUE),
    ('ART-0004', 'Cable HDMI 2 m',                'CARTON', 'Carton de 50 cables',            TRUE),
    ('ART-0005', 'Riz parfume 25 kg',             'SAC',    NULL,                             TRUE),
    ('ART-0006', 'Ancien modele de lampe',        'PIECE',  'Plus commande',                  FALSE)
ON CONFLICT (reference) DO NOTHING;

-- ===== Commandes d'achat de test =====
-- CA-2025-9001 : passee, arrivee prevue depassee (=> "En retard")
-- CA-2025-9002 : brouillon
-- Numeros en 2025-9xxx pour ne jamais entrer en conflit avec la numerotation reelle.

INSERT INTO commande_achat (numero, fournisseur_id, date_commande, date_arrivee_prevue, devise, taux_change,
                            montant_total, statut, observations, cree_par_id)
SELECT 'CA-2025-9001', f.id, CURRENT_DATE - 45, CURRENT_DATE - 5, 'CNY', 640, 0, 'COMMANDEE',
       'Expedition maritime via Toamasina', u.id
FROM fournisseur f, utilisateur u
WHERE f.nom = 'Guangzhou Trading Co.' AND u.username = 'admin'
ON CONFLICT (numero) DO NOTHING;

INSERT INTO commande_achat (numero, fournisseur_id, date_commande, date_arrivee_prevue, devise, taux_change,
                            montant_total, statut, observations, cree_par_id)
SELECT 'CA-2025-9002', f.id, CURRENT_DATE - 2, CURRENT_DATE + 30, 'EUR', 5100, 0, 'BROUILLON', NULL, u.id
FROM fournisseur f, utilisateur u
WHERE f.nom = 'Euro Import SARL' AND u.username = 'maker'
ON CONFLICT (numero) DO NOTHING;

INSERT INTO ligne_commande_achat (commande_id, article_id, quantite_commandee, prix_unitaire)
SELECT c.id, a.id, l.qte, l.pu
FROM (VALUES
        ('CA-2025-9001', 'ART-0001', 50,  820.00),
        ('CA-2025-9001', 'ART-0002', 200,  35.50),
        ('CA-2025-9001', 'ART-0003', 100,  95.00),
        ('CA-2025-9002', 'ART-0004', 10,  120.00)
     ) AS l (numero, reference, qte, pu)
JOIN commande_achat c ON c.numero = l.numero
JOIN article a        ON a.reference = l.reference
ON CONFLICT (commande_id, article_id) DO NOTHING;

UPDATE commande_achat c
SET montant_total = (SELECT COALESCE(SUM(l.quantite_commandee * l.prix_unitaire), 0)
                     FROM ligne_commande_achat l WHERE l.commande_id = c.id)
WHERE c.numero IN ('CA-2025-9001', 'CA-2025-9002');

-- ===== Reception de test =====
-- BR-2025-9001 : arrivage partiel de CA-2025-9001, saisi par "maker",
-- en attente de validation par "checker" (ou admin).

INSERT INTO reception (numero, commande_id, transitaire_id, date_reception, taux_change, statut, observations, saisi_par_id)
SELECT 'BR-2025-9001', c.id, t.id, CURRENT_DATE - 1, 645, 'SAISIE', 'Conteneur 20 pieds, 2 telephones abimes', u.id
FROM commande_achat c, transitaire t, utilisateur u
WHERE c.numero = 'CA-2025-9001' AND t.nom = 'Transit Express Toamasina' AND u.username = 'maker'
ON CONFLICT (numero) DO NOTHING;

INSERT INTO ligne_reception (reception_id, ligne_commande_id, quantite_recue, quantite_refusee, commentaire)
SELECT r.id, lc.id, l.recue, l.refusee, l.commentaire
FROM (VALUES
        ('ART-0001', 48, 2, 'Ecrans fissures'),
        ('ART-0002', 200, 0, NULL)
     ) AS l (reference, recue, refusee, commentaire)
JOIN reception r            ON r.numero = 'BR-2025-9001'
JOIN article a              ON a.reference = l.reference
JOIN ligne_commande_achat lc ON lc.commande_id = r.commande_id AND lc.article_id = a.id
ON CONFLICT (reception_id, ligne_commande_id) DO NOTHING;

INSERT INTO frais_reception (reception_id, type, montant, reference_facture)
SELECT r.id, f.type, f.montant, f.ref
FROM (VALUES
        ('FRET',    2500000, 'FT-7781'),
        ('DOUANE',  4800000, 'DQ-2025-118'),
        ('TRANSIT',  600000, 'FT-7782')
     ) AS f (type, montant, ref)
JOIN reception r ON r.numero = 'BR-2025-9001'
WHERE NOT EXISTS (SELECT 1 FROM frais_reception x WHERE x.reception_id = r.id);
