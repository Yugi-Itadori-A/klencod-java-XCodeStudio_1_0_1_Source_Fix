package com.xio.xcodestudio;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ProjectManager {
    private final File workspace;
    public ProjectManager(Context context) {
        workspace = new File(context.getFilesDir(), "workspace");
        if (!workspace.exists() && !workspace.mkdirs()) throw new IllegalStateException("Cannot create workspace");
    }
    public File getWorkspace() { return workspace; }

    public synchronized File createProject(String requestedName, String template) throws IOException {
        String name = sanitizeName(requestedName);
        File target = new File(workspace, name);
        if (target.exists()) throw new IOException("A project with this name already exists");
        if (!target.mkdirs()) throw new IOException("Could not create project directory");
        try { applyTemplate(target, template == null ? "empty" : template); }
        catch (IOException ex) { deleteRecursively(target); throw ex; }
        writeText(new File(target, ".xio-project.json"), "{\n  \"format\": 1,\n  \"name\": \"" + jsonEscape(name) + "\",\n  \"template\": \"" + jsonEscape(template) + "\"\n}\n");
        return target;
    }

    public synchronized File createImportedProject(String requestedName) throws IOException {
        String name = sanitizeName(requestedName);
        File target = new File(workspace, name);
        if (target.exists()) throw new IOException("A project with this name already exists");
        if (!target.mkdirs()) throw new IOException("Could not create project directory");
        writeText(new File(target, ".xio-project.json"), "{\n  \"format\": 1,\n  \"name\": \"" + jsonEscape(name) + "\",\n  \"imported\": true\n}\n");
        return target;
    }

    public File[] listProjects() {
        File[] files = workspace.listFiles(File::isDirectory);
        if (files == null) return new File[0];
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files;
    }

    public void renameProject(File project, String newName) throws IOException {
        ensureProject(project);
        String safeName = sanitizeName(newName);
        if (project.getName().equals(safeName)) return;
        File target = new File(workspace, safeName);
        if (target.exists()) throw new IOException("A project with this name already exists");
        if (!project.renameTo(target)) throw new IOException("Rename failed");
    }

    public void deleteProject(File project) throws IOException { ensureProject(project); deleteRecursively(project); }

    public List<String> listFiles(File project) throws IOException {
        ensureProject(project);
        ArrayList<String> result = new ArrayList<>();
        collect(project, project, result);
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private void collect(File base, File current, List<String> out) throws IOException {
        File[] children = current.listFiles();
        if (children == null) return;
        Arrays.sort(children, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File child : children) {
            if (child.getName().equals(".git") || child.getName().equals("build") || child.getName().equals(".gradle")) continue;
            if (child.isDirectory()) collect(base, child, out);
            else if (child.isFile()) out.add(relativePath(base, child));
        }
    }

    public File resolveFile(File project, String relative) throws IOException {
        ensureProject(project);
        if (relative == null || relative.trim().isEmpty()) throw new IOException("Empty file path");
        File file = new File(project, relative);
        String base = project.getCanonicalPath() + File.separator;
        String path = file.getCanonicalPath();
        if (!path.startsWith(base)) throw new IOException("Path escapes project directory");
        return file;
    }

    public String readText(File project, String relative) throws IOException {
        File f = resolveFile(project, relative);
        if (!f.isFile()) throw new FileNotFoundException(relative);
        if (f.length() > 3_000_000) throw new IOException("File is larger than the 3 MB editor limit");
        return new String(readAll(new FileInputStream(f)), StandardCharsets.UTF_8);
    }

    public void writeText(File project, String relative, String text) throws IOException {
        File f = resolveFile(project, relative);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create parent folder");
        try (OutputStream out = new FileOutputStream(f)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
    }

    public void createFile(File project, String relative) throws IOException {
        File f = resolveFile(project, relative);
        File p = f.getParentFile();
        if (p != null && !p.exists() && !p.mkdirs()) throw new IOException("Could not create folder");
        if (!f.createNewFile()) throw new IOException("File already exists");
    }

    public void deleteFile(File project, String relative) throws IOException {
        File f = resolveFile(project, relative);
        if (!f.exists()) throw new FileNotFoundException(relative);
        deleteRecursively(f);
    }

    public static String relativePath(File base, File file) throws IOException {
        String b = base.getCanonicalPath();
        String f = file.getCanonicalPath();
        if (!f.startsWith(b + File.separator)) throw new IOException("File outside project");
        return f.substring(b.length() + 1).replace(File.separatorChar, '/');
    }

    public static String sanitizeName(String value) throws IOException {
        String name = value == null ? "" : value.trim().replaceAll("[^\\p{L}\\p{N}._ -]", "_").replace(' ', '-');
        name = name.replaceAll("\\.{2,}", ".").replaceAll("^[. -]+|[. -]+$", "");
        if (name.isEmpty() || name.length() > 64) throw new IOException("Project name must contain 1–64 safe characters");
        return name;
    }

    public List<String> diagnose(File project) throws IOException {
        ensureProject(project);
        List<String> issues = new ArrayList<>();
        List<String> files = listFiles(project);
        if (files.isEmpty()) issues.add("Project contains no files yet.");
        if (new File(project, "app/src/main/AndroidManifest.xml").exists() && !new File(project, "app/build.gradle").exists())
            issues.add("Android manifest found, but app/build.gradle is missing.");
        if (new File(project, "settings.gradle").exists() && !new File(project, "app/src/main/AndroidManifest.xml").exists())
            issues.add("Gradle project found. Verify that it has an Android application module if you expect an APK.");
        if (files.size() > 5000) issues.add("Large project: more than 5,000 files may take longer to upload.");
        if (issues.isEmpty()) issues.add("No basic workspace issues detected. This is not a compiler or security audit.");
        return issues;
    }

    private void ensureProject(File project) throws IOException {
        if (project == null) throw new IOException("No project selected");
        String base = workspace.getCanonicalPath() + File.separator;
        if (!project.getCanonicalPath().startsWith(base) || !project.isDirectory()) throw new IOException("Invalid project directory");
    }

    private void applyTemplate(File dir, String template) throws IOException {
        switch (template.toLowerCase(Locale.ROOT)) {
            case "android-java":
                put(dir, "settings.gradle", "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\nrootProject.name = 'StarterAndroid'\ninclude ':app'\n");
                put(dir, "build.gradle", "plugins { id 'com.android.application' version '8.9.2' apply false }\n");
                put(dir, "gradle.properties", "org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\n");
                put(dir, "app/build.gradle", "plugins { id 'com.android.application' }\nandroid { namespace 'com.example.starter'; compileSdk 35; defaultConfig { applicationId 'com.example.starter'; minSdk 26; targetSdk 35; versionCode 1; versionName '1.0' }; compileOptions { sourceCompatibility JavaVersion.VERSION_17; targetCompatibility JavaVersion.VERSION_17 } }\n");
                put(dir, "app/src/main/AndroidManifest.xml", "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"><application android:theme=\"@android:style/Theme.Material.Light.NoActionBar\" android:label=\"Starter\"><activity android:name=\".MainActivity\" android:exported=\"true\"><intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter></activity></application></manifest>\n");
                put(dir, "app/src/main/java/com/example/starter/MainActivity.java", "package com.example.starter;\nimport android.app.Activity;\nimport android.os.Bundle;\nimport android.widget.TextView;\npublic class MainActivity extends Activity { @Override protected void onCreate(Bundle state) { super.onCreate(state); TextView view = new TextView(this); view.setText(\"Hello from XCode Studio\"); view.setTextSize(24); view.setPadding(32,32,32,32); setContentView(view); } }\n");
                put(dir, ".github/workflows/android.yml", androidWorkflow());
                break;
            case "web":
                put(dir, "index.html", "<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Starter</title><link rel=\"stylesheet\" href=\"styles.css\"></head><body><main><h1>Hello, world</h1><p>Built with XCode Studio.</p><button id=\"hello\">Test</button></main><script src=\"script.js\"></script></body></html>\n");
                put(dir, "styles.css", "body{font-family:system-ui,sans-serif;margin:0;padding:3rem;background:#111;color:#f4f4f4}main{max-width:48rem;margin:auto}button{padding:.7rem 1rem;border:0;border-radius:.6rem}\n");
                put(dir, "script.js", "document.querySelector('#hello').addEventListener('click',()=>alert('Hello from XCode Studio'));\n");
                break;
            case "python":
                put(dir, "main.py", "def main():\n    print('Hello from XCode Studio')\n\nif __name__ == '__main__':\n    main()\n");
                put(dir, "README.md", "# Python project\n\nOpen `main.py` and edit your code.\n");
                break;
            case "cpp":
                put(dir, "main.cpp", "#include <iostream>\nint main() { std::cout << \"Hello from XCode Studio\\n\"; return 0; }\n");
                put(dir, "CMakeLists.txt", "cmake_minimum_required(VERSION 3.22)\nproject(StarterCpp LANGUAGES CXX)\nset(CMAKE_CXX_STANDARD 17)\nadd_executable(starter main.cpp)\n");
                break;
            case "flutter":
                put(dir, "pubspec.yaml", "name: starter_flutter\ndescription: Starter project created with XCode Studio\npublish_to: none\nversion: 1.0.0+1\nenvironment:\n  sdk: '>=3.3.0 <4.0.0'\ndependencies:\n  flutter:\n    sdk: flutter\ndev_dependencies:\n  flutter_test:\n    sdk: flutter\nflutter:\n  uses-material-design: true\n");
                put(dir, "lib/main.dart", "import 'package:flutter/material.dart';\nvoid main() => runApp(const StarterApp());\nclass StarterApp extends StatelessWidget { const StarterApp({super.key}); @override Widget build(BuildContext context) => const MaterialApp(home: Scaffold(body: Center(child: Text('Hello from XCode Studio')))); }\n");
                break;
            default:
                put(dir, "README.md", "# " + dir.getName() + "\n\nProject created with XCode Studio. Add your source files here.\n");
        }
    }

    private static String androidWorkflow() {
        return "name: Android build\non:\n  workflow_dispatch:\n  push:\n    paths: ['app/**', '*.gradle', 'settings.gradle']\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with:\n          distribution: temurin\n          java-version: '17'\n      - uses: android-actions/setup-android@v3\n      - uses: gradle/actions/setup-gradle@v4\n        with:\n          gradle-version: '8.11.1'\n      - run: sdkmanager 'platforms;android-35' 'build-tools;35.0.0'\n      - run: gradle --no-daemon :app:assembleDebug\n      - uses: actions/upload-artifact@v4\n        with:\n          name: debug-apk\n          path: app/build/outputs/apk/debug/*.apk\n          if-no-files-found: error\n";
    }

    private void put(File base, String path, String text) throws IOException { writeText(new File(base, path), text); }
    private static void writeText(File f, String text) throws IOException {
        File p = f.getParentFile(); if (p != null && !p.exists() && !p.mkdirs()) throw new IOException("Cannot create " + p);
        try (OutputStream out = new FileOutputStream(f)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
    }
    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n); return out.toByteArray();
        }
    }
    public static void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) { File[] children = file.listFiles(); if (children != null) for (File child : children) deleteRecursively(child); }
        if (file.exists() && !file.delete()) throw new IOException("Could not delete " + file.getName());
    }
    private static String jsonEscape(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
