-- =====================================================================
-- V11 : gestion des utilisateurs par l'ADMIN
--
--   - mot de passe temporaire : genere par le systeme a la creation ou a la
--     reinitialisation ; l'utilisateur doit le changer a sa connexion suivante
--   - suivi : date du dernier changement de mot de passe, derniere connexion
--   - description des roles, affichee dans l'ecran d'attribution des roles
-- Pas de suppression d'utilisateur : on desactive (tracabilite des operations).
-- =====================================================================

ALTER TABLE utilisateur ADD COLUMN doit_changer_mot_de_passe BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE utilisateur ADD COLUMN date_mot_de_passe TIMESTAMP;
ALTER TABLE utilisateur ADD COLUMN derniere_connexion TIMESTAMP;
ALTER TABLE utilisateur ADD COLUMN date_modification TIMESTAMP;

ALTER TABLE role ADD COLUMN description VARCHAR(255);

UPDATE role SET description = CASE code
    WHEN 'ADMIN'              THEN 'Accès complet : paramètres, prix de vente, utilisateurs et tableaux de bord.'
    WHEN 'GESTIONNAIRE_STOCK' THEN 'Consulte le stock, ouvre et compte les inventaires.'
    WHEN 'VENDEUR'            THEN 'Saisit les ventes, gère les clients, les encaissements et les avoirs.'
    WHEN 'ACHETEUR'           THEN 'Gère les commandes d''achat, les fournisseurs et les transitaires.'
    WHEN 'MAKER'              THEN 'Saisit les opérations soumises à validation (réceptions…).'
    WHEN 'CHECKER'            THEN 'Valide ou rejette les opérations saisies par d''autres (réceptions, ventes, inventaires).'
    END;
