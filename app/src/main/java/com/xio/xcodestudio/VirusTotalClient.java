package com.xio.xcodestudio;

import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/** Optional VirusTotal v3 file analysis. Uploading a file transmits it to a third party. */
public final class VirusTotalClient {
    private static final long MAX_FILE_BYTES = 32L * 1024L * 1024L;
    private VirusTotalClient() {}

    public static JSONObject upload(File file, String apiKey) throws IOException {
        if (file == null || !file.isFile()) throw new IOException("Choose a file to scan");
        if (file.length() > MAX_FILE_BYTES) throw new IOException("This client supports files up to 32 MB for direct upload");
        String boundary = "----XIO" + Long.toHexString(System.nanoTime());
        HttpURLConnection c = (HttpURLConnection) new URL("https://www.virustotal.com/api/v3/files").openConnection();
        c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(20000); c.setReadTimeout(90000);
        c.setRequestProperty("x-apikey", apiKey); c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try (OutputStream out = c.getOutputStream(); InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + file.getName().replace("\"", "_") + "\"\r\nContent-Type: application/octet-stream\r\n\r\n");
            byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) out.write(b, 0, n);
            write(out, "\r\n--" + boundary + "--\r\n");
        }
        int status = c.getResponseCode(); InputStream stream = status >= 400 ? c.getErrorStream() : c.getInputStream();
        String body = stream == null ? "" : HttpJson.readText(stream); c.disconnect();
        if (status < 200 || status >= 300) throw new IOException("VirusTotal HTTP " + status + ": " + HttpJson.trim(errorMessage(body)));
        try { return new JSONObject(body); } catch (Exception ex) { throw new IOException("Invalid scan response", ex); }
    }

    public static JSONObject analysis(String analysisId, String apiKey) throws IOException {
        Map<String, String> h = new HashMap<>(); h.put("x-apikey", apiKey);
        HttpJson.Response r = HttpJson.request("GET", "https://www.virustotal.com/api/v3/analyses/" + analysisId, h, null);
        HttpJson.requireSuccess(r); return r.json();
    }

    public static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) digest.update(b, 0, n); }
        StringBuilder out = new StringBuilder(); for (byte b : digest.digest()) out.append(String.format(java.util.Locale.ROOT, "%02x", b)); return out.toString();
    }
    private static String errorMessage(String body) { try { return new JSONObject(body).optJSONObject("error").optString("message", body); } catch (Exception ex) { return body; } }
    private static void write(OutputStream out, String value) throws IOException { out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
}
