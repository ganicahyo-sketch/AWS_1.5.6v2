package id.turus.stasiuncuaca;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

/** App appearance selector; it does not alter weather, ThingSpeak or agronomy logic. */
public final class ThemeManager {
    private static final String PREFS = "thingspeak_config";
    private static final String PREF_THEME_MODE = "theme_mode";
    private static final String MODE_SYSTEM = "system";
    private static final String MODE_LIGHT = "light";
    private static final String MODE_DARK = "dark";

    private ThemeManager() {}

    public static Context wrap(Context base) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        int desired = desiredNightMode(base, config);
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | desired;
        return base.createConfigurationContext(config);
    }

    public static void recreateIfNeeded(Activity activity) {
        Configuration config = activity.getResources().getConfiguration();
        int current = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int desired = desiredNightMode(activity, config);
        if (current != desired) activity.recreate();
    }

    public static String getMode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(PREF_THEME_MODE, MODE_SYSTEM);
    }

    public static int getSelectionIndex(Context context) {
        String mode = getMode(context);
        if (MODE_LIGHT.equals(mode)) return 1;
        if (MODE_DARK.equals(mode)) return 2;
        return 0;
    }

    public static void saveMode(SharedPreferences.Editor editor, int index) {
        String mode = MODE_SYSTEM;
        if (index == 1) mode = MODE_LIGHT;
        else if (index == 2) mode = MODE_DARK;
        editor.putString(PREF_THEME_MODE, mode);
    }

    private static int desiredNightMode(Context context, Configuration config) {
        String mode = getMode(context);
        if (MODE_DARK.equals(mode)) return Configuration.UI_MODE_NIGHT_YES;
        if (MODE_LIGHT.equals(mode)) return Configuration.UI_MODE_NIGHT_NO;
        int system = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return system == Configuration.UI_MODE_NIGHT_YES
                ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
    }
}
