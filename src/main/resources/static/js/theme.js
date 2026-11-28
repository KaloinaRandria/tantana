/*
 * Mode clair / mode sombre.
 * Le theme est pose sur <html data-bs-theme="light|dark"> des le <head> (script en ligne du layout,
 * pas de clignotement). Sans choix memorise, on suit le reglage du systeme (et ses changements) ;
 * un clic sur .bascule-theme memorise le choix dans le navigateur (localStorage).
 * Evenement « tt:theme » emis sur document a chaque changement (graphiques a redessiner).
 */
(function () {
    var CLE = 'tt-theme';
    var racine = document.documentElement;
    var systeme = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

    function choixMemorise() {
        try {
            var t = localStorage.getItem(CLE);
            return t === 'clair' || t === 'sombre' ? t : null;
        } catch (e) {
            return null;
        }
    }

    function estSombre() {
        return racine.getAttribute('data-bs-theme') === 'dark';
    }

    function appliquer(sombre) {
        if (sombre === estSombre()) return;
        racine.setAttribute('data-bs-theme', sombre ? 'dark' : 'light');
        majBoutons();
        document.dispatchEvent(new CustomEvent('tt:theme', {detail: {sombre: sombre}}));
    }

    function majBoutons() {
        var texte = estSombre() ? 'Passer en mode clair' : 'Passer en mode sombre';
        document.querySelectorAll('.bascule-theme').forEach(function (b) {
            b.title = texte;
            b.setAttribute('aria-label', texte);
            b.setAttribute('aria-pressed', String(estSombre()));
        });
    }

    document.querySelectorAll('.bascule-theme').forEach(function (b) {
        b.addEventListener('click', function () {
            var sombre = !estSombre();
            try { localStorage.setItem(CLE, sombre ? 'sombre' : 'clair'); } catch (e) { /* navigation privee */ }
            appliquer(sombre);
        });
    });

    // reglage du systeme modifie (ex. passage automatique en sombre le soir) : suivi tant qu'aucun choix
    if (systeme && systeme.addEventListener) {
        systeme.addEventListener('change', function (e) {
            if (!choixMemorise()) appliquer(e.matches);
        });
    }

    majBoutons();
})();
