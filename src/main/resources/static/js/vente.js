/*
 * Saisie d'une vente : prix catalogue et stock affiches selon l'article, montants et totaux
 * en direct (TVA sur le total HT, arrondie a l'Ariary comme sur la facture), alertes
 * remise > plafond et quantite > stock. Le serveur recalcule tout : ceci n'est qu'un apercu.
 */
(function () {
    const form = document.getElementById('form-vente');
    if (!form) return;

    const corps = form.querySelector('#table-lignes tbody');
    const modele = document.getElementById('modele-ligne');
    const tva = parseFloat(form.dataset.tva) || 0;
    const remiseMax = parseFloat(form.dataset.remiseMax) || 0;
    const fmt = new Intl.NumberFormat('fr-FR', {maximumFractionDigits: 0});
    const fmtQte = new Intl.NumberFormat('fr-FR', {maximumFractionDigits: 3});
    const nombre = (c) => { const v = parseFloat(c && c.value); return isNaN(v) ? 0 : v; };

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
        let totalHt = 0, controle = false, manque = false;
        corps.querySelectorAll('tr.ligne').forEach((tr) => {
            const option = tr.querySelector('.champ-article').selectedOptions[0];
            const prix = option && option.dataset.prix ? parseFloat(option.dataset.prix) : null;
            const stock = option && option.dataset.stock ? parseFloat(option.dataset.stock) : null;
            const qte = nombre(tr.querySelector('.champ-quantite'));
            const remise = nombre(tr.querySelector('.champ-remise'));

            tr.querySelector('.prix').textContent = prix === null ? '—' : fmt.format(prix);
            const celStock = tr.querySelector('.stock');
            celStock.textContent = stock === null ? '—' : fmtQte.format(stock) + ' ' + (option.dataset.unite || '');
            celStock.classList.toggle('text-danger', stock !== null && qte > stock);
            celStock.classList.toggle('fw-semibold', stock !== null && qte > stock);

            const net = prix === null ? 0 : Math.round(prix * (100 - remise)) / 100;
            const montant = net * qte;
            tr.querySelector('.montant').textContent = fmt.format(montant);
            tr.querySelector('.champ-remise').classList.toggle('border-warning', remise > remiseMax);

            totalHt += montant;
            controle = controle || remise > remiseMax;
            manque = manque || (stock !== null && qte > stock);
        });
        const ht = Math.round(totalHt);
        const montantTva = Math.round(ht * tva / 100);
        document.getElementById('total-ht').textContent = fmt.format(ht);
        document.getElementById('total-tva').textContent = fmt.format(montantTva);
        document.getElementById('total-ttc').textContent = fmt.format(ht + montantTva);
        document.getElementById('alerte-controle').classList.toggle('d-none', !controle);
        document.getElementById('alerte-stock').classList.toggle('d-none', !manque);
    }

    document.getElementById('ajouter-ligne').addEventListener('click', () => {
        corps.appendChild(modele.content.cloneNode(true));
        renumeroter();
        recalculer();
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
    corps.addEventListener('change', recalculer);
    renumeroter();
    recalculer();
})();
