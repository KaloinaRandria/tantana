/*
 * Formulaire de reception : ajout / suppression des frais (renumerotation frais[i] pour Spring)
 * et total des frais en direct.
 */
(function () {
    const form = document.getElementById('form-reception');
    if (!form) return;

    const corps = form.querySelector('#table-frais tbody');
    const modele = document.getElementById('modele-frais');
    const format = new Intl.NumberFormat('fr-FR', {maximumFractionDigits: 2});

    function renumeroter() {
        corps.querySelectorAll('tr.frais').forEach((tr, i) => {
            tr.querySelectorAll('select, input').forEach((champ) => {
                const nom = champ.dataset.champ || (champ.name.match(/\.(\w+)$/) || [])[1];
                if (!nom) return;
                champ.dataset.champ = nom;
                champ.name = `frais[${i}].${nom}`;
                champ.id = `frais${i}.${nom}`;
            });
        });
    }

    function total() {
        let somme = 0;
        corps.querySelectorAll('.champ-montant').forEach((c) => {
            const v = parseFloat(c.value);
            if (!isNaN(v)) somme += v;
        });
        document.getElementById('total-frais').textContent = format.format(somme);
    }

    document.getElementById('ajouter-frais').addEventListener('click', () => {
        corps.appendChild(modele.content.cloneNode(true));
        renumeroter();
        corps.querySelector('tr.frais:last-child select').focus();
    });

    corps.addEventListener('click', (e) => {
        const bouton = e.target.closest('.supprimer-frais');
        if (!bouton) return;
        bouton.closest('tr').remove();
        renumeroter();
        total();
    });

    corps.addEventListener('input', total);
    renumeroter();
    total();
})();
