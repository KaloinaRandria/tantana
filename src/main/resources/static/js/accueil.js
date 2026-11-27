/*
 * Graphiques de l'accueil (Chart.js).
 * Regles appliquees : une seule echelle par graphique, barres fines a bouts arrondis (4px),
 * grille discrete, legende + infobulle au survol, couleurs de la palette validee
 * (serie 1 bleu #2a78d6, serie 2 orange #eb6834), texte jamais dans la couleur des series.
 */
(function () {
    const d = window.donneesAccueil;
    if (!d || typeof Chart === 'undefined') return;
    // Attendre la police Inter : sinon Chart.js mesure les libelles avec la police de secours
    // et les coupe une fois Inter affichee (plus large).
    (document.fonts ? document.fonts.ready : Promise.resolve()).then(() => dessiner(d));
})();

function dessiner(d) {

    const COULEURS = {serie1: '#2a78d6', serie2: '#eb6834', grille: '#e8ebf0', axe: '#d3d8e0', texte: '#475569', muet: '#8590a2'};
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
    Chart.defaults.plugins.tooltip.backgroundColor = '#0f172a';
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

    // ----- CA et marge par mois -----
    const mensuel = document.getElementById('graphique-mensuel');
    if (mensuel) {
        new Chart(mensuel, {
            type: 'bar',
            data: {
                labels: d.mois,
                datasets: [
                    {label: "Chiffre d'affaires HT", data: d.ca.map(nombre), backgroundColor: COULEURS.serie1, ...barre},
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
        });
    }

    // ----- Barres horizontales classees (une teinte) -----
    function barresHorizontales(canvas, libelles, valeurs, libelleSerie) {
        new Chart(canvas, {
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
        });
    }

    // series deja classees par le serveur (montant / marge decroissants)
    const enc = document.getElementById('graphique-encaissements');
    if (enc) {
        barresHorizontales(enc, d.encaissementsLibelles, d.encaissementsMontants.map(nombre), 'Encaissé');
    }

    const art = document.getElementById('graphique-articles');
    if (art) {
        barresHorizontales(art, d.articlesLibelles, d.articlesMarges.map(nombre), 'Marge nette');
    }
}
