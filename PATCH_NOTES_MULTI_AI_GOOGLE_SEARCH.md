# STASIUN CUACA v1.5.7: multi-provider AI + Google Search + USB crash fix

- Added selectable AI providers: OpenAI, Google Gemini, OpenRouter.
- Each provider has its own API key and model preference. Keys are entered locally in app settings and are not hard-coded.
- Added Google Search/web-grounding option: OpenAI Responses web_search, Gemini google_search grounding, or OpenRouter :online model routing.
- OpenAlex literature search remains enabled and is supplied as research context.
- Fixed FieldNotesActivity invalid SOIL_SOURCE[3] reference where present (valid USB source is index 1, CWT-7in1 USB).
- Existing weather, ThingSpeak, field notes, sensor, CSV/PDF and agronomy features retained.

Build/test note: a complete Android build and live API/device tests must be verified by GitHub Actions; provider availability, API quotas, and model names depend on each provider account.
