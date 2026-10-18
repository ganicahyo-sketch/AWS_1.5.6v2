package id.stasiuncuaca;

import java.util.Locale;

/** Central crop catalog used by Settings and Field Notes. Manual entry remains supported. */
public final class CropDatabase {
    private CropDatabase() {}

    public static final String MANUAL = "Lainnya / isi manual";

    /**
     * All general-crop profiles currently supported by AgronomyEngine.
     * Kelapa sawit is intentionally excluded from STASIUN CUACA v1.5.7.
     */
    public static final String[] CROPS = {
        "Padi", "Jagung", "Kedelai", "Cabai", "Tomat", "Singkong", "Ubi jalar",
        "Kentang", "Bawang merah", "Kacang tanah", "Kacang hijau", "Kacang panjang",
        "Kubis", "Pakcoy", "Sawi", "Selada", "Brokoli", "Kembang kol", "Kangkung",
        "Bayam", "Wortel", "Mentimun", "Terong", "Melon", "Semangka", "Kale / kailan",
        MANUAL
    };

    public static boolean isManual(String crop) {
        return crop != null && MANUAL.equalsIgnoreCase(crop.trim());
    }

    public static boolean isKnown(String crop) {
        if (crop == null) return false;
        String n = normalize(crop);
        for (String item : CROPS) {
            if (MANUAL.equals(item)) continue;
            if (normalize(item).equals(n)) return true;
        }
        return false;
    }

    public static String normalize(String crop) {
        if (crop == null) return "";
        return crop.toLowerCase(Locale.US).replace((char)160, ' ').replaceAll("\s+", " ").trim();
    }

    /**
     * Tbase defaults for the GDD screening model (deg C).
     * Values are literature/model reference values, not cultivar-specific constants.
     */
    public static double tBase(String crop) {
        String c = AgronomyEngine.normalizeCrop(crop).toLowerCase(Locale.US);
        if (c.contains("padi") || c.contains("rice")) return 10.0;
        if (c.contains("jagung") || c.contains("maize") || c.contains("corn")) return 10.0;
        if (c.contains("kedelai") || c.contains("soy")) return 10.0;
        if (c.contains("cabai") || c.contains("chili") || c.contains("pepper")) return 10.0;
        if (c.contains("tomat") || c.contains("tomato")) return 7.0;
        if (c.contains("singkong") || c.contains("cassava")) return 10.0;
        if (c.contains("ubi jalar") || c.contains("sweet potato")) return 12.0;
        if (c.contains("kentang") || c.contains("potato")) return 2.0;
        if (c.contains("bawang") || c.contains("shallot") || c.contains("onion")) return 4.5;
        if (c.contains("kacang tanah") || c.contains("groundnut")) return 10.0;
        if (c.contains("kacang hijau") || c.contains("mungbean")) return 8.0;
        if (c.contains("kacang panjang") || c.contains("long bean")) return 10.0;
        if (c.contains("kubis") || c.equals("kol") || c.contains("cabbage")) return 4.5;
        if (c.contains("pakcoy") || c.contains("pokcoy") || c.contains("pak choi") || c.contains("bok choy")) return 4.5;
        if (c.contains("sawi") || c.contains("mustard green") || c.contains("caisim") || c.contains("caisin") || c.contains("choy sum")) return 5.0;
        if (c.contains("selada") || c.contains("lettuce")) return 4.0;
        if (c.contains("brokoli") || c.contains("broccoli")) return 4.5;
        if (c.contains("kembang kol") || c.contains("cauliflower")) return 4.5;
        if (c.contains("kangkung") || c.contains("water spinach")) return 10.0; // warm-season proxy; no robust crop-specific value found
        if (c.contains("bayam") || c.contains("amaranth") || c.contains("spinach")) return 7.0; // amaranth proxy; spinach itself is ~4 C
        if (c.contains("wortel") || c.contains("carrot")) return 6.0;
        if (c.contains("timun") || c.contains("mentimun") || c.contains("cucumber")) return 10.0;
        if (c.contains("terong") || c.contains("eggplant")) return 10.0;
        if (c.contains("melon")) return 10.0;
        if (c.contains("semangka") || c.contains("watermelon")) return 10.0;
        if (c.contains("kale") || c.contains("kailan")) return 4.5; // Brassica/cabbage proxy
        return Double.NaN;
    }

    public static String tBaseBasis(String crop) {
        String c = AgronomyEngine.normalizeCrop(crop).toLowerCase(Locale.US);
        if (c.contains("padi") || c.contains("rice") || c.contains("jagung") || c.contains("maize") || c.contains("corn")
                || c.contains("kedelai") || c.contains("soy") || c.contains("kacang tanah") || c.contains("groundnut")
                || c.contains("kacang hijau") || c.contains("mungbean") || c.contains("kacang panjang") || c.contains("long bean")
                || c.contains("ubi jalar") || c.contains("sweet potato") || c.contains("singkong") || c.contains("cassava")
                || c.contains("wortel") || c.contains("carrot") || c.contains("kentang") || c.contains("potato")
                || c.contains("kubis") || c.equals("kol") || c.contains("cabbage") || c.contains("selada") || c.contains("lettuce")
                || c.contains("brokoli") || c.contains("broccoli") || c.contains("kembang kol") || c.contains("cauliflower")
                || c.contains("mentimun") || c.contains("cucumber") || c.contains("terong") || c.contains("eggplant")
                || c.contains("melon") || c.contains("semangka") || c.contains("watermelon") || c.contains("tomat") || c.contains("tomato"))
            return "FAO56rev / review 2025; nilai indikatif, cek varietas/lokasi.";
        if (c.contains("cabai") || c.contains("chili") || c.contains("pepper")) return "Review threshold 2025; pepper Tbase sekitar 10 °C.";
        if (c.contains("bawang") || c.contains("shallot") || c.contains("onion")) return "FAO56rev onion 4,5 °C; shallot dipakai sebagai proxy.";
        if (c.contains("sawi") || c.contains("mustard green") || c.contains("caisim") || c.contains("caisin")) return "FAO56rev mustard 5 °C; didukung studi mustard.";
        if (c.contains("pakcoy") || c.contains("pokcoy") || c.contains("pak choi") || c.contains("bok choy")) return "Brassica/cabbage proxy 4,5 °C; verifikasi varietas.";
        if (c.contains("kale") || c.contains("kailan")) return "Brassica/cabbage proxy 4,5 °C; tidak ada nilai universal tunggal.";
        if (c.contains("kangkung") || c.contains("water spinach")) return "Proxy warm-season 10 °C; nilai crop-specific belum cukup kuat.";
        if (c.contains("bayam") || c.contains("amaranth")) return "Amaranth proxy 7 °C; bila yang dimaksud spinach gunakan ~4 °C.";
        return "Tidak ada nilai spesifik yang cukup kuat; isi manual bila tersedia.";
    }
}
