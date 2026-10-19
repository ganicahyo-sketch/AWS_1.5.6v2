package id.stasiuncuaca;

import java.time.LocalDate;

/**
 * Estimated macro-weather variables for providers that do not expose radiation,
 * sunshine duration or soil moisture. The algorithms are screening estimates:
 * FAO-56 Penman-Monteith ET0, Hargreaves-Samani solar radiation, the default
 * Angstrom-Prescott sunshine relation, and a simple FAO-56-style root-zone
 * water bucket. They must not be presented as direct sensor measurements.
 */
public final class WeatherMacroModel {
    private WeatherMacroModel() {}

    public static final class DailyDerived {
        public final double extraterrestrialRadiationMjM2;
        public final double solarRadiationMjM2;
        public final double daylightHours;
        public final double sunshineHours;
        public final double et0Mm;
        public final double shortwaveDaylightMeanWm2;
        public final double parWm2;
        public final double ppfdUmolM2S;
        public final double luxEstimate;

        DailyDerived(double ra, double rs, double daylight, double sunshine, double et0) {
            extraterrestrialRadiationMjM2 = ra;
            solarRadiationMjM2 = rs;
            daylightHours = daylight;
            sunshineHours = sunshine;
            et0Mm = et0;
            shortwaveDaylightMeanWm2 = daylight > 0 && Double.isFinite(rs) ? rs * 1_000_000.0 / (daylight * 3600.0) : Double.NaN;
            parWm2 = Double.isFinite(shortwaveDaylightMeanWm2) ? LightConversion.shortwaveToParWm2(shortwaveDaylightMeanWm2) : Double.NaN;
            ppfdUmolM2S = Double.isFinite(parWm2) ? LightConversion.parToPpfd(parWm2) : Double.NaN;
            // Approximate broad-spectrum daylight illuminance conversion; not a lux sensor reading.
            luxEstimate = Double.isFinite(shortwaveDaylightMeanWm2) ? shortwaveDaylightMeanWm2 * 120.0 : Double.NaN;
        }
    }

    /** Estimate daily radiation and ET0 from OpenWeather daily minimum/maximum temperature, humidity, pressure and wind. */
    public static DailyDerived estimateDaily(double latitudeDeg, double elevationM, LocalDate date,
            double tMaxC, double tMinC, double relativeHumidityPct, double pressureHpa,
            double windSpeed10mMs, double cloudCoverPct) {
        if (date == null || !Double.isFinite(latitudeDeg) || latitudeDeg < -90 || latitudeDeg > 90
                || !Double.isFinite(tMaxC) || !Double.isFinite(tMinC) || tMaxC < tMinC) {
            return new DailyDerived(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        int day = date.getDayOfYear();
        double ra = extraterrestrialRadiation(latitudeDeg, day);
        double daylight = dayLengthHours(latitudeDeg, day);
        if (!Double.isFinite(ra) || !Double.isFinite(daylight)) {
            return new DailyDerived(ra, Double.NaN, daylight, Double.NaN, Double.NaN);
        }
        // Hargreaves-Samani global radiation estimate; 0.16 is the usual inland default.
        // Temperature range already reflects part of the cloudiness signal; cloudCover is
        // retained as provenance but not used as a second arbitrary attenuation factor.
        double rs = Math.min(ra * 0.82, 0.16 * Math.sqrt(Math.max(0.0, tMaxC - tMinC)) * ra);
        // Invert the default Angstrom-Prescott relation Rs/Ra = 0.25 + 0.50(n/N).
        // This is an estimated sunshine duration, not a measured WMO sunshine recorder value.
        double sunshineFraction = ra <= 0 ? 0 : clamp((rs / ra - 0.25) / 0.50, 0, 1);
        double sunshine = Math.min(daylight, daylight * sunshineFraction);
        double et0 = fao56Et0(latitudeDeg, day, elevationM, tMaxC, tMinC,
                relativeHumidityPct, pressureHpa, windSpeed10mMs, rs);
        return new DailyDerived(ra, rs, daylight, sunshine, et0);
    }

    /**
     * Advance a daily root-zone water-bucket. fcPct and pwpPct are volumetric %,
     * depthCm is effective root-zone depth, and fractionAvailable is 0..1.
     */
    public static double nextAvailableWaterFraction(double fractionAvailable, double precipitationMm,
            double et0Mm, double kc, double fieldCapacityPct, double wiltingPointPct, double rootDepthCm) {
        if (!Double.isFinite(fractionAvailable) || !Double.isFinite(precipitationMm)
                || !Double.isFinite(et0Mm) || !Double.isFinite(fieldCapacityPct)
                || !Double.isFinite(wiltingPointPct) || !Double.isFinite(rootDepthCm)
                || fieldCapacityPct <= wiltingPointPct || rootDepthCm <= 0) return Double.NaN;
        double tawMm = (fieldCapacityPct - wiltingPointPct) / 100.0 * rootDepthCm * 10.0;
        if (!(tawMm > 0)) return Double.NaN;
        double effectiveRain = Math.max(0.0, precipitationMm) * 0.8;
        double cropUse = Math.max(0.0, et0Mm) * (Double.isFinite(kc) && kc > 0 ? kc : 1.0);
        double store = clamp(fractionAvailable, 0, 1) * tawMm;
        return clamp((store + effectiveRain - cropUse) / tawMm, 0, 1);
    }

    public static double moisturePctVol(double availableFraction, double fcPct, double pwpPct) {
        if (!Double.isFinite(availableFraction) || !Double.isFinite(fcPct) || !Double.isFinite(pwpPct) || fcPct <= pwpPct) return Double.NaN;
        return pwpPct + clamp(availableFraction, 0, 1) * (fcPct - pwpPct);
    }

    /** Crop-coefficient screening defaults for the water-balance estimate only. */
    public static double cropCoefficient(String phase) {
        if (phase == null) return 0.85;
        String p = phase.toLowerCase(java.util.Locale.ROOT);
        if (p.contains("awal") || p.contains("establishment") || p.contains("perkecambahan")) return 0.60;
        if (p.contains("generatif") || p.contains("pembungaan") || p.contains("buah") || p.contains("pembesaran")) return 1.00;
        if (p.contains("pematangan") || p.contains("panen")) return 0.75;
        return 0.85;
    }

    public static double fao56Et0(double latitudeDeg, int dayOfYear, double elevationM,
            double tMaxC, double tMinC, double rhMeanPct, double pressureHpa,
            double wind10mMs, double rsMjM2Day) {
        if (!Double.isFinite(latitudeDeg) || !Double.isFinite(tMaxC) || !Double.isFinite(tMinC)
                || !Double.isFinite(rhMeanPct) || !Double.isFinite(wind10mMs)
                || !Double.isFinite(rsMjM2Day) || tMaxC < tMinC) return Double.NaN;
        double t = (tMaxC + tMinC) / 2.0;
        double ea = clamp(rhMeanPct, 0, 100) / 100.0 * saturationVapourPressure(t);
        double es = (saturationVapourPressure(tMaxC) + saturationVapourPressure(tMinC)) / 2.0;
        double delta = 4098.0 * saturationVapourPressure(t) / Math.pow(t + 237.3, 2.0);
        double pKpa = Double.isFinite(pressureHpa) && pressureHpa > 0
                ? pressureHpa / 10.0 : 101.3 * Math.pow((293.0 - 0.0065 * Math.max(0.0, finiteOr(elevationM, 0))) / 293.0, 5.26);
        double gamma = 0.000665 * pKpa;
        double ra = extraterrestrialRadiation(latitudeDeg, dayOfYear);
        double z = Math.max(0.0, finiteOr(elevationM, 0.0));
        double rso = (0.75 + 2.0e-5 * z) * ra;
        double rsRso = rso > 0 ? clamp(rsMjM2Day / rso, 0.05, 1.0) : 0.05;
        double rns = (1.0 - 0.23) * rsMjM2Day;
        double tMaxK = tMaxC + 273.16, tMinK = tMinC + 273.16;
        double cloudTerm = Math.max(0.0, 1.35 * rsRso - 0.35);
        double rnl = 4.903e-9 * ((Math.pow(tMaxK, 4) + Math.pow(tMinK, 4)) / 2.0)
                * (0.34 - 0.14 * Math.sqrt(Math.max(0.0, ea))) * cloudTerm;
        double rn = rns - rnl;
        double u2 = Math.max(0.0, wind10mMs) * 4.87 / Math.log(67.8 * 10.0 - 5.42);
        double numerator = 0.408 * delta * rn + gamma * (900.0 / (t + 273.0)) * u2 * Math.max(0.0, es - ea);
        double denominator = delta + gamma * (1.0 + 0.34 * u2);
        if (!(denominator > 0)) return Double.NaN;
        return clamp(numerator / denominator, 0.0, 20.0);
    }

    public static double extraterrestrialRadiation(double latitudeDeg, int dayOfYear) {
        double lat = Math.toRadians(clamp(latitudeDeg, -89.9, 89.9));
        double dr = 1.0 + 0.033 * Math.cos(2.0 * Math.PI / 365.0 * dayOfYear);
        double solarDeclination = 0.409 * Math.sin(2.0 * Math.PI / 365.0 * dayOfYear - 1.39);
        double x = -Math.tan(lat) * Math.tan(solarDeclination);
        double sunsetAngle = Math.acos(clamp(x, -1.0, 1.0));
        return (24.0 * 60.0 / Math.PI) * 0.0820 * dr * (sunsetAngle * Math.sin(lat) * Math.sin(solarDeclination)
                + Math.cos(lat) * Math.cos(solarDeclination) * Math.sin(sunsetAngle));
    }

    public static double dayLengthHours(double latitudeDeg, int dayOfYear) {
        double lat = Math.toRadians(clamp(latitudeDeg, -89.9, 89.9));
        double dec = 0.409 * Math.sin(2.0 * Math.PI / 365.0 * dayOfYear - 1.39);
        double x = -Math.tan(lat) * Math.tan(dec);
        double ws = Math.acos(clamp(x, -1, 1));
        return 24.0 / Math.PI * ws;
    }

    public static double saturationVapourPressure(double temperatureC) {
        if (!Double.isFinite(temperatureC)) return Double.NaN;
        return 0.6108 * Math.exp(17.27 * temperatureC / (temperatureC + 237.3));
    }

    private static double finiteOr(double value, double fallback) { return Double.isFinite(value) ? value : fallback; }
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
