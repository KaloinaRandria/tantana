package mg.working.tantana.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Client;
import mg.working.tantana.entity.LigneVente;
import mg.working.tantana.entity.Parametre;
import mg.working.tantana.entity.StatutVente;
import mg.working.tantana.entity.TypeClient;
import mg.working.tantana.entity.Vente;
import mg.working.tantana.exception.RegleMetierException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Generation de la facture PDF (OpenPDF) d'une vente validee. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FactureService {

    private static final Color GRIS = new Color(100, 116, 139);
    private static final Color FOND_ENTETE = new Color(30, 41, 59);
    private static final Color FOND_CLAIR = new Color(241, 245, 249);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VenteService venteService;
    private final ParametreService parametreService;

    public byte[] genererPdf(Long venteId) {
        Vente vente = venteService.trouverAvecDetails(venteId);
        if (vente.getStatut() != StatutVente.VALIDEE) {
            throw new RegleMetierException("Seule une vente validée a une facture.");
        }
        Parametre societe = parametreService.obtenir();

        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, sortie);
            doc.addTitle("Facture " + vente.getNumero());
            doc.open();
            entete(doc, societe, vente);
            doc.add(lignes(vente));
            doc.add(totaux(vente));
            piedDePage(doc, societe, vente);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer la facture " + vente.getNumero(), e);
        }
        return sortie.toByteArray();
    }

    // ===================== Blocs =====================

    private void entete(Document doc, Parametre s, Vente v) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{55, 45});
        t.setWidthPercentage(100);

        // Societe
        PdfPCell gauche = cellule();
        gauche.addElement(new Paragraph(s.getRaisonSociale(), police(16, Font.BOLD, FOND_ENTETE)));
        ajouterSi(gauche, s.getAdresse());
        ajouterSi(gauche, joindre(s.getTelephone(), s.getEmail()));
        ajouterSi(gauche, joindre(prefixe("NIF ", s.getNif()), prefixe("STAT ", s.getStat())));
        ajouterSi(gauche, prefixe("RCS ", s.getRcs()));
        t.addCell(gauche);

        // Titre + client
        PdfPCell droite = cellule();
        Paragraph titre = new Paragraph("FACTURE", police(20, Font.BOLD, FOND_ENTETE));
        titre.setAlignment(Element.ALIGN_RIGHT);
        droite.addElement(titre);
        Paragraph num = new Paragraph("N° " + v.getNumero() + "   du " + v.getDateVente().format(DATE), police(10, Font.BOLD, null));
        num.setAlignment(Element.ALIGN_RIGHT);
        droite.addElement(num);
        t.addCell(droite);
        doc.add(t);

        PdfPTable cadre = new PdfPTable(new float[]{55, 45});
        cadre.setWidthPercentage(100);
        cadre.setSpacingBefore(14);
        cadre.addCell(cellule());
        PdfPCell client = new PdfPCell();
        client.setBorderColor(GRIS);
        client.setPadding(8);
        Client c = v.getClient();
        client.addElement(new Paragraph("Facturé à", police(8, Font.NORMAL, GRIS)));
        client.addElement(new Paragraph(c.getNom(), police(11, Font.BOLD, null)));
        ajouterSi(client, c.getAdresse());
        ajouterSi(client, c.getTelephone());
        if (c.getType() == TypeClient.PROFESSIONNEL) {
            ajouterSi(client, joindre(prefixe("NIF ", c.getNif()), prefixe("STAT ", c.getStat())));
        }
        if (v.getLieuLivraison() != null) {
            Paragraph livraison = new Paragraph("Livré à", police(8, Font.NORMAL, GRIS));
            livraison.setSpacingBefore(4);
            client.addElement(livraison);
            ajouterSi(client, v.getLieuLivraison());
        }
        cadre.addCell(client);
        doc.add(cadre);
    }

    private PdfPTable lignes(Vente v) {
        PdfPTable t = new PdfPTable(new float[]{14, 36, 10, 15, 9, 16});
        t.setWidthPercentage(100);
        t.setSpacingBefore(16);
        t.setHeaderRows(1);
        String ht = v.isTvaApplicable() ? " HT" : "";
        for (String titre : new String[]{"Réf.", "Désignation", "Qté", "P.U." + ht, "Remise", "Montant" + ht}) {
            PdfPCell h = new PdfPCell(new Phrase(titre, police(9, Font.BOLD, Color.WHITE)));
            h.setBackgroundColor(FOND_ENTETE);
            h.setPadding(5);
            h.setBorderColor(FOND_ENTETE);
            h.setHorizontalAlignment(titre.startsWith("Réf") || titre.startsWith("Dés") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            t.addCell(h);
        }
        boolean pair = false;
        for (LigneVente l : v.getLignes()) {
            Color fond = pair ? FOND_CLAIR : Color.WHITE;
            t.addCell(texte(l.getArticle().getReference(), fond, Element.ALIGN_LEFT));
            t.addCell(texte(l.getArticle().getDesignation(), fond, Element.ALIGN_LEFT));
            t.addCell(texte(l.getQuantite().stripTrailingZeros().toPlainString(), fond, Element.ALIGN_RIGHT));
            t.addCell(texte(ariary(l.getPrixCatalogueHt()), fond, Element.ALIGN_RIGHT));
            t.addCell(texte(l.getRemisePct().signum() == 0 ? "—" : l.getRemisePct().stripTrailingZeros().toPlainString() + " %",
                    fond, Element.ALIGN_RIGHT));
            t.addCell(texte(ariary(l.getMontantHt()), fond, Element.ALIGN_RIGHT));
            pair = !pair;
        }
        return t;
    }

    private PdfPTable totaux(Vente v) {
        PdfPTable t = new PdfPTable(new float[]{60, 22, 18});
        t.setWidthPercentage(100);
        t.setSpacingBefore(8);
        boolean tva = v.isTvaApplicable();
        if (v.getTotalRemise().signum() > 0) {
            ligneTotal(t, tva ? "Total brut HT" : "Total brut", v.getTotalBrutHt(), false);
            ligneTotal(t, "Remises", v.getTotalRemise().negate(), false);
        }
        if (v.isAvecFraisLivraison()) {
            ligneTotal(t, tva ? "Articles HT" : "Articles", v.getTotalArticlesHt(), false);
            ligneTotal(t, tva ? "Frais de livraison HT" : "Frais de livraison", v.getFraisLivraison(), false);
        }
        if (tva) {
            ligneTotal(t, "Total HT", v.getTotalHt(), false);
            ligneTotal(t, "TVA " + v.getTauxTva().stripTrailingZeros().toPlainString() + " %", v.getTotalTva(), false);
            ligneTotal(t, "Total TTC (Ariary)", v.getTotalTtc(), true);
        } else {
            // TVA 0 % : entreprise non assujettie, ni TVA ni mention HT / TTC sur la facture
            ligneTotal(t, "Total (Ariary)", v.getTotalTtc(), true);
        }
        return t;
    }

    private void piedDePage(Document doc, Parametre s, Vente v) throws DocumentException {
        Paragraph lettres = new Paragraph();
        lettres.setSpacingBefore(14);
        lettres.add(new Chunk("Arrêtée la présente facture à la somme de : ", police(9, Font.NORMAL, null)));
        String montant = MontantEnLettres.ecrire(v.getTotalTtc());
        lettres.add(new Chunk(Character.toUpperCase(montant.charAt(0)) + montant.substring(1) + " Ariary",
                police(9, Font.BOLD, null)));
        doc.add(lettres);

        Paragraph vendeur = new Paragraph("Vendeur : " + v.getVendeur().getNomComplet(), police(8, Font.NORMAL, GRIS));
        vendeur.setSpacingBefore(10);
        doc.add(vendeur);

        if (s.getMentionsFacture() != null) {
            Paragraph mentions = new Paragraph(s.getMentionsFacture(), police(8, Font.ITALIC, GRIS));
            mentions.setSpacingBefore(20);
            mentions.setAlignment(Element.ALIGN_CENTER);
            doc.add(mentions);
        }
    }

    // ===================== Outils =====================

    private void ligneTotal(PdfPTable t, String libelle, BigDecimal montant, boolean principal) {
        t.addCell(cellule());
        Font f = police(principal ? 11 : 9, principal ? Font.BOLD : Font.NORMAL, principal ? Color.WHITE : null);
        PdfPCell l = new PdfPCell(new Phrase(libelle, f));
        PdfPCell m = new PdfPCell(new Phrase(ariary(montant), f));
        for (PdfPCell c : new PdfPCell[]{l, m}) {
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(4);
            if (principal) {
                c.setBackgroundColor(FOND_ENTETE);
            }
        }
        m.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l);
        t.addCell(m);
    }

    private PdfPCell texte(String valeur, Color fond, int alignement) {
        PdfPCell c = new PdfPCell(new Phrase(valeur, police(9, Font.NORMAL, null)));
        c.setBackgroundColor(fond);
        c.setBorderColor(FOND_CLAIR);
        c.setPadding(5);
        c.setHorizontalAlignment(alignement);
        return c;
    }

    private PdfPCell cellule() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private void ajouterSi(PdfPCell cellule, String texte) {
        if (texte != null && !texte.isBlank()) {
            cellule.addElement(new Paragraph(texte, police(9, Font.NORMAL, null)));
        }
    }

    private static Font police(float taille, int style, Color couleur) {
        Font f = new Font(Font.HELVETICA, taille, style);
        if (couleur != null) {
            f.setColor(couleur);
        }
        return f;
    }

    private static String joindre(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return a + "  ·  " + b;
    }

    private static String prefixe(String prefixe, String valeur) {
        return valeur == null ? null : prefixe + valeur;
    }

    /** 1234567 -> « 1 234 567,00 » (Ariary, 2 decimales). */
    private static String ariary(BigDecimal montant) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0.00", symboles).format(montant.setScale(2, java.math.RoundingMode.HALF_UP));
    }
}
