package id.stasiuncuaca;

/**
 * Estimasi PAR energi dan PPFD dari radiasi shortwave global (GHI).
 *
 * PAR secara fisiologis merujuk pada radiasi 400–700 nm. PPFD adalah
 * kerapatan fluks foton pada rentang 400–700 nm dalam µmol m⁻² s⁻¹.
 * Karena input Open-Meteo adalah broadband shortwave, konversi berikut
 * merupakan estimasi berbasis koefisien rata-rata, bukan pengukuran
 * langsung dengan quantum sensor/spectroradiometer.
 */
public final class LightConversion {
    private LightConversion() {}

    /** Fraksi nominal broadband shortwave yang dipakai sebagai estimasi PAR energi. */
    public static final double DEFAULT_PAR_FRACTION = 0.45;

    /** Faktor pendekatan cahaya matahari: µmol/J PAR, dipakai untuk estimasi PPFD. */
    public static final double DEFAULT_PAR_EFFICACY_UMOL_PER_J = 4.57;

    public static double shortwaveToParWm2(double shortwaveWm2) {
        if (!Double.isFinite(shortwaveWm2) || shortwaveWm2 < 0) return Double.NaN;
        return shortwaveWm2 * DEFAULT_PAR_FRACTION;
    }

    public static double parToPpfd(double parWm2) {
        if (!Double.isFinite(parWm2) || parWm2 < 0) return Double.NaN;
        return parWm2 * DEFAULT_PAR_EFFICACY_UMOL_PER_J;
    }

    public static double shortwaveToPpfd(double shortwaveWm2) {
        return parToPpfd(shortwaveToParWm2(shortwaveWm2));
    }

    public static String methodologyNote() {
        return "PAR = 400–700 nm; PPFD = µmol foton m⁻² s⁻¹ pada 400–700 nm. " +
                "Dari shortwave broadband digunakan estimasi PAR = " + DEFAULT_PAR_FRACTION +
                " × shortwave dan PPFD = PAR × " + DEFAULT_PAR_EFFICACY_UMOL_PER_J +
                " µmol/J. Nilai ini indikatif dan sebaiknya dikalibrasi/diganti dengan sensor quantum bila tersedia.";
    }
}
