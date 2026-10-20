# FIX19 — Java compiler scope fix

## Root cause
`FertilizerConverterDialog.show(Activity, SharedPreferences, Runnable)` names its `setOnShowListener` lambda parameter `ignored`. A nested `catch (Exception ignored)` reused the same identifier inside that lambda scope, which Java rejects.

## Change
Renamed only the nested JSON serialization catch variable to `jsonError` and added a comment. Fertilizer dose calculation, result persistence, agronomy summary, dolomite result, PDF report, and application version remain unchanged.

## Verification
Static checks and ZIP integrity are performed for this package. Run `./gradlew --no-daemon :app:assembleDebug --stacktrace --warning-mode all` in GitHub Actions to verify full Android compilation.
