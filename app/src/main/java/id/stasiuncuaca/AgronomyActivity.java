package id.stasiuncuaca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Comprehensive agronomy dashboard and evidence-assisted AI report. */
public class AgronomyActivity extends Activity {
    private static final String PREFS="thingspeak_config";
    private static final String DEFAULT_AI_KEY="";
    private final ExecutorService net=Executors.newSingleThreadExecutor();
    private android.content.SharedPreferences prefs;
    private TextView report, aiStatus;
    private LinearLayout reportCards;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        setContentView(R.layout.activity_agronomy);
        report=findViewById(R.id.agroReport); aiStatus=findViewById(R.id.agroAiStatus);
        reportCards=findViewById(R.id.agroReportCards);
        report.setVisibility(View.GONE);
        aiStatus.setTextSize(16);
        aiStatus.setLineSpacing(dp(5), 1.08f);
        findViewById(R.id.agroBack).setOnClickListener(v->finish());
        findViewById(R.id.agroFieldNotes).setOnClickListener(v->startActivity(new Intent(this,FieldNotesActivity.class)));
        findViewById(R.id.agroPdf).setOnClickListener(v->startActivity(new Intent(this,PdfReportActivity.class)));
        findViewById(R.id.agroAiButton).setOnClickListener(v->requestAi());
        findViewById(R.id.agroFertilizerConvert).setOnClickListener(v -> FertilizerConverterDialog.show(this, prefs, this::render));
        findViewById(R.id.agroDolomiteCalculator).setOnClickListener(v -> DolomiteCalculatorDialog.show(this, prefs, this::render));
        findViewById(R.id.agroNotes).setOnClickListener(v->showNotes());
        render();
    }
    @Override protected void onDestroy(){net.shutdownNow();super.onDestroy();}

    private void render(){
        String crop=prefs.getString("crop","Tanaman pertanian");
        int hst=(int)Math.round(num(prefs.getString("farm_hst","-1")));
        AgronomyEngine.CropProfile cp=AgronomyEngine.profile(crop);
        String mode=prefs.getString("farm_cultivation_mode","Konvensional / PHT");
        String method=prefs.getString("soil_test_method","Metode tidak diketahui");
        double ph=num(prefs.getString("soil_ph","")),n=num(prefs.getString("soil_n","")),p=num(prefs.getString("soil_p","")),k=num(prefs.getString("soil_k",""));
        double nLow=num(prefs.getString("soil_n_low","")),nHigh=num(prefs.getString("soil_n_high",""));
        if (!NitrogenInference.usesMethodSpecificThresholds(prefs.getString("soil_n_classification_basis", NitrogenInference.BASIS_INFERRED))) { nLow=Double.NaN; nHigh=Double.NaN; }
        double ec=num(prefs.getString("soil_ec_us_cm","")),ece=num(prefs.getString("soil_ece_ds_m","")),moist=num(prefs.getString("soil_moisture_pct",""));
        double bd=num(prefs.getString("soil_bulk_density_g_cm3","1.30")),depth=num(prefs.getString("soil_depth_cm","20")),fc=num(prefs.getString("soil_fc_pct","")),pwp=num(prefs.getString("soil_pwp_pct",""));
        double phBuf=num(prefs.getString("soil_ph_buffer","")),al=num(prefs.getString("soil_al_dd","")),hd=num(prefs.getString("soil_h_dd","")),cec=num(prefs.getString("soil_cec","")),om=num(prefs.getString("soil_om_pct","")),lime=num(prefs.getString("soil_lime_requirement_kg_ha",""));
        double temp=num(prefs.getString("om_temp","")),rh=num(prefs.getString("om_rh","")),rain=num(prefs.getString("om_rain","")),et0=num(prefs.getString("om_et0","")),vpd=num(prefs.getString("om_vpd",""));
        double apparent=num(prefs.getString("om_apparent_temp","")),dew=num(prefs.getString("om_dewpoint","")),pressure=num(prefs.getString("om_pressure",""));
        double wind=num(prefs.getString("om_wind_speed","")),gust=num(prefs.getString("om_wind_gust","")),cloud=num(prefs.getString("om_cloud_cover",""));
        double uv=num(prefs.getString("om_uv","")),visibility=num(prefs.getString("om_visibility","")),shortwave=num(prefs.getString("om_radiation",""));
        double parEnergy=num(prefs.getString("om_par","")),ppfd=num(prefs.getString("om_ppfd","")),sunshine=num(prefs.getString("om_sun_hours",""));
        StringBuilder s=new StringBuilder();
        s.append("ANALISIS AGRONOMI TERPADU v1.5.7\n\n");
        if (crop.toLowerCase(Locale.US).contains("sawit") || crop.toLowerCase(Locale.US).contains("oil palm")) {
            report.setText("Kelapa sawit tidak termasuk komoditas STASIUN CUACA v1.5.7. Pilih komoditas tanaman lain pada Catatan Lapangan.");
            report.setVisibility(View.VISIBLE); reportCards.setVisibility(View.GONE); return;
        }
        String areaM2 = pref("farm_area_m2", "");
        if (areaM2.isEmpty()) { double ha=num(pref("farm_area_ha", "")); areaM2=Double.isFinite(ha)?show(ha*10000):"--"; }
        String yieldKg = pref("farm_target_yield_kg", "");
        s.append("KOMODITAS: ").append(crop).append("\n");
        String variety = prefs.getString("farm_variety", "").trim();
        if (!variety.isEmpty()) s.append("VARIETAS/GALUR/KLON: ").append(variety).append("\n");
        String manualPhase = prefs.getString("farm_phase_manual", "").trim();
        String effectivePhase = manualPhase.isEmpty() ? AgronomyEngine.phase(hst,crop) : manualPhase + " (manual)";
        s.append("MODE: ").append(mode).append("\nHST: ").append(hst<0?"--":hst).append("\nFASE: ").append(effectivePhase).append("\nLUAS LAHAN: ").append(areaM2).append(" m²\nTARGET PRODUKSI TOTAL: ").append(yieldKg.isEmpty()?"opsional / belum diisi":yieldKg+" kg").append("\n\n");
        s.append("1. PARAMETER TANAH\n");
        s.append("Rentang pH profil komoditas: ").append(show(cp.phMin)).append("–").append(show(cp.phMax)).append("; hasil: ").append(AgronomyEngine.classifyPH(ph,cp)).append("\n");
        s.append("pH tanah ").append(show(ph)).append(" -> ").append(AgronomyEngine.classifyPH(ph,cp)).append("\n");
        s.append("Basis klasifikasi N: ").append(prefs.getString("soil_n_classification_basis", NitrogenInference.BASIS_INFERRED)).append("\n");
        appendDolomiteCalculation(s, crop);
        s.append("Metode N/P/K: ").append(method).append("\n");
        s.append("N tersedia: ").append(show(n)).append(" mg/kg\n");
        if (AgronomyEngine.hasValidNThresholds(nLow,nHigh)) {
            s.append("Kelas N tersedia berdasarkan ambang metode yang diisi: ").append(AgronomyEngine.classifyN(n,nLow,nHigh)).append("\n");
        } else {
            s.append("Kelas N dari N-total estimasi (asumsi utama 2%): ").append(NitrogenInference.classifyEstimatedTotalN(n)).append("\n");
        }
        s.append("N-total estimasi: ").append(NitrogenInference.estimateTotalN(n)).append("\n");
        s.append("Rentang kelas pada sensitivitas asumsi rasio 1–3%: ").append(NitrogenInference.classifyRange(n)).append("\n");
        s.append("Rentang N-total screening untuk klasifikasi: sangat rendah <0,10%; rendah 0,10–0,20%; sedang >0,20–0,50%; tinggi >0,50–0,75%; sangat tinggi >0,75%.\n");
        s.append("P tersedia ").append(show(p)).append(" mg/kg -> ").append(AgronomyEngine.classifyP(p,method)).append("; kisaran screening profil ").append(show(cp.pMin)).append("–").append(show(cp.pMax)).append(" mg/kg.\n");
        s.append("K tersedia ").append(show(k)).append(" mg/kg -> ").append(AgronomyEngine.classifyK(k,method)).append("; kisaran screening profil ").append(show(cp.kMin)).append("–").append(show(cp.kMax)).append(" mg/kg.\n");
        s.append("EC sensor ").append(show(ec)).append(" µS/cm -> ").append(AgronomyEngine.classifyEC(ec,cp)).append("; ambang profil ").append(show(cp.ecThresholdDsM)).append(" dS/m (EC µS/cm ÷ 1000 hanya konversi unit, bukan ECe lab).\n");
        if(Double.isFinite(ece))s.append("ECe lab ").append(show(ece)).append(" dS/m -> ").append(AgronomyEngine.classifyECe(ece)).append("\n");
        else s.append("ECe lab: belum tersedia; EC sensor tidak dikonversi otomatis menjadi ECe.\n");
        s.append("Kelembapan tanah ").append(show(moist)).append(" % -> ").append(AgronomyEngine.classifyMoisture(moist)).append("\n");
        s.append("FC/PWP: ").append(show(fc)).append(" / ").append(show(pwp)).append(" % volume -> ").append(AgronomyEngine.soilWaterAssessment(moist,fc,pwp,depth,et0,crop)).append("\n");
        if(Double.isFinite(depth)&&Double.isFinite(bd)){
            s.append("Stok teoritis lapisan ").append(show(depth)).append(" cm; BD ").append(show(bd)).append(" g/cm³: N-tersedia input ").append(show(AgronomyEngine.soilStockKgHa(n,bd,depth))).append("; P input ").append(show(AgronomyEngine.soilStockKgHa(p,bd,depth))).append("; K input ").append(show(AgronomyEngine.soilStockKgHa(k,bd,depth))).append(" kg/ha.\n");
            s.append("Ini konversi konsentrasi input menjadi stok lapisan teoritis, bukan stok N-total terukur, serapan tanaman, atau kredit pupuk langsung.\n");
        }
        s.append("\n2. PARAMETER CUACA (TERMASUK AIR DAN EKOLOGI)\n");
        String weatherProvider = pref("macro_weather_provider_used", pref("macro_weather_provider", "Open-Meteo"));
        s.append("Sumber cuaca makro: ").append(weatherProvider).append(". Data historis/reanalisis dan nilai model bukan rekaman sensor langsung.\n");
        s.append("Suhu ").append(show(temp)).append(" °C -> ").append(AgronomyEngine.temperatureStatus(temp,crop)).append("; kisaran profil ").append(show(cp.tempMin)).append("–").append(show(cp.tempMax)).append(" °C.\n");
        double selectedTbase = resolveTBase(crop, cp.tBase);
        String tbaseBasis = CropDatabase.tBaseSourceLabel(crop, selectedTbase);
        s.append("Tbase/GDD: ").append(show(selectedTbase)).append(" °C • ").append(tbaseBasis).append("\n");
        s.append("RH ").append(show(rh)).append(" % • hujan ").append(show(rain)).append(" mm/hari • ET₀ ").append(show(et0)).append(" mm/hari (").append(pref("om_et0_method", "metode belum tercatat")).append(")\n");
        s.append("VPD ").append(show(vpd)).append(" kPa -> ").append(AgronomyEngine.classifyVpd(vpd)).append(". ").append(AgronomyEngine.vpdCombinedStatus(vpd,moist,fc,pwp,depth,et0,crop)).append("\n");
        s.append("Angin ").append(pref("om_wind_direction","--")).append(" • ").append(pref("om_wind_speed","--")).append(" m/s • gust ").append(pref("om_wind_gust","--")).append(" m/s\n");
        s.append("Terasa ").append(pref("om_apparent_temp","--")).append(" °C • titik embun ").append(pref("om_dewpoint","--")).append(" °C • awan ").append(pref("om_cloud_cover","--")).append(" % • visibilitas ").append(pref("om_visibility","--")).append(" m • UV ").append(pref("om_uv","--")).append("\n");
        double macroSoilTemp = num(pref("om_soil_temp", ""));
        s.append("Suhu tanah makro model: ").append(show(macroSoilTemp)).append(" °C bila tersedia; tidak mengganti input/sensor tanah.\n");
        double macroSoilMoistureFraction = num(pref("om_soil_moisture", ""));
        double macroSoilMoisturePct = Double.isFinite(macroSoilMoistureFraction) && macroSoilMoistureFraction >= 0 && macroSoilMoistureFraction <= 1.0 ? macroSoilMoistureFraction * 100.0 : macroSoilMoistureFraction;
        s.append("Kelembapan tanah makro model: ").append(show(macroSoilMoisturePct)).append(" %vol • ").append(pref("om_soil_moisture_method", "tidak tersedia")).append(". Berbeda dari sensor tanah petak.\n");
        s.append("Radiasi sesaat: ").append(show(shortwave)).append(" W/m²; radiasi harian: ").append(pref("om_radiation_daily", "--")).append(" MJ/m²/hari • ").append(pref("om_radiation_method", "metode belum tercatat")).append("\n");
        s.append("Penyinaran: ").append(show(sunshine)).append(" jam/hari • daylight ").append(pref("om_daylight_hours", "--")).append(" jam/hari • ").append(pref("om_sunshine_method", "metode belum tercatat")).append("\n");
        s.append("PAR: ").append(show(parEnergy)).append(" W/m² • PPFD: ").append(show(ppfd)).append(" µmol/m²/s • lux: ").append(pref("om_lux", pref("lux", "--"))).append(" (lux estimasi dari radiasi; bukan sensor illuminance).\n");
        if (Double.isFinite(rain) && Double.isFinite(et0)) {
            if (rain < et0) s.append("Dampak budidaya: hujan < ET₀; periksa kelembapan zona akar dan jadwalkan irigasi berdasarkan kondisi lahan.\n");
            else if (rain > Math.max(10.0, et0 * 2.0)) s.append("Dampak budidaya: hujan sangat melebihi ET₀; jaga drainase dan tunda pemupukan bila tanah jenuh atau hujan deras masih diperkirakan.\n");
            else s.append("Dampak budidaya: bandingkan hujan dan ET₀ dengan kelembapan tanah sebelum menetapkan irigasi.\n");
        }
        if (Double.isFinite(sunshine) && sunshine < 3) s.append("Penyinaran pendek dapat memperlambat pengeringan kanopi; atur jarak tanaman dan pekerjaan kebun saat daun kering.\n");
        if (Double.isFinite(wind) && wind >= 5) s.append("Angin kuat: perhatikan drift penyemprotan, rebah, dan keseragaman irigasi.\n");
        s.append("Prakiraan 7 hari: ").append(pref("om_forecast_7d","belum ada")).append("\n");
        s.append("\n3. PEMUPUKAN NPK & PREDIKSI KEBUTUHAN FASE\n");
        double target=num(pref("farm_target_yield_t_ha",""));
        double[] credit=fertCredit(crop);
        double nn=AgronomyEngine.nutrientNeed(n,"N",crop,hst,credit[0],target,method,depth,bd,nLow,nHigh,manualPhase);
        double pp=AgronomyEngine.nutrientNeed(p,"P",crop,hst,credit[1],target,method,depth,bd,nLow,nHigh,manualPhase);
        double kk=AgronomyEngine.nutrientNeed(k,"K",crop,hst,credit[2],target,method,depth,bd,nLow,nHigh,manualPhase);
        prefs.edit().putString("last_need_n_kg_ha", Double.isFinite(nn)?String.format(Locale.US,"%.3f",nn):"")
                .putString("last_need_p2o5_kg_ha", Double.isFinite(pp)?String.format(Locale.US,"%.3f",pp):"")
                .putString("last_need_k2o_kg_ha", Double.isFinite(kk)?String.format(Locale.US,"%.3f",kk):"").apply();
        s.append("N skrining fase: ").append(show(nn)).append(" kg N/ha\nP2O5 skrining fase: ").append(show(pp)).append(" kg P2O5/ha\nK2O skrining fase: ").append(show(kk)).append(" kg K2O/ha\n");
        s.append("Rumus skrining: ").append(AgronomyEngine.nutrientFormulaText()).append("\n");
        s.append("INDIKATOR & BATAS: kelas P/K merupakan screening sesuai pilihan metode. Untuk N, ambang rendah/tinggi khusus metode yang valid dipakai bila tersedia; jika kosong atau generik, kelas N-total ditaksir dari N tersedia dengan asumsi utama 2% (sensitivitas 1–3%) sehingga dukungan N tanah tidak otomatis dianggap nol. Jika N tersedia tidak ada, dukungan tanah N belum dapat diperkirakan. Angka pupuk adalah estimasi awal, bukan rekomendasi laboratorium atau STCR/PUTS terkalibrasi.\n");
        if ("Komoditas umum".equals(cp.name)) s.append("PERHATIAN: komoditas belum memiliki profil spesifik di aplikasi; kebutuhan musim memakai baseline umum dan ketidakpastian tinggi. Verifikasi dengan rekomendasi penyuluh/laboratorium setempat.\n");
        s.append("PENTING: bukan QUEFTS/STCR penuh. Gunakan persamaan STCR/PUTS lokal bila tersedia.\n");
        appendFertilizerProductsAndUsage(s, crop, nn, pp, kk, areaM2);
        s.append("\n4. RISK SCREENING PREDIKSI POTENSI OPT\n");
        s.append(AgronomyEngine.optRisk(crop,temp,rh,rain,num(pref("om_wind_speed","")),recent("opt_history",6),hst)).append("\n");
        s.append("\n5. DATABASE & REKOMENDASI TEKNIS\n");
        addRecommendations(s,crop,mode,ph,ec,ece,moist,fc,pwp,vpd,temp,rh,rain,et0,hst,method);
        s.append("\n6. CATATAN\n").append(AgronomyEngine.evidenceBrief()).append("\n");
        // Histori terpadu disimpan untuk laporan cetak/PDF, tetapi tidak ditampilkan pada layar analisis utama.
        String compact = compactReport(s.toString());
        report.setText(compact);
        renderAgronomyReportCards(compact);
        prefs.edit().putString("last_agronomy_analysis",compact).apply();
    }

    /** Show computed lime details explicitly; these are estimates, not a laboratory lime requirement. */
    private void appendDolomiteCalculation(StringBuilder s, String currentCrop) {
        if (!prefs.getBoolean("limecalc_has_result", false)) {
            s.append("KALKULATOR DOLOMIT: belum ada hasil perhitungan tersimpan. Buka Kalkulator Dolomit untuk menghitung estimasi dan simpan hasilnya.\n");
            return;
        }
        String calculatedCrop = prefs.getString("limecalc_crop", "");
        s.append("HASIL PERHITUNGAN DOLOMIT (estimasi pH-only, bukan hasil uji laboratorium)\n");
        s.append("Komoditas kalkulator: ").append(calculatedCrop.isEmpty() ? "tidak tercatat" : calculatedCrop);
        if (!calculatedCrop.isEmpty() && !same(calculatedCrop, currentCrop))
            s.append(" — PERHATIAN: hasil ini dihitung untuk komoditas berbeda dari pilihan saat ini.");
        s.append("\n");
        s.append("pH awal → target: ").append(show(num(prefs.getString("limecalc_current_ph", ""))))
                .append(" → ").append(show(num(prefs.getString("limecalc_target_ph", ""))))
                .append("; ΔpH ").append(show(num(prefs.getString("limecalc_delta_ph", "")))).append(".\n");
        s.append("Tekstur tanah: ").append(prefs.getString("limecalc_texture", "tidak tercatat"))
                .append("; faktor CaCO₃-ekuivalen: ").append(show(num(prefs.getString("limecalc_rate_t_ha_per_ph", ""))))
                .append(" t/ha/unit pH; efektivitas netralisasi dolomit: ")
                .append(show(num(prefs.getString("limecalc_neutralizing_pct", "")))).append("%.\n");
        s.append("Kebutuhan CaCO₃-ekuivalen estimasi: ").append(show(num(prefs.getString("limecalc_caco3_equivalent_t_ha", ""))))
                .append(" t/ha. Dosis dolomit hasil hitung: ").append(show(num(prefs.getString("limecalc_dolomite_t_ha", ""))))
                .append(" t/ha = ").append(show(num(prefs.getString("limecalc_dolomite_kg_ha", "")))).append(" kg/ha.\n");
        s.append("Luas yang digunakan kalkulator: ").append(show(num(prefs.getString("limecalc_area_ha", ""))))
                .append(" ha; total dolomit untuk luas tersebut: ")
                .append(show(num(prefs.getString("limecalc_total_kg", "")))).append(" kg.\n");
        s.append("Ringkasan kalkulator: ").append(prefs.getString("limecalc_summary", "Tidak ada ringkasan."))
                .append("\nCatatan: estimasi pH-only; validasi dosis dengan uji buffer/Al-dd/H-dd/CEC dan kebutuhan Ca/Mg sebelum aplikasi.\n");
    }

    /** Distinguish mathematically converted product doses from fertilizer actually recorded as applied. */
    private void appendFertilizerProductsAndUsage(StringBuilder s, String crop, double nNeed, double pNeed, double kNeed, String areaText) {
        s.append("\nHASIL KONVERSI KEBUTUHAN HARA KE PRODUK PUPUK\n");
        JSONArray items;
        try { items = new JSONArray(prefs.getString("fert_conv_result_items", "[]")); }
        catch (Exception badJson) { items = new JSONArray(); }
        String savedCrop = prefs.getString("fert_conv_result_crop", "");
        double savedN = num(prefs.getString("fert_conv_result_target_n", ""));
        double savedP = num(prefs.getString("fert_conv_result_target_p2o5", ""));
        double savedK = num(prefs.getString("fert_conv_result_target_k2o", ""));
        double savedArea = num(prefs.getString("fert_conv_result_area_m2", ""));
        double currentArea = num(areaText);
        boolean sameNeed = near(savedN, nNeed) && near(savedP, pNeed) && near(savedK, kNeed);
        boolean sameArea = (!Double.isFinite(savedArea) && !Double.isFinite(currentArea))
                || (Double.isFinite(savedArea) && Double.isFinite(currentArea) && Math.abs(savedArea - currentArea) <= 1.0);
        boolean sameCrop = !savedCrop.isEmpty() && same(savedCrop, crop);
        if (items.length() == 0) {
            String oldSummary = prefs.getString("fert_conv_result_summary", "");
            if (prefs.getLong("fert_conv_result_created", 0L) > 0L && !oldSummary.trim().isEmpty()) {
                s.append("Hasil konversi tersimpan, tetapi tidak ada dosis produk positif yang dihasilkan oleh kombinasi saat itu. Ringkasan: ")
                        .append(oldSummary.replace("\n", " • ")).append("\n");
            } else {
                s.append("Belum ada hasil konversi dosis produk yang tersimpan. Buka tombol Konversi Dosis Pupuk, hitung dengan pupuk yang tersedia, lalu simpan hasil melalui tombol Hitung Konversi.\n");
            }
        } else {
            long calculatedAt = prefs.getLong("fert_conv_result_created", 0L);
            s.append("Komoditas saat konversi: ").append(savedCrop.isEmpty() ? "tidak tercatat" : savedCrop).append("; ");
            if (calculatedAt > 0L) {
                try { s.append("waktu hitung ").append(new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new java.util.Date(calculatedAt))).append(". "); }
                catch (Exception ignored) { }
            }
            if (sameCrop && sameNeed && sameArea) {
                s.append("STATUS: sesuai dengan komoditas, kebutuhan N/P₂O₅/K₂O dan luas saat ini.\n");
            } else {
                s.append("STATUS: HASIL KONVERSI TERAKHIR — tidak sepenuhnya cocok dengan kebutuhan/luas/komoditas saat ini; hitung ulang sebelum digunakan.\n");
                s.append("Target saat konversi: N ").append(show(savedN)).append("; P₂O₅ ").append(show(savedP)).append("; K₂O ").append(show(savedK)).append(" kg/ha.\n");
            }
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                s.append("• ").append(item.optString("product", "Pupuk"))
                        .append(": ").append(show(item.optDouble("doseKgHa", Double.NaN))).append(" kg/ha");
                if (!item.isNull("totalKg") && Double.isFinite(item.optDouble("totalKg", Double.NaN)))
                    s.append("; total hitung ").append(show(item.optDouble("totalKg", Double.NaN))).append(" kg pada luas saat konversi");
                s.append("; kadar label N/P₂O₅/K₂O ")
                        .append(show(item.optDouble("nPct", Double.NaN))).append("/")
                        .append(show(item.optDouble("pPct", Double.NaN))).append("/")
                        .append(show(item.optDouble("kPct", Double.NaN))).append("%.\n");
            }
            s.append("Hara tersuplai dibanding target (kg/ha): N ")
                    .append(show(num(prefs.getString("fert_conv_result_delivered_n", "")))).append("/").append(show(savedN))
                    .append("; P₂O₅ ").append(show(num(prefs.getString("fert_conv_result_delivered_p2o5", "")))).append("/").append(show(savedP))
                    .append("; K₂O ").append(show(num(prefs.getString("fert_conv_result_delivered_k2o", "")))).append("/").append(show(savedK)).append(".\n");
        }
        s.append("Catatan penting: hasil konversi adalah dosis matematis berdasarkan kadar label produk dan bukan bukti bahwa pupuk benar-benar sudah diaplikasikan. Catat pemakaian aktual pada Riwayat Pemupukan.\n");

        s.append("\nPUPUK YANG DICATAT SUDAH DIGUNAKAN (RIWAYAT PEMUPUKAN)\n");
        int count = 0;
        try {
            JSONArray history = new JSONArray(prefs.getString("fert_history", "[]"));
            for (int i = history.length() - 1; i >= 0 && count < 6; i--) {
                JSONObject o = history.optJSONObject(i);
                if (o == null || !same(o.optString("crop", ""), crop)) continue;
                if (count > 0) s.append("\n");
                s.append("• ").append(o.optString("date", "tanggal tidak tercatat"))
                        .append(" — ").append(o.optString("product", "Pupuk"))
                        .append(" (kategori ").append(o.optString("category", "tidak tercatat"))
                        .append("): ").append(show(o.optDouble("dose", Double.NaN))).append(" ").append(o.optString("unit", ""));
                String nPct = o.optString("nPct", ""), pPct = o.optString("pPct", ""), kPct = o.optString("kPct", "");
                if (!nPct.isEmpty() || !pPct.isEmpty() || !kPct.isEmpty())
                    s.append("; kadar N/P₂O₅/K₂O ").append(nPct.isEmpty()?"--":nPct).append("/").append(pPct.isEmpty()?"--":pPct).append("/").append(kPct.isEmpty()?"--":kPct).append("%");
                if (!o.optString("method", "").trim().isEmpty()) s.append("; cara aplikasi: ").append(o.optString("method", ""));
                if (!o.optString("stage", "").trim().isEmpty()) s.append("; fase: ").append(o.optString("stage", ""));
                if (!o.optString("note", "").trim().isEmpty()) s.append("; catatan: ").append(o.optString("note", ""));
                count++;
            }
        } catch (Exception ignored) { }
        if (count == 0) s.append("Belum ada riwayat penggunaan pupuk untuk komoditas ini. Dosis hasil konversi di atas tidak otomatis tercatat sebagai pemakaian aktual.\n");
        else s.append("\nRiwayat menampilkan maksimal 6 catatan terbaru untuk komoditas yang dipilih.\n");
    }

    private boolean near(double a, double b) {
        return Double.isFinite(a) && Double.isFinite(b) && Math.abs(a - b) <= 0.10;
    }

    private double resolveTBase(String crop, double profileTbase) {
        String savedCrop = pref("farm_tbase_crop", "");
        String savedText = pref("farm_tbase_c", "");
        if (!savedText.trim().isEmpty() && savedCrop.equals(CropDatabase.key(crop))) {
            try {
                double value = Double.parseDouble(savedText.trim().replace(',', '.'));
                if (Double.isFinite(value) && value >= -20 && value <= 60) return value;
            } catch (NumberFormatException ignored) { }
        }
        double databaseValue = CropDatabase.tBase(crop);
        if (Double.isFinite(databaseValue)) return databaseValue;
        // Do not invent a generic Tbase for free-text crops with no supported database value.
        return CropDatabase.isKnown(crop) && Double.isFinite(profileTbase) ? profileTbase : Double.NaN;
    }

    private String compactReport(String text){
        String[] lines=text.split("\\n"); StringBuilder out=new StringBuilder(); boolean skip=false;
        for(String line:lines){String t=line.trim();
            if(t.startsWith("7. CATATAN ILMIAH")){skip=true;continue;}
            if(skip){
                if(t.startsWith("8. HISTORI TERPADU")) skip=false;
                else continue;
            }
            if(t.startsWith("URUTAN ANALISIS:" ) || t.startsWith("Rumus skrining:" ) || t.startsWith("PENTING:" ) || t.startsWith("Stok = konsentrasi")) continue;
            if(t.startsWith("SELESAI ANALISIS AWAL")) continue;
            if(t.startsWith("Catatan: parameter yang tidak memiliki ambang universal")) continue;
            if(t.contains("tidak dikonversi otomatis") && t.startsWith("ECe lab:")) continue;
            out.append(line).append("\n");
        }
        return out.toString().trim();
    }

    private void showNotes(){
        ScrollView root=new ScrollView(this); int pad=(int)(16*getResources().getDisplayMetrics().density); root.setPadding(pad,pad,pad,pad);
        LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        TextView tv=new TextView(this); tv.setTextColor(getResources().getColor(R.color.text_main)); tv.setTextSize(14);
        tv.setText("CATATAN • METODE & SUMBER\n\n"+
                "ALUR ANALISIS\n"+
                "1) Baca data terukur dari sensor/ThingSpeak/manual dan cuaca makro dari provider terpilih (Open-Meteo, OpenWeather, atau WeatherAPI).\n"+
                "2) Klasifikasikan tiap parameter sesuai rentang/ambang yang relevan.\n"+
                "3) Hubungkan parameter dan tentukan faktor pembatas utama.\n"+
                "4) Baru susun rekomendasi teknis berdasarkan bukti dan fase tanaman.\n\n"+
                "STOK HARA TANAH\n"+
                "Stok unsur pada lapisan tanah dihitung sebagai pendekatan massa tanah: konsentrasi input (mg/kg) × bulk density (g/cm³) × kedalaman (cm) × 0,10 = kg/ha. Bila input N adalah N tersedia, hasilnya hanya stok teoritis berdasarkan N tersedia, bukan N-total terukur, bukan serapan, dan tidak dipakai sebagai kredit pupuk.\n\n"+
                "KEBUTUHAN HARA / SKRINING\n"+
                "Rumus saat ini: baseline dosis hara pupuk musim × fraksi fase × skala target hasil × (1 − dukungan kelas tanah), lalu dikurangi kredit konservatif dari pupuk terdahulu × fraksi ketersediaan asumsi. Baseline profil sudah berupa dosis hara pupuk kg/ha sehingga tidak dibagi lagi dengan efisiensi pemulihan; itu akan menggandakan/menaikkan dosis secara tidak tepat. Faktor dukungan dan fraksi kredit tetap asumsi screening, bukan model QUEFTS/STCR terkalibrasi lokal. Gunakan rekomendasi PUTS/STCR spesifik lokasi bila tersedia.\n\n"+
                "pH & KAPUR\n"+
                "Kalkulator dolomit memakai pendekatan asumsi yang bisa diedit: ΔpH × faktor CaCO₃-ekuivalen (t/ha/unit pH), dibagi efektivitas netralisasi produk, lalu dikalikan luas lahan untuk total kg. Faktor bawaan: mineral ringan 1,0; mineral sedang 2,0; liat/berat 4,0; gambut 5,0 t/ha/unit pH. Ini estimasi kasar, bukan dosis laboratorium; pH-buffer, Al-dd/H-dd, CEC/KTK, bahan organik dan kedalaman olah lebih tepat untuk menetapkan kebutuhan kapur.\n\n"+
                "EC vs ECe\n"+
                "EC sensor adalah parameter lapangan dalam µS/cm; hubungan satuan: µS/cm ÷ 1000 = dS/m. ECe laboratorium tidak boleh dipaksakan dari satu faktor konversi universal karena bergantung metode, ekstraksi, kadar air, suhu, tekstur dan kondisi tanah.\n\n"+
                "AIR TANAH\n"+
                "Open-Meteo soil moisture dinyatakan sebagai m³/m³ dan untuk tampilan persentase volume dikalikan 100. FC/PWP digunakan bila tersedia untuk menilai FTSW/air tersedia.\n\n"+
                "VPD & ET₀\n"+
                "VPD dibaca bersama suhu/RH dan kondisi air tanah. ET₀ Open-Meteo dapat berasal dari produk FAO-56 Penman-Monteith; pada OpenWeather/WeatherAPI ET₀ dapat merupakan estimasi FAO-56 PM berbasis input/radiasi model kecuali API/paket menyediakannya langsung. ET₀ adalah kebutuhan referensi, bukan kebutuhan irigasi komoditas secara langsung.\n\n"+
                "PAR / PPFD\n"+
                "PAR estimasi = 0,45 × shortwave; PPFD estimasi = PAR × 4,57 µmol/J. Ini estimasi broadband, bukan pengukuran quantum sensor.\n\n"+
                "OPT\n"+
                "Daftar OPT di mesin lokal hanya screening. AI dapat mencari OPT tambahan berdasarkan komoditas, fase, lokasi, gejala, riwayat, dan cuaca; hasil tetap merupakan risk screening, bukan diagnosis pasti.\n\n"+
                "MODE ORGANIK\n"+
                "Rekomendasi harus menggunakan input/proses yang diizinkan sistem organik yang berlaku dan tidak boleh menganggap semua input kimia setara dengan input organik.\n\n"+
                "SUMBER UTAMA\n"+
                "• Open-Meteo Historical Weather API — data historis reanalysis, variabel cuaca, tanah, ET₀, VPD dan radiasi.\n"+
                "• OpenWeather One Call 3.0 — cuaca terkini/prakiraan; hasil estimasi turunan diberi label.\n"+
                "• WeatherAPI Forecast/History API — cakupan dan parameter historis bergantung paket akun; ET₀/radiasi ditandai langsung atau estimasi sesuai respons.\n"+
                "• OpenAlex — pencarian literatur ilmiah.\n"+
                "• AgronomyEngine / NitrogenInference — model skrining lokal aplikasi.\n"+
                "• Referensi EC/ECe: Pedosphere 32(6), 2022, DOI 10.1016/j.pedsph.2022.06.023; Journal of the Saudi Society of Agricultural Sciences 23(4), 2024, DOI 10.1016/j.jssas.2023.12.005.\n"+
                "• Rujukan PPFD: Thimijan & Heins (1983), HortScience 18(6):818–822, DOI 10.21273/HORTSCI.18.6.818.");
        content.addView(tv,new LinearLayout.LayoutParams(-1,-2)); root.addView(content,new ScrollView.LayoutParams(-1,-2)); new AlertDialog.Builder(this).setTitle("CATATAN").setView(root).setPositiveButton("TUTUP",null).show();
    }

    /** Present the same results in clear cards; show actions first and keep technical details later. */
    private void renderAgronomyReportCards(String raw) {
        if (reportCards == null || raw == null) {
            if (report != null) report.setVisibility(View.VISIBLE);
            return;
        }
        reportCards.removeAllViews();
        reportCards.setVisibility(View.VISIBLE);
        String[] lines = raw.split("\\r?\\n");
        String titleText = lines.length > 0 ? lines[0].trim() : "HASIL ANALISIS KEBUN";
        TextView title = new TextView(this);
        title.setText(titleText.replace("ANALISIS AGRONOMI TERPADU", "HASIL ANALISIS KEBUN").replace(" v1.5.7", ""));
        title.setTextColor(0xFF29C6C7);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLineSpacing(dp(3), 1.06f);
        title.setPadding(dp(2), dp(3), dp(2), dp(5));
        reportCards.addView(title, fullWidthWrap());

        StringBuilder metadata = new StringBuilder();
        int i = 1;
        for (; i < lines.length; i++) {
            String line = lines[i].trim();
            if (isAgronomyHeading(line)) break;
            if (line.isEmpty()) continue;
            metadata.append(friendlyAgronomyLine(line)).append('\n');
        }
        if (metadata.length() > 0) addAgronomyCard("RINGKASAN KEBUN", metadata.toString().trim(), false, false);
        addAgronomyNote("Baca dahulu bagian saran tindakan. Angka dari input atau sensor bukan otomatis hasil laboratorium; kata ‘perkiraan’ berarti hasil hitungan model.");

        java.util.LinkedHashMap<String, ArrayList<String>> sections = new java.util.LinkedHashMap<>();
        String heading = null;
        ArrayList<String> body = new ArrayList<>();
        for (; i < lines.length; i++) {
            String line = lines[i].trim();
            if (isAgronomyHeading(line)) {
                if (heading != null) sections.put(heading, body);
                heading = line;
                body = new ArrayList<>();
            } else if (!line.isEmpty()) body.add(friendlyAgronomyLine(line));
        }
        if (heading != null) sections.put(heading, body);
        ArrayList<String> ordered = new ArrayList<>(sections.keySet());
        java.util.Collections.sort(ordered, (a, b) -> Integer.compare(agronomySectionOrder(a), agronomySectionOrder(b)));
        for (String h : ordered) {
            String label = friendlyAgronomyHeading(h);
            addAgronomyCard(label, joinFriendlyLines(sections.get(h)), h.toUpperCase(Locale.ROOT).contains("REKOMENDASI"), h.toUpperCase(Locale.ROOT).contains("CATATAN"));
        }
    }

    private int agronomySectionOrder(String heading) {
        String h = heading.toUpperCase(Locale.ROOT);
        if (h.contains("PARAMETER TANAH")) return 10;
        if (h.contains("PARAMETER CUACA")) return 20;
        if (h.contains("PEMUPUKAN NPK")) return 30;
        if (h.contains("RISK SCREENING PREDIKSI POTENSI OPT")) return 40;
        if (h.contains("DATABASE & REKOMENDASI TEKNIS")) return 50;
        if (h.contains("CATATAN")) return 60;
        return 70;
    }

    private boolean isAgronomyHeading(String line) {
        return line != null && line.matches("^[1-9]\\. .+");
    }

    private String friendlyAgronomyHeading(String input) {
        String value = input.replaceFirst("^[0-9]+\\.\\s*", "").trim();
        if (value.contains("PARAMETER TANAH")) return "1. PARAMETER TANAH";
        if (value.contains("PARAMETER CUACA")) return "2. PARAMETER CUACA (TERMASUK AIR DAN EKOLOGI)";
        if (value.contains("PEMUPUKAN NPK")) return "3. PEMUPUKAN NPK & PREDIKSI KEBUTUHAN FASE";
        if (value.contains("RISK SCREENING PREDIKSI POTENSI OPT")) return "4. RISK SCREENING PREDIKSI POTENSI OPT";
        if (value.contains("DATABASE & REKOMENDASI TEKNIS")) return "5. DATABASE & REKOMENDASI TEKNIS";
        if (value.equalsIgnoreCase("CATATAN") || value.contains("CATATAN")) return "6. CATATAN";
        return value;
    }

    private String friendlyAgronomyLine(String input) {
        if (input == null) return "";
        String value = input.trim();
        String[][] replacements = {
            {"KOMODITAS:", "Tanaman:"}, {"MODE:", "Cara budidaya:"}, {"HST:", "Umur setelah tanam (hari):"},
            {"FASE:", "Tahap tanaman:"}, {"LUAS:", "Luas kebun:"}, {"TARGET HASIL:", "Target hasil:"},
            {"pH ", "pH tanah "}, {"Metode N/P/K:", "Metode pemeriksaan N/P/K:"},
            {"N tersedia ", "Nitrogen (N) tersedia "}, {"P tersedia ", "Fosfor (P) tersedia "}, {"K tersedia ", "Kalium (K) tersedia "},
            {"N-total diprediksi dari N tersedia:", "Perkiraan N total dari N tersedia (bukan hasil laboratorium):"},
            {"Kelas N-total yang mungkin:", "Perkiraan kelompok N total:"}, {"EC sensor ", "EC dari sensor tanah "},
            {"ECe lab", "ECe hasil laboratorium"}, {"Kelembapan tanah", "Kadar air tanah"},
            {"Stok lapisan", "Perkiraan hara di lapisan tanah"}, {"stok ≠ serapan langsung tanaman", "jumlah hara dalam tanah tidak sama dengan jumlah yang langsung diserap tanaman"},
            {"N skrining fase:", "Perkiraan kebutuhan nitrogen (N) pada tahap ini:"},
            {"P2O5 skrining fase:", "Perkiraan kebutuhan fosfor (P₂O₅) pada tahap ini:"},
            {"K2O skrining fase:", "Perkiraan kebutuhan kalium (K₂O) pada tahap ini:"},
            {"Rumus skrining:", "Cara hitung perkiraan (catatan teknis):"}, {"PENTING: bukan QUEFTS/STCR penuh.", "Penting: ini perkiraan awal, bukan hasil model lokal yang sudah dikalibrasi."},
            {"RH ", "Kelembapan udara (RH) "}, {"VPD ", "Tekanan udara kering bagi tanaman (VPD) "},
            {"Forecast 7 hari:", "Prakiraan cuaca 7 hari:"}, {"URUTAN ANALISIS:", "Cara membaca hasil:"},
            {"OPT — RISK SCREENING", "hama/penyakit — perkiraan risiko awal"}, {"screening", "perkiraan awal"},
            {"PHT", "pengendalian hama terpadu"}, {"kg/ha", "kg/ha (kilogram per hektare)"},
            {"mg/kg", "mg/kg"}, {"• ", "• "}
        };
        for (String[] pair : replacements) value = value.replace(pair[0], pair[1]);
        value = value.replace("; ", ";\n• ");
        return value;
    }

    private String joinFriendlyLines(ArrayList<String> lines) {
        StringBuilder out = new StringBuilder();
        if (lines != null) for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            if (out.length() > 0) out.append('\n');
            out.append(line.trim());
        }
        return out.toString();
    }

    private void addAgronomyCard(String heading, String body, boolean priority, boolean technical) {
        int accent = priority ? 0xFFF3B64A : 0xFF29C6C7;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(roundBg(technical ? 0xFF102B3C : 0xFF122B3B, technical ? 0xFF244353 : 0xFF214457));
        LinearLayout.LayoutParams lp = fullWidthWrap();
        lp.topMargin = dp(9);
        reportCards.addView(card, lp);
        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextColor(accent);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLineSpacing(dp(2), 1.04f);
        title.setPadding(0, 0, 0, dp(6));
        card.addView(title, fullWidthWrap());
        if (technical) addAgronomyBody(card, "Bagian ini berisi keterangan teknis tambahan. Petani bisa mengutamakan bagian saran tindakan di atas.", false);
        if (body == null || body.trim().isEmpty()) {
            addAgronomyBody(card, "Belum ada informasi yang cukup untuk bagian ini.", false);
            return;
        }
        for (String rawLine : body.split("\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            String[] split = line.split("\\n");
            for (String piece : split) {
                String content = piece.trim();
                if (content.isEmpty()) continue;
                boolean bullet = content.startsWith("•") || content.startsWith("-");
                if (bullet) content = content.replaceFirst("^[•-]\\s*", "");
                addAgronomyBody(card, content, bullet);
            }
        }
    }

    private void addAgronomyNote(String text) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(roundBg(0xFF173546, 0xFF3E5A67));
        LinearLayout.LayoutParams lp = fullWidthWrap();
        lp.topMargin = dp(7);
        reportCards.addView(card, lp);
        TextView title = new TextView(this);
        title.setText("CATATAN PENTING");
        title.setTextColor(0xFFFFD479);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title, fullWidthWrap());
        addAgronomyBody(card, text, false);
    }

    private void addAgronomyBody(LinearLayout parent, String text, boolean bullet) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams rowLp = fullWidthWrap();
        rowLp.topMargin = dp(3);
        rowLp.bottomMargin = dp(4);
        parent.addView(row, rowLp);
        if (bullet) {
            TextView dot = new TextView(this);
            dot.setText("•"); dot.setTextColor(0xFF45D483); dot.setTextSize(17); dot.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            dot.setPadding(0, 0, dp(8), 0);
            row.addView(dot, new LinearLayout.LayoutParams(dp(18), -2));
        }
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(0xFFF0F4F6);
        textView.setTextSize(15.5f);
        textView.setLineSpacing(dp(5), 1.08f);
        row.addView(textView, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private GradientDrawable roundBg(int color, int stroke) {
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(13)); bg.setStroke(dp(1), stroke); return bg;
    }

    private LinearLayout.LayoutParams fullWidthWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private CharSequence styleAiResponse(String raw) {
        String[] lines = (raw == null ? "" : raw).replace("**", "").replace("###", "").replace("##", "").split("\\r?\\n", -1);
        SpannableString styled = new SpannableString(android.text.TextUtils.join("\n", lines));
        String all = styled.toString();
        int start = 0;
        while (start < all.length()) {
            int end = all.indexOf('\n', start); if (end < 0) end = all.length();
            String line = all.substring(start, end).trim();
            if (!line.isEmpty() && (line.matches("^[0-9]+[.)].*") || line.equals(line.toUpperCase(Locale.ROOT)) && line.length() > 5 && !line.startsWith("•"))) {
                styled.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new ForegroundColorSpan(0xFF29C6C7), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new RelativeSizeSpan(1.06f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            start = end + 1;
        }
        return styled;
    }

    private void addRecommendations(StringBuilder s,String crop,String mode,double ph,double ec,double ece,double moist,double fc,double pwp,double vpd,double t,double rh,double rain,double et0,int hst,String method){
        AgronomyEngine.CropProfile p=AgronomyEngine.profile(crop);
        if(Double.isFinite(ph)&&ph<p.phMin) s.append("• pH rendah: lakukan uji pH-buffer/Al-dd/H-dd/CEC atau gunakan kebutuhan kapur laboratorium; jangan hitung dolomit dari pH saja.\n");
        if(Double.isFinite(ph)&&ph>p.phMax) s.append("• pH tinggi: stop sementara kapur/dolomit; periksa alkalinitas air dan dasar kebutuhan pengasaman.\n");
        if(Double.isFinite(ec)&&ec/1000.0>p.ecThresholdDsM) s.append("• EC sensor tinggi: kurangi pemupukan pekat, cek air/drainase, dan pantau tren. Konfirmasi dengan ECe bila salinitas dicurigai.\n");
        if(Double.isFinite(ece)&&ece>=4) s.append("• ECe menunjukkan salinitas sedikit sampai sangat tinggi menurut kelas USDA-NRCS; pilih tindakan berdasarkan toleransi komoditas dan pemeriksaan zona akar.\n");
        String water=AgronomyEngine.soilWaterAssessment(moist,fc,pwp,20,et0,crop);
        if(water.startsWith("KURANG")) s.append("• Air tanah kurang: cek zona akar dan kebutuhan irigasi sebelum menambah pupuk larut.\n");
        if(Double.isFinite(vpd)&&vpd>2) s.append("• VPD tinggi: pantau layu/kerontokan bunga atau gejala kehilangan air; kombinasi dengan tanah kering lebih serius.\n");
        if(Double.isFinite(t)&& (t<p.tempMin||t>p.tempMax)) s.append("• Suhu di luar kisaran ekologis profil: kurangi pekerjaan stres pada tanaman dan pantau gejala.\n");
        if(Double.isFinite(rh)&&rh>90&&Double.isFinite(rain)&&rain>=5) s.append("• RH + hujan tinggi: tingkatkan scouting penyakit, sanitasi dan sirkulasi udara.\n");
        if(mode.toLowerCase(Locale.US).contains("organik")) s.append("• ORGANIK: gunakan input/proses yang diizinkan standar organik yang berlaku; prioritaskan bahan organik matang, sanitasi, varietas toleran, mekanis dan agen hayati. Verifikasi status input sebelum aplikasi.\n");
        else s.append("• KONVENSIONAL/PHT: gunakan pemupukan berimbang, monitoring ambang tindakan, rotasi bahan aktif dan patuhi label/interval pra-panen.\n");
        s.append("• Waktu pemupukan: aplikasikan saat tanah lembap tetapi tidak tergenang; hindari sesaat sebelum hujan deras atau ketika tanah sangat kering. Bagi dosis N mengikuti fase dan rekomendasi setempat; jangan menebar dosis pekat saat panas terik.\n");
        s.append("• Fase: ").append(AgronomyEngine.phase(hst,crop)).append(". Prioritaskan tindakan berdasarkan fase dan data lapang, bukan umur saja.\n");
    }

    private double[] fertCredit(String crop){
        double n=0,p=0,k=0;
        try {
            JSONArray a=new JSONArray(prefs.getString("fert_history","[]"));
            LocalDate cutoff=LocalDate.now().minusDays(90);
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i);
                if(o==null||!same(o.optString("crop",""),crop)||!"kg/ha".equalsIgnoreCase(o.optString("unit","kg/ha")))continue;
                String date=o.optString("date","");
                if(date.isEmpty()) continue;
                LocalDate d;
                try { d=LocalDate.parse(date); } catch(Exception invalidDate) { continue; }
                if(d.isBefore(cutoff)) continue;
                double dose=o.optDouble("dose",0);
                n+=dose*o.optDouble("nPct",0)/100.0;
                p+=dose*o.optDouble("pPct",0)/100.0;
                k+=dose*o.optDouble("kPct",0)/100.0;
            }
        } catch(Exception ignored) {}
        return new double[]{n,p,k};
    }
    private boolean same(String a,String b){return AgronomyEngine.normalizeCrop(a).equalsIgnoreCase(AgronomyEngine.normalizeCrop(b));}
    private String recent(String key,int max){try{JSONArray a=new JSONArray(prefs.getString(key,"[]"));StringBuilder s=new StringBuilder();for(int i=Math.max(0,a.length()-max);i<a.length();i++)s.append(a.optJSONObject(i)).append("\n");return s.toString().trim().isEmpty()?"(tidak ada)":s.toString();}catch(Exception e){return "(tidak ada)";}}
    private void requestAi(){
        final String provider=prefs.getString("ai_provider","OpenAI");
        final String key;
        final String model;
        if("Google Gemini".equalsIgnoreCase(provider)){key=prefs.getString("gemini_api_key","").trim();model=prefs.getString("gemini_model","gemini-2.5-flash").trim();}
        else if("OpenRouter".equalsIgnoreCase(provider)){key=prefs.getString("openrouter_api_key","").trim();model=prefs.getString("openrouter_model","google/gemini-2.5-flash").trim();}
        else {key=prefs.getString("ai_api_key",DEFAULT_AI_KEY).trim();model=prefs.getString("ai_model","gpt-6-luna").trim();}
        if(key.isEmpty()){aiStatus.setText("Masukkan API key untuk penyedia "+provider+" di Pengaturan.");return;}
        final TextView button=findViewById(R.id.agroAiButton); button.setEnabled(false); aiStatus.setText("Menganalisis data, mencari OPT dan menyusun rekomendasi AI...");
        final String actualModel=model.isEmpty()?("Google Gemini".equalsIgnoreCase(provider)?"gemini-2.5-flash":("OpenRouter".equalsIgnoreCase(provider)?"google/gemini-2.5-flash":"gpt-6-luna")):model;
        final boolean webSearch=prefs.getBoolean("ai_google_search",true);
        final double aiLat=prefs.getFloat("latitude",Float.NaN); final double aiLon=prefs.getFloat("longitude",Float.NaN);
        final String context="LOKASI GPS: "+(Double.isFinite(aiLat)?String.format(Locale.US,"%.6f",aiLat):"--")+", "+(Double.isFinite(aiLon)?String.format(Locale.US,"%.6f",aiLon):"--")+"\n"+report.getText().toString();
        net.execute(()->{try{String lit=fetchOpenAlex(prefs.getString("crop","Tanaman pertanian"), context);String ans=callAi(provider,key,actualModel,context,lit,webSearch);prefs.edit().putString("last_ai_advice",ans).putLong("last_ai_advice_epoch",System.currentTimeMillis()).apply();runOnUiThread(()->{aiStatus.setText(styleAiResponse(ans));button.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{aiStatus.setText("AI gagal ("+provider+"): "+msg(e));button.setEnabled(true);});}});
    }
    private String fetchOpenAlex(String crop,String context)throws Exception{
        String q=""+crop+" agriculture soil fertility pest disease weather forecasting";
        String url="https://api.openalex.org/works?search="+URLEncoder.encode(q,"UTF-8")+"&filter=from_publication_date:2018-01-01&per-page=6&sort=relevance_score:desc";
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestMethod("GET");if(c.getResponseCode()!=200)throw new Exception("OpenAlex HTTP "+c.getResponseCode());JSONObject r=new JSONObject(readAll(c.getInputStream()));JSONArray a=r.optJSONArray("results");StringBuilder out=new StringBuilder();if(a!=null)for(int i=0;i<a.length();i++){JSONObject w=a.optJSONObject(i);if(w==null)continue;out.append(i+1).append(") ").append(w.optString("display_name",""));out.append(" | year=").append(w.optInt("publication_year",0));String doi=w.optString("doi","");if(!doi.isEmpty())out.append(" | ").append(doi);out.append("\n");}return out.length()==0?"Tidak ada hasil OpenAlex yang cocok.":out.toString();
    }
    private String agronomyPrompt(String ctx,String lit){
        return "Anda adalah agronom pendamping petani Indonesia. Berikan keputusan dan rekomendasi tindakan berbasis data. Wajib bagian: RINGKASAN KONDISI; FAKTOR PEMBATAS; REKOMENDASI 0-24 JAM; REKOMENDASI 1-7 HARI; PEMUPUKAN; AIR; PREDIKSI POTENSI OPT; TINDAKAN PHT/OPT; PRIORITAS TINDAKAN; DATA YANG PERLU DIUKUR. " +
        "Untuk OPT, cari OPT relevan berdasarkan komoditas, fase, lokasi Indonesia, cuaca dan gejala; utamakan sumber resmi pemerintah/universitas, extension, jurnal dan organisasi pertanian. Sebut nama umum/ilmiah bila dapat diverifikasi, potensi risiko, alasan, gejala yang perlu diperiksa dan sumber. Ini skrining risiko, bukan diagnosis; jangan mengarang dosis pestisida. " +
        "Gunakan data tanah, umur/fase tanaman, sasaran produksi, riwayat pupuk, jenis tanah dan air; jangan menyamakan N/P/K sensor langsung dengan dosis pupuk tanpa metode tervalidasi. Bedakan EC dari ECe; jangan hitung kapur hanya dari pH. Jika data kurang, berikan estimasi/skenario yang jelas berlabel dan nyatakan ketidakpastian. " +
        "Gunakan bahasa Indonesia mudah dipahami petani. Jika varietas/galur/klon dan target produksi tersedia, prioritaskan mencari deskripsi varietas yang dapat diverifikasi dan jadikan target sebagai skenario, bukan bukti pasti. Pisahkan fakta terukur, screening model dan rekomendasi. Akhiri dengan prioritas tindakan konkret.\n\nDATA DAN ANALISIS AWAL:\n"+ctx+"\n\nHASIL PUSTAKA OPENALEX:\n"+lit+"\n\nJika pencarian web aktif, gunakan untuk memverifikasi SOP pemupukan dan budidaya sesuai komoditas, varietas, wilayah, status hara tanah, target produksi dan fase tanaman. Utamakan sumber resmi Kementerian Pertanian/BRMP, penyuluh atau universitas, publikasi varietas, jurnal, pedoman budidaya dan SOP spesifik lokasi. Cantumkan URL sumber untuk klaim penting; bedakan hasil percobaan varietas dari target kebun dan jangan menganggapnya sebagai jaminan hasil.";
    }
    private String callAi(String provider,String key,String model,String ctx,String lit,boolean webSearch)throws Exception{
        String prompt=agronomyPrompt(ctx,lit);
        if("Google Gemini".equalsIgnoreCase(provider)) return callGemini(key,model,prompt,webSearch);
        if("OpenRouter".equalsIgnoreCase(provider)) return callOpenRouter(key,model,prompt,webSearch);
        return callOpenAi(key,model,prompt,webSearch);
    }
    private String callOpenAi(String key,String model,String prompt,boolean webSearch)throws Exception{
        JSONObject p=new JSONObject();p.put("model",model);p.put("store",false);p.put("max_output_tokens",5000);p.put("instructions","Anda adalah agronom pendamping petani Indonesia. Jangan mengarang data.");p.put("input",prompt);
        if(webSearch){JSONArray tools=new JSONArray();JSONObject t=new JSONObject();t.put("type","web_search");tools.put(t);p.put("tools",tools);p.put("tool_choice","required");}
        HttpURLConnection c=(HttpURLConnection)new URL("https://api.openai.com/v1/responses").openConnection();c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");writeJson(c,p);int code=c.getResponseCode();if(code<200||code>=300)throw httpError("OpenAI",code,c);JSONObject r=new JSONObject(readAll(c.getInputStream()));String t=r.optString("output_text","").trim();if(!t.isEmpty())return t;JSONArray out=r.optJSONArray("output");if(out!=null){StringBuilder z=new StringBuilder();for(int i=0;i<out.length();i++){JSONObject it=out.optJSONObject(i);if(it==null)continue;JSONArray cc=it.optJSONArray("content");if(cc==null)continue;for(int j=0;j<cc.length();j++){JSONObject part=cc.optJSONObject(j);if(part!=null&&"output_text".equals(part.optString("type")))z.append(part.optString("text","")).append("\n");}}if(z.length()>0)return z.toString().trim();}throw new Exception("Respons OpenAI tidak berisi teks.");
    }
    private String callGemini(String key,String model,String prompt,boolean webSearch)throws Exception{
        JSONObject body=new JSONObject();JSONArray contents=new JSONArray();JSONObject content=new JSONObject();JSONArray parts=new JSONArray();JSONObject part=new JSONObject();part.put("text",prompt);parts.put(part);content.put("parts",parts);contents.put(content);body.put("contents",contents);JSONObject cfg=new JSONObject();cfg.put("temperature",0.4);cfg.put("maxOutputTokens",5000);body.put("generationConfig",cfg);
        if(webSearch){JSONArray tools=new JSONArray();JSONObject tool=new JSONObject();tool.put("google_search",new JSONObject());tools.put(tool);body.put("tools",tools);}
        String url="https://generativelanguage.googleapis.com/v1beta/models/"+URLEncoder.encode(model,"UTF-8")+":generateContent?key="+URLEncoder.encode(key,"UTF-8");HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");writeJson(c,body);int code=c.getResponseCode();if(code<200||code>=300)throw httpError("Gemini",code,c);JSONObject r=new JSONObject(readAll(c.getInputStream()));JSONArray candidates=r.optJSONArray("candidates");if(candidates==null||candidates.length()==0)throw new Exception("Respons Gemini kosong.");JSONObject candidate=candidates.optJSONObject(0);JSONObject cont=candidate==null?null:candidate.optJSONObject("content");JSONArray ps=cont==null?null:cont.optJSONArray("parts");StringBuilder out=new StringBuilder();if(ps!=null)for(int i=0;i<ps.length();i++){JSONObject pp=ps.optJSONObject(i);if(pp!=null)out.append(pp.optString("text", ""));}if(out.length()==0)throw new Exception("Gemini tidak mengembalikan teks.");return out.toString().trim();
    }
    private String callOpenRouter(String key,String model,String prompt,boolean webSearch)throws Exception{
        String selected=model;if(webSearch&&!selected.endsWith(":online"))selected=selected+":online";JSONObject body=new JSONObject();body.put("model",selected);body.put("temperature",0.4);body.put("max_tokens",5000);JSONArray messages=new JSONArray();JSONObject m=new JSONObject();m.put("role","user");m.put("content",prompt);messages.put(m);body.put("messages",messages);
        HttpURLConnection c=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("HTTP-Referer","https://github.com/ganicahyo-sketch");c.setRequestProperty("X-Title","Agronomy Advisor");writeJson(c,body);int code=c.getResponseCode();if(code<200||code>=300)throw httpError("OpenRouter",code,c);JSONObject r=new JSONObject(readAll(c.getInputStream()));JSONArray choices=r.optJSONArray("choices");if(choices==null||choices.length()==0)throw new Exception("Respons OpenRouter kosong.");JSONObject choice=choices.optJSONObject(0);JSONObject message=choice==null?null:choice.optJSONObject("message");String out=message==null?"":message.optString("content","").trim();if(out.isEmpty())throw new Exception("OpenRouter tidak mengembalikan teks.");return out;
    }
    private void writeJson(HttpURLConnection c,JSONObject body)throws Exception{byte[] b=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);try(OutputStream o=c.getOutputStream()){o.write(b);}}
    private Exception httpError(String provider,int code,HttpURLConnection c){try{String er=readAll(c.getErrorStream());return new Exception(provider+" HTTP "+code+(er.isEmpty()?"":" — "+er.substring(0,Math.min(500,er.length()))));}catch(Exception ignored){return new Exception(provider+" HTTP "+code);}}
    private String pref(String k,String d){return prefs.getString(k,d);}
    private double num(String s){try{return s==null||s.trim().isEmpty()?Double.NaN:Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    private String show(double v){return Double.isFinite(v)?String.format(Locale.US,"%.2f",v):"--";}
    private String readAll(InputStream in)throws Exception{if(in==null)return "";StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null)b.append(l);}return b.toString();}
    private String msg(Exception e){return e.getMessage()==null?"kesalahan tidak diketahui":e.getMessage();}
}
