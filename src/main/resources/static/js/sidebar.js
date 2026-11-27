/*
 * Sidebar retractable (grand ecran uniquement).
 * Le bouton #bascule-sidebar ajoute/retire la classe "sidebar-reduite" sur <html> ;
 * l'etat est memorise dans le navigateur (localStorage) et reapplique des le <head> du layout.
 */
(function () {
    var CLE = 'tt-sidebar-reduite';
    var racine = document.documentElement;
    var bouton = document.getElementById('bascule-sidebar');

    // infobulles : en mode reduit, le libelle du lien s'affiche au survol
    document.querySelectorAll('#sidebar .nav-link').forEach(function (lien) {
        if (!lien.title) {
            var libelle = Array.prototype.filter.call(lien.childNodes, function (n) { return n.nodeType === 3; })
                .map(function (n) { return n.textContent.trim(); }).join(' ').trim();
            if (libelle) lien.setAttribute('data-libelle', libelle);
        }
    });

    function appliquer(reduite) {
        racine.classList.toggle('sidebar-reduite', reduite);
        document.querySelectorAll('#sidebar .nav-link[data-libelle]').forEach(function (lien) {
            if (reduite) lien.title = lien.getAttribute('data-libelle');
            else lien.removeAttribute('title');
        });
        if (bouton) {
            var texte = reduite ? 'Agrandir le menu' : 'Réduire le menu';
            bouton.title = texte;
            bouton.setAttribute('aria-label', texte);
            bouton.setAttribute('aria-expanded', String(!reduite));
        }
    }

    appliquer(racine.classList.contains('sidebar-reduite'));

    // active les transitions apres le premier rendu (pas d'animation au chargement de page)
    requestAnimationFrame(function () { racine.classList.add('sidebar-anime'); });

    if (bouton) {
        bouton.addEventListener('click', function () {
            var reduite = !racine.classList.contains('sidebar-reduite');
            appliquer(reduite);
            try { localStorage.setItem(CLE, reduite ? '1' : '0'); } catch (e) {}
        });
    }
})();
