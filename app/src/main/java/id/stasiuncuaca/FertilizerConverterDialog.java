package id.stasiuncuaca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Converts nutrient requirements (kg/ha of N, P2O5, K2O) to farmer-selected
 * fertilizer products using the guaranteed analysis written on each bag.
 * Results are a mathematical conversion, not a replacement for a local
 * agronomy recommendation or a label/SOP rate.
 */
public final class FertilizerConverterDialog {
    private FertilizerConverterDialog() {}

    private static final int TEAL = 0xFF29C6C7;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9DB0BC;

    public static void show(Activity activity, SharedPreferences prefs) {
        float density = activity.getResources().getDisplayMetrics().density;
        int pad = Math.max(12, Math.round(14 * density));
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView intro = label(activity, "Masukkan kebutuhan unsur hara (kg/ha). Centang hanya pupuk yang benar-benar tersedia di kebun, lalu tulis nama serta kadar N, P₂O₅, K₂O seperti pada label karung. Hingga lima pupuk tunggal/majemuk bisa diganti sesuai stok (misalnya Urea, ZA, SP-36, KCl, NPK, atau produk lain). Hasil dihitung per hektare dan untuk luas petak jika luas m² sudah disimpan.", 14, WHITE);
        root.addView(intro);

        EditText targetN = input(activity, "Kebutuhan N (kg/ha)", prefs.getString("last_need_n_kg_ha", ""));
        EditText targetP = input(activity, "Kebutuhan P₂O₅ (kg/ha)", prefs.getString("last_need_p2o5_kg_ha", ""));
        EditText targetK = input(activity, "Kebutuhan K₂O (kg/ha)", prefs.getString("last_need_k2o_kg_ha", ""));
        root.addView(targetN); root.addView(targetP); root.addView(targetK);

        String[] defaultName = {"Urea", "SP-36", "KCl", "NPK 15-15-15", "ZA"};
        String[][] defaultPct = {{"46", "0", "0"}, {"0", "36", "0"}, {"0", "0", "60"}, {"15", "15", "15"}, {"21", "0", "0"}};
        // Up to five products can be chosen/edited; labels are always user-adjustable.
        EditText[][] products = new EditText[defaultName.length][4];
        CheckBox[] enabled = new CheckBox[defaultName.length];
        for (int i = 0; i < products.length; i++) {
            TextView head = label(activity, "PUPUK " + (i + 1) + " — boleh diganti sesuai stok petani", 14, TEAL);
            head.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            head.setPadding(0, Math.round(10 * density), 0, Math.round(3 * density));
            root.addView(head);
            enabled[i] = new CheckBox(activity);
            enabled[i].setText("Pupuk ini tersedia / boleh dihitung");
            enabled[i].setTextColor(WHITE);
            enabled[i].setButtonTintList(android.content.res.ColorStateList.valueOf(TEAL));
            enabled[i].setChecked(prefs.getBoolean("fert_conv_enabled_" + i, false));
            root.addView(enabled[i]);
            products[i][0] = inputText(activity, "Nama pupuk", prefs.getString("fert_conv_name_" + i, defaultName[i]));
            products[i][1] = input(activity, "N (%)", prefs.getString("fert_conv_n_" + i, defaultPct[i][0]));
            products[i][2] = input(activity, "P₂O₅ (%)", prefs.getString("fert_conv_p_" + i, defaultPct[i][1]));
            products[i][3] = input(activity, "K₂O (%)", prefs.getString("fert_conv_k_" + i, defaultPct[i][2]));
            root.addView(products[i][0]);
            LinearLayout row = new LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 1; j < 4; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (j > 1) lp.setMargins(Math.round(5 * density), 0, 0, 0);
                row.addView(products[i][j], lp);
            }
            root.addView(row);
        }

        TextView result = label(activity, "Hasil konversi akan ditampilkan setelah dihitung.", 14, WHITE);
        result.setPadding(0, Math.round(12 * density), 0, Math.round(8 * density));
        root.addView(result);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("KONVERSI DOSIS PUPUK")
                .setView(scroll)
                .setNegativeButton("TUTUP", null)
                .setNeutralButton("HITUNG KONVERSI", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            double[] target = {number(targetN), number(targetP), number(targetK)};
            if (!finiteNonnegative(target[0]) || !finiteNonnegative(target[1]) || !finiteNonnegative(target[2])) {
                result.setText("Isi kebutuhan N, P₂O₅, dan K₂O sebagai angka ≥ 0 kg/ha. Nilai kebutuhan yang kosong belum siap dikonversi.");
                return;
            }
            double[][] matrix = new double[3][products.length];
            String[] names = new String[products.length];
            boolean anyEnabled = false;
            SharedPreferences.Editor saved = prefs.edit();
            for (int i = 0; i < products.length; i++) {
                names[i] = products[i][0].getText().toString().trim();
                if (names[i].isEmpty()) names[i] = "Pupuk " + (i + 1);
                boolean use = enabled[i].isChecked();
                anyEnabled |= use;
                saved.putBoolean("fert_conv_enabled_" + i, use)
                        .putString("fert_conv_name_" + i, products[i][0].getText().toString().trim());
                for (int j = 0; j < 3; j++) {
                    double pct = number(products[i][j + 1]);
                    saved.putString(new String[]{"fert_conv_n_", "fert_conv_p_", "fert_conv_k_"}[j] + i,
                            products[i][j + 1].getText().toString().trim());
                    if (!use) continue;
                    if (!finiteNonnegative(pct) || pct > 100) {
                        result.setText("Kadar hara pupuk yang dicentang harus berada pada rentang 0–100%. Periksa kolom N, P₂O₅, K₂O.");
                        return;
                    }
                    matrix[j][i] = pct / 100.0;
                }
            }
            if (!anyEnabled) {
                result.setText("Centang setidaknya satu pupuk yang benar-benar tersedia sebelum menghitung konversi.");
                return;
            }
            saved.apply();
            double[] dose = nonnegativeLeastSquares(matrix, target);
            double[] delivered = new double[3];
            StringBuilder out = new StringBuilder("ESTIMASI DOSIS PRODUK\n");
            double area = numberString(prefs.getString("farm_area_m2", ""));
            if (!(area > 0)) area = numberString(prefs.getString("farm_area_ha", "")) * 10000.0;
            for (int i = 0; i < products.length; i++) {
                if (dose[i] > 0.0001) {
                    out.append(String.format(Locale.US, "• %s: %.2f kg/ha", names[i], dose[i]));
                    if (area > 0) out.append(String.format(Locale.US, " (%.2f kg untuk %.2f m²)", dose[i] * area / 10000.0, area));
                    out.append("\n");
                }
                for (int j = 0; j < 3; j++) delivered[j] += matrix[j][i] * dose[i];
            }
            out.append(String.format(Locale.US, "\nUnsur hara yang tersuplai (kg/ha): N %.2f/%.2f; P₂O₅ %.2f/%.2f; K₂O %.2f/%.2f.\n",
                    delivered[0], target[0], delivered[1], target[1], delivered[2], target[2]));
            double residual = Math.sqrt(square(delivered[0] - target[0]) + square(delivered[1] - target[1]) + square(delivered[2] - target[2]));
            if (residual > 0.5) out.append("PERHATIAN: kombinasi pupuk yang dimasukkan tidak dapat memenuhi semua target sekaligus. Tambah/ubah produk atau terima kekurangan/kelebihan yang tercantum; jangan menganggapnya tepat.");
            else out.append("Kesesuaian matematis baik untuk kadar label yang dimasukkan; cek tetap dosis label, waktu aplikasi dan SOP lokal.");
            out.append("\n\nCatatan: ini mengonversi kebutuhan hara menjadi produk, bukan validasi kebutuhan agronomi. Nilai soil meter 8-in-1 adalah input screening; gunakan rekomendasi spesifik lokasi atau laboratorium jika tersedia.");
            result.setText(out.toString());
        }));
        dialog.show();
    }

    private static TextView label(Activity a, String text, float size, int color) {
        TextView t = new TextView(a); t.setText(text); t.setTextSize(size); t.setTextColor(color); t.setLineSpacing(3, 1.05f); return t;
    }

    private static EditText input(Activity a, String hint, String value) {
        EditText e = inputText(a, hint, value);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        return e;
    }

    private static EditText inputText(Activity a, String hint, String value) {
        EditText e = new EditText(a); e.setSingleLine(true); e.setText(value); e.setHint(hint); e.setTextSize(14); e.setTextColor(WHITE); e.setHintTextColor(MUTED);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setMinHeight(Math.round(46 * a.getResources().getDisplayMetrics().density)); return e;
    }

    private static double number(EditText e) { return numberString(e == null ? "" : e.getText().toString()); }
    private static double numberString(String value) {
        if (value == null || value.trim().isEmpty()) return Double.NaN;
        try { return Double.parseDouble(value.trim().replace(',', '.')); } catch (Exception ignored) { return Double.NaN; }
    }
    private static boolean finiteNonnegative(double x) { return Double.isFinite(x) && x >= 0; }
    private static double square(double x) { return x * x; }

    /** Enumerates nonnegative combinations of up to three active products from the five entered. */
    private static double[] nonnegativeLeastSquares(double[][] a, double[] b) {
        int productCount = a[0].length;
        double bestError = square(b[0]) + square(b[1]) + square(b[2]);
        double[] best = new double[productCount];
        for (int mask = 1; mask < (1 << productCount); mask++) {
            int k = Integer.bitCount(mask);
            // There are three target nutrients (N, P2O5, K2O). An exact/best
            // nonnegative cone solution in this space needs at most three active columns.
            if (k > 3) continue;
            int[] idx = new int[k]; int pos = 0;
            for (int i = 0; i < productCount; i++) if ((mask & (1 << i)) != 0) idx[pos++] = i;
            double[][] normal = new double[k][k + 1];
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < k; j++) for (int r = 0; r < 3; r++) normal[i][j] += a[r][idx[i]] * a[r][idx[j]];
                for (int r = 0; r < 3; r++) normal[i][k] += a[r][idx[i]] * b[r];
            }
            double[] x = solve(normal, k); if (x == null) continue;
            boolean valid = true; for (double v : x) if (!Double.isFinite(v) || v < -1e-6) valid = false;
            if (!valid) continue;
            double[] candidate = new double[productCount]; for (int i = 0; i < k; i++) candidate[idx[i]] = Math.max(0, x[i]);
            double err = 0;
            for (int r = 0; r < 3; r++) { double d = -b[r]; for (int i = 0; i < productCount; i++) d += a[r][i] * candidate[i]; err += d * d; }
            if (err < bestError) { bestError = err; best = candidate; }
        }
        return best;
    }

    private static double[] solve(double[][] m, int n) {
        for (int col = 0; col < n; col++) {
            int pivot = col; for (int r = col + 1; r < n; r++) if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) pivot = r;
            if (Math.abs(m[pivot][col]) < 1e-10) return null;
            double[] tmp = m[col]; m[col] = m[pivot]; m[pivot] = tmp;
            double div = m[col][col]; for (int j = col; j <= n; j++) m[col][j] /= div;
            for (int r = 0; r < n; r++) if (r != col) { double f = m[r][col]; for (int j = col; j <= n; j++) m[r][j] -= f * m[col][j]; }
        }
        double[] x = new double[n]; for (int i = 0; i < n; i++) x[i] = m[i][n]; return x;
    }
}
