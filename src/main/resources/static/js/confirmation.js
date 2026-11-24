/*
 * Confirmation commune (remplace les confirm() du navigateur).
 *
 * Tout <form data-confirmation="..."> affiche la fenetre #modal-confirmation
 * avant d'etre envoye. Attributs optionnels :
 *   data-confirmation-titre, data-confirmation-bouton, data-confirmation-style
 */
(function () {
    const fenetre = document.getElementById('modal-confirmation');
    if (!fenetre || typeof bootstrap === 'undefined') return;

    const modal = bootstrap.Modal.getOrCreateInstance(fenetre);
    const titre = fenetre.querySelector('[data-role="titre"]');
    const message = fenetre.querySelector('[data-role="message"]');
    const icone = fenetre.querySelector('[data-role="icone"]');
    const bouton = fenetre.querySelector('[data-role="confirmer"]');

    const ICONES = {
        danger: 'bi-exclamation-octagon text-danger',
        warning: 'bi-exclamation-triangle text-warning',
        success: 'bi-check2-circle text-success',
        dark: 'bi-lock text-dark',
        primary: 'bi-question-circle text-primary'
    };

    let formulaireEnAttente = null;

    document.addEventListener('submit', (e) => {
        const form = e.target;
        if (!(form instanceof HTMLFormElement) || !form.dataset.confirmation) return;
        if (form.dataset.confirme === 'oui') {
            delete form.dataset.confirme;
            return; // deja confirme : on laisse partir
        }
        e.preventDefault();
        formulaireEnAttente = form;

        const style = form.dataset.confirmationStyle || 'primary';
        titre.textContent = form.dataset.confirmationTitre || 'Confirmation';
        message.textContent = form.dataset.confirmation;
        icone.className = 'bi fs-4 ' + (ICONES[style] || ICONES.primary);
        bouton.textContent = form.dataset.confirmationBouton || 'Confirmer';
        bouton.className = 'btn btn-' + style;
        modal.show();
    });

    bouton.addEventListener('click', () => {
        if (!formulaireEnAttente) return;
        const form = formulaireEnAttente;
        formulaireEnAttente = null;
        bouton.disabled = true; // evite le double envoi
        form.dataset.confirme = 'oui';
        modal.hide();
        form.requestSubmit ? form.requestSubmit() : form.submit();
    });

    fenetre.addEventListener('hidden.bs.modal', () => {
        formulaireEnAttente = null;
        bouton.disabled = false;
    });

    // Focus sur le bouton de confirmation a l'ouverture (validation au clavier avec Entree)
    fenetre.addEventListener('shown.bs.modal', () => bouton.focus());
})();
