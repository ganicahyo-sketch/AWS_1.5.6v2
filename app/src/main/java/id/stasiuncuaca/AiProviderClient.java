package id.stasiuncuaca;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Provider-neutral AI HTTP client.
 * API keys are always supplied at runtime from local preferences; no key is embedded here.
 */
public final class AiProviderClient {
    public static final String OPENAI = "OpenAI";
    public static final String GEMINI = "Google Gemini";
    public static final String OPENROUTER = "OpenRouter Free";

    private AiProviderClient() {}

    public static String generate(String provider, String apiKey, String model,
                                  String instructions, String input) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("API key " + provider + " belum diisi di Pengaturan.");
        }
        if (OPENAI.equals(provider)) return callOpenAi(apiKey.trim(), model, instructions, input);
        if (GEMINI.equals(provider)) return callGemini(apiKey.trim(), model, instructions, input);
        if (OPENROUTER.equals(provider)) return callOpenRouter(apiKey.trim(), model, instructions, input);
        throw new Exception("Provider AI tidak dikenal: " + provider);
    }

    private static String callOpenAi(String key, String model, String instructions, String input) throws Exception {
        JSONObject p = new JSONObject();
        p.put("model", model);
        p.put("store", false);
        p.put("max_output_tokens", 5000);
        JSONArray tools = new JSONArray();
        JSONObject webSearch = new JSONObject();
        webSearch.put("type", "web_search");
        tools.put(webSearch);
        p.put("tools", tools);
        p.put("tool_choice", "required");
        p.put("instructions", instructions);
        p.put("input", input);

        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL("https://api.openai.com/v1/responses").openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(12000);
            c.setReadTimeout(60000);
            c.setDoOutput(true);
            c.setRequestProperty("Authorization", "Bearer " + key);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] body = p.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(body.length);
            try (OutputStream o = c.getOutputStream()) { o.write(body); }
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                String er = readAll(c.getErrorStream());
                throw new Exception("OpenAI HTTP " + code + (er.isEmpty() ? "" : " — " + trimError(er)));
            }
            JSONObject r = new JSONObject(readAll(c.getInputStream()));
            String t = r.optString("output_text", "").trim();
            if (!t.isEmpty()) return t;
            JSONArray out = r.optJSONArray("output");
            if (out != null) {
                StringBuilder z = new StringBuilder();
                for (int i = 0; i < out.length(); i++) {
                    JSONObject it = out.optJSONObject(i);
                    if (it == null) continue;
                    JSONArray cc = it.optJSONArray("content");
                    if (cc == null) continue;
                    for (int j = 0; j < cc.length(); j++) {
                        JSONObject part = cc.optJSONObject(j);
                        if (part != null && "output_text".equals(part.optString("type"))) {
                            z.append(part.optString("text", "")).append("\n");
                        }
                    }
                }
                if (z.length() > 0) return z.toString().trim();
            }
            throw new Exception("Respons OpenAI tidak berisi teks.");
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String callGemini(String key, String model, String instructions, String input) throws Exception {
        try {
            return callGeminiRequest(key, model, instructions, input, true);
        } catch (Exception first) {
            String message = first.getMessage() == null ? "" : first.getMessage().toLowerCase();
            if (message.contains("google_search") || message.contains("grounding") || message.contains("tool")
                    || message.contains("not available") || message.contains("unsupported")) {
                return callGeminiRequest(key, model, instructions, input, false);
            }
            throw first;
        }
    }

    private static String callGeminiRequest(String key, String model, String instructions, String input,
                                            boolean useGoogleSearch) throws Exception {
        String safeModel = model == null || model.trim().isEmpty() ? "gemini-3.8-flash" : model.trim();
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + URLEncoder.encode(safeModel, "UTF-8") + ":generateContent";

        JSONObject root = new JSONObject();
        JSONObject system = new JSONObject();
        JSONArray systemParts = new JSONArray();
        systemParts.put(new JSONObject().put("text", instructions));
        system.put("parts", systemParts);
        root.put("system_instruction", system);

        JSONArray contents = new JSONArray();
        JSONObject user = new JSONObject();
        user.put("role", "user");
        JSONArray userParts = new JSONArray();
        userParts.put(new JSONObject().put("text", input));
        user.put("parts", userParts);
        contents.put(user);
        root.put("contents", contents);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("maxOutputTokens", 5000);
        generationConfig.put("temperature", 0.2);
        root.put("generationConfig", generationConfig);

        if (useGoogleSearch) {
            JSONArray tools = new JSONArray();
            tools.put(new JSONObject().put("google_search", new JSONObject()));
            root.put("tools", tools);
        }

        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(endpoint).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(12000);
            c.setReadTimeout(60000);
            c.setDoOutput(true);
            c.setRequestProperty("x-goog-api-key", key);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] body = root.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(body.length);
            try (OutputStream o = c.getOutputStream()) { o.write(body); }
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                String er = readAll(c.getErrorStream());
                throw new Exception("Gemini HTTP " + code + (er.isEmpty() ? "" : " — " + trimError(er)));
            }
            JSONObject r = new JSONObject(readAll(c.getInputStream()));
            JSONArray candidates = r.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject candidate = candidates.optJSONObject(0);
                JSONObject content = candidate == null ? null : candidate.optJSONObject("content");
                JSONArray parts = content == null ? null : content.optJSONArray("parts");
                if (parts != null) {
                    StringBuilder out = new StringBuilder();
                    for (int i = 0; i < parts.length(); i++) {
                        JSONObject part = parts.optJSONObject(i);
                        if (part != null) out.append(part.optString("text", ""));
                    }
                    String text = out.toString().trim();
                    if (!text.isEmpty()) return text;
                }
            }
            throw new Exception("Respons Gemini tidak berisi teks.");
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String callOpenRouter(String key, String model, String instructions, String input) throws Exception {
        String safeModel = model == null || model.trim().isEmpty() ? "openrouter/free" : model.trim();
        JSONObject root = new JSONObject();
        root.put("model", safeModel);
        root.put("max_tokens", 5000);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", instructions));
        messages.put(new JSONObject().put("role", "user").put("content", input));
        root.put("messages", messages);

        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(12000);
            c.setReadTimeout(60000);
            c.setDoOutput(true);
            c.setRequestProperty("Authorization", "Bearer " + key);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            c.setRequestProperty("X-Title", "STASIUN CUACA v1.5.6");
            byte[] body = root.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(body.length);
            try (OutputStream o = c.getOutputStream()) { o.write(body); }
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                String er = readAll(c.getErrorStream());
                throw new Exception("OpenRouter HTTP " + code + (er.isEmpty() ? "" : " — " + trimError(er)));
            }
            JSONObject r = new JSONObject(readAll(c.getInputStream()));
            JSONArray choices = r.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject choice = choices.optJSONObject(0);
                JSONObject message = choice == null ? null : choice.optJSONObject("message");
                if (message != null) {
                    String text = message.optString("content", "").trim();
                    if (!text.isEmpty()) return text;
                    JSONArray contentParts = message.optJSONArray("content");
                    if (contentParts != null) {
                        StringBuilder combined = new StringBuilder();
                        for (int i = 0; i < contentParts.length(); i++) {
                            JSONObject part = contentParts.optJSONObject(i);
                            if (part != null) combined.append(part.optString("text", ""));
                        }
                        text = combined.toString().trim();
                        if (!text.isEmpty()) return text;
                    }
                }
            }
            throw new Exception("Respons OpenRouter tidak berisi teks.");
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String trimError(String error) {
        String t = error == null ? "" : error.trim();
        return t.substring(0, Math.min(500, t.length()));
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder b = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) b.append(line).append('\n');
        }
        return b.toString().trim();
    }
}
