-- =====================================================================
-- V2 : roles de double controle (principe des 4 yeux)
--   MAKER   : saisit / cree une operation (vente, achat, ajustement...)
--   CHECKER : valide ou rejette une operation saisie par un MAKER
-- Regle metier a appliquer plus tard dans les services :
-- un utilisateur ne doit jamais valider une operation qu'il a lui-meme creee.
-- =====================================================================

INSERT INTO role (code, libelle) VALUES
    ('MAKER',   'Initiateur (création des opérations)'),
    ('CHECKER', 'Validateur (validation des opérations)')
ON CONFLICT (code) DO NOTHING;
