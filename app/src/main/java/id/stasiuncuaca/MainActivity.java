package id.stasiuncuaca;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS = "thingspeak_config";
    private static final String DEFAULT_CHANNEL = "2981880";
    private static final String DEFAULT_READ_KEY = "P4B56Z7HZM56Q7HJ";
    private static final String DEFAULT_TITLE = "STASIUN CUACA";
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private static final long TS_REFRESH_MS = 30_000L;
    private static final long WEATHER_REFRESH_MS = 10 * 60_000L;
    private static final int LOCATION_REQ = 77;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final ExecutorService net = Executors.newFixedThreadPool(2);
    private android.content.SharedPreferences prefs;

    private TextView title, subtitle, statusChip, lastAccess, deviceDate, deviceClock, dataTime, channelView;
    private TextView weatherStatus, weatherLocation, weatherTemp, weatherFeels, weatherDew, weatherHumidity,
            weatherPressure, weatherCloud, weatherUv, weatherVisibility, weatherWind, weatherWindDir,
            weatherRain, weatherEt0, weatherCondition, weatherSunrise, weatherSunset, weatherSunHours, weatherAlerts,
            weatherForecast, weatherUpdated, weatherSw, weatherPar, weatherPpfd, weatherPpfdNote, weatherLux, weatherSoilMoisture;
    private TextView soil, soilStatus, optRisk, overall, finalStatus;
    private GridLayout fieldGrid;
    private TextView[] fieldLabels = new TextView[8];
    private TextView[] fieldValues = new TextView[8];
    private TextView[] fieldUnits = new TextView[8];
    private String[] detectedNames = new String[8];
    private String[] latestValues = new String[8];
    private String latestCreatedAt = "";
    private long lastWeatherEpoch = 0L;
    private String lastWeatherProvider = "";
    private long lastSeenLocationUpdateEpoch = 0L;
    private boolean tsLoading = false;
    private boolean weatherLoading = false;
    private int weatherRequestId = 0;
    private LocationManager locationManager;
    private LocationListener gpsListener;
    private Location bestFreshLocation;
    private boolean gpsRequestActive = false;
    private long gpsRequestStartedAtMs = 0L;
    private final Runnable gpsTimeout = () -> finishGpsRequest();

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 1000L);
        }
    };

    private final Runnable refreshTick = new Runnable() {
        @Override public void run() {
            loadThingSpeak(false);
            handler.postDelayed(this, TS_REFRESH_MS);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        lastSeenLocationUpdateEpoch = prefs.getLong("location_updated_epoch", 0L);
        bindViews();
        buildFieldCards();

        findViewById(R.id.settings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.refresh).setOnClickListener(v -> { loadThingSpeak(true); loadOpenMeteo(true); });
        findViewById(R.id.weatherRefresh).setOnClickListener(v -> loadOpenMeteo(true));
        findViewById(R.id.btnGps).setOnClickListener(v -> requestLocation());
        findViewById(R.id.btnFieldNotes).setOnClickListener(v -> startActivity(new Intent(this, FieldNotesActivity.class)));
        findViewById(R.id.btnAgronomy).setOnClickListener(v -> startActivity(new Intent(this, AgronomyActivity.class)));
        findViewById(R.id.btnCsv).setOnClickListener(v -> startActivity(new Intent(this, CsvDownloadActivity.class)));
        findViewById(R.id.btnPdfReport).setOnClickListener(v -> startActivity(new Intent(this, PdfReportActivity.class)));

        updateHeader();
        updateClock();
        handler.post(clockTick);
        loadThingSpeak(true);
        boolean requestGpsOnLaunch = getIntent() != null && getIntent().getBooleanExtra("request_fresh_gps", false);
        if (!requestGpsOnLaunch) loadOpenMeteo(false);
        if (requestGpsOnLaunch) {
            getIntent().removeExtra("request_fresh_gps");
            requestLocation();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        updateHeader();
        updateClock();
        loadThingSpeak(false);
        long locationEpoch = prefs.getLong("location_updated_epoch", 0L);
        boolean locationChangedElsewhere = locationEpoch > lastSeenLocationUpdateEpoch;
        if (locationChangedElsewhere) {
            lastSeenLocationUpdateEpoch = locationEpoch;
            lastWeatherEpoch = 0L;
            weatherRequestId++;
            weatherLoading = false;
        }
        String configuredWeatherProvider = prefs.getString("macro_weather_provider", "Open-Meteo");
        boolean providerChanged = !configuredWeatherProvider.equalsIgnoreCase(lastWeatherProvider);
        if (providerChanged) {
            lastWeatherEpoch = 0L;
            weatherRequestId++;
            weatherLoading = false;
        }
        if (!gpsRequestActive) loadOpenMeteo(locationChangedElsewhere || providerChanged);
        handler.removeCallbacks(refreshTick);
        handler.postDelayed(refreshTick, TS_REFRESH_MS);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshTick);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        clearGpsRequest();
        net.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
        statusChip = findViewById(R.id.statusChip);
        lastAccess = findViewById(R.id.lastAccess);
        deviceDate = findViewById(R.id.deviceDate);
        deviceClock = findViewById(R.id.deviceClock);
        dataTime = findViewById(R.id.dataTime);
        channelView = findViewById(R.id.channelView);
        fieldGrid = findViewById(R.id.grid);

        weatherStatus = findViewById(R.id.weatherStatus);
        weatherLocation = findViewById(R.id.weatherLocation);
        weatherTemp = findViewById(R.id.weatherTemp);
        weatherFeels = findViewById(R.id.weatherFeels);
        weatherDew = findViewById(R.id.weatherDew);
        weatherHumidity = findViewById(R.id.weatherHumidity);
        weatherPressure = findViewById(R.id.weatherPressure);
        weatherCloud = findViewById(R.id.weatherCloud);
        weatherUv = findViewById(R.id.weatherUv);
        weatherVisibility = findViewById(R.id.weatherVisibility);
        weatherWind = findViewById(R.id.weatherWind);
        weatherWindDir = findViewById(R.id.weatherWindDir);
        weatherRain = findViewById(R.id.weatherRain);
        weatherEt0 = findViewById(R.id.weatherEt0);
        weatherCondition = findViewById(R.id.weatherCondition);
        weatherSunrise = findViewById(R.id.weatherSunrise);
        weatherSunset = findViewById(R.id.weatherSunset);
        weatherSunHours = findViewById(R.id.weatherSunHours);
        weatherAlerts = findViewById(R.id.weatherAlerts);
        weatherForecast = findViewById(R.id.weatherForecast);
        weatherUpdated = findViewById(R.id.weatherUpdated);
        weatherSw = findViewById(R.id.weatherSw);
        weatherPar = findViewById(R.id.weatherPar);
        weatherPpfd = findViewById(R.id.weatherPpfd);
        weatherPpfdNote = findViewById(R.id.weatherPpfdNote);
        weatherLux = findViewById(R.id.weatherLux);
        weatherSoilMoisture = findViewById(R.id.weatherSoilMoisture);

        soil = findViewById(R.id.soil);
        soilStatus = findViewById(R.id.soilStatus);
        optRisk = findViewById(R.id.optRisk);
        overall = findViewById(R.id.overall);
        finalStatus = findViewById(R.id.finalStatus);
    }

    private void buildFieldCards() {
        fieldGrid.removeAllViews();
        for (int i = 0; i < 8; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(14, 12, 14, 12);
            card.setBackgroundResource(R.drawable.bg_panel);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(i % 2, 1, 1f);
            lp.rowSpec = GridLayout.spec(i / 2);
            lp.setMargins(4, 4, 4, 4);
            card.setLayoutParams(lp);

            TextView label = new TextView(this);
            label.setText("Field " + (i + 1));
            label.setTextColor(getColorCompat(R.color.cyan));
            label.setTextSize(13);
            label.setGravity(Gravity.START);

            TextView value = new TextView(this);
            value.setText("--");
            value.setTextColor(getColorCompat(R.color.text_main));
            value.setTextSize(20);
            value.setPadding(0, 4, 0, 0);

            TextView unit = new TextView(this);
            unit.setText("");
            unit.setTextColor(getColorCompat(R.color.text_muted));
            unit.setTextSize(12);
            unit.setPadding(0, 2, 0, 0);

            card.addView(label);
            card.addView(value);
            card.addView(unit);
            fieldGrid.addView(card);
            fieldLabels[i] = label;
            fieldValues[i] = value;
            fieldUnits[i] = unit;
        }
    }

    private int getColorCompat(int id) {
        return getResources().getColor(id);
    }

    private void updateHeader() {
        String t = prefs.getString("app_title", DEFAULT_TITLE).trim();
        title.setText(t.isEmpty() ? DEFAULT_TITLE : t);
        subtitle.setText("GANI CAHYO-UNS • v1.5.7 • Cuaca Makro + Agronomi");
        String ch = prefs.getString("channel", "").trim();
        if (ch.isEmpty()) ch = DEFAULT_CHANNEL;
        channelView.setText("CHANNEL: " + (ch.isEmpty() ? "--" : ch));
    }

    private void updateClock() {
        ZonedDateTime n = ZonedDateTime.now(WIB);
        deviceDate.setText(cap(n.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("id", "ID")))));
        deviceClock.setText(n.format(DateTimeFormatter.ofPattern("HH:mm:ss 'WIB'", Locale.US)));
    }

    private String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Load whichever macro-weather provider is selected in Settings. Kept under the old method name
     * so existing GPS/refresh callers remain compatible. */
    private void loadOpenMeteo(boolean manual) {
        double lat = prefs.getFloat("latitude", Float.NaN);
        double lon = prefs.getFloat("longitude", Float.NaN);
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
            weatherStatus.setText("CUACA MAKRO • KOORDINAT BELUM ADA");
            weatherLocation.setText("Tekan GUNAKAN GPS HP atau isi latitude/longitude pada Pengaturan.");
            return;
        }
        long now = System.currentTimeMillis();
        if (!manual && now - lastWeatherEpoch < WEATHER_REFRESH_MS) return;
        if (weatherLoading) return;
        weatherLoading = true;
        final int requestId = ++weatherRequestId;
        lastWeatherEpoch = now;
        final String provider = prefs.getString("macro_weather_provider", "Open-Meteo");
        lastWeatherProvider = provider;
        weatherStatus.setText("MEMUAT " + provider.toUpperCase(Locale.ROOT) + "...");
        net.execute(() -> {
            try {
                String normalizedProvider = provider.toLowerCase(Locale.ROOT);
                Weather w = normalizedProvider.contains("openweather")
                        ? fetchOpenWeather(lat, lon)
                        : (normalizedProvider.contains("weatherapi") ? fetchWeatherApi(lat, lon) : fetchOpenMeteo(lat, lon));
                runOnUiThread(() -> { if (requestId == weatherRequestId) { renderWeather(w); weatherLoading = false; } });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (requestId == weatherRequestId) {
                        weatherStatus.setText(provider.toUpperCase(Locale.ROOT) + " GAGAL");
                        weatherLocation.setText(msg(e));
                        finalStatus.setText("Pemeriksaan cuaca: " + msg(e));
                        weatherLoading = false;
                    }
                });
            }
        });
    }

    private Weather fetchOpenMeteo(double lat, double lon) throws Exception {
        String current = "temperature_2m,relative_humidity_2m,apparent_temperature,dew_point_2m,precipitation,weather_code,surface_pressure,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,uv_index,shortwave_radiation,vapour_pressure_deficit,soil_temperature_0_to_10cm,soil_moisture_0_to_10cm";
        String daily = "temperature_2m_max,temperature_2m_min,precipitation_sum,sunshine_duration,daylight_duration,shortwave_radiation_sum,et0_fao_evapotranspiration,wind_direction_10m_dominant,uv_index_max,sunrise,sunset";
        String u = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=" + URLEncoder.encode(current, "UTF-8")
                + "&daily=" + URLEncoder.encode(daily, "UTF-8")
                + "&wind_speed_unit=ms&timezone=Asia%2FJakarta&forecast_days=7";
        JSONObject root = getJson(u);
        JSONObject cur = root.getJSONObject("current");
        JSONObject d = root.getJSONObject("daily");
        Weather w = new Weather();
        w.source = "Open-Meteo";
        w.condition = wmo(cur.optInt("weather_code", -1));
        w.radiationMethod = "Shortwave/radiasi dari model Open-Meteo";
        w.soilMoistureMethod = "Soil moisture model Open-Meteo (m³/m³; bukan sensor lapangan)";
        w.temp = cur.optDouble("temperature_2m", Double.NaN);
        w.rh = cur.optDouble("relative_humidity_2m", Double.NaN);
        w.apparent = cur.optDouble("apparent_temperature", Double.NaN);
        w.dewpoint = cur.optDouble("dew_point_2m", Double.NaN);
        w.rainNow = cur.optDouble("precipitation", Double.NaN);
        w.code = cur.optInt("weather_code", -1);
        w.pressure = cur.optDouble("surface_pressure", Double.NaN);
        w.cloud = cur.optDouble("cloud_cover", Double.NaN);
        w.windSpeed = cur.optDouble("wind_speed_10m", Double.NaN);
        w.windDir = cur.optDouble("wind_direction_10m", Double.NaN);
        w.gust = cur.optDouble("wind_gusts_10m", Double.NaN);
        w.visibility = cur.optDouble("visibility", Double.NaN);
        w.uv = cur.optDouble("uv_index", Double.NaN);
        w.uvDailyMax = first(d, "uv_index_max");
        w.sw = cur.optDouble("shortwave_radiation", Double.NaN);
        w.vpd = cur.optDouble("vapour_pressure_deficit", Double.NaN);
        w.soilTemp = cur.optDouble("soil_temperature_0_to_10cm", Double.NaN);
        w.soilMoisture = cur.optDouble("soil_moisture_0_to_10cm", Double.NaN);
        w.elevation = root.optDouble("elevation", Double.NaN);
        w.par = LightConversion.shortwaveToParWm2(w.sw);
        w.ppfd = LightConversion.parToPpfd(w.par);
        w.lux = Double.isFinite(w.sw) ? w.sw * 120.0 : Double.NaN;
        w.shortwaveDaily = first(d, "shortwave_radiation_sum");
        w.daylightHours = first(d, "daylight_duration") / 3600.0;
        w.sunshineMethod = "Open-Meteo sunshine_duration";
        w.et0Method = "Open-Meteo ET₀ FAO-56 Penman-Monteith";
        w.soilMoistureMethod = "Open-Meteo soil_moisture_0_to_10cm; estimasi/model grid, bukan sensor petak";
        w.et0 = first(d, "et0_fao_evapotranspiration");
        w.sunHours = first(d, "sunshine_duration") / 3600.0;
        w.rainDay = first(d, "precipitation_sum");
        w.sunrise = firstString(d, "sunrise");
        w.sunset = firstString(d, "sunset");
        w.forecast = forecastSummary(d);
        return w;
    }

    /** OpenWeather free/standard endpoints only: Current Weather 2.5 + 5 Day / 3 Hour Forecast 2.5.
     * UV and historical daily summaries are not supplied by these endpoints. */
    private Weather fetchOpenWeather(double lat, double lon) throws Exception {
        String key = prefs.getString("openweather_api_key", "").trim();
        if (key.isEmpty()) throw new Exception("API key OpenWeather kosong. Isi melalui Pengaturan → Cuaca Makro.");
        String coords = "lat=" + String.format(Locale.US, "%.6f", lat) + "&lon=" + String.format(Locale.US, "%.6f", lon);
        String encodedKey = URLEncoder.encode(key, "UTF-8");
        JSONObject currentRoot = getJson("https://api.openweathermap.org/data/2.5/weather?" + coords + "&units=metric&appid=" + encodedKey);
        JSONObject forecastRoot = getJson("https://api.openweathermap.org/data/2.5/forecast?" + coords + "&units=metric&appid=" + encodedKey);
        JSONObject main = currentRoot.optJSONObject("main"); if (main == null) main = new JSONObject();
        JSONObject wind = currentRoot.optJSONObject("wind"); if (wind == null) wind = new JSONObject();
        JSONObject clouds = currentRoot.optJSONObject("clouds"); if (clouds == null) clouds = new JSONObject();
        JSONObject sys = currentRoot.optJSONObject("sys"); if (sys == null) sys = new JSONObject();
        JSONObject city = forecastRoot.optJSONObject("city"); if (city == null) city = new JSONObject();
        JSONArray forecastList = forecastRoot.optJSONArray("list");
        if (forecastList == null || forecastList.length() == 0)
            throw new Exception("OpenWeather Current Weather berhasil, tetapi Forecast 5 hari tidak mengembalikan daftar prakiraan.");
        int offsetSeconds = city.optInt("timezone", currentRoot.optInt("timezone", 25200));
        ZoneId providerZone = ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofTotalSeconds(Math.max(-64800, Math.min(64800, offsetSeconds))));
        long currentEpoch = currentRoot.optLong("dt", System.currentTimeMillis() / 1000L);
        LocalDate localDate = Instant.ofEpochSecond(currentEpoch).atZone(providerZone).toLocalDate();
        OpenWeatherDay today = openWeatherDay(forecastList, providerZone, localDate);
        double currentTemp = main.optDouble("temp", Double.NaN);
        double tMax = today == null ? main.optDouble("temp_max", currentTemp) : today.max;
        double tMin = today == null ? main.optDouble("temp_min", currentTemp) : today.min;
        double elev = prefs.getFloat("elevation", 0f);
        if (!Double.isFinite(elev)) elev = 0.0;
        double humidity = main.optDouble("humidity", Double.NaN);
        double pressure = main.optDouble("pressure", Double.NaN);
        double speed = wind.optDouble("speed", Double.NaN);
        double cloud = clouds.optDouble("all", Double.NaN);
        WeatherMacroModel.DailyDerived derived = WeatherMacroModel.estimateDaily(lat, elev,
                localDate, tMax, tMin, humidity, pressure, speed, cloud);
        Weather w = new Weather();
        w.source = "OpenWeather Standard 2.5 (Current + 5 Day / 3 Hour Forecast)";
        w.temp = currentTemp;
        w.apparent = main.optDouble("feels_like", Double.NaN);
        w.dewpoint = estimateDewPointMagnus(currentTemp, humidity); // Derived from temp/RH; not an API measurement.
        w.rh = humidity;
        w.pressure = pressure;
        w.cloud = cloud;
        w.windSpeed = speed;
        w.windDir = wind.optDouble("deg", Double.NaN);
        w.gust = wind.optDouble("gust", Double.NaN);
        w.visibility = currentRoot.optDouble("visibility", Double.NaN);
        // The legacy UV Index API is retired; the standard current/forecast endpoints do not provide UV.
        w.uv = Double.NaN;
        w.uvDailyMax = Double.NaN;
        JSONObject currentRain = currentRoot.optJSONObject("rain");
        w.rainNow = currentRain != null && currentRain.has("1h") ? currentRain.optDouble("1h", Double.NaN) : 0.0;
        w.rainDay = today == null ? Double.NaN : today.rainMm; // forecast rain remaining on the location's local date
        w.code = openWeatherToWmo(currentRoot);
        JSONArray currentWeather = currentRoot.optJSONArray("weather");
        JSONObject weatherItem = currentWeather == null ? null : currentWeather.optJSONObject(0);
        w.condition = weatherItem == null ? "Kondisi OpenWeather" : weatherItem.optString("description", weatherItem.optString("main", "Kondisi cuaca"));
        w.elevation = elev;
        w.shortwaveDaily = derived.solarRadiationMjM2;
        w.daylightHours = derived.daylightHours;
        w.sunHours = derived.sunshineHours;
        w.et0 = derived.et0Mm;
        w.sw = derived.shortwaveDaylightMeanWm2;
        w.par = derived.parWm2;
        w.ppfd = derived.ppfdUmolM2S;
        w.lux = derived.luxEstimate;
        w.vpd = AgronomyEngine.vpd(w.temp, w.rh);
        w.sunrise = epochLocal(sys.optLong("sunrise", 0L), providerZone.getId());
        w.sunset = epochLocal(sys.optLong("sunset", 0L), providerZone.getId());
        w.forecast = openWeatherForecast(forecastList, providerZone);
        w.et0Method = "Estimasi FAO-56 Penman-Monteith dengan radiasi Hargreaves-Samani; bukan nilai langsung OpenWeather";
        w.sunshineMethod = "Estimasi inversi Angström-Prescott; bukan pengukuran sunshine recorder";
        w.radiationMethod = "Estimasi radiasi Rs Hargreaves-Samani, 0,16√(Tmax−Tmin)×Ra";
        w.soilMoistureMethod = "Estimasi neraca air zona akar; bukan pengukuran OpenWeather atau sensor petak";
        populateModeledSoilMoisture(w, localDate, w.rainDay, "openweather");
        return w;
    }

    /** WeatherAPI forecast provider. Uses one response for current conditions, 7-day forecast,
     * astronomy, UV and daily precipitation. Solar radiation/ET0/shine are explicitly modeled
     * because those values are not guaranteed in the WeatherAPI response. */
    private Weather fetchWeatherApi(double lat, double lon) throws Exception {
        String key = prefs.getString("weatherapi_api_key", "").trim();
        if (key.isEmpty()) throw new Exception("API key WeatherAPI belum diisi. Buka Pengaturan → Cuaca Makro → WeatherAPI.");
        String forecastBase = "https://api.weatherapi.com/v1/forecast.json?key=" + URLEncoder.encode(key, "UTF-8")
                + "&q=" + URLEncoder.encode(String.format(Locale.US, "%.6f,%.6f", lat, lon), "UTF-8")
                + "&days=";
        JSONObject root;
        try {
            root = getJson(forecastBase + "7&aqi=no&alerts=no");
        } catch (Exception firstError) {
            String errorText = firstError.getMessage() == null ? "" : firstError.getMessage().toLowerCase(Locale.ROOT);
            // Some account plans restrict the forecast horizon. Retry a shorter, free-tier-compatible window
            // only for plan/forecast access errors; do not hide invalid keys, quota exhaustion, or bad coordinates.
            if (!(errorText.contains("2009") || errorText.contains("plan") || errorText.contains("subscription")
                    || errorText.contains("forecast days") || errorText.contains("forecast range"))) throw firstError;
            root = getJson(forecastBase + "3&aqi=no&alerts=no");
        }
        JSONObject location = root.optJSONObject("location");
        JSONObject current = root.optJSONObject("current");
        JSONObject forecast = root.optJSONObject("forecast");
        JSONArray forecastDays = forecast == null ? null : forecast.optJSONArray("forecastday");
        if (location == null || current == null || forecastDays == null || forecastDays.length() == 0)
            throw new Exception("WeatherAPI tidak mengembalikan location/current/forecastday yang lengkap.");
        JSONObject today = forecastDays.optJSONObject(0);
        JSONObject day = today == null ? null : today.optJSONObject("day");
        JSONObject astro = today == null ? null : today.optJSONObject("astro");
        if (today == null || day == null) throw new Exception("Data harian WeatherAPI kosong.");
        if (astro == null) astro = new JSONObject();

        ZoneId providerZone;
        try { providerZone = ZoneId.of(location.optString("tz_id", "Asia/Jakarta")); }
        catch (Exception ignored) { providerZone = WIB; }
        LocalDate localDate;
        try { localDate = LocalDate.parse(today.optString("date", LocalDate.now(providerZone).toString())); }
        catch (Exception ignored) { localDate = LocalDate.now(providerZone); }
        double tMax = day.optDouble("maxtemp_c", current.optDouble("temp_c", Double.NaN));
        double tMin = day.optDouble("mintemp_c", current.optDouble("temp_c", Double.NaN));
        double elev = prefs.getFloat("elevation", 0f);
        double windMs = current.optDouble("wind_kph", Double.NaN) / 3.6;
        double gustMs = current.optDouble("gust_kph", Double.NaN) / 3.6;
        double cloud = current.optDouble("cloud", Double.NaN);
        WeatherMacroModel.DailyDerived derived = WeatherMacroModel.estimateDaily(lat, elev, localDate,
                tMax, tMin, day.optDouble("avghumidity", current.optDouble("humidity", Double.NaN)),
                current.optDouble("pressure_mb", Double.NaN), windMs, cloud);

        Weather w = new Weather();
        w.source = "WeatherAPI";
        w.temp = current.optDouble("temp_c", Double.NaN);
        w.apparent = current.optDouble("feelslike_c", Double.NaN);
        w.dewpoint = current.optDouble("dewpoint_c", Double.NaN);
        w.rh = current.optDouble("humidity", Double.NaN);
        w.pressure = current.optDouble("pressure_mb", Double.NaN);
        w.cloud = cloud;
        w.windSpeed = windMs;
        w.windDir = current.optDouble("wind_degree", Double.NaN);
        w.gust = gustMs;
        w.visibility = current.optDouble("vis_km", Double.NaN) * 1000.0;
        w.uv = current.optDouble("uv", Double.NaN);
        w.uvDailyMax = day.optDouble("uv", Double.NaN);
        w.rainNow = current.optDouble("precip_mm", Double.NaN);
        // WeatherAPI's daily total precipitation includes the liquid precipitation total.
        // Keep a valid 0.0 when returned as zero; only fall back to current hourly value if omitted.
        w.rainDay = day.has("totalprecip_mm") ? day.optDouble("totalprecip_mm", Double.NaN) : w.rainNow;
        JSONObject condition = current.optJSONObject("condition");
        w.condition = condition == null ? "Kondisi WeatherAPI" : condition.optString("text", "Kondisi WeatherAPI");
        w.code = -1;
        w.elevation = elev;
        w.shortwaveDaily = derived.solarRadiationMjM2;
        w.daylightHours = derived.daylightHours;
        w.sunHours = derived.sunshineHours;
        w.et0 = derived.et0Mm;
        w.sw = derived.shortwaveDaylightMeanWm2;
        w.par = derived.parWm2;
        w.ppfd = derived.ppfdUmolM2S;
        w.lux = derived.luxEstimate;
        w.vpd = AgronomyEngine.vpd(w.temp, w.rh);
        w.sunrise = weatherApiAstronomyIso(localDate, astro.optString("sunrise", ""));
        w.sunset = weatherApiAstronomyIso(localDate, astro.optString("sunset", ""));
        w.forecast = weatherApiForecast(forecastDays);
        w.et0Method = "Estimasi FAO-56 Penman-Monteith dengan radiasi Hargreaves-Samani; bukan nilai ET0 langsung dari paket API";
        w.sunshineMethod = "Estimasi inversi Angström-Prescott; bukan pengukuran sunshine recorder";
        w.radiationMethod = "Estimasi radiasi Hargreaves-Samani; bukan data radiasi terukur WeatherAPI";
        populateModeledSoilMoisture(w, localDate, w.rainDay, "weatherapi");
        return w;
    }

    private String weatherApiAstronomyIso(LocalDate date, String value) {
        if (date == null || value == null || value.trim().isEmpty()) return "";
        try {
            java.time.LocalTime time = java.time.LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern("h:mm a", Locale.US));
            return date + "T" + time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US));
        } catch (Exception ignored) { return ""; }
    }

    private String weatherApiForecast(JSONArray forecastDays) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < forecastDays.length() && i < 7; i++) {
            JSONObject item = forecastDays.optJSONObject(i);
            if (item == null) continue;
            JSONObject day = item.optJSONObject("day");
            if (day == null) continue;
            JSONObject cond = day.optJSONObject("condition");
            if (out.length() > 0) out.append(" • ");
            out.append(item.optString("date", "--")).append(' ')
                    .append(cond == null ? "Cuaca" : cond.optString("text", "Cuaca"))
                    .append(' ').append(fmt(day.optDouble("mintemp_c", Double.NaN), 0))
                    .append('–').append(fmt(day.optDouble("maxtemp_c", Double.NaN), 0)).append("°C")
                    .append(" hujan ").append(fmt(day.optDouble("totalprecip_mm", Double.NaN), 1)).append(" mm");
        }
        return out.length() == 0 ? "--" : out.toString();
    }

    private double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private double nestedNumber(JSONObject parent, String objectKey, String numberKey) {
        JSONObject nested = parent == null ? null : parent.optJSONObject(objectKey);
        return nested == null ? Double.NaN : nested.optDouble(numberKey, Double.NaN);
    }

    private double estimateDewPointMagnus(double tempC, double rhPct) {
        if (!Double.isFinite(tempC) || !Double.isFinite(rhPct) || rhPct <= 0.0 || rhPct > 100.0 || tempC <= -243.04) return Double.NaN;
        double a = 17.625, b = 243.04;
        double gamma = Math.log(rhPct / 100.0) + (a * tempC) / (b + tempC);
        double denominator = a - gamma;
        return Math.abs(denominator) < 1e-9 ? Double.NaN : (b * gamma) / denominator;
    }

    private int openWeatherToWmo(JSONObject current) {
        JSONArray a = current.optJSONArray("weather");
        JSONObject item = a == null ? null : a.optJSONObject(0);
        int id = item == null ? 0 : item.optInt("id", 0);
        if (id >= 200 && id < 300) return 95;
        if (id >= 300 && id < 400) return 51;
        if (id >= 500 && id < 600) return id >= 502 ? 65 : 61;
        if (id >= 600 && id < 700) return 71;
        if (id == 701 || id == 741) return 45;
        if (id == 800) return 0;
        if (id == 801 || id == 802) return 2;
        if (id >= 803) return 3;
        return -1;
    }

    private String epochLocal(long seconds) {
        return epochLocal(seconds, WIB.getId());
    }

    private String epochLocal(long seconds, String timezone) {
        if (seconds <= 0) return "";
        try {
            ZoneId zone = ZoneId.of(timezone == null || timezone.trim().isEmpty() ? "Asia/Jakarta" : timezone.trim());
            return Instant.ofEpochSecond(seconds).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.US));
        } catch (Exception ignored) {
            return Instant.ofEpochSecond(seconds).atZone(WIB).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.US));
        }
    }

    private static class OpenWeatherDay {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double rainMm = 0.0;
        boolean hasAny = false;
        String condition = "Cuaca";
    }

    private OpenWeatherDay openWeatherDay(JSONArray list, ZoneId zone, LocalDate target) {
        if (list == null || zone == null || target == null) return null;
        OpenWeatherDay day = new OpenWeatherDay();
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i); if (item == null) continue;
            long epoch = item.optLong("dt", 0L); if (epoch <= 0L) continue;
            LocalDate date;
            try { date = Instant.ofEpochSecond(epoch).atZone(zone).toLocalDate(); }
            catch (Exception ignored) { continue; }
            if (!target.equals(date)) continue;
            JSONObject values = item.optJSONObject("main"); if (values == null) values = new JSONObject();
            double temp = values.optDouble("temp", Double.NaN);
            double min = values.optDouble("temp_min", temp), max = values.optDouble("temp_max", temp);
            if (Double.isFinite(min)) day.min = Math.min(day.min, min);
            if (Double.isFinite(max)) day.max = Math.max(day.max, max);
            JSONObject rain = item.optJSONObject("rain");
            if (rain != null) day.rainMm += Math.max(0.0, rain.optDouble("3h", 0.0));
            JSONArray weather = item.optJSONArray("weather");
            JSONObject weather0 = weather == null ? null : weather.optJSONObject(0);
            if (weather0 != null && "Cuaca".equals(day.condition)) day.condition = weather0.optString("main", "Cuaca");
            day.hasAny = true;
        }
        if (!day.hasAny) return null;
        if (!Double.isFinite(day.min)) day.min = Double.NaN;
        if (!Double.isFinite(day.max)) day.max = Double.NaN;
        return day;
    }

    private String openWeatherForecast(JSONArray list, ZoneId zone) {
        StringBuilder b = new StringBuilder();
        LocalDate lastDate = null;
        for (int i = 0; list != null && i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i); if (item == null) continue;
            long epoch = item.optLong("dt", 0L); if (epoch <= 0L) continue;
            LocalDate date;
            try { date = Instant.ofEpochSecond(epoch).atZone(zone).toLocalDate(); }
            catch (Exception ignored) { continue; }
            if (date.equals(lastDate)) continue;
            lastDate = date;
            OpenWeatherDay day = openWeatherDay(list, zone, date); if (day == null) continue;
            if (b.length() > 0) b.append(" • ");
            b.append(date).append(' ').append(day.condition).append(' ')
                    .append(fmt(day.min, 0)).append('–').append(fmt(day.max, 0)).append("°C • Hujan ")
                    .append(fmt(day.rainMm, 1)).append(" mm");
        }
        return b.toString();
    }

    private void populateModeledSoilMoisture(Weather w, LocalDate today, double rainTodayMm, String providerKey) {
        // Keep macro-weather/model soil moisture separate from actual field sensor readings.
        // A sensor value remains available to Agronomy in soil_moisture_pct, but must not be
        // presented as a prediction from OpenWeather or stored into the macro-weather cache.
        double fc = num(prefs.getString("soil_fc_pct", ""));
        double pwp = num(prefs.getString("soil_pwp_pct", ""));
        boolean defaultHydraulics = !(Double.isFinite(fc) && Double.isFinite(pwp) && fc > pwp);
        if (defaultHydraulics) { fc = 30.0; pwp = 12.0; }
        double depth = num(prefs.getString("soil_root_depth_cm", "30"));
        if (!Double.isFinite(depth) || depth <= 0) depth = 30.0;
        String modelPrefix = providerKey == null || providerKey.trim().isEmpty() ? "macro" : providerKey.trim().toLowerCase(Locale.ROOT);
        double fraction = prefs.getFloat(modelPrefix + "_soil_water_fraction", 0.5f);
        String previousDate = prefs.getString(modelPrefix + "_soil_water_date", "");
        boolean updatedToday = today.toString().equals(previousDate);
        if (!updatedToday) {
            String phase = prefs.getString("farm_phase_manual", "");
            double kc = WeatherMacroModel.cropCoefficient(phase);
            fraction = WeatherMacroModel.nextAvailableWaterFraction(fraction,
                    Double.isFinite(rainTodayMm) ? rainTodayMm : 0.0,
                    Double.isFinite(w.et0) ? w.et0 : 0.0, kc, fc, pwp, depth);
            if (!Double.isFinite(fraction)) fraction = 0.5;
            prefs.edit().putFloat(modelPrefix + "_soil_water_fraction", (float) fraction)
                    .putString(modelPrefix + "_soil_water_date", today.toString()).apply();
        }
        double moisturePct = WeatherMacroModel.moisturePctVol(fraction, fc, pwp);
        w.soilMoisture = moisturePct / 100.0;
        w.soilMoistureMethod = "Estimasi makro neraca air zona akar (water-bucket bergaya FAO-56, Kc screening, hujan efektif 80%, kedalaman "
                + fmt(depth, 0) + " cm" + (defaultHydraulics ? "; FC/PWP asumsi 30/12% vol" : "; FC/PWP input")
                + (updatedToday ? "; state tersimpan hari ini" : "; awal/lanjutan state model") + "); bukan prediksi sensor/observasi satelit";
    }

    private void renderWeather(Weather w) {
        weatherStatus.setText("ONLINE • " + w.source.toUpperCase(Locale.ROOT));
        weatherLocation.setText("GPS "+fmt(prefs.getFloat("latitude",0),5)+", "+fmt(prefs.getFloat("longitude",0),5)
                + " • elevasi DEM " + fmt(w.elevation,0) + " mdpl");
        weatherTemp.setText("Suhu " + fmt(w.temp,1) + " °C");
        weatherFeels.setText("Terasa " + fmt(w.apparent,1) + " °C");
        weatherHumidity.setText("RH " + fmt(w.rh,0) + " %");
        weatherDew.setText("Titik embun" + ((w.source != null && w.source.startsWith("OpenWeather Standard")) ? " (estimasi) " : " ") + fmt(w.dewpoint,1) + " °C");
        weatherPressure.setText("Tekanan " + fmt(w.pressure,0) + " hPa");
        weatherCloud.setText("Awan " + fmt(w.cloud,0) + " %");
        if (Double.isFinite(w.uv) && Double.isFinite(w.uvDailyMax)) {
            weatherUv.setText("UV saat ini " + fmt(w.uv,1) + " • maks hari ini " + fmt(w.uvDailyMax,1));
        } else if (Double.isFinite(w.uv)) {
            weatherUv.setText("UV " + fmt(w.uv,1));
        } else if (Double.isFinite(w.uvDailyMax)) {
            weatherUv.setText("UV maks hari ini " + fmt(w.uvDailyMax,1));
        } else if (w.source != null && w.source.startsWith("OpenWeather Standard")) {
            weatherUv.setText("UV tidak tersedia pada endpoint standar OpenWeather");
        } else {
            weatherUv.setText("UV -- (tidak tersedia)");
        }
        weatherVisibility.setText("Visibilitas " + fmt(w.visibility / 1000.0,1) + " km");
        weatherWind.setText("Angin " + fmt(w.windSpeed,1) + " m/s");
        weatherWindDir.setText("Arah " + compass(w.windDir));
        if (w.source != null && w.source.startsWith("OpenWeather Standard")) {
            weatherRain.setText("Prakiraan hujan sisa hari " + fmt(w.rainDay,1) + " mm • 1 jam terakhir " + fmt(w.rainNow,1) + " mm");
        } else {
            weatherRain.setText("Hujan/prakiraan hari ini " + fmt(w.rainDay,1) + " mm • 1 jam terakhir " + fmt(w.rainNow,1) + " mm");
        }
        weatherEt0.setText("ET₀ " + fmt(w.et0,2) + " mm/hari");
        weatherCondition.setText("Kondisi: " + (w.condition == null || w.condition.isEmpty() ? wmo(w.code) : w.condition));
        weatherSunrise.setText("Terbit " + shortTime(w.sunrise));
        weatherSunset.setText("Terbenam " + shortTime(w.sunset));
        weatherSunHours.setText("Lama penyinaran: " + fmt(w.sunHours,2) + " jam/hari • "
                + (w.source.contains("Open-Meteo") ? "Open-Meteo" : "estimasi model"));
        weatherAlerts.setText("VPD " + fmt(w.vpd,2) + " kPa • " + AgronomyEngine.classifyVpd(w.vpd) + " • Gust " + fmt(w.gust,1) + " m/s");
        weatherForecast.setText("Prakiraan cuaca: " + w.forecast);
        weatherUpdated.setText("Pembaruan: " + ZonedDateTime.now(WIB).format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss", Locale.US)));
        weatherSw.setText("Shortwave " + fmt(w.sw,0) + " W/m²");
        weatherPar.setText("PAR estimasi " + fmt(w.par,0) + " W/m²");
        weatherPpfd.setText("PPFD estimasi " + fmt(w.ppfd,0) + " µmol/m²/s");
        weatherLux.setText("Lux estimasi " + fmt(w.lux,0) + " lux (dari shortwave; bukan pengukuran lux langsung)");
        weatherPpfdNote.setText("PAR = 0,45 × shortwave; PPFD = PAR × 4,57 µmol/J. "
                + (w.source.contains("Open-Meteo") ? w.radiationMethod + "; ET₀ " + w.et0Method
                : w.radiationMethod + "; ET₀ " + w.et0Method));
        weatherSoilMoisture.setText("Kelembapan tanah makro: " + fmt(w.soilMoisture * 100.0, 1) + " %vol • " + w.soilMoistureMethod);

        String crop = prefs.getString("crop", "Tanaman pertanian");
        double n = num(prefs.getString("soil_n", ""));
        double p = num(prefs.getString("soil_p", ""));
        double k = num(prefs.getString("soil_k", ""));
        double ph = num(prefs.getString("soil_ph", ""));
        double ec = num(prefs.getString("soil_ec_us_cm", ""));
        double moist = num(prefs.getString("soil_moisture_pct", ""));
        double ece = num(prefs.getString("soil_ece_ds_m", ""));
        double nLow = num(prefs.getString("soil_n_low", ""));
        double nHigh = num(prefs.getString("soil_n_high", ""));
        if (!NitrogenInference.usesMethodSpecificThresholds(prefs.getString("soil_n_classification_basis", NitrogenInference.BASIS_INFERRED))) {
            nLow = Double.NaN;
            nHigh = Double.NaN;
        }
        AgronomyEngine.CropProfile cp = AgronomyEngine.profile(crop);
        prefs.edit().putString("om_temp",fmt(w.temp,2)).putString("om_rh",fmt(w.rh,2))
                .putString("om_apparent_temp",fmt(w.apparent,2)).putString("om_dewpoint",fmt(w.dewpoint,2))
                .putString("om_pressure",fmt(w.pressure,2)).putString("om_rain",fmt(w.rainDay,2))
                .putString("om_et0",fmt(w.et0,2)).putString("om_vpd",fmt(w.vpd,3))
                .putString("om_wind_speed",fmt(w.windSpeed,2)).putString("om_wind_direction",compass(w.windDir))
                .putString("om_wind_gust",fmt(w.gust,2)).putString("om_cloud_cover",fmt(w.cloud,0))
                .putString("om_visibility",fmt(w.visibility,0)).putString("om_uv",fmt(w.uv,1))
                .putString("om_uv_daily_max",fmt(w.uvDailyMax,1))
                .putString("om_rain_now",fmt(w.rainNow,2))
                .putString("om_sunrise",shortTime(w.sunrise)).putString("om_sunset",shortTime(w.sunset))
                .putString("om_sun_hours",fmt(w.sunHours,2)).putString("om_radiation",fmt(w.sw,2))
                .putString("om_par",fmt(w.par,3)).putString("om_ppfd",fmt(w.ppfd,3)).putString("om_lux",fmt(w.lux,0))
                .putString("om_soil_temp",fmt(w.soilTemp,2)).putString("om_soil_moisture",fmt(w.soilMoisture,2))
                 .putString("om_weather",w.condition == null || w.condition.isEmpty() ? wmo(w.code) : w.condition)
                .putString("om_forecast_7d",w.forecast)
                .putString("om_soil_moisture_method",w.soilMoistureMethod)
                .putString("om_sunshine_method",w.sunshineMethod)
                .putString("om_et0_method",w.et0Method)
                .putString("om_radiation_method",w.radiationMethod)
                .putString("om_daylight_hours",fmt(w.daylightHours,2))
                .putString("om_radiation_daily",fmt(w.shortwaveDaily,3))
                .putString("macro_weather_provider_used",w.source).apply();

        soil.setText("pH " + fmt(ph,2) + " • N tersedia " + fmt(n,1) + " • P " + fmt(p,1) + " • K " + fmt(k,1) + " mg/kg\nEC " + fmt(ec,0) + " µS/cm • kelembapan " + fmt(moist,1) + " %");
        String nPred = NitrogenInference.estimateTotalN(n);
        String nClass = NitrogenInference.classifyRange(n);
        String nStatus = AgronomyEngine.hasValidNThresholds(nLow,nHigh)
                ? "Kelas N tersedia (ambang yang diisi): " + AgronomyEngine.classifyN(n,nLow,nHigh)
                : "Kelas N-total estimasi (asumsi 2%): " + NitrogenInference.classifyEstimatedTotalN(n);
        soilStatus.setText("pH: " + AgronomyEngine.classifyPH(ph,cp)
                + "\nN tersedia: " + fmt(n,1) + " mg/kg"
                + "\n" + nStatus
                + "\nN-total estimasi: " + nPred
                + "\nRentang kelas pada sensitivitas 1–3%: " + nClass
                + "\nP: " + AgronomyEngine.classifyP(p,prefs.getString("soil_test_method",""))
                + " • K: " + AgronomyEngine.classifyK(k,prefs.getString("soil_test_method",""))
                + "\nEC sensor: " + AgronomyEngine.classifyEC(ec,cp)
                + "\nECe lab: " + (Double.isFinite(ece)?AgronomyEngine.classifyECe(ece):"belum ada"));
        int hst = (int)Math.round(num(prefs.getString("farm_hst","-1")));
        optRisk.setText(AgronomyEngine.optRisk(crop,w.temp,w.rh,w.rainDay,w.windSpeed,recentOpt(),hst));
        overall.setText(buildWeatherStatusImpact(crop, w));
        lastAccess.setText("Cuaca diperbarui: " + ZonedDateTime.now(WIB).format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)) + " WIB");
        finalStatus.setText("Cuaca makro: " + w.source + " • ET₀, radiasi, PAR, PPFD, lux, penyinaran, dan kelembapan tanah model; estimasi ditandai.");
    }

    private String buildWeatherStatusImpact(String cropName, Weather w) {
        AgronomyEngine.CropProfile cp = AgronomyEngine.profile(cropName);
        StringBuilder out = new StringBuilder();
        out.append("ANALISIS STATUS PARAMETER CUACA\n");
        out.append("Komoditas: ").append(cropName).append(" • sumber: ").append(w.source).append("\n\n");
        if (Double.isFinite(w.temp)) {
            if (w.temp < cp.tempMin) out.append("• Suhu ").append(fmt(w.temp,1)).append("°C di bawah kisaran profil ").append(fmt(cp.tempMin,0)).append("–").append(fmt(cp.tempMax,0)).append("°C: pertumbuhan bisa melambat. Hindari pemindahan bibit/pekerjaan akar pada waktu paling dingin dan pantau tanaman.\n");
            else if (w.temp > cp.tempMax) out.append("• Suhu ").append(fmt(w.temp,1)).append("°C di atas kisaran profil: stres panas meningkat. Prioritaskan cek air, naungan sementara bila cocok, dan kurangi pekerjaan berat saat terik.\n");
            else out.append("• Suhu berada dalam kisaran profil komoditas; tetap pantau variasi siang-malam.\n");
        }
        if (Double.isFinite(w.et0) && Double.isFinite(w.rainDay)) {
            if (w.rainDay < w.et0) out.append("• Hujan harian (").append(fmt(w.rainDay,1)).append(" mm) lebih kecil daripada ET₀ (").append(fmt(w.et0,1)).append(" mm): cek kelembapan zona akar sebelum menjadwalkan irigasi.\n");
            else if (w.rainDay > Math.max(10, w.et0 * 2.0)) out.append("• Hujan melebihi kebutuhan atmosfer harian: periksa drainase, genangan, dan penundaan pemupukan susulan sampai kondisi lahan memungkinkan.\n");
            else out.append("• Hujan/ET₀ tidak menunjukkan defisit yang jelas dari angka harian saja; konfirmasi dengan kelembapan tanah dan fase tanaman.\n");
        }
        if (Double.isFinite(w.vpd)) {
            if (w.vpd >= 2.0) out.append("• VPD tinggi (").append(fmt(w.vpd,2)).append(" kPa): kehilangan air tanaman dapat meningkat. Lakukan pengairan berdasar kondisi zona akar, bukan angka cuaca semata.\n");
            else if (w.vpd <= 0.4) out.append("• VPD rendah: penguapan/transpirasi atmosfer cenderung lambat; hindari menambah air tanpa memeriksa tanah.\n");
        }
        if (Double.isFinite(w.windSpeed) && w.windSpeed >= 5.0) out.append("• Angin cukup kuat: perhatikan drift semprotan, rebah pada tanaman tinggi, dan keseragaman irigasi.\n");
        if (Double.isFinite(w.rh) && w.rh >= 90 && Double.isFinite(w.rainDay) && w.rainDay >= 5)
            out.append("• Udara sangat lembap disertai hujan: beri jarak/sirkulasi kanopi dan jadwalkan kerja ketika daun mengering; pemeriksaan OPT tetap dilakukan terpisah di fitur Prediksi OPT.\n");
        if (Double.isFinite(w.sunHours) && w.sunHours < 3.0) out.append("• Penyinaran pendek: pengeringan kanopi/lahan dapat melambat dan pertumbuhan fotosintesis bisa terbatas; hindari menilai kebutuhan pupuk dari cuaca saja.\n");
        else if (Double.isFinite(w.sunHours) && w.sunHours >= 7.0) out.append("• Penyinaran panjang: potensi fotosintesis tersedia, tetapi kebutuhan air dan risiko panas dapat naik.\n");
        if (Double.isFinite(w.sw)) out.append("• Radiasi/PPFD/lux adalah ").append(w.source.contains("OpenWeather") ? "estimasi berbasis model" : "nilai model cuaca").append("; gunakan sebagai konteks, bukan pengganti sensor cahaya di petak.\n");
        if (out.toString().endsWith("\n\n")) out.append("Lengkapi data cuaca untuk memperoleh saran budidaya yang lebih spesifik.\n");
        return out.toString().trim();
    }

    private void loadThingSpeak(boolean manual) {
        if (tsLoading) return;
        tsLoading = true;
        String ch = prefs.getString("channel", "").trim();
        if (ch.isEmpty()) ch = DEFAULT_CHANNEL;
        String key = prefs.getString("read_key", "").trim();
        if (key.isEmpty()) key = DEFAULT_READ_KEY;
        if (ch.isEmpty()) {
            statusChip.setText("THINGSPeak BELUM DIATUR");
            tsLoading = false;
            return;
        }
        final String channel = ch;
        final String readKey = key;
        statusChip.setText(manual ? "MEMUAT THINGSPEAK..." : "THINGSPEAK TERHUBUNG");
        net.execute(() -> {
            try {
                JSONObject meta = null;
                try {
                    meta = getJson("https://api.thingspeak.com/channels/" + URLEncoder.encode(channel,"UTF-8") + ".json"
                            + (readKey.isEmpty() ? "" : "?api_key=" + URLEncoder.encode(readKey,"UTF-8")));
                } catch (Exception ignored) {}
                JSONObject feed = getJson("https://api.thingspeak.com/channels/" + URLEncoder.encode(channel,"UTF-8")
                        + "/feeds/last.json?timezone=Asia%2FJakarta&status=true"
                        + (readKey.isEmpty() ? "" : "&api_key=" + URLEncoder.encode(readKey,"UTF-8")));
                final JSONObject metadata = meta;
                final String[] vals = new String[8];
                for (int i=0;i<8;i++) vals[i] = feed.optString("field"+(i+1),"");
                final String created = feed.optString("created_at","");
                runOnUiThread(() -> renderThingSpeak(metadata, vals, created, channel));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusChip.setText("THINGSPEAK GAGAL");
                    finalStatus.setText("ThingSpeak: " + msg(e));
                    tsLoading = false;
                });
            }
        });
    }

    private void renderThingSpeak(JSONObject metadata, String[] vals, String created, String ch) {
        for (int i=0;i<8;i++) {
            detectedNames[i] = metadata == null ? "Field " + (i+1) : metadata.optString("field"+(i+1), "Field " + (i+1));
            latestValues[i] = vals[i];
            String custom = prefs.getString("field_name_"+(i+1), "").trim();
            String unit = prefs.getString("field_unit_"+(i+1), "").trim();
            String displayName = custom.isEmpty() ? detectedNames[i] : custom;
            if (displayName.isEmpty()) displayName = "Field " + (i+1);
            fieldLabels[i].setText(displayName);
            fieldValues[i].setText(vals[i].isEmpty() ? "--" : vals[i]);
            fieldUnits[i].setText(unit);
            prefs.edit()
                    .putString("ts_field_"+(i+1), vals[i])
                    .putString("ts_field_name_"+(i+1), detectedNames[i])
                    .putString("ts_field_unit_"+(i+1), unit)
                    .apply();
        }
        prefs.edit().putString("ts_channel", ch).putString("ts_created_at", created).apply();
        String titleFromChannel = metadata == null ? "" : metadata.optString("name","");
        channelView.setText("CHANNEL: " + ch + (titleFromChannel.isEmpty() ? "" : " • " + titleFromChannel));
        dataTime.setText("Data ThingSpeak: " + (created.isEmpty() ? "--" : created.replace("T"," ").replace("Z","")));
        statusChip.setText("ONLINE • THINGSPEAK");
        lastAccess.setText("Akses ThingSpeak: " + ZonedDateTime.now(WIB).format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)) + " WIB");
        tsLoading = false;
    }

    private JSONObject getJson(String urlString) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection)new URL(urlString).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(9000);
            c.setReadTimeout(15000);
            c.setUseCaches(false);
            c.setRequestProperty("Accept","application/json");
            int code = c.getResponseCode();
            InputStream in = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
            String body = readAll(in);
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code + (body.trim().isEmpty()?"":" — "+body.substring(0,Math.min(200,body.length()))));
            if (body.trim().isEmpty()) throw new Exception("Respons kosong");
            return new JSONObject(body);
        } finally { if (c != null) c.disconnect(); }
    }

    private double first(JSONObject o, String key) {
        JSONArray a = o.optJSONArray(key);
        return a == null || a.length()==0 ? Double.NaN : a.optDouble(0,Double.NaN);
    }

    private String firstString(JSONObject o, String key) {
        JSONArray a = o.optJSONArray(key);
        return a == null || a.length()==0 ? "" : a.optString(0,"");
    }

    private String forecastSummary(JSONObject d) {
        JSONArray dates=d.optJSONArray("time"), tmax=d.optJSONArray("temperature_2m_max"), tmin=d.optJSONArray("temperature_2m_min"), rain=d.optJSONArray("precipitation_sum"), et=d.optJSONArray("et0_fao_evapotranspiration");
        if (dates == null) return "--";
        StringBuilder s=new StringBuilder();
        for(int i=0;i<dates.length()&&i<7;i++){
            if(i>0)s.append(" | ");
            s.append(dates.optString(i,"--")).append(" ")
                    .append(fmt(tmin==null?Double.NaN:tmin.optDouble(i,Double.NaN),0)).append("–")
                    .append(fmt(tmax==null?Double.NaN:tmax.optDouble(i,Double.NaN),0)).append("°C")
                    .append(" hujan ").append(fmt(rain==null?Double.NaN:rain.optDouble(i,Double.NaN),1)).append(" mm")
                    .append(" ET0 ").append(fmt(et==null?Double.NaN:et.optDouble(i,Double.NaN),1));
        }
        return s.toString();
    }

    private void requestLocation() {
        if (gpsRequestActive) {
            Toast.makeText(this, "Sedang mencari koordinat; lokasi yang tersedia tetap ditampilkan.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_REQ);
            return;
        }
        try {
            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
            if (locationManager == null) throw new IllegalStateException("Layanan lokasi tidak tersedia");
            bestFreshLocation = null;
            gpsRequestStartedAtMs = System.currentTimeMillis();
            gpsRequestActive = true;

            // Show the best Android cached location immediately (including an older fix, clearly
            // labelled with its age), then listen for a newer GPS/network fix. This prevents the
            // form from appearing empty or "failed" while Android warms up a fresh location.
            Location cached = bestLastKnownLocation(locationManager);
            if (cached != null) {
                bestFreshLocation = new Location(cached);
                storeLocationAndRefresh(cached, "Lokasi terakhir Android digunakan sementara" + accuracyLabel(cached)
                        + " • " + locationAgeLabel(cached) + "; mencari fix yang lebih baru...");
            } else {
                weatherStatus.setText("MENCARI LOKASI HP...");
                weatherLocation.setText("Meminta koordinat baru dari GPS satelit/jaringan...");
            }
            Toast.makeText(this, "Meminta pembaruan lokasi HP...", Toast.LENGTH_SHORT).show();

            gpsListener = new LocationListener() {
                @Override public void onLocationChanged(Location location) {
                    if (location == null || !gpsRequestActive || !validLocation(location)) return;
                    long ageMs = locationAgeMs(location);
                    // A provider can report a cached fix right after subscription. Accept recent
                    // fixes using elapsed-realtime age rather than wall-clock-only timestamp tests.
                    if (ageMs < 0L || ageMs > 120_000L) return;
                    boolean improved = bestFreshLocation == null || isBetterLocation(location, bestFreshLocation);
                    if (improved) bestFreshLocation = new Location(location);
                    boolean goodGps = LocationManager.GPS_PROVIDER.equals(location.getProvider())
                            && (!location.hasAccuracy() || location.getAccuracy() <= 150f);
                    boolean goodNetwork = location.hasAccuracy() && location.getAccuracy() <= 100f;
                    if (goodGps || goodNetwork || !location.hasAccuracy()) {
                        // Commit the best fix once, avoiding duplicate weather requests for the
                        // same callback. A fresh but coarse fix is still shown immediately.
                        finishGpsRequest();
                    } else if (improved) {
                        storeLocationAndRefresh(bestFreshLocation, "Koordinat diperbarui dari "
                                + providerLabel(bestFreshLocation) + accuracyLabel(bestFreshLocation)
                                + "; menunggu pembacaan yang lebih akurat...");
                    }
                }
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
                @SuppressWarnings("deprecation")
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            };
            boolean requested = false;
            for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestLocationUpdates(provider, 1000L, 0f, gpsListener, android.os.Looper.getMainLooper());
                        requested = true;
                    }
                } catch (SecurityException ignored) {
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!requested) {
                if (bestFreshLocation != null) {
                    finishGpsRequest();
                } else {
                    clearGpsRequest();
                    weatherStatus.setText("LOKASI HP TIDAK AKTIF");
                    weatherLocation.setText("Aktifkan layanan Lokasi/GPS. Koordinat lama tidak dihapus.");
                    Toast.makeText(this, "Aktifkan Lokasi/GPS pada HP untuk mendapatkan koordinat baru.", Toast.LENGTH_LONG).show();
                }
                return;
            }
            handler.removeCallbacks(gpsTimeout);
            handler.postDelayed(gpsTimeout, 20_000L);
        } catch(Exception e) {
            clearGpsRequest();
            weatherStatus.setText("GPS BELUM SIAP");
            weatherLocation.setText("Tidak dapat meminta lokasi: " + msg(e) + ". Koordinat lama dipertahankan.");
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
        double lat = location.getLatitude(), lon = location.getLongitude();
        return Double.isFinite(lat) && Double.isFinite(lon)
                && lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }

    private long locationAgeMs(Location location) {
        if (location == null) return Long.MAX_VALUE;
        if (Build.VERSION.SDK_INT >= 17 && location.getElapsedRealtimeNanos() > 0L) {
            long ageNanos = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
            return ageNanos < -5_000_000_000L ? Long.MAX_VALUE : Math.max(0L, ageNanos / 1_000_000L);
        }
        long timestamp = location.getTime();
        if (timestamp <= 0L) return Long.MAX_VALUE;
        long age = System.currentTimeMillis() - timestamp;
        return age < -5000L ? Long.MAX_VALUE : Math.max(0L, age);
    }

    private boolean isBetterLocation(Location candidate, Location current) {
        if (current == null) return true;
        long candidateAge = locationAgeMs(candidate);
        long currentAge = locationAgeMs(current);
        // A fix that is more than two minutes newer should replace an old cached fix even
        // when its reported accuracy is slightly worse. This is critical when the phone moved.
        if (candidateAge != Long.MAX_VALUE && currentAge != Long.MAX_VALUE) {
            if (candidateAge + 120_000L < currentAge) return true;
            if (currentAge + 120_000L < candidateAge) return false;
        }
        if (candidate.hasAccuracy() && current.hasAccuracy()) {
            if (candidate.getAccuracy() + 5f < current.getAccuracy()) return true;
            if (current.getAccuracy() + 5f < candidate.getAccuracy()) return false;
        } else if (candidate.hasAccuracy() != current.hasAccuracy()) {
            return candidate.hasAccuracy();
        }
        return candidateAge <= currentAge;
    }

    private String locationAgeLabel(Location location) {
        long age = locationAgeMs(location);
        if (age == Long.MAX_VALUE) return "umur lokasi tidak diketahui";
        if (age < 60_000L) return "umur lokasi " + Math.max(1L, age / 1000L) + " detik";
        if (age < 3_600_000L) return "umur lokasi " + (age / 60_000L) + " menit";
        if (age < 86_400_000L) return "umur lokasi " + (age / 3_600_000L) + " jam";
        return "umur lokasi " + (age / 86_400_000L) + " hari";
    }

    private String providerLabel(Location location) {
        if (location == null) return "lokasi HP";
        if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) return "GPS satelit";
        if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) return "jaringan/Wi-Fi";
        return "cache lokasi HP";
    }

    private String accuracyLabel(Location location) {
        return location != null && location.hasAccuracy()
                ? " • akurasi ±" + fmt(location.getAccuracy(), 0) + " m" : "";
    }

    private void storeLocationAndRefresh(Location location, String statusText) {
        if (!validLocation(location)) return;
        float lat = (float) location.getLatitude();
        float lon = (float) location.getLongitude();
        long updatedAt = System.currentTimeMillis();
        prefs.edit().putFloat("latitude", lat).putFloat("longitude", lon)
                .putLong("location_updated_epoch", updatedAt).apply();
        lastSeenLocationUpdateEpoch = updatedAt;
        weatherLocation.setText("GPS " + fmt(lat,6) + ", " + fmt(lon,6) + " • " + statusText);
        weatherStatus.setText("KOORDINAT GPS TERSEDIA");
        lastWeatherEpoch = 0L;
        weatherRequestId++;
        weatherLoading = false;
        loadOpenMeteo(true);
    }

    private void finishGpsRequest() {
        if (!gpsRequestActive) return;
        Location result = bestFreshLocation == null ? null : new Location(bestFreshLocation);
        clearGpsRequest();
        if (result == null) {
            float savedLat = prefs.getFloat("latitude", Float.NaN);
            float savedLon = prefs.getFloat("longitude", Float.NaN);
            if (Float.isFinite(savedLat) && Float.isFinite(savedLon)
                    && savedLat >= -90f && savedLat <= 90f && savedLon >= -180f && savedLon <= 180f) {
                weatherStatus.setText("LOKASI TERSIMPAN • BELUM ADA FIX BARU");
                weatherLocation.setText("GPS " + fmt(savedLat,6) + ", " + fmt(savedLon,6)
                        + " • koordinat tersimpan tetap digunakan; Android belum memberikan fix baru. Aktifkan Lokasi HP atau coba di area terbuka.");
            } else {
                weatherStatus.setText("LOKASI BARU BELUM TERSEDIA");
                weatherLocation.setText("Android belum mengirim koordinat dan belum ada lokasi tersimpan. Aktifkan Lokasi HP atau coba di area terbuka.");
            }
            return;
        }
        boolean fresh = locationAgeMs(result) <= 120_000L;
        String status = fresh
                ? "koordinat baru dari " + providerLabel(result) + accuracyLabel(result)
                : "lokasi terakhir Android digunakan sementara (" + locationAgeLabel(result) + "); belum ada fix baru";
        storeLocationAndRefresh(result, status);
        weatherStatus.setText(fresh ? "KOORDINAT GPS DIPERBARUI" : "LOKASI TERAKHIR DIGUNAKAN");
        Toast.makeText(this, fresh
                ? "Koordinat lokasi diperbarui: " + fmt((float)result.getLatitude(),5) + ", " + fmt((float)result.getLongitude(),5)
                : "Lokasi terakhir perangkat sudah dipakai. Android belum memberi fix baru; coba di tempat terbuka.", Toast.LENGTH_LONG).show();
    }

    private void clearGpsRequest() {
        handler.removeCallbacks(gpsTimeout);
        gpsRequestActive = false;
        if (locationManager != null && gpsListener != null) {
            try { locationManager.removeUpdates(gpsListener); } catch (SecurityException ignored) {}
        }
        gpsListener = null;
        locationManager = null;
    }

    private String recentOpt(){
        try{JSONArray a=new JSONArray(prefs.getString("opt_history","[]"));StringBuilder s=new StringBuilder();for(int i=Math.max(0,a.length()-5);i<a.length();i++)s.append(a.optJSONObject(i)).append("; ");return s.toString();}
        catch(Exception e){return "";}
    }

    private double num(String s){try{return s==null||s.trim().isEmpty()?Double.NaN:Double.parseDouble(s.trim().replace(',','.'));}catch(Exception e){return Double.NaN;}}
    private String fmt(double v,int d){return Double.isFinite(v)?String.format(Locale.US,"%."+d+"f",v):"--";}
    private String msg(Exception e){return e.getMessage()==null?"kesalahan tidak diketahui":e.getMessage();}
    private String readAll(InputStream in)throws Exception{if(in==null)return "";StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null)b.append(l);}return b.toString();}
    private String shortTime(String s){if(s==null||s.isEmpty())return "--";int p=s.indexOf('T');if(p>=0&&s.length()>=p+6)return s.substring(p+1,p+6);return s;}
    public static String compass(double deg){if(!Double.isFinite(deg))return "--";String[] p={"Utara","Utara-Timur Laut","Timur Laut","Timur-Timur Laut","Timur","Timur-Tenggara","Tenggara","Selatan-Tenggara","Selatan","Selatan-Barat Daya","Barat Daya","Barat-Barat Daya","Barat","Barat-Barat Laut","Barat Laut","Utara-Barat Laut"};int i=(int)Math.floor((deg+11.25)/22.5)%16;return p[i];}
    private String wmo(int c){switch(c){case 0:return "Cerah";case 1:case 2:return "Cerah berawan / sebagian berawan";case 3:return "Berawan";case 45:case 48:return "Kabut";case 51:case 53:case 55:return "Gerimis";case 61:case 63:case 65:return "Hujan";case 66:case 67:return "Hujan beku";case 71:case 73:case 75:return "Salju";case 80:case 81:case 82:return "Hujan deras lokal";case 95:return "Badai petir";default:return "Kode WMO "+c;}}

    private static final class Weather{
        double temp,rh,apparent,dewpoint,rainNow,pressure,cloud,windSpeed,windDir,gust,visibility,uv,uvDailyMax=Double.NaN,sw,par,ppfd,lux,vpd,soilTemp,soilMoisture,et0,sunHours,rainDay,elevation,daylightHours,shortwaveDaily;
        int code; String forecast,sunrise,sunset,source,condition,soilMoistureMethod,sunshineMethod,et0Method,radiationMethod;
    }
}
