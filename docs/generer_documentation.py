# -*- coding: utf-8 -*-
"""Genere docs/Documentation_Tantana.xlsx : documentation metier complete, un onglet par theme."""
import sys
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from openpyxl.comments import Comment

OUT = sys.argv[1]

FONT = "Arial"
NAVY = "1F3864"
F_TITLE = Font(name=FONT, size=16, bold=True, color=NAVY)
F_SUB = Font(name=FONT, size=10, italic=True, color="595959")
F_H2 = Font(name=FONT, size=12, bold=True, color=NAVY)
F_HEAD = Font(name=FONT, size=10, bold=True, color="FFFFFF")
F_BODY = Font(name=FONT, size=10)
F_BOLD = Font(name=FONT, size=10, bold=True)
F_INPUT = Font(name=FONT, size=10, color="0000FF")
F_LINK = Font(name=FONT, size=10, color="0563C1", underline="single")
FILL_HEAD = PatternFill("solid", fgColor=NAVY)
FILL_EXPL_HEAD = PatternFill("solid", fgColor="7F6000")
FILL_EXPL = PatternFill("solid", fgColor="FFF2CC")
FILL_INPUT = PatternFill("solid", fgColor="FFFF00")
FILL_TOTAL = PatternFill("solid", fgColor="D9E1F2")
FILL_ZEBRA = PatternFill("solid", fgColor="F2F2F2")
THIN = Side(style="thin", color="BFBFBF")
BORDER = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)
WRAP = Alignment(wrap_text=True, vertical="top")
CENTER = Alignment(horizontal="center", vertical="center", wrap_text=True)

MGA = '#,##0;-#,##0;"-"'
MGA2 = '#,##0.00;-#,##0.00;"-"'
MGA4 = '#,##0.0000;-#,##0.0000;"-"'
QTE = '#,##0.###;-#,##0.###;"-"'
PCT = '0.0" %";-0.0" %";"-"'

wb = Workbook()
wb.remove(wb.active)
SOMMAIRE = []  # (onglet, description)


def sheet(name, title, subtitle, description):
    ws = wb.create_sheet(name)
    ws.sheet_view.showGridLines = False
    ws["A1"] = title
    ws["A1"].font = F_TITLE
    ws["A2"] = subtitle
    ws["A2"].font = F_SUB
    ws["A3"] = "← Retour au sommaire"
    ws["A3"].hyperlink = "#'Sommaire'!A1"
    ws["A3"].font = F_LINK
    SOMMAIRE.append((name, description))
    return ws


def h2(ws, row, text):
    ws.cell(row=row, column=1, value=text).font = F_H2
    return row + 1


def table(ws, row, headers, rows, widths=None, expl_last=True):
    """Ecrit un tableau ; la derniere colonne (Explication) est mise en valeur."""
    n = len(headers)
    for c, h in enumerate(headers, 1):
        cell = ws.cell(row=row, column=c, value=h)
        cell.font = F_HEAD
        cell.fill = FILL_EXPL_HEAD if (expl_last and c == n) else FILL_HEAD
        cell.alignment = CENTER
        cell.border = BORDER
    ws.row_dimensions[row].height = 30
    for i, r in enumerate(rows):
        rr = row + 1 + i
        for c, v in enumerate(r, 1):
            cell = ws.cell(row=rr, column=c, value=v)
            cell.font = F_BODY
            cell.alignment = WRAP
            cell.border = BORDER
            if expl_last and c == n:
                cell.fill = FILL_EXPL
            elif i % 2 == 1:
                cell.fill = FILL_ZEBRA
    if widths:
        for c, w in enumerate(widths, 1):
            cur = ws.column_dimensions[get_column_letter(c)].width or 0
            ws.column_dimensions[get_column_letter(c)].width = max(cur, w)
    return row + len(rows) + 2


# =====================================================================
# Sommaire (rempli a la fin)
# =====================================================================
ws_sommaire = wb.create_sheet("Sommaire")

# =====================================================================
# 1. Vue d'ensemble
# =====================================================================
ws = sheet("Vue d'ensemble", "Vue d'ensemble de l'application Tantana",
           "Chaîne Achat → Réception → Stock → Vente → Encaissement / Avoir, et Inventaire",
           "Les modules, leur enchaînement et les 3 seuls moments où le stock bouge")
r = 5
r = h2(ws, r, "Modules de l'application")
r = table(ws, r, ["Module", "URL", "Rôle(s) d'accès", "Documents produits", "Fait bouger le stock ?", "Explication"], [
    ["Articles (catalogue)", "/articles", "ADMIN, ACHETEUR, GESTIONNAIRE_STOCK", "Fiche article (réf. ART-0001)", "Non",
     "Référentiel des produits. La référence est générée automatiquement à la création et ne change plus. Un article désactivé n'est plus proposé dans les nouvelles commandes ni dans les ventes."],
    ["Fournisseurs / Transitaires", "/achats/...", "ADMIN, ACHETEUR", "Fiches", "Non",
     "Le fournisseur vend la marchandise (prix en devise). Le transitaire facture les frais d'approche (fret, douane, transit…) qui s'ajoutent au coût de revient."],
    ["Commandes d'achat", "/achats/commandes", "ADMIN, ACHETEUR", "Bon de commande CA-AAAA-NNNN", "Non",
     "Engagement auprès du fournisseur : articles, quantités, prix unitaires dans la devise du fournisseur, date d'arrivée prévue."],
    ["Réceptions", "/achats/receptions", "ADMIN, ACHETEUR + MAKER / CHECKER", "Bon de réception BR-AAAA-NNNN", "OUI — à la validation",
     "Constat de l'arrivée de la marchandise. Saisie par un MAKER, validée par un CHECKER différent. La validation fait entrer le stock et recalcule le CMUP."],
    ["Stock", "/stock", "ADMIN, GESTIONNAIRE_STOCK, ACHETEUR", "Fiche de stock + mouvements", "—",
     "Quantité et coûts moyens (CMUP achat et CMUP revient) par article. Toute variation est tracée dans un mouvement de stock."],
    ["Inventaires", "/stock/inventaires", "ADMIN, GESTIONNAIRE_STOCK, CHECKER", "Inventaire INV-AAAA-NNNN", "OUI — à la validation",
     "Comptage physique à l'aveugle, articles gelés pendant le comptage, écarts justifiés puis validés par un CHECKER (ajustements de stock)."],
    ["Clients", "/clients", "ADMIN, VENDEUR", "Fiche client", "Non",
     "Particulier ou professionnel (NIF et STAT obligatoires). Plafond de crédit > 0 = paiement à crédit autorisé. Le client « comptoir » sert aux ventes anonymes."],
    ["Ventes / Factures", "/ventes", "ADMIN, VENDEUR (+ CHECKER pour contrôler)", "Facture FV-AAAA-NNNN", "OUI — à la facturation",
     "Le vendeur saisit et soumet. Facturation immédiate si tout est dans les règles, sinon contrôle par un CHECKER (remise, vente à perte, plafond de crédit)."],
    ["Paiements", "/ventes/{id}/paiements", "ADMIN, VENDEUR (annulation : ADMIN)", "Encaissement", "Non",
     "Encaissements partiels ou totaux d'une facture validée. Le statut de paiement est calculé, jamais saisi."],
    ["Avoirs", "/ventes/{id}/avoirs", "ADMIN, CHECKER (+ VENDEUR pour l'URL)", "Avoir AV-AAAA-NNNN", "Oui si « retour en stock »",
     "Seule façon de corriger une facture validée (retour client, erreur, geste commercial)."],
    ["Tarifs", "/tarifs", "ADMIN", "Historique des prix", "Non",
     "Prix de vente HT du catalogue. Chaque changement est historisé (ancien prix, nouveau prix, coût de revient du moment). Prix sous le revient : motif obligatoire."],
    ["Paramètres / Utilisateurs", "/admin", "ADMIN", "—", "Non",
     "Taux de TVA, remise maximale du vendeur, identité de la société (mentions de facture), gestion des comptes et des rôles."],
    ["Rapport des ventes", "rapport", "ADMIN, CHECKER", "Tableau de bord", "Non",
     "Chiffre d'affaires, marges et analyses sur les factures validées."],
], [26, 24, 30, 28, 20, 70])
r = h2(ws, r, "Les 3 seuls moments où le stock bouge")
r = table(ws, r, ["Événement", "Type de mouvement", "Sens", "Coût utilisé", "Effet sur le CMUP", "Explication"], [
    ["Validation d'une réception (CHECKER)", "ENTREE_ACHAT", "+", "Coût d'achat MGA et coût de revient de la ligne", "Recalculé (moyenne pondérée)",
     "La marchandise reçue en bon état entre en stock. Les quantités refusées sont tracées mais n'entrent pas."],
    ["Facturation d'une vente", "SORTIE_VENTE", "−", "CMUP actuel", "Inchangé",
     "La quantité vendue sort au CMUP du moment ; ce coût est figé sur la ligne de vente pour calculer la marge."],
    ["Avoir avec retour en stock", "RETOUR_VENTE", "+", "Coût figé sur la ligne de vente d'origine", "Recalculé",
     "La marchandise revient au coût auquel elle était sortie, pour ne pas fausser la moyenne."],
    ["Validation d'un inventaire (CHECKER)", "AJUSTEMENT_POSITIF / AJUSTEMENT_NEGATIF", "+ / −", "CMUP actuel", "Inchangé",
     "Le stock est remis au niveau compté. Un écart d'inventaire n'est pas un achat : il ne change pas le coût moyen."],
], [36, 28, 8, 34, 24, 70])

# =====================================================================
# 2. Roles et droits
# =====================================================================
ws = sheet("Rôles et droits", "Rôles et droits d'accès",
           "Deux niveaux : filtre par URL (SecurityConfig) + filtre par action (@PreAuthorize) + règle « pas soi-même » (services)",
           "Qui peut voir, saisir, valider quoi ; principe des 4 yeux")
r = 5
r = h2(ws, r, "Rôles")
r = table(ws, r, ["Code", "Libellé", "Accès (URL)", "Actions principales", "Explication"], [
    ["ADMIN", "Administrateur", "Tout", "Tout, y compris paramètres, tarifs, utilisateurs, annulation de paiement",
     "Peut jouer MAKER et CHECKER, mais la règle des 4 yeux s'applique quand même : il ne peut pas valider ce qu'il a lui-même saisi."],
    ["ACHETEUR", "Acheteur", "/achats/**, /articles/**, /stock/** (lecture), /stock/inventaires/**", "Commandes d'achat : créer, modifier, passer, annuler, clôturer",
     "Donne l'accès aux écrans d'achat. Pour saisir ou valider une réception, il faut en plus MAKER ou CHECKER."],
    ["MAKER", "Initiateur (création des opérations)", "— (s'ajoute à ACHETEUR)", "Réceptions : saisir, modifier, supprimer (seulement les siennes)",
     "Celui qui fait. Sa saisie n'a aucun effet sur le stock tant qu'un CHECKER ne l'a pas validée."],
    ["CHECKER", "Validateur (validation des opérations)", "/stock/inventaires/** ; pour le reste s'ajoute à ACHETEUR / VENDEUR", "Valider/rejeter réceptions et inventaires, approuver/refuser ventes, créer des avoirs, rapport des ventes",
     "Celui qui contrôle. Toujours une personne différente de l'auteur. Le CHECKER a besoin de VENDEUR pour atteindre /ventes et d'ACHETEUR pour /achats."],
    ["VENDEUR", "Vendeur", "/ventes/**, /clients/**", "Ventes : saisir, modifier, supprimer, soumettre (les siennes) ; encaisser des paiements",
     "Vend dans la limite de la remise maximale. Au-delà, ou si vente à perte / plafond de crédit dépassé, la vente part en contrôle."],
    ["GESTIONNAIRE_STOCK", "Gestionnaire de stock", "/stock/**, /articles/**, /stock/inventaires/**", "Inventaires : ouvrir, compter, justifier les écarts, annuler",
     "Responsable du comptage physique. Ne peut pas valider un inventaire qu'il a ouvert ou compté."],
], [22, 30, 40, 50, 70])
r = h2(ws, r, "Principe des 4 yeux (maker / checker)")
r = table(ws, r, ["Opération", "Auteur (MAKER)", "Contrôleur (CHECKER)", "Interdit", "Explication"], [
    ["Réception", "Celui qui a saisi la réception", "ADMIN ou CHECKER", "Valider ou rejeter sa propre saisie",
     "Message : « Vous ne pouvez pas valider ou rejeter votre propre saisie : elle doit être contrôlée par une autre personne. »"],
    ["Vente en contrôle", "Le vendeur", "ADMIN ou CHECKER", "Approuver ou refuser sa propre vente",
     "Seules les ventes au statut « À valider » passent par un contrôleur ; les autres sont facturées directement par le vendeur."],
    ["Inventaire", "Celui qui a ouvert ET ceux qui ont compté", "ADMIN ou CHECKER", "Valider/rejeter un inventaire qu'on a ouvert ou compté",
     "Le contrôleur doit n'avoir participé à aucune étape du comptage."],
    ["Modification / suppression", "Seul l'auteur", "—", "Modifier la réception ou la vente d'un autre",
     "Une réception ou un brouillon de vente ne peut être modifié ou supprimé que par la personne qui l'a créé."],
], [24, 36, 24, 44, 70])

# =====================================================================
# 3. Cycles de vie
# =====================================================================
ws = sheet("Cycles de vie", "Cycles de vie (statuts) de chaque document",
           "Chaque ligne = un statut : d'où il vient, ce qui le déclenche, qui agit, effet sur le stock",
           "Tous les statuts : Commande, Réception, Vente, Paiement, Inventaire")
r = 5
rows = [
    # Commande
    ["Commande d'achat", "BROUILLON", "Brouillon", "—", "Créer la commande", "ACHETEUR / ADMIN", "Aucun",
     "Numéro CA-AAAA-NNNN attribué dès la création. Entièrement modifiable."],
    ["Commande d'achat", "COMMANDEE", "Commandée", "BROUILLON", "« Passer » la commande", "ACHETEUR / ADMIN", "Aucun",
     "Envoyée au fournisseur. Au moins une ligne obligatoire. Encore modifiable et annulable tant qu'aucune réception n'a été saisie. Peut apparaître « en retard » si la date d'arrivée prévue est dépassée."],
    ["Commande d'achat", "PARTIELLEMENT_RECUE", "Partiellement reçue", "COMMANDEE", "Automatique : validation d'une réception, il reste à recevoir", "Système (via CHECKER)", "Indirect (par la réception)",
     "Jamais posé à la main. Plus modifiable. On peut encore recevoir le reste ou clôturer."],
    ["Commande d'achat", "RECUE", "Reçue", "COMMANDEE / PARTIELLEMENT_RECUE", "Automatique : tout est reçu", "Système (via CHECKER)", "Indirect",
     "Toutes les lignes ont un reste à recevoir = 0. Statut final."],
    ["Commande d'achat", "CLOTUREE", "Clôturée", "PARTIELLEMENT_RECUE", "« Clôturer »", "ACHETEUR / ADMIN", "Aucun",
     "Le reste ne sera jamais livré. Refusé si une réception attend encore une validation ou une correction. Les quantités reçues et le stock ne changent pas."],
    ["Commande d'achat", "ANNULEE", "Annulée", "BROUILLON / COMMANDEE", "« Annuler »", "ACHETEUR / ADMIN", "Aucun",
     "Seulement si aucune réception n'a été saisie (même non validée)."],
    # Reception
    ["Réception", "SAISIE", "À valider", "— / REJETEE", "Enregistrer (création ou correction)", "MAKER / ADMIN", "Aucun",
     "Numéro BR-AAAA-NNNN. Une seule réception en attente par commande. Une réception rejetée puis corrigée revient ici."],
    ["Réception", "VALIDEE", "Validée", "SAISIE", "« Valider »", "CHECKER / ADMIN (≠ auteur)", "ENTREE_ACHAT",
     "Frais répartis et figés, quantités reçues ajoutées à la commande, entrée en stock, CMUP recalculé, statut de la commande mis à jour. Plus modifiable ni supprimable."],
    ["Réception", "REJETEE", "Rejetée", "SAISIE", "« Rejeter » avec motif", "CHECKER / ADMIN (≠ auteur)", "Aucun",
     "Motif obligatoire. L'auteur corrige puis réenregistre : la réception repasse en SAISIE."],
    # Vente
    ["Vente", "BROUILLON", "Brouillon", "— / A_VALIDER (refus)", "Enregistrer", "VENDEUR / ADMIN", "Aucun",
     "Pas de numéro de facture. Modifiable et supprimable par son seul vendeur. Les prix sont relus dans le catalogue."],
    ["Vente", "A_VALIDER", "À valider", "BROUILLON", "« Valider » par le vendeur, avec au moins un motif de contrôle", "VENDEUR", "Aucun",
     "Motifs : remise > plafond vendeur, prix net < prix de revient, plafond de crédit dépassé. Les motifs sont enregistrés sur la vente."],
    ["Vente", "VALIDEE", "Facturée", "BROUILLON (sans motif) / A_VALIDER (approuvée)", "« Valider » sans motif, ou « Approuver »", "VENDEUR, ou CHECKER / ADMIN (≠ vendeur)", "SORTIE_VENTE",
     "Numéro FV-AAAA-NNNN attribué, stock sorti au CMUP, coûts et marge figés. Ne change plus : se corrige uniquement par un avoir."],
    # Paiement (calcule)
    ["Statut de paiement (calculé)", "NON_PAYEE", "Non payée", "—", "Facture validée sans paiement", "—", "—",
     "Calculé à chaque affichage, jamais stocké : montant payé = 0 et reste à payer > 0."],
    ["Statut de paiement (calculé)", "PARTIELLE", "Partiellement payée", "—", "Paiement partiel", "VENDEUR / ADMIN", "—",
     "Montant payé > 0 mais reste à payer > 0."],
    ["Statut de paiement (calculé)", "PAYEE", "Payée", "—", "Reste à payer = 0", "VENDEUR / ADMIN", "—",
     "Le cumul des paiements non annulés couvre le net à payer."],
    ["Statut de paiement (calculé)", "SOLDEE_PAR_AVOIR", "Soldée par avoir", "—", "Avoirs = totalité de la facture", "CHECKER / ADMIN", "—",
     "Net à payer = 0 grâce aux avoirs : plus rien à encaisser."],
    # Inventaire
    ["Inventaire", "EN_COMPTAGE", "En comptage", "— / A_VALIDER (rejet)", "Ouvrir l'inventaire", "GESTIONNAIRE_STOCK / ADMIN", "Aucun (articles GELÉS)",
     "Photo du stock théorique et des coûts. Comptage à l'aveugle (le compteur ne voit pas le théorique). Aucun mouvement possible sur les articles concernés."],
    ["Inventaire", "A_VALIDER", "À valider", "EN_COMPTAGE", "Terminer le comptage", "GESTIONNAIRE_STOCK / ADMIN", "Aucun (toujours gelés)",
     "Les écarts deviennent visibles et doivent être justifiés (motif, et commentaire pour « Autre »)."],
    ["Inventaire", "VALIDE", "Validé", "A_VALIDER", "« Valider »", "CHECKER / ADMIN (n'a ni ouvert ni compté)", "AJUSTEMENT + / −",
     "Refusé s'il reste des écarts sans motif. Ajustements passés au CMUP actuel. Articles dégelés."],
    ["Inventaire", "ANNULE", "Annulé", "EN_COMPTAGE / A_VALIDER", "« Annuler » avec motif", "GESTIONNAIRE_STOCK / ADMIN", "Aucun",
     "Aucun effet sur le stock, articles dégelés."],
]
r = table(ws, r, ["Document", "Statut (code)", "Libellé affiché", "Vient de", "Déclencheur", "Qui", "Effet sur le stock", "Explication"],
          rows, [24, 22, 20, 28, 34, 28, 22, 70])
ws.freeze_panes = "A6"

# =====================================================================
# 4. Validations
# =====================================================================
ws = sheet("Validations", "Quand y a-t-il une validation ?",
           "Récapitulatif de tous les points de contrôle de l'application",
           "Tableau de synthèse : quelle opération est validée, par qui, avec quel effet")
r = 5
r = table(ws, r, ["Opération", "Validation par une autre personne ?", "Condition de déclenchement", "Qui saisit", "Qui valide", "Si validé", "Si rejeté / refusé", "Explication"], [
    ["Commande d'achat", "NON", "—", "ACHETEUR", "— (simple « Passer »)", "Passe en COMMANDEE", "—",
     "La commande n'engage pas le stock : un simple passage de statut suffit, sans double contrôle."],
    ["Réception", "OUI, toujours", "Toute réception", "MAKER", "CHECKER ≠ auteur", "Entrée en stock + CMUP + statut commande", "Retour à l'auteur avec motif (REJETEE)",
     "Le stock et la valeur du stock ne changent qu'après le regard d'une deuxième personne."],
    ["Vente", "SEULEMENT S'IL Y A UN MOTIF", "Remise > plafond vendeur (défaut 10 %) ; prix net < revient ; encours + TTC > plafond crédit", "VENDEUR", "CHECKER ≠ vendeur", "Facture : numéro, sortie de stock, marge figée", "Retour en BROUILLON avec commentaire obligatoire",
     "Sans motif, le vendeur facture seul. Les contrôles ciblent uniquement les risques (perte, remise excessive, crédit)."],
    ["Inventaire", "OUI, toujours", "Tout inventaire terminé", "GESTIONNAIRE_STOCK", "CHECKER (n'a ni ouvert ni compté)", "Ajustements de stock au CMUP", "Retour en comptage (recomptage), quantités conservées",
     "Tous les écarts doivent être justifiés avant de pouvoir valider."],
    ["Avoir", "Réservé au contrôleur", "Toute correction de facture", "CHECKER / ADMIN", "— (créé directement par le contrôleur)", "Montant crédité, éventuel retour en stock", "—",
     "Seuls les profils de contrôle peuvent émettre un avoir : pas de double saisie, mais le rôle est restreint."],
    ["Annulation de paiement", "Réservé à l'ADMIN", "Erreur de saisie, chèque rejeté…", "VENDEUR (paiement)", "ADMIN", "Paiement barré, cumul payé recalculé", "—",
     "L'annulation est tracée avec un motif ; le paiement reste visible."],
    ["Prix de vente sous le revient", "Réservé à l'ADMIN", "Nouveau prix < coût de revient", "ADMIN", "—", "Prix modifié et historisé", "—",
     "Autorisé (déstockage, promotion) mais le motif devient obligatoire."],
], [22, 22, 40, 20, 26, 32, 30, 60])

# =====================================================================
# 4b. Actions et validation (detail action par action)
# =====================================================================
ws = sheet("Actions et validation", "Détail de chaque action : validation requise ou non",
           "OUI = toujours validée par une autre personne ; CONDITIONNELLE = seulement dans certains cas ; NON = effet immédiat",
           "Liste exhaustive des actions, module par module, avec ou sans validation")
r = 5
A = [
    # module, action, qui, validation, par qui, condition, effet immediat, explication
    ["Articles", "Créer / modifier un article", "ADMIN, ACHETEUR, GESTIONNAIRE_STOCK", "NON", "—", "—", "Fiche enregistrée, référence ART-NNNN générée", "Donnée de référence, sans impact sur le stock ni la comptabilité."],
    ["Articles", "Activer / désactiver un article", "ADMIN, ACHETEUR, GESTIONNAIRE_STOCK", "NON", "—", "—", "Article retiré des nouvelles commandes / ventes", "Le stock et l'historique sont conservés."],
    ["Tarifs", "Modifier le prix de vente", "ADMIN", "NON", "—", "Motif obligatoire si prix < revient", "Prix changé et historisé", "Réservé à l'ADMIN : le contrôle se fait par le rôle, pas par une 2e personne."],
    ["Clients", "Créer / modifier un client", "ADMIN, VENDEUR", "NON", "—", "NIF + STAT si professionnel", "Fiche enregistrée", "Le plafond de crédit défini ici déclenche plus tard le contrôle des ventes."],
    ["Clients", "Désactiver un client", "ADMIN, VENDEUR", "NON", "—", "Impossible pour le client comptoir", "Client non proposé dans les nouvelles ventes", "—"],
    ["Commandes", "Créer / modifier une commande (brouillon)", "ADMIN, ACHETEUR", "NON", "—", "Aucune réception saisie", "Commande enregistrée", "Aucun effet sur le stock."],
    ["Commandes", "Passer la commande", "ADMIN, ACHETEUR", "NON", "—", "Brouillon avec au moins une ligne", "Statut COMMANDEE", "Pas de double contrôle : l'engagement fournisseur n'affecte pas le stock."],
    ["Commandes", "Annuler la commande", "ADMIN, ACHETEUR", "NON", "—", "Brouillon / commandée sans réception", "Statut ANNULEE", "Définitif."],
    ["Commandes", "Clôturer la commande", "ADMIN, ACHETEUR", "NON", "—", "Partiellement reçue, aucune réception en attente", "Statut CLOTUREE", "Le reliquat est abandonné ; le stock ne change pas."],
    ["Réceptions", "Saisir / corriger une réception", "MAKER (+ ACHETEUR)", "OUI", "CHECKER ≠ auteur", "Toujours", "Statut SAISIE (« À valider »), aucun effet stock", "La saisie seule ne change rien : elle attend le contrôleur."],
    ["Réceptions", "Supprimer une réception", "MAKER auteur", "NON", "—", "Non validée", "Réception supprimée", "Possible seulement avant validation."],
    ["Réceptions", "Valider une réception", "CHECKER (+ ACHETEUR)", "— (c'est la validation)", "—", "Statut SAISIE, validateur ≠ auteur", "Entrée en stock, CMUP recalculé, statut commande mis à jour", "Le moment où l'achat devient du stock."],
    ["Réceptions", "Rejeter une réception", "CHECKER (+ ACHETEUR)", "— (c'est le contrôle)", "—", "Motif obligatoire", "Statut REJETEE, retour à l'auteur", "Aucun effet stock."],
    ["Ventes", "Créer / modifier un brouillon de vente", "VENDEUR", "NON", "—", "Vendeur auteur", "Brouillon enregistré", "Pas de numéro de facture, pas de sortie de stock."],
    ["Ventes", "Supprimer un brouillon", "VENDEUR auteur", "NON", "—", "Statut BROUILLON", "Brouillon supprimé", "Une facture, elle, ne se supprime pas."],
    ["Ventes", "Valider (soumettre) la vente", "VENDEUR auteur", "CONDITIONNELLE", "CHECKER ≠ vendeur", "Remise > plafond, OU prix net < revient, OU plafond de crédit dépassé", "Sans motif : facturée immédiatement. Avec motif : statut A_VALIDER", "Le vendeur est autonome tant qu'il reste dans les règles."],
    ["Ventes", "Approuver une vente en contrôle", "CHECKER (+ VENDEUR)", "— (c'est la validation)", "—", "Statut A_VALIDER, contrôleur ≠ vendeur", "Facture FV, sortie de stock, marge figée", "Commentaire facultatif."],
    ["Ventes", "Refuser une vente en contrôle", "CHECKER (+ VENDEUR)", "— (c'est le contrôle)", "—", "Commentaire obligatoire", "Retour en BROUILLON", "Le vendeur corrige puis soumet à nouveau."],
    ["Paiements", "Enregistrer un paiement", "ADMIN, VENDEUR", "NON", "—", "Facture validée, montant ≤ reste à payer, référence sauf espèces", "Montant payé et statut de paiement mis à jour", "Effet immédiat ; la traçabilité repose sur la référence."],
    ["Paiements", "Annuler un paiement", "ADMIN", "NON (réservé ADMIN)", "—", "Motif obligatoire", "Paiement barré, reste à payer recalculé", "Contrôle par le rôle : seul l'ADMIN peut annuler."],
    ["Avoirs", "Créer un avoir", "CHECKER, ADMIN (+ VENDEUR)", "NON (réservé contrôleur)", "—", "Facture validée, motif, quantité ≤ créditable", "Avoir AV émis, éventuel retour en stock", "Contrôle par le rôle : seul un profil de contrôle peut émettre un avoir."],
    ["Inventaires", "Ouvrir un inventaire", "GESTIONNAIRE_STOCK, ADMIN", "NON", "—", "Articles non déjà dans un inventaire ouvert", "Photo du stock, articles GELÉS", "Aucun ajustement à ce stade, mais plus aucun mouvement possible sur ces articles."],
    ["Inventaires", "Saisir le comptage / terminer", "GESTIONNAIRE_STOCK, ADMIN", "OUI", "CHECKER n'ayant ni ouvert ni compté", "Toujours", "Statut A_VALIDER, écarts visibles", "Aucun effet stock avant validation."],
    ["Inventaires", "Justifier les écarts", "GESTIONNAIRE_STOCK, ADMIN", "NON", "—", "Motif pour chaque écart", "Motifs enregistrés", "Préalable obligatoire à la validation."],
    ["Inventaires", "Valider l'inventaire", "CHECKER, ADMIN", "— (c'est la validation)", "—", "Tous les écarts justifiés, contrôleur ≠ ouvreur/compteurs", "Ajustements +/−, articles dégelés", "Le stock est remis au niveau compté."],
    ["Inventaires", "Rejeter l'inventaire", "CHECKER, ADMIN", "— (c'est le contrôle)", "—", "Motif obligatoire", "Retour en comptage", "Recomptage, quantités saisies conservées."],
    ["Inventaires", "Annuler l'inventaire", "GESTIONNAIRE_STOCK, ADMIN", "NON", "—", "Motif obligatoire, inventaire en cours", "Statut ANNULE, articles dégelés", "Aucun effet sur le stock."],
    ["Administration", "Modifier les paramètres (TVA, remise max…)", "ADMIN", "NON", "—", "—", "Appliqué aux nouvelles ventes", "Les ventes existantes gardent leur taux de TVA."],
    ["Administration", "Créer un utilisateur / attribuer des rôles", "ADMIN", "NON", "—", "—", "Compte et droits mis à jour", "Attribuer à une même personne MAKER et CHECKER ne contourne pas les 4 yeux : elle ne pourra pas valider sa propre saisie."],
]
r = table(ws, r, ["Module", "Action", "Qui peut la faire", "Validation requise ?", "Validée par", "Condition", "Effet immédiat", "Explication"],
          A, [16, 38, 30, 22, 26, 44, 42, 60])
# Couleur selon la reponse
from openpyxl.formatting.rule import FormulaRule
first_row, last_row = 6, 5 + len(A)
rng = f"D{first_row}:D{last_row}"
ws.conditional_formatting.add(rng, FormulaRule(formula=[f'LEFT(D{first_row},3)="OUI"'], fill=PatternFill("solid", fgColor="F8CBAD"), font=Font(name=FONT, bold=True)))
ws.conditional_formatting.add(rng, FormulaRule(formula=[f'LEFT(D{first_row},5)="CONDI"'], fill=PatternFill("solid", fgColor="FFE699"), font=Font(name=FONT, bold=True)))
ws.conditional_formatting.add(rng, FormulaRule(formula=[f'LEFT(D{first_row},3)="NON"'], fill=PatternFill("solid", fgColor="C6EFCE"), font=Font(name=FONT, bold=True)))
ws.auto_filter.ref = f"A5:H{last_row}"
ws.freeze_panes = "C6"
r = h2(ws, r, "Synthèse")
syn = [("OUI", "Action toujours soumise à une 2e personne"), ("CONDITIONNELLE", "Soumise seulement si un motif de contrôle est détecté"),
       ("NON", "Effet immédiat (éventuellement restreint à un rôle)"), ("— (c'est la validation)", "Action de validation ou de rejet elle-même")]
for c_, h in enumerate(["Réponse", "Nombre d'actions", "Explication"], 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == 3 else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
r += 1
for key, expl in syn:
    ws.cell(row=r, column=1, value=key).font = F_BOLD
    crit = key[:3] + "*" if key != "— (c'est la validation)" else "—*"
    c = ws.cell(row=r, column=2, value=f'=COUNTIF($D${first_row}:$D${last_row},"{crit}")'); c.font = F_BOLD; c.fill = FILL_TOTAL
    c = ws.cell(row=r, column=3, value=expl); c.fill = FILL_EXPL; c.font = F_BODY; c.alignment = WRAP
    for c_ in range(1, 4):
        ws.cell(row=r, column=c_).border = BORDER
    r += 1

# =====================================================================
# 5. Commandes
# =====================================================================
ws = sheet("Commandes", "Commandes d'achat — règles métier", "Service : CommandeAchatService",
           "Règles de saisie, de passage, d'annulation et de clôture des commandes")
r = 5
r = table(ws, r, ["N°", "Règle", "Moment", "Message / comportement", "Explication"], [
    [1, "Date d'arrivée prévue ≥ date de commande", "Saisie", "« L'arrivée prévue ne peut pas précéder la date de commande »", "Cohérence du planning d'arrivage et du calcul des retards."],
    [2, "Fournisseur actif", "Saisie", "« Ce fournisseur est désactivé »", "Un fournisseur désactivé reste accepté sur une commande qui l'avait déjà."],
    [3, "Devise MGA ⇒ taux de change = 1", "Saisie", "Taux forcé à 1", "En Ariary, aucune conversion n'est nécessaire."],
    [4, "Un article par ligne", "Saisie", "« Article déjà présent sur une autre ligne : modifiez plutôt sa quantité »", "Évite les doublons et garantit un reste à recevoir par article."],
    [5, "Article actif pour une nouvelle ligne", "Saisie", "« L'article … est désactivé »", "Un article désactivé ne peut plus être commandé."],
    [6, "Montant total = Σ (quantité × prix unitaire), en devise", "Saisie", "Recalculé à chaque enregistrement, 2 décimales", "Montant de l'engagement dans la devise du fournisseur."],
    [7, "Passer : statut BROUILLON et au moins une ligne", "Passer", "« Seule une commande en brouillon peut être passée » / « aucun article »", "La commande devient un engagement envoyé au fournisseur."],
    [8, "Modifiable / annulable : BROUILLON ou COMMANDEE ET aucune réception saisie", "Modifier / Annuler", "« … n'est plus modifiable : une réception a déjà été saisie »", "Dès qu'une réception existe (même non validée), la commande est figée pour ne pas désaligner réception et commande."],
    [9, "Clôturer : PARTIELLEMENT_RECUE et aucune réception en attente", "Clôturer", "« Une réception est encore en attente : validez-la ou supprimez-la avant de clôturer »", "Abandon du reliquat : le fournisseur ne livrera pas le reste."],
    [10, "Contrôle de concurrence (version)", "Modifier", "« La commande a été modifiée par un autre utilisateur pendant votre saisie »", "Verrou optimiste : empêche d'écraser la modification d'un collègue."],
    [11, "Reste à recevoir = MAX(commandé − reçu ; 0)", "Calcul", "Par ligne", "Sert à pré-remplir les réceptions et à plafonner les quantités reçues."],
], [6, 52, 18, 60, 64])

# =====================================================================
# 6. Receptions
# =====================================================================
ws = sheet("Réceptions", "Réceptions — règles métier (maker / checker)", "Service : ReceptionService, RepartitionFrais",
           "Saisie par le MAKER, validation par le CHECKER, répartition des frais, entrée en stock")
r = 5
r = h2(ws, r, "Saisie (MAKER)")
r = table(ws, r, ["N°", "Règle", "Message / comportement", "Explication"], [
    [1, "Commande COMMANDEE ou PARTIELLEMENT_RECUE", "« … aucune réception possible »", "On ne reçoit que des commandes en attente d'arrivage."],
    [2, "Une seule réception en attente par commande", "« Une réception de cette commande attend déjà une validation ou une correction »", "Évite deux réceptions concurrentes qui dépasseraient la quantité commandée."],
    [3, "Pré-remplissage : chaque ligne restante avec la quantité attendue", "Quantité reçue = reste à recevoir", "Gain de temps : on corrige seulement ce qui diffère."],
    [4, "Quantité reçue ≤ reste à recevoir", "« Maximum … (reste à recevoir) »", "On ne peut pas recevoir plus que commandé."],
    [5, "Quantité reçue (bon état) / quantité refusée (abîmée)", "Seule la quantité reçue entre en stock", "La quantité refusée est tracée pour le litige fournisseur."],
    [6, "Au moins une quantité reçue ou refusée", "« Saisissez au moins une quantité reçue ou refusée »", "Une réception vide n'a pas de sens."],
    [7, "Date de réception : pas dans le futur, pas avant la commande", "Messages sur le champ date", "Cohérence chronologique."],
    [8, "Transitaire actif + taux de change du jour", "« Ce transitaire est désactivé »", "Le taux de la réception (et non celui de la commande) sert à valoriser le stock."],
    [9, "Frais du transitaire : type, montant, référence facture", "FRET, DOUANE, TRANSIT, MANUTENTION, STOCKAGE, AUTRE", "Ces frais s'ajoutent au coût de revient des articles reçus."],
    [10, "Modification / suppression : auteur uniquement, avant validation", "« Seul l'auteur de la saisie peut la modifier »", "Une réception validée ne se supprime pas : le stock a déjà bougé."],
], [6, 52, 60, 70])
r = h2(ws, r, "Validation (CHECKER) — dans une seule transaction")
r = table(ws, r, ["Étape", "Action", "Détail", "Explication"], [
    [1, "Verrou sur la commande", "Lecture « pour mise à jour »", "Deux validations simultanées passent l'une après l'autre : impossible de dépasser la quantité commandée."],
    [2, "Contrôle des 4 yeux", "Statut SAISIE et validateur ≠ auteur", "Garantit le double regard."],
    [3, "Commande toujours en attente", "COMMANDEE ou PARTIELLEMENT_RECUE", "Une commande annulée ou clôturée entre-temps bloque la validation."],
    [4, "Répartition des frais", "Au prorata de la valeur d'achat MGA des quantités reçues ; figés sur chaque ligne", "Voir l'onglet « Simu CMUP » : frais ligne = total frais × valeur ligne / valeur totale."],
    [5, "Re-contrôle des quantités", "Quantité reçue ≤ reste à recevoir", "Sinon : « Rejetez la réception pour correction »."],
    [6, "Mise à jour de la commande", "Quantité reçue de la ligne += quantité reçue", "Le reste à recevoir diminue."],
    [7, "Entrée en stock", "Mouvement ENTREE_ACHAT au coût d'achat et au coût de revient", "Le CMUP achat et le CMUP revient sont recalculés."],
    [8, "Statut de la commande", "RECUE si tout est reçu, sinon PARTIELLEMENT_RECUE", "Automatique."],
    [9, "Statut de la réception", "VALIDEE + validateur + date", "Traçabilité de qui a validé et quand."],
], [8, 34, 60, 76])
r = h2(ws, r, "Rejet (CHECKER)")
table(ws, r, ["Règle", "Détail", "Explication"], [
    ["Motif obligatoire", "« Indiquez le motif du rejet pour que l'auteur puisse corriger »", "L'auteur sait quoi corriger ; la réception repasse en SAISIE dès qu'il la réenregistre."],
], None)

# =====================================================================
# 7. Ventes
# =====================================================================
ws = sheet("Ventes", "Ventes — règles métier", "Service : VenteService ; entités Vente, LigneVente",
           "Saisie, motifs de contrôle, approbation, facturation et marge")
r = 5
r = h2(ws, r, "Saisie (vendeur)")
r = table(ws, r, ["N°", "Règle", "Message / comportement", "Explication"], [
    [1, "Client actif", "« Ce client est désactivé »", "Toléré si la vente avait déjà ce client."],
    [2, "Article actif ET avec un prix de vente", "« … n'a pas encore de prix de vente »", "Seuls les articles vendables sont proposés."],
    [3, "Prix relu dans le catalogue", "Jamais pris du navigateur", "Empêche la falsification du prix côté client."],
    [4, "Un article par ligne", "« Article déjà présent sur une autre ligne »", "Une ligne par article, on modifie la quantité."],
    [5, "TVA copiée des paramètres", "Taux figé sur la vente", "Un changement ultérieur du taux ne modifie pas les factures existantes."],
    [6, "Modification / suppression : vendeur auteur, BROUILLON uniquement", "« Seul un brouillon peut être modifié »", "Une facture se corrige par un avoir."],
], [6, 52, 50, 70])
r = h2(ws, r, "Soumission (« Valider » par le vendeur)")
r = table(ws, r, ["N°", "Contrôle", "Si non respecté", "Explication"], [
    [1, "Statut BROUILLON et au moins une ligne", "Erreur bloquante", "On ne soumet qu'un brouillon complet."],
    [2, "Stock disponible ≥ quantité demandée", "Erreur bloquante « Stock insuffisant »", "Le stock ne devient jamais négatif."],
    [3, "Remise ligne ≤ remise maximale vendeur (paramètre, défaut 10 %)", "Motif de contrôle → A_VALIDER", "Le vendeur peut accorder seul une remise jusqu'au plafond."],
    [4, "Prix net HT ≥ CMUP de revient", "Motif « vente à perte » → A_VALIDER", "Protège la marge."],
    [5, "Client à crédit (plafond > 0, hors comptoir) : encours + TTC ≤ plafond", "Motif « plafond de crédit dépassé » → A_VALIDER", "Plafond 0 = paiement comptant : pas de contrôle de crédit."],
    [6, "Aucun motif", "Facturation immédiate", "Le vendeur facture seul quand tout est dans les règles."],
], [6, 60, 40, 70])
r = h2(ws, r, "Contrôle (CHECKER ≠ vendeur)")
r = table(ws, r, ["Action", "Effet", "Explication"], [
    ["Approuver (commentaire facultatif)", "Facturation (voir ci-dessous)", "Le contrôleur accepte les motifs en connaissance de cause."],
    ["Refuser (commentaire obligatoire)", "Retour en BROUILLON", "Le vendeur corrige (remise, prix, client) puis soumet à nouveau."],
], [40, 40, 70])
r = h2(ws, r, "Facturation (finaliser) — tout ou rien")
table(ws, r, ["Étape", "Action", "Explication"], [
    [1, "Numéro FV-AAAA-NNNN + statut VALIDEE + date de validation", "La base exige « VALIDEE ⇔ numéro renseigné ». Suite continue, attribuée à la validation seulement."],
    [2, "Verrous de stock pris dans l'ordre des id d'article", "Pas d'interblocage entre deux ventes simultanées."],
    [3, "Sortie de stock SORTIE_VENTE au CMUP", "Refus si stock insuffisant ou article gelé par un inventaire."],
    [4, "Coût d'achat et coût de revient figés sur chaque ligne", "La marge reste juste même si le CMUP change plus tard."],
    [5, "Recalcul des totaux et marge figée", "Voir onglet « Formules » et « Simu Vente »."],
], [8, 60, 80])

# =====================================================================
# 8. Paiements et avoirs
# =====================================================================
ws = sheet("Paiements et avoirs", "Paiements et avoirs", "Services : PaiementService, AvoirService",
           "Encaissement, annulation de paiement, avoirs et retour en stock")
r = 5
r = h2(ws, r, "Paiements")
r = table(ws, r, ["N°", "Règle", "Message / comportement", "Explication"], [
    [1, "Facture validée uniquement", "« Seule une facture validée peut être encaissée »", "Un brouillon n'est pas une créance."],
    [2, "Mode de paiement obligatoire", "Espèces, MVola, Orange Money, Airtel Money, Virement, Chèque", "Sert au rapprochement de caisse."],
    [3, "Montant > 0 et ≤ reste à payer", "« Le montant dépasse le reste à payer »", "Facture verrouillée : deux encaissements simultanés ne peuvent pas dépasser le reste."],
    [4, "Date : pas dans le futur, pas avant la facture", "Messages sur la date", "Cohérence chronologique."],
    [5, "Référence obligatoire sauf espèces", "N° de transaction, de chèque ou de virement", "Permet de rapprocher le paiement avec le relevé."],
    [6, "Annulation : ADMIN, motif obligatoire", "Paiement barré mais visible", "Erreur de saisie, chèque rejeté… Le cumul payé est recalculé."],
], [6, 44, 56, 70])
r = h2(ws, r, "Avoirs")
table(ws, r, ["N°", "Règle", "Message / comportement", "Explication"], [
    [1, "Sur une facture validée uniquement", "« Un avoir ne peut porter que sur une facture validée »", "Seule façon de corriger une facture."],
    [2, "Motif obligatoire", "Figure sur le document", "Retour client, erreur, geste commercial…"],
    [3, "Quantité ≤ quantité créditable", "Créditable = quantité vendue − quantité déjà créditée", "On ne peut pas créditer plus que vendu."],
    [4, "Montant HT ligne = prix net HT de la vente × quantité", "Arrondi 2 décimales ; total HT arrondi à l'Ariary", "L'avoir reprend le prix réellement facturé (remise comprise)."],
    [5, "TVA = taux de la facture d'origine", "TVA = ROUND(HT × taux / 100 ; 0)", "Symétrie avec la facture."],
    [6, "Dernier avoir qui solde toute la facture", "Ajusté à l'Ariary près", "Le total des avoirs égale exactement la facture : pas de reliquat d'arrondi."],
    [7, "Case « retour en stock »", "Mouvement RETOUR_VENTE au coût figé sur la ligne de vente", "La marchandise réintègre le stock sans fausser le CMUP. Refusé si l'article est gelé."],
    [8, "Numéro AV-AAAA-NNNN", "Attribué à la création", "Montant avoirs de la facture += TTC de l'avoir."],
], [6, 44, 56, 70])

# =====================================================================
# 9. Stock
# =====================================================================
ws = sheet("Stock", "Stock et mouvements", "Service : StockService ; entités Stock, MouvementStock",
           "Point d'entrée unique, CMUP, gel, types de mouvements")
r = 5
r = h2(ws, r, "Types de mouvements")
r = table(ws, r, ["Code", "Libellé", "Sens", "Origine", "Coût du mouvement", "Effet CMUP", "Explication"], [
    ["ENTREE_ACHAT", "Entrée (réception achat)", "+1", "Validation de réception", "Coût d'achat MGA + coût de revient de la ligne", "Recalculé", "Seule source normale d'augmentation du coût moyen."],
    ["SORTIE_VENTE", "Sortie (vente)", "−1", "Facturation", "CMUP actuel", "Inchangé", "Le coût est figé sur la ligne de vente pour la marge."],
    ["RETOUR_VENTE", "Retour client (avoir)", "+1", "Avoir avec retour", "Coûts figés de la ligne de vente", "Recalculé", "Réintègre au coût de sortie : neutre si le CMUP n'a pas bougé entre-temps."],
    ["AJUSTEMENT_POSITIF", "Ajustement +", "+1", "Validation d'inventaire", "CMUP actuel", "Inchangé (entrée au CMUP)", "Surplus constaté au comptage."],
    ["AJUSTEMENT_NEGATIF", "Ajustement −", "−1", "Validation d'inventaire", "CMUP actuel", "Inchangé", "Manque constaté (casse, vol, erreur…)."],
], [22, 26, 8, 26, 40, 22, 60])
r = h2(ws, r, "Règles")
table(ws, r, ["N°", "Règle", "Explication"], [
    [1, "Toute modification du stock passe par StockService", "Quantité, CMUP et mouvement écrits ensemble : traçabilité complète (quantité après, CMUP après, référence du document, utilisateur)."],
    [2, "Verrou du stock (« pour mise à jour ») avant chaque mouvement", "Deux opérations simultanées sur le même article s'exécutent l'une après l'autre."],
    [3, "Le stock ne devient jamais négatif", "Sortie refusée si quantité disponible < quantité demandée."],
    [4, "Article gelé pendant un inventaire ouvert", "Aucune entrée ni sortie tant que l'inventaire est EN_COMPTAGE ou A_VALIDER (sauf ses propres ajustements)."],
    [5, "Mouvements exécutés dans la transaction de l'opération", "Si une étape échoue (stock insuffisant…), rien n'est enregistré."],
    [6, "Deux CMUP par article", "CMUP achat (prix fournisseur × taux) et CMUP revient (achat + frais). La valeur du stock existe dans les deux versions."],
    [7, "Stock vide ⇒ le CMUP repart du coût de la prochaine entrée", "L'ancien coût n'a plus de sens quand il n'y a plus rien en stock."],
    [8, "CMUP stocké avec 4 décimales", "Valeur du stock arrondie à l'Ariary."],
], [6, 60, 90])

# =====================================================================
# 10. Inventaire
# =====================================================================
ws = sheet("Inventaire", "Inventaires physiques", "Service : InventaireService",
           "Étapes, comptage à l'aveugle, gel, justification, classement ABC")
r = 5
r = h2(ws, r, "Étapes")
r = table(ws, r, ["Étape", "Statut", "Qui", "Ce qui se passe", "Explication"], [
    ["1. Ouverture", "EN_COMPTAGE", "GESTIONNAIRE_STOCK", "Photo du stock théorique et des CMUP de chaque article ; articles gelés", "Chaque stock est verrouillé le temps de la photo : une vente en cours passe avant ou après, jamais au milieu."],
    ["2. Comptage", "EN_COMPTAGE", "GESTIONNAIRE_STOCK (un ou plusieurs compteurs)", "Saisie des quantités comptées, en une ou plusieurs fois", "À l'aveugle : le compteur ne voit pas la quantité théorique, pour ne pas être influencé."],
    ["3. Fin du comptage", "A_VALIDER", "GESTIONNAIRE_STOCK", "Les écarts apparaissent", "Écart = compté − théorique."],
    ["4. Justification", "A_VALIDER", "GESTIONNAIRE_STOCK", "Motif obligatoire pour chaque écart (commentaire pour « Autre »)", "Casse, péremption, vol/perte, erreur de réception, erreur de vente, échantillon, autre."],
    ["5a. Validation", "VALIDE", "CHECKER (n'a ni ouvert ni compté)", "Ajustements + / − au CMUP actuel, articles dégelés", "Refusée tant qu'un écart n'est pas justifié."],
    ["5b. Rejet", "EN_COMPTAGE", "CHECKER", "Recomptage, quantités déjà saisies conservées", "Motif obligatoire : ce qu'il faut recompter et pourquoi."],
    ["Annulation", "ANNULE", "GESTIONNAIRE_STOCK", "Aucun effet sur le stock, articles dégelés", "Motif obligatoire."],
], [18, 16, 36, 56, 70])
r = h2(ws, r, "Règles d'ouverture")
r = table(ws, r, ["N°", "Règle", "Explication"], [
    [1, "Type COMPLET : tous les articles actifs + inactifs ayant encore du stock", "Inventaire annuel de clôture. Refusé si un autre inventaire est ouvert."],
    [2, "Type TOURNANT : sélection d'articles", "Comptage régulier sans fermer le magasin (ex. classe A chaque mois)."],
    [3, "Un article ne peut être que dans un seul inventaire ouvert", "« … est déjà dans l'inventaire … »."],
    [4, "Au moins un article coché", "« Cochez au moins un article à compter »."],
], [6, 60, 80])
r = h2(ws, r, "Classement ABC (aide au choix des articles)")
table(ws, r, ["Classe", "Règle", "Explication"], [
    ["A", "Part cumulée de la valeur AVANT l'article < 80 %", "Les articles qui portent l'essentiel de la valeur du stock : à compter le plus souvent. Le premier article est toujours A."],
    ["B", "Part cumulée avant l'article entre 80 % et 95 %", "Valeur intermédiaire."],
    ["C", "Part cumulée ≥ 95 %, ou valeur nulle", "Beaucoup d'articles, peu de valeur : comptage moins fréquent."],
    ["Tri", "Valeur de revient du stock décroissante, puis référence", "La valeur utilisée est quantité × CMUP revient."],
], [10, 60, 80])

# =====================================================================
# 11. Formules
# =====================================================================
ws = sheet("Formules", "Catalogue des formules de calcul",
           "Toutes les formules de l'application, avec arrondi, moment où elles sont figées et emplacement dans le code",
           "Toutes les formules : coût MGA, frais, revient, CMUP, TVA, marge, paiement, écarts, ABC")
r = 5
F = [
    # Achats / receptions
    ["Achat", "Montant d'une ligne de commande", "Quantité commandée × prix unitaire (devise)", "—", "Saisie", "LigneCommandeAchat.getMontant", "10 × 10 USD = 100 USD", "Engagement auprès du fournisseur, dans sa devise."],
    ["Achat", "Montant total de la commande", "Σ montants des lignes", "2 décimales", "Saisie", "CommandeAchat.recalculerMontantTotal", "—", "Total du bon de commande en devise."],
    ["Achat", "Reste à recevoir", "MAX(quantité commandée − quantité reçue ; 0)", "—", "Temps réel", "LigneCommandeAchat.getQuantiteRestante", "100 − 60 = 40", "Plafond de la prochaine réception, et critère du statut RECUE."],
    ["Réception", "Coût unitaire MGA (coût d'achat)", "Prix unitaire fournisseur × taux de change de la réception", "4 décimales", "Validation", "LigneReception.getCoutUnitaireMga", "10 USD × 4 500 = 45 000", "Convertit le prix fournisseur en Ariary au taux du jour de réception (pas celui de la commande)."],
    ["Réception", "Valeur d'achat de la ligne", "Coût unitaire MGA × quantité reçue", "—", "Validation", "LigneReception.getValeurAchatMga", "45 000 × 100 = 4 500 000", "Base de répartition des frais du transitaire."],
    ["Réception", "Frais de la ligne", "Total des frais × valeur de la ligne ÷ valeur totale reçue", "—", "Validation", "RepartitionFrais.fraisUnitaires", "1 000 000 × 30 % = 300 000", "Les frais sont partagés au prorata de la valeur. Si toutes les valeurs sont nulles (articles gratuits) : au prorata des quantités."],
    ["Réception", "Frais unitaire", "Frais de la ligne ÷ quantité reçue", "4 décimales", "Figé à la validation", "LigneReception.fraisUnitaire", "300 000 / 60 = 5 000", "Part des frais portée par chaque unité reçue en bon état."],
    ["Réception", "Revient unitaire (coût de revient)", "Coût unitaire MGA + frais unitaire", "4 décimales", "Figé à la validation", "LigneReception.getCoutRevientUnitaireMga", "45 000 + 5 000 = 50 000", "Ce que coûte réellement une unité rendue en magasin : achat + frais d'approche."],
    # Stock
    ["Stock", "CMUP achat (Coût Moyen Unitaire Pondéré)", "Si stock ≤ 0 : coût d'entrée ; sinon (Q stock × CMUP + Q entrée × coût entrée) ÷ (Q stock + Q entrée)", "4 décimales", "À chaque entrée", "Stock.entrer / moyennePonderee", "(40×45 000 + 160×50 600) / 200 = 49 480", "Coût moyen d'une unité en stock, pondéré par les quantités. Une sortie ne le modifie pas."],
    ["Stock", "CMUP revient", "Même formule avec les coûts de revient", "4 décimales", "À chaque entrée", "Stock.entrer", "(40×50 000 + 160×54 600) / 200 = 53 680", "Coût moyen frais compris : sert à la marge et au contrôle de vente à perte."],
    ["Stock", "Valeur du stock (achat)", "Quantité × CMUP achat", "0 décimale (Ariary)", "Temps réel", "Stock.getValeur", "200 × 49 480 = 9 896 000", "Valorisation du stock au prix d'achat."],
    ["Stock", "Valeur du stock (revient)", "Quantité × CMUP revient", "0 décimale", "Temps réel", "Stock.getValeurRevient", "200 × 53 680 = 10 736 000", "Valorisation frais compris ; sert au classement ABC."],
    # Vente
    ["Vente", "Prix net unitaire HT", "Prix catalogue HT × (100 − remise %) ÷ 100", "2 décimales", "Saisie", "LigneVente.appliquer", "60 000 × 95 / 100 = 57 000", "Prix après remise de la ligne."],
    ["Vente", "Montant HT de la ligne", "Prix net HT × quantité", "2 décimales", "Saisie", "LigneVente.appliquer", "57 000 × 10 = 570 000", "Base du total HT."],
    ["Vente", "Montant brut HT de la ligne", "Prix catalogue HT × quantité", "—", "Saisie", "LigneVente.getMontantBrutHt", "60 000 × 10 = 600 000", "Avant remise : sert à calculer la remise totale."],
    ["Vente", "Total remise", "MAX(Σ brut HT − Σ montant HT ; 0)", "—", "Saisie", "Vente.recalculerTotaux", "600 000 − 570 000 = 30 000", "Montant de la remise accordée."],
    ["Vente", "TVA", "Total HT × taux TVA ÷ 100", "0 décimale (Ariary)", "Saisie", "Vente.recalculerTotaux", "570 000 × 20 % = 114 000", "Calculée une seule fois sur le total HT : pas d'écarts d'arrondi ligne à ligne. Masquée si taux = 0 %."],
    ["Vente", "Total TTC", "Total HT + TVA", "—", "Saisie", "Vente.recalculerTotaux", "570 000 + 114 000 = 684 000", "Montant facturé au client."],
    ["Vente", "Coût de revient total", "Σ (coût de revient unitaire figé × quantité)", "0 décimale", "Figé à la facturation", "Vente.figerMarge", "50 000 × 10 = 500 000", "Coût de la marchandise vendue, au CMUP revient du moment de la vente."],
    ["Vente", "Marge de la ligne", "Montant HT − coût de revient unitaire × quantité", "0 décimale", "Figé à la facturation", "LigneVente.getMarge", "570 000 − 500 000 = 70 000", "Gain brut de la ligne, frais d'approche déduits."],
    ["Vente", "Marge totale", "Total HT − coût de revient total", "—", "Figé à la facturation", "Vente.figerMarge", "70 000", "Gain brut de la facture."],
    ["Vente", "Taux de marque", "Marge totale × 100 ÷ total HT", "1 décimale", "Calculé", "Vente.getTauxMarque", "70 000 / 570 000 = 12,3 %", "Part de la marge dans le chiffre d'affaires HT (≠ taux de marge, qui se calcule sur le coût)."],
    ["Vente", "Contrôle remise", "Remise ligne > remise max vendeur ⇒ contrôle", "—", "Soumission", "VenteService.motifsDeControle", "15 % > 10 % ⇒ À valider", "Le vendeur ne peut accorder seul qu'une remise limitée."],
    ["Vente", "Contrôle vente à perte", "Prix net HT < CMUP revient ⇒ contrôle", "—", "Soumission", "VenteService.motifsDeControle", "48 000 < 50 000 ⇒ À valider", "Empêche de vendre sous le coût sans accord."],
    ["Vente", "Contrôle plafond de crédit", "Client à crédit : encours + TTC > plafond ⇒ contrôle", "—", "Soumission", "VenteService.motifsDeControle", "800 000 + 684 000 > 1 000 000", "Limite le risque d'impayé. Plafond 0 = comptant, pas de contrôle."],
    # Paiement / avoir
    ["Encaissement", "Net à payer", "MAX(total TTC − montant des avoirs ; 0)", "—", "Calculé", "Vente.getNetAPayer", "684 000 − 68 400 = 615 600", "Ce que le client doit après avoirs."],
    ["Encaissement", "Reste à payer", "MAX(net à payer − montant payé ; 0)", "—", "Calculé", "Vente.getResteAPayer", "615 600 − 300 000 = 315 600", "Plafond du prochain paiement ; alimente l'encours client."],
    ["Encaissement", "Trop-perçu", "MAX(montant payé − net à payer ; 0)", "—", "Calculé", "Vente.getTropPercu", "—", "À rembourser au client (payé plus que le net, après un avoir)."],
    ["Encaissement", "Statut de paiement", "Net = 0 et avoirs > 0 ⇒ Soldée par avoir ; reste = 0 ⇒ Payée ; payé > 0 ⇒ Partielle ; sinon Non payée", "—", "Calculé", "Vente.getStatutPaiement", "—", "Jamais saisi : déduit des montants."],
    ["Encaissement", "Encours client", "Σ restes à payer des factures validées du client", "—", "Calculé", "VenteRepository.encoursClient", "—", "Utilisé pour le contrôle du plafond de crédit."],
    ["Avoir", "Quantité créditable", "MAX(quantité vendue − quantité déjà en avoir ; 0)", "—", "Temps réel", "LigneVente.getQuantiteCreditable", "10 − 1 = 9", "Plafond de la quantité d'un nouvel avoir."],
    ["Avoir", "Montant HT d'une ligne d'avoir", "Prix net HT de la vente × quantité créditée", "2 décimales (total HT à 0)", "Création", "AvoirService.creer", "57 000 × 1 = 57 000", "On rembourse le prix réellement facturé."],
    ["Avoir", "TVA / TTC de l'avoir", "TVA = total HT × taux ÷ 100 ; TTC = HT + TVA", "0 décimale", "Création", "Avoir.calculerTotaux", "57 000 + 11 400 = 68 400", "Même taux que la facture d'origine."],
    # Inventaire
    ["Inventaire", "Écart", "Quantité comptée − quantité théorique", "—", "Après comptage", "LigneInventaire.getEcart", "38 − 40 = −2", "Positif = surplus, négatif = manque."],
    ["Inventaire", "Valeur de l'écart", "Écart × CMUP revient figé à l'ouverture", "0 décimale", "Après comptage", "LigneInventaire.getEcartValeur", "−2 × 53 680 = −107 360", "Impact financier de l'écart."],
    ["Inventaire", "Écart %", "Écart × 100 ÷ quantité théorique (vide si théorique = 0)", "1 décimale", "Après comptage", "LigneInventaire.getEcartPct", "−2 / 40 = −5,0 %", "Permet de repérer les écarts anormaux."],
    ["Inventaire", "Classe ABC", "Part cumulée avant l'article = cumul ÷ total : < 80 % ⇒ A ; < 95 % ⇒ B ; sinon C", "4 décimales sur la part", "Ouverture", "InventaireService.articlesInventoriables", "—", "Priorise les articles de forte valeur pour les comptages tournants."],
]
r = table(ws, r, ["Domaine", "Indicateur", "Formule", "Arrondi", "Figé / calculé quand", "Emplacement dans le code", "Exemple", "Explication"],
          F, [14, 34, 56, 18, 20, 36, 34, 70])
ws.freeze_panes = "C6"

# =====================================================================
# 12. Simulateur CMUP (formules vivantes)
# =====================================================================
ws = sheet("Simu CMUP", "Simulateur : coût de revient et CMUP",
           "Cellules jaunes à texte bleu = à modifier. Tout le reste est calculé par des formules Excel identiques à celles de l'application.",
           "Simulateur vivant : réception avec frais → revient unitaire → évolution du CMUP")
r = 5
ws.cell(row=r, column=1, value="Légende :").font = F_BOLD
c = ws.cell(row=r, column=2, value="Saisie")
c.font = F_INPUT; c.fill = FILL_INPUT; c.border = BORDER
c = ws.cell(row=r, column=3, value="Calcul (formule)")
c.font = F_BODY; c.border = BORDER
c = ws.cell(row=r, column=4, value="Explication")
c.font = F_BODY; c.fill = FILL_EXPL; c.border = BORDER

r = 7
r = h2(ws, r, "Étape 1 — Réception n°1 : paramètres")
param_rows = [("Taux de change de la réception (MGA pour 1 USD)", 4500, MGA, "Taux du jour de réception, saisi sur la réception."),
              ("Total des frais du transitaire (MGA)", 1300000, MGA, "Fret + douane + transit + manutention + stockage + autre.")]
for c_, h in enumerate(["Paramètre", "Valeur", "", "Explication"], 1):
    cell = ws.cell(row=r, column=c_, value=h if h else None)
    if h:
        cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == 4 else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
r += 1
P_TAUX = f"$B${r}"
P_FRAIS = f"$B${r+1}"
for lab, val, fmt, expl in param_rows:
    ws.cell(row=r, column=1, value=lab).font = F_BODY
    ws.cell(row=r, column=1).border = BORDER
    c = ws.cell(row=r, column=2, value=val); c.font = F_INPUT; c.fill = FILL_INPUT; c.number_format = fmt; c.border = BORDER
    c = ws.cell(row=r, column=4, value=expl); c.font = F_BODY; c.fill = FILL_EXPL; c.alignment = WRAP; c.border = BORDER
    r += 1
r += 1
r = h2(ws, r, "Étape 2 — Répartition des frais et revient unitaire (par ligne reçue)")
heads = ["Article", "Prix unitaire (USD)", "Quantité reçue", "Coût unitaire MGA", "Valeur d'achat MGA",
         "Part de la valeur", "Frais de la ligne", "Frais unitaire", "Revient unitaire", "Explication"]
for c_, h in enumerate(heads, 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == len(heads) else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
ws.row_dimensions[r].height = 30
r += 1
lines = [("Riz 50 kg", 10, 100), ("Huile 20 L", 25, 40), ("Sucre 50 kg", 12, 50)]
first = r
last = r + len(lines) - 1
expl_lines = [
    "Coût MGA = prix USD × taux (4 déc.). Valeur = coût × quantité. Frais ligne = total frais × part. Revient = coût + frais unitaire.",
    "Les frais suivent la valeur : un article plus cher porte plus de frais.",
    "Le revient unitaire est figé sur la ligne de réception à la validation, puis entre dans le CMUP revient.",
]
for i, (art, prix, q) in enumerate(lines):
    ws.cell(row=r, column=1, value=art)
    ws.cell(row=r, column=2, value=prix)
    ws.cell(row=r, column=3, value=q)
    ws.cell(row=r, column=4, value=f"=ROUND(B{r}*{P_TAUX},4)")
    ws.cell(row=r, column=5, value=f"=D{r}*C{r}")
    ws.cell(row=r, column=6, value=f"=IF($E${last+1}=0,0,E{r}/$E${last+1})")
    ws.cell(row=r, column=7, value=f"={P_FRAIS}*F{r}")
    ws.cell(row=r, column=8, value=f"=IF(C{r}=0,0,ROUND(G{r}/C{r},4))")
    ws.cell(row=r, column=9, value=f"=D{r}+H{r}")
    ws.cell(row=r, column=10, value=expl_lines[i])
    for c_ in range(1, 11):
        cell = ws.cell(row=r, column=c_); cell.border = BORDER; cell.font = F_BODY; cell.alignment = WRAP
    for c_ in (1, 2, 3):
        ws.cell(row=r, column=c_).font = F_INPUT; ws.cell(row=r, column=c_).fill = FILL_INPUT
    ws.cell(row=r, column=10).fill = FILL_EXPL
    ws.cell(row=r, column=2).number_format = MGA2
    ws.cell(row=r, column=3).number_format = QTE
    for c_ in (4, 8, 9):
        ws.cell(row=r, column=c_).number_format = MGA4
    for c_ in (5, 7):
        ws.cell(row=r, column=c_).number_format = MGA
    ws.cell(row=r, column=6).number_format = "0.00%"
    r += 1
ws.cell(row=r, column=1, value="TOTAL")
ws.cell(row=r, column=3, value=f"=SUM(C{first}:C{last})")
ws.cell(row=r, column=5, value=f"=SUM(E{first}:E{last})")
ws.cell(row=r, column=6, value=f"=SUM(F{first}:F{last})")
ws.cell(row=r, column=7, value=f"=SUM(G{first}:G{last})")
ws.cell(row=r, column=10, value="Contrôle : la somme des frais répartis doit égaler le total des frais saisi.")
for c_ in range(1, 11):
    cell = ws.cell(row=r, column=c_); cell.font = F_BOLD; cell.fill = FILL_TOTAL; cell.border = BORDER; cell.alignment = WRAP
ws.cell(row=r, column=10).fill = FILL_EXPL; ws.cell(row=r, column=10).font = F_BODY
ws.cell(row=r, column=3).number_format = QTE
ws.cell(row=r, column=5).number_format = MGA
ws.cell(row=r, column=6).number_format = "0.00%"
ws.cell(row=r, column=7).number_format = MGA
RIZ_ROW = first
r += 2

r = h2(ws, r, "Étape 3 — Évolution du stock et du CMUP de l'article « Riz 50 kg »")
ws.cell(row=r, column=1, value="Types : « Entrée » (réception ou retour), « Sortie » (vente ou ajustement −). Une sortie utilise le CMUP du moment, sans le modifier.").font = F_SUB
r += 1
heads = ["Événement", "Type", "Quantité", "Coût d'achat entrée", "Coût de revient entrée",
         "Stock avant", "CMUP achat avant", "CMUP revient avant",
         "Stock après", "CMUP achat après", "CMUP revient après", "Valeur stock (revient)", "Explication"]
for c_, h in enumerate(heads, 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == len(heads) else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
ws.row_dimensions[r].height = 30
r += 1
ev_first = r
events = [
    ("Réception n°1 (BR) — validation", "Entrée", f"=C{RIZ_ROW}", f"=D{RIZ_ROW}", f"=I{RIZ_ROW}",
     "Stock vide : le CMUP prend directement le coût de cette réception (formule « si stock ≤ 0 »)."),
    ("Vente (FV) — facturation", "Sortie", 60, None, None,
     "Sortie au CMUP : la quantité baisse, les CMUP ne changent pas. Ces coûts sont figés sur la ligne de vente."),
    ("Réception n°2 (BR) — validation", "Entrée", 160, 50600, 54600,
     "Moyenne pondérée : (stock avant × CMUP avant + quantité × coût) ÷ (stock avant + quantité)."),
    ("Avoir avec retour en stock (AV)", "Entrée", 5, f"=J{ev_first+1}", f"=K{ev_first+1}",
     "Le retour rentre au coût figé sur la vente (ici le CMUP au moment de la vente), ce qui préserve les CMUP."),
    ("Inventaire — ajustement −", "Sortie", 3, None, None,
     "Ajustement négatif au CMUP actuel, sans le modifier (un écart d'inventaire n'est pas un achat)."),
    ("Vente — tout le stock restant", "Sortie", f"=F{ev_first+5}", None, None,
     "Le stock revient à 0 ; les CMUP restent affichés mais seront remplacés à la prochaine entrée."),
    ("Réception n°3 sur stock vide", "Entrée", 50, 52000, 56000,
     "Stock à 0 : l'ancien CMUP est oublié, le nouveau CMUP = coût de cette réception."),
]
for i, (lab, typ, q, ca, cr, expl) in enumerate(events):
    ws.cell(row=r, column=1, value=lab)
    ws.cell(row=r, column=2, value=typ)
    ws.cell(row=r, column=3, value=q)
    if typ == "Entrée":
        ws.cell(row=r, column=4, value=ca)
        ws.cell(row=r, column=5, value=cr)
    else:
        ws.cell(row=r, column=4, value="—")
        ws.cell(row=r, column=5, value="—")
    if i == 0:
        ws.cell(row=r, column=6, value=0)
        ws.cell(row=r, column=7, value=0)
        ws.cell(row=r, column=8, value=0)
    else:
        ws.cell(row=r, column=6, value=f"=I{r-1}")
        ws.cell(row=r, column=7, value=f"=J{r-1}")
        ws.cell(row=r, column=8, value=f"=K{r-1}")
    ws.cell(row=r, column=9, value=f'=IF(B{r}="Entrée",F{r}+C{r},F{r}-C{r})')
    ws.cell(row=r, column=10, value=f'=IF(B{r}="Entrée",IF(F{r}<=0,ROUND(D{r},4),ROUND((F{r}*G{r}+C{r}*D{r})/(F{r}+C{r}),4)),G{r})')
    ws.cell(row=r, column=11, value=f'=IF(B{r}="Entrée",IF(F{r}<=0,ROUND(E{r},4),ROUND((F{r}*H{r}+C{r}*E{r})/(F{r}+C{r}),4)),H{r})')
    ws.cell(row=r, column=12, value=f"=ROUND(I{r}*K{r},0)")
    ws.cell(row=r, column=13, value=expl)
    for c_ in range(1, 14):
        cell = ws.cell(row=r, column=c_); cell.border = BORDER; cell.font = F_BODY; cell.alignment = WRAP
    # saisies : type, quantite, couts (sauf formules liees)
    for c_ in (2, 3, 4, 5):
        v = ws.cell(row=r, column=c_).value
        if not (isinstance(v, str) and (v.startswith("=") or v == "—")):
            ws.cell(row=r, column=c_).font = F_INPUT; ws.cell(row=r, column=c_).fill = FILL_INPUT
    ws.cell(row=r, column=13).fill = FILL_EXPL
    ws.cell(row=r, column=3).number_format = QTE
    for c_ in (6, 9):
        ws.cell(row=r, column=c_).number_format = QTE
    for c_ in (4, 5, 7, 8, 10, 11):
        ws.cell(row=r, column=c_).number_format = MGA4
    ws.cell(row=r, column=12).number_format = MGA
    r += 1
ev_last = r - 1
ws.cell(row=ev_first + 1, column=3).comment = Comment("Quantité vendue : à modifier. Doit rester ≤ stock avant (l'application refuse un stock négatif).", "Doc")
r += 1
r = h2(ws, r, "Pourquoi « pondéré » ? (comparaison avec la réception n°2)")
cmp_rows = [
    ("Moyenne simple des deux coûts d'achat", f"=(G{ev_first+2}+D{ev_first+2})/2",
     "Faux : ignore que la 2e réception compte beaucoup plus d'unités que le stock restant."),
    ("CMUP achat (moyenne pondérée)", f"=J{ev_first+2}",
     "Juste : chaque coût pèse selon sa quantité."),
    ("Écart", f"=B{r+2}-B{r+1}", "Erreur de valorisation par unité si on utilisait la moyenne simple."),
]
for c_, h in enumerate(["Méthode", "Valeur (MGA)", "", "Explication"], 1):
    if h:
        cell = ws.cell(row=r, column=c_, value=h)
        cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == 4 else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
r += 1
for lab, f, expl in cmp_rows:
    ws.cell(row=r, column=1, value=lab).border = BORDER
    ws.cell(row=r, column=1).font = F_BODY
    c = ws.cell(row=r, column=2, value=f); c.number_format = MGA4; c.border = BORDER; c.font = F_BODY
    c = ws.cell(row=r, column=4, value=expl); c.fill = FILL_EXPL; c.font = F_BODY; c.alignment = WRAP; c.border = BORDER
    r += 1
for col, w in zip("ABCDEFGHIJKLM", [40, 14, 14, 18, 18, 14, 18, 18, 14, 18, 18, 18, 70]):
    ws.column_dimensions[col].width = w
ws.column_dimensions["J"].width = max(ws.column_dimensions["J"].width, 60)

# =====================================================================
# 13. Simulateur Vente
# =====================================================================
ws = sheet("Simu Vente", "Simulateur : facture, contrôles, marge et paiement",
           "Cellules jaunes à texte bleu = à modifier. Formules identiques à celles de l'application (arrondis compris).",
           "Simulateur vivant : remise, TVA, TTC, marge, motifs de contrôle, statut de paiement")
r = 5
r = h2(ws, r, "Paramètres")
for c_, h in enumerate(["Paramètre", "Valeur", "", "Explication"], 1):
    if h:
        cell = ws.cell(row=r, column=c_, value=h)
        cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == 4 else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
r += 1
params = [
    ("Taux de TVA (%)", 20, '0.00', "Paramètre général (défaut 20). Copié sur la vente à sa création. 0 % = entreprise non assujettie."),
    ("Remise maximale vendeur (%)", 10, '0.00', "Au-delà, la vente part en contrôle (défaut 10)."),
    ("Client à crédit ? (OUI / NON)", "OUI", None, "OUI = plafond de crédit > 0 et client non comptoir."),
    ("Plafond de crédit du client (MGA)", 1500000, MGA, "0 = paiement comptant."),
    ("Encours actuel du client (MGA)", 800000, MGA, "Somme des restes à payer de ses factures validées."),
]
prow = {}
for lab, val, fmt, expl in params:
    ws.cell(row=r, column=1, value=lab).font = F_BODY; ws.cell(row=r, column=1).border = BORDER
    c = ws.cell(row=r, column=2, value=val); c.font = F_INPUT; c.fill = FILL_INPUT; c.border = BORDER
    if fmt:
        c.number_format = fmt
    c = ws.cell(row=r, column=4, value=expl); c.font = F_BODY; c.fill = FILL_EXPL; c.alignment = WRAP; c.border = BORDER
    prow[lab] = f"$B${r}"
    r += 1
TVA, RMAX, CRED, PLAF, ENC = (prow[k] for k in [p[0] for p in params])
r += 1
r = h2(ws, r, "Lignes de la vente")
heads = ["Article", "Prix catalogue HT", "Remise %", "Quantité", "CMUP revient (stock)", "Prix net HT", "Montant brut HT",
         "Montant HT", "Coût de revient", "Marge ligne", "Contrôle remise", "Contrôle perte", "Explication"]
for c_, h in enumerate(heads, 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == len(heads) else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
ws.row_dimensions[r].height = 30
r += 1
vlines = [("Riz 50 kg", 60000, 5, 10, 50000, "Remise 5 % ≤ plafond, prix net > revient : aucun motif."),
          ("Huile 20 L", 140000, 15, 4, 110000, "Remise 15 % > plafond 10 % : motif de contrôle."),
          ("Sucre 50 kg", 62000, 0, 6, 63500, "Prix net < revient : vente à perte, motif de contrôle.")]
vf = r
vl = r + len(vlines) - 1
for art, p, rem, q, rev, expl in vlines:
    vals = [art, p, rem, q, rev,
            f"=ROUND(B{r}*(100-C{r})/100,2)",
            f"=B{r}*D{r}",
            f"=ROUND(F{r}*D{r},2)",
            f"=E{r}*D{r}",
            f"=ROUND(H{r}-I{r},0)",
            f'=IF(C{r}>{RMAX},"Contrôle","OK")',
            f'=IF(F{r}<E{r},"Contrôle","OK")',
            expl]
    for c_, v in enumerate(vals, 1):
        cell = ws.cell(row=r, column=c_, value=v); cell.border = BORDER; cell.font = F_BODY; cell.alignment = WRAP
    for c_ in range(1, 6):
        ws.cell(row=r, column=c_).font = F_INPUT; ws.cell(row=r, column=c_).fill = FILL_INPUT
    ws.cell(row=r, column=13).fill = FILL_EXPL
    for c_ in (2, 5, 6, 7, 8):
        ws.cell(row=r, column=c_).number_format = MGA2
    ws.cell(row=r, column=3).number_format = '0.00'
    ws.cell(row=r, column=4).number_format = QTE
    for c_ in (9, 10):
        ws.cell(row=r, column=c_).number_format = MGA
    r += 1
r += 1
r = h2(ws, r, "Totaux de la facture")
for c_, h in enumerate(["Total", "Valeur", "Formule", "Explication"], 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == 4 else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
r += 1
t = {}
tot_rows = [
    ("Total brut HT", f"=SUM(G{vf}:G{vl})", "Σ prix catalogue × quantité", "Avant remise.", MGA2),
    ("Total HT", f"=SUM(H{vf}:H{vl})", "Σ montants HT", "Après remise.", MGA2),
    ("Total remise", "=MAX(B{brut}-B{ht},0)", "MAX(brut − HT ; 0)", "Montant total de la remise.", MGA2),
    ("TVA", "=ROUND(B{ht}*" + TVA + "/100,0)", "ROUND(HT × taux / 100 ; 0)", "Une seule fois sur le total HT, arrondie à l'Ariary.", MGA),
    ("Total TTC", "=B{ht}+B{tva}", "HT + TVA", "Montant facturé.", MGA2),
    ("Coût de revient total", f"=ROUND(SUM(I{vf}:I{vl}),0)", "ROUND(Σ revient × quantité ; 0)", "Figé à la facturation au CMUP revient du moment.", MGA),
    ("Marge totale", "=B{ht}-B{cout}", "HT − coût de revient total", "Gain brut de la facture.", MGA2),
    ("Taux de marque", '=IF(B{ht}=0,"",ROUND(B{marge}*100/B{ht},1))', "Marge × 100 / HT", "Part de la marge dans le CA HT.", PCT),
]
keys = ["brut", "ht", "rem", "tva", "ttc", "cout", "marge", "tm"]
for k in keys:
    t[k] = r + keys.index(k)
for (lab, f, ftxt, expl, fmt), k in zip(tot_rows, keys):
    rr = t[k]
    f = f.format(**t)
    ws.cell(row=rr, column=1, value=lab).font = F_BOLD
    c = ws.cell(row=rr, column=2, value=f); c.number_format = fmt; c.font = F_BOLD; c.fill = FILL_TOTAL
    ws.cell(row=rr, column=3, value=ftxt).font = F_BODY
    c = ws.cell(row=rr, column=4, value=expl); c.fill = FILL_EXPL; c.font = F_BODY; c.alignment = WRAP
    for c_ in range(1, 5):
        ws.cell(row=rr, column=c_).border = BORDER
r = t["tm"] + 2
r = h2(ws, r, "Décision à la soumission")
for c_, h in enumerate(["Contrôle", "Résultat", "Règle", "Explication"], 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == 4 else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
r += 1
d0 = r
dec = [
    ("Lignes avec remise > plafond", f'=COUNTIF(K{vf}:K{vl},"Contrôle")', "Remise ligne > remise max vendeur", "Nombre de lignes qui imposent un contrôle de remise."),
    ("Lignes vendues à perte", f'=COUNTIF(L{vf}:L{vl},"Contrôle")', "Prix net < CMUP revient", "Nombre de lignes sous le coût de revient."),
    ("Encours après cette vente", f"={ENC}+B{t['ttc']}", "Encours + TTC", "Comparé au plafond de crédit."),
    ("Plafond de crédit dépassé ?", f'=IF(AND({CRED}="OUI",B{d0+2}>{PLAF}),"Contrôle","OK")', "Client à crédit et encours + TTC > plafond", "Pour un client comptant (plafond 0), pas de contrôle."),
    ("STATUT APRÈS SOUMISSION", f'=IF(OR(B{d0}>0,B{d0+1}>0,B{d0+3}="Contrôle"),"A_VALIDER (contrôle CHECKER)","VALIDEE (facturée directement)")', "Au moins un motif ⇒ À valider", "Sans motif : numéro de facture, sortie de stock, marge figée immédiatement."),
]
for lab, f, regle, expl in dec:
    ws.cell(row=r, column=1, value=lab).font = F_BOLD
    c = ws.cell(row=r, column=2, value=f); c.font = F_BOLD; c.fill = FILL_TOTAL
    if "Encours" in lab:
        c.number_format = MGA
    ws.cell(row=r, column=3, value=regle).font = F_BODY
    c = ws.cell(row=r, column=4, value=expl); c.fill = FILL_EXPL; c.font = F_BODY; c.alignment = WRAP
    for c_ in range(1, 5):
        ws.cell(row=r, column=c_).border = BORDER
        ws.cell(row=r, column=c_).alignment = WRAP
    r += 1
r += 1
r = h2(ws, r, "Encaissement et avoirs (après facturation)")
for c_, h in enumerate(["Élément", "Valeur", "Formule", "Explication"], 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == 4 else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
r += 1
p0 = r
pay = [
    ("Montant des avoirs émis (TTC)", 0, None, "Saisie : cumul TTC des avoirs sur cette facture.", True),
    ("Montant payé (paiements non annulés)", 500000, None, "Saisie : cumul des encaissements.", True),
    ("Net à payer", f"=MAX(B{t['ttc']}-B{p0},0)", "MAX(TTC − avoirs ; 0)", "Ce que le client doit après avoirs.", False),
    ("Reste à payer", f"=MAX(B{p0+2}-B{p0+1},0)", "MAX(net − payé ; 0)", "Plafond du prochain paiement.", False),
    ("Trop-perçu", f"=MAX(B{p0+1}-B{p0+2},0)", "MAX(payé − net ; 0)", "À rembourser au client.", False),
    ("Statut de paiement", f'=IF(AND(B{p0+2}=0,B{p0}>0),"Soldée par avoir",IF(B{p0+3}=0,"Payée",IF(B{p0+1}>0,"Partiellement payée","Non payée")))',
     "Ordre des tests de Vente.getStatutPaiement", "Calculé, jamais saisi.", False),
]
for lab, f, ftxt, expl, is_input in pay:
    ws.cell(row=r, column=1, value=lab).font = F_BOLD
    c = ws.cell(row=r, column=2, value=f)
    if is_input:
        c.font = F_INPUT; c.fill = FILL_INPUT
    else:
        c.font = F_BOLD; c.fill = FILL_TOTAL
    if "Statut" not in lab:
        c.number_format = MGA2
    ws.cell(row=r, column=3, value=ftxt).font = F_BODY
    c = ws.cell(row=r, column=4, value=expl); c.fill = FILL_EXPL; c.font = F_BODY; c.alignment = WRAP
    for c_ in range(1, 5):
        ws.cell(row=r, column=c_).border = BORDER
    r += 1
for col, w in zip("ABCDEFGHIJKLM", [38, 20, 40, 60, 18, 16, 18, 16, 16, 14, 14, 14, 60]):
    ws.column_dimensions[col].width = w

# =====================================================================
# 14. Simulateur Inventaire
# =====================================================================
ws = sheet("Simu Inventaire", "Simulateur : écarts d'inventaire et classement ABC",
           "Cellules jaunes à texte bleu = à modifier.",
           "Simulateur vivant : écart, valeur de l'écart, écart %, mouvement, classe ABC")
r = 5
r = h2(ws, r, "Écarts d'inventaire")
heads = ["Article", "Quantité théorique (photo)", "Quantité comptée", "CMUP revient figé", "Écart", "Valeur de l'écart",
         "Écart %", "Mouvement à la validation", "Motif obligatoire ?", "Explication"]
for c_, h in enumerate(heads, 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == len(heads) else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
ws.row_dimensions[r].height = 30
r += 1
inv = [("Riz 50 kg", 40, 38, 53680, "Manque de 2 : ajustement négatif au CMUP actuel."),
       ("Huile 20 L", 25, 26, 125000, "Surplus de 1 : ajustement positif au CMUP actuel."),
       ("Sucre 50 kg", 30, 30, 63500, "Aucun écart : aucun mouvement, pas de motif."),
       ("Sel 1 kg", 0, 4, 1200, "Théorique 0 : écart % non calculable (vide).")]
i0 = r
for art, th, co, cm, expl in inv:
    vals = [art, th, co, cm, f"=C{r}-B{r}", f"=ROUND(E{r}*D{r},0)",
            f'=IF(B{r}=0,"",ROUND(E{r}*100/B{r},1))',
            f'=IF(E{r}>0,"AJUSTEMENT_POSITIF",IF(E{r}<0,"AJUSTEMENT_NEGATIF","Aucun"))',
            f'=IF(E{r}=0,"Non","OUI")', expl]
    for c_, v in enumerate(vals, 1):
        cell = ws.cell(row=r, column=c_, value=v); cell.border = BORDER; cell.font = F_BODY; cell.alignment = WRAP
    for c_ in range(1, 5):
        ws.cell(row=r, column=c_).font = F_INPUT; ws.cell(row=r, column=c_).fill = FILL_INPUT
    ws.cell(row=r, column=10).fill = FILL_EXPL
    for c_ in (2, 3, 5):
        ws.cell(row=r, column=c_).number_format = QTE
    ws.cell(row=r, column=4).number_format = MGA4
    ws.cell(row=r, column=6).number_format = MGA
    ws.cell(row=r, column=7).number_format = PCT
    r += 1
ws.cell(row=r, column=1, value="Impact total")
ws.cell(row=r, column=6, value=f"=SUM(F{i0}:F{r-1})")
ws.cell(row=r, column=10, value="Somme des valeurs d'écart : impact financier de l'inventaire (au coût de revient).")
for c_ in range(1, 11):
    cell = ws.cell(row=r, column=c_); cell.font = F_BOLD; cell.fill = FILL_TOTAL; cell.border = BORDER; cell.alignment = WRAP
ws.cell(row=r, column=10).fill = FILL_EXPL; ws.cell(row=r, column=10).font = F_BODY
ws.cell(row=r, column=6).number_format = MGA
r += 3
r = h2(ws, r, "Classement ABC (saisir les articles triés par valeur décroissante)")
heads = ["Article", "Quantité", "CMUP revient", "Valeur de revient", "Cumul AVANT l'article", "Part cumulée avant", "Classe", "Explication"]
for c_, h in enumerate(heads, 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == len(heads) else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
ws.row_dimensions[r].height = 30
r += 1
abc = [("Groupe électrogène", 3, 2500000), ("Huile 20 L", 26, 125000), ("Riz 50 kg", 38, 53680),
       ("Sucre 50 kg", 30, 63500), ("Savon carton", 20, 45000), ("Sel 1 kg", 50, 1200), ("Article sans stock", 0, 0)]
a0 = r
a1 = r + len(abc) - 1
for i, (art, q, cm) in enumerate(abc):
    vals = [art, q, cm, f"=ROUND(B{r}*C{r},0)",
            0 if i == 0 else f"=E{r-1}+D{r-1}",
            f"=IF($D${a1+1}=0,0,ROUND(E{r}/$D${a1+1},4))",
            f'=IF(OR($D${a1+1}=0,D{r}=0),"C",IF(F{r}<0.8,"A",IF(F{r}<0.95,"B","C")))',
            ["Premier article : cumul avant = 0, donc toujours A.", "Cumul avant < 80 % ⇒ A.", "", "", "", "", "Valeur nulle ⇒ C."][i]]
    for c_, v in enumerate(vals, 1):
        cell = ws.cell(row=r, column=c_, value=v); cell.border = BORDER; cell.font = F_BODY; cell.alignment = WRAP
    for c_ in range(1, 4):
        ws.cell(row=r, column=c_).font = F_INPUT; ws.cell(row=r, column=c_).fill = FILL_INPUT
    ws.cell(row=r, column=8).fill = FILL_EXPL
    ws.cell(row=r, column=2).number_format = QTE
    ws.cell(row=r, column=3).number_format = MGA4
    ws.cell(row=r, column=4).number_format = MGA
    ws.cell(row=r, column=5).number_format = MGA
    ws.cell(row=r, column=6).number_format = "0.00%"
    r += 1
ws.cell(row=r, column=1, value="Total valeur du stock")
ws.cell(row=r, column=4, value=f"=SUM(D{a0}:D{a1})")
ws.cell(row=r, column=8, value="Base du calcul des parts. Seuils : A < 80 %, B < 95 %, sinon C.")
for c_ in range(1, 9):
    cell = ws.cell(row=r, column=c_); cell.font = F_BOLD; cell.fill = FILL_TOTAL; cell.border = BORDER; cell.alignment = WRAP
ws.cell(row=r, column=8).fill = FILL_EXPL; ws.cell(row=r, column=8).font = F_BODY
ws.cell(row=r, column=4).number_format = MGA
for col, w in zip("ABCDEFGHIJ", [26, 18, 18, 18, 20, 20, 26, 20, 18, 60]):
    ws.column_dimensions[col].width = w
ws.column_dimensions["H"].width = 50

# =====================================================================
# 15. Referentiels
# =====================================================================
ws = sheet("Référentiels", "Référentiels, numérotation et paramètres", "Valeurs fixes de l'application (énumérations)",
           "Devises, modes de paiement, types de frais, motifs d'écart, numérotation, paramètres")
r = 5
r = h2(ws, r, "Numérotation des documents")
r = table(ws, r, ["Document", "Préfixe", "Format", "Attribué quand", "Explication"], [
    ["Commande d'achat", "CA", "CA-AAAA-NNNN", "À la création", "Compteur par année (année de la date de commande)."],
    ["Bon de réception", "BR", "BR-AAAA-NNNN", "À la création", "Année de la date de réception."],
    ["Facture de vente", "FV", "FV-AAAA-NNNN", "À la validation seulement", "Suite continue sans trou : un brouillon supprimé ne consomme pas de numéro."],
    ["Avoir", "AV", "AV-AAAA-NNNN", "À la création", "Document légal de correction d'une facture."],
    ["Inventaire", "INV", "INV-AAAA-NNNN", "À l'ouverture", "—"],
    ["Article", "ART", "ART-0001", "À la création", "Compteur unique sans année ; saute les numéros déjà pris (anciennes références saisies à la main)."],
], [24, 10, 18, 24, 70])
r = h2(ws, r, "Paramètres généraux (écran Administration > Paramètres)")
r = table(ws, r, ["Paramètre", "Défaut", "Bornes", "Explication"], [
    ["Taux de TVA (%)", 20, "0 à 100", "Copié sur chaque vente à sa création. 0 % = non assujetti : ni TVA ni HT/TTC affichés."],
    ["Remise maximale vendeur (%)", 10, "0 à 100", "Remise que le vendeur peut accorder seul ; au-delà, contrôle par un CHECKER."],
    ["Identité de la société", "—", "—", "Raison sociale, adresse, téléphone, e-mail, NIF, STAT, RCS, mentions de facture : imprimés sur les factures."],
], [30, 10, 12, 80])
refs = []
for code, lib, expl in [("USD", "Dollar américain", ""), ("EUR", "Euro", ""), ("CNY", "Yuan chinois", ""), ("AED", "Dirham des Émirats", ""),
                        ("INR", "Roupie indienne", ""), ("JPY", "Yen japonais", ""), ("ZAR", "Rand sud-africain", ""),
                        ("MGA", "Ariary malgache", "Taux forcé à 1. Le stock est toujours valorisé en MGA.")]:
    refs.append(["Devise d'achat", code, lib, expl or "Devise du fournisseur ; convertie en MGA au taux de la réception."])
for code, lib in [("ESPECES", "Espèces"), ("MVOLA", "MVola"), ("ORANGE_MONEY", "Orange Money"), ("AIRTEL_MONEY", "Airtel Money"),
                  ("VIREMENT", "Virement bancaire"), ("CHEQUE", "Chèque")]:
    refs.append(["Mode de paiement", code, lib, "Pas de référence obligatoire." if code == "ESPECES" else "Référence obligatoire (n° de transaction / chèque / virement)."])
for code, lib in [("FRET", "Fret / transport"), ("DOUANE", "Droits et taxes de douane"), ("TRANSIT", "Honoraires de transit"),
                  ("MANUTENTION", "Manutention"), ("STOCKAGE", "Magasinage / stockage"), ("AUTRE", "Autre")]:
    refs.append(["Type de frais (réception)", code, lib, "S'ajoute au coût de revient, réparti au prorata de la valeur reçue."])
for code, lib in [("CASSE", "Casse / détérioration"), ("PEREMPTION", "Produit périmé"), ("VOL_PERTE", "Vol ou perte inexpliquée"),
                  ("ERREUR_RECEPTION", "Erreur de réception (quantité saisie)"), ("ERREUR_VENTE", "Erreur de vente / livraison"),
                  ("ECHANTILLON", "Échantillon / usage interne"), ("AUTRE", "Autre (préciser en commentaire)")]:
    refs.append(["Motif d'écart (inventaire)", code, lib, "Commentaire obligatoire." if code == "AUTRE" else "Obligatoire pour toute ligne avec écart."])
refs += [["Type d'inventaire", "COMPLET", "Complet", "Tous les articles — inventaire annuel de clôture."],
         ["Type d'inventaire", "TOURNANT", "Tournant", "Sélection d'articles — comptage régulier sans fermer le magasin."],
         ["Type de client", "PARTICULIER", "Particulier", "Le client « comptoir » est toujours un particulier."],
         ["Type de client", "PROFESSIONNEL", "Professionnel", "NIF et STAT obligatoires (mentions de facture)."]]
r = h2(ws, r, "Énumérations")
table(ws, r, ["Référentiel", "Code", "Libellé", "Explication"], refs, [30, 20, 36, 70])

# =====================================================================
# 16. Glossaire
# =====================================================================
ws = sheet("Glossaire", "Glossaire", "Vocabulaire métier utilisé dans l'application",
           "Définitions : CMUP, revient, maker/checker, avoir, encours, taux de marque…")
r = 5
table(ws, r, ["Terme", "Définition", "Explication / usage dans l'application"], [
    ["CMUP", "Coût Moyen Unitaire Pondéré", "Coût moyen d'une unité en stock, pondéré par les quantités. Recalculé à chaque entrée, inchangé par les sorties. Voir « Simu CMUP »."],
    ["CMUP achat", "CMUP calculé sur le prix fournisseur converti en MGA", "Valorise le stock au prix d'achat pur."],
    ["CMUP revient", "CMUP calculé sur le coût de revient (achat + frais)", "Sert à la marge, au contrôle de vente à perte et au classement ABC."],
    ["Coût d'achat unitaire MGA", "Prix fournisseur × taux de change de la réception", "Première composante du revient."],
    ["Frais d'approche", "Fret, douane, transit, manutention, stockage…", "Facturés par le transitaire, répartis sur les articles reçus au prorata de leur valeur."],
    ["Revient unitaire", "Coût d'achat unitaire MGA + frais unitaire", "Ce que coûte vraiment une unité rendue en magasin. Figé sur la ligne de réception à la validation."],
    ["Taux de change", "Nombre de MGA pour 1 unité de devise", "Celui de la réception (pas de la commande) sert à valoriser le stock."],
    ["Maker", "Initiateur : celui qui saisit", "Rôle MAKER pour les réceptions ; par extension le vendeur ou le compteur d'inventaire."],
    ["Checker", "Validateur : celui qui contrôle", "Rôle CHECKER : valide/rejette, approuve/refuse. Toujours une autre personne que l'auteur."],
    ["Principe des 4 yeux", "Deux personnes différentes pour saisir et valider", "Réduit les erreurs et les fraudes : personne ne valide son propre travail."],
    ["Brouillon", "Document en préparation", "Modifiable et supprimable, sans effet sur le stock ni numéro légal (pour les ventes)."],
    ["Facture", "Vente au statut VALIDEE", "Numérotée, définitive : elle ne change plus."],
    ["Avoir", "Document qui annule tout ou partie d'une facture", "Seule façon de corriger une facture ; peut faire revenir la marchandise en stock."],
    ["HT / TVA / TTC", "Hors taxes / taxe sur la valeur ajoutée / toutes taxes comprises", "TTC = HT + TVA ; TVA calculée une seule fois sur le total HT."],
    ["Remise", "Réduction en % sur le prix catalogue", "Plafonnée pour le vendeur (paramètre). Au-delà : contrôle."],
    ["Marge", "Montant HT − coût de revient", "Figée à la facturation."],
    ["Taux de marque", "Marge ÷ chiffre d'affaires HT", "Exprimé en %. À ne pas confondre avec le taux de marge (marge ÷ coût)."],
    ["Vente à perte", "Prix net HT < CMUP revient", "Autorisée seulement après contrôle par un CHECKER."],
    ["Encours client", "Total des restes à payer des factures validées du client", "Comparé au plafond de crédit."],
    ["Plafond de crédit", "Encours maximum autorisé pour un client", "0 = paiement comptant uniquement."],
    ["Client comptoir", "Client générique pour les ventes anonymes", "Toujours particulier, ne peut pas être désactivé, pas de contrôle de crédit."],
    ["Net à payer / Reste à payer", "TTC − avoirs / net − paiements", "Calculés, jamais saisis."],
    ["Trop-perçu", "Paiements − net à payer (si positif)", "À rembourser au client, typiquement après un avoir."],
    ["Mouvement de stock", "Trace d'une entrée ou d'une sortie", "Quantité, coût, stock et CMUP après, document d'origine, utilisateur."],
    ["Inventaire à l'aveugle", "Comptage sans voir la quantité théorique", "Évite que le compteur soit influencé."],
    ["Gel", "Blocage des mouvements d'un article", "Pendant un inventaire ouvert : ni réception, ni vente, ni retour sur l'article."],
    ["Classement ABC", "Tri des articles par poids dans la valeur du stock", "A : 80 % de la valeur ; B : 80–95 % ; C : le reste."],
    ["Reliquat / clôture", "Partie non livrée d'une commande", "Clôturer = abandonner le reliquat."],
    ["Verrou optimiste (version)", "Numéro de version du document", "Empêche d'écraser la modification d'un collègue : « Rechargez la page »."],
    ["Verrou pessimiste", "Lecture « pour mise à jour » en base", "Deux validations ou ventes simultanées sur le même stock passent l'une après l'autre."],
], [28, 50, 90])

# =====================================================================
# Sommaire
# =====================================================================
ws = ws_sommaire
ws.sheet_view.showGridLines = False
ws["A1"] = "Documentation fonctionnelle — Tantana"
ws["A1"].font = F_TITLE
ws["A2"] = "Gestion des achats, réceptions, stock, ventes, encaissements, avoirs et inventaires. Montants en Ariary (MGA)."
ws["A2"].font = F_SUB
ws["A3"] = "Cliquez sur un onglet pour l'ouvrir. Dans chaque tableau, la dernière colonne (fond jaune pâle) explique ce que fait la ligne."
ws["A3"].font = F_SUB
r = 5
for c_, h in enumerate(["N°", "Onglet", "Contenu", "Explication"], 1):
    cell = ws.cell(row=r, column=c_, value=h)
    cell.font = F_HEAD; cell.fill = FILL_EXPL_HEAD if c_ == 4 else FILL_HEAD; cell.alignment = CENTER; cell.border = BORDER
r += 1
usage = {
    "Vue d'ensemble": "À lire en premier : la carte de l'application.",
    "Rôles et droits": "Pour attribuer les rôles aux utilisateurs.",
    "Cycles de vie": "Pour comprendre un statut affiché à l'écran.",
    "Validations": "Réponse à « quand faut-il une validation ? ».",
    "Actions et validation": "Filtrez la colonne « Validation requise ? » pour voir chaque action.",
    "Commandes": "Règles détaillées du module Achats.",
    "Réceptions": "Règles détaillées et étapes de validation.",
    "Ventes": "Règles de saisie, de contrôle et de facturation.",
    "Paiements et avoirs": "Règles d'encaissement et de correction de facture.",
    "Stock": "Comment et quand le stock bouge.",
    "Inventaire": "Procédure de comptage et classement ABC.",
    "Formules": "Référence de toutes les formules avec exemple.",
    "Simu CMUP": "Modifiez les cellules jaunes pour tester vos propres chiffres.",
    "Simu Vente": "Testez une facture : remise, TVA, marge, contrôle, paiement.",
    "Simu Inventaire": "Testez des écarts et un classement ABC.",
    "Référentiels": "Listes de valeurs et numérotation.",
    "Glossaire": "Vocabulaire métier.",
}
for i, (name, desc) in enumerate(SOMMAIRE, 1):
    ws.cell(row=r, column=1, value=i)
    c = ws.cell(row=r, column=2, value=name)
    c.hyperlink = f"#'{name}'!A1"
    ws.cell(row=r, column=3, value=desc)
    ws.cell(row=r, column=4, value=usage.get(name, ""))
    for c_ in range(1, 5):
        cell = ws.cell(row=r, column=c_); cell.border = BORDER; cell.alignment = WRAP; cell.font = F_BODY
    ws.cell(row=r, column=2).font = F_LINK
    ws.cell(row=r, column=4).fill = FILL_EXPL
    r += 1
r += 1
ws.cell(row=r, column=1, value="Légende des couleurs").font = F_H2
r += 1
leg = [("Texte bleu sur fond jaune", "Valeur à saisir / modifier (simulateurs)", FILL_INPUT, F_INPUT),
       ("Fond bleu clair", "Total ou résultat calculé par formule", FILL_TOTAL, F_BOLD),
       ("Fond jaune pâle", "Colonne Explication", FILL_EXPL, F_BODY)]
for lab, expl, fill, font in leg:
    c = ws.cell(row=r, column=2, value=lab); c.fill = fill; c.font = font; c.border = BORDER
    c = ws.cell(row=r, column=3, value=expl); c.font = F_BODY; c.border = BORDER
    r += 1
r += 1
ws.cell(row=r, column=1, value="Source : code de l'application (services et entités Java, migrations SQL). Les simulateurs reproduisent les formules et arrondis du code.").font = F_SUB
for col, w in zip("ABCD", [6, 26, 70, 60]):
    ws.column_dimensions[col].width = w

# Mise en page commune
for s in wb.worksheets:
    s.page_setup.orientation = "landscape"
    s.page_setup.fitToWidth = 1
    s.page_setup.fitToHeight = 0
    s.sheet_properties.pageSetUpPr.fitToPage = True

wb.calculation.fullCalcOnLoad = True
wb.save(OUT)
print("OK", OUT, [s.title for s in wb.worksheets])
