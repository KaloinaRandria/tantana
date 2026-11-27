-- =====================================================================
-- V12 : reference des articles generee automatiquement (ART-0001, ...)
--   Compteur unique (non remis a zero chaque annee) : annee = 0 dans la
--   table numerotation. Il demarre au plus grand numero ART-xxxx existant
--   pour ne jamais reattribuer une reference deja en base.
-- =====================================================================

INSERT INTO numerotation (prefixe, annee, dernier)
SELECT 'ART', 0,
       COALESCE(MAX(CAST(SUBSTRING(reference FROM '^ART-([0-9]{1,9})$') AS INTEGER)), 0)
FROM article
ON CONFLICT (prefixe, annee) DO NOTHING;
