/*
 * Loaders sur tous les boutons.
 *
 *  - Formulaire envoye  -> le bouton d'envoi est desactive et affiche une roue de chargement
 *                          (empeche aussi le double clic = double enregistrement).
 *  - Lien en forme de bouton (<a class="btn">) -> meme roue pendant le changement de page.
 *  - Toute navigation   -> fine barre de progression en haut de l'ecran.
 *
 * Exclusions : boutons qui ouvrent une fenetre / un menu (data-bs-toggle), liens vers un
 * nouvel onglet (target=_blank, ex. facture PDF), ancres (#), et tout element marque
 * data-sans-loader.
 *
 * S'appuie sur la fenetre de confirmation : confirmation.js annule le premier envoi
 * (defaultPrevented) puis relance le formulaire apres confirmation ; le loader ne
 * demarre que sur l'envoi reel.
 */
(function () {
    const barre = document.getElementById('barre-chargement');
    const ROUE = '<span class="spinner-border" role="status" aria-hidden="true"></span>';

    function demarrerBarre() {
        if (barre) {
            barre.classList.remove('actif');
            void barre.offsetWidth; // relance l'animation
            barre.classList.add('actif');
        }
    }

    function activer(bouton) {
        if (!bouton || bouton.classList.contains('en-cours')) return;
        bouton.dataset.contenuInitial = bouton.innerHTML;
        // largeur figee : le bouton ne « saute » pas quand son contenu change
        bouton.style.minWidth = bouton.offsetWidth + 'px';
        const icone = bouton.querySelector('i.bi');
        if (icone) {
            icone.outerHTML = ROUE;          // la roue remplace l'icone, le libelle reste
        } else if (bouton.textContent.trim()) {
            bouton.insertAdjacentHTML('afterbegin', ROUE);
        } else {
            bouton.innerHTML = ROUE;
        }
        bouton.classList.add('en-cours');
        bouton.setAttribute('aria-busy', 'true');
        if (bouton.tagName === 'BUTTON') {
            // desactive APRES l'envoi (un bouton desactive pendant l'envoi n'envoie pas son name/value)
            setTimeout(() => { bouton.disabled = true; }, 0);
        }
    }

    function reinitialiser() {
        document.querySelectorAll('.btn.en-cours').forEach((b) => {
            if (b.dataset.contenuInitial !== undefined) b.innerHTML = b.dataset.contenuInitial;
            b.classList.remove('en-cours');
            b.removeAttribute('aria-busy');
            b.style.minWidth = '';
            if (b.tagName === 'BUTTON') b.disabled = false;
        });
        if (barre) barre.classList.remove('actif');
    }

    // ----- Formulaires -----
    document.addEventListener('submit', (e) => {
        const form = e.target;
        if (e.defaultPrevented || !(form instanceof HTMLFormElement) || form.hasAttribute('data-sans-loader')) return;
        if (form.target === '_blank') return;
        const bouton = e.submitter && e.submitter.classList.contains('btn') ? e.submitter
            : form.querySelector('button[type="submit"].btn, button:not([type]).btn');
        // les fenetres modales se referment proprement avant le changement de page
        activer(bouton);
        demarrerBarre();
    });

    // ----- Liens en forme de bouton -----
    document.addEventListener('click', (e) => {
        if (e.defaultPrevented || e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
        const lien = e.target.closest('a[href]');
        if (!lien) return;
        const href = lien.getAttribute('href');
        if (!href || href.startsWith('#') || href.startsWith('javascript:') || lien.target === '_blank'
            || lien.hasAttribute('download') || lien.hasAttribute('data-bs-toggle') || lien.hasAttribute('data-sans-loader')) {
            return;
        }
        if (lien.classList.contains('btn')) activer(lien);
        demarrerBarre();
    });

    // Retour arriere du navigateur (page restauree depuis le cache) : on remet les boutons a zero
    window.addEventListener('pageshow', (e) => { if (e.persisted) reinitialiser(); });
})();
