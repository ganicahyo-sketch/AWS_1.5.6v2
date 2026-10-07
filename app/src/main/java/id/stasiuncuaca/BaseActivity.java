package id.stasiuncuaca;

import android.app.Activity;
import android.content.Context;

/** Common base used only to apply the selected application appearance. */
public abstract class BaseActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeManager.wrap(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeManager.recreateIfNeeded(this);
    }
}
