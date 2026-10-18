package id.stasiuncuaca;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Catatan Lapangan + mesin analisis agronomi v1.5.7.
 *
 * Semua nilai CWT-7in1 (RS485/USB) dianggap benar sebagai input proyek.
 * Interpretasi tetap diberi label screening bila standar sangat tergantung
 * metode ekstraksi/laboratorium.
 */
public class FieldNotesActivity extends Activity {
    private static final String PREFS = "thingspeak_config";
    private static final String KEY_NOTES = "field_notes_v153";
    private static final String KEY_FERT = "fert_history";
    private static final String KEY_OPT = "opt_history";
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private SharedPreferences prefs;

    private EditText date, time, crop, plantDate, hst, area, observation, action, targetYield;
    private EditText soilPh, soilMoisture, soilTemp, soilEc, soilN, soilP, soilK, soilDepth, soilBulkDensity, soilFc, soilPwp, soilPhBuffer, soilAlDd, soilHDd, soilCec, soilOm, soilEce, soilLimeReq, soilNLow, soilNHigh;
    private Spinner soilTestMethod, ageUnit, dataSource, bulkDensityPreset;
    private EditText airTemp, airRh, pressure, rain24, et0, lux, par, sunHours, windSpeed;
    private Spinner noteType, severity, soilSource, windDirection, cultivation;
    private TextView autoPhase, analysisView, timelineView, soilSummaryView, dataSourceStatus, usbSoilStatus;
    private LinearLayout analysisCards;
    private UsbSoilMeter usbSoilMeter;
    private final ExecutorService net = Executors.newSingleThreadExecutor();
    private final Handler recalculationHandler = new Handler(Looper.getMainLooper());
    private final Runnable pendingRecalculation = () -> {
        if (prefs != null && !isFinishing()) runAnalysis(prefs);
    };

    private static final String[] NOTE_TYPES = {
            "Pengamatan umum", "Tanah", "Pemupukan", "OPT", "Irigasi/air",
            "Pertumbuhan", "Panen", "Lainnya"
    };
    private static final String[] SEVERITY = {
            "Tidak ada", "Ringan", "Sedang", "Berat", "Sangat berat"
    };
    private static final String[] SOIL_SOURCE = {
            "CWT-7in1 RS485", "CWT-7in1 USB", "Input manual"
    };
    private static final String[] CULTIVATION = {
            "Konvensional / PHT", "Organik"
    };
    private static final String[] DATA_SOURCES = {
            "Prioritas: ThingSpeak → Open-Meteo",
            "ThingSpeak",
            "Open-Meteo",
            "Manual (tidak otomatis)"
    };
    private static final String[] BULK_DENSITY_PRESETS = {
            "Tanah mineral — default 1,30 g/cm³",
            "Tanah gambut — referensi 0,30 g/cm³",
            "Input sendiri"
    };

    private static final String[] WIND = {
            "Tenang", "Utara", "Utara-Timur Laut", "Timur Laut", "Timur-Timur Laut",
            "Timur", "Timur-Tenggara", "Tenggara", "Selatan-Tenggara", "Selatan",
            "Selatan-Barat Daya", "Barat Daya", "Barat-Barat Daya", "Barat",
            "Barat-Barat Laut", "Barat Laut", "Utara-Barat Laut", "Variabel"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_notes);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        date = findViewById(R.id.fnDate);
        time = findViewById(R.id.fnTime);
        crop = findViewById(R.id.fnCrop);
        plantDate = findViewById(R.id.fnPlantDate);
        hst = findViewById(R.id.fnHst);
        area = findViewById(R.id.fnArea);
        observation = findViewById(R.id.fnObservation);
        action = findViewById(R.id.fnAction);
        targetYield = findViewById(R.id.fnTargetYield);

        soilPh = findViewById(R.id.fnSoilPh);
        soilMoisture = findViewById(R.id.fnSoilMoisture);
        soilTemp = findViewById(R.id.fnSoilTemp);
        soilEc = findViewById(R.id.fnSoilEc);
        soilN = findViewById(R.id.fnSoilN);
        soilP = findViewById(R.id.fnSoilP);
        soilK = findViewById(R.id.fnSoilK);
        soilDepth = findViewById(R.id.fnSoilDepth);
        soilBulkDensity = findViewById(R.id.fnSoilBulkDensity);
        soilFc = findViewById(R.id.fnSoilFc);
        soilPwp = findViewById(R.id.fnSoilPwp);
        soilPhBuffer = findViewById(R.id.fnSoilPhBuffer);
        soilAlDd = findViewById(R.id.fnSoilAlDd);
        soilHDd = findViewById(R.id.fnSoilHDd);
        soilCec = findViewById(R.id.fnSoilCec);
        soilOm = findViewById(R.id.fnSoilOm);
        soilEce = findViewById(R.id.fnSoilEce);
        soilLimeReq = findViewById(R.id.fnSoilLimeReq);
        soilTestMethod = findViewById(R.id.fnSoilTestMethod);
        soilNLow = findViewById(R.id.fnSoilNLow); soilNHigh = findViewById(R.id.fnSoilNHigh);
        ageUnit = findViewById(R.id.fnAgeUnit);
        dataSource = findViewById(R.id.fnDataSource);
        bulkDensityPreset = findViewById(R.id.fnBulkDensityPreset);
        dataSourceStatus = findViewById(R.id.fnDataSourceStatus);
        usbSoilStatus = findViewById(R.id.fnUsbSoilStatus);

        airTemp = findViewById(R.id.fnAirTemp);
        airRh = findViewById(R.id.fnAirRh);
        pressure = findViewById(R.id.fnPressure);
        rain24 = findViewById(R.id.fnRain24);
        et0 = findViewById(R.id.fnEt0);
        lux = findViewById(R.id.fnLux);
        par = findViewById(R.id.fnPar);
        sunHours = findViewById(R.id.fnSunHours);
        windSpeed = findViewById(R.id.fnWindSpeed);

        noteType = findViewById(R.id.fnNoteType);
        severity = findViewById(R.id.fnSeverity);
        soilSource = findViewById(R.id.fnSoilSource);
        windDirection = findViewById(R.id.fnWindDirection);
        cultivation = findViewById(R.id.fnCultivation);

        autoPhase = findViewById(R.id.fnAutoPhase);
        analysisView = findViewById(R.id.fnAnalysis);
        analysisCards = findViewById(R.id.fnAnalysisCards);
        // Successful results use separate, readable cards rather than one dense wall of text.
        analysisView.setTextSize(16);
        analysisView.setLineSpacing(dp(6), 1.10f);
        analysisView.setPadding(0, dp(6), 0, dp(8));
        analysisView.setVisibility(View.GONE);
        timelineView = findViewById(R.id.fnTimeline);
        soilSummaryView = findViewById(R.id.fnSoilSummary);

        bindSpinner(noteType, NOTE_TYPES);
        bindSpinner(severity, SEVERITY);
        bindSpinner(soilSource, SOIL_SOURCE);
        bindSpinner(soilTestMethod, new String[]{"Metode tidak diketahui", "HCl 25%", "Bray-1", "Olsen", "Mehlich-3", "Mehlich-3 ICP", "NH4OAc"});
        bindSpinner(windDirection, WIND);
        bindSpinner(cultivation, CULTIVATION);
        bindSpinner(dataSource, DATA_SOURCES);
        bindSpinner(bulkDensityPreset, BULK_DENSITY_PRESETS);
        selectSpinner(dataSource, prefs.getString("agro_data_source", DATA_SOURCES[0]));
        int bdPreset = prefs.getInt("soil_bd_preset", 0);
        if (bdPreset < 0 || bdPreset >= BULK_DENSITY_PRESETS.length) bdPreset = 0;
        bulkDensityPreset.setSelection(bdPreset);
        String savedSoilSource = prefs.getString("soil_source", "");
        if (!savedSoilSource.isEmpty()) selectSpinner(soilSource, savedSoilSource);
        String savedCultivation = prefs.getString("farm_cultivation_mode", "");
        if (!savedCultivation.isEmpty()) {
            for (int i = 0; i < CULTIVATION.length; i++) {
                if (CULTIVATION[i].equalsIgnoreCase(savedCultivation)) { cultivation.setSelection(i); break; }
            }
        }

        LocalDateTime now = LocalDateTime.now(WIB);
        date.setText(now.format(DATE_FMT));
        time.setText(now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US)));
        crop.setText(prefs.getString("crop", ""));
        area.setText(prefs.getString("farm_area_ha", ""));
        plantDate.setText(prefs.getString("farm_planting_date", ""));
        hst.setText(prefs.getString("farm_hst", ""));
        String savedAgeValue=prefs.getString("farm_age_value", ""); if (!savedAgeValue.isEmpty()) hst.setText(savedAgeValue);
        selectSpinner(ageUnit,prefs.getString("farm_age_unit","HST / hari"));
        targetYield.setText(prefs.getString("farm_target_yield_t_ha", ""));
        soilPh.setText(prefs.getString("soil_ph", ""));
        soilMoisture.setText(prefs.getString("soil_moisture_pct", ""));
        soilEc.setText(prefs.getString("soil_ec_us_cm", ""));
        soilN.setText(prefs.getString("soil_n", ""));
        soilP.setText(prefs.getString("soil_p", ""));
        soilK.setText(prefs.getString("soil_k", "")); soilNLow.setText(prefs.getString("soil_n_low","")); soilNHigh.setText(prefs.getString("soil_n_high",""));
        soilDepth.setText(prefs.getString("soil_depth_cm", "20"));
        soilBulkDensity.setText(prefs.getString("soil_bulk_density_g_cm3", "1.30"));
        soilFc.setText(prefs.getString("soil_fc_pct", ""));
        soilPwp.setText(prefs.getString("soil_pwp_pct", ""));
        soilPhBuffer.setText(prefs.getString("soil_ph_buffer", ""));
        soilAlDd.setText(prefs.getString("soil_al_dd", ""));
        soilHDd.setText(prefs.getString("soil_h_dd", ""));
        soilCec.setText(prefs.getString("soil_cec", ""));
        soilOm.setText(prefs.getString("soil_om_pct", ""));
        soilEce.setText(prefs.getString("soil_ece_ds_m", ""));
        soilLimeReq.setText(prefs.getString("soil_lime_requirement_kg_ha", ""));
        selectSpinner(soilTestMethod, prefs.getString("soil_test_method", "Metode tidak diketahui"));

        airTemp.setText(prefs.getString("om_temp", ""));
        airRh.setText(prefs.getString("om_rh", ""));
        pressure.setText(prefs.getString("om_pressure", ""));
        rain24.setText(prefs.getString("om_rain", ""));
        et0.setText(prefs.getString("om_et0", ""));
        vpdFromOpenMeteo();
        windSpeed.setText(prefs.getString("om_wind_speed", ""));
        sunHours.setText(prefs.getString("om_sun_hours", ""));

        findViewById(R.id.fnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fnSave).setOnClickListener(v -> saveFieldNote(prefs));
        findViewById(R.id.fnAddFertilizer).setOnClickListener(v -> showFertilizerDialog(prefs));
        findViewById(R.id.fnAddOpt).setOnClickListener(v -> showOptDialog(prefs));
        findViewById(R.id.fnAnalyze).setOnClickListener(v -> runAnalysis(prefs));
        findViewById(R.id.fnAutoFill).setOnClickListener(v -> applyAutomaticData(prefs));
        findViewById(R.id.fnUsbSoilScan).setOnClickListener(v -> {
            if (usbSoilMeter != null) usbSoilMeter.scanNow();
            else usbSoilStatus.setText("Pemindai USB sedang disiapkan…");
        });
        findViewById(R.id.fnManageHistory).setOnClickListener(v -> showHistoryManager(prefs));
        findViewById(R.id.fnNotes).setOnClickListener(v -> showMethodNotes());
        dataSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.edit().putString("agro_data_source", DATA_SOURCES[position]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        bulkDensityPreset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.edit().putInt("soil_bd_preset", position).apply();
                applyBulkDensityPreset(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        plantDate.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) updateAutoPhase(); });
        hst.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) updateAutoPhase(); });
        crop.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) updateAutoPhase(); });

        updateAutoPhase();
        applyBulkDensityPreset(bdPreset);
        refreshViews(prefs);
        // Gunakan cache bila tersedia; tombol "AMBIL DATA TERBARU" melakukan refresh jaringan.
        applyAutomaticDataFromCache(prefs);
        attachAutomaticRecalculation();
        runAnalysis(prefs);
        usbSoilMeter = new UsbSoilMeter(this, new UsbSoilMeter.Listener() {
            @Override public void onStatus(String status) {
                if (usbSoilStatus != null) usbSoilStatus.setText(status);
            }
            @Override public void onValues(UsbSoilMeter.Values v) {
                soilMoisture.setText(String.format(Locale.US, "%.1f", v.moisturePct));
                soilTemp.setText(String.format(Locale.US, "%.1f", v.temperatureC));
                soilEc.setText(String.format(Locale.US, "%.0f", v.ecUsCm));
                soilPh.setText(String.format(Locale.US, "%.1f", v.ph));
                soilN.setText(String.format(Locale.US, "%.0f", v.nitrogen));
                soilP.setText(String.format(Locale.US, "%.0f", v.phosphorus));
                soilK.setText(String.format(Locale.US, "%.0f", v.potassium));
                selectSpinner(soilSource, SOIL_SOURCE[1]);
                prefs.edit()
                    .putString("soil_moisture_pct", soilMoisture.getText().toString())
                    .putString("soil_temp_c", soilTemp.getText().toString())
                    .putString("soil_ec_us_cm", soilEc.getText().toString())
                    .putString("soil_ph", soilPh.getText().toString())
                    .putString("soil_n", soilN.getText().toString())
                    .putString("soil_p", soilP.getText().toString())
                    .putString("soil_k", soilK.getText().toString())
                    .putString("soil_source", SOIL_SOURCE[1]).apply();
                soilSummaryView.setText(buildSoilSummary());
                scheduleAnalysisRecalculation();
                if (usbSoilStatus != null) usbSoilStatus.setText("USB 8-in-1 aktif • M=" + String.format(Locale.US, "%.1f", v.moisturePct)
                    + "% • T=" + String.format(Locale.US, "%.1f", v.temperatureC) + "°C • EC="
                    + String.format(Locale.US, "%.0f", v.ecUsCm) + " µS/cm • fertility="
                    + String.format(Locale.US, "%.0f", v.fertility) + ". Pemetaan register generik; verifikasi satuan dengan manual alat.");
            }
        });
    }


    @Override protected void onStart() {
        super.onStart();
        if (usbSoilMeter != null) usbSoilMeter.start();
    }

    @Override protected void onStop() {
        if (usbSoilMeter != null) usbSoilMeter.stop();
        super.onStop();
    }

    @Override protected void onDestroy() {
        recalculationHandler.removeCallbacks(pendingRecalculation);
        if (usbSoilMeter != null) usbSoilMeter.dispose();
        super.onDestroy();
    }

    private void vpdFromOpenMeteo() {
        // VPD is calculated directly by Open-Meteo and also by the local engine from T/RH.
        // The field is named below using the existing fnVpd view when available.
        int id = getResources().getIdentifier("fnVpd", "id", getPackageName());
        if (id != 0) { TextView v = findViewById(id); if (v instanceof EditText) ((EditText)v).setText(prefsSafe("om_vpd")); }
    }

    private String prefsSafe(String key) { return getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, ""); }

    private void selectSpinner(Spinner spinner, String value) { if (value == null) return; android.widget.Adapter a = spinner.getAdapter(); if (a == null) return; for (int i=0;i<a.getCount();i++) if (value.equalsIgnoreCase(String.valueOf(a.getItem(i)))) { spinner.setSelection(i); break; } }

    private void bindSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
    }

    private void updateAutoPhase() {
        String c = crop.getText().toString().trim();
        int age = currentHst();
        if (age >= 0) {
            autoPhase.setText("Prediksi fase: " + AgronomyEngine.phase(age, c) + " • HST " + age + " (input " + hst.getText().toString().trim() + " " + ageUnit.getSelectedItem() + ")");
        } else {
            autoPhase.setText("Prediksi fase: isi tanggal tanam atau umur tanaman.");
        }
    }

    private int currentHst() {
        if (validDate(plantDate.getText().toString().trim())) {
            try {
                int d=(int)java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(plantDate.getText().toString().trim(),DATE_FMT),LocalDate.now(WIB));
                if(d>=0) return d;
            } catch(Exception ignored) {}
        }
        double raw=num(hst.getText().toString());
        return Double.isNaN(raw)?-1:ageToHst(raw);
    }




    private int ageToHst(double raw) {
        if (!Double.isFinite(raw) || raw < 0) return -1;
        String u=String.valueOf(ageUnit.getSelectedItem()).toLowerCase(Locale.US);
        if(u.contains("minggu")) return (int)Math.round(raw*7.0);
        if(u.contains("bulan")) return (int)Math.round(raw*30.44);
        if(u.contains("tahun")) return (int)Math.round(raw*365.25);
        return (int)Math.round(raw);
    }


    private void saveFieldNote(SharedPreferences prefs) {
        String c = crop.getText().toString().trim();
        if (c.isEmpty()) { crop.setError("Tanaman wajib diisi"); crop.requestFocus(); return; }
        if (!validDate(date.getText().toString().trim())) { date.setError("Gunakan YYYY-MM-DD"); date.requestFocus(); return; }
        if (!plantDate.getText().toString().trim().isEmpty() && !validDate(plantDate.getText().toString().trim())) {
            plantDate.setError("Gunakan YYYY-MM-DD"); return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("date", date.getText().toString().trim());
            o.put("time", time.getText().toString().trim());
            o.put("crop", c);
            o.put("cultivation", cultivation.getSelectedItem().toString());
            o.put("plantingDate", plantDate.getText().toString().trim());
            putTextNumber(o, "hst", hst);
            putTextNumber(o, "targetYieldTHa", targetYield);
            putTextNumber(o, "areaHa", area);
            o.put("phase", agePhase());
            o.put("type", noteType.getSelectedItem().toString());
            o.put("severity", severity.getSelectedItem().toString());
            o.put("observation", observation.getText().toString().trim());
            o.put("action", action.getText().toString().trim());
            o.put("structuredTemplate", "tanaman/fase; lokasi; gejala/OPT; luas/populasi; kondisi cuaca; tindakan; hasil");
            o.put("soilSource", soilSource.getSelectedItem().toString());
            putTextNumber(o, "soilPh", soilPh);
            putTextNumber(o, "soilMoisturePct", soilMoisture);
            putTextNumber(o, "soilTemperatureC", soilTemp);
            putTextNumber(o, "soilEcUsCm", soilEc);
            putTextNumber(o, "soilNmgKg", soilN);
            putTextNumber(o, "soilPmgKg", soilP);
            putTextNumber(o, "soilKmgKg", soilK);
            putTextNumber(o, "soilDepthCm", soilDepth);
            putTextNumber(o, "soilBulkDensityGcm3", soilBulkDensity);
            putTextNumber(o, "soilFcPct", soilFc);
            putTextNumber(o, "soilPwpPct", soilPwp);
            putTextNumber(o, "soilPhBuffer", soilPhBuffer);
            putTextNumber(o, "soilAlDd", soilAlDd);
            putTextNumber(o, "soilHDd", soilHDd);
            putTextNumber(o, "soilCec", soilCec);
            putTextNumber(o, "soilOmPct", soilOm);
            putTextNumber(o, "soilEceDsM", soilEce);
            putTextNumber(o, "soilLimeRequirementKgHa", soilLimeReq);
            o.put("soilTestMethod", soilTestMethod.getSelectedItem().toString());
            putTextNumber(o, "airTempC", airTemp);
            putTextNumber(o, "airRhPct", airRh);
            putTextNumber(o, "pressureHpa", pressure);
            putTextNumber(o, "rain24mm", rain24);
            putTextNumber(o, "et0Mm", et0);
            putTextNumber(o, "lux", lux);
            putTextNumber(o, "parUmol", par);
            putTextNumber(o, "sunHours", sunHours);
            putTextNumber(o, "windSpeedMs", windSpeed);
            o.put("windDirection", windDirection.getSelectedItem().toString());
            o.put("created", System.currentTimeMillis());

            appendHistory(prefs, KEY_NOTES, o, 500);
            prefs.edit()
                    .putString("crop", c)
                    .putString("farm_area_ha", area.getText().toString().trim())
                    .putString("farm_cultivation_mode", cultivation.getSelectedItem().toString())
                    .putString("farm_planting_date", plantDate.getText().toString().trim())
                    .putString("farm_hst", String.valueOf(currentHst())).putString("farm_age_value", hst.getText().toString().trim()).putString("farm_age_unit", ageUnit.getSelectedItem().toString())
                    .putString("farm_target_yield_t_ha", targetYield.getText().toString().trim())
                    .putString("soil_ph", soilPh.getText().toString().trim())
                    .putString("soil_moisture_pct", soilMoisture.getText().toString().trim())
                    .putString("soil_ec_us_cm", soilEc.getText().toString().trim())
                    .putString("soil_n", soilN.getText().toString().trim())
                    .putString("soil_p", soilP.getText().toString().trim())
                    .putString("soil_k", soilK.getText().toString().trim()).putString("soil_source", soilSource.getSelectedItem().toString()).putString("soil_n_low",soilNLow.getText().toString().trim()).putString("soil_n_high",soilNHigh.getText().toString().trim())
                    .putString("soil_depth_cm", soilDepth.getText().toString().trim())
                    .putString("soil_bulk_density_g_cm3", soilBulkDensity.getText().toString().trim())
                    .putString("soil_fc_pct", soilFc.getText().toString().trim())
                    .putString("soil_pwp_pct", soilPwp.getText().toString().trim())
                    .putString("soil_ph_buffer", soilPhBuffer.getText().toString().trim())
                    .putString("soil_al_dd", soilAlDd.getText().toString().trim())
                    .putString("soil_h_dd", soilHDd.getText().toString().trim())
                    .putString("soil_cec", soilCec.getText().toString().trim())
                    .putString("soil_om_pct", soilOm.getText().toString().trim())
                    .putString("soil_ece_ds_m", soilEce.getText().toString().trim())
                    .putString("soil_lime_requirement_kg_ha", soilLimeReq.getText().toString().trim())
                    .putString("soil_test_method", soilTestMethod.getSelectedItem().toString())
                    .putInt("soil_bd_preset", bulkDensityPreset.getSelectedItemPosition())
                    .apply();

            // Storage has completed successfully at this point. Do not report a save failure
            // because a later UI refresh or agronomy analysis happens to throw.
            Toast.makeText(this, "Catatan lapangan tersimpan.", Toast.LENGTH_SHORT).show();
            observation.setText(""); action.setText("");
        } catch (Exception ex) {
            Toast.makeText(this, "Catatan lapangan gagal disimpan: " +
                    (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()),
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            refreshViews(prefs);
            runAnalysis(prefs);
        } catch (Exception ex) {
            Toast.makeText(this, "Catatan sudah tersimpan, tetapi tampilan analisis gagal diperbarui.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String agePhase() {
        double v = num(hst.getText().toString());
        return Double.isNaN(v) ? "" : AgronomyEngine.phase((int)Math.round(v), crop.getText().toString().trim());
    }

    private void showFertilizerDialog(SharedPreferences prefs) {
        LinearLayout root = dialogRoot();
        EditText dte = edit(root, "Tanggal (YYYY-MM-DD)", LocalDate.now(WIB).format(DATE_FMT), false);
        EditText product = edit(root, "Nama pupuk", "", false);
        Spinner cat = spinner(root, "Jenis pupuk", new String[]{"Pupuk N","Pupuk P","Pupuk K","NPK","Dolomit","Pupuk kandang","POC","Lainnya"});
        EditText dose = edit(root, "Dosis produk per ha", "", true);
        Spinner unit = spinner(root, "Satuan", new String[]{"kg/ha","L/ha"});
        EditText nPct = edit(root, "Kandungan N (%)", "", true);
        EditText pPct = edit(root, "Kandungan P2O5 (%)", "", true);
        EditText kPct = edit(root, "Kandungan K2O (%)", "", true);
        EditText method = edit(root, "Cara pemberian", "", false);
        EditText stage = edit(root, "Umur/fase tanaman", agePhase(), false);
        EditText result = edit(root, "Hasil/pengamatan setelah aplikasi", "", false);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Tambah Riwayat Pemupukan").setView(wrap(root)).setNegativeButton("BATAL", null).setPositiveButton("SIMPAN", null).create();
        d.setOnShowListener(v -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            double dv = num(dose.getText().toString());
            if (!validDate(dte.getText().toString().trim())) { dte.setError("Tanggal harus YYYY-MM-DD"); return; }
            if (product.getText().toString().trim().isEmpty() || Double.isNaN(dv) || dv <= 0) { product.setError("Nama pupuk dan dosis wajib diisi"); return; }
            try {
                JSONObject o = new JSONObject();
                o.put("date", dte.getText().toString().trim()); o.put("product", product.getText().toString().trim());
                o.put("category", cat.getSelectedItem().toString()); o.put("dose", dv); o.put("unit", unit.getSelectedItem().toString());
                o.put("nPct", safe(nPct)); o.put("pPct", safe(pPct)); o.put("kPct", safe(kPct));
                o.put("method", method.getText().toString().trim()); o.put("stage", stage.getText().toString().trim()); o.put("note", result.getText().toString().trim());
                o.put("crop", crop.getText().toString().trim()); o.put("cultivation", cultivation.getSelectedItem().toString()); o.put("created", System.currentTimeMillis());
                appendHistory(prefs, KEY_FERT, o, 300); d.dismiss(); refreshViews(prefs); runAnalysis(prefs);
            } catch (Exception ex) { Toast.makeText(this, "Riwayat pupuk gagal disimpan.", Toast.LENGTH_SHORT).show(); }
        }));
        d.show();
    }

    private void showOptDialog(SharedPreferences prefs) {
        LinearLayout root = dialogRoot();
        EditText dte = edit(root, "Tanggal (YYYY-MM-DD)", LocalDate.now(WIB).format(DATE_FMT), false);
        EditText target = edit(root, "Nama OPT / penyakit", "", false);
        EditText pop = edit(root, "Populasi (unit/ha, bila ada)", "", true);
        EditText affected = edit(root, "Luas/proporsi serangan (%)", "", true);
        EditText obs = edit(root, "Gejala khas / lokasi serangan", "", false);
        Spinner method = spinner(root, "Pengendalian", new String[]{"Monitoring","Kultur teknis","Manual","Mekanis","Biologis","Kimia","Tidak dilakukan"});
        EditText product = edit(root, "Produk (bila ada)", "", false);
        EditText active = edit(root, "Bahan aktif (bila ada)", "", false);
        EditText dose = edit(root, "Dosis sesuai label", "", true);
        Spinner doseUnit = spinner(root, "Satuan dosis", new String[]{"g/ha","kg/ha","ml/ha","L/ha"});
        EditText water = edit(root, "Volume air (L/ha)", "", true);
        EditText controlCost = edit(root, "Biaya pengendalian (Rp/ha)", "", true);
        EditText cropPrice = edit(root, "Harga komoditas (Rp/kg)", "", true);
        EditText damageCoeff = edit(root, "Koefisien kerusakan (kg/ha per unit OPT/ha)", "", true);
        EditText result = edit(root, "Hasil pengendalian", "", false);
        EditText note = edit(root, "Catatan", "", false);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Tambah Riwayat OPT / Pengendalian").setView(wrap(root)).setNegativeButton("BATAL", null).setPositiveButton("SIMPAN", null).create();
        d.setOnShowListener(v -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            if (target.getText().toString().trim().isEmpty()) { target.setError("Nama OPT/penyakit wajib diisi"); return; }
            if (!validDate(dte.getText().toString().trim())) { dte.setError("Tanggal harus YYYY-MM-DD"); return; }
            try {
                JSONObject o = new JSONObject();
                o.put("date", dte.getText().toString().trim()); o.put("target", target.getText().toString().trim());
                putTextNumber(o, "populationPerHa", pop); putTextNumber(o, "affectedPct", affected); o.put("observation", obs.getText().toString().trim());
                o.put("method", method.getSelectedItem().toString()); o.put("product", product.getText().toString().trim()); o.put("active", active.getText().toString().trim());
                putTextNumber(o, "dose", dose); o.put("doseUnit", doseUnit.getSelectedItem().toString()); putTextNumber(o, "waterLHa", water);
                putTextNumber(o, "controlCostRpHa", controlCost); putTextNumber(o, "commodityPriceRpKg", cropPrice); putTextNumber(o, "damageCoefficient", damageCoeff);
                o.put("result", result.getText().toString().trim()); o.put("note", note.getText().toString().trim()); o.put("crop", crop.getText().toString().trim());
                o.put("cultivation", cultivation.getSelectedItem().toString()); o.put("created", System.currentTimeMillis());
                appendHistory(prefs, KEY_OPT, o, 300); d.dismiss(); refreshViews(prefs); runAnalysis(prefs);
            } catch (Exception ex) { Toast.makeText(this, "Riwayat OPT gagal disimpan.", Toast.LENGTH_SHORT).show(); }
        }));
        d.show();
    }

    private void applyBulkDensityPreset(int position) {
        if (bulkDensityPreset == null || soilBulkDensity == null) return;
        if (position == 0) soilBulkDensity.setText("1.30");
        else if (position == 1) soilBulkDensity.setText("0.30");
        // position 2 intentionally preserves manual input.
    }

    private void applyAutomaticDataFromCache(SharedPreferences prefs) {
        String src = dataSource == null ? DATA_SOURCES[0] : String.valueOf(dataSource.getSelectedItem());
        boolean hasCache = !prefs.getString("ts_field_1", "").trim().isEmpty()
                || !prefs.getString("om_temp", "").trim().isEmpty();
        if (!hasCache || src.toLowerCase(Locale.US).contains("tidak")) return;
        applySourceValues(prefs, null, null, true);
    }

    private void applyAutomaticData(SharedPreferences prefs) {
        final String src = String.valueOf(dataSource.getSelectedItem());
        dataSourceStatus.setText("Mengambil data terbaru: " + src + "...");
        net.execute(() -> {
            JSONObject tsMeta = null, tsFeed = null, om = null;
            String tsError = "", omError = "";
            try {
                if (src.startsWith("Prioritas") || src.equals("ThingSpeak")) {
                    JSONObject[] x = fetchThingSpeakSnapshot(prefs);
                    tsMeta = x[0]; tsFeed = x[1];
                }
            } catch (Exception e) { tsError = e.getMessage() == null ? "ThingSpeak gagal" : e.getMessage(); }
            try {
                if (src.startsWith("Prioritas") || src.equals("Open-Meteo")) {
                    om = fetchOpenMeteoSnapshot(prefs);
                }
            } catch (Exception e) { omError = e.getMessage() == null ? "Open-Meteo gagal" : e.getMessage(); }
            final JSONObject fTsMeta = tsMeta, fTsFeed = tsFeed, fOm = om;
            final String fTsError = tsError, fOmError = omError;
            runOnUiThread(() -> {
                try {
                    applySourceValues(prefs, fTsMeta, fTsFeed, false, fOm);
                    String msg = "Data terisi.";
                    if (!fTsError.isEmpty()) msg += " TS: " + fTsError + ".";
                    if (!fOmError.isEmpty()) msg += " OM: " + fOmError + ".";
                    dataSourceStatus.setText(msg);
                    refreshViews(prefs);
                    runAnalysis(prefs);
                } catch (Exception e) {
                    dataSourceStatus.setText("Pengisian otomatis gagal: " + (e.getMessage() == null ? "data tidak terbaca" : e.getMessage()));
                }
            });
        });
    }

    // Overload retained so cache application can use the stored prefs only.
    private void applySourceValues(SharedPreferences prefs, JSONObject tsMeta, JSONObject tsFeed, boolean cacheOnly) {
        applySourceValues(prefs, tsMeta, tsFeed, cacheOnly, null);
    }

    private void applySourceValues(SharedPreferences prefs, JSONObject tsMeta, JSONObject tsFeed, boolean cacheOnly, JSONObject om) {
        String src = String.valueOf(dataSource.getSelectedItem());
        boolean useTs = src.startsWith("Prioritas") || src.equals("ThingSpeak");
        boolean useOm = src.startsWith("Prioritas") || src.equals("Open-Meteo");
        JSONObject feed = tsFeed;
        JSONObject meta = tsMeta;
        if (cacheOnly && useTs) {
            try {
                feed = new JSONObject(); meta = new JSONObject();
                for (int i=1;i<=8;i++) {
                    feed.put("field"+i, prefs.getString("ts_field_"+i, ""));
                    meta.put("field"+i, prefs.getString("ts_field_name_"+i, prefs.getString("field_name_"+i, "")));
                }
            } catch (org.json.JSONException ignored) {
                feed = null;
                meta = null;
            }
        }
        if (useTs && feed != null) {
            applyThingSpeakFields(prefs, meta, feed);
        }
        if (useOm) {
            JSONObject x = om;
            if (x == null) x = openMeteoObjectFromPrefs(prefs);
            if (x != null) applyOpenMeteoFields(x, useTs, meta, feed);
        }
        dataSourceStatus.setText("Sumber input: " + src + ". Nilai otomatis tetap dapat diedit manual.");
    }

    private JSONObject[] fetchThingSpeakSnapshot(SharedPreferences prefs) throws Exception {
        String ch = prefs.getString("channel", "").trim();
        String key = prefs.getString("read_key", "").trim();
        if (ch.isEmpty()) ch = "2981880";
        String base = "https://api.thingspeak.com/channels/" + java.net.URLEncoder.encode(ch,"UTF-8");
        String auth = key.isEmpty() ? "" : "?api_key=" + java.net.URLEncoder.encode(key,"UTF-8");
        JSONObject meta = null;
        try { meta = httpJson(base + ".json" + auth); } catch (Exception ignored) {}
        if (meta == null) meta = new JSONObject();
        for (int i=1;i<=8;i++) {
            if (meta.optString("field"+i,"").trim().isEmpty()) {
                String cachedName = prefs.getString("ts_field_name_"+i, prefs.getString("field_name_"+i, ""));
                if (!cachedName.trim().isEmpty()) meta.put("field"+i, cachedName);
            }
        }
        String sep = key.isEmpty() ? "" : "&api_key=" + java.net.URLEncoder.encode(key,"UTF-8");
        JSONObject feed = httpJson(base + "/feeds/last.json?timezone=Asia%2FJakarta&status=true" + sep);
        return new JSONObject[]{meta, feed};
    }

    private JSONObject fetchOpenMeteoSnapshot(SharedPreferences prefs) throws Exception {
        double lat = prefs.getFloat("latitude", Float.NaN), lon = prefs.getFloat("longitude", Float.NaN);
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) throw new Exception("koordinat GPS belum tersedia");
        String cur = "temperature_2m,relative_humidity_2m,apparent_temperature,dew_point_2m,precipitation,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,shortwave_radiation,vapour_pressure_deficit,soil_temperature_0_to_10cm,soil_moisture_0_to_10cm,uv_index,cloud_cover,visibility";
        String daily = "precipitation_sum,sunshine_duration,et0_fao_evapotranspiration";
        String u = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=" + java.net.URLEncoder.encode(cur,"UTF-8")
                + "&daily=" + java.net.URLEncoder.encode(daily,"UTF-8")
                + "&timezone=Asia%2FJakarta&forecast_days=1";
        return httpJson(u);
    }

    private void applyThingSpeakFields(SharedPreferences prefs, JSONObject meta, JSONObject feed) {
        for (int i=1;i<=8;i++) {
            String v = feed.optString("field"+i, "").trim();
            String n = meta.optString("field"+i, prefs.getString("field_name_"+i, "Field "+i));
            String u = prefs.getString("field_unit_"+i, "");
            prefs.edit().putString("ts_field_"+i,v).putString("ts_field_name_"+i,n).putString("ts_field_unit_"+i,u).apply();
        }
        mapTsValue("airTemp", matchTs(meta, feed, new String[]{"suhu udara","air temp","temperature"}, new String[]{"tanah","soil"}));
        mapTsValue("airRh", matchTs(meta, feed, new String[]{"kelembapan udara","humidity","rh"}, new String[]{"tanah","soil"}));
        mapTsValue("pressure", matchTs(meta, feed, new String[]{"tekanan","pressure","baro"}, new String[]{}));
        mapTsValue("rain24", matchTs(meta, feed, new String[]{"hujan 24","rain","precip"}, new String[]{}));
        mapTsValue("et0", matchTs(meta, feed, new String[]{"et0","evapotrans"}, new String[]{}));
        mapTsValue("soilPh", matchTs(meta, feed, new String[]{"pH"," ph"}, new String[]{}));
        mapTsValue("soilEc", matchTs(meta, feed, new String[]{"ec","conductivity","konduktiv"}, new String[]{}));
        mapTsValue("soilN", matchTs(meta, feed, new String[]{"n tersedia","nitrogen","nitrat"," N"}, new String[]{"wind","angin","rain"}));
        mapTsValue("soilP", matchTs(meta, feed, new String[]{"p tersedia","phosph","fosfor"," P"}, new String[]{}));
        mapTsValue("soilK", matchTs(meta, feed, new String[]{"k tersedia","potassium","kalium"," K"}, new String[]{}));
        mapTsValue("soilMoisture", matchTs(meta, feed, new String[]{"kelembapan tanah","soil moisture","soil_moisture","moisture"}, new String[]{"udara","air"}), true);
        mapTsValue("soilTemp", matchTs(meta, feed, new String[]{"suhu tanah","soil temp","soil_temperature"}, new String[]{}));
        mapTsValue("soilOm", matchTs(meta, feed, new String[]{"bahan organik","organic matter","organik"}, new String[]{}));
        mapTsValue("soilCec", matchTs(meta, feed, new String[]{"cec","ktk","cat ion exchange"}, new String[]{}));
        mapTsValue("soilEce", matchTs(meta, feed, new String[]{"ece","ec e","ec_e"}, new String[]{}));
        mapTsValue("windSpeed", matchTs(meta, feed, new String[]{"kecepatan angin","wind speed","wind"}, new String[]{"arah","direction","gust"}));
        String dir = matchTs(meta, feed, new String[]{"arah angin","wind direction","direction"}, new String[]{});
        if (!dir.isEmpty()) {
            double deg=num(dir);
            selectSpinner(windDirection, Double.isFinite(deg) ? MainActivity.compass(deg) : dir);
        }
        mapTsValue("par", matchTs(meta, feed, new String[]{"ppfd","par"}, new String[]{"shortwave"}));
        mapTsValue("sunHours", matchTs(meta, feed, new String[]{"sunshine","lama penyinaran","durasi sinar"}, new String[]{}));
        mapTsValue("vpd", matchTs(meta, feed, new String[]{"vpd"}, new String[]{}));
    }

    private String matchTs(JSONObject meta, JSONObject feed, String[] aliases, String[] excludes) {
        for (int i=1;i<=8;i++) {
            String name = meta.optString("field"+i, "");
            String custom = prefs.getString("field_name_"+i, "");
            String both = normalize(name + " " + custom);
            if (both.isEmpty()) continue;
            boolean hit=false;
            String padded=" "+both+" ";
            for (String a:aliases) {
                String aa=normalize(a);
                if (aa.isEmpty()) continue;
                if (aa.length()<=2) { if (padded.contains(" "+aa+" ")) { hit=true; break; } }
                else if (padded.contains(" "+aa+" ") || both.contains(aa)) { hit=true; break; }
            }
            if (!hit) continue;
            boolean excluded=false;
            for (String x:excludes) if (!normalize(x).isEmpty() && both.contains(normalize(x))) { excluded=true; break; }
            if (!excluded) return feed.optString("field"+i, "").trim();
        }
        return "";
    }

    private String normalize(String s) { return s == null ? "" : s.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+"," ").trim(); }

    private void mapTsValue(String field, String value) { mapTsValue(field,value,false); }
    private void mapTsValue(String field, String value, boolean moisture) {
        if (value == null || value.trim().isEmpty()) return;
        EditText e=null;
        if (field.equals("airTemp")) e=airTemp; else if(field.equals("airRh"))e=airRh; else if(field.equals("pressure"))e=pressure;
        else if(field.equals("rain24"))e=rain24; else if(field.equals("et0"))e=et0; else if(field.equals("soilPh"))e=soilPh;
        else if(field.equals("soilEc"))e=soilEc; else if(field.equals("soilN"))e=soilN; else if(field.equals("soilP"))e=soilP; else if(field.equals("soilK"))e=soilK;
        else if(field.equals("soilMoisture"))e=soilMoisture; else if(field.equals("soilTemp"))e=soilTemp; else if(field.equals("soilOm"))e=soilOm;
        else if(field.equals("soilCec"))e=soilCec; else if(field.equals("soilEce"))e=soilEce; else if(field.equals("windSpeed"))e=windSpeed; else if(field.equals("par"))e=par;
        else if(field.equals("sunHours"))e=sunHours;
        if(e!=null) {
            String out=value.trim();
            double x=num(out);
            if(moisture && Double.isFinite(x)) {
                if (x >= 0 && x <= 1.0) out=String.format(Locale.US,"%.2f",x*100.0);
            }
            e.setText(out);
        }
        if(field.equals("vpd")) {
            // VPD is not an input widget in the field form; it is recomputed from T/RH by the engine.
            prefsSafeEdit("ts_vpd_override", value.trim());
        }
    }

    private void applyOpenMeteoFields(JSONObject root, boolean tsFirst, JSONObject tsMeta, JSONObject tsFeed) {
        try {
            JSONObject c=root.optJSONObject("current"), d=root.optJSONObject("daily");
            if(c==null) return;
            setOm(airTemp, c.optDouble("temperature_2m",Double.NaN), tsFirst && hasTs(tsMeta,tsFeed,new String[]{"suhu udara","air temp","temperature"},new String[]{"tanah","soil"}));
            setOm(airRh, c.optDouble("relative_humidity_2m",Double.NaN), tsFirst && hasTs(tsMeta,tsFeed,new String[]{"kelembapan udara","humidity","rh"},new String[]{"tanah","soil"}));
            setOm(pressure, c.optDouble("surface_pressure",Double.NaN), tsFirst && hasTs(tsMeta,tsFeed,new String[]{"tekanan","pressure","baro"},new String[]{}));
            setOm(rain24, d==null?Double.NaN:first(d,"precipitation_sum"), tsFirst && hasTs(tsMeta,tsFeed,new String[]{"hujan 24","rain","precip"},new String[]{}));
            setOm(et0, d==null?Double.NaN:first(d,"et0_fao_evapotranspiration"), tsFirst && hasTs(tsMeta,tsFeed,new String[]{"et0","evapotrans"},new String[]{}));
            setOm(soilTemp, c.optDouble("soil_temperature_0_to_10cm",Double.NaN), tsFirst && hasTs(tsMeta,tsFeed,new String[]{"suhu tanah","soil temp","soil_temperature"},new String[]{}));
            setOm(soilMoisture, c.optDouble("soil_moisture_0_to_10cm",Double.NaN)*100.0, tsFirst && hasTs(tsMeta,tsFeed,new String[]{"kelembapan tanah","soil moisture","soil_moisture","moisture"},new String[]{"udara","air"}));
            setOm(windSpeed, c.optDouble("wind_speed_10m",Double.NaN), tsFirst && hasTs(tsMeta,tsFeed,new String[]{"kecepatan angin","wind speed","wind"},new String[]{"arah","direction","gust"}));
            setOm(par, LightConversion.parToPpfd(LightConversion.shortwaveToParWm2(c.optDouble("shortwave_radiation",Double.NaN))), false);
            setOm(sunHours, d==null?Double.NaN:first(d,"sunshine_duration")/3600.0, tsFirst && hasTs(tsMeta,tsFeed,new String[]{"sunshine","lama penyinaran","durasi sinar"},new String[]{}));
            String dir=MainActivity.compass(c.optDouble("wind_direction_10m",Double.NaN)); if(!dir.equals("--") && !(tsFirst && hasTs(tsMeta,tsFeed,new String[]{"arah angin","wind direction","direction"},new String[]{}))) selectSpinner(windDirection,dir);
            prefs.edit().putString("om_temp", show(c.optDouble("temperature_2m",Double.NaN)))
                    .putString("om_rh", show(c.optDouble("relative_humidity_2m",Double.NaN)))
                    .putString("om_pressure", show(c.optDouble("surface_pressure",Double.NaN)))
                    .putString("om_rain", show(d==null?Double.NaN:first(d,"precipitation_sum")))
                    .putString("om_et0", show(d==null?Double.NaN:first(d,"et0_fao_evapotranspiration")))
                    .putString("om_soil_temp", show(c.optDouble("soil_temperature_0_to_10cm",Double.NaN)))
                    .putString("om_soil_moisture", show(c.optDouble("soil_moisture_0_to_10cm",Double.NaN)))
                    .putString("om_wind_speed", show(c.optDouble("wind_speed_10m",Double.NaN)))
                    .putString("om_wind_direction", dir)
                    .putString("om_vpd", show(c.optDouble("vapour_pressure_deficit",Double.NaN))).apply();
        } catch(Exception ignored) {}
    }

    private JSONObject openMeteoObjectFromPrefs(SharedPreferences prefs) {
        // Cache is already applied by MainActivity; this method exists as a no-network fallback.
        JSONObject root=new JSONObject(), c=new JSONObject(), d=new JSONObject();
        try {
            c.put("temperature_2m", num(prefs.getString("om_temp","")));
            c.put("relative_humidity_2m", num(prefs.getString("om_rh","")));
            c.put("surface_pressure", num(prefs.getString("om_pressure","")));
            c.put("soil_temperature_0_to_10cm", num(prefs.getString("om_soil_temp","")));
            c.put("soil_moisture_0_to_10cm", num(prefs.getString("om_soil_moisture","")));
            c.put("wind_speed_10m", num(prefs.getString("om_wind_speed","")));
            c.put("wind_direction_10m", windDegrees(prefs.getString("om_wind_direction","")));
            c.put("vapour_pressure_deficit", num(prefs.getString("om_vpd","")));
            JSONArray rain=new JSONArray(); rain.put(num(prefs.getString("om_rain",""))); d.put("precipitation_sum",rain);
            JSONArray et=new JSONArray(); et.put(num(prefs.getString("om_et0",""))); d.put("et0_fao_evapotranspiration",et);
            root.put("current",c).put("daily",d); return root;
        } catch(Exception e){return null;}
    }

    private double windDegrees(String s){
        String[] names={"utara","utara-timur laut","timur laut","timur-timur laut","timur","timur-tenggara","tenggara","selatan-tenggara","selatan","selatan-barat daya","barat daya","barat-barat daya","barat","barat-barat laut","barat laut","utara-barat laut"};
        String x=normalize(s); for(int i=0;i<names.length;i++) if(x.equals(normalize(names[i]))) return i*22.5; return Double.NaN;
    }

    private double first(JSONObject o,String key){JSONArray a=o.optJSONArray(key);return a==null||a.length()==0?Double.NaN:a.optDouble(0,Double.NaN);}
    private boolean hasTs(JSONObject meta, JSONObject feed, String[] aliases, String[] excludes) {
        if(meta==null || feed==null) return false;
        return !matchTs(meta,feed,aliases,excludes).isEmpty();
    }
    private void setOm(EditText e,double value,boolean blockedByTs){ if(!blockedByTs && Double.isFinite(value)) e.setText(show(value)); }
    private void setIfFinite(EditText e,double v){setIfFinite(e,v,false);}
    private void setIfFinite(EditText e,double v,boolean onlyEmpty){if(e!=null&&Double.isFinite(v)&&(!onlyEmpty||e.getText().toString().trim().isEmpty()))e.setText(show(v));}
    private void prefsSafeEdit(String k,String v){getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(k,v).apply();}
    private JSONObject httpJson(String url)throws Exception{
        java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
        c.setRequestMethod("GET"); c.setConnectTimeout(8000); c.setReadTimeout(12000); c.setUseCaches(false); c.setRequestProperty("Accept","application/json");
        int code=c.getResponseCode(); InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream(); String body=readAll(in); c.disconnect();
        if(code<200||code>=300)throw new Exception("HTTP "+code); if(body.trim().isEmpty())throw new Exception("Respons kosong"); return new JSONObject(body);
    }

    private String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[4096];
        int n;
        try {
            while ((n = in.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, n, java.nio.charset.StandardCharsets.UTF_8));
            }
        } finally {
            try { in.close(); } catch (Exception ignored) {}
        }
        return sb.toString();
    }

    private String compactAnalysis(String text){
        String[] lines=text.split("\\n"); StringBuilder out=new StringBuilder();
        boolean skipScientific=false;
        for(String line:lines){
            String t=line.trim();
            if(t.startsWith("8. DASAR ILMIAH")){skipScientific=true; continue;}
            if(skipScientific) continue;
            if(t.startsWith("Catatan model:") || t.startsWith("Interpretasi: status") || t.startsWith("• FAO-") || t.startsWith("• Sumber bukti") || t.startsWith("• STCR-style") || t.startsWith("• Model penyakit") || t.startsWith("• GDD/") || t.startsWith("• pH/kapur") || t.startsWith("• EC:")) continue;
            if(t.startsWith("PENTING: bukan QUEFTS")) continue;
            if(t.startsWith("URUTAN ANALISIS:")) continue;
            if(t.startsWith("Catatan: parameter")) continue;
            if(t.startsWith("SELESAI ANALISIS AWAL")) continue;
            out.append(line).append('\n');
        }
        return out.toString().trim();
    }

    /** Reorders the same calculated results into short, clearly separated sections for farmers. */
    private String formatFarmerFriendlyAnalysis(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Analisis belum tersedia. Lengkapi data kebun lalu tekan ANALISIS AGRONOMI SEKARANG.";
        String[] head = raw.split("\\n", 4);
        String sourceTitle = head.length > 0 ? head[0].trim() : "";
        String cropLabel = sourceTitle.contains("•") ? sourceTitle.substring(sourceTitle.indexOf('•') + 1).trim() : sourceTitle;
        String cultivationLine = head.length > 1 ? friendlyLine(head[1].replace("Budidaya:", "Cara budidaya:")) : "";
        String phaseLine = head.length > 2 ? friendlyLine(head[2].replace("Fase:", "Tahap tanaman:")) : "";

        String data = blockBetween(raw, "DASAR DATA & KETIDAKPASTIAN", "1. STATUS TANAH");
        String soil = blockBetween(raw, "1. STATUS TANAH", "2. ANALISIS RENTANG SEMUA PARAMETER");
        String range = blockBetween(raw, "2. ANALISIS RENTANG SEMUA PARAMETER", "3. KECUKUPAN NPK & PERKIRAAN DOSIS FASE");
        String fertilizer = blockBetween(raw, "3. KECUKUPAN NPK & PERKIRAAN DOSIS FASE", "4. AIR, VPD & CUACA");
        String weather = blockBetween(raw, "4. AIR, VPD & CUACA", "5. PREDIKSI POTENSI OPT");
        String opt = blockBetween(raw, "5. PREDIKSI POTENSI OPT (BERDASARKAN CUACA + RIWAYAT)", "6. CATATAN LAPANGAN TERARAH");
        String notes = blockBetween(raw, "6. CATATAN LAPANGAN TERARAH", "7. REKOMENDASI MENYELURUH UNTUK PETANI");
        String recommendations = blockAfter(raw, "7. REKOMENDASI MENYELURUH UNTUK PETANI");

        StringBuilder out = new StringBuilder();
        out.append("HASIL ANALISIS KEBUN");
        if (!cropLabel.isEmpty()) out.append(" • ").append(cropLabel);
        out.append("\n");
        if (!cultivationLine.isEmpty()) out.append(cultivationLine).append("\n");
        if (!phaseLine.isEmpty()) out.append(phaseLine).append("\n\n");
        out.append("Catatan: hasil ini membantu menentukan langkah awal. Jika data tanah belum lengkap, kebutuhan pupuk masih berupa perkiraan.\n\n");

        appendReadableSection(out, "1. SARAN YANG BISA DILAKUKAN", recommendations, true);
        appendReadableSection(out, "2. DATA DAN KEPASTIAN HASIL", friendlyProvenance(data), false, false);
        appendReadableSection(out, "3. KONDISI TANAH", soil, false);
        appendReadableSection(out, "4. PERKIRAAN KEBUTUHAN PUPUK", friendlyFertilizer(fertilizer), false);
        appendReadableSection(out, "5. AIR DAN CUACA", weather, false);
        appendReadableSection(out, "6. PERINGATAN DINI HAMA / PENYAKIT", opt, false);
        appendReadableSection(out, "7. CATATAN KEBUN", notes, false);
        appendReadableSection(out, "8. RINCIAN TAMBAHAN", range, false);
        out.append("\nARTI ISTILAH SEDERHANA\n");
        out.append("• pH = ukuran tanah masam atau basa.\n");
        out.append("• N, P, K = nitrogen, fosfor, dan kalium; tiga unsur hara penting bagi tanaman.\n");
        out.append("• mg/kg = miligram hara per kilogram tanah.\n");
        out.append("• µS/cm = ukuran daya hantar listrik tanah; dipakai sebagai petunjuk awal kadar garam, bukan pengganti uji laboratorium.\n");
        out.append("• kg/ha = kilogram per hektare. Angka kebutuhan hara tidak selalu sama dengan berat pupuk jadi yang harus diberikan; sesuaikan dengan kadar hara pada kemasan pupuk.\n");
        out.append("\nArti status: ‘rendah’, ‘sedang’, atau ‘tinggi’ adalah petunjuk awal. Jika tersedia, utamakan hasil laboratorium tanah dan anjuran petugas pertanian setempat.\n");
        return out.toString().replace("--", "belum ada data").trim();
    }

    private String friendlyFertilizer(String body) {
        if (body == null || body.trim().isEmpty()) return body;
        return "Angka di bawah adalah perkiraan kebutuhan unsur hara untuk tahap tanaman ini, bukan selalu jumlah pupuk jadi yang harus ditimbang.\n"
                + "Satuan kg/ha berarti kilogram per hektare. Jumlah pupuk jadi bergantung pada jenis dan kadar hara pupuk yang digunakan.\n"
                + body;
    }

    private String friendlyCommaList(String values) {
        if (values == null || values.trim().isEmpty()) return "belum ada";
        StringBuilder out = new StringBuilder();
        for (String item : values.split(",\\s*")) {
            if (out.length() > 0) out.append(" • ");
            out.append(friendlyLine(item.trim()));
        }
        return out.toString();
    }

    private String blockBetween(String source, String startHeader, String endHeader) {
        int start = source.indexOf(startHeader);
        if (start < 0) return "";
        start = source.indexOf('\n', start);
        if (start < 0) return "";
        start++;
        int end = source.indexOf(endHeader, start);
        if (end < 0) end = source.length();
        return source.substring(start, end).trim();
    }

    private String blockAfter(String source, String startHeader) {
        int start = source.indexOf(startHeader);
        if (start < 0) return "";
        start = source.indexOf('\n', start);
        if (start < 0) return "";
        return source.substring(start + 1).trim();
    }

    private void appendReadableSection(StringBuilder out, String heading, String body, boolean recommendationList) {
        appendReadableSection(out, heading, body, recommendationList, true);
    }

    private void appendReadableSection(StringBuilder out, String heading, String body, boolean recommendationList, boolean applyFriendlyLanguage) {
        if (heading == null || heading.trim().isEmpty()) return;
        out.append("\n").append(heading).append("\n\n");
        if (body == null || body.trim().isEmpty()) {
            out.append("Belum ada informasi yang cukup untuk bagian ini.\n\n");
            return;
        }
        String[] lines = body.split("\\n");
        boolean hasContent = false;
        for (String original : lines) {
            String line = original.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("ANALISIS AWAL — SEMUA PARAMETER")) line = "Pemeriksaan ringkas seluruh data yang tersedia:";
            if (applyFriendlyLanguage) line = friendlyLine(line);
            if (line.isEmpty()) continue;
            if (recommendationList && !line.startsWith("•") && !line.startsWith("-")) line = "• " + line;
            out.append(line).append("\n\n");
            hasContent = true;
        }
        if (!hasContent) out.append("Belum ada informasi yang cukup untuk bagian ini.\n\n");
    }

    /** Converts model/soil terms into common Indonesian while preserving measured numbers and outcomes. */
    private String friendlyLine(String line) {
        if (line == null) return "";
        String x = line.trim();
        String[][] replacements = {
            {"Kebutuhan screening fase ini", "Perkiraan kebutuhan tanaman pada tahap ini"},
            {"Kredit pupuk tercatat 90 hari", "Perkiraan hara dari pupuk yang tercatat dalam 90 hari terakhir"},
            {"STCR-style screening", "perkiraan kebutuhan hara"},
            {"Tingkat ketidakpastian rekomendasi", "Ketidakpastian hasil perkiraan"},
            {"Data asli tersedia (input pengguna/sensor/API/riwayat; periksa metode dan satuannya):", "Data asli yang sudah ada:"},
            {"Data belum tersedia:", "Data yang belum diisi atau belum ditemukan:"},
            {"Estimasi/default yang dipakai hanya di dalam perhitungan:", "Perkiraan atau nilai bawaan yang dipakai dalam hitungan:"},
            {"PENTING: nilai asli dari input, sensor USB/RS485, ThingSpeak/Open-Meteo, riwayat atau laboratorium tidak ditimpa oleh estimasi. Saat nilai asli baru/terbaru masuk, analisis dihitung ulang dengan nilai asli tersebut.", "Nilai asli dari pengguna, sensor, cuaca, riwayat, atau laboratorium tidak diganti oleh perkiraan. Jika data asli terbaru masuk, hasil akan dihitung ulang."},
            {"PENTING:", "Penting:"}, {"pH-buffer", "uji penyangga pH"}, {"Al-dd", "aluminium tanah (Al-dd)"},
            {"H-dd", "keasaman cadangan (H-dd)"}, {"CEC/KTK", "kemampuan tanah menahan hara (KTK)"}, {"CEC", "KTK"},
            {"bulk density", "berat isi tanah"}, {"Stok lapisan", "Perkiraan jumlah hara dalam lapisan tanah"},
            {"N-total diprediksi dari N tersedia", "Perkiraan N total dari N tersedia (bukan hasil laboratorium)"},
            {"Kelas N-total yang mungkin", "Perkiraan kelompok N total"},
            {"N tersedia", "Nitrogen (N) tersedia"}, {"P tersedia", "Fosfor (P) tersedia"}, {"K tersedia", "Kalium (K) tersedia"},
            {"ECe laboratorium", "ECe hasil laboratorium"}, {"ECe lab", "ECe hasil laboratorium"},
            {"EC sensor", "EC dari sensor tanah"}, {"EC:", "EC tanah:"},
            {"Kelembapan tanah sensor", "Kadar air tanah dari sensor"}, {"Kelembapan tanah", "Kadar air tanah"},
            {"Air tanah:", "Kondisi air di tanah:"}, {"FC/PWP", "batas air tanah (kapasitas lapang/titik layu)"},
            {"P2O5", "fosfor (P₂O₅)"}, {"K2O", "kalium (K₂O)"},
            {"screening", "perkiraan awal"}, {"VPD", "tekanan udara kering bagi tanaman (VPD)"},
            {"ET0", "perkiraan kebutuhan air dari cuaca (ET0)"}, {"ET₀", "perkiraan kebutuhan air dari cuaca (ET0)"},
            {"RH", "kelembapan udara"}, {"PAR", "cahaya untuk fotosintesis (PAR)"}, {"OPT", "hama/penyakit (OPT)"}, {"PHT", "pengendalian hama terpadu (PHT)"},
            {"HST", "hari setelah tanam"}, {"mg/kg", "mg/kg"}, {"kg/ha", "kg/ha"},
            {"→", " — "}, {"; ", ";\n• "}
        };
        for (String[] pair : replacements) x = x.replace(pair[0], pair[1]);
        x = x.replace("LEBIH RENDAH (tetap perkiraan awal; validasi lokal tetap diperlukan)", "Lebih rendah, tetapi tetap perlu pemeriksaan setempat.")
             .replace("LEBIH RENDAH (tetap screening; validasi lokal tetap diperlukan)", "Lebih rendah, tetapi tetap perlu pemeriksaan setempat.")
             .replace("SEDANG–TINGGI", "Sedang hingga tinggi — hasil masih perlu diperiksa.")
             .replace("SEDANG-TINGGI", "Sedang hingga tinggi — hasil masih perlu diperiksa.")
             .replace("TINGGI.", "Tinggi — hasil masih berupa gambaran awal.")
             .replace("kg/ha.", "kg/ha (kilogram per hektare).")
             .replace("jangan memakai hujan saja untuk memutuskan irigasi", "jangan menentukan penyiraman hanya dari jumlah hujan");
        return x;
    }

    private String friendlyProvenance(String data) {
        if (data == null || data.trim().isEmpty()) return "Belum ada ringkasan sumber data.";
        StringBuilder out = new StringBuilder();
        for (String original : data.split("\\n")) {
            String line = original.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("Data asli tersedia") || line.startsWith("Data asli yang sudah ada")) {
                int colon = line.indexOf(':');
                String values = colon >= 0 ? line.substring(colon + 1).trim() : "belum ada";
                values = values.replaceAll("\\.$", "");
                out.append("Data asli yang sudah tersedia:\n• ").append(friendlyCommaList(values)).append("\n\n");
            } else if (line.startsWith("Data belum tersedia") || line.startsWith("Data yang belum diisi")) {
                int colon = line.indexOf(':');
                String values = colon >= 0 ? line.substring(colon + 1).trim() : "belum diketahui";
                out.append("Data yang masih perlu dilengkapi:\n• ").append(friendlyCommaList(values)).append("\n\n");
            } else if (line.startsWith("Estimasi/default") || line.startsWith("Perkiraan atau nilai bawaan")) {
                int colon = line.indexOf(':');
                String values = colon >= 0 ? line.substring(colon + 1).trim() : line;
                out.append("Bagian yang masih memakai perkiraan:\n");
                for (String value : values.split(";\\s*")) {
                    String item = friendlyLine(value.trim());
                    if (!item.isEmpty()) out.append("• ").append(item).append("\n");
                }
                out.append("\n");
            } else if (line.startsWith("Tingkat ketidakpastian rekomendasi") || line.startsWith("Ketidakpastian hasil perkiraan")) {
                String lower = line.toLowerCase(Locale.US);
                if (lower.contains("lebih rendah")) {
                    out.append("Ketidakpastian: lebih rendah karena lebih banyak data tersedia. Hasil tetap berupa perkiraan dan perlu disesuaikan dengan kondisi kebun.\n\n");
                } else if (lower.contains("sedang")) {
                    out.append("Ketidakpastian: sedang sampai tinggi. Gunakan hasil ini sebagai panduan awal dan lengkapi data tanah jika memungkinkan.\n\n");
                } else if (lower.contains("tinggi")) {
                    out.append("Ketidakpastian: tinggi. Hasil ini hanya gambaran awal; lengkapi data tanah bila memungkinkan.\n\n");
                } else {
                    out.append("Ketidakpastian belum dapat ditentukan dengan jelas. Periksa data yang digunakan sebelum mengikuti rekomendasi.\n\n");
                }
            } else if (line.startsWith("PENTING:") || line.startsWith("Nilai asli")) {
                out.append(friendlyLine(line)).append("\n\n");
            } else {
                out.append(friendlyLine(line)).append("\n\n");
            }
        }
        return out.toString().trim();
    }

    /** Presents the same calculated result as spacious cards for readers with little technical background. */
    private void renderFarmerAnalysisCards(String text) {
        if (analysisCards == null) {
            analysisView.setVisibility(View.VISIBLE);
            analysisView.setText(styleFarmerFriendlyAnalysis(text));
            return;
        }
        analysisView.setVisibility(View.GONE);
        analysisCards.setVisibility(View.VISIBLE);
        analysisCards.removeAllViews();

        String safe = text == null ? "" : text.trim();
        String[] lines = safe.split("\\r?\\n");
        String title = lines.length > 0 && !lines[0].trim().isEmpty() ? lines[0].trim() : "HASIL ANALISIS KEBUN";
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(0xFF29C6C7);
        titleView.setTextSize(20);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setLineSpacing(dp(3), 1.05f);
        titleView.setPadding(dp(2), dp(4), dp(2), dp(5));
        analysisCards.addView(titleView, matchWrap());

        // Crop, cultivation mode and plant stage are compact metadata, not part of the long report.
        StringBuilder meta = new StringBuilder();
        int cursor = 1;
        for (; cursor < lines.length && cursor < 4; cursor++) {
            String line = lines[cursor].trim();
            if (line.isEmpty() || isFarmerSectionHeader(line) || line.startsWith("Catatan:")) break;
            if (meta.length() > 0) meta.append("\n");
            meta.append(line);
        }
        if (meta.length() > 0) addMetaCard(meta.toString());

        StringBuilder intro = new StringBuilder();
        int index = cursor;
        while (index < lines.length && !isFarmerSectionHeader(lines[index].trim())) {
            String line = lines[index].trim();
            if (!line.isEmpty()) {
                if (intro.length() > 0) intro.append(" ");
                intro.append(line);
            }
            index++;
        }
        if (intro.length() > 0) addInfoCard("SEBELUM MENGIKUTI SARAN", intro.toString(), 0xFFFFD479);

        String currentHeading = null;
        ArrayList<String> bodyLines = new ArrayList<>();
        for (; index < lines.length; index++) {
            String line = lines[index].trim();
            if (isFarmerSectionHeader(line)) {
                if (currentHeading != null) addAnalysisSectionCard(currentHeading, bodyLines);
                currentHeading = line;
                bodyLines = new ArrayList<>();
            } else if (!line.isEmpty()) {
                bodyLines.add(line);
            }
        }
        if (currentHeading != null) addAnalysisSectionCard(currentHeading, bodyLines);
        if (analysisCards.getChildCount() <= 1) {
            addInfoCard("HASIL BELUM TERSEDIA", "Lengkapi data kebun lalu tekan tombol ANALISIS AGRONOMI SEKARANG.", 0xFFFFD479);
        }
    }

    private boolean isFarmerSectionHeader(String line) {
        return line != null && (line.matches("^[1-9][0-9]*\\. .+") || line.equalsIgnoreCase("ARTI ISTILAH SEDERHANA"));
    }

    private void addMetaCard(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xFFC4D3DB);
        view.setTextSize(14);
        view.setLineSpacing(dp(3), 1.08f);
        view.setPadding(dp(10), dp(7), dp(10), dp(7));
        view.setBackground(roundBackground(0xFF102B3C, 0xFF214457));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.bottomMargin = dp(8);
        analysisCards.addView(view, lp);
    }

    private void addAnalysisSectionCard(String heading, ArrayList<String> bodyLines) {
        boolean priority = heading.startsWith("1.");
        int accent = priority ? 0xFFF3B64A : 0xFF29C6C7;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(roundBackground(priority ? 0xFF193747 : 0xFF122B3B, priority ? 0xFF80612C : 0xFF214457));
        LinearLayout.LayoutParams cardLp = matchWrap();
        cardLp.topMargin = dp(9);
        analysisCards.addView(card, cardLp);

        TextView title = new TextView(this);
        title.setText(cleanSectionHeading(heading));
        title.setTextColor(accent);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLineSpacing(dp(2), 1.04f);
        title.setPadding(0, 0, 0, dp(7));
        card.addView(title, matchWrap());

        if (bodyLines == null || bodyLines.isEmpty()) {
            addBodyParagraph(card, "Belum ada informasi yang cukup untuk bagian ini.", false);
            return;
        }
        for (String item : bodyLines) {
            String line = item == null ? "" : item.trim();
            if (line.isEmpty()) continue;
            // Break apart bullet lists that were returned on one line by a data source.
            String[] split = line.split("\\n");
            for (String sub : split) {
                String content = sub.trim();
                if (content.isEmpty()) continue;
                boolean bullet = content.startsWith("•") || content.startsWith("-");
                if (bullet) content = content.replaceFirst("^[•-]\\s*", "");
                addBodyParagraph(card, content, bullet);
            }
        }
    }

    private String cleanSectionHeading(String heading) {
        if (heading == null) return "Hasil analisis";
        String value = heading.replaceFirst("^[0-9]+\\.\\s*", "").trim();
        value = value.replace("SARAN YANG BISA DILAKUKAN", "SARAN UNTUK KEBUN ANDA")
                .replace("DATA DAN KEPASTIAN HASIL", "DATA YANG DIGUNAKAN")
                .replace("PERKIRAAN KEBUTUHAN PUPUK", "PERKIRAAN KEBUTUHAN PUPUK")
                .replace("PERINGATAN DINI HAMA / PENYAKIT", "WASPADA HAMA DAN PENYAKIT")
                .replace("RINCIAN TAMBAHAN", "RINCIAN TAMBAHAN (BOLEH DIBACA BELAKANGAN)");
        return value;
    }

    private void addBodyParagraph(LinearLayout parent, String text, boolean bullet) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams rowLp = matchWrap();
        rowLp.topMargin = dp(3);
        rowLp.bottomMargin = dp(5);
        parent.addView(row, rowLp);

        if (bullet) {
            TextView marker = new TextView(this);
            marker.setText("•");
            marker.setTextColor(0xFF45D483);
            marker.setTextSize(17);
            marker.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            marker.setPadding(0, 0, dp(8), 0);
            row.addView(marker, new LinearLayout.LayoutParams(dp(18), -2));
        }
        TextView body = new TextView(this);
        body.setText(text);
        body.setTextColor(0xFFF0F4F6);
        body.setTextSize(15.5f);
        body.setLineSpacing(dp(5), 1.08f);
        body.setIncludeFontPadding(true);
        row.addView(body, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private void addInfoCard(String heading, String body, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(11));
        card.setBackground(roundBackground(0xFF173546, 0xFF3E5A67));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(7);
        lp.bottomMargin = dp(2);
        analysisCards.addView(card, lp);
        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextColor(accent);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title, matchWrap());
        addBodyParagraph(card, body, false);
    }

    private GradientDrawable roundBackground(int color, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(13));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void renderFarmerAnalysisError(String message) {
        if (analysisView != null) analysisView.setVisibility(View.GONE);
        if (analysisCards == null) {
            analysisView.setVisibility(View.VISIBLE);
            analysisView.setText(message);
            return;
        }
        analysisCards.setVisibility(View.VISIBLE);
        analysisCards.removeAllViews();
        addInfoCard("ANALISIS BELUM BERHASIL", message, 0xFFFFD479);
    }

    private CharSequence styleFarmerFriendlyAnalysis(String text) {
        SpannableString styled = new SpannableString(text == null ? "" : text);
        if (styled.length() == 0) return styled;
        String value = styled.toString();
        int lineStart = 0;
        int lineNumber = 0;
        while (lineStart < value.length()) {
            int lineEnd = value.indexOf('\n', lineStart);
            if (lineEnd < 0) lineEnd = value.length();
            String line = value.substring(lineStart, lineEnd).trim();
            if (!line.isEmpty() && (lineNumber == 0 || line.equals("ARTI ISTILAH SEDERHANA") || line.matches("^[0-9]+\\. .*"))) {
                styled.setSpan(new StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new ForegroundColorSpan(0xFF29C6C7), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new RelativeSizeSpan(lineNumber == 0 ? 1.20f : 1.08f), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            lineStart = lineEnd + 1;
            lineNumber++;
        }
        return styled;
    }

    private void showMethodNotes(){
        LinearLayout root=dialogRoot();
        TextView tv=new TextView(this);
        tv.setTextColor(0xFFFFFFFF); tv.setTextSize(14); tv.setPadding(4,4,4,4);
        tv.setText(""+
                "CATATAN & DASAR PERHITUNGAN\\n\\n"+
                "1) EC sensor\\n"+
                "Nilai EC sensor dalam µS/cm dianggap sebagai data masukan yang benar untuk skrining dan pemantauan. Konversi satuan hanya µS/cm ÷ 1000 = dS/m. Kelas operasional aplikasi: <1; 1–2; >2–3; >3–4; >4 dS/m. ECe laboratorium hanya data pembanding bila tersedia dan bukan syarat analisis. Tidak ada faktor konversi universal dari setiap EC sensor lapang ke ECe karena hubungan dipengaruhi metode ekstraksi, tekstur, kadar air, suhu, bulk density dan kondisi tanah.\\n\\n"+
                "2) Kelembapan Open-Meteo\\n"+
                "Open-Meteo memberi soil moisture dalam m³/m³. Aplikasi mengubahnya menjadi % volume dengan ×100 sebelum ditampilkan.\\n\\n"+
                "3) Bulk density\\n"+
                "Default tanah mineral = 1,30 g/cm³; preset gambut = 0,30 g/cm³; pengguna tetap dapat memilih Input sendiri. Nilai preset adalah asumsi referensi, bukan hasil pengukuran.\\n\\n"+
                "4) Stok hara\\n"+
                "Stok lapisan dihitung dari konsentrasi (mg/kg) × bulk density (g/cm³) × kedalaman (cm) × 0,10 = kg/ha. Stok bukan otomatis sama dengan serapan tanaman.\\n\\n"+
                "5) VPD & cuaca\\n"+
                "VPD menggunakan data suhu dan RH. ET₀ berasal dari Open-Meteo/FAO-56 pada sumber yang tersedia. Hujan, ET₀, kelembapan tanah, angin dan VPD dibaca bersama; satu parameter tidak dipakai sendirian untuk keputusan irigasi/OPT.\\n\\n"+
                "6) N/P/K dan pH\\n"+
                "Nilai sensor/lab yang dimasukkan diperlakukan sebagai data nyata. Kelas/rentang adalah alat interpretasi; metode ekstraksi tetap dicatat karena dapat mengubah ambang. Kebutuhan kapur tidak dihitung dari pH saja bila data buffer/Al-dd/H-dd/CEC atau rekomendasi lab tersedia.\\n\\n"+
                "7) OpenAlex\\n"+
                "Analisis AI tetap dapat memakai OpenAlex untuk menemukan literatur ilmiah relevan. Hasil literatur dipakai sebagai dukungan bukti, bukan pengganti data lapang.\\n\\n"+
                "Rujukan EC/ECe: ScienceDirect, Pedosphere 32(6), 2022, DOI 10.1016/j.pedsph.2022.06.023; Journal of the Saudi Society of Agricultural Sciences 23(4), 2024, DOI 10.1016/j.jssas.2023.12.005."
        );
        root.addView(tv,new LinearLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle("NOTE — METODE & RUMUS").setView(wrap(root)).setPositiveButton("TUTUP",null).show();
    }

    private void showHistoryManager(SharedPreferences prefs){
        String[] labels={"Catatan lapangan","Riwayat pemupukan","Riwayat OPT / pengendalian"};
        new AlertDialog.Builder(this).setTitle("KELOLA / HAPUS HISTORI").setItems(labels,(d,which)->{
            if(which==0) showDeleteHistoryDialog(prefs,KEY_NOTES,"Catatan lapangan",true);
            else if(which==1) showDeleteHistoryDialog(prefs,KEY_FERT,"Riwayat pemupukan",false);
            else showDeleteHistoryDialog(prefs,KEY_OPT,"Riwayat OPT / pengendalian",false);
        }).setNegativeButton("TUTUP",null).show();
    }

    private void showDeleteHistoryDialog(SharedPreferences prefs,String key,String title,boolean notes){
        try{
            JSONArray a=new JSONArray(prefs.getString(key,"[]"));
            if(a.length()==0){Toast.makeText(this,"Tidak ada histori untuk dihapus.",Toast.LENGTH_SHORT).show();return;}
            String[] items=new String[a.length()];
            for(int i=0;i<a.length();i++){
                JSONObject o=a.optJSONObject(i); if(o==null){items[i]="#"+(i+1);continue;}
                String date=o.optString("date","--"), body;
                if(notes) body=o.optString("crop","Tanaman")+" • "+o.optString("type","catatan")+" • "+o.optString("observation","");
                else if(key.equals(KEY_FERT)) body=o.optString("crop","Tanaman")+" • "+o.optString("product","pupuk")+" • "+o.optDouble("dose",0)+" "+o.optString("unit","");
                else body=o.optString("crop","Tanaman")+" • "+o.optString("target","OPT")+" • "+o.optString("method","");
                if(body.length()>90) body=body.substring(0,90)+"…";
                items[i]=date+" — "+body;
            }
            boolean[] checked=new boolean[a.length()];
            AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Pilih yang akan dihapus\n"+title)
                    .setMultiChoiceItems(items,checked,(dialog,which,isChecked)->checked[which]=isChecked)
                    .setNegativeButton("BATAL",null)
                    .setNeutralButton("HAPUS SEMUA",null)
                    .setPositiveButton("HAPUS DIPILIH",null).create();
            dlg.setOnShowListener(v->{
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{
                    try{JSONArray out=new JSONArray();int removed=0;for(int i=0;i<a.length();i++){if(checked[i]){removed++;continue;}out.put(a.get(i));}prefs.edit().putString(key,out.toString()).apply();Toast.makeText(this,removed+" item dihapus.",Toast.LENGTH_SHORT).show();refreshViews(prefs);runAnalysis(prefs);dlg.dismiss();}catch(Exception e){Toast.makeText(this,"Gagal menghapus item.",Toast.LENGTH_SHORT).show();}
                });
                dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(x->{
                    new AlertDialog.Builder(this).setTitle("Hapus semua histori?").setMessage(title+" akan dikosongkan.")
                            .setNegativeButton("BATAL",null).setPositiveButton("HAPUS",(dd,ww)->{prefs.edit().remove(key).apply();Toast.makeText(this,"Semua histori dihapus.",Toast.LENGTH_SHORT).show();refreshViews(prefs);runAnalysis(prefs);dlg.dismiss();}).show();
                });
            });
            dlg.show();
        }catch(Exception e){Toast.makeText(this,"Histori tidak dapat dibaca.",Toast.LENGTH_SHORT).show();}
    }

    private void runAnalysis(SharedPreferences prefs) {
        try {
            String c = crop.getText().toString().trim();
            AgronomyEngine.CropProfile p = AgronomyEngine.profile(c);
            int days = hstInt();
            double ph = num(soilPh.getText().toString()), ec = num(soilEc.getText().toString()),
                    moist = num(soilMoisture.getText().toString()), st = num(soilTemp.getText().toString());
            double n = num(soilN.getText().toString()), pp = num(soilP.getText().toString()), k = num(soilK.getText().toString());
            double depth = num(soilDepth.getText().toString()), bd = num(soilBulkDensity.getText().toString());
            double nLow = num(soilNLow.getText().toString());
            double nHigh = num(soilNHigh.getText().toString());
            double at = num(airTemp.getText().toString()), rh = num(airRh.getText().toString()), pressureHpa = num(pressure.getText().toString()), rain = num(rain24.getText().toString()), e0 = num(et0.getText().toString()), luxVal = num(lux.getText().toString()), parVal = num(par.getText().toString()), sunVal = num(sunHours.getText().toString()), wind = num(windSpeed.getText().toString());
            double vpd = AgronomyEngine.vpd(at, rh);
            double[] credit = fertilizerCredit(prefs, c, 90);
            double targetYieldValue = num(targetYield.getText().toString());

            StringBuilder s = new StringBuilder();
            s.append("ANALISIS AGRONOMI TERPADU • ").append(p.name).append("\n");
            s.append("Budidaya: ").append(cultivation.getSelectedItem()).append("\n");
            s.append("Fase: ").append(AgronomyEngine.phase(days, c)).append(" (HST ").append(days >= 0 ? days : "-").append(")\n\n");
            appendDataQualityAndProvenance(s, c, days, ph, ec, moist, n, pp, k, targetYieldValue,
                    depth, bd, at, rh, rain, e0, prefs);

            s.append("1. STATUS TANAH\n");
            s.append("pH: ").append(show(ph)).append(" → ").append(AgronomyEngine.classifyPH(ph, p)).append("\n");
            String methodName = soilTestMethod.getSelectedItem().toString();
            double phBuffer = num(soilPhBuffer.getText().toString()), alDd = num(soilAlDd.getText().toString()), hDd = num(soilHDd.getText().toString()), cec = num(soilCec.getText().toString()), om = num(soilOm.getText().toString());
            double ece = num(soilEce.getText().toString()), limeLab = num(soilLimeReq.getText().toString());
            double fc = num(soilFc.getText().toString()), pwp = num(soilPwp.getText().toString());
            s.append("Metode uji tanah: ").append(methodName).append("\n");
            s.append("pH-buffer: ").append(show(phBuffer)).append("; Al-dd: ").append(show(alDd)).append("; H-dd: ").append(show(hDd)).append("; CEC: ").append(show(cec)).append("; bahan organik: ").append(show(om)).append(" %\n");
            s.append("Tindakan pH: ").append(AgronomyEngine.limeAdvice(ph, p, cultivation.getSelectedItem().toString(), phBuffer, alDd, hDd, cec, om, limeLab)).append("\n");
            if (Double.isFinite(ece)) s.append("ECe laboratorium: ").append(show(ece)).append(" dS/m → ").append(AgronomyEngine.classifyECe(ece)).append("\n");
            s.append("EC sensor: ").append(show(ec)).append(" µS/cm → ").append(AgronomyEngine.classifyEC(ec, p)).append("\n");
            if (Double.isFinite(depth) && Double.isFinite(bd)) {
                s.append("Stok lapisan ").append(show(depth)).append(" cm; bulk density ").append(show(bd)).append(" g/cm³: N=").append(show(AgronomyEngine.soilStockKgHa(n,bd,depth))).append(" kg/ha; P=").append(show(AgronomyEngine.soilStockKgHa(pp,bd,depth))).append(" kg/ha; K=").append(show(AgronomyEngine.soilStockKgHa(k,bd,depth))).append(" kg/ha\n");
            }
            s.append("Air tanah: ").append(AgronomyEngine.soilWaterAssessment(moist, fc, pwp, Math.max(1, depth), e0, c)).append("\n");
            if (!Double.isNaN(ph)) s.append("  Tindakan: ").append(AgronomyEngine.amendmentAdvice(ph, p, cultivation.getSelectedItem().toString())).append("\n");
            s.append("N tersedia: ").append(show(n)).append(" mg/kg → ").append(AgronomyEngine.classifyN(n,nLow,nHigh)).append("\n");
            s.append("N-total diprediksi dari N tersedia: ").append(NitrogenInference.estimateTotalN(n)).append("\n");
            s.append("Kelas N-total yang mungkin: ").append(NitrogenInference.classifyRange(n)).append("\n");
            s.append("P tersedia: ").append(show(pp)).append(" mg/kg → ").append(AgronomyEngine.classifyP(pp, methodName)).append("\n");
            s.append("K tersedia: ").append(show(k)).append(" mg/kg → ").append(AgronomyEngine.classifyK(k, methodName)).append("\n");
            s.append("EC: ").append(show(ec)).append(" µS/cm → ").append(AgronomyEngine.classifyEC(ec, p)).append("\n");
            s.append("Kelembapan tanah: ").append(show(moist)).append(" % → ").append(AgronomyEngine.classifyMoisture(moist, c)).append("\n");
            if (!Double.isNaN(st)) s.append("Suhu tanah: ").append(show(st)).append(" °C\n");

            double nNeed = AgronomyEngine.nutrientNeed(n, "N", c, days, credit[0], targetYieldValue, methodName, depth, bd, nLow, nHigh);
            double pNeed = AgronomyEngine.nutrientNeed(pp, "P", c, days, credit[1], targetYieldValue, methodName, depth, bd, nLow, nHigh);
            double kNeed = AgronomyEngine.nutrientNeed(k, "K", c, days, credit[2], targetYieldValue, methodName, depth, bd, nLow, nHigh);
            s.append("\n2. ANALISIS RENTANG SEMUA PARAMETER\n");
            s.append(AgronomyEngine.comprehensiveRangeAnalysis(
                    c, ph, n, nLow, nHigh, pp, k, ec, ece, moist, fc, pwp,
                    num(prefs.getString("om_soil_moisture","")), st, om, cec, bd, depth, at, Double.NaN, Double.NaN, rh, pressureHpa,
                    rain, e0, wind, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    luxVal, Double.NaN, Double.NaN, parVal, sunVal, vpd, methodName));
            s.append("\nFC/PWP: ").append(show(fc)).append(" / ").append(show(pwp)).append(" % volume → ")
                    .append(AgronomyEngine.soilWaterAssessment(moist, fc, pwp, Math.max(1, depth), e0, c)).append("\n");
            if (!Double.isNaN(phBuffer)) s.append("pH-buffer: ").append(show(phBuffer)).append(" → interpretasi mengikuti metode uji yang digunakan.\n");
            if (!Double.isNaN(alDd)) s.append("Al-dd: ").append(show(alDd)).append(" → interpretasi mengikuti metode laboratorium/lokasi.\n");
            if (!Double.isNaN(hDd)) s.append("H-dd: ").append(show(hDd)).append(" → interpretasi mengikuti metode laboratorium/lokasi.\n");
            s.append("\n3. KECUKUPAN NPK & PERKIRAAN DOSIS FASE\n");
            s.append("Kebutuhan screening fase ini: N ").append(show(nNeed)).append(" kg/ha, P2O5 ").append(show(pNeed)).append(" kg/ha, K2O ").append(show(kNeed)).append(" kg/ha.\n");
            s.append("Rumus screening: ").append(AgronomyEngine.nutrientFormulaText()).append("\n");
            if ("Komoditas umum".equals(p.name)) s.append("PERHATIAN: komoditas ini belum memiliki profil kebutuhan spesifik di aplikasi; angka memakai baseline umum dan tingkat ketidakpastian tinggi. Verifikasi dengan rekomendasi penyuluh/laboratorium setempat.\n");
            s.append("Kredit pupuk tercatat 90 hari (jumlah hara nominal dari label): N ").append(show(credit[0])).append(", P2O5 ").append(show(credit[1])).append(", K2O ").append(show(credit[2])).append(" kg/ha; model hanya mengkreditkan fraksi pemulihan estimasi.\n");
            s.append("Interpretasi: status rendah → peluang respons pupuk lebih besar; sedang → dosis sebaiknya berimbang; tinggi → jangan menambah unsur tersebut tanpa alasan agronomis.\n");
            s.append("Catatan model: dosis di atas adalah STCR-style screening, bukan dosis legal/spesifik kabupaten. Bila tersedia rekomendasi PUTS, peta status hara, petak omisi, atau persamaan STCR lokal, gunakan itu sebagai prioritas.\n");

            s.append("\n4. AIR, VPD & CUACA\n");
            if (!Double.isNaN(vpd)) {
                s.append("VPD: ").append(show(vpd)).append(" kPa -> ").append(AgronomyEngine.classifyVpd(vpd)).append("\n");
                s.append("VPD + tanah: ").append(AgronomyEngine.vpdCombinedStatus(vpd,moist,fc,pwp,depth,e0,c)).append("\n");
            } else {
                s.append("VPD: data suhu + RH belum lengkap.\n");
            }
            s.append(AgronomyEngine.weatherStatus(at, rh, rain, e0, vpd, c)).append("\n");
            if (!Double.isNaN(rain) && !Double.isNaN(e0)) s.append("Neraca sederhana hujan-ET0: ").append(show(rain - e0)).append(" mm; gunakan bersama kelembapan tanah, jangan memakai hujan saja untuk memutuskan irigasi.\n");
            if (!Double.isNaN(pressureHpa)) s.append("Tekanan udara: ").append(show(pressureHpa)).append(" hPa. Tekanan tunggal tidak menentukan hujan/OPT; gunakan bersama tren.\n");
            if (!Double.isNaN(luxVal)) s.append("Cahaya: ").append(show(luxVal)).append(" lux. ");
            if (!Double.isNaN(parVal)) s.append("PAR: ").append(show(parVal)).append(" µmol m⁻² s⁻¹. ");
            if (!Double.isNaN(sunVal)) s.append("Lama penyinaran: ").append(show(sunVal)).append(" jam/hari.\n");
            s.append("Arah angin: ").append(windDirection.getSelectedItem()).append("; kecepatan: ").append(show(wind)).append(" m/s.\n");
            s.append("Kisaran suhu rujukan screening komoditas: ").append(show(p.tempMin)).append("–").append(show(p.tempMax)).append(" °C.\n");

            s.append("\n5. PREDIKSI POTENSI OPT (BERDASARKAN CUACA + RIWAYAT)\n");
            s.append(AgronomyEngine.optRisk(c, at, rh, rain, wind, recentOptSummary(prefs, c))).append("\n");
            s.append("Penting: skor cuaca adalah peringatan dini, bukan diagnosis. Konfirmasi dengan gejala, populasi, luas serangan, dan keberadaan musuh alami.\n");

            s.append("\n6. CATATAN LAPANGAN TERARAH\n");
            s.append("Isi 1 paragraf dengan pola: [tanaman + fase] [petak/lokasi] [gejala/OPT] [luas/populasi] [cuaca/tanah] [tindakan] [hasil].\n");
            String obs = observation.getText().toString().trim();
            String act = action.getText().toString().trim();
            if (!obs.isEmpty()) s.append("Isi terbaru: ").append(obs.replace("\n", " ")).append("\n");
            if (!act.isEmpty()) s.append("Tindakan: ").append(act.replace("\n", " ")).append("\n");

            s.append("\n7. REKOMENDASI MENYELURUH UNTUK PETANI\n");
            appendRecommendations(s, c, p, ph, ec, moist, n, pp, k, at, rh, rain, e0, vpd, days, cultivation.getSelectedItem().toString(), nLow, nHigh, methodName);

            s.append("\n8. DASAR ILMIAH YANG DIPAKAI\n");
            s.append("• FAO-56 Penman–Monteith untuk ET0 dan Kc/ETc.\n");
            s.append("• Sumber bukti untuk AI: ").append(AgronomyEngine.evidenceCitations()).append("\n");
            s.append("• FAO/IRRI/Permentan Indonesia untuk status hara dan kebutuhan pemupukan spesifik lokasi.\n");
            s.append("• STCR-style: kebutuhan tanaman dikurangi kontribusi tanah, kredit pupuk, lalu dibagi efisiensi pemulihan; koefisien lokal harus diutamakan bila tersedia.\n");
            s.append("• Model penyakit: suhu, RH, hujan, dan periode basah/leaf wetness sebagai indikator risiko; model spesifik perlu parameter patogen setempat.\n");
            s.append("• GDD/thermal time untuk fase tanaman dan perkembangan serangga bila parameter Tbase tersedia.\n");
            s.append("• pH/kapur: kebutuhan dosis tidak ditentukan dari pH saja; gunakan pH-buffer/kemasaman tertukar/Al-dd atau uji kebutuhan kapur.\n");
            s.append("• EC sensor: gunakan sebagai parameter input utama dengan rentang operasional; ECe lab hanya pembanding bila tersedia.\n");

            String compact = compactAnalysis(s.toString());
            String readable = formatFarmerFriendlyAnalysis(compact);
            renderFarmerAnalysisCards(readable);
            // Keep the technical source text too; the readable version is used by the PDF/report.
            prefs.edit().putString("last_field_analysis_raw", compact)
                    .putString("last_field_analysis", readable).apply();
            soilSummaryView.setText(buildSoilSummary());
            timelineView.setText(buildTimeline(prefs, c));
        } catch (Exception ex) {
            renderFarmerAnalysisError("Analisis belum berhasil dihitung. Periksa kembali data kebun yang diisi.\n\nRincian: " + (ex.getMessage() == null ? "data belum lengkap" : ex.getMessage()));
        }
    }

    /** Recompute recommendations from the current form without replacing user/sensor values with estimates. */
    private void attachAutomaticRecalculation() {
        EditText[] watched = {crop, plantDate, hst, area, targetYield, soilPh, soilMoisture, soilTemp,
                soilEc, soilN, soilP, soilK, soilDepth, soilBulkDensity, soilFc, soilPwp,
                soilPhBuffer, soilAlDd, soilHDd, soilCec, soilOm, soilEce, soilLimeReq,
                soilNLow, soilNHigh, airTemp, airRh, pressure, rain24, et0, lux, par, sunHours, windSpeed};
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable e) {
                updateAutoPhase();
                scheduleAnalysisRecalculation();
            }
        };
        for (EditText field : watched) if (field != null) field.addTextChangedListener(watcher);
        AdapterView.OnItemSelectedListener recalcOnSelection = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                scheduleAnalysisRecalculation();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        soilTestMethod.setOnItemSelectedListener(recalcOnSelection);
        cultivation.setOnItemSelectedListener(recalcOnSelection);
        ageUnit.setOnItemSelectedListener(recalcOnSelection);
    }

    private void scheduleAnalysisRecalculation() {
        recalculationHandler.removeCallbacks(pendingRecalculation);
        recalculationHandler.postDelayed(pendingRecalculation, 450L);
    }

    /** Makes data provenance and uncertainty visible; estimates stay inside the model and never overwrite source fields. */
    private void appendDataQualityAndProvenance(StringBuilder s, String cropName, int hstDays,
            double ph, double ec, double moisture, double n, double p, double k, double targetYieldValue,
            double depth, double bulkDensity, double airTempValue, double rh, double rain, double et0Value,
            SharedPreferences prefs) {
        int observed = 0;
        StringBuilder actual = new StringBuilder(), missing = new StringBuilder(), assumptions = new StringBuilder();
        boolean cropOk = cropName != null && !cropName.trim().isEmpty();
        if (cropOk) { observed++; append(actual, "komoditas"); } else appendMissing(missing, "komoditas");
        appendPresent(actual, "umur/HST", hstDays >= 0); if (hstDays < 0) appendMissing(missing, "umur/HST"); else observed++;
        boolean phOk=Double.isFinite(ph), ecOk=Double.isFinite(ec), mOk=Double.isFinite(moisture), nOk=Double.isFinite(n), pOk=Double.isFinite(p), kOk=Double.isFinite(k);
        if (phOk) { observed++; append(actual,"pH"); } else appendMissing(missing,"pH");
        if (ecOk) { observed++; append(actual,"EC"); } else appendMissing(missing,"EC");
        if (mOk) { observed++; append(actual,"kelembapan tanah"); } else appendMissing(missing,"kelembapan tanah");
        if (nOk) { observed++; append(actual,"N tersedia"); } else appendMissing(missing,"N tersedia");
        if (pOk) { observed++; append(actual,"P tersedia"); } else appendMissing(missing,"P tersedia");
        if (kOk) { observed++; append(actual,"K tersedia"); } else appendMissing(missing,"K tersedia");
        if (Double.isFinite(targetYieldValue) && targetYieldValue > 0) { observed++; append(actual,"target hasil"); }
        else { appendMissing(missing,"target hasil"); assumptions.append("target hasil kosong → skala hasil 1,0 (baseline model); "); }
        if (area != null && Double.isFinite(num(area.getText().toString())) && num(area.getText().toString()) > 0) { observed++; append(actual,"luas lahan"); }
        else appendMissing(missing,"luas lahan");
        if (prefs.contains("fert_history") && !prefs.getString("fert_history", "[]").equals("[]")) { observed++; append(actual,"riwayat pemupukan"); }
        else { appendMissing(missing,"riwayat pemupukan"); assumptions.append("kredit pupuk = 0 bila riwayat tidak tersedia; "); }
        boolean defaultBd = bulkDensityPreset != null && bulkDensityPreset.getSelectedItemPosition() < 2;
        if (defaultBd) assumptions.append("bulk density ").append(show(bulkDensity)).append(" g/cm³ = preset/asumsi jenis tanah, bukan pengukuran; ");
        if (!Double.isFinite(depth) || depth <= 0) assumptions.append("kedalaman efektif belum ada; perhitungan stok tanah tidak dipakai; ");
        else if (!prefs.contains("soil_depth_cm")) assumptions.append("kedalaman 20 cm = default model sampai pengguna memasukkan nilai; ");
        if (!nOk || !pOk || !kOk) assumptions.append("N/P/K tanah yang kosong tidak diisi angka palsu; kebutuhan pupuk memakai baseline komoditas + fase dan asumsi konservatif kontribusi tanah nol untuk unsur tanpa hasil uji; ");
        if (!Double.isFinite(ph)) assumptions.append("pH tidak diestimasi numerik; rekomendasi koreksi pH dibatasi pada tindakan aman; ");
        if (!Double.isFinite(ec)) assumptions.append("EC tidak diestimasi numerik; evaluasi salinitas ditunda sampai ada hasil; ");
        if (!Double.isFinite(airTempValue) || !Double.isFinite(rh)) assumptions.append("VPD/komponen cuaca yang memerlukan suhu dan RH dihitung hanya jika keduanya tersedia; ");
        AgronomyEngine.CropProfile cp = AgronomyEngine.profile(cropName);
        assumptions.append("baseline kebutuhan musim ").append(cp.name).append(" (N/P/K ")
                .append(show(cp.seasonalN)).append("/").append(show(cp.seasonalP)).append("/").append(show(cp.seasonalK))
                .append(" kg/ha) dan fraksi fase = asumsi model agronomi; bukan hasil laboratorium.");
        String uncertainty = observed >= 10 && phOk && ecOk && nOk && pOk && kOk && Double.isFinite(targetYieldValue)
                ? "LEBIH RENDAH (tetap screening; validasi lokal tetap diperlukan)"
                : observed >= 6 ? "SEDANG–TINGGI" : "TINGGI";
        s.append("DASAR DATA & KETIDAKPASTIAN\n");
        s.append("Data asli tersedia (input pengguna/sensor/API/riwayat; periksa metode dan satuannya): ")
                .append(actual.length()==0 ? "belum ada" : actual.toString()).append(".\n");
        s.append("Data belum tersedia: ").append(missing.length()==0 ? "parameter inti yang dinilai sudah terisi" : missing.toString()).append(".\n");
        s.append("Estimasi/default yang dipakai hanya di dalam perhitungan: ").append(assumptions).append("\n");
        s.append("Tingkat ketidakpastian rekomendasi: ").append(uncertainty).append(".\n");
        s.append("PENTING: nilai asli dari input, sensor USB/RS485, ThingSpeak/Open-Meteo, riwayat atau laboratorium tidak ditimpa oleh estimasi. Saat nilai asli baru/terbaru masuk, analisis dihitung ulang dengan nilai asli tersebut.\n\n");
    }

    private void appendPresent(StringBuilder target, String label, boolean present) {
        if (present) append(target, label);
    }
    private void append(StringBuilder target, String label) {
        if (target.length() > 0) target.append(", ");
        target.append(label);
    }
    private void appendMissing(StringBuilder target, String label) { append(target, label); }

    private void appendRecommendations(StringBuilder s, String c, AgronomyEngine.CropProfile p,
                                       double ph, double ec, double moist, double n, double pp, double k,
                                       double at, double rh, double rain, double e0, double vpd, int hst, String cultivation, double nLow, double nHigh, String methodName) {
        if (!Double.isNaN(ph) && ph < p.phMin) s.append("• Koreksi pH: jangan menebak dosis kapur. Utamakan uji buffer/kemasaman; gunakan kapur/dolomit sesuai kebutuhan Ca/Mg.\n");
        if (!Double.isNaN(ph) && ph > p.phMax) s.append("• pH tinggi: hentikan dolomit/kapur. Periksa air irigasi dan pertimbangkan pengasaman hanya setelah hitung kebutuhan.\n");
        if (!Double.isNaN(ec) && ec / 1000.0 > p.ecThresholdDsM) s.append("• EC tinggi: kurangi pupuk pekat sekali aplikasi, periksa kualitas air, drainase, dan lakukan pemantauan ulang setelah hujan/irigasi.\n");
        if (!Double.isNaN(moist) && moist < 20) s.append("• Tanah kering: prioritaskan pemenuhan air sebelum memberi pupuk larut, lalu cek ulang kelembapan.\n");
        if (!Double.isNaN(moist) && moist > 85) s.append("• Tanah terlalu lembap: cek drainase dan tunda pupuk yang mudah hilang sampai kondisi memungkinkan.\n");
        if (!Double.isNaN(n) && (AgronomyEngine.classifyN(n,nLow,nHigh).startsWith("Rendah") || AgronomyEngine.classifyN(n,nLow,nHigh).startsWith("Sangat Rendah"))) s.append("• N rendah: utamakan sumber N yang sesuai fase dan bagi aplikasi agar efisiensi lebih baik.\n");
        if (!Double.isNaN(pp) && (AgronomyEngine.classifyP(pp,methodName).startsWith("Tinggi") || AgronomyEngine.classifyP(pp,methodName).startsWith("Sangat Tinggi"))) s.append("• P tinggi: jangan menambah P secara rutin; fokuskan dosis pada unsur yang memang kurang.\n");
        if (!Double.isNaN(k) && (AgronomyEngine.classifyK(k,methodName).startsWith("Tinggi") || AgronomyEngine.classifyK(k,methodName).startsWith("Sangat Tinggi"))) s.append("• K tinggi: jangan menambah K tanpa bukti kebutuhan; K dapat mengalami luxury consumption.\n");
        if (!Double.isNaN(vpd) && vpd > 2.0) s.append("• VPD tinggi: pantau layu; bila air tersedia, pertahankan kelembapan zona akar dan lakukan aplikasi pupuk pada waktu lebih sejuk.\n");
        if (!Double.isNaN(rain) && !Double.isNaN(e0) && rain < e0) s.append("• Air masuk < kebutuhan atmosfer: cek cadangan air tanah sebelum menetapkan irigasi.\n");
        if (!Double.isNaN(rh) && rh > 90 && !Double.isNaN(rain) && rain > 5) s.append("• RH + hujan tinggi: jadwalkan scouting penyakit lebih rapat; utamakan sanitasi, sirkulasi udara, dan PHT.\n");
        if (cultivation.toLowerCase(Locale.US).contains("organik")) {
            s.append("• Mode ORGANIK: rekomendasi mengutamakan kompos/pupuk kandang matang, pupuk yang diizinkan skema sertifikasi, sanitasi, varietas toleran, agen hayati, dan pengendalian mekanis. Verifikasi bahan dengan standar sertifikasi organik yang berlaku.\n");
        } else {
            s.append("• Mode KONVENSIONAL/PHT: gunakan pemupukan berimbang dan PHT. Pestisida hanya bila monitoring menunjukkan kebutuhan; patuhi label, interval pra-panen, dan rotasi bahan aktif.\n");
        }
        s.append("• Fase saat ini: ").append(AgronomyEngine.phase(hst, c)).append(". Fokuskan tindakan pada kebutuhan fase, bukan hanya umur kalender.\n");
    }

    private double[] fertilizerCredit(SharedPreferences prefs, String cropName, int days) {
        double n = 0, p = 0, k = 0;
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_FERT, "[]"));
            LocalDate cutoff = LocalDate.now(WIB).minusDays(days);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i); if (o == null) continue;
                if (!sameCrop(o.optString("crop", ""), cropName)) continue;
                if (!"kg/ha".equalsIgnoreCase(o.optString("unit", "kg/ha"))) continue;
                LocalDate d = parseDate(o.optString("date", "")); if (d == null || d.isBefore(cutoff)) continue;
                double dose = o.optDouble("dose", 0);
                n += dose * o.optDouble("nPct", 0) / 100.0;
                p += dose * o.optDouble("pPct", 0) / 100.0;
                k += dose * o.optDouble("kPct", 0) / 100.0;
            }
        } catch (Exception ignored) {}
        return new double[]{n, p, k};
    }

    private String recentOptSummary(SharedPreferences prefs, String cropName) {
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_OPT, "[]"));
            LocalDate cutoff = LocalDate.now(WIB).minusDays(30);
            ArrayList<JSONObject> list = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i); if (o == null || !sameCrop(o.optString("crop", ""), cropName)) continue;
                LocalDate d = parseDate(o.optString("date", "")); if (d != null && !d.isBefore(cutoff)) list.add(o);
            }
            if (list.isEmpty()) return "tidak ada kejadian OPT 30 hari terakhir.";
            list.sort(Comparator.comparingLong((JSONObject x) -> x.optLong("created", 0)).reversed());
            StringBuilder s = new StringBuilder();
            int n = Math.min(3, list.size());
            for (int i = 0; i < n; i++) {
                JSONObject o = list.get(i);
                if (i > 0) s.append("; ");
                s.append(o.optString("target", "OPT"));
                double aff = o.optDouble("affectedPct", Double.NaN);
                if (!Double.isNaN(aff)) s.append(" ").append(show(aff)).append("% serangan");
            }
            return s.toString();
        } catch (Exception ignored) { return "riwayat OPT belum terbaca."; }
    }

    private String buildSoilSummary() {
        return "SUMBER: " + soilSource.getSelectedItem() + "\n" +
                "pH " + show(num(soilPh.getText().toString())) + " • N " + show(num(soilN.getText().toString())) +
                " • P " + show(num(soilP.getText().toString())) + " • K " + show(num(soilK.getText().toString())) + " mg/kg\n" +
                "EC " + show(num(soilEc.getText().toString())) + " µS/cm • kelembapan " + show(num(soilMoisture.getText().toString())) + " %";
    }

    private String buildTimeline(SharedPreferences prefs, String cropName) {
        ArrayList<JSONObject> all = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(KEY_NOTES, "[]"));
            for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null && sameCrop(o.optString("crop", ""), cropName)) all.add(wrapTimeline(o.optString("date", ""), o.optString("time", ""), "LAPANG • " + o.optString("type", ""), o.optString("observation", ""), o.optLong("created", 0))); }
            JSONArray f = new JSONArray(prefs.getString(KEY_FERT, "[]"));
            for (int i = 0; i < f.length(); i++) { JSONObject o = f.optJSONObject(i); if (o != null && sameCrop(o.optString("crop", ""), cropName)) all.add(wrapTimeline(o.optString("date", ""), "", "PUPUK • " + o.optString("product", ""), show(o.optDouble("dose", 0)) + " " + o.optString("unit", ""), o.optLong("created", 0))); }
            JSONArray z = new JSONArray(prefs.getString(KEY_OPT, "[]"));
            for (int i = 0; i < z.length(); i++) { JSONObject o = z.optJSONObject(i); if (o != null && sameCrop(o.optString("crop", ""), cropName)) all.add(wrapTimeline(o.optString("date", ""), "", "OPT • " + o.optString("target", ""), o.optString("method", "Monitoring"), o.optLong("created", 0))); }
        } catch (Exception ignored) {}
        all.sort(Comparator.comparingLong((JSONObject x) -> x.optLong("created", 0)).reversed());
        StringBuilder s = new StringBuilder("TIMELINE TERPADU • 50 TERBARU\n");
        if (all.isEmpty()) return s.append("Belum ada histori.").toString();
        for (int i = 0; i < Math.min(50, all.size()); i++) {
            JSONObject o = all.get(i); s.append("\n").append(o.optString("date", "--"));
            if (!o.optString("time", "").isEmpty()) s.append(" ").append(o.optString("time"));
            s.append(" • ").append(o.optString("type", "--")).append("\n  ").append(o.optString("text", "").replace("\n", " ")).append("\n");
        }
        return s.toString().trim();
    }

    private JSONObject wrapTimeline(String date, String time, String type, String text, long created) throws Exception {
        JSONObject x = new JSONObject(); x.put("date", date); x.put("time", time); x.put("type", type); x.put("text", text); x.put("created", created); return x;
    }

    private void refreshViews(SharedPreferences prefs) {
        String c = crop.getText().toString().trim();
        soilSummaryView.setText(buildSoilSummary());
        timelineView.setText(buildTimeline(prefs, c));
    }

    private void appendHistory(SharedPreferences prefs, String key, JSONObject o, int max) throws Exception {
        String raw = prefs.getString(key, "[]");
        JSONArray old;
        try {
            old = new JSONArray(raw);
        } catch (Exception corruptHistory) {
            // Preserve a damaged history for recovery rather than blocking all new saves.
            prefs.edit().putString(key + "_corrupt_backup", raw == null ? "" : raw).apply();
            old = new JSONArray();
        }
        JSONArray out = new JSONArray();
        int start = Math.max(0, old.length() - max + 1);
        for (int i = start; i < old.length(); i++) out.put(old.get(i));
        out.put(o);
        prefs.edit().putString(key, out.toString()).apply();
    }

    private LinearLayout dialogRoot() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); int p = dp(18); r.setPadding(p,p,p,p); return r; }
    private ScrollView wrap(LinearLayout root) { ScrollView s = new ScrollView(this); s.addView(root); return s; }
    private EditText edit(LinearLayout root, String hint, String value, boolean numeric) {
        EditText e = new EditText(this); e.setHint(hint); e.setText(value); e.setTextColor(0xFFFFFFFF); e.setHintTextColor(0xFF9DB0BC); e.setTextSize(14); e.setMinHeight(dp(48));
        e.setGravity(Gravity.TOP); e.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); root.addView(e, new LinearLayout.LayoutParams(-1, -2)); return e;
    }
    private Spinner spinner(LinearLayout root, String label, String[] values) {
        TextView l = new TextView(this); l.setText(label); l.setTextColor(0xFF29C6C7); l.setTextSize(11); root.addView(l);
        Spinner sp = new Spinner(this); ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values); a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); sp.setAdapter(a); root.addView(sp, new LinearLayout.LayoutParams(-1, dp(48))); return sp;
    }
    private void putTextNumber(JSONObject o, String key, EditText e) throws Exception { double v = num(e.getText().toString()); if (!Double.isNaN(v) && !Double.isInfinite(v)) o.put(key, v); }
    private double safe(EditText e) { double v = num(e.getText().toString()); return Double.isNaN(v) ? 0 : v; }
    private double num(String s) { if (s == null || s.trim().isEmpty()) return Double.NaN; try { return Double.parseDouble(s.trim().replace(',','.')); } catch (Exception ex) { return Double.NaN; } }
    private int hstInt() { return currentHst(); }
    private LocalDate parseDate(String s) { try { return LocalDate.parse(s.trim(), DATE_FMT); } catch (Exception ex) { return null; } }
    private boolean validDate(String s) { return parseDate(s) != null; }
    private boolean sameCrop(String a, String b) { return a != null && b != null && (a.trim().equalsIgnoreCase(b.trim()) || AgronomyEngine.normalizeCrop(a).equalsIgnoreCase(AgronomyEngine.normalizeCrop(b))); }
    private String show(double v) { return Double.isNaN(v) || Double.isInfinite(v) ? "--" : String.format(Locale.US, "%.2f", v); }
    private int dp(int v) { return Math.max(1, Math.round(v * getResources().getDisplayMetrics().density)); }
}
