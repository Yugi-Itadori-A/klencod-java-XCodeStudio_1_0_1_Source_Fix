package com.xio.xcodestudio;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** GitHub REST integration. The caller must run these methods off the UI thread. */
public final class GitHubClient {
    private static final String API = "https://api.github.com";
    private final String token;
    public GitHubClient(String token) { this.token = token == null ? "" : token.trim(); }
    private Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("Authorization", "Bearer " + token);
        h.put("X-GitHub-Api-Version", "2022-11-28");
        h.put("Accept", "application/vnd.github+json");
        return h;
    }
    private JSONObject call(String method, String path, JSONObject body) throws IOException {
        HttpJson.Response r = HttpJson.request(method, API + path, headers(), body);
        HttpJson.requireSuccess(r);
        return r.json();
    }
    public JSONObject user() throws IOException { return call("GET", "/user", null); }
    public JSONArray repositories() throws IOException {
        HttpJson.Response r = HttpJson.request("GET", API + "/user/repos?per_page=100&sort=updated", headers(), null);
        HttpJson.requireSuccess(r);
        try { return new JSONArray(r.body); }
        catch (Exception ex) { throw new IOException("GitHub returned an invalid repository list", ex); }
    }

    public JSONObject createRepository(String name, String description, boolean isPrivate) throws IOException {
        JSONObject body = new JSONObject();
        try { body.put("name", name); body.put("description", description == null ? "" : description); body.put("private", isPrivate); body.put("auto_init", true); }
        catch (Exception ex) { throw new IOException(ex); }
        return call("POST", "/user/repos", body);
    }

    public void pushProject(File project, String owner, String repo, String branch, String message) throws IOException {
        if (project == null || !project.isDirectory()) throw new IOException("Project directory is unavailable");
        String targetBranch = branch == null || branch.trim().isEmpty() ? "main" : branch.trim();
        JSONArray tree = new JSONArray();
        List<File> files = new ArrayList<>(); collectFiles(project, project, files);
        if (files.isEmpty()) throw new IOException("Project has no files to upload");
        long totalBytes = 0;
        for (File file : files) {
            totalBytes += file.length();
            if (totalBytes > 20L * 1024L * 1024L) throw new IOException("Upload is limited to 20 MB per operation in this mobile client");
        }
        for (File file : files) {
            String relative = ProjectManager.relativePath(project, file);
            byte[] data = readLimited(file, 10L * 1024L * 1024L);
            JSONObject blob = new JSONObject();
            try { blob.put("content", android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)); blob.put("encoding", "base64"); }
            catch (Exception ex) { throw new IOException(ex); }
            JSONObject created = call("POST", "/repos/" + enc(owner) + "/" + enc(repo) + "/git/blobs", blob);
            JSONObject item = new JSONObject();
            try { item.put("path", relative); item.put("mode", "100644"); item.put("type", "blob"); item.put("sha", created.getString("sha")); tree.put(item); }
            catch (Exception ex) { throw new IOException(ex); }
        }
        String baseTree = null;
        String parentCommit = null;
        try {
            JSONObject ref = call("GET", "/repos/" + enc(owner) + "/" + enc(repo) + "/git/ref/heads/" + enc(targetBranch), null);
            parentCommit = ref.getJSONObject("object").getString("sha");
            JSONObject commit = call("GET", "/repos/" + enc(owner) + "/" + enc(repo) + "/git/commits/" + parentCommit, null);
            baseTree = commit.getJSONObject("tree").getString("sha");
        } catch (IOException missingBranch) {
            // Empty repositories have no branch yet; create a root commit below.
        }
        JSONObject treeBody = new JSONObject();
        try { treeBody.put("tree", tree); if (baseTree != null) treeBody.put("base_tree", baseTree); }
        catch (Exception ex) { throw new IOException(ex); }
        JSONObject treeResult = call("POST", "/repos/" + enc(owner) + "/" + enc(repo) + "/git/trees", treeBody);
        JSONObject commitBody = new JSONObject();
        try { commitBody.put("message", message == null || message.trim().isEmpty() ? "Update from XCode Studio" : message); commitBody.put("tree", treeResult.getString("sha")); if (parentCommit != null) { JSONArray parents = new JSONArray(); parents.put(parentCommit); commitBody.put("parents", parents); } }
        catch (Exception ex) { throw new IOException(ex); }
        JSONObject newCommit = call("POST", "/repos/" + enc(owner) + "/" + enc(repo) + "/git/commits", commitBody);
        JSONObject refBody = new JSONObject();
        try { refBody.put("sha", newCommit.getString("sha")); }
        catch (Exception ex) { throw new IOException(ex); }
        if (parentCommit == null) {
            try { refBody.put("ref", "refs/heads/" + targetBranch); call("POST", "/repos/" + enc(owner) + "/" + enc(repo) + "/git/refs", refBody); }
            catch (IOException createFailure) {
                // If another client initialized it concurrently, try a normal update.
                call("PATCH", "/repos/" + enc(owner) + "/" + enc(repo) + "/git/refs/heads/" + enc(targetBranch), withForce(refBody));
            }
        } else call("PATCH", "/repos/" + enc(owner) + "/" + enc(repo) + "/git/refs/heads/" + enc(targetBranch), withForce(refBody));
    }

    /** Download a repository source archive to a temporary ZIP file. */
    public File downloadRepositoryZip(String owner, String repo, String branch, File destination) throws IOException {
        String ref = branch == null || branch.trim().isEmpty() ? "main" : branch.trim();
        String url = API + "/repos/" + enc(owner) + "/" + enc(repo) + "/zipball/" + enc(ref);
        HttpURLConnection api = (HttpURLConnection) new URL(url).openConnection();
        api.setInstanceFollowRedirects(false);
        api.setRequestMethod("GET"); api.setConnectTimeout(20000); api.setReadTimeout(90000);
        for (Map.Entry<String, String> h : headers().entrySet()) api.setRequestProperty(h.getKey(), h.getValue());
        int status = api.getResponseCode();
        InputStream source;
        HttpURLConnection redirected = null;
        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
            String location = api.getHeaderField("Location"); api.disconnect();
            if (location == null || !location.startsWith("https://")) throw new IOException("GitHub returned an invalid archive URL");
            redirected = (HttpURLConnection) new URL(location).openConnection();
            redirected.setInstanceFollowRedirects(true); redirected.setConnectTimeout(20000); redirected.setReadTimeout(90000);
            int redirectedStatus = redirected.getResponseCode();
            if (redirectedStatus < 200 || redirectedStatus >= 300) { redirected.disconnect(); throw new IOException("Repository archive download failed (HTTP " + redirectedStatus + ")"); }
            source = redirected.getInputStream();
        } else if (status >= 200 && status < 300) source = api.getInputStream();
        else {
            String detail = "GitHub archive download failed (HTTP " + status + ")";
            try { InputStream err = api.getErrorStream(); if (err != null) detail += ": " + HttpJson.trim(HttpJson.readText(err)); } catch (Exception ignored) {}
            api.disconnect(); throw new IOException(detail);
        }
        long total = 0;
        try (InputStream in = new BufferedInputStream(source); OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) != -1) { total += n; if (total > 300L * 1024L * 1024L) throw new IOException("Repository archive exceeds 300 MB"); out.write(b, 0, n); }
        } finally { api.disconnect(); if (redirected != null) redirected.disconnect(); }
        return destination;
    }

    public JSONObject dispatchWorkflow(String owner, String repo, String workflow, String branch) throws IOException {
        JSONObject body = new JSONObject();
        try { body.put("ref", branch == null || branch.isEmpty() ? "main" : branch); }
        catch (Exception ex) { throw new IOException(ex); }
        HttpJson.Response r = HttpJson.request("POST", API + "/repos/" + enc(owner) + "/" + enc(repo) + "/actions/workflows/" + enc(workflow) + "/dispatches", headers(), body);
        if (r.status != 204 && (r.status < 200 || r.status >= 300)) HttpJson.requireSuccess(r);
        return new JSONObject();
    }

    /** Stream a workflow artifact ZIP while keeping the PAT away from the signed storage redirect. */
    public void downloadArtifactZip(String owner, String repo, long artifactId, OutputStream destination) throws IOException {
        String endpoint = API + "/repos/" + enc(owner) + "/" + enc(repo) + "/actions/artifacts/" + artifactId + "/zip";
        HttpURLConnection api = (HttpURLConnection) new URL(endpoint).openConnection();
        api.setInstanceFollowRedirects(false); api.setRequestMethod("GET"); api.setConnectTimeout(20000); api.setReadTimeout(90000);
        for (Map.Entry<String, String> h : headers().entrySet()) api.setRequestProperty(h.getKey(), h.getValue());
        int status = api.getResponseCode();
        HttpURLConnection redirected = null;
        InputStream source;
        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
            String location = api.getHeaderField("Location"); api.disconnect();
            if (location == null || !location.startsWith("https://")) throw new IOException("GitHub returned an invalid artifact download URL");
            redirected = (HttpURLConnection) new URL(location).openConnection();
            redirected.setInstanceFollowRedirects(true); redirected.setConnectTimeout(20000); redirected.setReadTimeout(120000);
            int redirectedStatus = redirected.getResponseCode();
            if (redirectedStatus < 200 || redirectedStatus >= 300) { redirected.disconnect(); throw new IOException("Artifact download failed (HTTP " + redirectedStatus + ")"); }
            source = redirected.getInputStream();
        } else if (status >= 200 && status < 300) source = api.getInputStream();
        else {
            String detail = "Artifact download failed (HTTP " + status + ")";
            try { InputStream err = api.getErrorStream(); if (err != null) detail += ": " + HttpJson.trim(HttpJson.readText(err)); } catch (Exception ignored) {}
            api.disconnect(); throw new IOException(detail);
        }
        try (InputStream in = new BufferedInputStream(source)) {
            byte[] b = new byte[8192]; int n; long total = 0;
            while ((n = in.read(b)) != -1) { total += n; if (total > 300L * 1024L * 1024L) throw new IOException("Artifact archive exceeds 300 MB"); destination.write(b, 0, n); }
        } finally { api.disconnect(); if (redirected != null) redirected.disconnect(); }
    }

    public JSONArray workflowRuns(String owner, String repo) throws IOException {
        JSONObject response = call("GET", "/repos/" + enc(owner) + "/" + enc(repo) + "/actions/runs?per_page=20", null);
        JSONArray runs = response.optJSONArray("workflow_runs"); return runs == null ? new JSONArray() : runs;
    }

    public JSONObject listArtifacts(String owner, String repo) throws IOException {
        return call("GET", "/repos/" + enc(owner) + "/" + enc(repo) + "/actions/artifacts?per_page=100", null);
    }

    private static JSONObject withForce(JSONObject original) throws IOException {
        try { JSONObject copy = new JSONObject(original.toString()); copy.put("force", false); return copy; }
        catch (Exception ex) { throw new IOException(ex); }
    }
    private static void collectFiles(File base, File current, List<File> output) throws IOException {
        File[] children = current.listFiles(); if (children == null) return;
        Arrays.sort(children, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File child : children) {
            String n = child.getName();
            if (n.equals(".git") || n.equals("build") || n.equals(".gradle") || n.equals(".idea")) continue;
            if (child.isDirectory()) collectFiles(base, child, output); else if (child.isFile()) output.add(child);
        }
    }
    private static byte[] readLimited(File f, long max) throws IOException {
        if (f.length() > max) throw new IOException("File too large to upload: " + f.getName());
        try (InputStream in = new FileInputStream(f); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) out.write(b, 0, n); return out.toByteArray();
        }
    }
    private static String enc(String s) throws IOException { return URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
}
