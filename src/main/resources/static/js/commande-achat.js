/*
 * Formulaire de commande d'achat :
 *  - ajout / suppression de lignes (renumerotation lignes[0], lignes[1]... pour Spring)
 *  - montants de ligne et totaux recalcules en direct
 *  - devise proposee depuis le fournisseur, taux force a 1 pour l'Ariary
 */
(function () {
    const form = document.getElementById('form-commande');
    if (!form) return;

    const corps = form.querySelector('#table-lignes tbody');
    const modele = document.getElementById('modele-ligne');
    const champFournisseur = form.querySelector('#fournisseurId');
    const champDevise = form.querySelector('#devise');
    const champTaux = form.querySelector('#tauxChange');

    const formatDevise = new Intl.NumberFormat('fr-FR', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    const formatAriary = new Intl.NumberFormat('fr-FR', {maximumFractionDigits: 0});

    const nombre = (champ) => {
        const v = parseFloat(champ && champ.value);
        return isNaN(v) ? 0 : v;
    };

    /** Redonne a chaque champ le nom attendu par Spring : lignes[i].quantite, etc. */
    function renumeroter() {
        corps.querySelectorAll('tr.ligne').forEach((tr, i) => {
            tr.querySelectorAll('select, input').forEach((champ) => {
                const nom = champ.dataset.champ || (champ.name.match(/\.(\w+)$/) || [])[1];
                if (!nom) return;
                champ.dataset.champ = nom;
                champ.name = `lignes[${i}].${nom}`;
                champ.id = `lignes${i}.${nom}`;
            });
        });
    }

    function recalculer() {
        let total = 0;
        corps.querySelectorAll('tr.ligne').forEach((tr) => {
            const montant = nombre(tr.querySelector('.champ-quantite')) * nombre(tr.querySelector('.champ-prix'));
            tr.querySelector('.montant-ligne').textContent = formatDevise.format(montant);
            total += montant;
        });
        document.getElementById('total-devise').textContent = formatDevise.format(total);
        document.getElementById('total-mga').textContent = formatAriary.format(total * nombre(champTaux));
    }

    function majDevise() {
        const devise = champDevise.value || '?';
        form.querySelectorAll('.libelle-devise').forEach((el) => (el.textContent = devise));
        if (devise === 'MGA') {
            champTaux.value = 1;
            champTaux.readOnly = true;
        } else {
            champTaux.readOnly = false;
        }
        recalculer();
    }

    document.getElementById('ajouter-ligne').addEventListener('click', () => {
        corps.appendChild(modele.content.cloneNode(true));
        renumeroter();
        // nouvelle ligne : le curseur va dans la recherche d'article
        const select = corps.querySelector('tr.ligne:last-child .champ-article');
        const liste = window.ListeRecherchable ? window.ListeRecherchable.activer(select) : null;
        (liste || select).focus();
    });

    corps.addEventListener('click', (e) => {
        const bouton = e.target.closest('.supprimer-ligne');
        if (!bouton) return;
        bouton.closest('tr').remove();
        renumeroter();
        recalculer();
    });

    corps.addEventListener('input', recalculer);
    champTaux.addEventListener('input', recalculer);
    champDevise.addEventListener('change', majDevise);

    // Nouveau fournisseur choisi : on propose sa devise habituelle
    champFournisseur.addEventListener('change', () => {
        const option = champFournisseur.selectedOptions[0];
        if (option && option.dataset.devise) {
            champDevise.value = option.dataset.devise;
            majDevise();
        }
    });

    renumeroter();
    majDevise();
})();
