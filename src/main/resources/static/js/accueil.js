/*
 * Graphiques de l'accueil (Chart.js).
 * Regles appliquees : une seule echelle par graphique, barres fines a bouts arrondis (4px),
 * grille discrete, legende + infobulle au survol, couleurs de la palette validee
 * (series 1 bleu / 2 orange, un pas par theme : voir --tt-serie-* dans app.css),
 * texte jamais dans la couleur des series. Couleurs lues dans le theme (clair / sombre) ;
 * les graphiques sont redessines quand le theme change (evenement « tt:theme » de theme.js).
 */
const graphiquesAccueil = [];
let modeCourbe = 'periode'; // conserve au changement de theme

(function () {
    const d = window.donneesAccueil;
    if (!d || typeof Chart === 'undefined') return;
    // Attendre la police Inter : sinon Chart.js mesure les libelles avec la police de secours
    // et les coupe une fois Inter affichee (plus large).
    (document.fonts ? document.fonts.ready : Promise.resolve()).then(() => dessiner(d));
    document.addEventListener('tt:theme', () => {
        graphiquesAccueil.splice(0).forEach((g) => g.destroy());
        dessiner(d);
    });
})();

/** Couleurs du theme affiche, lues dans les variables CSS. */
function couleursTheme() {
    const s = getComputedStyle(document.documentElement);
    const v = (nom) => s.getPropertyValue(nom).trim();
    const sombre = document.documentElement.getAttribute('data-bs-theme') === 'dark';
    return {
        serie1: v('--tt-serie-1'), serie2: v('--tt-serie-2'),
        grille: v('--tt-bordure'), axe: v('--tt-bordure-forte'),
        texte: v('--tt-texte-2'), muet: v('--tt-texte-3'), surface: v('--tt-surface'),
        infobulle: sombre ? '#e6ebf3' : '#0f172a', infobulleTexte: sombre ? '#0f172a' : '#ffffff'
    };
}

function dessiner(d) {

    const COULEURS = couleursTheme();
    const fmt = new Intl.NumberFormat('fr-FR', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    const fmtAxe = new Intl.NumberFormat('fr-FR', {maximumFractionDigits: 0}); // graduations des axes
    const compact = (v) => {
        const a = Math.abs(v);
        if (a >= 1e9) return (v / 1e9).toLocaleString('fr-FR', {maximumFractionDigits: 1}) + ' Md';
        if (a >= 1e6) return (v / 1e6).toLocaleString('fr-FR', {maximumFractionDigits: 1}) + ' M';
        if (a >= 1e3) return (v / 1e3).toLocaleString('fr-FR', {maximumFractionDigits: 0}) + ' k';
        return fmtAxe.format(v);
    };
    const nombre = (v) => Number(v) || 0;

    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    Chart.defaults.font.size = 12;
    Chart.defaults.color = COULEURS.muet;
    Chart.defaults.plugins.legend.display = false; // legende HTML au-dessus du graphique
    Chart.defaults.plugins.tooltip.backgroundColor = COULEURS.infobulle;
    Chart.defaults.plugins.tooltip.titleColor = COULEURS.infobulleTexte;
    Chart.defaults.plugins.tooltip.bodyColor = COULEURS.infobulleTexte;
    Chart.defaults.plugins.tooltip.padding = 10;
    Chart.defaults.plugins.tooltip.cornerRadius = 8;
    Chart.defaults.plugins.tooltip.titleFont = {weight: '600'};
    Chart.defaults.plugins.tooltip.boxPadding = 4;
    Chart.defaults.maintainAspectRatio = false;
    Chart.defaults.animation.duration = 500;

    const axeValeurs = {
        beginAtZero: true,
        border: {display: false},
        grid: {color: COULEURS.grille, drawTicks: false},
        ticks: {padding: 8, callback: (v) => compact(v)}
    };
    const axeCategories = {grid: {display: false}, border: {color: COULEURS.axe}, ticks: {padding: 6, color: COULEURS.texte}};
    const barre = {borderRadius: 4, borderSkipped: 'start', maxBarThickness: 22};
    const infobulleMga = (ctx) => ` ${ctx.dataset.label} : ${fmt.format(ctx.parsed.y ?? ctx.parsed.x)} MGA`;

    // ----- Courbe d'evolution du CA (periode choisie vs periode precedente) -----
    const courbe = document.getElementById('graphique-courbe-ca');
    if (courbe && d.courbe) {
        const c = d.courbe;
        const brut = c.ca.map(nombre);
        // null = rang sans equivalent dans la periode precedente (plus courte) : pas de trait
        const brutPrec = c.caPrecedent.map((v) => (v === null ? null : nombre(v)));
        const cumul = (serie) => {
            let total = 0;
            return serie.map((v) => (v === null ? null : (total += v)));
        };
        const series = {periode: [brut, brutPrec], cumul: [cumul(brut), cumul(brutPrec)]};
        const peu = brut.length <= 31; // points visibles seulement quand ils restent lisibles
        const graphique = new Chart(courbe, {
            type: 'line',
            data: {
                labels: c.libelles,
                datasets: [
                    {label: c.periode, data: series[modeCourbe][0], borderColor: COULEURS.serie1, backgroundColor: COULEURS.serie1,
                        borderWidth: 2, cubicInterpolationMode: 'monotone', pointRadius: peu ? 3 : 0, pointHoverRadius: 5,
                        pointBorderColor: COULEURS.surface, pointBorderWidth: 2, fill: false},
                    {label: 'Période précédente (' + c.periodePrecedente + ')', data: series[modeCourbe][1],
                        borderColor: COULEURS.muet, backgroundColor: COULEURS.muet, borderDash: [5, 4],
                        borderWidth: 2, cubicInterpolationMode: 'monotone', pointRadius: 0, pointHoverRadius: 4, fill: false}
                ]
            },
            options: {
                interaction: {mode: 'index', intersect: false},
                plugins: {tooltip: {
                    filter: (item) => item.parsed.y !== null,
                    callbacks: {label: (ctx) => ` ${ctx.dataset.label} : ${fmt.format(ctx.parsed.y)} MGA`}
                }},
                scales: {
                    x: {...axeCategories, ticks: {...axeCategories.ticks, autoSkip: true, maxRotation: 0, autoSkipPadding: 12}},
                    y: axeValeurs
                }
            }
        });

        graphiquesAccueil.push(graphique);

        // bascule « par jour / par mois » <-> « cumule » (onclick : remplace le gestionnaire
        // du graphique precedent quand on redessine apres un changement de theme)
        const boutons = document.querySelectorAll('[data-mode-courbe]');
        boutons.forEach((bouton) => {
            bouton.onclick = () => {
                const mode = bouton.dataset.modeCourbe;
                modeCourbe = mode;
                boutons.forEach((b) => {
                    b.classList.toggle('active', b === bouton);
                    b.setAttribute('aria-pressed', String(b === bouton));
                });
                graphique.data.datasets[0].data = series[mode][0];
                graphique.data.datasets[1].data = series[mode][1];
                graphique.update();
            };
        });
    }

    // ----- CA et marge par mois -----
    const mensuel = document.getElementById('graphique-mensuel');
    if (mensuel) {
        graphiquesAccueil.push(new Chart(mensuel, {
            type: 'bar',
            data: {
                labels: d.mois,
                datasets: [
                    {label: d.libelleCa || "Chiffre d'affaires", data: d.ca.map(nombre), backgroundColor: COULEURS.serie1, ...barre},
                    {label: 'Marge nette', data: d.marge.map(nombre), backgroundColor: COULEURS.serie2, ...barre}
                ]
            },
            options: {
                // ecart de 2px entre les barres d'un meme mois
                datasets: {bar: {categoryPercentage: 0.62, barPercentage: 0.92}},
                interaction: {mode: 'index', intersect: false},
                plugins: {tooltip: {callbacks: {label: infobulleMga}}},
                scales: {x: axeCategories, y: axeValeurs}
            }
        }));
    }

    // ----- Barres horizontales classees (une teinte) -----
    function barresHorizontales(canvas, libelles, valeurs, libelleSerie) {
        graphiquesAccueil.push(new Chart(canvas, {
            type: 'bar',
            data: {labels: libelles, datasets: [{label: libelleSerie, data: valeurs, backgroundColor: COULEURS.serie1, ...barre}]},
            options: {
                indexAxis: 'y',
                plugins: {tooltip: {callbacks: {label: (ctx) => ` ${libelleSerie} : ${fmt.format(ctx.parsed.x)} MGA`}}},
                scales: {
                    x: {...axeValeurs, ticks: {...axeValeurs.ticks}},
                    y: {grid: {display: false}, border: {color: COULEURS.axe}, ticks: {color: COULEURS.texte, padding: 6,
                        callback: function (v) { const l = this.getLabelForValue(v); return l.length > 34 ? l.slice(0, 33) + '…' : l; }}}
                }
            }
        }));
    }

    // series deja classees par le serveur (montant / marge decroissants)
    const enc = document.getElementById('graphique-encaissements');
    if (enc) {
        barresHorizontales(enc, d.encaissementsLibelles, d.encaissementsMontants.map(nombre), 'Encaissé');
    }
}
