package com.xio.xcodestudio;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Direct HTTPS clients for the selected model provider. Code is never sent unless the UI obtains consent. */
public final class AIClient {
    private AIClient() {}

    public static String ask(String provider, String model, String endpoint, String apiKey, String prompt, String sourceCode) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) throw new IOException("Save an API key first");
        String userText = prompt == null ? "" : prompt.trim();
        if (userText.isEmpty()) throw new IOException("Enter a question or instruction");
        String content = userText;
        if (sourceCode != null && !sourceCode.trim().isEmpty()) content += "\n\n--- Selected source code ---\n" + sourceCode;
        String p = provider == null ? "OpenAI" : provider;
        String m = model == null || model.trim().isEmpty() ? defaultModel(p) : model.trim();
        if (p.equalsIgnoreCase("Gemini")) return askGemini(m, apiKey, content);
        if (p.equalsIgnoreCase("Claude")) return askClaude(m, apiKey, content);
        String url = endpoint == null || endpoint.trim().isEmpty() ? defaultEndpoint(p) : endpoint.trim();
        return askOpenAiCompatible(url, m, apiKey, content);
    }

    private static String askOpenAiCompatible(String url, String model, String key, String prompt) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            JSONArray messages = new JSONArray();
            JSONObject system = new JSONObject(); system.put("role", "system"); system.put("content", "You are a careful programming assistant. Explain uncertainty and do not claim to run code unless you actually did.");
            JSONObject user = new JSONObject(); user.put("role", "user"); user.put("content", prompt);
            messages.put(system); messages.put(user); body.put("messages", messages);
            body.put("temperature", 0.2);
        } catch (Exception ex) { throw new IOException(ex); }
        Map<String, String> headers = new HashMap<>(); headers.put("Authorization", "Bearer " + key);
        HttpJson.Response response = HttpJson.request("POST", url, headers, body);
        HttpJson.requireSuccess(response);
        try {
            JSONObject root = response.json();
            return root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "");
        } catch (Exception ex) { throw new IOException("The provider response did not contain a chat answer: " + HttpJson.trim(response.body), ex); }
    }

    private static String askGemini(String model, String key, String prompt) throws IOException {
        JSONObject body = new JSONObject();
        try {
            JSONArray parts = new JSONArray(); JSONObject part = new JSONObject(); part.put("text", prompt); parts.put(part);
            JSONArray contents = new JSONArray(); JSONObject content = new JSONObject(); content.put("parts", parts); contents.put(content); body.put("contents", contents);
            JSONObject config = new JSONObject(); config.put("temperature", 0.2); body.put("generationConfig", config);
        } catch (Exception ex) { throw new IOException(ex); }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model.replace(" ", "") + ":generateContent?key=" + java.net.URLEncoder.encode(key, "UTF-8");
        HttpJson.Response response = HttpJson.request("POST", url, null, body);
        HttpJson.requireSuccess(response);
        try { return response.json().getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text", ""); }
        catch (Exception ex) { throw new IOException("Gemini returned no text answer: " + HttpJson.trim(response.body), ex); }
    }

    private static String askClaude(String model, String key, String prompt) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("model", model); body.put("max_tokens", 4096);
            JSONArray messages = new JSONArray(); JSONObject user = new JSONObject(); user.put("role", "user"); user.put("content", prompt); messages.put(user); body.put("messages", messages);
        } catch (Exception ex) { throw new IOException(ex); }
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", key); headers.put("anthropic-version", "2023-06-01");
        HttpJson.Response response = HttpJson.request("POST", "https://api.anthropic.com/v1/messages", headers, body);
        HttpJson.requireSuccess(response);
        try { return response.json().getJSONArray("content").getJSONObject(0).optString("text", ""); }
        catch (Exception ex) { throw new IOException("Claude returned no text answer: " + HttpJson.trim(response.body), ex); }
    }

    private static String defaultEndpoint(String provider) {
        if ("OpenRouter".equalsIgnoreCase(provider)) return "https://openrouter.ai/api/v1/chat/completions";
        if ("Groq".equalsIgnoreCase(provider)) return "https://api.groq.com/openai/v1/chat/completions";
        if ("Mistral".equalsIgnoreCase(provider)) return "https://api.mistral.ai/v1/chat/completions";
        if ("DeepSeek".equalsIgnoreCase(provider)) return "https://api.deepseek.com/chat/completions";
        return "https://api.openai.com/v1/chat/completions";
    }
    private static String defaultModel(String provider) {
        if ("Gemini".equalsIgnoreCase(provider)) return "gemini-2.5-flash";
        if ("Claude".equalsIgnoreCase(provider)) return "claude-sonnet-4-5";
        if ("Groq".equalsIgnoreCase(provider)) return "llama-3.3-70b-versatile";
        if ("Mistral".equalsIgnoreCase(provider)) return "mistral-small-latest";
        if ("DeepSeek".equalsIgnoreCase(provider)) return "deepseek-chat";
        if ("OpenRouter".equalsIgnoreCase(provider)) return "openai/gpt-4o-mini";
        return "gpt-4o-mini";
    }
}
