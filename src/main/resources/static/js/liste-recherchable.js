/*
 * Listes deroulantes recherchables (Tom Select).
 *
 * Tout <select data-recherche="Texte d'aide"> devient un champ ou l'on tape pour filtrer
 * (reference, nom, pays...). Le <select> d'origine reste dans le formulaire et garde sa valeur :
 * les scripts existants (change, selectedOptions, dataset des options) fonctionnent sans changement.
 *
 * Ajouter data-recherche-vide pour garder l'option vide choisissable (filtres « Tous … »).
 *
 * Une option peut porter data-detail="..." : affiche sous le libelle et inclus dans la recherche
 * (ex. « Stock 25 Pièce · 1 007 900 MGA HT »).
 *
 * Les listes portant le meme data-unique="groupe" s'excluent : une valeur choisie dans l'une
 * n'est plus proposee dans les autres (ex. un article deja present sur une ligne de vente).
 *
 * Les lignes ajoutees dynamiquement sont activees automatiquement ; pour donner le focus a une
 * nouvelle ligne : ListeRecherchable.activer(select).focus().
 */
(function () {
    if (typeof TomSelect === 'undefined') return;

    /**
     * Valeurs deja choisies dans les AUTRES listes du meme groupe data-unique
     * (ex. un article deja present sur une autre ligne de la vente).
     */
    function valeursPrises(select) {
        const groupe = select.dataset.unique;
        if (!groupe) return new Set();
        return new Set([...document.querySelectorAll('select[data-unique]')]
            .filter((s) => s !== select && s.dataset.unique === groupe && s.value !== '')
            .map((s) => s.value));
    }

    function activer(select) {
        if (!select) return null;
        if (select.tomselect) return select.tomselect;
        const liste = creer(select);
        if (select.dataset.unique) {
            // resultats de recherche sans les valeurs deja choisies sur les autres lignes
            const rechercher = liste.search.bind(liste);
            liste.search = (texte) => {
                const resultat = rechercher(texte);
                const prises = valeursPrises(select);
                return prises.size === 0 ? resultat
                    : Object.assign({}, resultat, {items: resultat.items.filter((i) => !prises.has(String(i.id)))});
            };
        }
        return liste;
    }

    function creer(select) {
        return new TomSelect(select, {
            maxOptions: 200,
            // filtrage immediat (300 ms par defaut) : sinon « lemur » + Entree choisit
            // la premiere option de la liste non encore filtree
            refreshThrottle: 0,
            placeholder: select.dataset.recherche || 'Rechercher…',
            // filtres de liste : l'option vide (« Tous les fournisseurs ») reste choisissable
            allowEmptyOption: select.hasAttribute('data-recherche-vide'),
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
