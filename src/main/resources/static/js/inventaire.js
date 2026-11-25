/*
 * Inventaires : choix des articles (ouverture), saisie du comptage, filtre des ecarts.
 */
(function () {
    // ----- Filtre texte sur les lignes d'un tableau (data-texte) -----
    function brancherFiltre(champId, selecteurLignes) {
        const champ = document.getElementById(champId);
        if (!champ) return;
        champ.addEventListener('input', () => {
            const t = champ.value.trim().toLowerCase();
            document.querySelectorAll(selecteurLignes).forEach(tr => {
                tr.hidden = t !== '' && !(tr.dataset.texte || '').includes(t);
            });
        });
    }

    // ===================== Ouverture =====================
    const formInventaire = document.getElementById('form-inventaire');
    if (formInventaire) {
        const blocComplet = document.getElementById('bloc-complet');
        const blocTournant = document.getElementById('bloc-tournant');
        const compteur = document.getElementById('nb-coches');
        const cases = () => [...blocTournant.querySelectorAll('input[name="articleIds"]:not(:disabled)')];

        const majType = () => {
            const type = formInventaire.querySelector('input[name="type"]:checked')?.value;
            blocComplet.hidden = type !== 'COMPLET';
            blocTournant.hidden = type === 'COMPLET';
        };
        const majCompteur = () => { compteur.textContent = cases().filter(c => c.checked).length; };

        formInventaire.querySelectorAll('input[name="type"]').forEach(r => r.addEventListener('change', majType));
        blocTournant.addEventListener('change', majCompteur);
        blocTournant.querySelectorAll('[data-cocher-classe]').forEach(bouton => {
            bouton.addEventListener('click', () => {
                const classe = bouton.dataset.cocherClasse;
                cases().forEach(c => {
                    c.checked = classe !== '' && c.closest('tr').dataset.classe === classe;
                });
                majCompteur();
            });
        });
        // Inventaire complet : les cases cochees ne doivent pas partir
        formInventaire.addEventListener('submit', () => {
            if (blocTournant.hidden) cases().forEach(c => { c.checked = false; });
        }, true);

        brancherFiltre('filtre-articles', '#bloc-tournant tbody tr');
        majType();
        majCompteur();
    }

    // ===================== Comptage =====================
    const formComptage = document.getElementById('form-comptage');
    if (formComptage) {
        const soumettre = document.getElementById('champ-soumettre');
        const MESSAGE = 'Le comptage sera clos : les écarts avec le stock du système apparaîtront et devront être '
            + 'justifiés, puis validés par une autre personne. Vous ne pourrez plus modifier les quantités, '
            + 'sauf si le contrôleur demande un recomptage.';

        formComptage.querySelectorAll('button[data-action]').forEach(bouton => {
            bouton.addEventListener('click', () => {
                if (bouton.dataset.action === 'soumettre') {
                    const vides = [...formComptage.querySelectorAll('.champ-compte')].filter(c => c.value.trim() === '');
                    soumettre.value = 'true';
                    formComptage.dataset.confirmationTitre = 'Terminer le comptage';
                    formComptage.dataset.confirmationBouton = 'Terminer le comptage';
                    formComptage.dataset.confirmationStyle = vides.length ? 'danger' : 'primary';
                    formComptage.dataset.confirmation = vides.length
                        ? vides.length + ' article(s) ne sont pas encore comptés : saisissez 0 pour un article absent.'
                        : MESSAGE;
                } else {
                    soumettre.value = 'false';
                    delete formComptage.dataset.confirmation;
                }
            });
        });

        // Ligne comptee : fond leger pour suivre l'avancement
        const marquer = champ => champ.closest('tr').classList.toggle('table-success', champ.value.trim() !== '');
        formComptage.querySelectorAll('.champ-compte').forEach(champ => {
            marquer(champ);
            champ.addEventListener('input', () => marquer(champ));
        });
        brancherFiltre('filtre-lignes', '#table-lignes tbody tr');
    }

    // ===================== Resultat : ecarts seulement =====================
    const interrupteur = document.getElementById('ecarts-seulement');
    if (interrupteur) {
        const appliquer = () => document.querySelectorAll('#table-ecarts tbody tr').forEach(tr => {
            tr.hidden = interrupteur.checked && tr.dataset.ecart !== 'true';
        });
        interrupteur.addEventListener('change', appliquer);
        appliquer();
    }
})();
