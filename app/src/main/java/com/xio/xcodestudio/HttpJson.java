package com.xio.xcodestudio;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpJson {
    public static final class Response {
        public final int status;
        public final String body;
        public final Map<String, java.util.List<String>> headers;
        Response(int status, String body, Map<String, java.util.List<String>> headers) { this.status = status; this.body = body; this.headers = headers; }
        public JSONObject json() throws IOException {
            try { return new JSONObject(body == null || body.isEmpty() ? "{}" : body); }
            catch (Exception ex) { throw new IOException("Server returned invalid JSON: " + trim(body), ex); }
        }
    }
    private HttpJson() {}

    public static Response request(String method, String urlString, Map<String, String> headers, JSONObject payload) throws IOException {
        byte[] bytes = payload == null ? null : payload.toString().getBytes(StandardCharsets.UTF_8);
        URL url = new URL(urlString);
        if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IOException("HTTPS endpoints are required for requests carrying credentials");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(45000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "XCode-Studio-Android/1.0");
        if (headers != null) for (Map.Entry<String, String> e : headers.entrySet()) connection.setRequestProperty(e.getKey(), e.getValue());
        if (bytes != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = stream == null ? "" : readText(stream);
        Response result = new Response(status, body, connection.getHeaderFields());
        connection.disconnect();
        return result;
    }

    public static String readText(InputStream stream) throws IOException {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }
    public static String trim(String s) { if (s == null) return ""; String t = s.replaceAll("\\s+", " ").trim(); return t.length() > 500 ? t.substring(0, 500) + "…" : t; }
    public static void requireSuccess(Response response) throws IOException {
        if (response.status < 200 || response.status >= 300) {
            String detail = response.body;
            try { JSONObject o = new JSONObject(response.body); detail = o.optString("message", response.body); } catch (Exception ignored) {}
            throw new IOException("HTTP " + response.status + ": " + trim(detail));
        }
    }
}
