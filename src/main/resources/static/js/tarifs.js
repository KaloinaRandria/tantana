/*
 * Page Tarifs : remplit la fenetre « Prix de vente » avec la ligne cliquee
 * et calcule en direct le TTC et la marge (taux de marque).
 */
(function () {
    const fenetre = document.getElementById('modal-prix');
    if (!fenetre) return;

    const form = document.getElementById('form-prix');
    const champPrix = form.querySelector('#prix');
    const champMotif = form.querySelector('#motif');
    const tva = parseFloat(champPrix.dataset.tva) || 0;
    const fmt = new Intl.NumberFormat('fr-FR', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    let revient = null;

    function recalculer() {
        const prix = parseFloat(champPrix.value);
        const valide = !isNaN(prix);
        fenetre.querySelector('[data-role="ttc"]').textContent = valide ? fmt.format(prix * (100 + tva) / 100) : '—';

        const zoneMarge = fenetre.querySelector('[data-role="marge"]');
        const alerte = fenetre.querySelector('[data-role="alerte-perte"]');
        if (!valide || revient === null) {
            zoneMarge.textContent = '—';
            zoneMarge.className = '';
            alerte.classList.add('d-none');
            champMotif.required = false;
            return;
        }
        const marge = prix - revient;
        const taux = prix > 0 ? (marge * 100 / prix).toFixed(1).replace('.', ',') : '—';
        zoneMarge.textContent = `${fmt.format(marge)} MGA (${taux} %)`;
        zoneMarge.className = marge < 0 ? 'text-danger' : 'text-success';
        alerte.classList.toggle('d-none', marge >= 0);
        champMotif.required = marge < 0;
    }

    fenetre.addEventListener('show.bs.modal', (e) => {
        const bouton = e.relatedTarget;
        form.action = form.dataset.actionBase + bouton.dataset.articleId;
        fenetre.querySelector('[data-role="article"]').textContent = bouton.dataset.articleLibelle;
        revient = bouton.dataset.revient ? parseFloat(bouton.dataset.revient) : null;
        fenetre.querySelector('[data-role="revient"]').textContent = revient === null ? 'non reçu' : fmt.format(revient);
        champPrix.value = bouton.dataset.prix ? Math.round(parseFloat(bouton.dataset.prix)) : '';
        champMotif.value = '';
        recalculer();
    });

    fenetre.addEventListener('shown.bs.modal', () => champPrix.focus());
    champPrix.addEventListener('input', recalculer);
})();
