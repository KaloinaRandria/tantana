-- Livraison d'une vente : lieu ou livrer et frais factures au client (0 = pas de frais).
-- Les frais font partie du total HT de la facture (soumis a la TVA de la vente).
ALTER TABLE vente
    ADD COLUMN lieu_livraison  VARCHAR(255),
    ADD COLUMN frais_livraison NUMERIC(18, 2) NOT NULL DEFAULT 0 CHECK (frais_livraison >= 0);
