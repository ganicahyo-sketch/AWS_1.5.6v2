package id.stasiuncuaca;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Laporan PDF mandiri: cuaca, tanah, histori, analisis, rekomendasi, dan AI.
 * Menggunakan PdfDocument bawaan Android - tanpa library tambahan.
 */
public class PdfReportActivity extends Activity {
    private static final String PREFS = "thingspeak_config";
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter PRINT_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("id", "ID"));

    private Spinner reportMode;
    private TextView reportInfo;
    private File lastPdf;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_pdf_report);
        reportMode = findViewById(R.id.pdfReportMode);
        reportInfo = findViewById(R.id.pdfReportInfo);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Laporan lengkap", "Laporan ringkas"});
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportMode.setAdapter(a);
        findViewById(R.id.pdfBack).setOnClickListener(v -> finish());
        findViewById(R.id.pdfSave).setOnClickListener(v -> createAndSave());
        findViewById(R.id.pdfPrint).setOnClickListener(v -> createAndPrint());
        reportInfo.setText("Laporan A4 dengan tata letak bertahap, nomor halaman, ringkasan eksekutif, dan tabel riwayat yang dapat berlanjut antarhalaman. Pilih laporan lengkap untuk catatan dan dasar ilmiah lebih rinci, atau laporan ringkas untuk ringkasan dan riwayat terbaru.");
    }

    private void createAndSave() {
        try {
            File f = buildPdf(reportMode.getSelectedItemPosition() == 0);
            lastPdf = f;
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            i.setType("application/pdf");
            i.putExtra(android.content.Intent.EXTRA_TITLE, fileName());
            startActivityForResult(i, 9001);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuat PDF: " + safe(e), Toast.LENGTH_LONG).show();
        }
    }

    private void createAndPrint() {
        try {
            lastPdf = buildPdf(reportMode.getSelectedItemPosition() == 0);
            PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            if (pm == null) throw new Exception("Layanan cetak Android tidak tersedia");
            pm.print("STASIUN CUACA • GANI CAHYO-UNS", new PdfPrintAdapter(lastPdf), printAttributes());
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka cetak: " + safe(e), Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 9001 || resultCode != RESULT_OK || data == null || data.getData() == null || lastPdf == null) return;
        try (InputStream in = new BufferedInputStream(new FileInputStream(lastPdf))) {
            OutputStream raw = getContentResolver().openOutputStream(data.getData());
            if (raw == null) throw new Exception("Lokasi penyimpanan tidak dapat dibuka");
            try (OutputStream out = new BufferedOutputStream(raw)) {
                byte[] buf = new byte[32768]; int n;
                while ((n = in.read(buf)) >= 0) { if (n == 0) continue; out.write(buf, 0, n); }
                out.flush();
            }
            Toast.makeText(this, "PDF tersimpan.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan PDF: " + safe(e), Toast.LENGTH_LONG).show();
        }
    }

    private PrintAttributes printAttributes() {
        return new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("stasiun-cuaca", "STASIUN CUACA", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
    }

    private String fileName() {
        String crop = pref("crop", "Tanaman").trim().replaceAll("[^A-Za-z0-9_-]+", "_");
        String date = LocalDate.now(WIB).format(DateTimeFormatter.BASIC_ISO_DATE);
        return "STASIUN-CUACA_LAPORAN_AGRONOMI_" + crop + "_" + date + ".pdf";
    }

    private File buildPdf(boolean complete) throws Exception {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        File dir = new File(getCacheDir(), "reports");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Folder laporan gagal dibuat");
        File out = File.createTempFile("stasiun_report_", ".pdf", dir);

        PdfDocument doc = new PdfDocument();
        ReportCanvas rc = new ReportCanvas(doc);
        rc.brand("STASIUN CUACA • GANI CAHYO-UNS");
        rc.heading("LAPORAN AGRONOMI");
        rc.subheading("Edisi " + (complete ? "lengkap" : "ringkas") + " • Cuaca, tanah, dan rekomendasi budidaya");
        rc.meta("Tanggal laporan: " + java.time.ZonedDateTime.now(WIB).format(PRINT_DATE) + " WIB");
        rc.meta("Komoditas: " + pref("crop", "Tanaman pertanian") + " | Sistem budidaya: " + pref("farm_cultivation_mode", "Konvensional / PHT"));
        rc.meta("Luas lahan: " + pref("farm_area_ha", "--") + " ha | Umur: " + pref("farm_hst", "--") + " | Fase: " + phaseText());
        rc.line();

        addExecutiveSummary(rc, p);
        addCurrentWeather(rc, p);
        addSoil(rc, p);
        addAgronomyAnalysis(rc, p);
        addOptRisk(rc, p);
        addRecommendations(rc, p);
        addHistory(rc, p, complete);
        if (complete) {
            addAi(rc, p);
            addSources(rc);
        }

        rc.finish(out);
        return out;
    }

    private void addExecutiveSummary(ReportCanvas rc, android.content.SharedPreferences p) {
        String crop = pref("crop", "Tanaman pertanian");
        AgronomyEngine.CropProfile cp = AgronomyEngine.profile(crop);
        double ph = num(pref("soil_ph", ""));
        double n = num(pref("soil_n", ""));
        double pp = num(pref("soil_p", ""));
        double k = num(pref("soil_k", ""));
        double nLow = num(pref("soil_n_low", ""));
        double nHigh = num(pref("soil_n_high", ""));
        if (!NitrogenInference.usesMethodSpecificThresholds(pref("soil_n_classification_basis", NitrogenInference.BASIS_INFERRED))) { nLow = Double.NaN; nHigh = Double.NaN; }
        double vpd = num(pref("om_vpd", ""));
        rc.section("RINGKASAN EKSEKUTIF");
        rc.kv("Komoditas dan fase", crop + " • " + phaseText());
        rc.kv("Indikator cuaca", "Suhu " + pref("om_temp", "--") + " °C; RH " + pref("om_rh", "--") + " %; VPD " + pref("om_vpd", "--") + " kPa (" + AgronomyEngine.classifyVpd(vpd) + "); hujan " + pref("om_rain", "--") + " mm/hari; ET0 " + pref("om_et0", "--") + " mm/hari.");
        String nSummary = AgronomyEngine.hasValidNThresholds(nLow,nHigh)
                ? "N tersedia " + show(n) + " mg/kg (kelas menurut ambang yang diisi: " + AgronomyEngine.classifyN(n,nLow,nHigh) + ")"
                : "N tersedia " + show(n) + " mg/kg (kelas N-total estimasi asumsi 2%: " + NitrogenInference.classifyEstimatedTotalN(n) + ")";
        rc.kv("Kondisi tanah", "pH " + show(ph) + " (" + AgronomyEngine.classifyPH(ph, cp) + "); " + nSummary + "; P " + show(pp) + " mg/kg (" + AgronomyEngine.classifyP(pp, pref("soil_test_method", "Metode tidak diketahui")) + "); K " + show(k) + " mg/kg (" + AgronomyEngine.classifyK(k, pref("soil_test_method", "Metode tidak diketahui")) + ").");
        rc.text("Ringkasan ini adalah pemeriksaan awal berbasis data yang tersimpan. Ikuti rekomendasi terperinci pada bab berikutnya dan konfirmasi kondisi yang memerlukan pemeriksaan lapangan atau analisis laboratorium.");
        rc.line();
    }

    private void addCurrentWeather(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("1. DATA CUACA TERKINI DAN PRAKIRAAN");
        rc.kv("Sumber cuaca makro", pref("macro_weather_provider_used", pref("macro_weather_provider", "Open-Meteo")));
        rc.kv("Suhu udara", pref("om_temp","--")+" °C");
        rc.kv("Suhu terasa", pref("om_apparent_temp","--")+" °C");
        rc.kv("Titik embun", pref("om_dewpoint","--")+" °C");
        rc.kv("Kelembapan", pref("om_rh","--")+" %");
        rc.kv("Tekanan", pref("om_pressure","--")+" hPa");
        rc.kv("Curah hujan harian", pref("om_rain","--")+" mm/hari");
        rc.kv("Curah hujan 1 jam terakhir", pref("om_rain_now","--")+" mm");
        rc.kv("Matahari terbit / terbenam", pref("om_sunrise","--")+" / "+pref("om_sunset","--")+" (waktu lokasi)");
        rc.kv("UV saat ini / maksimum hari ini", pref("om_uv","--")+" / "+pref("om_uv_daily_max","--"));
        rc.kv("ET0 FAO-56", pref("om_et0","--")+" mm/hari");
        double vpd=num(pref("om_vpd","")); rc.kv("VPD",pref("om_vpd","--")+" kPa -> "+AgronomyEngine.classifyVpd(vpd));
        rc.kv("Arah angin",pref("om_wind_direction","--")+" | "+pref("om_wind_speed","--")+" m/s | gust "+pref("om_wind_gust","--")+" m/s");
        rc.kv("Awan",pref("om_cloud_cover","--")+" % | UV "+pref("om_uv","--"));
        rc.kv("Visibilitas",pref("om_visibility","--")+" m");
        rc.kv("Lama penyinaran",pref("om_sun_hours","--")+" jam/hari");
        rc.kv("Radiasi Shortwave",pref("om_radiation","--")+" W/m²");
        rc.kv("PAR estimasi",pref("om_par","--")+" W/m²");
        rc.kv("PPFD estimasi",pref("om_ppfd","--")+" µmol/m²/s");
        rc.kv("Kondisi",pref("om_weather","--"));
        rc.kv("Forecast 7 hari",pref("om_forecast_7d","--"));
        rc.line();
    }


    private void addSoil(ReportCanvas rc, android.content.SharedPreferences p) {
        String crop=pref("crop","Tanaman pertanian"); AgronomyEngine.CropProfile cp=AgronomyEngine.profile(crop);
        double ph=num(pref("soil_ph","")),n=num(pref("soil_n","")),pp=num(pref("soil_p","")),k=num(pref("soil_k","")),nLow=num(pref("soil_n_low","")),nHigh=num(pref("soil_n_high",""));
        if (!NitrogenInference.usesMethodSpecificThresholds(pref("soil_n_classification_basis", NitrogenInference.BASIS_INFERRED))) { nLow=Double.NaN; nHigh=Double.NaN; }
        double ec=num(pref("soil_ec_us_cm","")),ece=num(pref("soil_ece_ds_m","")),moist=num(pref("soil_moisture_pct",""));
        double depth=num(pref("soil_depth_cm","20")),bd=num(pref("soil_bulk_density_g_cm3","1.30")),fc=num(pref("soil_fc_pct","")),pwp=num(pref("soil_pwp_pct",""));
        double phBuf=num(pref("soil_ph_buffer","")),al=num(pref("soil_al_dd","")),hd=num(pref("soil_h_dd","")),cec=num(pref("soil_cec","")),om=num(pref("soil_om_pct","")),lime=num(pref("soil_lime_requirement_kg_ha",""));
        String method=pref("soil_test_method","Metode tidak diketahui");
        rc.section("2. KONDISI TANAH DAN HASIL PENGUKURAN");
        rc.kv("Sumber sensor",pref("soil_source","CWT-7IN1 RS485 / USB"));
        rc.kv("Metode N/P/K",method);
        rc.kv("pH",show(ph)+" -> "+AgronomyEngine.classifyPH(ph,cp));
        rc.kv("Tindakan pH",AgronomyEngine.limeAdvice(ph,cp,pref("farm_cultivation_mode","Konvensional / PHT"),phBuf,al,hd,cec,om,lime));
        rc.kv("Kalkulator dolomit (estimasi pH-only)", pref("limecalc_summary", "Belum dihitung; gunakan kalkulator dolomit pada Analisis Agronomi."));
        rc.kv("pH-buffer / Al-dd / H-dd / CEC",show(phBuf)+" / "+show(al)+" / "+show(hd)+" / "+show(cec));
        rc.kv("Bahan organik",show(om)+" %");
        rc.kv("N tersedia",show(n)+" mg/kg");
        rc.kv("Basis klasifikasi N",pref("soil_n_classification_basis", NitrogenInference.BASIS_INFERRED));
        if(AgronomyEngine.hasValidNThresholds(nLow,nHigh))
            rc.kv("Kelas N tersedia",AgronomyEngine.classifyN(n,nLow,nHigh)+" (ambang metode yang diisi pengguna)");
        else
            rc.kv("Kelas N-total estimasi",NitrogenInference.classifyEstimatedTotalN(n)+" (asumsi utama 2%)");
        rc.kv("N-total estimasi",NitrogenInference.estimateTotalN(n));
        rc.kv("Rentang kelas sensitivitas 1–3%",NitrogenInference.classifyRange(n));
        rc.kv("P tersedia",show(pp)+" mg/kg -> "+AgronomyEngine.classifyP(pp,method));
        rc.kv("K tersedia",show(k)+" mg/kg -> "+AgronomyEngine.classifyK(k,method));
        rc.kv("EC sensor",show(ec)+" µS/cm -> "+AgronomyEngine.classifyEC(ec,cp)+" (input utama; ECe lab opsional)");
        rc.kv("ECe laboratorium",Double.isFinite(ece)?show(ece)+" dS/m -> "+AgronomyEngine.classifyECe(ece):"belum ada");
        rc.kv("Kelembapan",show(moist)+" %");
        rc.kv("FC / PWP",show(fc)+" / "+show(pwp)+" % volume");
        rc.kv("Status air tanah",AgronomyEngine.soilWaterAssessment(moist,fc,pwp,depth,num(pref("om_et0","")),crop));
        rc.kv("Lapisan / bulk density",show(depth)+" cm / "+show(bd)+" g/cm³");
        rc.kv("Stok teoritis input N-tersedia / P / K",show(AgronomyEngine.soilStockKgHa(n,bd,depth))+" / "+show(AgronomyEngine.soilStockKgHa(pp,bd,depth))+" / "+show(AgronomyEngine.soilStockKgHa(k,bd,depth))+" kg/ha");
        rc.text("Stok adalah konversi teoritis dari konsentrasi input. Jika input N merupakan N tersedia, hasilnya bukan N-total terukur dan tidak digunakan sebagai kredit pupuk. Stok juga bukan serapan langsung tanaman. EC sensor lapang dipakai sebagai input utama; ECe laboratorium bersifat opsional.");
        rc.line();
    }


    private void addAgronomyAnalysis(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("3. ANALISIS AGRONOMI");
        String saved = pref("last_field_analysis", "");
        if (saved.trim().isEmpty()) saved = pref("last_agronomy_analysis", "");
        if (saved.trim().isEmpty()) {
            double n = num(pref("soil_n", "")), pp = num(pref("soil_p", "")), k = num(pref("soil_k", ""));
            double nLow = num(pref("soil_n_low", "")), nHigh = num(pref("soil_n_high", ""));
            if (!NitrogenInference.usesMethodSpecificThresholds(pref("soil_n_classification_basis", NitrogenInference.BASIS_INFERRED))) { nLow = Double.NaN; nHigh = Double.NaN; }
            String method = pref("soil_test_method", "Metode tidak diketahui");
            String crop = pref("crop", "Tanaman pertanian"); int hst = (int)Math.round(num(pref("farm_hst", "-1")));
            rc.text("Belum ada analisis tersimpan. Ringkasan otomatis: fase " + AgronomyEngine.phase(hst, crop) + "; N " + AgronomyEngine.classifyN(n,nLow,nHigh) + "; P " + AgronomyEngine.classifyP(pp,method) + "; K " + AgronomyEngine.classifyK(k,method) + "; VPD " + AgronomyEngine.classifyVpd(num(pref("om_vpd", ""))) + ".");
        } else {
            // Preserve paragraph breaks in the farmer-friendly saved analysis.
            String[] paragraphs = saved.split("\n", -1);
            boolean firstLine = true;
            for (String paragraph : paragraphs) {
                String line = paragraph == null ? "" : paragraph.trim();
                if (line.isEmpty()) {
                    rc.gap(4f);
                    continue;
                }
                boolean numberedHeading = line.matches("^[0-9]+\\. .*");
                boolean allCapsHeading = line.length() <= 72
                        && line.equals(line.toUpperCase(Locale.ROOT))
                        && line.matches(".*[A-Z].*")
                        && !line.startsWith("-") && !line.startsWith("•");
                if (firstLine || numberedHeading || allCapsHeading) rc.subheading(line);
                else rc.text(line);
                firstLine = false;
            }
        }
        rc.line();
    }

    private void addOptRisk(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("4. RISIKO HAMA DAN PENYAKIT (OPT)");
        String crop = pref("crop", "Tanaman pertanian");
        double t = num(pref("om_temp", "")), rh = num(pref("om_rh", "")), rain = num(pref("om_rain", "")), wind = num(pref("om_wind_speed", ""));
        rc.text(AgronomyEngine.optRisk(crop, t, rh, rain, wind, recentOpt(p, crop)));
        rc.text("Catatan: ini adalah peringatan dini berbasis cuaca + histori, bukan diagnosis pasti. Konfirmasi di lapangan melalui gejala, populasi, luas serangan, dan musuh alami.");
        rc.line();
    }

    private void addHistory(ReportCanvas rc, android.content.SharedPreferences p, boolean complete) {
        rc.section("6. RIWAYAT KEGIATAN LAPANGAN");
        List<HistoryItem> items = new ArrayList<>();
        collect(items, p.getString("field_notes_v153", "[]"), "LAPANG", "observation", "action");
        collect(items, p.getString("fert_history", "[]"), "PUPUK", "product", "note");
        collect(items, p.getString("opt_history", "[]"), "OPT", "target", "result");
        Collections.sort(items, Comparator.comparingLong(a -> -a.created));
        if (items.isEmpty()) {
            rc.text("Belum ada histori tersimpan.");
        } else {
            rc.tableHeader(new String[]{"Tanggal", "Jenis", "Kegiatan / OPT", "Detail"}, new float[]{70, 55, 145, 245});
            int historyLimit = complete ? 30 : 8;
            for (int i = 0; i < Math.min(historyLimit, items.size()); i++) {
                HistoryItem h = items.get(i);
                rc.tableRow(new String[]{
                        cleanCell(h.date),
                        cleanCell(h.type),
                        cleanCell(h.main),
                        cleanCell(joinDetail(h.second, h.extra))
                }, new float[]{70, 55, 145, 245});
            }
            rc.endTable();
            if (!complete && items.size() > historyLimit) {
                rc.text("Laporan ringkas menampilkan " + historyLimit + " catatan terbaru dari " + items.size() + " catatan tersimpan. Gunakan laporan lengkap untuk melihat lebih banyak riwayat.");
            }
        }
        rc.line();
    }

    private String joinDetail(String second, String extra) {
        String a = cleanCell(second);
        String b = cleanCell(extra);
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + "\n" + b;
    }

    private String cleanCell(String s) {
        if (s == null) return "";
        String value = s.replace('\"', '\'').replace("\n", " ").replace("\r", " ").trim();
        // Keep unusually long notes from creating a single table row taller than a printed page.
        if (value.length() > 280) value = value.substring(0, 277).trim() + "…";
        return value;
    }

    private void collect(List<HistoryItem> out, String json, String type, String mainKey, String secondKey) {
        try {
            JSONArray a = new JSONArray(json);
            for (int i=0;i<a.length();i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                HistoryItem h = new HistoryItem();
                h.created = o.optLong("created", 0); h.date = o.optString("date", "--");
                h.type = type; h.main = o.optString(mainKey, ""); h.second = o.optString(secondKey, "");
                h.extra = o.optString("cultivation", "");
                if ("PUPUK".equals(type)) h.extra += " | dosis " + o.optString("dose", "--") + " " + o.optString("unit", "");
                if ("OPT".equals(type)) h.extra += " | serangan " + o.optString("affectedPct", "--") + "% | metode " + o.optString("method", "");
                out.add(h);
            }
        } catch (Exception ignored) {}
    }

    private void addRecommendations(ReportCanvas rc, android.content.SharedPreferences p) {
        rc.section("5. REKOMENDASI TEKNIS");
        String crop=pref("crop","Tanaman pertanian"); AgronomyEngine.CropProfile cp=AgronomyEngine.profile(crop); String mode=pref("farm_cultivation_mode","Konvensional / PHT");
        double ph=num(pref("soil_ph","")),ec=num(pref("soil_ec_us_cm","")),ece=num(pref("soil_ece_ds_m","")),m=num(pref("soil_moisture_pct",""));
        double vpd=num(pref("om_vpd","")),rain=num(pref("om_rain","")),et0=num(pref("om_et0","")); int hst=(int)Math.round(num(pref("farm_hst","-1")));
        if(Double.isFinite(ph)&&ph<cp.phMin)rc.text("- pH rendah: jangan menentukan dosis dolomit hanya dari pH. Pakai pH-buffer, Al-dd/H-dd, CEC atau kebutuhan kapur laboratorium.");
        if(Double.isFinite(ph)&&ph>cp.phMax)rc.text("- pH tinggi: hentikan sementara kapur/dolomit; periksa alkalinitas air dan dasar kebutuhan pengasaman.");
        if(Double.isFinite(ec)&&ec/1000.0>cp.ecThresholdDsM)rc.text("- EC sensor tinggi: kurangi pupuk pekat sekali aplikasi, cek drainase dan kualitas air; konfirmasi dengan ECe bila perlu.");
        if(Double.isFinite(ece)&&ece>=4)rc.text("- ECe menunjukkan salinitas sedikit-sangat tinggi menurut kelas USDA-NRCS; tindakan harus menyesuaikan toleransi tanaman.");
        if(Double.isFinite(m)&&m<20)rc.text("- Tanah cenderung kering: cek air zona akar sebelum memberi pupuk yang membutuhkan air untuk larut.");
        if(Double.isFinite(m)&&m>85)rc.text("- Tanah sangat lembap: cek drainase dan tunda input yang berisiko hilang.");
        if(Double.isFinite(vpd)&&vpd>2)rc.text("- VPD tinggi: pantau layu/kehilangan air; kombinasi VPD tinggi + tanah kering menjadi prioritas.");
        if(Double.isFinite(rain)&&Double.isFinite(et0)&&rain<et0)rc.text("- Hujan < ET0: cek cadangan air tanah dan forecast, jangan menetapkan irigasi dari hujan saja.");
        rc.text("- Fase: "+AgronomyEngine.phase(hst,crop)+". Kebutuhan pupuk mengikuti fase, target hasil, status tanah, efisiensi dan histori.");
        rc.text("- Model dosis: "+AgronomyEngine.nutrientFormulaText());
        rc.text(mode.toLowerCase(Locale.US).contains("organik") ? "- ORGANIK: gunakan input/proses yang diizinkan standar organik; prioritaskan bahan organik matang, sanitasi, rotasi, varietas toleran dan agen hayati." : "- KONVENSIONAL/PHT: monitoring dulu, kemudian tindakan; pestisida mengikuti label, interval pra-panen, APD dan rotasi bahan aktif.");
        rc.text("- OPT: gunakan hasil screening sebagai peringatan dini. Verifikasi gejala, populasi, luas serangan, stadia dan musuh alami.");
        rc.line();
    }


    private void addAi(ReportCanvas rc, android.content.SharedPreferences p) {
        String ai = pref("last_ai_advice", "");
        if (ai.trim().isEmpty()) return;
        rc.section("7. CATATAN REKOMENDASI AI TERAKHIR");
        rc.text(ai);
        rc.line();
    }

    private void addSources(ReportCanvas rc) {
        rc.section("8. DASAR ILMIAH DAN BATASAN INTERPRETASI");
        rc.text("- FAO-56 Penman-Monteith untuk ET0 dan kerangka Kc/ETc.");
        rc.text("- Pendekatan status hara tanah / pemupukan spesifik lokasi; persamaan STCR yang presisi memerlukan kalibrasi tanah-komoditas-setempat.");
        rc.text("- PHT/IPM dan ambang ekonomi: keputusan pengendalian mengikuti hasil monitoring, bukan cuaca saja.");
        rc.text("- Model OPT berbasis cuaca adalah peringatan dini; verifikasi organisme dan serangan di lapangan tetap wajib.");
        rc.text("- pH: kebutuhan kapur sebaiknya memakai uji buffer/kemasaman tertukar jika dosis koreksi diperlukan.");
        rc.text("- EC sensor lapang adalah parameter input utama untuk pemantauan/rentang; ECe laboratorium dapat dipakai sebagai pembanding bila tersedia.");
        rc.text("- FAO-56: Penman-Monteith, TAW/RAW/available soil water.\n- USDA-NRCS: kelas ECe salinitas.\n- UMN/soil testing guidance: pH-buffer diperlukan untuk kebutuhan kapur.\n- Mehlich-3: kelas P/K berbasis metode tidak disamakan dengan metode lain.\n- SNI 6729:2016: sistem pertanian organik Indonesia.\n- Disease forecasting: suhu + RH + hujan/leaf wetness; degree-day untuk serangga bila model Tbase tersedia.\n- Sumber pustaka rinci: SCIENTIFIC_SOURCES.md.");
    }

    private String recentOpt(android.content.SharedPreferences p, String crop) {
        try {
            JSONArray a = new JSONArray(p.getString("opt_history", "[]"));
            StringBuilder s = new StringBuilder(); int count=0;
            for (int i=a.length()-1;i>=0 && count<3;i--) {
                JSONObject o=a.optJSONObject(i); if(o==null)continue;
                if(!crop.equalsIgnoreCase(o.optString("crop", crop))) continue;
                if(count++>0)s.append("; ");
                s.append(o.optString("target","OPT"));
            }
            return s.length()==0?"tidak ada histori OPT terbaru":s.toString();
        } catch(Exception e){return "histori OPT tidak terbaca";}
    }

    private String phaseText(){ int h=(int)Math.round(num(pref("farm_hst","-1"))); return AgronomyEngine.phase(h,pref("crop","Tanaman pertanian")); }
    private String pref(String k,String def){ return getSharedPreferences(PREFS,MODE_PRIVATE).getString(k,def); }
    private static double num(String s){try{return s==null||s.trim().isEmpty()?Double.NaN:Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    private static String show(double x){return Double.isFinite(x)?String.format(Locale.US,"%.2f",x):"--";}
    private static String safe(Throwable e){return e.getMessage()==null?"kesalahan tidak diketahui":e.getMessage();}

    private static final class HistoryItem {
        long created; String date,type,main,second,extra;
    }

    private static final class PdfPrintAdapter extends PrintDocumentAdapter {
        private final File file;
        private PdfRenderer renderer;
        private int pages;
        PdfPrintAdapter(File file){this.file=file;}
        @Override public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, android.os.Bundle extras) {
            try {
                if (cancellationSignal.isCanceled()) { callback.onLayoutCancelled(); return; }
                ParcelFileDescriptor pfd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
                renderer=new PdfRenderer(pfd); pages=renderer.getPageCount(); renderer.close(); renderer=null;
                PrintDocumentInfo info=new PrintDocumentInfo.Builder(file.getName()).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages).build();
                callback.onLayoutFinished(info,true);
            } catch(Exception e){callback.onLayoutFailed(e.getMessage());}
        }
        @Override public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
            try(InputStream in=new BufferedInputStream(new FileInputStream(file));OutputStream out=new BufferedOutputStream(new ParcelFileDescriptor.AutoCloseOutputStream(destination))){
                byte[] buf=new byte[32768];int n;while((n=in.read(buf))>=0){if(cancellationSignal.isCanceled()){callback.onWriteCancelled();return;}if(n>0)out.write(buf,0,n);}out.flush();callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }catch(Exception e){callback.onWriteFailed(e.getMessage());}
        }
    }

    private static final class ReportCanvas {
        private static final int W = 595, H = 842, LEFT = 40, RIGHT = 40, TOP = 38, BOTTOM = 42;
        private final PdfDocument doc;
        private PdfDocument.Page page;
        private Canvas c;
        private Paint title, head, body, label, brandPaint, rule, sectionFill, accentFill;
        private Paint tableHeaderFill, tableHeaderText, tableGrid, rowFillA, rowFillB;
        private float y;
        private String[] lastTableHeader;
        private float[] lastTableWidths;
        private String pendingSection;
        private boolean inTable;
        private int rowIndex;

        ReportCanvas(PdfDocument doc) {
            this.doc = doc;
            title = p(14f, true, 0xFF17324D);
            head = p(12f, true, 0xFF17324D);
            body = p(11f, false, 0xFF202B35);
            label = p(11f, true, 0xFF243746);
            brandPaint = p(9f, true, 0xFF167D78);
            rule = stroke(0xFFCBD5DF, 0.8f);
            sectionFill = fill(0xFFEAF1F6);
            accentFill = fill(0xFF167D78);
            tableHeaderFill = fill(0xFF17324D);
            tableHeaderText = p(11f, true, 0xFFFFFFFF);
            tableGrid = stroke(0xFFD3DCE4, 0.65f);
            rowFillA = fill(0xFFFFFFFF);
            rowFillB = fill(0xFFF5F8FA);
            newPage();
        }

        private Paint p(float size, boolean bold, int color) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            paint.setColor(color);
            paint.setTextSize(size);
            // Ask for Arial explicitly. Android substitutes its closest sans-serif face if Arial is absent.
            paint.setTypeface(Typeface.create("Arial", bold ? Typeface.BOLD : Typeface.NORMAL));
            return paint;
        }

        private Paint fill(int color) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            return paint;
        }

        private Paint stroke(int color, float width) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            paint.setStrokeWidth(width);
            paint.setStyle(Paint.Style.STROKE);
            return paint;
        }

        private void newPage() {
            if (page != null) finishCurrentPage();
            page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, doc.getPages().size() + 1).create());
            c = page.getCanvas();
            y = TOP + 12f;
            if (inTable && lastTableHeader != null) drawTableHeaderInternal();
        }

        private void finishCurrentPage() {
            if (page == null) return;
            float footerRuleY = H - BOTTOM + 7f;
            c.drawLine(LEFT, footerRuleY, W - RIGHT, footerRuleY, rule);
            Paint footer = p(8f, false, 0xFF657583);
            c.drawText("STASIUN CUACA • LAPORAN AGRONOMI", LEFT, H - 16f, footer);
            String pageLabel = "Halaman " + (doc.getPages().size() + 1);
            c.drawText(pageLabel, W - RIGHT - footer.measureText(pageLabel), H - 16f, footer);
            doc.finishPage(page);
            page = null;
        }

        // y is the next text baseline; this is intentionally measured against page height,
        // not against the bottom-margin value. The old comparison created a new page on almost
        // every draw call, leaving a single line or item per page.
        private void need(float height) {
            if (y + height > H - BOTTOM) newPage();
        }

        void brand(String text) {
            need(15f);
            c.drawText(text, LEFT, y, brandPaint);
            y += 15f;
        }

        void heading(String text) {
            need(22f);
            c.drawText(text, LEFT, y, title);
            y += 21f;
        }

        void subheading(String text) {
            String[] lines = wrapLines(text == null ? "" : text, head, W - LEFT - RIGHT);
            float lineHeight = 17f;
            flushPendingSection(lines.length * lineHeight);
            need(Math.max(34f, lines.length * lineHeight + 6f));
            for (String lineText : lines) {
                need(lineHeight);
                c.drawText(lineText, LEFT, y, head);
                y += lineHeight;
            }
            y += 2f;
        }

        void meta(String text) {
            wrap(text, body, 15f, LEFT);
        }

        void section(String text) {
            if (pendingSection != null) flushPendingSection(15f);
            pendingSection = text == null ? "" : text;
        }

        private void flushPendingSection(float upcomingContentHeight) {
            if (pendingSection == null) return;
            float contentReserve = Math.min(90f, Math.max(15f, upcomingContentHeight));
            need(30f + contentReserve);
            y += 6f;
            float top = y - 11f;
            c.drawRect(LEFT, top, W - RIGHT, y + 7f, sectionFill);
            c.drawRect(LEFT, top, LEFT + 3f, y + 7f, accentFill);
            c.drawText(pendingSection, LEFT + 9f, y + 3f, head);
            y += 24f;
            pendingSection = null;
        }

        void line() {
            flushPendingSection(14f);
            need(14f);
            c.drawLine(LEFT, y, W - RIGHT, y, rule);
            y += 13f;
        }

        void gap(float height) {
            if (height <= 0f || pendingSection != null) return;
            need(height);
            y += height;
        }

        void kv(String key, String value) {
            String safeKey = key == null ? "" : key.trim();
            String safeValue = value == null ? "--" : value.trim();
            float valueX = LEFT + 157f;
            float keyWidth = valueX - LEFT - 12f;
            float valueWidth = W - RIGHT - valueX;
            String[] keyLines = wrapLines(safeKey, label, keyWidth);
            String[] valueLines = wrapLines(safeValue, body, valueWidth);
            int rows = Math.max(keyLines.length, valueLines.length);
            float lineHeight = 14f;
            float rowHeight = rows * lineHeight + 3f;
            flushPendingSection(rowHeight); 
            need(rowHeight);
            for (int i = 0; i < rows; i++) {
                if (i < keyLines.length) c.drawText(keyLines[i], LEFT, y + i * lineHeight, label);
                if (i < valueLines.length) c.drawText(valueLines[i], valueX, y + i * lineHeight, body);
            }
            y += rowHeight;
        }

        void text(String text) {
            if (pendingSection != null) {
                float firstParagraphHeight = 15f;
                if (text != null) {
                    for (String paragraph : text.replace('\r', ' ').split("\\n", -1)) {
                        if (!paragraph.trim().isEmpty()) {
                            firstParagraphHeight = Math.min(90f, wrapLines(paragraph, body, W - LEFT - RIGHT).length * 15f);
                            break;
                        }
                    }
                }
                flushPendingSection(firstParagraphHeight);
            }
            wrap(text, body, 15f, LEFT);
            y += 2f;
        }

        void tableHeader(String[] cells, float[] widths) {
            // Reserve the heading, table header, and a full first row together to avoid an orphaned header.
            flushPendingSection(220f);
            // Make room before enabling table continuation, avoiding a duplicated first header.
            need(28f);
            lastTableHeader = cells.clone();
            lastTableWidths = widths.clone();
            inTable = true;
            rowIndex = 0;
            drawTableHeaderInternal();
        }

        private void drawTableHeaderInternal() {
            float total = 0f;
            for (float width : lastTableWidths) total += width;
            float tableRight = Math.min(W - RIGHT, LEFT + total);
            c.drawRect(LEFT, y - 10f, tableRight, y + 10f, tableHeaderFill);
            float x = LEFT;
            for (int i = 0; i < lastTableHeader.length; i++) {
                String value = lastTableHeader[i] == null ? "" : lastTableHeader[i];
                String[] lines = wrapLines(value, tableHeaderText, Math.max(20f, lastTableWidths[i] - 8f));
                // Most headings are short; if one wraps, render it cleanly across the header height.
                c.drawText(lines[0], x + 4f, y + 3f, tableHeaderText);
                x += lastTableWidths[i];
                if (i < lastTableHeader.length - 1) c.drawLine(x, y - 10f, x, y + 10f, tableGrid);
            }
            c.drawRect(LEFT, y - 10f, tableRight, y + 10f, tableGrid);
            y += 23f;
        }

        void tableRow(String[] cells, float[] widths) {
            String[][] wrapped = new String[cells.length][];
            int maxLines = 1;
            for (int i = 0; i < cells.length; i++) {
                wrapped[i] = wrapLines(cells[i], body, Math.max(24f, widths[i] - 8f));
                maxLines = Math.max(maxLines, wrapped[i].length);
            }
            float lineHeight = 14f;
            float rowHeight = maxLines * lineHeight + 7f;
            need(rowHeight + 1f);
            float top = y - 10f;
            float bottom = top + rowHeight;
            float total = 0f;
            for (float width : widths) total += width;
            float tableRight = Math.min(W - RIGHT, LEFT + total);
            c.drawRect(LEFT, top, tableRight, bottom, (rowIndex % 2 == 0) ? rowFillA : rowFillB);
            float x = LEFT;
            for (int i = 0; i < cells.length; i++) {
                for (int j = 0; j < wrapped[i].length; j++) {
                    c.drawText(wrapped[i][j], x + 4f, y + j * lineHeight + 1f, body);
                }
                x += widths[i];
                if (i < cells.length - 1) c.drawLine(x, top, x, bottom, tableGrid);
            }
            c.drawRect(LEFT, top, tableRight, bottom, tableGrid);
            c.drawLine(LEFT, bottom, tableRight, bottom, tableGrid);
            y = bottom + 5f;
            rowIndex++;
        }

        void endTable() {
            inTable = false;
            lastTableHeader = null;
            lastTableWidths = null;
            rowIndex = 0;
        }

        private String[] wrapLines(String input, Paint paint, float maxWidth) {
            if (input == null || input.trim().isEmpty()) return new String[]{""};
            float safeWidth = Math.max(24f, maxWidth);
            List<String> output = new ArrayList<>();
            String[] paragraphs = input.replace('\r', ' ').split("\\n", -1);
            for (String paragraph : paragraphs) {
                String trimmed = paragraph.trim();
                if (trimmed.isEmpty()) {
                    output.add("");
                    continue;
                }
                String current = "";
                for (String word : trimmed.split("\\s+")) {
                    // Break unusually long tokens (e.g. URLs or serialized weather fields) so they
                    // cannot run beyond the page edge.
                    if (paint.measureText(word) > safeWidth) {
                        if (!current.isEmpty()) { output.add(current); current = ""; }
                        String part = "";
                        for (int i = 0; i < word.length(); i++) {
                            String next = part + word.charAt(i);
                            if (!part.isEmpty() && paint.measureText(next) > safeWidth) {
                                output.add(part);
                                part = String.valueOf(word.charAt(i));
                            } else part = next;
                        }
                        current = part;
                        continue;
                    }
                    String next = current.isEmpty() ? word : current + " " + word;
                    if (!current.isEmpty() && paint.measureText(next) > safeWidth) {
                        output.add(current);
                        current = word;
                    } else current = next;
                }
                if (!current.isEmpty()) output.add(current);
            }
            return output.isEmpty() ? new String[]{""} : output.toArray(new String[0]);
        }

        private void wrap(String input, Paint paint, float lineHeight, float x) {
            if (input == null) return;
            String[] paragraphs = input.replace('\r', ' ').split("\\n", -1);
            float maxWidth = W - RIGHT - x;
            for (int paragraphIndex = 0; paragraphIndex < paragraphs.length; paragraphIndex++) {
                String paragraph = paragraphs[paragraphIndex].trim();
                if (paragraph.isEmpty()) {
                    y += Math.max(4f, lineHeight * 0.35f);
                    continue;
                }
                String[] lines = wrapLines(paragraph, paint, maxWidth);
                for (String lineText : lines) {
                    need(lineHeight);
                    c.drawText(lineText, x, y, paint);
                    y += lineHeight;
                }
                if (paragraphIndex < paragraphs.length - 1) y += 3f;
            }
        }

        void finish(File output) throws Exception {
            flushPendingSection(15f);
            if (page != null) finishCurrentPage();
            try (FileOutputStream stream = new FileOutputStream(output)) {
                doc.writeTo(stream);
            } finally {
                doc.close();
            }
        }
    }
}
