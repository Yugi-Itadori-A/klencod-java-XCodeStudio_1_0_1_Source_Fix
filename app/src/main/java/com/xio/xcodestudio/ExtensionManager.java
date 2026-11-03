package com.xio.xcodestudio;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Metadata-only extension registry. It deliberately does not execute imported code. */
public final class ExtensionManager {
    public static final class Extension {
        public final String id, name, version, description;
        public final boolean enabled;
        Extension(String id, String name, String version, String description, boolean enabled) {
            this.id=id; this.name=name; this.version=version; this.description=description; this.enabled=enabled;
        }
    }
    private final File root;
    private final SharedPreferences prefs;
    public ExtensionManager(Context context) {
        root = new File(context.getFilesDir(), "extensions"); if (!root.exists()) root.mkdirs();
        prefs = context.getSharedPreferences("xio_extensions", Context.MODE_PRIVATE);
    }
    public List<Extension> list() {
        List<Extension> result = new ArrayList<>(); File[] dirs = root.listFiles(File::isDirectory); if (dirs == null) return result;
        for (File dir : dirs) {
            File manifest = new File(dir, "extension.json"); if (!manifest.isFile()) continue;
            try (InputStream in = new FileInputStream(manifest)) {
                byte[] b = new byte[(int)Math.min(manifest.length(), 65536)]; int n = in.read(b);
                JSONObject o = new JSONObject(new String(b, 0, Math.max(0,n), StandardCharsets.UTF_8));
                String id = o.optString("id", dir.getName());
                result.add(new Extension(id, o.optString("name", id), o.optString("version", "0.0.0"),
                    o.optString("description", "No description"), prefs.getBoolean(id, false)));
            } catch (Exception ignored) {}
        }
        return result;
    }
    public void setEnabled(String id, boolean enabled) { prefs.edit().putBoolean(id, enabled).apply(); }
    public File getDirectory() { return root; }
}
