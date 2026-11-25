-- =====================================================================
-- Donnees de SIMULATION : DEV UNIQUEMENT (dossier db/dev).
--
-- Genere environ 12 mois d'activite coherente, relative a la date du jour :
--   - referentiels : 8 fournisseurs, 4 transitaires, 31 articles, ~25 clients, 2 vendeurs
--   - achats : commandes cycliques par fournisseur (devise, taux du jour),
--              receptions saisies par "maker" et validees par "checker",
--              frais du transitaire repartis, entrees en stock au CMUP,
--              receptions partielles, refus, reliquats clotures, une reception rejetee
--   - prix de vente : prix initial puis revisions mensuelles (historique)
--   - ventes : saisonnalite (pic en decembre), croissance sur l'annee,
--              remises, controles CHECKER, factures FV, sorties de stock
--   - paiements : comptant pour les particuliers, credit pour les pros,
--                 creances encore ouvertes, un paiement annule
--   - avoirs : retours en stock, gestes commerciaux, annulations totales
--   - en fin de periode : brouillons, ventes a valider, reception en attente
--
-- Les numeros (CA, BR, FV, AV) passent par la table numerotation, exactement
-- comme l'application : aucune collision avec les numeros reels.
-- Le script ne s'execute qu'une fois : il s'arrete si ART-1001 a deja du stock.
-- Pour regenerer : vider la base de dev puis relancer l'application.
-- Mot de passe des nouveaux comptes : Tantana@2026 (comme R__donnees_test).
-- =====================================================================

-- ===== Utilisateurs supplementaires =====
INSERT INTO utilisateur (username, password, nom_complet, email, actif) VALUES
    ('vendeur2', '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Vendeur Analakely',   'vendeur2@tantana.test', TRUE),
    ('vendeur3', '{bcrypt}$2a$10$eUIvnVTsBqndGCtZBOdSVuKgeKwVeNkHcCwIdgXV2OYGzSe2jqFx6', 'Vendeur Behoririka',  'vendeur3@tantana.test', TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO utilisateur_role (utilisateur_id, role_id)
SELECT u.id, r.id
FROM (VALUES ('vendeur2', 'VENDEUR'), ('vendeur3', 'VENDEUR')) AS a (username, code)
JOIN utilisateur u ON u.username = a.username
JOIN role r        ON r.code     = a.code
ON CONFLICT DO NOTHING;

-- ===== Fournisseurs et transitaires =====
INSERT INTO fournisseur (nom, pays, devise, contact, telephone, email, actif) VALUES
    ('Shenzhen Mobile Parts',   'Chine',          'CNY', 'Zhang Min',     '+86 755 0000 0000', 'export@szmobile.test',  TRUE),
    ('Osaka Kitchenware',       'Japon',          'JPY', 'Kenji Mori',    '+81 6 0000 0000',   'sales@osakakw.test',    TRUE),
    ('Mumbai Textile Exports',  'Inde',           'INR', 'Ravi Patel',    '+91 22 0000 0000',  'orders@mte.test',       TRUE),
    ('Durban Agro Foods',       'Afrique du Sud', 'ZAR', 'Thabo Nkosi',   '+27 31 000 0000',   'trade@durbanagro.test', TRUE),
    ('Bangkok Rice Mill',       'Thailande',      'USD', 'Somchai K.',    '+66 2 000 0000',    'export@bkkrice.test',   TRUE),
    ('Global Tools Inc.',       'Etats-Unis',     'USD', 'Mike Johnson',  '+1 305 000 0000',   'intl@globaltools.test', TRUE),
    ('Antsirabe Distribution',  'Madagascar',     'MGA', 'Rakoto Be',     '+261 34 05 000 00', 'vente@antsirabe-distri.test', TRUE)
ON CONFLICT (nom) DO NOTHING;

INSERT INTO transitaire (nom, contact, telephone, email, adresse, actif) VALUES
    ('Sea Cargo Madagascar', 'Randria Fara', '+261 32 00 000 00', 'ops@seacargo.test',  'Port de Toamasina',  TRUE),
    ('Trans Hautes Terres',  'Rabe Solo',    '+261 34 00 111 11', 'route@tht.test',     'Antsirabe',          TRUE)
ON CONFLICT (nom) DO NOTHING;

-- ===== Catalogue de simulation =====
-- prix_devise : prix fournisseur ; coef : prix de vente = revient x coef (NULL = prix "A fixer") ;
-- rotation : ventes moyennes visees par mois (unites)
DROP TABLE IF EXISTS sim_catalogue;
CREATE TEMP TABLE sim_catalogue (
    reference   VARCHAR(50),
    designation VARCHAR(200),
    unite       VARCHAR(20),
    fournisseur VARCHAR(150),
    prix_devise NUMERIC,
    coef        NUMERIC,
    rotation    NUMERIC
);

INSERT INTO sim_catalogue VALUES
    ('ART-1001', 'Smartphone 6.7 pouces 256 Go',         'PIECE',  'Shenzhen Mobile Parts',  950,    1.35, 22),
    ('ART-1002', 'Tablette 10 pouces',                   'PIECE',  'Shenzhen Mobile Parts',  720,    1.35, 10),
    ('ART-1003', 'Batterie externe 20000 mAh',           'PIECE',  'Shenzhen Mobile Parts',  68,     1.60, 60),
    ('ART-1004', 'Coque de protection silicone',         'PIECE',  'Shenzhen Mobile Parts',  6,      2.20, 150),
    ('ART-1005', 'Montre connectee',                     'PIECE',  'Shenzhen Mobile Parts',  160,    1.50, 15),
    ('ART-1031', 'Ecouteurs a conduction osseuse',       'PIECE',  'Shenzhen Mobile Parts',  210,    NULL, 8),
    ('ART-1006', 'Ventilateur sur pied 40 cm',           'PIECE',  'Guangzhou Trading Co.',  85,     1.50, 30),
    ('ART-1007', 'Bouilloire electrique 1,7 L',          'PIECE',  'Guangzhou Trading Co.',  42,     1.55, 35),
    ('ART-1008', 'Ampoules LED 12 W (carton de 100)',    'CARTON', 'Guangzhou Trading Co.',  260,    1.45, 8),
    ('ART-1009', 'Multiprise 5 prises',                  'PIECE',  'Guangzhou Trading Co.',  18,     1.70, 70),
    ('ART-1010', 'Autocuiseur 6 L',                      'PIECE',  'Osaka Kitchenware',      4200,   1.45, 12),
    ('ART-1011', 'Set de couteaux de cuisine',           'LOT',    'Osaka Kitchenware',      2600,   1.50, 10),
    ('ART-1012', 'Lamba coton imprime',                  'PIECE',  'Mumbai Textile Exports', 180,    1.80, 120),
    ('ART-1013', 'Drap de lit 2 places',                 'PIECE',  'Mumbai Textile Exports', 520,    1.60, 40),
    ('ART-1014', 'Tissu coton au metre',                 'METRE',  'Mumbai Textile Exports', 95,     1.70, 300),
    ('ART-1015', 'Huile de tournesol bidon 20 L',        'PIECE',  'Durban Agro Foods',      520,    1.25, 60),
    ('ART-1016', 'Lait en poudre 25 kg',                 'SAC',    'Durban Agro Foods',      1250,   1.25, 25),
    ('ART-1017', 'Riz blanc importe 50 kg',              'SAC',    'Bangkok Rice Mill',      28,     1.20, 180),
    ('ART-1018', 'Riz parfume jasmin 25 kg',             'SAC',    'Bangkok Rice Mill',      21,     1.25, 80),
    ('ART-1019', 'Perceuse visseuse sans fil',           'PIECE',  'Global Tools Inc.',      38,     1.55, 12),
    ('ART-1020', 'Caisse a outils 82 pieces',            'LOT',    'Global Tools Inc.',      45,     1.50, 8),
    ('ART-1021', 'Groupe electrogene 3 kVA',             'PIECE',  'Global Tools Inc.',      310,    1.35, 4),
    ('ART-1022', 'Creme hydratante 200 ml',              'PIECE',  'Euro Import SARL',       4.2,    1.90, 60),
    ('ART-1023', 'Parfum 100 ml',                        'PIECE',  'Euro Import SARL',       18,     1.80, 20),
    ('ART-1024', 'Lait de toilette bebe 500 ml',         'PIECE',  'Euro Import SARL',       3.6,    1.80, 40),
    ('ART-1025', 'Climatiseur split 12000 BTU',          'PIECE',  'Dubai General Supply',   950,    1.30, 6),
    ('ART-1026', 'Televiseur LED 43 pouces',             'PIECE',  'Dubai General Supply',   780,    1.30, 10),
    ('ART-1027', 'Refrigerateur 180 L',                  'PIECE',  'Dubai General Supply',   1100,   1.30, 5),
    ('ART-1028', 'Savon de menage (carton de 48)',       'CARTON', 'Antsirabe Distribution', 62000,  1.20, 50),
    ('ART-1029', 'Eau minerale 1,5 L (pack de 6)',       'LOT',    'Antsirabe Distribution', 7200,   1.30, 200),
    ('ART-1030', 'Pates alimentaires (carton de 20)',    'CARTON', 'Antsirabe Distribution', 38000,  1.25, 40);

INSERT INTO article (reference, designation, unite, description, actif)
SELECT reference, designation, unite, 'Article de simulation', TRUE FROM sim_catalogue
ON CONFLICT (reference) DO NOTHING;

-- ===== Clients =====
INSERT INTO client (nom, type, telephone, email, adresse, nif, stat, plafond_credit, actif)
SELECT v.nom, v.type, v.tel, v.email, v.adresse, v.nif, v.stat, v.plafond, TRUE
FROM (VALUES
        ('Rakotomalala Fanja',          'PARTICULIER',   '+261 34 20 000 01', NULL, 'Ambohipo',          NULL,         NULL,                0),
        ('Randrianarisoa Tojo',         'PARTICULIER',   '+261 33 20 000 02', NULL, 'Ankadifotsy',       NULL,         NULL,                0),
        ('Rasoanaivo Lalao',            'PARTICULIER',   '+261 32 20 000 03', NULL, 'Ampefiloha',        NULL,         NULL,                0),
        ('Andriamanana Haja',           'PARTICULIER',   '+261 34 20 000 04', NULL, 'Itaosy',            NULL,         NULL,                0),
        ('Razafindrabe Voahangy',       'PARTICULIER',   '+261 34 20 000 05', NULL, 'Ivandry',           NULL,         NULL,                0),
        ('Rabemananjara Tiana',         'PARTICULIER',   '+261 33 20 000 06', NULL, 'Andravoahangy',     NULL,         NULL,                0),
        ('Ravelojaona Mamy',            'PARTICULIER',   '+261 32 20 000 07', NULL, 'Ambanidia',         NULL,         NULL,                0),
        ('Ranaivoson Nirina',           'PARTICULIER',   '+261 34 20 000 08', NULL, 'Mahamasina',        NULL,         NULL,                0),
        ('Rakotondrabe Sitraka',        'PARTICULIER',   '+261 33 20 000 09', NULL, 'Talatamaty',        NULL,         NULL,                0),
        ('Raharimalala Onja',           'PARTICULIER',   '+261 34 20 000 10', NULL, 'Ambatobe',          NULL,         NULL,                0),
        ('Epicerie Tsara Be',           'PROFESSIONNEL', '+261 34 30 000 01', 'tsarabe@client.test',     'Isotry',             '5000000001', '47111112025000001', 15000000),
        ('Quincaillerie Fanantenana',   'PROFESSIONNEL', '+261 33 30 000 02', 'fanantenana@client.test', 'Anosibe',            '5000000002', '47521112025000002', 25000000),
        ('Hotel Les Lemuriens',         'PROFESSIONNEL', '+261 32 30 000 03', 'achats@lemuriens.test',   'Antsahavola',        '5000000003', '55101112025000003', 40000000),
        ('Superette Ambohijatovo',      'PROFESSIONNEL', '+261 34 30 000 04', 'superette@client.test',   'Ambohijatovo',       '5000000004', '47111112025000004', 30000000),
        ('Electro Plus Antsirabe',      'PROFESSIONNEL', '+261 33 30 000 05', 'electroplus@client.test', 'Antsirabe',          '5000000005', '47431112025000005', 60000000),
        ('Restaurant Ny Kintana',       'PROFESSIONNEL', '+261 32 30 000 06', 'nykintana@client.test',   'Analakely',          '5000000006', '56101112025000006', 10000000),
        ('Boutique Mendrika',           'PROFESSIONNEL', '+261 34 30 000 07', 'mendrika@client.test',    'Behoririka',         '5000000007', '47711112025000007', 12000000),
        ('Grossiste Mahajanga Import',  'PROFESSIONNEL', '+261 33 30 000 08', 'mjg-import@client.test',  'Mahajanga',          '5000000008', '46391112025000008', 80000000),
        ('Parfumerie Soa',              'PROFESSIONNEL', '+261 32 30 000 09', 'soa@client.test',         'Tsaralalana',        '5000000009', '47751112025000009', 8000000),
        ('Cooperative Vatovavy',        'PROFESSIONNEL', '+261 34 30 000 10', 'vatovavy@client.test',    'Manakara',           '5000000010', '46211112025000010', 35000000)
     ) AS v (nom, type, tel, email, adresse, nif, stat, plafond)
WHERE NOT EXISTS (SELECT 1 FROM client c WHERE c.nom = v.nom)
  AND NOT EXISTS (SELECT 1 FROM client c WHERE c.nif = v.nif);

-- ===== Fonctions utilitaires (session uniquement) =====

-- Meme logique que NumerotationRepository.incrementer
CREATE OR REPLACE FUNCTION pg_temp.sim_num(p_prefixe TEXT, p_date DATE) RETURNS TEXT AS $f$
DECLARE
    v_dernier INTEGER;
BEGIN
    INSERT INTO numerotation (prefixe, annee, dernier) VALUES (p_prefixe, extract(year FROM p_date)::int, 1)
    ON CONFLICT (prefixe, annee) DO UPDATE SET dernier = numerotation.dernier + 1
    RETURNING dernier INTO v_dernier;
    RETURN format('%s-%s-%s', p_prefixe, extract(year FROM p_date)::int, lpad(v_dernier::text, 4, '0'));
END
$f$ LANGUAGE plpgsql;

-- Taux de reference (Ariary pour 1 unite de devise)
CREATE OR REPLACE FUNCTION pg_temp.sim_taux(p_devise TEXT) RETURNS NUMERIC AS $f$
    SELECT CASE p_devise WHEN 'USD' THEN 4500 WHEN 'EUR' THEN 5200 WHEN 'CNY' THEN 620 WHEN 'AED' THEN 1225
                         WHEN 'INR' THEN 54 WHEN 'JPY' THEN 30 WHEN 'ZAR' THEN 250 ELSE 1 END::numeric
$f$ LANGUAGE sql;

-- Entree en stock (achat ou retour client) : meme calcul que Stock.entrer
CREATE OR REPLACE FUNCTION pg_temp.sim_entree(p_article BIGINT, p_qte NUMERIC, p_achat NUMERIC, p_revient NUMERIC,
                                              p_type TEXT, p_ref TEXT, p_reception BIGINT, p_avoir BIGINT,
                                              p_user BIGINT, p_ts TIMESTAMP) RETURNS VOID AS $f$
DECLARE
    s stock%ROWTYPE;
    v_cmup NUMERIC;
    v_rev  NUMERIC;
BEGIN
    INSERT INTO stock (article_id, quantite, cmup, cmup_revient, date_maj) VALUES (p_article, 0, 0, 0, p_ts)
    ON CONFLICT (article_id) DO NOTHING;
    SELECT * INTO s FROM stock WHERE article_id = p_article FOR UPDATE;
    IF s.quantite <= 0 THEN
        v_cmup := round(p_achat, 4);
        v_rev  := round(p_revient, 4);
    ELSE
        v_cmup := round((s.quantite * s.cmup + p_qte * p_achat) / (s.quantite + p_qte), 4);
        v_rev  := round((s.quantite * s.cmup_revient + p_qte * p_revient) / (s.quantite + p_qte), 4);
    END IF;
    UPDATE stock SET quantite = quantite + p_qte, cmup = v_cmup, cmup_revient = v_rev,
                     date_maj = p_ts, version = version + 1
    WHERE id = s.id;
    INSERT INTO mouvement_stock (article_id, type, quantite, cout_unitaire, cout_revient_unitaire, quantite_apres,
                                 cmup_apres, cmup_revient_apres, reference, reception_id, avoir_id, utilisateur_id, date_mouvement)
    VALUES (p_article, p_type, p_qte, round(p_achat, 4), round(p_revient, 4), s.quantite + p_qte,
            v_cmup, v_rev, p_ref, p_reception, p_avoir, p_user, p_ts);
END
$f$ LANGUAGE plpgsql;

-- Sortie de stock pour une vente : au CMUP, sans le modifier
CREATE OR REPLACE FUNCTION pg_temp.sim_sortie(p_article BIGINT, p_qte NUMERIC, p_ref TEXT, p_vente BIGINT,
                                              p_user BIGINT, p_ts TIMESTAMP,
                                              OUT o_cmup NUMERIC, OUT o_revient NUMERIC) AS $f$
DECLARE
    s stock%ROWTYPE;
BEGIN
    SELECT * INTO s FROM stock WHERE article_id = p_article FOR UPDATE;
    UPDATE stock SET quantite = quantite - p_qte, date_maj = p_ts, version = version + 1 WHERE id = s.id;
    INSERT INTO mouvement_stock (article_id, type, quantite, cout_unitaire, cout_revient_unitaire, quantite_apres,
                                 cmup_apres, cmup_revient_apres, reference, vente_id, utilisateur_id, date_mouvement)
    VALUES (p_article, 'SORTIE_VENTE', p_qte, s.cmup, s.cmup_revient, s.quantite - p_qte,
            s.cmup, s.cmup_revient, p_ref, p_vente, p_user, p_ts);
    o_cmup := s.cmup;
    o_revient := s.cmup_revient;
END
$f$ LANGUAGE plpgsql;

-- Reference de paiement plausible selon le mode
CREATE OR REPLACE FUNCTION pg_temp.sim_ref_paiement(p_mode TEXT) RETURNS TEXT AS $f$
    SELECT CASE p_mode
        WHEN 'ESPECES'      THEN NULL
        WHEN 'CHEQUE'       THEN 'CHQ ' || lpad((floor(random()::numeric * 9999999))::text, 7, '0')
        WHEN 'VIREMENT'     THEN 'VIR-' || lpad((floor(random()::numeric * 999999))::text, 6, '0')
        ELSE upper(left(p_mode, 2)) || lpad((floor(random()::numeric * 99999999))::text, 8, '0')
    END
$f$ LANGUAGE sql;

-- ===== Plans de travail de la simulation =====
DROP TABLE IF EXISTS sim_plan_reception;
CREATE TEMP TABLE sim_plan_reception (commande_id BIGINT, jour DATE, fraction NUMERIC, etape INT);
DROP TABLE IF EXISTS sim_plan_paiement;
CREATE TEMP TABLE sim_plan_paiement (vente_id BIGINT, jour DATE, part NUMERIC, mode TEXT);
DROP TABLE IF EXISTS sim_plan_avoir;
CREATE TEMP TABLE sim_plan_avoir (vente_id BIGINT, jour DATE, genre TEXT);

-- ===== Generation =====
DO $sim$
DECLARE
    c_debut_ventes CONSTANT DATE := (date_trunc('month', CURRENT_DATE) - INTERVAL '11 months')::date;
    c_debut_achats CONSTANT DATE := (date_trunc('month', CURRENT_DATE) - INTERVAL '11 months')::date - 90;
    c_fin          CONSTANT DATE := CURRENT_DATE;
    c_saison       CONSTANT NUMERIC[] := ARRAY[0.80, 0.85, 0.95, 1.00, 1.05, 1.15, 0.95, 1.00, 1.10, 1.00, 1.20, 1.60];
    u_admin BIGINT; u_acheteur BIGINT; u_maker BIGINT; u_checker BIGINT;
    v_vendeurs BIGINT[];
    v_tva NUMERIC; v_remise_max NUMERIC;
    d DATE; ts TIMESTAMP;
    f RECORD; c RECORD; a RECORD; l RECORD; p RECORD; o RECORD;
    v_id BIGINT; v_num TEXT; v_taux NUMERIC; v_n INT; v_i INT; v_k INT;
    v_valeur NUMERIC; v_frais NUMERIC; v_mer BOOLEAN; v_jour DATE;
    v_client BIGINT; v_type TEXT; v_vendeur BIGINT; v_qte NUMERIC; v_remise NUMERIC; v_pu NUMERIC;
    v_controle TEXT; v_ttc NUMERIC; v_net NUMERIC; v_reste NUMERIC; v_mode TEXT; v_genre TEXT;
    v_ht NUMERIC; v_tva_av NUMERIC; v_cout NUMERIC; v_motif TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM mouvement_stock m JOIN article ar ON ar.id = m.article_id WHERE ar.reference = 'ART-1001') THEN
        RAISE NOTICE 'Simulation deja chargee : rien a faire.';
        RETURN;
    END IF;

    PERFORM setseed(0.2026);
    SELECT id INTO u_admin    FROM utilisateur WHERE username = 'admin';
    SELECT id INTO u_acheteur FROM utilisateur WHERE username = 'acheteur';
    SELECT id INTO u_maker    FROM utilisateur WHERE username = 'maker';
    SELECT id INTO u_checker  FROM utilisateur WHERE username = 'checker';
    SELECT array_agg(id ORDER BY username) INTO v_vendeurs
    FROM utilisateur WHERE username IN ('vendeur', 'vendeur2', 'vendeur3', 'polyvalent', 'maker');
    SELECT taux_tva, remise_max_vendeur INTO v_tva, v_remise_max FROM parametre WHERE id = 1;

    d := c_debut_achats;
    WHILE d <= c_fin LOOP
        ts := d + TIME '08:00';

        -- ---------- 1. Commandes d'achat (cycle par fournisseur) ----------
        FOR f IN
            SELECT fo.id, fo.devise, row_number() OVER (ORDER BY fo.nom) AS rang,
                   CASE fo.devise WHEN 'CNY' THEN 50 WHEN 'JPY' THEN 45 WHEN 'INR' THEN 40 WHEN 'USD' THEN 45
                                  WHEN 'ZAR' THEN 25 WHEN 'EUR' THEN 20 WHEN 'AED' THEN 21 ELSE 5 END AS delai,
                   CASE fo.devise WHEN 'MGA' THEN 14 ELSE 35 END AS cycle
            FROM fournisseur fo
            WHERE fo.nom IN (SELECT DISTINCT fournisseur FROM sim_catalogue)
        LOOP
            CONTINUE WHEN (d - c_debut_achats) % f.cycle <> (f.rang * 4) % f.cycle;
            ts := ts + INTERVAL '7 minutes';
            v_taux := round(pg_temp.sim_taux(f.devise) * (0.98 + random()::numeric * 0.04), 2);
            INSERT INTO commande_achat (numero, fournisseur_id, date_commande, date_arrivee_prevue, devise, taux_change,
                                        montant_total, statut, observations, cree_par_id, date_creation, date_modification)
            VALUES (pg_temp.sim_num('CA', d), f.id, d, d + f.delai, f.devise, v_taux, 0, 'COMMANDEE',
                    CASE WHEN f.delai >= 40 THEN 'Expedition maritime' WHEN f.devise = 'MGA' THEN 'Livraison par route'
                         ELSE 'Expedition aerienne' END,
                    CASE WHEN random()::numeric < 0.6 THEN u_acheteur ELSE u_maker END, ts, ts)
            RETURNING id INTO v_id;

            -- Reapprovisionnement "recompletement" : on commande de quoi couvrir le delai + le cycle,
            -- moins le stock et les quantites deja en commande. La consommation est estimee sur les
            -- ventes des 90 derniers jours (le catalogue sert de reference au demarrage).
            INSERT INTO ligne_commande_achat (commande_id, article_id, quantite_commandee, prix_unitaire)
            SELECT v_id, ar.id, q.qte, round(sc.prix_devise * (0.97 + random()::numeric * 0.08), 2)
            FROM sim_catalogue sc
            JOIN article ar ON ar.reference = sc.reference
            JOIN fournisseur fo ON fo.nom = sc.fournisseur
            LEFT JOIN stock s ON s.article_id = ar.id
            CROSS JOIN LATERAL (
                SELECT CASE WHEN d < c_debut_ventes + 60 THEN sc.rotation
                            ELSE greatest(sc.rotation * 0.5,
                                          coalesce((SELECT sum(lv.quantite) FROM ligne_vente lv JOIN vente v ON v.id = lv.vente_id
                                                    WHERE lv.article_id = ar.id AND v.statut = 'VALIDEE' AND v.date_vente > d - 90), 0) / 3.0)
                       END AS rot,
                       coalesce((SELECT sum(lc.quantite_commandee - lc.quantite_recue)
                                 FROM ligne_commande_achat lc JOIN commande_achat ca ON ca.id = lc.commande_id
                                 WHERE lc.article_id = ar.id AND ca.statut IN ('COMMANDEE', 'PARTIELLEMENT_RECUE')), 0) AS en_cours
            ) r
            CROSS JOIN LATERAL (
                SELECT round(greatest(0, r.rot * (f.cycle + f.delai) / 30.0 * 1.3 * (0.9 + random()::numeric * 0.2)
                                         - coalesce(s.quantite, 0) - r.en_cours) / 5) * 5 AS qte
            ) q
            WHERE fo.id = f.id AND q.qte >= 5
              AND (sc.coef IS NOT NULL OR d < c_debut_achats + f.cycle);
            GET DIAGNOSTICS v_n = ROW_COUNT;
            IF v_n = 0 THEN
                -- rien a commander : on annule la creation et on rend le numero (pas de trou dans la suite)
                DELETE FROM commande_achat WHERE id = v_id;
                UPDATE numerotation SET dernier = dernier - 1 WHERE prefixe = 'CA' AND annee = extract(year FROM d);
                CONTINUE;
            END IF;

            UPDATE commande_achat ca
            SET montant_total = (SELECT sum(quantite_commandee * prix_unitaire) FROM ligne_commande_achat WHERE commande_id = ca.id)
            WHERE ca.id = v_id;

            -- arrivee reelle : de 4 jours d'avance a 8 jours de retard ; 15 % des envois en deux fois
            v_jour := d + f.delai + floor(random()::numeric * 13)::int - 4;
            IF f.devise <> 'MGA' AND random()::numeric < 0.15 THEN
                INSERT INTO sim_plan_reception VALUES (v_id, v_jour, 0.6, 1), (v_id, v_jour + 12 + floor(random()::numeric * 10)::int, 1, 2);
            ELSE
                INSERT INTO sim_plan_reception VALUES (v_id, v_jour, 1, 1);
            END IF;
        END LOOP;

        -- ---------- 2. Receptions du jour (saisie maker, validation checker) ----------
        FOR p IN SELECT * FROM sim_plan_reception WHERE jour = d ORDER BY commande_id, etape LOOP
            SELECT * INTO c FROM commande_achat WHERE id = p.commande_id;
            CONTINUE WHEN c.statut NOT IN ('COMMANDEE', 'PARTIELLEMENT_RECUE');
            ts := ts + INTERVAL '25 minutes';
            v_mer := c.devise IN ('CNY', 'JPY', 'INR', 'USD', 'ZAR');
            v_taux := round(c.taux_change * (0.985 + random()::numeric * 0.035), 2);
            v_motif := CASE WHEN c.devise = 'MGA' THEN 'Trans Hautes Terres'
                            WHEN NOT v_mer THEN 'Ivato Fret Services'
                            WHEN random()::numeric < 0.5 THEN 'Transit Express Toamasina'
                            ELSE 'Sea Cargo Madagascar' END;
            SELECT id INTO v_id FROM transitaire WHERE nom = v_motif;

            -- de temps en temps, une premiere saisie rejetee par le validateur
            IF p.etape = 1 AND random()::numeric < 0.06 THEN
                INSERT INTO reception (numero, commande_id, transitaire_id, date_reception, taux_change, statut, observations,
                                       motif_rejet, saisi_par_id, date_saisie, valide_par_id, date_validation)
                VALUES (pg_temp.sim_num('BR', d), c.id, v_id, d, v_taux, 'REJETEE', NULL,
                        'Quantites inversees entre deux articles : merci de ressaisir selon le bordereau.',
                        u_maker, ts, u_checker, ts + INTERVAL '1 hour');
                ts := ts + INTERVAL '90 minutes';
            END IF;

            INSERT INTO reception (numero, commande_id, transitaire_id, date_reception, taux_change, statut, observations,
                                   saisi_par_id, date_saisie, valide_par_id, date_validation)
            VALUES (pg_temp.sim_num('BR', d), c.id, v_id, d, v_taux, 'VALIDEE',
                    CASE WHEN p.fraction < 1 THEN 'Premier conteneur, le reste suit' END,
                    u_maker, ts, u_checker, ts + INTERVAL '3 hours')
            RETURNING id, numero INTO v_id, v_num;

            INSERT INTO ligne_reception (reception_id, ligne_commande_id, quantite_recue, quantite_refusee, commentaire)
            SELECT v_id, t.id, t.cible - t.refus, t.refus,
                   CASE WHEN t.refus > 0 THEN 'Colis endommages a l''arrivee' END
            FROM (SELECT x.id, x.cible,
                         CASE WHEN random()::numeric < 0.08 AND x.cible >= 20 THEN greatest(1, round(x.cible * 0.02)) ELSE 0 END AS refus
                  FROM (SELECT lc.id,
                               CASE WHEN p.fraction < 1 THEN round(lc.quantite_commandee * p.fraction)
                                    ELSE lc.quantite_commandee - lc.quantite_recue END AS cible
                        FROM ligne_commande_achat lc WHERE lc.commande_id = c.id) x
                  WHERE x.cible > 0) t;

            -- frais du transitaire (Ariary)
            SELECT coalesce(sum(lr.quantite_recue * round(lc.prix_unitaire * v_taux, 4)), 0) INTO v_valeur
            FROM ligne_reception lr JOIN ligne_commande_achat lc ON lc.id = lr.ligne_commande_id
            WHERE lr.reception_id = v_id;

            IF c.devise = 'MGA' THEN
                INSERT INTO frais_reception (reception_id, type, montant, reference_facture) VALUES
                    (v_id, 'TRANSIT',     round(v_valeur * 0.02, -3) + 50000, 'THT-' || lpad((floor(random()::numeric * 99999))::text, 5, '0'));
            ELSE
                INSERT INTO frais_reception (reception_id, type, montant, reference_facture) VALUES
                    (v_id, 'FRET',        round(v_valeur * CASE WHEN v_mer THEN 0.07 ELSE 0.10 END, -3), 'FT-' || lpad((floor(random()::numeric * 99999))::text, 5, '0')),
                    (v_id, 'DOUANE',      round(v_valeur * 0.17, -3), 'DQ-' || extract(year FROM d) || '-' || lpad((floor(random()::numeric * 9999))::text, 4, '0')),
                    (v_id, 'TRANSIT',     round(v_valeur * 0.015, -3), 'FT-' || lpad((floor(random()::numeric * 99999))::text, 5, '0')),
                    (v_id, 'MANUTENTION', 150000 + round(random()::numeric * 300000, -3), NULL);
                IF d > c.date_arrivee_prevue + 5 THEN
                    INSERT INTO frais_reception (reception_id, type, montant, reference_facture, commentaire)
                    VALUES (v_id, 'STOCKAGE', 50000 * (d - c.date_arrivee_prevue), NULL, 'Magasinage au port');
                END IF;
            END IF;
            SELECT sum(montant) INTO v_frais FROM frais_reception WHERE reception_id = v_id;

            -- repartition des frais au prorata de la valeur, entrees en stock
            FOR l IN
                SELECT lr.id, lr.quantite_recue, lc.id AS lc_id, lc.article_id, round(lc.prix_unitaire * v_taux, 4) AS cout
                FROM ligne_reception lr JOIN ligne_commande_achat lc ON lc.id = lr.ligne_commande_id
                WHERE lr.reception_id = v_id AND lr.quantite_recue > 0
            LOOP
                v_pu := CASE WHEN v_valeur > 0 THEN round(v_frais * l.cout / v_valeur, 4) ELSE 0 END;
                UPDATE ligne_reception SET frais_unitaire = v_pu WHERE id = l.id;
                UPDATE ligne_commande_achat SET quantite_recue = quantite_recue + l.quantite_recue WHERE id = l.lc_id;
                PERFORM pg_temp.sim_entree(l.article_id, l.quantite_recue, l.cout, l.cout + v_pu,
                                           'ENTREE_ACHAT', v_num, v_id, NULL, u_checker, ts + INTERVAL '3 hours');
            END LOOP;

            IF NOT EXISTS (SELECT 1 FROM ligne_commande_achat WHERE commande_id = c.id AND quantite_recue < quantite_commandee) THEN
                UPDATE commande_achat SET statut = 'RECUE', date_modification = ts + INTERVAL '3 hours', version = version + 1 WHERE id = c.id;
            ELSIF EXISTS (SELECT 1 FROM sim_plan_reception WHERE commande_id = c.id AND jour > d) THEN
                UPDATE commande_achat SET statut = 'PARTIELLEMENT_RECUE', date_modification = ts + INTERVAL '3 hours', version = version + 1 WHERE id = c.id;
            ELSE
                -- reliquat refuse jamais relivre : la commande est cloturee
                UPDATE commande_achat
                SET statut = 'CLOTUREE', date_modification = ts + INTERVAL '1 day', version = version + 2,
                    observations = coalesce(observations || '. ', '') || 'Cloturee : reliquat refuse non relivre'
                WHERE id = c.id;
            END IF;

            -- prix de vente initial des nouveaux articles
            FOR a IN
                SELECT ar.id, ar.prix_vente_ht, s.cmup_revient, sc.coef
                FROM ligne_reception lr
                JOIN ligne_commande_achat lc ON lc.id = lr.ligne_commande_id
                JOIN article ar ON ar.id = lc.article_id
                JOIN sim_catalogue sc ON sc.reference = ar.reference
                JOIN stock s ON s.article_id = ar.id
                WHERE lr.reception_id = v_id AND ar.prix_vente_ht IS NULL AND sc.coef IS NOT NULL
            LOOP
                v_pu := round(a.cmup_revient * a.coef, -2);
                UPDATE article SET prix_vente_ht = v_pu WHERE id = a.id;
                INSERT INTO historique_prix_vente (article_id, ancien_prix, nouveau_prix, cmup_revient, motif, modifie_par_id, date_modification)
                VALUES (a.id, NULL, v_pu, a.cmup_revient, 'Prix initial apres premiere reception', u_admin, ts + INTERVAL '4 hours');
            END LOOP;
        END LOOP;

        -- ---------- 3. Revision mensuelle des prix (le 1er du mois) ----------
        IF extract(day FROM d) = 1 AND d >= c_debut_ventes THEN
            FOR a IN
                SELECT ar.id, ar.prix_vente_ht, s.cmup_revient, round(s.cmup_revient * sc.coef, -2) AS cible
                FROM article ar
                JOIN sim_catalogue sc ON sc.reference = ar.reference
                JOIN stock s ON s.article_id = ar.id
                WHERE ar.prix_vente_ht IS NOT NULL AND sc.coef IS NOT NULL AND s.cmup_revient > 0
            LOOP
                CONTINUE WHEN abs(a.cible - a.prix_vente_ht) <= a.prix_vente_ht * 0.04;
                UPDATE article SET prix_vente_ht = a.cible WHERE id = a.id;
                INSERT INTO historique_prix_vente (article_id, ancien_prix, nouveau_prix, cmup_revient, motif, modifie_par_id, date_modification)
                VALUES (a.id, a.prix_vente_ht, a.cible, a.cmup_revient,
                        CASE WHEN a.cible > a.prix_vente_ht THEN 'Hausse du cout de revient (taux de change, fret)'
                             ELSE 'Baisse du cout de revient : prix aligne' END,
                        u_admin, d + TIME '07:30');
            END LOOP;
        END IF;

        -- ---------- 4. Avoirs prevus ce jour ----------
        FOR p IN SELECT * FROM sim_plan_avoir WHERE jour = d AND jour <= c_fin LOOP
            SELECT * INTO o FROM vente WHERE id = p.vente_id;
            v_genre := CASE WHEN p.genre = 'TOTAL' AND (o.montant_paye > 0 OR o.montant_avoirs > 0) THEN 'RETOUR' ELSE p.genre END;
            v_motif := CASE v_genre WHEN 'TOTAL'  THEN 'Annulation de la facture a la demande du client'
                                    WHEN 'RETOUR' THEN 'Retour de marchandise defectueuse'
                                    ELSE 'Geste commercial : retard de livraison' END;
            ts := ts + INTERVAL '11 minutes';
            INSERT INTO avoir (numero, vente_id, date_avoir, motif, taux_tva, total_ht, total_tva, total_ttc,
                               cout_revient_total, saisi_par_id, date_creation)
            VALUES (pg_temp.sim_num('AV', d), o.id, d, v_motif, o.taux_tva, 0, 0, 0, 0, o.vendeur_id, ts)
            RETURNING id, numero INTO v_id, v_num;

            IF v_genre = 'TOTAL' THEN
                INSERT INTO ligne_avoir (avoir_id, ligne_vente_id, quantite, montant_ht, retour_stock)
                SELECT v_id, lv.id, lv.quantite - lv.quantite_avoir,
                       round(lv.prix_unitaire_ht * (lv.quantite - lv.quantite_avoir), 2), TRUE
                FROM ligne_vente lv WHERE lv.vente_id = o.id AND lv.quantite > lv.quantite_avoir;
            ELSE
                INSERT INTO ligne_avoir (avoir_id, ligne_vente_id, quantite, montant_ht, retour_stock)
                SELECT v_id, x.id, x.q, round(x.prix_unitaire_ht * x.q, 2), v_genre = 'RETOUR'
                FROM (SELECT lv.id, lv.prix_unitaire_ht,
                             CASE WHEN v_genre = 'RETOUR' THEN least(lv.quantite - lv.quantite_avoir, greatest(1, floor(lv.quantite * 0.2)))
                                  ELSE 1 END AS q
                      FROM ligne_vente lv WHERE lv.vente_id = o.id AND lv.quantite > lv.quantite_avoir
                      ORDER BY random()::numeric LIMIT 1) x;
            END IF;

            UPDATE ligne_vente lv SET quantite_avoir = lv.quantite_avoir + la.quantite
            FROM ligne_avoir la WHERE la.avoir_id = v_id AND la.ligne_vente_id = lv.id;

            SELECT round(sum(la.montant_ht)), round(sum(la.quantite * lv.cout_revient_unitaire))
            INTO v_ht, v_cout
            FROM ligne_avoir la JOIN ligne_vente lv ON lv.id = la.ligne_vente_id WHERE la.avoir_id = v_id;
            v_tva_av := round(v_ht * o.taux_tva / 100);
            v_ttc := v_ht + v_tva_av;
            IF NOT EXISTS (SELECT 1 FROM ligne_vente WHERE vente_id = o.id AND quantite > quantite_avoir) THEN
                -- dernier avoir qui solde la facture : ajuste a l'Ariary pres
                v_ht := o.total_ht - coalesce((SELECT sum(total_ht) FROM avoir WHERE vente_id = o.id AND id <> v_id), 0);
                v_ttc := o.total_ttc - o.montant_avoirs;
                v_tva_av := v_ttc - v_ht;
            END IF;
            UPDATE avoir SET total_ht = v_ht, total_tva = v_tva_av, total_ttc = v_ttc, cout_revient_total = v_cout WHERE id = v_id;
            UPDATE vente SET montant_avoirs = montant_avoirs + v_ttc, version = version + 1 WHERE id = o.id;

            FOR l IN
                SELECT la.quantite, lv.article_id, lv.cout_achat_unitaire, lv.cout_revient_unitaire
                FROM ligne_avoir la JOIN ligne_vente lv ON lv.id = la.ligne_vente_id
                WHERE la.avoir_id = v_id AND la.retour_stock
            LOOP
                PERFORM pg_temp.sim_entree(l.article_id, l.quantite, l.cout_achat_unitaire, l.cout_revient_unitaire,
                                           'RETOUR_VENTE', v_num, NULL, v_id, o.vendeur_id, ts);
            END LOOP;
        END LOOP;

        -- ---------- 5. Ventes du jour ----------
        IF d >= c_debut_ventes THEN
            v_n := floor(4.5 * c_saison[extract(month FROM d)::int]
                         * CASE extract(dow FROM d) WHEN 0 THEN 0.25 WHEN 6 THEN 1.3 ELSE 1 END
                         * (1 + 0.3 * (d - c_debut_ventes) / 365.0)
                         + random()::numeric * 3)::int;
            ts := d + TIME '08:30';
            FOR v_i IN 1 .. v_n LOOP
                ts := ts + (5 + floor(random()::numeric * 40)::int) * INTERVAL '1 minute';
                IF random()::numeric < 0.35 THEN
                    SELECT id, 'COMPTOIR' INTO v_client, v_type FROM client WHERE comptoir;
                ELSIF random()::numeric < 0.45 THEN
                    SELECT id, 'PARTICULIER' INTO v_client, v_type FROM client
                    WHERE type = 'PARTICULIER' AND NOT comptoir AND actif ORDER BY random()::numeric LIMIT 1;
                ELSE
                    SELECT id, 'PROFESSIONNEL' INTO v_client, v_type FROM client
                    WHERE type = 'PROFESSIONNEL' AND actif ORDER BY random()::numeric LIMIT 1;
                END IF;
                v_vendeur := v_vendeurs[1 + floor(random()::numeric * array_length(v_vendeurs, 1))::int];

                INSERT INTO vente (client_id, date_vente, statut, taux_tva, vendeur_id, date_creation)
                VALUES (v_client, d, 'BROUILLON', v_tva, v_vendeur, ts)
                RETURNING id INTO v_id;

                v_controle := '';
                v_k := CASE WHEN v_type = 'PROFESSIONNEL' THEN 2 + floor(random()::numeric * 3)::int ELSE 1 + (random()::numeric < 0.3)::int END;
                FOR a IN
                    SELECT ar.id, ar.reference, ar.prix_vente_ht, s.quantite, s.cmup_revient, sc.rotation
                    FROM article ar
                    JOIN sim_catalogue sc ON sc.reference = ar.reference
                    JOIN stock s ON s.article_id = ar.id
                    WHERE s.quantite >= 1 AND ar.prix_vente_ht IS NOT NULL AND ar.actif
                    ORDER BY -ln(1 - random()::numeric) / sc.rotation
                    LIMIT v_k
                LOOP
                    v_qte := CASE WHEN v_type = 'PROFESSIONNEL' THEN greatest(1, round(a.rotation / 30.0 * (1 + random()::numeric * 4)))
                                  ELSE 1 + (random()::numeric < 0.2)::int END;
                    v_qte := least(v_qte, floor(a.quantite));
                    v_remise := CASE WHEN v_type = 'PROFESSIONNEL' THEN
                                         CASE WHEN random()::numeric < 0.03 THEN 15 WHEN random()::numeric < 0.35 THEN 0
                                              WHEN random()::numeric < 0.5 THEN 5 ELSE 10 END
                                     WHEN random()::numeric < 0.12 THEN 5 ELSE 0 END;
                    v_pu := round(a.prix_vente_ht * (100 - v_remise) / 100, 2);
                    INSERT INTO ligne_vente (vente_id, article_id, quantite, prix_catalogue_ht, remise_pct, prix_unitaire_ht, montant_ht)
                    VALUES (v_id, a.id, v_qte, a.prix_vente_ht, v_remise, v_pu, round(v_pu * v_qte, 2));
                    IF v_remise > v_remise_max THEN
                        v_controle := v_controle || a.reference || ' : remise de ' || v_remise || ' % (plafond vendeur '
                                      || trim_scale(v_remise_max) || ' %). ';
                    END IF;
                    IF v_pu < a.cmup_revient THEN
                        v_controle := v_controle || a.reference || ' : prix net inferieur au prix de revient (vente a perte). ';
                    END IF;
                END LOOP;

                IF NOT EXISTS (SELECT 1 FROM ligne_vente WHERE vente_id = v_id) THEN
                    DELETE FROM vente WHERE id = v_id;
                    CONTINUE;
                END IF;

                UPDATE vente ve
                SET total_brut_ht = t.brut, total_ht = t.ht, total_remise = greatest(t.brut - t.ht, 0),
                    total_tva = round(t.ht * ve.taux_tva / 100), total_ttc = t.ht + round(t.ht * ve.taux_tva / 100)
                FROM (SELECT round(sum(prix_catalogue_ht * quantite)) AS brut, round(sum(montant_ht)) AS ht
                      FROM ligne_vente WHERE vente_id = v_id) t
                WHERE ve.id = v_id;

                -- fin de periode : quelques ventes restent en cours
                IF d > c_fin - 3 AND random()::numeric < 0.25 THEN
                    IF v_controle <> '' THEN
                        UPDATE vente SET statut = 'A_VALIDER', motif_controle = trim(v_controle) WHERE id = v_id;
                    END IF;
                    CONTINUE;
                END IF;

                IF v_controle <> '' THEN
                    IF random()::numeric < 0.1 THEN
                        -- refusee par le controleur : retour en brouillon, jamais revalidee
                        UPDATE vente SET motif_controle = trim(v_controle), controleur_id = u_checker,
                                         date_controle = ts + INTERVAL '40 minutes',
                                         commentaire_controle = 'Refuse : remise non justifiee, revoir le prix avec le client.'
                        WHERE id = v_id;
                        CONTINUE;
                    END IF;
                    UPDATE vente SET motif_controle = trim(v_controle), controleur_id = u_checker,
                                     date_controle = ts + INTERVAL '40 minutes',
                                     commentaire_controle = (ARRAY['Accorde : client fidele',
                                                                   'Accorde : volume important',
                                                                   'Accorde : alignement sur la concurrence',
                                                                   'Accorde : destockage'])[1 + floor(random()::numeric * 4)::int]
                    WHERE id = v_id;
                END IF;

                -- validation : numero de facture, couts figes, sorties de stock
                v_num := pg_temp.sim_num('FV', d);
                FOR l IN SELECT id, article_id, quantite FROM ligne_vente WHERE vente_id = v_id LOOP
                    SELECT * INTO o FROM pg_temp.sim_sortie(l.article_id, l.quantite, v_num, v_id, v_vendeur, ts + INTERVAL '45 minutes');
                    UPDATE ligne_vente SET cout_achat_unitaire = o.o_cmup, cout_revient_unitaire = o.o_revient WHERE id = l.id;
                END LOOP;
                UPDATE vente ve
                SET statut = 'VALIDEE', numero = v_num, date_validation = ts + INTERVAL '45 minutes', version = version + 1,
                    cout_revient_total = t.cout, marge_totale = ve.total_ht - t.cout
                FROM (SELECT round(sum(quantite * cout_revient_unitaire)) AS cout FROM ligne_vente WHERE vente_id = v_id) t
                WHERE ve.id = v_id
                RETURNING ve.total_ttc INTO v_ttc;

                -- paiement
                IF v_type <> 'PROFESSIONNEL' OR random()::numeric < 0.4 THEN
                    v_mode := CASE WHEN v_type = 'PROFESSIONNEL' THEN (ARRAY['VIREMENT', 'CHEQUE', 'ESPECES'])[1 + floor(random()::numeric * 3)::int]
                                   ELSE (ARRAY['ESPECES', 'ESPECES', 'MVOLA', 'MVOLA', 'ORANGE_MONEY', 'AIRTEL_MONEY'])[1 + floor(random()::numeric * 6)::int] END;
                    INSERT INTO paiement (vente_id, date_paiement, montant, mode, reference, saisi_par_id, date_saisie)
                    VALUES (v_id, d, v_ttc, v_mode, pg_temp.sim_ref_paiement(v_mode), v_vendeur, ts + INTERVAL '50 minutes');
                    UPDATE vente SET montant_paye = v_ttc WHERE id = v_id;
                    IF random()::numeric < 0.005 THEN
                        INSERT INTO paiement (vente_id, date_paiement, montant, mode, reference, saisi_par_id, date_saisie,
                                              annule, motif_annulation, annule_par_id, date_annulation)
                        VALUES (v_id, d, v_ttc, v_mode, pg_temp.sim_ref_paiement(v_mode), v_vendeur, ts + INTERVAL '52 minutes',
                                TRUE, 'Doublon de saisie', u_admin, ts + INTERVAL '2 hours');
                    END IF;
                ELSE
                    v_mode := CASE WHEN random()::numeric < 0.7 THEN 'VIREMENT' ELSE 'CHEQUE' END;
                    v_qte := random()::numeric;
                    IF v_qte < 0.65 THEN
                        INSERT INTO sim_plan_paiement VALUES (v_id, d + 15 + floor(random()::numeric * 25)::int, 1, v_mode);
                    ELSIF v_qte < 0.9 THEN
                        INSERT INTO sim_plan_paiement VALUES (v_id, d + 10 + floor(random()::numeric * 10)::int, 0.5, v_mode),
                                                             (v_id, d + 30 + floor(random()::numeric * 30)::int, 1, v_mode);
                    ELSE
                        -- client mauvais payeur : acompte seulement
                        INSERT INTO sim_plan_paiement VALUES (v_id, d + 20 + floor(random()::numeric * 20)::int, 0.3, v_mode);
                    END IF;
                END IF;

                -- avoirs : surtout sur les factures pros a credit (avant reglement), rarement apres un paiement comptant
                IF random()::numeric < (CASE WHEN EXISTS (SELECT 1 FROM sim_plan_paiement WHERE vente_id = v_id) THEN 0.07 ELSE 0.003 END) THEN
                    INSERT INTO sim_plan_avoir VALUES (v_id, d + 2 + floor(random()::numeric * 10)::int,
                        CASE WHEN random()::numeric < 0.2 THEN 'TOTAL' WHEN random()::numeric < 0.6 THEN 'RETOUR' ELSE 'GESTE' END);
                END IF;
            END LOOP;
        END IF;

        -- ---------- 6. Encaissements a credit prevus ce jour ----------
        FOR p IN SELECT * FROM sim_plan_paiement WHERE jour = d ORDER BY vente_id, part LOOP
            SELECT * INTO o FROM vente WHERE id = p.vente_id;
            v_net := greatest(o.total_ttc - o.montant_avoirs, 0);
            v_reste := v_net - o.montant_paye;
            CONTINUE WHEN v_reste <= 0;
            v_ttc := CASE WHEN p.part >= 1 THEN v_reste ELSE least(v_reste, round(v_net * p.part, -3)) END;
            CONTINUE WHEN v_ttc <= 0;
            INSERT INTO paiement (vente_id, date_paiement, montant, mode, reference, saisi_par_id, date_saisie)
            VALUES (o.id, d, v_ttc, p.mode, pg_temp.sim_ref_paiement(p.mode), u_admin, d + TIME '16:00');
            UPDATE vente SET montant_paye = montant_paye + v_ttc, version = version + 1 WHERE id = o.id;
        END LOOP;

        -- ---------- 7. Commande annulee (il y a 20 jours) ----------
        IF d = c_fin - 20 THEN
            INSERT INTO commande_achat (numero, fournisseur_id, date_commande, date_arrivee_prevue, devise, taux_change,
                                        montant_total, statut, observations, cree_par_id, date_creation, date_modification)
            SELECT pg_temp.sim_num('CA', d), fo.id, d, d + 45, 'USD', 4480, 0, 'ANNULEE',
                   'Annulee : fournisseur en rupture sur les groupes electrogenes', u_acheteur, d + TIME '10:00', d + TIME '15:00'
            FROM fournisseur fo WHERE fo.nom = 'Global Tools Inc.'
            RETURNING id INTO v_id;
            INSERT INTO ligne_commande_achat (commande_id, article_id, quantite_commandee, prix_unitaire)
            SELECT v_id, id, 10, 305 FROM article WHERE reference = 'ART-1021';
            UPDATE commande_achat SET montant_total = 3050 WHERE id = v_id;
        END IF;

        d := d + 1;
    END LOOP;

    -- ===== Fin de periode : documents en attente =====
    -- Commande en brouillon (pas encore envoyee au fournisseur)
    INSERT INTO commande_achat (numero, fournisseur_id, date_commande, date_arrivee_prevue, devise, taux_change,
                                montant_total, statut, observations, cree_par_id, date_creation)
    SELECT pg_temp.sim_num('CA', c_fin), fo.id, c_fin, c_fin + 25, 'ZAR', 252, 0, 'BROUILLON',
           'A valider avec la direction avant envoi', u_maker, c_fin + TIME '09:15'
    FROM fournisseur fo WHERE fo.nom = 'Durban Agro Foods'
    RETURNING id INTO v_id;
    INSERT INTO ligne_commande_achat (commande_id, article_id, quantite_commandee, prix_unitaire)
    SELECT v_id, ar.id, x.q, x.pu
    FROM (VALUES ('ART-1015', 120, 515), ('ART-1016', 40, 1240)) AS x (ref, q, pu)
    JOIN article ar ON ar.reference = x.ref;
    UPDATE commande_achat ca
    SET montant_total = (SELECT sum(quantite_commandee * prix_unitaire) FROM ligne_commande_achat WHERE commande_id = ca.id)
    WHERE ca.id = v_id;

    -- Vente avec remise hors plafond, en attente du controleur
    INSERT INTO vente (client_id, date_vente, statut, taux_tva, vendeur_id, date_creation, observations)
    SELECT cl.id, c_fin, 'BROUILLON', v_tva, v_vendeurs[1], c_fin + TIME '11:20', 'Commande pour l''ouverture d''un nouveau point de vente'
    FROM client cl WHERE cl.nom = 'Superette Ambohijatovo'
    RETURNING id INTO v_id;
    INSERT INTO ligne_vente (vente_id, article_id, quantite, prix_catalogue_ht, remise_pct, prix_unitaire_ht, montant_ht)
    SELECT v_id, ar.id, x.q, ar.prix_vente_ht, 15, round(ar.prix_vente_ht * 0.85, 2), round(round(ar.prix_vente_ht * 0.85, 2) * x.q, 2)
    FROM (VALUES ('ART-1015', 20), ('ART-1028', 15), ('ART-1029', 60)) AS x (ref, q)
    JOIN article ar ON ar.reference = x.ref AND ar.prix_vente_ht IS NOT NULL;
    UPDATE vente ve
    SET total_brut_ht = t.brut, total_ht = t.ht, total_remise = greatest(t.brut - t.ht, 0),
        total_tva = round(t.ht * ve.taux_tva / 100), total_ttc = t.ht + round(t.ht * ve.taux_tva / 100),
        statut = 'A_VALIDER',
        motif_controle = 'Remise de 15 % sur toutes les lignes (plafond vendeur ' || trim_scale(v_remise_max) || ' %).'
    FROM (SELECT round(sum(prix_catalogue_ht * quantite)) AS brut, round(sum(montant_ht)) AS ht
          FROM ligne_vente WHERE vente_id = v_id) t
    WHERE ve.id = v_id;

    -- Reception saisie par "maker", en attente de validation par "checker"
    SELECT ca.* INTO c FROM commande_achat ca
    JOIN sim_plan_reception pr ON pr.commande_id = ca.id AND pr.etape = 1 AND pr.jour > c_fin
    WHERE ca.statut = 'COMMANDEE' AND ca.date_arrivee_prevue <= c_fin + 3
    ORDER BY ca.date_arrivee_prevue LIMIT 1;
    IF FOUND THEN
        INSERT INTO reception (numero, commande_id, transitaire_id, date_reception, taux_change, statut, observations, saisi_par_id, date_saisie)
        SELECT pg_temp.sim_num('BR', c_fin), c.id, t.id, c_fin, round(c.taux_change * 1.01, 2), 'SAISIE',
               'Arrivage complet, en attente de controle', u_maker, c_fin + TIME '10:30'
        FROM transitaire t WHERE t.nom = 'Transit Express Toamasina'
        RETURNING id INTO v_id;
        INSERT INTO ligne_reception (reception_id, ligne_commande_id, quantite_recue, quantite_refusee)
        SELECT v_id, lc.id, lc.quantite_commandee, 0 FROM ligne_commande_achat lc WHERE lc.commande_id = c.id;
        INSERT INTO frais_reception (reception_id, type, montant, reference_facture)
        SELECT v_id, x.type, round(c.montant_total * c.taux_change * x.pct, -3), x.ref
        FROM (VALUES ('FRET', 0.07, 'FT-90001'), ('DOUANE', 0.17, 'DQ-EN-COURS'), ('TRANSIT', 0.015, 'FT-90002')) AS x (type, pct, ref);
        DELETE FROM sim_plan_reception WHERE commande_id = c.id;
    END IF;

    RAISE NOTICE 'Simulation : % commandes, % receptions, % ventes validees, % paiements, % avoirs',
        (SELECT count(*) FROM commande_achat), (SELECT count(*) FROM reception),
        (SELECT count(*) FROM vente WHERE statut = 'VALIDEE'), (SELECT count(*) FROM paiement),
        (SELECT count(*) FROM avoir);
END
$sim$;

DROP TABLE IF EXISTS sim_catalogue;
DROP TABLE IF EXISTS sim_plan_reception;
DROP TABLE IF EXISTS sim_plan_paiement;
DROP TABLE IF EXISTS sim_plan_avoir;
