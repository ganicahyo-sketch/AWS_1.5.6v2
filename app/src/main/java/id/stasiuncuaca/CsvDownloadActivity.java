package id.stasiuncuaca;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CSV exporter for ThingSpeak and Open-Meteo.
 *
 * ThingSpeak retains the existing range/all-history modes. Open-Meteo adds
 * historical CSV export for a user-selected date range at hourly or daily
 * resolution. Both sources are streamed to a temporary file so the final CSV
 * is not held fully in RAM.
 */
public class CsvDownloadActivity extends Activity {
    private static final String PREFS = "thingspeak_config";
    // Keep export defaults aligned with MainActivity and SettingsActivity. Settings
    // shows these values before a first save, so export must not interpret that
    // valid visible default as an empty configuration.
    private static final String DEFAULT_CHANNEL = "2981880";
    private static final String DEFAULT_READ_KEY = "P4B56Z7HZM56Q7HJ";
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter API_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);

    // ThingSpeak documents a maximum of 8,000 results per read request.
    private static final int MAX_RESULTS_PER_REQUEST = 8000;
    private static final int MAX_SPLIT_DEPTH = 32;
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 20000;
    private static final int NETWORK_RETRIES = 3;
    private static final int RECENT_ID_CACHE_LIMIT = 50000;

    // Safe early boundary. We do not need channel User API Key just to discover
    // a channel's creation time. Any actual ThingSpeak feed older than this
    // boundary would be outside the application's supported export window.
    private static final LocalDateTime HISTORY_START =
            LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService net = Executors.newSingleThreadExecutor();
    private android.content.SharedPreferences prefs;

    private LocalDate startDate;
    private LocalDate endDate;

    private TextView startView;
    private TextView endView;
    private TextView statusView;
    private TextView downloadButton;
    private TextView modeRange;
    private TextView modeAllHistory;
    private TextView sourceThingSpeak;
    private TextView sourceOpenMeteo;
    private TextView omHourly;
    private TextView omDaily;
    private LinearLayout rangePanel;
    private LinearLayout openMeteoPanel;
    private TextView historyInfo;
    private Spinner weatherProviderCsv;
    private TextView weatherProviderInfo;

    private boolean allHistoryMode = false;
    private boolean openMeteoMode = false;
    private boolean openMeteoHourly = true;
    private boolean downloading = false;
    private boolean readyToSave = false;

    private File pendingCsvFile;
    private String pendingFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_csv_download);

        startView = findViewById(R.id.startDate);
        endView = findViewById(R.id.endDate);
        statusView = findViewById(R.id.csvStatus);
        downloadButton = findViewById(R.id.downloadButton);
        modeRange = findViewById(R.id.modeRange);
        modeAllHistory = findViewById(R.id.modeAllHistory);
        sourceThingSpeak = findViewById(R.id.sourceThingSpeak);
        sourceOpenMeteo = findViewById(R.id.sourceOpenMeteo);
        omHourly = findViewById(R.id.omHourly);
        omDaily = findViewById(R.id.omDaily);
        rangePanel = findViewById(R.id.rangePanel);
        openMeteoPanel = findViewById(R.id.openMeteoPanel);
        historyInfo = findViewById(R.id.historyInfo);
        weatherProviderCsv = findViewById(R.id.historyWeatherProvider);
        weatherProviderInfo = findViewById(R.id.historyWeatherProviderInfo);
        ArrayAdapter<String> weatherProviderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Ikuti Pengaturan", "Open-Meteo", "OpenWeather Standard (saat ini/prakiraan)", "WeatherAPI"});
        weatherProviderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weatherProviderCsv.setAdapter(weatherProviderAdapter);
        weatherProviderCsv.setSelection(0);
        weatherProviderCsv.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                readyToSave = false; pendingCsvFile = null; pendingFileName = null;
                if (downloadButton != null) downloadButton.setText("UNDUH DATA CSV");
                updateMacroProviderUi();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        LocalDate today = LocalDate.now(WIB);
        // Default to completed days for historic exports; today can still be selected manually.
        endDate = today.minusDays(1);
        startDate = endDate.withDayOfMonth(1);
        refreshDateLabels();

        sourceThingSpeak.setOnClickListener(v -> setSource(false));
        sourceOpenMeteo.setOnClickListener(v -> setSource(true));
        modeRange.setOnClickListener(v -> setMode(false));
        modeAllHistory.setOnClickListener(v -> setMode(true));
        omHourly.setOnClickListener(v -> setOpenMeteoResolution(true));
        omDaily.setOnClickListener(v -> setOpenMeteoResolution(false));
        findViewById(R.id.startDate).setOnClickListener(v -> pickStartDate());
        findViewById(R.id.endDate).setOnClickListener(v -> pickEndDate());
        downloadButton.setOnClickListener(v -> {
            if (readyToSave) {
                openSaveDocument();
            } else {
                prepareCsv();
            }
        });
        findViewById(R.id.back).setOnClickListener(v -> finish());

        setSource(false);
    }

    private void setSource(boolean openMeteo) {
        if (downloading) return;
        openMeteoMode = openMeteo;
        sourceOpenMeteo.setBackgroundResource(openMeteo ? R.drawable.bg_button : R.drawable.bg_edit);
        sourceThingSpeak.setBackgroundResource(openMeteo ? R.drawable.bg_edit : R.drawable.bg_button);
        sourceOpenMeteo.setText("CUACA MAKRO");
        sourceOpenMeteo.setTextColor(openMeteo ? Color.rgb(7, 19, 31) : Color.WHITE);
        sourceThingSpeak.setTextColor(openMeteo ? Color.WHITE : Color.rgb(7, 19, 31));

        if (openMeteoMode) {
            allHistoryMode = false;
            modeAllHistory.setVisibility(android.view.View.GONE);
            modeRange.setVisibility(android.view.View.GONE);
            openMeteoPanel.setVisibility(android.view.View.VISIBLE);
            rangePanel.setVisibility(android.view.View.VISIBLE);
            historyInfo.setText("CUACA MAKRO HISTORIS: pilih provider, tanggal dan resolusi. Data model/reanalysis bukan rekaman sensor AWS. ET₀, penyinaran, kelembapan tanah, PAR, PPFD, radiasi, dan lux dimasukkan bila tersedia atau diberi label estimasi.");
            statusView.setText("Pilih provider cuaca makro dan rentang tanggal.");
            updateMacroProviderUi();
            setOpenMeteoResolution(openMeteoHourly);
        } else {
            modeAllHistory.setVisibility(android.view.View.VISIBLE);
            modeRange.setVisibility(android.view.View.VISIBLE);
            openMeteoPanel.setVisibility(android.view.View.GONE);
            setMode(allHistoryMode);
        }
    }

    private String selectedCsvWeatherProvider() {
        int pos = weatherProviderCsv == null ? 0 : weatherProviderCsv.getSelectedItemPosition();
        if (pos == 1) return "Open-Meteo";
        if (pos == 2) return "OpenWeather";
        if (pos == 3) return "WeatherAPI";
        String saved = prefs.getString("macro_weather_provider", "Open-Meteo");
        if (saved.toLowerCase(Locale.ROOT).contains("weatherapi")) return "WeatherAPI";
        return saved.toLowerCase(Locale.ROOT).contains("openweather") ? "OpenWeather" : "Open-Meteo";
    }

    private void updateMacroProviderUi() {
        if (weatherProviderInfo == null || weatherProviderCsv == null) return;
        String provider = selectedCsvWeatherProvider();
        if ("OpenWeather".equals(provider)) {
            weatherProviderInfo.setText("OpenWeather API standar (Current Weather + 5 Day / 3 Hour Forecast) hanya menyediakan cuaca saat ini dan prakiraan, bukan arsip cuaca historis. Ekspor CSV historis tidak tersedia untuk provider ini. Pilih Open-Meteo atau WeatherAPI untuk ekspor histori. UV tidak disediakan endpoint standar OpenWeather; ET₀, radiasi, sunshine, PAR, PPFD dan Lux tetap estimasi model.");
            omHourly.setVisibility(android.view.View.GONE);
            omDaily.setText("HISTORI TIDAK TERSEDIA");
            openMeteoHourly = false;
            omDaily.setVisibility(android.view.View.VISIBLE);
            omDaily.setEnabled(false);
        } else if ("WeatherAPI".equals(provider)) {
            weatherProviderInfo.setText("WeatherAPI History API • ekspor harian menggunakan satu permintaan per tanggal agar kompatibel lintas paket. Histori sejak 1 Januari 2010 bergantung pada akses paket; ET₀/radiasi hanya nilai langsung bila paket dan respons menyediakannya, selain itu estimasi diberi label.");
            omHourly.setVisibility(android.view.View.GONE);
            omDaily.setText("HARIAN");
            openMeteoHourly = false;
            omDaily.setVisibility(android.view.View.VISIBLE);
            omDaily.setEnabled(true);
        } else {
            weatherProviderInfo.setText("Open-Meteo Historical Weather API • mendukung CSV per jam maupun harian. ET₀, sunshine duration, dan radiasi tersedia pada endpoint historis; PAR, PPFD dan Lux turunan diberi label estimasi. Kelembapan tanah adalah model grid, bukan sensor petak.");
            omHourly.setVisibility(android.view.View.VISIBLE);
            omHourly.setEnabled(true);
            omDaily.setVisibility(android.view.View.VISIBLE);
            omDaily.setEnabled(true);
        }
        if (openMeteoMode && !downloading) statusView.setText("Provider CSV cuaca makro: " + provider + ".");
    }

    private void setOpenMeteoResolution(boolean hourly) {
        if ("OpenWeather".equals(selectedCsvWeatherProvider()) || "WeatherAPI".equals(selectedCsvWeatherProvider())) hourly = false;
        openMeteoHourly = hourly;
        omHourly.setBackgroundResource(hourly ? R.drawable.bg_button : R.drawable.bg_edit);
        omDaily.setBackgroundResource(hourly ? R.drawable.bg_edit : R.drawable.bg_button);
        omHourly.setTextColor(hourly ? Color.rgb(7, 19, 31) : Color.WHITE);
        omDaily.setTextColor(hourly ? Color.WHITE : Color.rgb(7, 19, 31));
        if (openMeteoMode && !downloading) {
            statusView.setText(hourly ? "Resolusi per jam dipilih • " + selectedCsvWeatherProvider() : "Resolusi harian dipilih • " + selectedCsvWeatherProvider());
        }
    }

    private void setMode(boolean allHistory) {
        if (downloading || openMeteoMode) return;

        allHistoryMode = allHistory;
        if (allHistoryMode) {
            modeAllHistory.setBackgroundResource(R.drawable.bg_button);
            modeRange.setBackgroundResource(R.drawable.bg_edit);
            modeAllHistory.setTextColor(Color.rgb(7, 19, 31));
            modeRange.setTextColor(Color.WHITE);

            rangePanel.setVisibility(android.view.View.GONE);
            historyInfo.setText(
                    "SELURUH HISTORI: aplikasi mencari data dari awal rentang aman sampai data terbaru, "
                            + "lalu otomatis memecah permintaan bila mencapai 8.000 entry.");
            statusView.setText(
                    "Seluruh histori akan digabung menjadi satu CSV. Proses dapat berlangsung lebih lama "
                            + "untuk channel dengan banyak data.");
        } else {
            modeRange.setBackgroundResource(R.drawable.bg_button);
            modeAllHistory.setBackgroundResource(R.drawable.bg_edit);
            modeRange.setTextColor(Color.rgb(7, 19, 31));
            modeAllHistory.setTextColor(Color.WHITE);

            rangePanel.setVisibility(android.view.View.VISIBLE);
            historyInfo.setText(
                    "RENTANG WAKTU: pilih tanggal mulai dan tanggal akhir. "
                            + "Permintaan panjang juga dipecah otomatis saat diperlukan.");
            statusView.setText("Pilih tanggal kemudian unduh.");
        }
    }

    private void refreshDateLabels() {
        startView.setText(startDate.toString());
        endView.setText(endDate.toString());
    }

    private void pickStartDate() {
        showPicker(startDate, (view, y, m, d) -> {
            LocalDate selected = LocalDate.of(y, m + 1, d);
            if (selected.isAfter(endDate)) {
                Toast.makeText(
                        this,
                        "Tanggal mulai tidak boleh melewati tanggal akhir.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            startDate = selected;
            refreshDateLabels();
        });
    }

    private void pickEndDate() {
        showPicker(endDate, (view, y, m, d) -> {
            LocalDate selected = LocalDate.of(y, m + 1, d);
            if (selected.isBefore(startDate)) {
                Toast.makeText(
                        this,
                        "Tanggal akhir tidak boleh sebelum tanggal mulai.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            endDate = selected;
            refreshDateLabels();
        });
    }

    private void showPicker(
            LocalDate date,
            DatePickerDialog.OnDateSetListener listener
    ) {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                listener,
                date.getYear(),
                date.getMonthValue() - 1,
                date.getDayOfMonth()
        );
        dialog.show();
    }

    /**
     * Read string-like values defensively: older builds may have saved a setting
     * under an alias or as a numeric preference. A visible default in Settings
     * must also be the effective default used by CSV export.
     */
    private String preferenceText(String key) {
        try {
            Object value = prefs.getAll().get(key);
            return value == null ? "" : String.valueOf(value).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String firstPreferenceText(String... keys) {
        for (String key : keys) {
            String value = preferenceText(key);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private String configuredChannel() {
        String value = firstPreferenceText("channel", "channel_id", "channelId", "thingspeak_channel_id");
        if (value.isEmpty()) value = DEFAULT_CHANNEL;
        // Strip copied spaces without silently guessing or changing the ID digits.
        return value.replaceAll("\\s+", "");
    }

    private String configuredReadKey() {
        String value = firstPreferenceText("read_key", "read_api_key", "readApiKey", "thingspeak_read_key");
        return value.isEmpty() ? DEFAULT_READ_KEY : value;
    }

    private String parseOpenMeteoError(String body) {
        if (body == null || body.trim().isEmpty()) return "";
        String text = body.trim();
        try {
            JSONObject json = new JSONObject(text);
            String reason = json.optString("reason", "").trim();
            if (!reason.isEmpty()) return reason;
            if (json.optBoolean("error", false)) {
                return "Permintaan ditolak API; periksa parameter atau rentang tanggal.";
            }
        } catch (Exception ignored) {
            // Use the bounded raw response below when it is not JSON.
        }
        return text.length() > 240 ? text.substring(0, 240) + "…" : text;
    }

    private void prepareCsv() {
        if (openMeteoMode) {
            if ("OpenWeather".equals(selectedCsvWeatherProvider())) {
                statusView.setText("OpenWeather endpoint standar tidak menyediakan data historis. Pilih Open-Meteo atau WeatherAPI untuk membuat CSV historis. OpenWeather tetap digunakan untuk cuaca saat ini dan prakiraan di Dashboard/Catatan Lapangan.");
                return;
            } else if ("WeatherAPI".equals(selectedCsvWeatherProvider())) prepareWeatherApiCsv();
            else prepareOpenMeteoCsv();
        } else {
            prepareThingSpeakCsv();
        }
    }

    private void prepareThingSpeakCsv() {
        if (downloading) return;

        String channel = configuredChannel();
        String key = configuredReadKey();

        if (channel.isEmpty()) {
            // Defensive guard. configuredChannel() normally falls back to the
            // same default channel used by the dashboard/settings screen.
            statusView.setText("Channel ID ThingSpeak belum tersedia. Buka Pengaturan, isi Channel ID, lalu Simpan.");
            return;
        }
        if (!channel.matches("\\d+")) {
            statusView.setText("Channel ID ThingSpeak tidak valid. Gunakan ID numerik saja; periksa Pengaturan.");
            return;
        }

        if (!allHistoryMode && startDate.isAfter(endDate)) {
            statusView.setText("Rentang tanggal tidak valid.");
            return;
        }

        downloading = true;
        readyToSave = false;
        downloadButton.setText("MENGAMBIL DATA...");
        downloadButton.setEnabled(false);

        final LocalDateTime exportStart;
        final LocalDateTime exportEnd;
        final String fileName;

        if (allHistoryMode) {
            exportStart = HISTORY_START;
            // Use the current local device time as the upper boundary. Adding one
            // second avoids excluding an entry created in the current second.
            exportEnd = LocalDateTime.now(WIB).plusSeconds(1);
            fileName = "STASIUN-CUACA-DESA-TURUS_SELURUH-HISTORI.csv";
            statusView.setText("Menyiapkan seluruh histori ThingSpeak • Channel ID " + channel + "...");
        } else {
            exportStart = startDate.atStartOfDay();
            // The selected end date is inclusive through 23:59:59 WIB.
            exportEnd = endDate.plusDays(1).atStartOfDay().minusSeconds(1);
            fileName = "STASIUN-CUACA-DESA-TURUS_"
                    + startDate.format(FILE_FMT)
                    + "_"
                    + endDate.format(FILE_FMT)
                    + ".csv";
            statusView.setText(
                    "Mengambil data ThingSpeak • Channel ID " + channel + " • "
                            + startDate + " sampai " + endDate + "..."
            );
        }

        net.execute(() -> {
            File temp = null;
            try {
                temp = File.createTempFile(
                        "stasiun_cuaca_turus_",
                        ".csv",
                        getCacheDir()
                );

                ExportStats stats = new ExportStats();
                final File tempFile = temp;

                try (Writer writer = Files.newBufferedWriter(
                        temp.toPath(),
                        StandardCharsets.UTF_8
                )) {
                    // UTF-8 BOM helps spreadsheet applications recognize UTF-8,
                    // while remaining valid UTF-8 CSV.
                    writer.write('\uFEFF');

                    fetchIntervalAdaptive(
                            channel,
                            key,
                            exportStart,
                            exportEnd,
                            writer,
                            stats,
                            0
                    );

                    writer.flush();
                }

                if (stats.rows <= 0 || stats.headerWritten == false) {
                    throw new Exception(
                            "Tidak ada data pada " +
                                    (allHistoryMode ? "histori channel." : "rentang tanggal tersebut.")
                    );
                }

                pendingCsvFile = tempFile;
                pendingFileName = fileName;
                readyToSave = true;

                final long totalRows = stats.rows;
                final int requests = stats.requests;

                runOnUiThread(() -> {
                    downloading = false;
                    downloadButton.setEnabled(true);
                    downloadButton.setText("SIMPAN FILE CSV");
                    statusView.setText(
                            "Data siap: "
                                    + totalRows
                                    + " baris • "
                                    + requests
                                    + " permintaan API. Pilih lokasi penyimpanan CSV."
                    );
                });

            } catch (Exception ex) {
                if (temp != null) {
                    try { temp.delete(); } catch (Exception ignored) {}
                }
                pendingCsvFile = null;
                readyToSave = false;

                final String message = safeMessage(ex);
                runOnUiThread(() -> {
                    downloading = false;
                    downloadButton.setEnabled(true);
                    downloadButton.setText("UNDUH DATA CSV");
                    statusView.setText("Gagal: " + message);
                });
            }
        });
    }

    private void prepareOpenMeteoCsv() {
        if (downloading) return;

        double lat = prefs.getFloat("latitude", Float.NaN);
        double lon = prefs.getFloat("longitude", Float.NaN);
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
            statusView.setText("Koordinat GPS belum tersedia. Gunakan GPS HP atau isi latitude/longitude di Pengaturan.");
            return;
        }
        LocalDate today = LocalDate.now(WIB);
        if (startDate.isAfter(endDate)) {
            statusView.setText("Rentang tanggal tidak valid.");
            return;
        }
        if (endDate.isAfter(today)) {
            statusView.setText("Tanggal akhir Open-Meteo tidak boleh melewati hari ini.");
            return;
        }

        downloading = true;
        readyToSave = false;
        downloadButton.setText("MENGAMBIL CUACA MAKRO...");
        downloadButton.setEnabled(false);

        final LocalDate exportStart = startDate;
        final LocalDate exportEnd = endDate;
        final boolean hourly = openMeteoHourly;
        final String resolution = hourly ? "hourly" : "daily";
        final String fileName = "STASIUN-CUACA_CUACA-MAKRO_OPEN-METEO_"
                + startDate.format(FILE_FMT) + "_" + endDate.format(FILE_FMT) + "_" + resolution + ".csv";
        statusView.setText("Mengambil Open-Meteo " + resolution + " " + startDate + " sampai " + endDate + "...");

        net.execute(() -> {
            File temp = null;
            try {
                temp = File.createTempFile("stasiun_cuaca_openmeteo_", ".csv", getCacheDir());
                final File tempFile = temp;
                OpenMeteoStats stats = new OpenMeteoStats();
                try (Writer writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8)) {
                    writer.write('\uFEFF');
                    fetchOpenMeteoHistorical(lat, lon, exportStart, exportEnd, hourly, writer, stats);
                    writer.flush();
                }
                if (stats.rows <= 0) throw new Exception("Open-Meteo tidak mengembalikan data untuk rentang tersebut.");

                pendingCsvFile = tempFile;
                pendingFileName = fileName;
                readyToSave = true;
                final long rows = stats.rows;
                runOnUiThread(() -> {
                    downloading = false;
                    downloadButton.setEnabled(true);
                    downloadButton.setText("SIMPAN FILE CSV");
                    statusView.setText("Open-Meteo siap disimpan: " + rows + " baris • " + resolution + " • GPS "
                            + String.format(Locale.US, "%.5f, %.5f", lat, lon));
                });
            } catch (Exception ex) {
                if (temp != null) try { temp.delete(); } catch (Exception ignored) {}
                pendingCsvFile = null;
                readyToSave = false;
                final String message = safeMessage(ex);
                runOnUiThread(() -> {
                    downloading = false;
                    downloadButton.setEnabled(true);
                    downloadButton.setText("UNDUH DATA CSV");
                    statusView.setText("Gagal Open-Meteo: " + message);
                });
            }
        });
    }

    private void fetchOpenMeteoHistorical(double lat, double lon, LocalDate start, LocalDate end,
                                          boolean hourly, Writer writer, OpenMeteoStats stats) throws Exception {
        String url;
        if (hourly) {
            // Historical Weather API does not expose hourly visibility or UV index.
            // Requesting either unsupported variable rejects the whole request (HTTP 400).
            // The legacy CSV columns are retained for compatibility and left blank.
            String vars = "temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,precipitation,rain,weather_code,surface_pressure,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m,shortwave_radiation,vapour_pressure_deficit,et0_fao_evapotranspiration,sunshine_duration,soil_temperature_0_to_7cm,soil_temperature_7_to_28cm,soil_moisture_0_to_7cm,soil_moisture_7_to_28cm";
            url = "https://archive-api.open-meteo.com/v1/archive?latitude=" + enc(String.format(Locale.US, "%.6f", lat))
                    + "&longitude=" + enc(String.format(Locale.US, "%.6f", lon))
                    + "&start_date=" + start + "&end_date=" + end
                    + "&hourly=" + enc(vars)
                    + "&timezone=Asia%2FJakarta&temperature_unit=celsius&wind_speed_unit=ms&precipitation_unit=mm&timeformat=iso8601&cell_selection=land";
        } else {
            String vars = "weather_code,temperature_2m_mean,temperature_2m_max,temperature_2m_min,apparent_temperature_mean,apparent_temperature_max,apparent_temperature_min,precipitation_sum,rain_sum,precipitation_hours,sunrise,sunset,daylight_duration,sunshine_duration,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,shortwave_radiation_sum,et0_fao_evapotranspiration";
            String hourlySoil = "soil_moisture_0_to_7cm,soil_moisture_7_to_28cm";
            url = "https://archive-api.open-meteo.com/v1/archive?latitude=" + enc(String.format(Locale.US, "%.6f", lat))
                    + "&longitude=" + enc(String.format(Locale.US, "%.6f", lon))
                    + "&start_date=" + start + "&end_date=" + end
                    + "&hourly=" + enc(hourlySoil)
                    + "&daily=" + enc(vars)
                    + "&timezone=Asia%2FJakarta&temperature_unit=celsius&wind_speed_unit=ms&precipitation_unit=mm&timeformat=iso8601&cell_selection=land";
        }

        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS * 2);
            c.setUseCaches(false);
            c.setRequestProperty("Accept", "application/json");
            int code = c.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                String detail = readErrorBody(c);
                String explanation = parseOpenMeteoError(detail);
                throw new Exception("Open-Meteo HTTP " + code
                        + (explanation.isEmpty() ? "" : " • " + explanation));
            }
            JSONObject root = new JSONObject(readAll(c.getInputStream()));
            double responseLat = root.optDouble("latitude", lat);
            double responseLon = root.optDouble("longitude", lon);
            double elevation = root.optDouble("elevation", Double.NaN);
            if (hourly) {
                JSONObject h = root.optJSONObject("hourly");
                if (h == null) throw new Exception("Respons Open-Meteo tidak memiliki data hourly.");
                JSONArray times = h.optJSONArray("time");
                if (times == null) throw new Exception("Kolom waktu hourly Open-Meteo tidak tersedia.");
                writer.write(csvLine("timestamp_wib","source","data_type","latitude","longitude","elevation_m",
                        "temperature_2m_c","relative_humidity_pct","dew_point_c","apparent_temperature_c","precipitation_mm","rain_mm","weather_code","surface_pressure_hpa","cloud_cover_pct","wind_speed_ms","wind_direction_deg","wind_gust_ms","visibility_m","uv_index","shortwave_radiation_wm2","shortwave_radiation_energy_MJ_m2_hour","PAR_est_W_m2_hourmean","PPFD_est_umol_m2_s_hourmean","lux_est_hourmean","vpd_kpa","et0_FAO56_PM_mm_this_hour","sunshine_duration_h_this_hour","soil_temperature_0_7cm_c","soil_temperature_7_28cm_c","soil_moisture_0_7cm_pctvol","soil_moisture_7_28cm_pctvol","radiation_method","PAR_method","PPFD_method","lux_method","et0_method","sunshine_method","soil_moisture_method"));
                writer.write("\r\n");
                for (int i = 0; i < times.length(); i++) {
                    String timestamp = times.optString(i, "");
                    double sw = arrNum(h, "shortwave_radiation", i);
                    String sunshineHours = fmtCsv(arrNum(h, "sunshine_duration", i) / 3600.0, 3);
                    writer.write(csvLine(timestamp,"OPEN-METEO","HISTORICAL_REANALYSIS",
                            fmtCsv(responseLat,5),fmtCsv(responseLon,5),fmtCsv(elevation,1),
                            fmtCsv(arrNum(h,"temperature_2m",i),2),fmtCsv(arrNum(h,"relative_humidity_2m",i),1),
                            fmtCsv(arrNum(h,"dew_point_2m",i),2),fmtCsv(arrNum(h,"apparent_temperature",i),2),
                            fmtCsv(arrNum(h,"precipitation",i),3),fmtCsv(arrNum(h,"rain",i),3),
                            csvNum(h,"weather_code",i),fmtCsv(arrNum(h,"surface_pressure",i),1),fmtCsv(arrNum(h,"cloud_cover",i),1),
                            fmtCsv(arrNum(h,"wind_speed_10m",i),2),fmtCsv(arrNum(h,"wind_direction_10m",i),1),fmtCsv(arrNum(h,"wind_gusts_10m",i),2),
                            "","",fmtCsv(sw,2),fmtCsv(sw*3600.0/1_000_000.0,5),fmtCsv(LightConversion.shortwaveToParWm2(sw),3),
                            fmtCsv(LightConversion.parToPpfd(LightConversion.shortwaveToParWm2(sw)),3),fmtCsv(sw*120.0,1),fmtCsv(arrNum(h,"vapour_pressure_deficit",i),3),
                            fmtCsv(arrNum(h,"et0_fao_evapotranspiration",i),3),sunshineHours,
                            fmtCsv(arrNum(h,"soil_temperature_0_to_7cm",i),2),fmtCsv(arrNum(h,"soil_temperature_7_to_28cm",i),2),
                            fmtCsv(arrNum(h,"soil_moisture_0_to_7cm",i) * 100.0,2),fmtCsv(arrNum(h,"soil_moisture_7_to_28cm",i) * 100.0,2),
                            "Open-Meteo hourly shortwave_radiation", "PAR=0.45×shortwave; estimated", "PPFD=PAR×4.57; estimated", "lux≈shortwave×120; broad-spectrum estimate", "Open-Meteo hourly ET₀ FAO-56 PM", "Open-Meteo sunshine_duration for hour", "Open-Meteo grid soil moisture m³/m³×100"));
                    writer.write("\r\n");
                    stats.rows++;
                    if (i % 48 == 0) postProgressOpenMeteo(stats, "Open-Meteo hourly: memproses...");
                }
            } else {
                JSONObject d = root.optJSONObject("daily");
                if (d == null) throw new Exception("Respons Open-Meteo tidak memiliki data daily.");
                JSONArray times = d.optJSONArray("time");
                if (times == null) throw new Exception("Kolom waktu daily Open-Meteo tidak tersedia.");
                writer.write(csvLine("date","source","data_type","latitude","longitude","elevation_m",
                        "weather_code","temperature_mean_c","temperature_max_c","temperature_min_c","apparent_temperature_mean_c","apparent_temperature_max_c","apparent_temperature_min_c",
                        "precipitation_sum_mm","rain_sum_mm","precipitation_hours","sunrise","sunset","daylight_hours","sunshine_hours","wind_speed_max_ms","wind_gusts_max_ms","wind_direction_dominant_deg","shortwave_radiation_sum_MJ_m2","et0_mm",
                        "soil_moisture_0_7cm_mean_pctvol","soil_moisture_7_28cm_mean_pctvol","shortwave_daylight_mean_W_m2_est","PAR_daylight_mean_W_m2_est","PPFD_daylight_mean_umol_m2_s_est","lux_daylight_mean_est","PAR_energy_sum_MJ_m2_est","radiation_method","PAR_method","PPFD_method","lux_method","et0_method","sunshine_method","soil_moisture_method"));
                writer.write("\r\n");
                for (int i=0;i<times.length();i++) {
                    writer.write(csvLine(times.optString(i,""),"OPEN-METEO","HISTORICAL_REANALYSIS",
                            fmtCsv(responseLat,5),fmtCsv(responseLon,5),fmtCsv(elevation,1),csvNum(d,"weather_code",i),
                            fmtCsv(arrNum(d,"temperature_2m_mean",i),2),fmtCsv(arrNum(d,"temperature_2m_max",i),2),fmtCsv(arrNum(d,"temperature_2m_min",i),2),
                            fmtCsv(arrNum(d,"apparent_temperature_mean",i),2),fmtCsv(arrNum(d,"apparent_temperature_max",i),2),fmtCsv(arrNum(d,"apparent_temperature_min",i),2),
                            fmtCsv(arrNum(d,"precipitation_sum",i),3),fmtCsv(arrNum(d,"rain_sum",i),3),fmtCsv(arrNum(d,"precipitation_hours",i),2),
                            arrString(d,"sunrise",i),arrString(d,"sunset",i),fmtCsv(arrNum(d,"daylight_duration",i)/3600.0,3),fmtCsv(arrNum(d,"sunshine_duration",i)/3600.0,3),
                            fmtCsv(arrNum(d,"wind_speed_10m_max",i),2),fmtCsv(arrNum(d,"wind_gusts_10m_max",i),2),fmtCsv(arrNum(d,"wind_direction_10m_dominant",i),1),
                            fmtCsv(arrNum(d,"shortwave_radiation_sum",i),3),fmtCsv(arrNum(d,"et0_fao_evapotranspiration",i),3),
                            fmtCsv(meanHourlyForDate(root,"soil_moisture_0_to_7cm",times.optString(i,""))*100.0,2),
                            fmtCsv(meanHourlyForDate(root,"soil_moisture_7_to_28cm",times.optString(i,""))*100.0,2),
                            fmtCsv(daylightMeanWm2(arrNum(d,"shortwave_radiation_sum",i),arrNum(d,"daylight_duration",i)),2),
                            fmtCsv(daylightMeanWm2(arrNum(d,"shortwave_radiation_sum",i),arrNum(d,"daylight_duration",i))*0.45,2),
                            fmtCsv(daylightMeanWm2(arrNum(d,"shortwave_radiation_sum",i),arrNum(d,"daylight_duration",i))*0.45*4.57,2),
                            fmtCsv(daylightMeanWm2(arrNum(d,"shortwave_radiation_sum",i),arrNum(d,"daylight_duration",i))*120.0,1),
                            fmtCsv(arrNum(d,"shortwave_radiation_sum",i)*0.45,3),
                            "Open-Meteo shortwave_radiation_sum", "PAR=0.45×shortwave; estimated", "PPFD=PAR×4.57; estimated", "lux≈shortwave×120; broad-spectrum estimate", "Open-Meteo daily ET₀ FAO-56 PM", "Open-Meteo daily sunshine_duration", "Open-Meteo hourly reanalysis soil moisture m³/m³×100"));
                    writer.write("\r\n");
                    stats.rows++;
                    if (i % 7 == 0) postProgressOpenMeteo(stats, "Open-Meteo harian: memproses...");
                }
            }
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private double meanHourlyForDate(JSONObject root, String variable, String date) {
        JSONObject hourly = root.optJSONObject("hourly");
        if (hourly == null) return Double.NaN;
        JSONArray times = hourly.optJSONArray("time"), values = hourly.optJSONArray(variable);
        if (times == null || values == null) return Double.NaN;
        double sum = 0; int count = 0;
        for (int i = 0; i < Math.min(times.length(), values.length()); i++) {
            if (!times.optString(i, "").startsWith(date) || values.isNull(i)) continue;
            double v = values.optDouble(i, Double.NaN);
            if (Double.isFinite(v)) { sum += v; count++; }
        }
        return count == 0 ? Double.NaN : sum / count;
    }

    private double daylightMeanWm2(double radiationMj, double daylightSeconds) {
        return Double.isFinite(radiationMj) && Double.isFinite(daylightSeconds) && daylightSeconds > 0
                ? radiationMj * 1_000_000.0 / daylightSeconds : Double.NaN;
    }

    private void prepareWeatherApiCsv() {
        if (downloading) return;
        String key = prefs.getString("weatherapi_api_key", "").trim();
        if (key.isEmpty()) { statusView.setText("API key WeatherAPI belum diisi. Buka Pengaturan → Cuaca Makro → WeatherAPI."); return; }
        double lat = prefs.getFloat("latitude", Float.NaN), lon = prefs.getFloat("longitude", Float.NaN);
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) { statusView.setText("Koordinat GPS belum tersedia. Gunakan GPS HP atau isi latitude/longitude di Pengaturan."); return; }
        LocalDate today = LocalDate.now(WIB);
        if (startDate.isAfter(endDate)) { statusView.setText("Rentang tanggal tidak valid."); return; }
        if (startDate.isBefore(LocalDate.of(2010, 1, 1))) { statusView.setText("WeatherAPI History API tersedia mulai 1 Januari 2010. Ubah tanggal awal."); return; }
        if (!endDate.isBefore(today)) { statusView.setText("Untuk histori WeatherAPI, pilih tanggal akhir paling lambat kemarin agar data satu hari penuh."); return; }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > 31) {
            new AlertDialog.Builder(this).setTitle("Ekspor histori WeatherAPI")
                    .setMessage("Rentang " + days + " hari akan memerlukan hingga " + days + " permintaan History API. Ketersediaan tanggal dan parameter bergantung pada paket/key WeatherAPI; kuota dapat terpakai.")
                    .setNegativeButton("BATAL", null)
                    .setPositiveButton("LANJUTKAN", (dialog, which) -> startWeatherApiCsv(lat, lon, startDate, endDate, days))
                    .show();
            return;
        }
        startWeatherApiCsv(lat, lon, startDate, endDate, days);
    }

    private void startWeatherApiCsv(double lat, double lon, LocalDate exportStart, LocalDate exportEnd, long days) {
        downloading = true; readyToSave = false;
        downloadButton.setText("MENGAMBIL WEATHERAPI..."); downloadButton.setEnabled(false);
        final String fileName = "STASIUN-CUACA_CUACA-MAKRO_WEATHERAPI_" + exportStart.format(FILE_FMT) + "_" + exportEnd.format(FILE_FMT) + "_daily.csv";
        statusView.setText("Mengambil histori WeatherAPI harian • sekitar " + days + " permintaan...");
        net.execute(() -> {
            File temp = null;
            try {
                temp = File.createTempFile("stasiun_cuaca_weatherapi_", ".csv", getCacheDir());
                final File tempFile = temp;
                OpenMeteoStats stats = new OpenMeteoStats();
                try (Writer writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8)) {
                    writer.write('\uFEFF');
                    fetchWeatherApiHistorical(lat, lon, exportStart, exportEnd, writer, stats);
                    writer.flush();
                }
                if (stats.rows <= 0) throw new Exception("WeatherAPI tidak mengembalikan data pada rentang tersebut.");
                pendingCsvFile = tempFile; pendingFileName = fileName; readyToSave = true;
                final long rows = stats.rows;
                runOnUiThread(() -> {
                    downloading = false; downloadButton.setEnabled(true); downloadButton.setText("SIMPAN FILE CSV");
                    statusView.setText("WeatherAPI siap disimpan: " + rows + " hari • " + days + " permintaan API • GPS " + String.format(Locale.US, "%.5f, %.5f", lat, lon));
                });
            } catch (Exception ex) {
                if (temp != null) try { temp.delete(); } catch (Exception ignored) {}
                pendingCsvFile = null; readyToSave = false;
                final String message = safeMessage(ex);
                runOnUiThread(() -> { downloading = false; downloadButton.setEnabled(true); downloadButton.setText("UNDUH DATA CSV"); statusView.setText("Gagal WeatherAPI: " + message); });
            }
        });
    }

    private void fetchWeatherApiHistorical(double lat, double lon, LocalDate start, LocalDate end, Writer writer, OpenMeteoStats stats) throws Exception {
        String key = prefs.getString("weatherapi_api_key", "").trim();
        double elevation = prefs.getFloat("elevation", 0f);
        double fc = preferenceNumber("soil_fc_pct", 30.0), pwp = preferenceNumber("soil_pwp_pct", 12.0);
        double rootDepth = preferenceNumber("soil_root_depth_cm", 30.0);
        if (!Double.isFinite(fc) || !Double.isFinite(pwp) || fc <= pwp) { fc = 30.0; pwp = 12.0; }
        if (!Double.isFinite(rootDepth) || rootDepth <= 0) rootDepth = 30.0;
        double kc = WeatherMacroModel.cropCoefficient(prefs.getString("farm_phase_manual", ""));
        double availableFraction = 0.50; // explicit model state; not measured soil moisture.
        writer.write(csvLine("date_local","source","data_type","latitude","longitude","temperature_mean_c","temperature_max_c","temperature_min_c","relative_humidity_mean_pct","pressure_mean_hpa","cloud_cover_mean_pct","precipitation_total_mm","wind_speed_max_ms","wind_direction_mean_deg","uv_index_max","sunrise_local","sunset_local","daylight_hours","estimated_sunshine_hours","shortwave_radiation_sum_MJ_m2","shortwave_source","PAR_est_daylight_mean_W_m2","PPFD_est_daylight_mean_umol_m2_s","lux_est_daylight_mean","ET0_mm","ET0_source","estimated_soil_moisture_pctvol","soil_moisture_method"));
        writer.write("\r\n");
        long total = ChronoUnit.DAYS.between(start, end) + 1;
        long done = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String query = enc(String.format(Locale.US, "%.6f,%.6f", lat, lon));
            String url = "https://api.weatherapi.com/v1/history.json?key=" + enc(key) + "&q=" + query + "&dt=" + date + "&aqi=no";
            JSONObject root = getJsonCsv(url);
            JSONObject forecast = root.optJSONObject("forecast");
            JSONArray fdays = forecast == null ? null : forecast.optJSONArray("forecastday");
            JSONObject fd = fdays == null ? null : fdays.optJSONObject(0);
            if (fd == null) throw new Exception("WeatherAPI tidak mengembalikan histori tanggal " + date + ". Periksa paket/key dan tanggal.");
            JSONObject day = fd.optJSONObject("day"); if (day == null) day = new JSONObject();
            JSONObject astro = fd.optJSONObject("astro"); if (astro == null) astro = new JSONObject();
            JSONArray hours = fd.optJSONArray("hour");
            double rhMean = finiteMean(hours, "humidity");
            if (!Double.isFinite(rhMean)) rhMean = day.optDouble("avghumidity", Double.NaN);
            double pMean = finiteMean(hours, "pressure_mb");
            double cloudMean = finiteMean(hours, "cloud");
            double windDir = circularMeanDegrees(hours, "wind_degree");
            double maxWindKph = day.optDouble("maxwind_kph", Double.NaN);
            double rain = day.optDouble("totalprecip_mm", 0.0);
            double tMax = day.optDouble("maxtemp_c", Double.NaN), tMin = day.optDouble("mintemp_c", Double.NaN);
            double tMean = day.optDouble("avgtemp_c", Double.NaN);
            double windMs = maxWindKph / 3.6;
            WeatherMacroModel.DailyDerived model = WeatherMacroModel.estimateDaily(lat, elevation, date, tMax, tMin, rhMean, pMean, windMs, cloudMean);
            // Use direct short_rad/ET0 where the subscriber response exposes them; otherwise retain a clearly labelled estimate.
            double directShortRadEnergy = sumHourlyRadiationMjm2(hours);
            boolean hasDirectShortRad = Double.isFinite(directShortRadEnergy);
            double shortwaveEnergy = hasDirectShortRad ? directShortRadEnergy : model.solarRadiationMjM2;
            String radiationSource = hasDirectShortRad ? "WeatherAPI short_rad; direct provider field" : "Hargreaves-Samani estimate; not directly returned by this package";
            double et0 = sumHourlyField(hours, "et0");
            boolean hasDirectEt0 = Double.isFinite(et0);
            if (!hasDirectEt0) et0 = model.et0Mm;
            String et0Source = hasDirectEt0 ? "WeatherAPI hourly ET0; direct provider field" : "FAO-56 PM estimate; not direct API field";
            double daylightHours = daylightHoursFromAstro(astro);
            if (!Double.isFinite(daylightHours)) daylightHours = model.daylightHours;
            double swMean = Double.isFinite(shortwaveEnergy) && daylightHours > 0 ? shortwaveEnergy * 1_000_000.0 / (daylightHours * 3600.0) : Double.NaN;
            double parMean = LightConversion.shortwaveToParWm2(swMean);
            double ppfd = LightConversion.parToPpfd(parMean);
            double lux = Double.isFinite(swMean) ? swMean * 120.0 : Double.NaN;
            availableFraction = WeatherMacroModel.nextAvailableWaterFraction(availableFraction, rain, et0, kc, fc, pwp, rootDepth);
            double soilPct = WeatherMacroModel.moisturePctVol(availableFraction, fc, pwp);
            writer.write(csvLine(date.toString(),"WeatherAPI","HISTORICAL_DAILY",""+fmtCsv(lat,5),fmtCsv(lon,5),fmtCsv(tMean,2),fmtCsv(tMax,2),fmtCsv(tMin,2),fmtCsv(rhMean,2),fmtCsv(pMean,2),fmtCsv(cloudMean,2),fmtCsv(rain,3),fmtCsv(windMs,2),fmtCsv(windDir,1),fmtCsv(day.optDouble("uv",Double.NaN),2),astro.optString("sunrise",""),astro.optString("sunset",""),fmtCsv(daylightHours,3),fmtCsv(model.sunshineHours,3),fmtCsv(shortwaveEnergy,3),radiationSource,fmtCsv(parMean,3),fmtCsv(ppfd,3),fmtCsv(lux,1),fmtCsv(et0,3),et0Source,fmtCsv(soilPct,3),"Water-bucket estimate from rainfall and ET0; FC/PWP/root depth and initial fraction are assumptions unless configured"));
            writer.write("\r\n");
            stats.rows++;
            done++;
            if (done % 3 == 0 || done == total) {
                final long progress = done;
                main.post(() -> statusView.setText("WeatherAPI histori: " + progress + "/" + total + " hari diproses..."));
            }
        }
    }

    private double finiteMean(JSONArray objects, String key) {
        if (objects == null) return Double.NaN;
        double sum = 0; int n = 0;
        for (int i=0; i<objects.length(); i++) {
            JSONObject o=objects.optJSONObject(i); if(o==null || o.isNull(key)) continue;
            double v=o.optDouble(key,Double.NaN); if(Double.isFinite(v)){sum+=v;n++;}
        }
        return n == 0 ? Double.NaN : sum / n;
    }

    private double circularMeanDegrees(JSONArray objects, String key) {
        if (objects == null) return Double.NaN;
        double sx=0, sy=0; int n=0;
        for(int i=0;i<objects.length();i++){
            JSONObject o=objects.optJSONObject(i); if(o==null || o.isNull(key)) continue;
            double d=o.optDouble(key,Double.NaN); if(!Double.isFinite(d)) continue;
            sx+=Math.cos(Math.toRadians(d)); sy+=Math.sin(Math.toRadians(d)); n++;
        }
        if(n==0) return Double.NaN;
        double deg=Math.toDegrees(Math.atan2(sy/n,sx/n)); return deg<0?deg+360:deg;
    }

    private double sumHourlyField(JSONArray objects, String key) {
        if (objects == null) return Double.NaN;
        double sum=0; int n=0;
        for(int i=0;i<objects.length();i++){
            JSONObject o=objects.optJSONObject(i); if(o==null || o.isNull(key)) continue;
            double v=o.optDouble(key,Double.NaN); if(Double.isFinite(v)){sum+=v;n++;}
        }
        return n == 0 ? Double.NaN : sum;
    }

    private double sumHourlyRadiationMjm2(JSONArray objects) {
        if (objects == null) return Double.NaN;
        double sum=0; int n=0;
        for(int i=0;i<objects.length();i++){
            JSONObject o=objects.optJSONObject(i); if(o==null || o.isNull("short_rad")) continue;
            double v=o.optDouble("short_rad",Double.NaN); if(Double.isFinite(v)){sum += Math.max(0,v) * 3600.0 / 1_000_000.0; n++;}
        }
        return n == 0 ? Double.NaN : sum;
    }

    private double daylightHoursFromAstro(JSONObject astro) {
        try {
            java.time.format.DateTimeFormatter f=java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.US);
            java.time.LocalTime rise=java.time.LocalTime.parse(astro.optString("sunrise",""),f);
            java.time.LocalTime set=java.time.LocalTime.parse(astro.optString("sunset",""),f);
            long mins=java.time.Duration.between(rise,set).toMinutes(); if(mins<0) mins+=24*60;
            return mins / 60.0;
        } catch(Exception ignored) { return Double.NaN; }
    }

    private double preferenceNumber(String key, double fallback) {
        try { String s = prefs.getString(key, "").trim(); if (!s.isEmpty()) { double d = Double.parseDouble(s.replace(',', '.')); if (Double.isFinite(d)) return d; } }
        catch (Exception ignored) { }
        return fallback;
    }

    private JSONObject getJsonCsv(String url) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET"); c.setConnectTimeout(CONNECT_TIMEOUT_MS); c.setReadTimeout(READ_TIMEOUT_MS * 2);
            c.setUseCaches(false); c.setRequestProperty("Accept", "application/json");
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                String detail = readErrorBody(c);
                String bounded = detail == null ? "" : detail.trim();
                try { JSONObject e = new JSONObject(bounded); String msg=e.optString("message","").trim(); String cod=e.optString("cod","").trim(); if(!msg.isEmpty()) bounded=msg+(cod.isEmpty()?"":" ("+cod+")"); } catch(Exception ignored){}
                throw new Exception("OpenWeather HTTP " + code + (bounded.isEmpty() ? "" : " • " + (bounded.length()>260?bounded.substring(0,260)+"…":bounded)));
            }
            return new JSONObject(readAll(c.getInputStream()));
        } finally { if (c != null) c.disconnect(); }
    }

    private void postProgressOpenMeteo(OpenMeteoStats stats, String message) {
        final long rows = stats.rows;
        main.post(() -> statusView.setText(message + " " + rows + " baris"));
    }

    private String enc(String s) throws Exception { return URLEncoder.encode(s, "UTF-8"); }

    private double arrNum(JSONObject obj, String key, int index) {
        JSONArray a = obj.optJSONArray(key);
        if (a == null || index < 0 || index >= a.length() || a.isNull(index)) return Double.NaN;
        return a.optDouble(index, Double.NaN);
    }

    private String arrString(JSONObject obj, String key, int index) {
        JSONArray a = obj.optJSONArray(key);
        return a == null || index < 0 || index >= a.length() || a.isNull(index) ? "" : a.optString(index, "");
    }

    private String csvNum(JSONObject obj, String key, int index) {
        JSONArray a = obj.optJSONArray(key);
        if (a == null || index < 0 || index >= a.length() || a.isNull(index)) return "";
        return a.optString(index, "");
    }

    private String fmtCsv(double value, int decimals) {
        return Double.isFinite(value) ? String.format(Locale.US, "%1$." + decimals + "f", value) : "";
    }

    private String csvLine(String... values) {
        StringBuilder b = new StringBuilder();
        for (int i=0;i<values.length;i++) {
            if (i>0) b.append(',');
            String v = values[i] == null ? "" : values[i];
            if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) {
                b.append('"').append(v.replace("\"", "\"\"")).append('"');
            } else {
                b.append(v);
            }
        }
        return b.toString();
    }

    /**
     * Reads one interval. If the response contains exactly 8,000 data rows,
     * the interval is split into two smaller intervals until each response is
     * safely below the API limit. This means long histories are not silently
     * truncated to the newest 8,000 records.
     */
    private void fetchIntervalAdaptive(
            String channel,
            String key,
            LocalDateTime start,
            LocalDateTime end,
            Writer writer,
            ExportStats stats,
            int depth
    ) throws Exception {
        if (depth > MAX_SPLIT_DEPTH) {
            throw new Exception(
                    "Rentang terlalu padat untuk dipecah lebih lanjut. "
                            + "Coba gunakan rentang tanggal yang lebih pendek."
            );
        }

        if (!start.isBefore(end)) return;

        CsvBatch batch = fetchCsvBatch(channel, key, start, end);
        stats.requests++;

        if (batch.lines.isEmpty()) {
            postProgress(stats, "Memeriksa rentang kosong...");
            return;
        }

        if (batch.lines.size() < MAX_RESULTS_PER_REQUEST) {
            appendBatch(writer, batch, stats);
            postProgress(
                    stats,
                    "Mengambil data... "
                            + stats.rows
                            + " baris • "
                            + stats.requests
                            + " permintaan"
            );
            return;
        }

        // The API returned the maximum number of rows. Do not append this
        // potentially truncated batch. Split it and fetch both halves instead.
        Duration duration = Duration.between(start, end);
        long seconds = duration.getSeconds();

        if (seconds <= 2) {
            throw new Exception(
                    "Terdapat 8.000 entry dalam interval yang sangat rapat. "
                            + "ThingSpeak membatasi 8.000 hasil per permintaan, "
                            + "dan interval ini sudah terlalu kecil untuk dipecah dengan presisi detik."
            );
        }

        long half = Math.max(1L, seconds / 2L);
        LocalDateTime mid = start.plusSeconds(half);

        postProgress(
                stats,
                "Rentang > 8.000 entry, memecah interval..."
        );

        // Both halves include the boundary. appendBatch() removes repeated
        // entry IDs, so a feed at exactly the split point is not duplicated.
        fetchIntervalAdaptive(
                channel, key, start, mid, writer, stats, depth + 1
        );
        fetchIntervalAdaptive(
                channel, key, mid, end, writer, stats, depth + 1
        );
    }

    private CsvBatch fetchCsvBatch(
            String channel,
            String key,
            LocalDateTime start,
            LocalDateTime end
    ) throws Exception {
        String startEncoded = URLEncoder.encode(
                start.format(API_FMT), "UTF-8"
        );
        String endEncoded = URLEncoder.encode(
                end.format(API_FMT), "UTF-8"
        );

        StringBuilder url = new StringBuilder(
                "https://api.thingspeak.com/channels/"
        );
        url.append(URLEncoder.encode(channel, "UTF-8"))
                .append("/feeds.csv?start=")
                .append(startEncoded)
                .append("&end=")
                .append(endEncoded)
                .append("&results=")
                .append(MAX_RESULTS_PER_REQUEST)
                .append("&timezone=Asia%2FJakarta");

        if (!key.isEmpty()) {
            url.append("&api_key=")
                    .append(URLEncoder.encode(key, "UTF-8"));
        }

        IOException lastIo = null;

        for (int attempt = 1; attempt <= NETWORK_RETRIES; attempt++) {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(url.toString()).openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(CONNECT_TIMEOUT_MS);
                c.setReadTimeout(READ_TIMEOUT_MS);
                c.setUseCaches(false);
                c.setRequestProperty("Accept", "text/csv");

                int code = c.getResponseCode();

                if (code != HttpURLConnection.HTTP_OK) {
                    String detail = readErrorBody(c);
                    if ((code == 429 || code >= 500) && attempt < NETWORK_RETRIES) {
                        backoff(attempt);
                        continue;
                    }
                    throw new Exception(
                            "HTTP " + code +
                                    (detail.isEmpty() ? "" : " • " + detail)
                    );
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                c.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                )) {
                    String header = null;
                    List<String> lines = new ArrayList<>();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;

                        if (header == null) {
                            if ("-1".equals(line.trim())) {
                                throw new Exception(
                                        "ThingSpeak menolak akses channel. Periksa Channel ID dan Read API Key."
                                );
                            }
                            header = line;
                            continue;
                        }

                        // ThingSpeak returns "-1" when the channel is not
                        // accessible. Check it before treating the first row
                        // as valid data.
                        if ("-1".equals(line.trim())) {
                            throw new Exception(
                                    "ThingSpeak menolak akses channel. Periksa Channel ID dan Read API Key."
                            );
                        }

                        lines.add(line);
                        if (lines.size() > MAX_RESULTS_PER_REQUEST) {
                            // Defensive guard; ThingSpeak should never exceed it.
                            throw new Exception(
                                    "Respons ThingSpeak melebihi batas 8.000 entry."
                            );
                        }
                    }

                    return new CsvBatch(header, lines);
                }

            } catch (IOException io) {
                lastIo = io;
                if (attempt >= NETWORK_RETRIES) {
                    throw io;
                }
                backoff(attempt);
            } finally {
                if (c != null) c.disconnect();
            }
        }

        throw (lastIo != null)
                ? lastIo
                : new Exception("Gagal mengakses ThingSpeak.");
    }

    private void appendBatch(
            Writer writer,
            CsvBatch batch,
            ExportStats stats
    ) throws IOException {
        if (!stats.headerWritten && batch.header != null) {
            writer.write(batch.header);
            writer.write("\r\n");
            stats.headerWritten = true;
        }

        for (String line : batch.lines) {
            Long entryId = parseEntryId(line);

            // The adaptive splitter intentionally overlaps split boundaries.
            // Keep a bounded recent-ID cache so the same boundary entry is not
            // written twice, without assuming entry_id is globally monotonic
            // with created_at (back-filled ThingSpeak entries can violate that).
            if (entryId != null) {
                if (stats.recentEntryIds.contains(entryId)) {
                    continue;
                }
                stats.recentEntryIds.add(entryId);
                if (stats.recentEntryIds.size() > RECENT_ID_CACHE_LIMIT) {
                    Iterator<Long> it = stats.recentEntryIds.iterator();
                    if (it.hasNext()) {
                        it.next();
                        it.remove();
                    }
                }
            }

            writer.write(line);
            writer.write("\r\n");
            stats.rows++;
        }
    }

    private Long parseEntryId(String csvLine) {
        String col = extractCsvColumn(csvLine, 1); // created_at = 0, entry_id = 1
        if (col == null || col.isEmpty()) return null;
        try {
            return Long.parseLong(col.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Small CSV parser sufficient for extracting one column while respecting
     * double-quoted CSV fields.
     */
    private String extractCsvColumn(String line, int targetColumn) {
        boolean quoted = false;
        StringBuilder field = new StringBuilder();
        int column = 0;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }

            if (ch == ',' && !quoted) {
                if (column == targetColumn) return field.toString();
                column++;
                field.setLength(0);
                continue;
            }

            if (column == targetColumn) {
                field.append(ch);
            }
        }

        return column == targetColumn ? field.toString() : null;
    }

    private String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder b = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) b.append(line).append('\n');
        }
        return b.toString();
    }

    private String readErrorBody(HttpURLConnection c) {
        InputStream stream = null;
        try {
            stream = c.getErrorStream();
            if (stream == null) return "";

            StringBuilder b = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = r.readLine()) != null) {
                    b.append(line);
                    if (b.length() > 500) break;
                }
            }
            return b.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(400L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void postProgress(ExportStats stats, String message) {
        final long rows = stats.rows;
        final int requests = stats.requests;

        main.post(() -> statusView.setText(
                message + " • " + rows + " baris • " + requests + " permintaan"
        ));
    }

    private void openSaveDocument() {
        if (pendingCsvFile == null || !pendingCsvFile.isFile()) {
            readyToSave = false;
            downloadButton.setText("UNDUH DATA CSV");
            statusView.setText("File sementara sudah tidak tersedia. Silakan unduh ulang.");
            return;
        }

        startActivityForResult(
                new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("text/csv")
                        .putExtra(Intent.EXTRA_TITLE, pendingFileName),
                4101
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != 4101 || resultCode != RESULT_OK || data == null) {
            if (requestCode == 4101 && resultCode != RESULT_OK) {
                statusView.setText(
                        "Penyimpanan dibatalkan. Tekan SIMPAN FILE CSV untuk memilih lokasi lagi."
                );
            }
            return;
        }

        Uri uri = data.getData();
        if (uri == null || pendingCsvFile == null || !pendingCsvFile.isFile()) {
            statusView.setText("Gagal: lokasi atau file CSV tidak tersedia.");
            return;
        }

        File source = pendingCsvFile;

        try (InputStream in = new FileInputStream(source);
             OutputStream out = getContentResolver().openOutputStream(uri)) {

            if (out == null) {
                throw new Exception("Lokasi penyimpanan tidak tersedia.");
            }

            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            out.flush();

            statusView.setText(
                    "CSV berhasil disimpan: " + pendingFileName
            );
            Toast.makeText(
                    this,
                    "CSV berhasil disimpan.",
                    Toast.LENGTH_SHORT
            ).show();

            try { source.delete(); } catch (Exception ignored) {}
            pendingCsvFile = null;
            pendingFileName = null;
            readyToSave = false;
            downloadButton.setText("UNDUH DATA CSV");

        } catch (Exception ex) {
            statusView.setText(
                    "Gagal menyimpan CSV: " + safeMessage(ex)
            );
        }
    }

    private String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        return (msg == null || msg.trim().isEmpty())
                ? ex.getClass().getSimpleName()
                : msg;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        main.removeCallbacksAndMessages(null);
        net.shutdownNow();

        // A prepared file is only a temporary cache. It is deleted after the
        // user saves successfully or when the activity is destroyed.
        if (pendingCsvFile != null) {
            try { pendingCsvFile.delete(); } catch (Exception ignored) {}
            pendingCsvFile = null;
        }
    }

    private static class OpenMeteoStats {
        long rows = 0;
    }

    private static class CsvBatch {
        final String header;
        final List<String> lines;

        CsvBatch(String header, List<String> lines) {
            this.header = header;
            this.lines = lines;
        }
    }

    private static class ExportStats {
        long rows = 0;
        int requests = 0;
        boolean headerWritten = false;
        final LinkedHashSet<Long> recentEntryIds = new LinkedHashSet<>();
    }
}
