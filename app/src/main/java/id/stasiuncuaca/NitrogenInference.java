package id.stasiuncuaca;

import java.util.Locale;

/**
 * Estimasi N-total tanah dari pembacaan N tersedia (mg/kg) untuk screening.
 * Asumsi utama: N tersedia = 2% dari N-total; rentang sensitivitas 1–3%.
 * Hasil bukan pengganti pengujian N-total laboratorium (mis. Kjeldahl).
 */
public final class NitrogenInference {
    private NitrogenInference() {}

    /** Default fraction of total soil N represented by the available-N reading. */
    public static final double DEFAULT_AVAILABLE_FRACTION = 0.02;
    public static final double MIN_AVAILABLE_FRACTION = 0.01;
    public static final double MAX_AVAILABLE_FRACTION = 0.03;

    /** Basis selector labels. Generic N/P/K extraction method names do not validate N cutoffs. */
    public static final String BASIS_INFERRED = "Estimasi N-total dari N tersedia (asumsi 2%, default)";
    public static final String BASIS_METHOD_THRESHOLDS = "Ambang N tersedia khusus metode/lab (isi kedua batas)";

    public static boolean usesMethodSpecificThresholds(String selectedBasis) {
        return BASIS_METHOD_THRESHOLDS.equals(selectedBasis == null ? "" : selectedBasis.trim());
    }

    private static boolean validAvailableN(double availableMgKg) {
        return Double.isFinite(availableMgKg) && availableMgKg >= 0.0;
    }

    /**
     * Convert N-available reading to estimated total N concentration (mg/kg).
     * Central assumption: available N is 2% of total N, so N-total = N-available × (100/2).
     */
    public static double estimateTotalNmgKg(double availableMgKg) {
        if (!validAvailableN(availableMgKg)) return Double.NaN;
        return (100.0 / 2.0) * availableMgKg;
    }

    /**
     * Estimated total soil N in percent by mass using the requested explicit conversion:
     * N-total (%) = ((100 / 2) × N-available (mg/kg)) / 10,000.
     * 10,000 mg/kg corresponds to 1% by mass.
     */
    public static double estimateTotalNPercent(double availableMgKg) {
        if (!validAvailableN(availableMgKg)) return Double.NaN;
        return availableMgKg / 200.0;
    }

    /** Lower end of the estimated total-N percentage, using 3% available/total ratio. */
    public static double estimateTotalNLowPercent(double availableMgKg) {
        if (!validAvailableN(availableMgKg)) return Double.NaN;
        return availableMgKg / (MAX_AVAILABLE_FRACTION * 10000.0);
    }

    /** Upper end of the estimated total-N percentage, using 1% available/total ratio. */
    public static double estimateTotalNHighPercent(double availableMgKg) {
        if (!validAvailableN(availableMgKg)) return Double.NaN;
        return availableMgKg / (MIN_AVAILABLE_FRACTION * 10000.0);
    }

    /** Five commonly used Indonesian soil total-N screening classes (% N-total). */
    public static String classifyTotalNPercent(double totalNPercent) {
        if (!Double.isFinite(totalNPercent) || totalNPercent < 0.0) return "Tidak ada data";
        if (totalNPercent < 0.10) return "Sangat Rendah";
        if (totalNPercent <= 0.20) return "Rendah";
        if (totalNPercent <= 0.50) return "Sedang";
        if (totalNPercent <= 0.75) return "Tinggi";
        return "Sangat Tinggi";
    }

    /**
     * One-step decision used throughout agronomy UI/report/need calculations:
     * explicit method-specific cutoffs win only when both are meaningful; otherwise
     * infer N-total from measured N-available using the 2% assumption and classify that.
     * Missing cutoffs are not interpreted as a measured soil N value of zero.
     */
    public static String classifyAvailableNAsTotalN(double availableMgKg) {
        if (!validAvailableN(availableMgKg)) return "Tidak ada data";
        return classifyTotalNPercent(estimateTotalNPercent(availableMgKg));
    }

    /** Primary inferred class uses the requested 2% assumption, not an empty threshold as zero. */
    public static String classifyEstimatedTotalN(double availableMgKg) {
        return classifyAvailableNAsTotalN(availableMgKg);
    }

    /**
     * Human-readable estimate. The central estimate uses 2%; 1–3% is only a sensitivity range.
     */
    public static String estimateTotalN(double availableMgKg) {
        if (!validAvailableN(availableMgKg)) return "Belum dapat diprediksi karena N tersedia belum valid.";
        double centerMgKg = estimateTotalNmgKg(availableMgKg);
        double centerPct = estimateTotalNPercent(availableMgKg);
        double lowMgKg = availableMgKg / MAX_AVAILABLE_FRACTION;
        double highMgKg = availableMgKg / MIN_AVAILABLE_FRACTION;
        double lowPct = estimateTotalNLowPercent(availableMgKg);
        double highPct = estimateTotalNHighPercent(availableMgKg);
        return String.format(Locale.US,
                "≈ %.0f mg/kg (%.3f%% N-total; asumsi utama N tersedia = 2%% N-total). Rentang sensitivitas asumsi 1–3%%: %.0f–%.0f mg/kg (%.3f–%.3f%%).",
                centerMgKg, centerPct, lowMgKg, highMgKg, lowPct, highPct);
    }

    /** Possible class interval under the 1–3% sensitivity range; point class remains the 2% estimate. */
    public static String classifyRange(double availableMgKg) {
        if (!validAvailableN(availableMgKg)) return "Belum dapat diprediksi";
        String lower = classifyTotalNPercent(estimateTotalNLowPercent(availableMgKg));
        String upper = classifyTotalNPercent(estimateTotalNHighPercent(availableMgKg));
        if (lower.equals(upper)) return lower;
        return lower + "–" + upper;
    }
}
