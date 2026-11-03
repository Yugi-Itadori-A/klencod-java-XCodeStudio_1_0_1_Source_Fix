package com.xio.xcodestudio;

import android.content.ContentResolver;
import android.net.Uri;
import java.io.*;
import java.util.zip.*;

public final class ZipManager {
    private static final int MAX_ENTRIES = 12000;
    private static final long MAX_UNCOMPRESSED = 300L * 1024L * 1024L;
    private static final long MAX_ARCHIVE = 300L * 1024L * 1024L;
    private ZipManager() {}

    public static int importZip(ContentResolver resolver, Uri uri, File destination) throws IOException {
        File temp = File.createTempFile("xio-import-", ".zip", destination.getParentFile());
        try {
            long copied = 0;
            try (InputStream in = resolver.openInputStream(uri); OutputStream out = new FileOutputStream(temp)) {
                if (in == null) throw new IOException("Cannot read selected archive");
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) != -1) { copied += n; if (copied > MAX_ARCHIVE) throw new IOException("Archive exceeds 300 MB"); out.write(buf, 0, n); }
            }
            return extract(temp, destination);
        } finally { if (temp.exists()) temp.delete(); }
    }

    public static int extract(File archive, File destination) throws IOException {
        String base = destination.getCanonicalPath() + File.separator;
        int count = 0; long total = 0;
        if (!destination.exists() && !destination.mkdirs()) throw new IOException("Cannot create destination");
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry; byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                if (++count > MAX_ENTRIES) throw new IOException("Archive contains too many entries");
                String raw = entry.getName();
                if (raw == null || raw.trim().isEmpty() || raw.startsWith("/") || raw.startsWith("\\") || raw.matches("^[A-Za-z]:.*"))
                    throw new IOException("Unsafe archive path");
                File out = new File(destination, raw);
                String canonical = out.getCanonicalPath();
                if (!canonical.equals(destination.getCanonicalPath()) && !canonical.startsWith(base)) throw new IOException("Archive path traversal blocked");
                if (entry.isDirectory()) { if (!out.exists() && !out.mkdirs()) throw new IOException("Cannot create directory"); }
                else {
                    File parent = out.getParentFile(); if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Cannot create directory");
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                        int n; while ((n = zis.read(buf)) != -1) { total += n; if (total > MAX_UNCOMPRESSED) throw new IOException("Uncompressed archive exceeds 300 MB"); os.write(buf, 0, n); }
                    }
                }
                zis.closeEntry();
            }
        }
        return count;
    }

    public static void exportProject(File project, ContentResolver resolver, Uri target) throws IOException {
        try (OutputStream raw = resolver.openOutputStream(target, "w")) {
            if (raw == null) throw new IOException("Cannot open export destination");
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(raw))) {
                zipDirectory(project, project, zip);
            }
        }
    }

    private static void zipDirectory(File base, File current, ZipOutputStream zip) throws IOException {
        File[] children = current.listFiles(); if (children == null) return;
        for (File child : children) {
            if (child.getName().equals(".git") || child.getName().equals("build") || child.getName().equals(".gradle")) continue;
            String path = ProjectManager.relativePath(base, child);
            if (child.isDirectory()) { ZipEntry dir = new ZipEntry(path + "/"); zip.putNextEntry(dir); zip.closeEntry(); zipDirectory(base, child, zip); }
            else {
                zip.putNextEntry(new ZipEntry(path));
                try (InputStream in = new BufferedInputStream(new FileInputStream(child))) { byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) zip.write(buf, 0, n); }
                zip.closeEntry();
            }
        }
    }
}
