/*
 * Listes deroulantes recherchables (Tom Select).
 *
 * Tout <select data-recherche="Texte d'aide"> devient un champ ou l'on tape pour filtrer
 * (reference, nom, pays...). Le <select> d'origine reste dans le formulaire et garde sa valeur :
 * les scripts existants (change, selectedOptions, dataset des options) fonctionnent sans changement.
 *
 * Une option peut porter data-detail="..." : affiche sous le libelle et inclus dans la recherche
 * (ex. « Stock 25 Pièce · 1 007 900 MGA HT »).
 *
 * Les lignes ajoutees dynamiquement sont activees automatiquement ; pour donner le focus a une
 * nouvelle ligne : ListeRecherchable.activer(select).focus().
 */
(function () {
    if (typeof TomSelect === 'undefined') return;

    function activer(select) {
        if (!select) return null;
        if (select.tomselect) return select.tomselect;
        return new TomSelect(select, {
            maxOptions: 200,
            // filtrage immediat (300 ms par defaut) : sinon « lemur » + Entree choisit
            // la premiere option de la liste non encore filtree
            refreshThrottle: 0,
            placeholder: select.dataset.recherche || 'Rechercher…',
            searchField: ['text', 'detail'],
            // hors du tableau : la liste n'est pas coupee par le defilement horizontal
            dropdownParent: 'body',
            // pendant la frappe, la valeur actuelle est masquee (on ne lit que ce qu'on tape)
            onType(texte) { this.wrapper.classList.toggle('en-saisie', texte !== ''); },
            onBlur() { this.wrapper.classList.remove('en-saisie'); },
            onChange(valeur) {
                this.wrapper.classList.remove('en-saisie');
                // une valeur choisie efface l'erreur affichee par le serveur pour ce champ
                if (valeur !== '') {
                    this.wrapper.classList.remove('is-invalid');
                    this.input.classList.remove('is-invalid');
                    const message = this.wrapper.parentElement.querySelector('.invalid-feedback');
                    if (message) message.textContent = '';
                }
            },
            render: {
                option: (d, echapper) => '<div><div>' + echapper(d.text) + '</div>'
                    + (d.detail ? '<div class="small text-muted">' + echapper(d.detail) + '</div>' : '') + '</div>',
                item: (d, echapper) => '<div>' + echapper(d.text) + '</div>',
                no_results: (d, echapper) => '<div class="no-results">Aucun résultat pour « ' + echapper(d.input) + ' »</div>'
            }
        });
    }

    function activerDans(racine) {
        if (racine.matches && racine.matches('select[data-recherche]')) activer(racine);
        if (racine.querySelectorAll) racine.querySelectorAll('select[data-recherche]').forEach(activer);
    }

    activerDans(document);

    // Lignes ajoutees par « Ajouter un article » (clone d'un <template>) ; lignes supprimees :
    // leur liste deroulante, rattachee a <body>, est retiree avec elles.
    new MutationObserver((mutations) => mutations.forEach((m) => {
        m.addedNodes.forEach((n) => {
            if (n.nodeType === Node.ELEMENT_NODE) activerDans(n);
        });
        m.removedNodes.forEach((n) => {
            if (n.nodeType !== Node.ELEMENT_NODE || !n.querySelectorAll) return;
            n.querySelectorAll('select').forEach((s) => {
                if (s.tomselect && s.tomselect.dropdown) s.tomselect.dropdown.remove();
            });
        });
    })).observe(document.body, {childList: true, subtree: true});

    window.ListeRecherchable = {activer};
})();
