package id.stasiuncuaca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Editable pH-based dolomite screening calculator; explicitly labels every heuristic. */
public final class DolomiteCalculatorDialog {
    private DolomiteCalculatorDialog() {}

    private static final String[] TEXTURE_LABELS = {
            "Mineral ringan / berpasir — 1,0 t CaCO3-eq/ha/pH",
            "Mineral sedang — 2,0 t CaCO3-eq/ha/pH",
            "Mineral berat / liat — 4,0 t CaCO3-eq/ha/pH",
            "Gambut — 5,0 t CaCO3-eq/ha/pH (variasi besar)",
            "Kustom — masukkan faktor sendiri"
    };
    private static final double[] TEXTURE_FACTORS = {1.0, 2.0, 4.0, 5.0, Double.NaN};

    public static void show(Activity activity, SharedPreferences prefs, Runnable onSaved) {
        show(activity, prefs, onSaved, null, null, null);
    }

    /** Optional current-form overrides prevent using stale preferences while editing a field note. */
    public static void show(Activity activity, SharedPreferences prefs, Runnable onSaved,
            String currentPhOverride, String cropOverride, String areaM2Override) {
        int pad = dp(activity, 14);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, pad);

        TextView intro = text(activity,
                "Estimasi awal dari selisih pH. Dosis kapur sebenarnya tidak dapat ditentukan akurat dari pH saja karena kemasaman cadangan/daya sangga, tekstur, kedalaman olah, Al-dd/CEC, bahan organik, dan mutu dolomit sangat berpengaruh.", 14, false);
        content.addView(intro, matchWrap());

        String currentPhValue = currentPhOverride == null ? prefs.getString("soil_ph", "") : currentPhOverride;
        if (currentPhValue == null || currentPhValue.trim().isEmpty()) currentPhValue = latestFieldNotePh(activity, prefs);
        EditText initial = input(activity, currentPhValue, "pH tanah saat ini", 2);
        content.addView(label(activity, "pH tanah saat ini")); content.addView(initial, matchWrap());

        String crop = cropOverride == null || cropOverride.trim().isEmpty() ? prefs.getString("crop", "Komoditas umum") : cropOverride.trim();
        AgronomyEngine.CropProfile profile = AgronomyEngine.profile(crop);
        double defaultTarget = Math.max(profile.phMin, Math.min(profile.phMax, (profile.phMin + profile.phMax) / 2.0));
        boolean sameSavedCrop = crop.equalsIgnoreCase(prefs.getString("limecalc_crop", ""));
        String savedTarget = sameSavedCrop ? prefs.getString("limecalc_target_ph", String.format(Locale.US, "%.2f", defaultTarget)) : String.format(Locale.US, "%.2f", defaultTarget);
        EditText target = input(activity, savedTarget, "pH target tanaman", 2);
        content.addView(label(activity, "pH target yang ingin dicapai")); content.addView(target, matchWrap());

        double defaultAreaM2 = parse(prefs.getString("farm_area_m2", ""));
        if (!Double.isFinite(defaultAreaM2) || defaultAreaM2 <= 0) {
            double legacyHa = parse(prefs.getString("farm_area_ha", ""));
            if (Double.isFinite(legacyHa) && legacyHa > 0) defaultAreaM2 = legacyHa * 10000.0;
        }
        String areaFallback = Double.isFinite(defaultAreaM2) && defaultAreaM2 > 0
                ? fmt(defaultAreaM2, 2) : prefs.getString("limecalc_area_m2", "");
        String areaString = areaM2Override != null ? areaM2Override : areaFallback;
        EditText area = input(activity, areaString, "Luas lahan dalam meter persegi", 4);
        content.addView(label(activity, "Luas lahan (m²)")); content.addView(area, matchWrap());

        Spinner texture = new Spinner(activity);
        texture.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, TEXTURE_LABELS));
        int savedTexture = prefs.getInt("limecalc_texture_position", 1);
        if (savedTexture < 0 || savedTexture >= TEXTURE_LABELS.length) savedTexture = 1;
        texture.setSelection(savedTexture);
        content.addView(label(activity, "Tekstur / daya sangga — pilih faktor asumsi")); content.addView(texture, matchWrap());

        // Preserve a previously saved edited factor; switching presets updates this field live.
        String rateString = prefs.getString("limecalc_rate_t_ha_per_ph", "2.0");
        EditText rate = input(activity, rateString, "Ton CaCO3 ekuivalen per ha untuk kenaikan 1 unit pH", 3);
        content.addView(label(activity, "Faktor kebutuhan kapur ekuivalen (t/ha per 1 pH)")); content.addView(rate, matchWrap());

        EditText neutralizing = input(activity, prefs.getString("limecalc_neutralizing_pct", "80"), "Efektivitas netralisasi produk dalam persen", 2);
        content.addView(label(activity, "Efektivitas netralisasi dolomit / ECCE (%)")); content.addView(neutralizing, matchWrap());

        TextView output = text(activity, "Masukkan pH awal, target, luas lahan, dan periksa faktor asumsi. Tekan HITUNG & SIMPAN untuk menghitung dosis.", 15, false);
        output.setPadding(0, dp(activity, 10), 0, dp(activity, 8));
        content.addView(output, matchWrap());
        TextView caveat = text(activity,
                "Faktor bawaan yang editable (bukan dosis resmi/universal): mineral ringan 1; mineral sedang 2; mineral liat/berat 4; gambut 5 t CaCO3-eq/ha/pH. Contoh literatur FAO pada gambut tropis menunjukkan nilai sekitar 5 t/ha dapat menaikkan pH mendekati satu unit pada lokasi tertentu; hasil tidak dapat digeneralisasi ke semua tanah. Untuk dosis final, utamakan kebutuhan kapur laboratorium/pH-buffer atau Al-dd dan cek kebutuhan Mg sebelum memilih dolomit.", 13, false);
        content.addView(caveat, matchWrap());

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Kalkulator Dolomit Berdasarkan pH")
                .setView(scroll)
                .setPositiveButton("HITUNG & SIMPAN", null)
                .setNegativeButton("TUTUP", null)
                .create();

        texture.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private final int[] previousPosition = {texture.getSelectedItemPosition()};
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Preserve saved/custom input on initial binding; update only after a real preset change.
                if (position != previousPosition[0]) {
                    previousPosition[0] = position;
                    if (position >= 0 && position < TEXTURE_FACTORS.length && Double.isFinite(TEXTURE_FACTORS[position]))
                        rate.setText(fmt(TEXTURE_FACTORS[position], 2));
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double p0 = parse(initial.getText().toString());
            double p1 = parse(target.getText().toString());
            double areaM2 = parse(area.getText().toString());
            double ha = areaM2 / 10000.0;
            double rateValue = parse(rate.getText().toString());
            double enp = parse(neutralizing.getText().toString());
            if (!Double.isFinite(p0) || p0 < 0 || p0 > 14) { initial.setError("Isi pH awal valid (0–14)"); return; }
            if (!Double.isFinite(p1) || p1 < 0 || p1 > 7.5) { target.setError("Isi pH target valid; untuk tanaman budidaya, jangan melebihi 7,5 tanpa dasar khusus"); return; }
            if (!Double.isFinite(areaM2) || areaM2 <= 0) { area.setError("Luas lahan harus lebih dari 0 m²"); return; }
            if (!Double.isFinite(rateValue) || rateValue <= 0 || rateValue > 20) { rate.setError("Faktor asumsi harus lebih dari 0 sampai 20 t/ha/pH"); return; }
            if (!Double.isFinite(enp) || enp <= 0 || enp > 100) { neutralizing.setError("Efektivitas netralisasi harus >0 sampai 100%"); return; }

            double delta = p1 - p0;
            double eq = Math.max(0.0, delta) * rateValue;
            double tHa = AgronomyEngine.estimateDolomiteTonHa(p0, p1, rateValue, enp);
            double totalKg = AgronomyEngine.estimateDolomiteKgTotal(tHa, ha);
            String soilType = TEXTURE_LABELS[texture.getSelectedItemPosition()];
            StringBuilder summary = new StringBuilder();
            if (delta <= 0) {
                summary.append("Tidak ada dosis dolomit yang dihitung: pH target tidak lebih tinggi dari pH awal.");
            } else {
                summary.append(String.format(Locale.US,
                        "pH %.2f → %.2f (Δ %.2f); faktor %.2f t CaCO3-eq/ha/pH; efektivitas dolomit %.1f%%.\nDosis estimasi: %.2f t/ha (%.0f kg/ha). Untuk luas %.2f m²: sekitar %.1f kg dolomit.",
                        p0, p1, delta, rateValue, enp, tHa, tHa * 1000.0, areaM2, totalKg));
                if (delta > 1.0 || tHa > 5.0)
                    summary.append("\nPERHATIAN: perubahan pH/dosis tinggi; jangan aplikasikan sekaligus tanpa uji buffer/Al-dd. Pertimbangkan aplikasi bertahap, pencampuran merata, lalu uji pH ulang.");
                if (p1 > profile.phMax)
                    summary.append("\nPERHATIAN: target di atas kisaran pH maksimum profil ").append(profile.name).append(" (maksimum ").append(fmt(profile.phMax,2)).append("). Periksa ulang target tanaman.");
                summary.append("\nIni estimasi pH-only, bukan kebutuhan kapur laboratorium.");
            }
            output.setText(summary.toString());
            prefs.edit()
                    .putString("limecalc_crop", crop)
                    .putString("limecalc_current_ph", fmt(p0, 3))
                    .putString("limecalc_target_ph", fmt(p1, 3))
                    .putString("limecalc_area_m2", fmt(areaM2, 2))
                    .putString("limecalc_area_ha", fmt(ha, 6))
                    .putString("limecalc_rate_t_ha_per_ph", fmt(rateValue, 4))
                    .putString("limecalc_neutralizing_pct", fmt(enp, 3))
                    .putInt("limecalc_texture_position", texture.getSelectedItemPosition())
                    .putString("limecalc_texture", soilType)
                    .putString("limecalc_delta_ph", fmt(delta, 3))
                    .putString("limecalc_caco3_equivalent_t_ha", fmt(eq, 4))
                    .putString("limecalc_dolomite_t_ha", fmt(tHa, 4))
                    .putString("limecalc_dolomite_kg_ha", fmt(tHa * 1000.0, 2))
                    .putString("limecalc_total_kg", fmt(totalKg, 2))
                    .putString("limecalc_summary", summary.toString())
                    .putBoolean("limecalc_has_result", true)
                    .apply();
            if (onSaved != null) onSaved.run();
            Toast.makeText(activity, "Estimasi dolomit disimpan ke laporan agronomi.", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private static String latestFieldNotePh(Activity activity, SharedPreferences prefs) {
        double bestPh = Double.NaN; long bestCreated = Long.MIN_VALUE;
        try {
            JSONArray a = new JSONArray(prefs.getString("field_notes_v153", "[]"));
            for (int i=0;i<a.length();i++) {
                JSONObject o=a.optJSONObject(i); if(o==null) continue;
                double ph=o.optDouble("soilPh", Double.NaN); long created=o.optLong("created", Long.MIN_VALUE);
                if(Double.isFinite(ph) && ph>=0 && ph<=14 && created>=bestCreated){bestPh=ph;bestCreated=created;}
            }
        } catch(Exception ignored) {}
        try {
            java.io.File f=new java.io.File(activity.getFilesDir(), "history_field_notes_v153.json");
            if(f.isFile()) {
                String raw=new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                JSONArray a=new JSONArray(raw);
                for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;double ph=o.optDouble("soilPh",Double.NaN);long created=o.optLong("created",Long.MIN_VALUE);if(Double.isFinite(ph)&&ph>=0&&ph<=14&&created>=bestCreated){bestPh=ph;bestCreated=created;}}
            }
        } catch(Exception ignored) {}
        return Double.isFinite(bestPh) ? fmt(bestPh, 2) : "";
    }

    private static TextView label(Activity a, String value) { TextView v = text(a, value, 14, true); v.setPadding(0, dp(a, 7), 0, dp(a, 1)); return v; }
    private static TextView text(Activity a, String value, int size, boolean bold) {
        TextView v = new TextView(a); v.setText(value); v.setTextSize(size); v.setTextColor(a.getResources().getColor(R.color.text_main));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLineSpacing(dp(a, 3), 1.03f); return v;
    }
    private static EditText input(Activity a, String value, String hint, int digits) {
        EditText e = new EditText(a); e.setSingleLine(true); e.setTextSize(15); e.setTextColor(a.getResources().getColor(R.color.text_main));
        e.setHint(hint); e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); e.setText(value == null ? "" : value);
        return e;
    }
    private static LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private static int dp(Activity a, int n) { return (int) (n * a.getResources().getDisplayMetrics().density + 0.5f); }
    private static double parse(String value) {
        try { if (value == null || value.trim().isEmpty()) return Double.NaN; return Double.parseDouble(value.trim().replace(',', '.')); }
        catch (Exception ignored) { return Double.NaN; }
    }
    private static String fmt(double value, int decimals) { return Double.isFinite(value) ? String.format(Locale.US, "% ." + decimals + "f", value).trim() : ""; }
}
