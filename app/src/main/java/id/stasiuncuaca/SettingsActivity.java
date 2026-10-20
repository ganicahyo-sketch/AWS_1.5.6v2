package id.stasiuncuaca;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class SettingsActivity extends Activity {
    private static final String PREFS = "thingspeak_config";
    private static final String DEFAULT_CHANNEL = "2981880";
    private static final String DEFAULT_READ_KEY = "P4B56Z7HZM56Q7HJ";
    private static final String DEFAULT_AI_KEY = "";
    private android.content.SharedPreferences p;
    private EditText title, channel, readKey, aiKey, aiModel, geminiKey, geminiModel, openRouterKey, openRouterModel, lat, lon, elev, tBase, openWeatherKey, weatherApiKey;
    private AutoCompleteTextView crop;
    private TextView tBaseStatus;
    private Spinner cult, aiProvider, weatherProvider;
    private CheckBox aiGoogleSearch;
    private static final int LOCATION_REQ = 78;
    private static final long GPS_TIMEOUT_MS = 15000L;
    private final Handler gpsHandler = new Handler(Looper.getMainLooper());
    private LocationManager gpsManager;
    private LocationListener gpsListener;
    private Location bestLocation;
    private boolean gpsRequestActive = false;
    private TextView gpsStatus, toggleRead, toggleAi;
    private final Runnable gpsTimeout = () -> finishGpsRequest(true);
    private EditText[] fieldName = new EditText[8];
    private EditText[] fieldUnit = new EditText[8];

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        p = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        title=findViewById(R.id.appTitle); channel=findViewById(R.id.channel); readKey=findViewById(R.id.readKey);
        aiKey=findViewById(R.id.aiKey); aiModel=findViewById(R.id.aiModel); crop=findViewById(R.id.crop);
        tBase=findViewById(R.id.tBase); tBaseStatus=findViewById(R.id.tBaseStatus);
        ArrayAdapter<String> cropAdapter=new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,CropDatabase.CROPS);
        crop.setAdapter(cropAdapter); crop.setThreshold(0);
        crop.setOnItemClickListener((parent,view,position,id)->{
            String selected=String.valueOf(parent.getItemAtPosition(position));
            if(CropDatabase.isManual(selected)){
                crop.setText(""); crop.setHint("Ketik nama komoditas manual"); crop.requestFocus();
                tBase.setText("");
                tBaseStatus.setText("Komoditas manual: isi Tbase sendiri jika nilainya diketahui.");
                p.edit().remove("farm_tbase_c").remove("farm_tbase_crop").apply();
            } else {
                String canonical=CropDatabase.canonicalName(selected);
                crop.setHint("Komoditas/tanaman");
                crop.setText(canonical == null ? selected : canonical); crop.setSelection(crop.length());
                applyTBaseForCrop(canonical == null ? selected : canonical, true);
            }
        });
        crop.setOnClickListener(v->crop.showDropDown());
        crop.setOnFocusChangeListener((v,hasFocus)->{ if(!hasFocus) applyTBaseForCrop(crop.getText().toString().trim(), false); });
        tBase.setOnFocusChangeListener((v,hasFocus)->{ if(!hasFocus) persistTBaseForCurrentCrop(); });
        weatherProvider=findViewById(R.id.weatherProvider); openWeatherKey=findViewById(R.id.openWeatherKey); weatherApiKey=findViewById(R.id.weatherApiKey);
        ArrayAdapter<String> weatherAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"Open-Meteo (tanpa API key)","OpenWeather Standard 2.5 (perlu API key)","WeatherAPI (perlu API key)"});
        weatherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); weatherProvider.setAdapter(weatherAdapter);
        aiProvider=findViewById(R.id.aiProvider); aiGoogleSearch=findViewById(R.id.aiGoogleSearch);
        geminiKey=findViewById(R.id.geminiKey); geminiModel=findViewById(R.id.geminiModel);
        openRouterKey=findViewById(R.id.openRouterKey); openRouterModel=findViewById(R.id.openRouterModel);
        lat=findViewById(R.id.latitude); lon=findViewById(R.id.longitude); elev=findViewById(R.id.elevation);
        cult=findViewById(R.id.cultivation); toggleRead=findViewById(R.id.toggleReadKey); toggleAi=findViewById(R.id.toggleAiKey);
        gpsStatus=findViewById(R.id.gpsStatus);
        for(int i=0;i<8;i++){
            int n=i+1;
            fieldName[i]=findViewById(getResources().getIdentifier("fieldName"+n,"id",getPackageName()));
            fieldUnit[i]=findViewById(getResources().getIdentifier("fieldUnit"+n,"id",getPackageName()));
        }
        ArrayAdapter<String> providerAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"OpenAI","Google Gemini","OpenRouter"});
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); aiProvider.setAdapter(providerAdapter);
        ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"Konvensional / PHT","Organik"});
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); cult.setAdapter(ad);
        load();
        findViewById(R.id.useGps).setOnClickListener(v -> requestGpsLocation());
        findViewById(R.id.saveSettings).setOnClickListener(v->save());
        findViewById(R.id.cancelSettings).setOnClickListener(v->finish());
        toggleRead.setOnClickListener(v->toggle(readKey,toggleRead));
        toggleAi.setOnClickListener(v->toggle(aiKey,toggleAi));
    }

    @Override protected void onResume() {
        super.onResume();
        // Coordinates are updated directly here when the user presses GUNAKAN GPS HP.
        // No navigation to a second MainActivity is required.
        if (gpsStatus != null && !gpsRequestActive && gpsStatus.getText().toString().trim().isEmpty()) {
            gpsStatus.setText("Lokasi tersimpan. Tekan GUNAKAN GPS HP untuk memperbarui.");
        }
    }

    @Override protected void onDestroy() {
        clearGpsRequest();
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_REQ) return;
        boolean granted = false;
        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            if ((Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])
                    || Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i]))
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED) granted = true;
        }
        if (granted) requestGpsLocation();
        else setGpsStatus("Izin lokasi tidak diberikan. Latitude/longitude lama tidak diubah.");
    }

    private void requestGpsLocation() {
        if (gpsRequestActive) {
            Toast.makeText(this, "Sedang memperbarui koordinat; tunggu sebentar.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQ);
            return;
        }
        try {
            gpsManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (gpsManager == null) throw new IllegalStateException("Layanan lokasi Android tidak tersedia");
            gpsRequestActive = true;
            bestLocation = null;

            // Immediate path: populate the form using a recent Android location cache while
            // requesting a new fix in parallel. A temporary lack of new satellite data no
            // longer leaves this button showing a generic update failure.
            Location last = bestLastKnownLocation(gpsManager);
            if (last != null) {
                bestLocation = new Location(last);
                storeLocationInForm(last, "Lokasi terakhir Android diisi segera (" + locationAgeLabel(last)
                        + "); mencari koordinat yang lebih baru…");
            } else {
                setGpsStatus("Meminta koordinat baru dari GPS/jaringan HP…");
            }

            gpsListener = new LocationListener() {
                @Override public void onLocationChanged(Location location) {
                    if (location == null || !gpsRequestActive || !validLocation(location)) return;
                    long age = locationAgeMs(location);
                    if (age > 120000L) return;
                    boolean improved = bestLocation == null || isBetterLocation(location, bestLocation);
                    if (improved) {
                        bestLocation = new Location(location);
                        // A valid improving fix is published immediately; no arbitrary 25 m
                        // cutoff is imposed, so approximate network location still works indoors.
                        storeLocationInForm(bestLocation, "Koordinat baru dari " + providerLabel(bestLocation)
                                + accuracyLabel(bestLocation) + ".");
                    }
                    if (!location.hasAccuracy() || location.getAccuracy() <= 100f
                            || LocationManager.GPS_PROVIDER.equals(location.getProvider()) && location.getAccuracy() <= 150f) {
                        finishGpsRequest(false);
                    }
                }
                @Override public void onProviderEnabled(String provider) {
                    setGpsStatus("Provider lokasi aktif: " + providerLabel(provider) + ". Menunggu koordinat…");
                }
                @Override public void onProviderDisabled(String provider) {}
                @SuppressWarnings("deprecation")
                @Override public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}
            };

            boolean requested = false;
            for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                try {
                    if (gpsManager.isProviderEnabled(provider)) {
                        gpsManager.requestLocationUpdates(provider, 1000L, 0f, gpsListener, Looper.getMainLooper());
                        requested = true;
                    }
                } catch (SecurityException ignored) {
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!requested) {
                if (bestLocation != null) {
                    finishGpsRequest(true);
                } else {
                    clearGpsRequest();
                    setGpsStatus("Lokasi/GPS HP tidak aktif. Aktifkan layanan Lokasi dan coba lagi; koordinat lama dipertahankan.");
                    Toast.makeText(this, "Aktifkan Lokasi/GPS pada HP untuk mendapatkan koordinat baru.", Toast.LENGTH_LONG).show();
                }
                return;
            }
            gpsHandler.removeCallbacks(gpsTimeout);
            gpsHandler.postDelayed(gpsTimeout, GPS_TIMEOUT_MS);
        } catch (Exception e) {
            clearGpsRequest();
            setGpsStatus("Tidak dapat memulai GPS: " + errorText(e) + ". Koordinat lama dipertahankan.");
            Toast.makeText(this, "GPS belum dapat dimulai. Periksa izin dan pengaturan Lokasi HP.", Toast.LENGTH_LONG).show();
        }
    }

    private Location bestLastKnownLocation(LocationManager manager) {
        Location best = null;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && validLocation(candidate)
                        && (best == null || isBetterLocation(candidate, best))) best = new Location(candidate);
            } catch (SecurityException ignored) {
            } catch (IllegalArgumentException ignored) {
            }
        }
        return best;
    }

    private boolean validLocation(Location location) {
        if (location == null) return false;
        double latitude = location.getLatitude(), longitude = location.getLongitude();
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    private long locationAgeMs(Location location) {
        if (location == null) return Long.MAX_VALUE;
        if (Build.VERSION.SDK_INT >= 17 && location.getElapsedRealtimeNanos() > 0L) {
            long ageNanos = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
            return ageNanos < -5_000_000_000L ? Long.MAX_VALUE : Math.max(0L, ageNanos / 1_000_000L);
        }
        long timestamp = location.getTime();
        if (timestamp <= 0) return Long.MAX_VALUE;
        long age = System.currentTimeMillis() - timestamp;
        return age < -5000L ? Long.MAX_VALUE : Math.max(0L, age);
    }

    private boolean isBetterLocation(Location candidate, Location current) {
        if (current == null) return true;
        long candidateAge = locationAgeMs(candidate), currentAge = locationAgeMs(current);
        // Prefer a fix that is clearly newer when the phone has moved, even if a cached fix
        // reports a nominally better accuracy than the new network/GPS fix.
        if (candidateAge != Long.MAX_VALUE && currentAge != Long.MAX_VALUE) {
            if (candidateAge + 120_000L < currentAge) return true;
            if (currentAge + 120_000L < candidateAge) return false;
        }
        if (candidate.hasAccuracy() && current.hasAccuracy()) {
            if (candidate.getAccuracy() + 5f < current.getAccuracy()) return true;
            if (current.getAccuracy() + 5f < candidate.getAccuracy()) return false;
        } else if (candidate.hasAccuracy() != current.hasAccuracy()) return candidate.hasAccuracy();
        return candidateAge <= currentAge;
    }

    private String locationAgeLabel(Location location) {
        long age = locationAgeMs(location);
        if (age == Long.MAX_VALUE) return "umur tidak diketahui";
        if (age < 60_000L) return Math.max(1L, age / 1000L) + " detik lalu";
        if (age < 3_600_000L) return (age / 60_000L) + " menit lalu";
        if (age < 86_400_000L) return (age / 3_600_000L) + " jam lalu";
        return (age / 86_400_000L) + " hari lalu";
    }

    private void finishGpsRequest(boolean timedOut) {
        if (!gpsRequestActive) return;
        Location result = bestLocation == null ? null : new Location(bestLocation);
        clearGpsRequest();
        if (result != null) {
            boolean fresh = locationAgeMs(result) <= 120_000L;
            if (fresh) {
                storeLocationInForm(result, "Koordinat baru dari " + providerLabel(result)
                        + accuracyLabel(result) + (timedOut ? " (fix terbaik yang tersedia)." : "."));
                Toast.makeText(this, "Latitude/longitude diperbarui.", Toast.LENGTH_SHORT).show();
            } else {
                storeLocationInForm(result, "Lokasi terakhir perangkat dipakai sementara (" + locationAgeLabel(result)
                        + "); Android belum memberi fix baru. Buka Lokasi HP dan coba di area terbuka.");
                Toast.makeText(this, "Lokasi terakhir perangkat sudah digunakan; belum ada fix baru dari Android.", Toast.LENGTH_LONG).show();
            }
        } else {
            String saved = lat.getText().toString().trim();
            String savedLon = lon.getText().toString().trim();
            setGpsStatus(saved.isEmpty() || savedLon.isEmpty()
                    ? "Android belum mengirim koordinat baru; kolom tetap kosong. Aktifkan Lokasi HP atau coba di area terbuka."
                    : "Latitude/longitude tersimpan (" + saved + ", " + savedLon
                        + ") tetap dipertahankan; Android belum mengirim fix baru. Aktifkan Lokasi HP atau coba di area terbuka.");
        }
    }

    private void storeLocationInForm(Location location, String message) {
        if (!validLocation(location)) return;
        float latitude = (float) location.getLatitude();
        float longitude = (float) location.getLongitude();
        lat.setText(String.format(Locale.US, "%.6f", latitude));
        lon.setText(String.format(Locale.US, "%.6f", longitude));
        long updatedAt = System.currentTimeMillis();
        SharedPreferences.Editor editor = p.edit().putFloat("latitude", latitude).putFloat("longitude", longitude)
                .putLong("location_updated_epoch", updatedAt);
        if (location.hasAltitude() && Double.isFinite(location.getAltitude())) {
            elev.setText(String.format(Locale.US, "%.1f", location.getAltitude()));
            editor.putFloat("elevation", (float) location.getAltitude());
        }
        // apply() updates SharedPreferences in memory immediately and writes asynchronously.
        // The dashboard observes location_updated_epoch on resume and refreshes the configured macro-weather provider,
        // so this button does not depend on the user pressing the separate Save Settings button.
        editor.apply();
        setGpsStatus(message + " Koordinat langsung diterapkan dan disimpan untuk dashboard.");
    }

    private void clearGpsRequest() {
        gpsHandler.removeCallbacks(gpsTimeout);
        gpsRequestActive = false;
        if (gpsManager != null && gpsListener != null) {
            try { gpsManager.removeUpdates(gpsListener); } catch (SecurityException ignored) {}
        }
        gpsListener = null;
        gpsManager = null;
    }

    private String providerLabel(Location location) { return location == null ? "lokasi HP" : providerLabel(location.getProvider()); }
    private String providerLabel(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) return "GPS satelit";
        if (LocationManager.NETWORK_PROVIDER.equals(provider)) return "jaringan/Wi-Fi";
        return "lokasi HP";
    }
    private String accuracyLabel(Location location) {
        return location != null && location.hasAccuracy() ? " • akurasi ±" + String.format(Locale.US, "%.0f m", location.getAccuracy()) : "";
    }
    private void setGpsStatus(String message) { if (gpsStatus != null) gpsStatus.setText(message); }
    private String errorText(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }

    private void load(){
        title.setText(p.getString("app_title","STASIUN CUACA"));
        String savedChannel=p.getString("channel","").trim();
        String savedReadKey=p.getString("read_key","").trim();
        channel.setText(savedChannel.isEmpty()?DEFAULT_CHANNEL:savedChannel);
        readKey.setText(savedReadKey.isEmpty()?DEFAULT_READ_KEY:savedReadKey);
        String savedAiKey=p.getString("ai_api_key","").trim();
        aiKey.setText(savedAiKey.isEmpty()?DEFAULT_AI_KEY:savedAiKey);
        aiModel.setText(p.getString("ai_model","gpt-6-luna"));
        geminiKey.setText(p.getString("gemini_api_key","")); geminiModel.setText(p.getString("gemini_model","gemini-2.5-flash"));
        openRouterKey.setText(p.getString("openrouter_api_key","")); openRouterModel.setText(p.getString("openrouter_model","google/gemini-2.5-flash"));
        String provider=p.getString("ai_provider","OpenAI"); aiProvider.setSelection(provider.equalsIgnoreCase("Google Gemini")?1:(provider.equalsIgnoreCase("OpenRouter")?2:0));
        aiGoogleSearch.setChecked(p.getBoolean("ai_google_search",true));
        crop.setText(p.getString("crop","Tanaman pertanian"));
        loadTBaseForCrop(crop.getText().toString().trim());
        String macroProvider=p.getString("macro_weather_provider","Open-Meteo").toLowerCase(Locale.US);
        weatherProvider.setSelection(macroProvider.contains("weatherapi") ? 2 : (macroProvider.contains("openweather") ? 1 : 0));
        openWeatherKey.setText(p.getString("openweather_api_key",""));
        weatherApiKey.setText(p.getString("weatherapi_api_key",""));
        lat.setText(f(p.getFloat("latitude",Float.NaN))); lon.setText(f(p.getFloat("longitude",Float.NaN))); elev.setText(f(p.getFloat("elevation",Float.NaN)));
        cult.setSelection(p.getString("farm_cultivation_mode","Konvensional / PHT").toLowerCase(Locale.US).contains("organik")?1:0);
        for(int i=0;i<8;i++){
            fieldName[i].setText(p.getString("field_name_"+(i+1),""));
            fieldUnit[i].setText(p.getString("field_unit_"+(i+1),""));
        }
    }

    private String f(float x){return Float.isFinite(x)?String.format(Locale.US,"%.6f",x):"";}

    private void save(){
        String tv=title.getText().toString().trim(); if(tv.isEmpty())tv="STASIUN CUACA";
        String cv=crop.getText().toString().trim(); if(cv.isEmpty())cv="Tanaman pertanian";
        if (tBase.getText().toString().trim().isEmpty() && Double.isFinite(CropDatabase.tBase(cv))) {
            tBase.setText(String.format(Locale.US, "%.1f", CropDatabase.tBase(cv)));
        }
        String tb = tBase.getText().toString().trim();
        float la=numOptional(lat.getText().toString()), lo=numOptional(lon.getText().toString()), el=numOptional(elev.getText().toString());
        float tbValue = numOptional(tb);
        if(!Float.isNaN(tbValue) && (tbValue < -20f || tbValue > 60f)){ tBase.setError("Tbase harus antara -20 sampai 60 °C"); return; }
        if(!Float.isNaN(la)&&(la<-90||la>90)){lat.setError("Latitude -90..90");return;}
        if(!Float.isNaN(lo)&&(lo<-180||lo>180)){lon.setError("Longitude -180..180");return;}
        android.content.SharedPreferences.Editor e=p.edit().putString("app_title",tv).putString("channel",channel.getText().toString().trim().isEmpty()?DEFAULT_CHANNEL:channel.getText().toString().trim())
                .putString("read_key",readKey.getText().toString().trim().isEmpty()?DEFAULT_READ_KEY:readKey.getText().toString().trim())
                .putString("ai_api_key",aiKey.getText().toString().trim()).putString("ai_model",aiModel.getText().toString().trim().isEmpty()?"gpt-6-luna":aiModel.getText().toString().trim())
                .putString("ai_provider",aiProvider.getSelectedItem().toString()).putBoolean("ai_google_search",aiGoogleSearch.isChecked())
                .putString("gemini_api_key",geminiKey.getText().toString().trim()).putString("gemini_model",geminiModel.getText().toString().trim().isEmpty()?"gemini-2.5-flash":geminiModel.getText().toString().trim())
                .putString("openrouter_api_key",openRouterKey.getText().toString().trim()).putString("openrouter_model",openRouterModel.getText().toString().trim().isEmpty()?"google/gemini-2.5-flash":openRouterModel.getText().toString().trim())
                .putString("crop",cv).putString("farm_cultivation_mode",cult.getSelectedItem().toString())
                .putString("macro_weather_provider",weatherProvider.getSelectedItemPosition()==1?"OpenWeather":(weatherProvider.getSelectedItemPosition()==2?"WeatherAPI":"Open-Meteo"))
                .putString("openweather_api_key",openWeatherKey.getText().toString().trim())
                .putString("weatherapi_api_key",weatherApiKey.getText().toString().trim());
        if(!Float.isNaN(tbValue)) e.putString("farm_tbase_c", String.format(Locale.US, "%.2f", tbValue)).putString("farm_tbase_crop", CropDatabase.key(cv));
        else e.remove("farm_tbase_c").remove("farm_tbase_crop");
        if(!Float.isNaN(la))e.putFloat("latitude",la); else e.remove("latitude");
        if(!Float.isNaN(lo))e.putFloat("longitude",lo); else e.remove("longitude");
        if(!Float.isNaN(el))e.putFloat("elevation",el); else e.remove("elevation");
        for(int i=0;i<8;i++){
            e.putString("field_name_"+(i+1),fieldName[i].getText().toString().trim());
            e.putString("field_unit_"+(i+1),fieldUnit[i].getText().toString().trim());
        }
        e.apply(); Toast.makeText(this,"Pengaturan tersimpan.",Toast.LENGTH_SHORT).show(); finish();
    }

    private void loadTBaseForCrop(String cropName) {
        String saved = p.getString("farm_tbase_c", p.getString("crop_tbase_c", "")).trim();
        String savedCrop = p.getString("farm_tbase_crop", p.getString("crop_tbase_crop", "")).trim();
        String currentKey = CropDatabase.key(cropName);
        float value = numOptional(saved);
        if (!saved.isEmpty() && Float.isFinite(value) && value >= -20f && value <= 60f
                && !currentKey.isEmpty() && currentKey.equals(savedCrop)) {
            tBase.setText(saved);
            tBaseStatus.setText("Tbase tersimpan untuk komoditas ini; dapat disesuaikan manual.");
            return;
        }
        applyTBaseForCrop(cropName, false);
    }

    private void applyTBaseForCrop(String cropName, boolean fromDatabaseSelection) {
        String canonical = CropDatabase.canonicalName(cropName);
        String currentKey = CropDatabase.key(cropName);
        String savedKey = p.getString("farm_tbase_crop", "");
        if (canonical != null) {
            double base = CropDatabase.tBase(canonical);
            if (Double.isFinite(base)) {
                if (fromDatabaseSelection || !currentKey.equals(savedKey) || tBase.getText().toString().trim().isEmpty()) {
                    String formatted = String.format(Locale.US, "%.1f", base);
                    tBase.setText(formatted);
                    p.edit().putString("farm_tbase_c", formatted).putString("farm_tbase_crop", currentKey).apply();
                }
                double currentValue = numOptional(tBase.getText().toString());
                tBaseStatus.setText(CropDatabase.matchesDatabaseTBase(canonical, currentValue)
                        ? "Tbase otomatis dari database: " + CropDatabase.tBaseBasis(canonical) + " Nilai dapat diedit."
                        : "Tbase manual/override tersimpan. Nilai database acuan: " + String.format(Locale.US, "%.1f °C", base) + ".");
                return;
            }
        }
        if (fromDatabaseSelection || (!currentKey.isEmpty() && !currentKey.equals(savedKey))) {
            tBase.setText("");
            p.edit().remove("farm_tbase_c").remove("farm_tbase_crop").apply();
        }
        tBaseStatus.setText("Komoditas tidak memiliki Tbase di database: isi sendiri bila nilai varietas/metode diketahui.");
    }

    private void persistTBaseForCurrentCrop() {
        String cropName = crop.getText().toString().trim();
        String valueText = tBase.getText().toString().trim();
        float value = numOptional(valueText);
        if (valueText.isEmpty()) {
            double databaseValue = CropDatabase.tBase(cropName);
            if (Double.isFinite(databaseValue)) {
                String formatted = String.format(Locale.US, "%.1f", databaseValue);
                tBase.setText(formatted);
                p.edit().putString("farm_tbase_c", formatted).putString("farm_tbase_crop", CropDatabase.key(cropName)).apply();
                tBaseStatus.setText("Tbase otomatis dikembalikan ke nilai database: " + formatted + " °C.");
            } else {
                p.edit().remove("farm_tbase_c").remove("farm_tbase_crop").apply();
                tBaseStatus.setText("Komoditas manual: isi Tbase sendiri jika nilainya diketahui.");
            }
            return;
        }
        if (!Float.isFinite(value) || value < -20f || value > 60f) {
            tBase.setError("Tbase harus antara -20 sampai 60 °C");
            return;
        }
        p.edit().putString("farm_tbase_c", String.format(Locale.US, "%.2f", value))
                .putString("farm_tbase_crop", CropDatabase.key(cropName)).apply();
        tBaseStatus.setText("Tbase " + String.format(Locale.US, "%.2f °C", value) + " tersimpan untuk komoditas ini.");
    }

    private float numOptional(String s){try{String t=s==null?"":s.trim();return t.isEmpty()?Float.NaN:Float.parseFloat(t.replace(',','.'));}catch(Exception ex){return Float.NaN;}}
    private void toggle(EditText e,TextView t){boolean visible=e.getInputType()==(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);e.setInputType(InputType.TYPE_CLASS_TEXT|(visible?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));t.setText(visible?"TAMPILKAN":"SEMBUNYIKAN");e.setSelection(e.length());}
}
