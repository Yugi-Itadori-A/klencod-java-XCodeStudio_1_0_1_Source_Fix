package com.xio.xcodestudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * XCode Studio — native Android front end.
 * Network operations are dispatched away from the UI thread. Imported extension code is never executed.
 */
public final class MainActivity extends Activity {
    private static final int BG = 0xFF08090D, PANEL = 0xFF12151D, PANEL2 = 0xFF1A1E29;
    private static final int TEXT = 0xFFF2F4FA, MUTED = 0xFF9AA3B5, ACCENT = 0xFF8B9BFF, RED = 0xFFFF7373;
    private static final int REQUEST_IMPORT = 1001, REQUEST_EXPORT = 1002, REQUEST_SCAN = 1003, REQUEST_ARTIFACT_SAVE = 1004;
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private ProjectManager projects;
    private ExtensionManager extensions;
    private LinearLayout root, header, nav, content;
    private File selectedProject;
    private String selectedFile;
    private EditText editor;
    private TextView editorFileLabel, statusLine;
    private String currentTab = "projects";
    private long pendingArtifactId = -1;
    private String pendingArtifactName = "github-artifact";
    private String pendingArtifactOwner = "";
    private String pendingArtifactRepo = "";
    private String localeCode = "ar";
    private boolean isRtl = true;
    private int themeMode = 0; // 0 dark, 1 black, 2 light
    private final String[] tabKeys = {"projects", "editor", "github", "ai", "security", "extensions", "settings"};
    private final String[] tabTranslationKeys = {"projects", "editor", "github", "ai", "security", "extensions", "settings"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            projects = new ProjectManager(this);
            extensions = new ExtensionManager(this);
            localeCode = getPreferences(MODE_PRIVATE).getString("locale", "ar");
            themeMode = getPreferences(MODE_PRIVATE).getInt("theme", 0);
            LanguageCatalog.Language language = LanguageCatalog.byCode(localeCode);
            isRtl = language.rtl;
            buildShell();
            showTab("projects");
        } catch (Throwable error) {
            TextView fallback = new TextView(this);
            fallback.setText("XCode Studio could not start.\n" + error.getClass().getSimpleName() + ": " + error.getMessage());
            fallback.setTextColor(TEXT); fallback.setPadding(32, 48, 32, 32); fallback.setTextSize(16); fallback.setBackgroundColor(BG);
            setContentView(fallback);
        }
    }

    @Override protected void onPause() {
        saveEditorQuietly();
        super.onPause();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    private int c(int dark, int light) {
        if (themeMode == 2) return light;
        if (themeMode == 1) {
            if (dark == BG) return 0xFF000000;
            if (dark == PANEL) return 0xFF070707;
            if (dark == PANEL2) return 0xFF151515;
        }
        return dark;
    }
    private void buildShell() {
        getWindow().setStatusBarColor(themeMode == 2 ? 0xFFF4F5F8 : (themeMode == 1 ? 0xFF000000 : BG));
        getWindow().setNavigationBarColor(themeMode == 2 ? 0xFFF4F5F8 : (themeMode == 1 ? 0xFF000000 : BG));
        getWindow().getDecorView().setSystemUiVisibility(themeMode == 2 ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(c(BG, 0xFFF4F5F8));
        header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL); header.setPadding(dp(18), dp(15), dp(18), dp(12));
        header.setBackgroundColor(c(PANEL, 0xFFFFFFFF));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = new TextView(this); brand.setText("XCode Studio"); brand.setTextColor(c(TEXT, 0xFF111827)); brand.setTextSize(21); brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView version = new TextView(this); version.setText("XIO  •  1.0.0"); version.setTextColor(MUTED); version.setTextSize(11); top.addView(version);
        header.addView(top);
        TextView subtitle = new TextView(this); subtitle.setText("بيئة تطوير ومشاريع برمجية لأندرويد"); subtitle.setTextColor(MUTED); subtitle.setTextSize(12); subtitle.setPadding(0, dp(4), 0, 0); header.addView(subtitle);
        root.addView(header);

        HorizontalScrollView navScroll = new HorizontalScrollView(this); navScroll.setHorizontalScrollBarEnabled(false); navScroll.setBackgroundColor(c(PANEL, 0xFFFFFFFF));
        nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(dp(8), dp(4), dp(8), dp(8)); navScroll.addView(nav);
        root.addView(navScroll);
        for (int i = 0; i < tabKeys.length; i++) {
            final String key = tabKeys[i]; Button b = smallButton(LocaleText.get(localeCode, tabTranslationKeys[i]));
            b.setOnClickListener(v -> showTab(key));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(40)); p.setMargins(dp(3), 0, dp(3), 0); nav.addView(b, p);
        }
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        statusLine = new TextView(this); statusLine.setText("جاهز"); statusLine.setTextSize(11); statusLine.setTextColor(MUTED); statusLine.setPadding(dp(16), dp(8), dp(16), dp(8)); statusLine.setBackgroundColor(c(PANEL, 0xFFFFFFFF)); root.addView(statusLine);
        setContentView(root);
        root.setLayoutDirection(isRtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        content.setLayoutDirection(isRtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
    }

    private void showTab(String tab) {
        if ("editor".equals(currentTab)) saveEditorQuietly();
        currentTab = tab; content.removeAllViews();
        switch (tab) {
            case "projects": renderProjects(); break;
            case "editor": renderEditor(); break;
            case "github": renderGitHub(); break;
            case "ai": renderAI(); break;
            case "security": renderSecurity(); break;
            case "extensions": renderExtensions(); break;
            case "settings": renderSettings(); break;
            default: renderProjects();
        }
    }

    private void renderProjects() {
        heading("مساحة المشاريع", "أنشئ مشاريع محلية، افتح ملفاتها، أو استورد مشروعًا مضغوطًا.");
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button create = primaryButton("＋ مشروع جديد"); create.setOnClickListener(v -> createProjectDialog());
        Button importZip = secondaryButton("استيراد ZIP"); importZip.setOnClickListener(v -> chooseZipImport());
        actions.addView(create, new LinearLayout.LayoutParams(0, dp(46), 1)); LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(46), 1); ip.setMargins(dp(8), 0, 0, 0); actions.addView(importZip, ip); content.addView(actions);
        gap(14);
        File[] all = projects.listProjects();
        if (all.length == 0) { emptyState("لا توجد مشاريع بعد", "أنشئ مشروع Java/Android أو Web أو Python أو C++ أو Flutter، أو استورد ZIP."); return; }
        for (File project : all) {
            LinearLayout card = card(); card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = new TextView(this); name.setText(project.getName()); name.setTextColor(c(TEXT, 0xFF111827)); name.setTextSize(16); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            TextView selected = new TextView(this); selected.setText(project.equals(selectedProject) ? "● نشط" : "مشروع"); selected.setTextColor(project.equals(selectedProject) ? ACCENT : MUTED); selected.setTextSize(11); row.addView(selected);
            card.addView(row); TextView path = muted("تخزين داخلي للتطبيق • " + project.list().length + " عنصرًا مباشرًا"); card.addView(path);
            LinearLayout buttons = new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
            Button open = smallButton("فتح"); open.setOnClickListener(v -> { selectedProject = project; selectedFile = null; showTab("editor"); });
            Button rename = smallButton("تسمية"); rename.setOnClickListener(v -> renameProject(project));
            Button export = smallButton("تصدير"); export.setOnClickListener(v -> { selectedProject = project; String fileName = project.getName() + ".zip"; createExportDocument(fileName); });
            Button delete = smallButton("حذف"); delete.setTextColor(RED); delete.setOnClickListener(v -> confirmDeleteProject(project));
            for (Button b : new Button[]{open, rename, export, delete}) { LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(38), 1); bp.setMargins(dp(3), dp(8), dp(3), 0); buttons.addView(b, bp); }
            card.addView(buttons); addCard(card);
        }
    }

    private void createProjectDialog() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(8), dp(6), dp(8), 0);
        EditText name = field("اسم المشروع", InputType.TYPE_CLASS_TEXT); form.addView(name);
        Spinner template = spinner(new String[]{"فارغ", "Android Java", "Web HTML/CSS/JS", "Python", "C++ CMake", "Flutter"}); form.addView(template);
        new AlertDialog.Builder(this).setTitle("إنشاء مشروع").setView(form).setNegativeButton("إلغاء", null).setPositiveButton("إنشاء", (d, w) -> {
            String[] ids = {"empty", "android-java", "web", "python", "cpp", "flutter"};
            try { selectedProject = projects.createProject(name.getText().toString(), ids[template.getSelectedItemPosition()]); selectedFile = null; showTab("editor"); toast("تم إنشاء المشروع"); }
            catch (Exception ex) { toast(ex.getMessage()); }
        }).show();
    }

    private void renameProject(File project) {
        EditText input = field("الاسم الجديد", InputType.TYPE_CLASS_TEXT); input.setText(project.getName());
        new AlertDialog.Builder(this).setTitle("إعادة تسمية المشروع").setView(input).setNegativeButton("إلغاء", null).setPositiveButton("حفظ", (d, w) -> {
            try { String old = project.getName(); String safe = ProjectManager.sanitizeName(input.getText().toString()); projects.renameProject(project, safe); if (selectedProject != null && selectedProject.getName().equals(old)) selectedProject = new File(projects.getWorkspace(), safe); renderProjects(); }
            catch (Exception ex) { toast(ex.getMessage()); }
        }).show();
    }

    private void confirmDeleteProject(File project) {
        new AlertDialog.Builder(this).setTitle("حذف المشروع").setMessage("سيتم حذف ملفات المشروع نهائيًا من مساحة التطبيق. متابعة؟")
                .setNegativeButton("إلغاء", null).setPositiveButton("حذف", (d, w) -> {
                    try { projects.deleteProject(project); if (project.equals(selectedProject)) { selectedProject = null; selectedFile = null; } renderProjects(); toast("تم حذف المشروع"); }
                    catch (Exception ex) { toast(ex.getMessage()); }
                }).show();
    }

    private void chooseZipImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/zip"); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        startActivityForResult(i, REQUEST_IMPORT);
    }

    private void createExportDocument(String name) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/zip"); i.addCategory(Intent.CATEGORY_OPENABLE); i.putExtra(Intent.EXTRA_TITLE, name); startActivityForResult(i, REQUEST_EXPORT);
    }

    private String displayName(Uri uri) {
        String last = uri.getLastPathSegment(); if (last == null) return "ImportedProject";
        int slash = last.lastIndexOf('/'); if (slash >= 0) last = last.substring(slash + 1);
        if (last.contains(":")) last = last.substring(last.lastIndexOf(':') + 1);
        if (last.toLowerCase().endsWith(".zip")) last = last.substring(0, last.length() - 4);
        return last.isEmpty() ? "ImportedProject" : last;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_ARTIFACT_SAVE) {
            final long artifactId = pendingArtifactId; final String owner = pendingArtifactOwner, repo = pendingArtifactRepo;
            final String token = githubToken();
            if (artifactId <= 0 || token.isEmpty()) { toast("تعذر تحديد ملف البناء أو رمز GitHub"); return; }
            background("تنزيل Artifact", () -> {
                try (java.io.OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                    if (out == null) throw new IOException("تعذر فتح ملف الحفظ");
                    new GitHubClient(token).downloadArtifactZip(owner, repo, artifactId, out);
                }
                return "تم حفظ أرشيف البناء في الموقع الذي اخترته";
            }, result -> toast(result));
        } else if (requestCode == REQUEST_IMPORT) {
            final String name = displayName(uri);
            background("استيراد المشروع", () -> {
                File dest = projects.createImportedProject(name);
                try { int count = ZipManager.importZip(getContentResolver(), uri, dest); return "تم استيراد " + count + " عنصرًا إلى " + dest.getName(); }
                catch (Exception ex) { try { ProjectManager.deleteRecursively(dest); } catch (Exception ignored) {} throw ex; }
            }, result -> { renderProjects(); toast(result); });
        } else if (requestCode == REQUEST_EXPORT) {
            if (selectedProject == null) { toast("اختر مشروعًا أولًا"); return; }
            File p = selectedProject;
            background("تصدير المشروع", () -> { ZipManager.exportProject(p, getContentResolver(), uri); return "تم إنشاء ملف ZIP"; }, result -> toast(result));
        } else if (requestCode == REQUEST_SCAN) {
            scanSelectedFile(uri);
        }
    }

    private void renderEditor() {
        heading("محرر الأكواد", selectedProject == null ? "اختر مشروعًا من صفحة المشاريع أولًا." : selectedProject.getName());
        if (selectedProject == null) { action("الانتقال إلى المشاريع", () -> showTab("projects")); return; }
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button add = smallButton("＋ ملف"); add.setOnClickListener(v -> createFileDialog());
        Button refresh = smallButton("تحديث"); refresh.setOnClickListener(v -> { saveEditorQuietly(); renderEditor(); });
        Button diagnose = smallButton("تشخيص"); diagnose.setOnClickListener(v -> diagnoseProject());
        for (Button b : new Button[]{add, refresh, diagnose}) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(40), 1); p.setMargins(dp(2), 0, dp(2), 0); row.addView(b, p); }
        content.addView(row); gap(10);
        List<String> files;
        try { files = projects.listFiles(selectedProject); } catch (Exception ex) { emptyState("تعذر قراءة المشروع", ex.getMessage()); return; }
        if (files.isEmpty()) { emptyState("المشروع فارغ", "أنشئ ملفًا جديدًا من الزر أعلاه."); return; }
        headingSmall("ملفات المشروع");
        LinearLayout fileList = card(); fileList.setOrientation(LinearLayout.VERTICAL);
        for (String path : files) {
            Button file = smallButton((path.equals(selectedFile) ? "●  " : "▸  ") + path);
            file.setGravity(isRtl ? Gravity.RIGHT | Gravity.CENTER_VERTICAL : Gravity.LEFT | Gravity.CENTER_VERTICAL);
            file.setOnClickListener(v -> openFile(path));
            fileList.addView(file, new LinearLayout.LayoutParams(-1, dp(39)));
        }
        addCard(fileList); gap(12);
        if (selectedFile != null) renderEditorPane();
        else mutedBlock("اختر ملفًا من القائمة لفتحه في المحرر.");
    }

    private void renderEditorPane() {
        TextView label = new TextView(this); label.setText(selectedFile); label.setTextColor(ACCENT); label.setTextSize(13); label.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); content.addView(label); gap(6);
        editor = new EditText(this); editor.setTextColor(themeMode == 2 ? 0xFF202433 : TEXT); editor.setHintTextColor(MUTED); editor.setTextSize(13); editor.setTypeface(Typeface.MONOSPACE); editor.setGravity(Gravity.TOP | Gravity.START); editor.setPadding(dp(12), dp(12), dp(12), dp(12)); editor.setMinLines(14); editor.setHorizontallyScrolling(true); editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setBackground(makeBg(themeMode == 2 ? 0xFFFFFFFF : 0xFF090C13, 12, themeMode == 2 ? 0xFFE0E4ED : 0xFF252B3A));
        try { editor.setText(projects.readText(selectedProject, selectedFile)); } catch (Exception ex) { editor.setText(""); toast(ex.getMessage()); }
        editor.addTextChangedListener(new SyntaxHighlighter(editor));
        content.addView(editor, new LinearLayout.LayoutParams(-1, dp(360)));
        gap(8); LinearLayout buttons = new LinearLayout(this);
        Button save = primaryButton("حفظ الملف"); save.setOnClickListener(v -> saveEditor());
        Button remove = secondaryButton("حذف الملف"); remove.setOnClickListener(v -> deleteCurrentFile());
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(44), 1)); LinearLayout.LayoutParams r = new LinearLayout.LayoutParams(0, dp(44), 1); r.setMargins(dp(8), 0, 0, 0); buttons.addView(remove, r); content.addView(buttons);
    }

    private void openFile(String path) {
        if (editor != null && selectedFile != null) saveEditorQuietly();
        selectedFile = path; editor = null; renderEditor();
    }

    private void saveEditorQuietly() { if (editor == null || selectedProject == null || selectedFile == null) return; try { projects.writeText(selectedProject, selectedFile, editor.getText().toString()); } catch (Exception ignored) {} }
    private void saveEditor() {
        if (editor == null || selectedProject == null || selectedFile == null) return;
        try { projects.writeText(selectedProject, selectedFile, editor.getText().toString()); setStatus("تم حفظ " + selectedFile); toast("تم الحفظ"); }
        catch (Exception ex) { toast(ex.getMessage()); }
    }
    private void createFileDialog() {
        EditText input = field("مثال: src/Main.java", InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this).setTitle("ملف جديد").setView(input).setNegativeButton("إلغاء", null).setPositiveButton("إنشاء", (d, w) -> {
            try { String path = input.getText().toString().trim(); saveEditorQuietly(); projects.createFile(selectedProject, path); selectedFile = path; renderEditor(); }
            catch (Exception ex) { toast(ex.getMessage()); }
        }).show();
    }
    private void deleteCurrentFile() {
        if (selectedProject == null || selectedFile == null) return;
        new AlertDialog.Builder(this).setTitle("حذف الملف").setMessage(selectedFile).setNegativeButton("إلغاء", null).setPositiveButton("حذف", (d, w) -> {
            try { projects.deleteFile(selectedProject, selectedFile); selectedFile = null; editor = null; renderEditor(); toast("تم حذف الملف"); }
            catch (Exception ex) { toast(ex.getMessage()); }
        }).show();
    }
    private void diagnoseProject() {
        try { List<String> findings = projects.diagnose(selectedProject); new AlertDialog.Builder(this).setTitle("تشخيص أساسي").setMessage(join(findings, "\n\n") + "\n\nهذا فحص بنية مبدئي، وليس تشغيلًا للمترجم أو تدقيقًا أمنيًا.").setPositiveButton("حسنًا", null).show(); }
        catch (Exception ex) { toast(ex.getMessage()); }
    }

    private void renderGitHub() {
        heading("GitHub وعمليات البناء", "ربط Personal Access Token، عرض المستودعات، رفع المشروع وتشغيل workflow ومتابعة النتائج.");
        EditText token = field("GitHub Personal Access Token", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); String saved = SecureStore.get(this, "github_token"); if (saved != null) token.setText(saved); content.addView(token);
        LinearLayout row = new LinearLayout(this);
        Button save = primaryButton("حفظ الرمز بأمان"); save.setOnClickListener(v -> saveSecret("github_token", token.getText().toString(), "تم حفظ رمز GitHub مشفرًا على الجهاز"));
        Button test = secondaryButton("التحقق من الحساب"); test.setOnClickListener(v -> { String t = token.getText().toString().trim(); if (t.isEmpty()) { toast("أدخل الرمز أولًا"); return; } try { SecureStore.put(this, "github_token", t); githubUser(t); } catch (Exception ex) { toast("تعذر حفظ الرمز: " + ex.getMessage()); } });
        row.addView(save, new LinearLayout.LayoutParams(0, dp(43), 1)); LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(43), 1); tp.setMargins(dp(8), 0, 0, 0); row.addView(test, tp); content.addView(row);
        gap(12); headingSmall("المستودعات");
        action("عرض مستودعات الحساب", () -> loadRepositories());
        EditText owner = field("اسم مالك المستودع (owner)", InputType.TYPE_CLASS_TEXT); EditText repo = field("اسم المستودع (repository)", InputType.TYPE_CLASS_TEXT); EditText branch = field("الفرع", InputType.TYPE_CLASS_TEXT); branch.setText("main");
        content.addView(owner); content.addView(repo); content.addView(branch);
        LinearLayout remoteActions = new LinearLayout(this);
        Button importRemote = secondaryButton("استيراد مشروع من GitHub");
        importRemote.setOnClickListener(v -> importGitHubProject(owner.getText().toString().trim(), repo.getText().toString().trim(), branch.getText().toString().trim()));
        Button createRemote = secondaryButton("إنشاء مستودع جديد");
        createRemote.setOnClickListener(v -> createGitHubRepository());
        remoteActions.addView(importRemote, new LinearLayout.LayoutParams(0, dp(43), 1));
        LinearLayout.LayoutParams crp = new LinearLayout.LayoutParams(0, dp(43), 1); crp.setMargins(dp(7), 0, 0, 0); remoteActions.addView(createRemote, crp);
        content.addView(remoteActions);
        Button push = primaryButton("رفع المشروع الحالي إلى GitHub"); push.setOnClickListener(v -> {
            if (selectedProject == null) { toast("اختر مشروعًا من صفحة المشاريع"); return; }
            String o = owner.getText().toString().trim(), r = repo.getText().toString().trim(), b = branch.getText().toString().trim(), t = SecureStore.get(this, "github_token");
            if (t == null || o.isEmpty() || r.isEmpty()) { toast("أدخل رمزًا محفوظًا ومالكًا ومستودعًا"); return; }
            File p = selectedProject; background("رفع الملفات إلى GitHub", () -> { new GitHubClient(t).pushProject(p, o, r, b, "Update from XCode Studio"); return "اكتمل رفع ملفات المشروع"; }, result -> toast(result));
        }); content.addView(push);
        gap(10); EditText workflow = field("اسم ملف Workflow", InputType.TYPE_CLASS_TEXT); workflow.setText("android-build.yml"); content.addView(workflow);
        Button run = secondaryButton("تشغيل GitHub Actions"); run.setOnClickListener(v -> {
            String o = owner.getText().toString().trim(), r = repo.getText().toString().trim(), b = branch.getText().toString().trim(), wf = workflow.getText().toString().trim(), t = SecureStore.get(this, "github_token");
            if (t == null || o.isEmpty() || r.isEmpty()) { toast("أدخل بيانات المستودع واحفظ الرمز"); return; }
            background("تشغيل Workflow", () -> { new GitHubClient(t).dispatchWorkflow(o, r, wf, b); return "تم إرسال طلب تشغيل Workflow. راجع حالة التشغيل من الزر التالي."; }, result -> toast(result));
        }); content.addView(run);
        Button runs = smallButton("عرض آخر عمليات البناء"); runs.setOnClickListener(v -> loadRuns(owner.getText().toString().trim(), repo.getText().toString().trim())); content.addView(runs);
        Button artifacts = smallButton("عرض ملفات APK / Artifacts"); artifacts.setOnClickListener(v -> loadArtifacts(owner.getText().toString().trim(), repo.getText().toString().trim())); content.addView(artifacts);
        mutedBlock("ملاحظة: يلزم تفعيل GitHub Actions وصلاحيات Contents/Actions للرمز. بناء APK يتم على GitHub-hosted runner، ولا يحدث تلقائيًا على الهاتف.");
    }

    private String githubToken() { String t = SecureStore.get(this, "github_token"); return t == null ? "" : t; }
    private void githubUser(String token) { background("التحقق من GitHub", () -> { JSONObject u = new GitHubClient(token).user(); return "تم الاتصال: " + u.optString("login") + " • " + u.optString("html_url"); }, this::toast); }
    private void loadRepositories() {
        String t = githubToken(); if (t.isEmpty()) { toast("احفظ رمز GitHub أولًا"); return; }
        background("تحميل المستودعات", () -> { JSONArray list = new GitHubClient(t).repositories(); StringBuilder s = new StringBuilder(); for (int i = 0; i < list.length(); i++) { JSONObject o = list.getJSONObject(i); s.append(o.optString("full_name")).append(o.optBoolean("private") ? "  • Private" : "  • Public").append("\n").append(o.optString("html_url")).append("\n\n"); } return s.length() == 0 ? "لا توجد مستودعات ظاهرة لهذا الحساب." : s.toString(); }, result -> showMessage("المستودعات", result));
    }
    private void createGitHubRepository() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(8), dp(4), dp(8), 0);
        EditText name = field("اسم المستودع", InputType.TYPE_CLASS_TEXT); EditText description = field("وصف اختياري", InputType.TYPE_CLASS_TEXT);
        CheckBox privateRepo = new CheckBox(this); privateRepo.setText("مستودع خاص"); privateRepo.setTextColor(c(TEXT, 0xFF202433));
        form.addView(name); form.addView(description); form.addView(privateRepo);
        new AlertDialog.Builder(this).setTitle("إنشاء مستودع GitHub").setView(form).setNegativeButton("إلغاء", null).setPositiveButton("إنشاء", (d, w) -> {
            String token = githubToken(), n = name.getText().toString().trim(), desc = description.getText().toString().trim();
            if (token.isEmpty() || n.isEmpty()) { toast("احفظ رمز GitHub وأدخل اسم المستودع"); return; }
            background("إنشاء المستودع", () -> { JSONObject o = new GitHubClient(token).createRepository(n, desc, privateRepo.isChecked()); return "تم إنشاء المستودع: " + o.optString("full_name") + "\n" + o.optString("html_url"); }, result -> showMessage("GitHub", result));
        }).show();
    }

    private void importGitHubProject(String owner, String repo, String branch) {
        String token = githubToken();
        if (token.isEmpty() || owner.isEmpty() || repo.isEmpty()) { toast("احفظ الرمز وأدخل owner وrepository"); return; }
        background("استيراد من GitHub", () -> {
            File archive = File.createTempFile("xio-github-", ".zip", getCacheDir());
            File unpack = new File(getCacheDir(), "xio-github-unpack-" + System.nanoTime());
            File destination = null;
            try {
                new GitHubClient(token).downloadRepositoryZip(owner, repo, branch, archive);
                ZipManager.extract(archive, unpack);
                destination = projects.createImportedProject(repo);
                File[] roots = unpack.listFiles();
                File source = roots != null && roots.length == 1 && roots[0].isDirectory() ? roots[0] : unpack;
                copyDirectoryContents(source, destination);
                return "تم استيراد " + owner + "/" + repo + " إلى " + destination.getName();
            } catch (Exception ex) {
                if (destination != null) try { ProjectManager.deleteRecursively(destination); } catch (Exception ignored) {}
                throw ex;
            } finally { archive.delete(); if (unpack.exists()) try { ProjectManager.deleteRecursively(unpack); } catch (Exception ignored) {} }
        }, result -> { renderProjects(); toast(result); });
    }

    private void copyDirectoryContents(File from, File to) throws IOException {
        File[] files = from.listFiles(); if (files == null) return;
        for (File file : files) {
            File target = new File(to, file.getName());
            if (file.isDirectory()) { if (!target.exists() && !target.mkdirs()) throw new IOException("Cannot create " + target.getName()); copyDirectoryContents(file, target); }
            else {
                try (java.io.InputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(file)); java.io.OutputStream out = new java.io.BufferedOutputStream(new java.io.FileOutputStream(target))) {
                    byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) out.write(b, 0, n);
                }
            }
        }
    }

    private void loadArtifacts(String owner, String repo) {
        String token = githubToken(); if (token.isEmpty() || owner.isEmpty() || repo.isEmpty()) { toast("أدخل بيانات المستودع واحفظ الرمز"); return; }
        setStatus("تحميل قائمة Artifacts…");
        io.execute(() -> {
            try {
                JSONObject response = new GitHubClient(token).listArtifacts(owner, repo);
                JSONArray all = response.optJSONArray("artifacts");
                ArrayList<JSONObject> available = new ArrayList<>();
                ArrayList<String> labels = new ArrayList<>();
                if (all != null) for (int i = 0; i < all.length(); i++) {
                    JSONObject item = all.getJSONObject(i);
                    if (item.optBoolean("expired")) continue;
                    available.add(item);
                    labels.add(item.optString("name", "Artifact") + " • " + (item.optLong("size_in_bytes") / 1024) + " KB");
                }
                runOnUiThread(() -> {
                    setStatus("اكتمل تحميل القائمة");
                    if (available.isEmpty()) { showMessage("Artifacts", "لا توجد ملفات بناء متاحة. شغّل Workflow وانتظر انتهاء البناء."); return; }
                    String[] choices = labels.toArray(new String[0]);
                    new AlertDialog.Builder(this).setTitle("اختر ملف البناء لتنزيله").setItems(choices, (dialog, which) -> {
                        JSONObject item = available.get(which);
                        pendingArtifactId = item.optLong("id", -1);
                        pendingArtifactName = item.optString("name", "github-artifact").replaceAll("[^A-Za-z0-9._-]", "_") + ".zip";
                        pendingArtifactOwner = owner; pendingArtifactRepo = repo;
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.setType("application/zip"); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.putExtra(Intent.EXTRA_TITLE, pendingArtifactName);
                        startActivityForResult(intent, REQUEST_ARTIFACT_SAVE);
                    }).setNegativeButton("إغلاق", null).show();
                });
            } catch (Exception ex) { runOnUiThread(() -> { setStatus("فشل تحميل Artifacts"); showMessage("حدث خطأ", ex.getMessage()); }); }
        });
    }

    private void loadRuns(String owner, String repo) {
        String t = githubToken(); if (t.isEmpty() || owner.isEmpty() || repo.isEmpty()) { toast("أدخل owner وrepository واحفظ الرمز"); return; }
        background("تحميل عمليات البناء", () -> { JSONArray list = new GitHubClient(t).workflowRuns(owner, repo); StringBuilder s = new StringBuilder(); for (int i = 0; i < list.length(); i++) { JSONObject o = list.getJSONObject(i); s.append("#").append(o.optInt("run_number")).append(" • ").append(o.optString("name")).append("\n").append("الحالة: ").append(o.optString("status")).append(" / ").append(o.optString("conclusion")).append("\n").append(o.optString("html_url")).append("\n\n"); } return s.length() == 0 ? "لا توجد عمليات بناء حتى الآن." : s.toString(); }, result -> showMessage("سجل البناء", result));
    }

    private void renderAI() {
        heading("مساعد البرمجة بالذكاء الاصطناعي", "استخدم مفتاح API الخاص بك. لن يُرسل كود المشروع إلا عند تحديد موافقة إرفاق الكود.");
        Spinner provider = spinner(new String[]{"OpenAI", "Gemini", "Claude", "OpenRouter", "Groq", "Mistral", "DeepSeek"}); content.addView(provider);
        EditText model = field("Model ID", InputType.TYPE_CLASS_TEXT); model.setText("gpt-4o-mini"); content.addView(model);
        EditText endpoint = field("Endpoint اختياري لمزوّد متوافق", InputType.TYPE_CLASS_TEXT); content.addView(endpoint);
        EditText key = field("API Key", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); String storedAiKey = SecureStore.get(this, "ai_key"); if (storedAiKey != null) key.setText(storedAiKey); content.addView(key);
        provider.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = provider.getSelectedItem().toString();
                String defaultModel = "gpt-4o-mini";
                if ("Gemini".equals(selected)) defaultModel = "gemini-2.5-flash";
                else if ("Claude".equals(selected)) defaultModel = "claude-sonnet-4-5";
                else if ("OpenRouter".equals(selected)) defaultModel = "openai/gpt-4o-mini";
                else if ("Groq".equals(selected)) defaultModel = "llama-3.3-70b-versatile";
                else if ("Mistral".equals(selected)) defaultModel = "mistral-small-latest";
                else if ("DeepSeek".equals(selected)) defaultModel = "deepseek-chat";
                model.setText(defaultModel);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        Button save = secondaryButton("حفظ مفتاح API بشكل مشفر"); save.setOnClickListener(v -> saveSecret("ai_key", key.getText().toString(), "تم حفظ مفتاح API")); content.addView(save);
        CheckBox consent = new CheckBox(this); consent.setText("أوافق على إرسال نص الملف المفتوح إلى المزوّد الخارجي"); consent.setTextColor(c(TEXT, 0xFF202433)); consent.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT)); content.addView(consent);
        EditText prompt = field("اكتب طلبك: شرح، مراجعة، إصلاح، أو اقتراح", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); prompt.setMinLines(4); content.addView(prompt);
        Button ask = primaryButton("إرسال إلى المساعد"); ask.setOnClickListener(v -> {
            String p = provider.getSelectedItem().toString(), m = model.getText().toString().trim(), e = endpoint.getText().toString().trim(), k = key.getText().toString().trim(), q = prompt.getText().toString();
            if (k.isEmpty()) k = SecureStore.get(this, "ai_key"); if (k == null || k.trim().isEmpty()) { toast("أدخل مفتاح API"); return; }
            String code = ""; if (consent.isChecked()) { if (editor == null) { toast("افتح ملفًا في المحرر أولًا"); return; } code = editor.getText().toString(); }
            final String source = code, token = k, providerName = p, modelName = m, url = e, question = q;
            background("الاتصال بمزوّد AI", () -> AIClient.ask(providerName, modelName, url, token, question, source), answer -> showMessage("إجابة المساعد", answer));
        }); content.addView(ask);
        mutedBlock("يتم إرسال الطلب إلى الخدمة المختارة عبر الإنترنت. لا تضع مفاتيحك داخل ملفات المشروع أو مستودعات Git.");
    }

    private void renderSecurity() {
        heading("فحص أمني للملفات", "فحص اختياري باستخدام VirusTotal. إرسال الملف يعني مشاركته مع مزوّد خارجي وقد يصبح متاحًا ضمن خدماته وفق شروطه.");
        EditText key = field("VirusTotal API Key", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); String old = SecureStore.get(this, "vt_key"); if (old != null) key.setText(old); content.addView(key);
        Button save = secondaryButton("حفظ مفتاح الفحص"); save.setOnClickListener(v -> saveSecret("vt_key", key.getText().toString(), "تم حفظ المفتاح")); content.addView(save);
        Button choose = primaryButton("اختيار APK لفحصه"); choose.setOnClickListener(v -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/vnd.android.package-archive"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, REQUEST_SCAN); }); content.addView(choose);
        mutedBlock("الحد المباشر في هذا العميل 32 MB. التقرير يعرض بصمة SHA-256 ونتائج التحليل التي يرجعها VirusTotal. عدم اكتشاف تهديدات لا يثبت أن الملف آمن 100%.");
    }

    private void scanSelectedFile(Uri uri) {
        String api = SecureStore.get(this, "vt_key"); if (api == null || api.isEmpty()) { toast("احفظ مفتاح VirusTotal أولًا"); return; }
        new AlertDialog.Builder(this).setTitle("تأكيد مشاركة الملف").setMessage("سيتم رفع ملف APK المحدد إلى VirusTotal لإجراء التحليل. هل توافق على مشاركة الملف مع الخدمة؟")
                .setNegativeButton("إلغاء", null).setPositiveButton("موافقة وفحص", (d, w) -> background("رفع الملف للفحص الأمني", () -> {
                    File temp = File.createTempFile("xio-scan-", ".apk", getCacheDir());
                    try {
                        try (java.io.InputStream in = getContentResolver().openInputStream(uri); java.io.OutputStream out = new java.io.FileOutputStream(temp)) {
                            if (in == null) throw new IOException("تعذرت قراءة الملف");
                            byte[] b = new byte[8192]; int n; long copied = 0;
                            while ((n = in.read(b)) != -1) { copied += n; if (copied > 32L * 1024L * 1024L) throw new IOException("حجم الملف يتجاوز حد الرفع المباشر 32 MB"); out.write(b, 0, n); }
                        }
                        String hash = VirusTotalClient.sha256(temp); JSONObject uploaded = VirusTotalClient.upload(temp, api); String id = uploaded.optJSONObject("data") == null ? "" : uploaded.optJSONObject("data").optString("id");
                        String result = "SHA-256: " + hash + "\nAnalysis ID: " + id;
                        if (!id.isEmpty()) { try { JSONObject analysis = VirusTotalClient.analysis(id, api); JSONObject stats = analysis.optJSONObject("data").optJSONObject("attributes").optJSONObject("stats"); if (stats != null) result += "\n\nإحصاءات التحليل الحالية:\nضار: " + stats.optInt("malicious") + "\nمشبوه: " + stats.optInt("suspicious") + "\nغير مكتشف: " + stats.optInt("undetected") + "\nغير ضار: " + stats.optInt("harmless"); } catch (Exception pending) { result += "\n\nالتحليل قيد المعالجة؛ أعد التحقق لاحقًا من Analysis ID."; } }
                        return result;
                    } finally { temp.delete(); }
                }, result -> showMessage("نتيجة VirusTotal", result)));
    }

    private void renderExtensions() {
        heading("الإضافات", "سجل إضافات محلي مع تعطيل افتراضي. لا يتم تشغيل ملفات الإضافات المستوردة تلقائيًا.");
        TextView path = muted("مجلد الإضافات: " + extensions.getDirectory().getAbsolutePath()); content.addView(path); gap(10);
        List<ExtensionManager.Extension> list = extensions.list();
        if (list.isEmpty()) emptyState("لا توجد إضافات مسجلة", "يجب أن تحتوي الإضافة على extension.json. هذا الإصدار يدير بيانات الإضافة فقط ولا يشغّل كودها.");
        for (ExtensionManager.Extension ext : list) {
            LinearLayout card = card(); card.setOrientation(LinearLayout.VERTICAL); TextView title = new TextView(this); title.setText(ext.name + "  •  " + ext.version); title.setTextColor(c(TEXT, 0xFF111827)); title.setTextSize(15); title.setTypeface(null, Typeface.BOLD); card.addView(title); card.addView(muted(ext.description));
            CheckBox enabled = new CheckBox(this); enabled.setText("مفعلة في السجل (لا يعني تشغيل كودها)"); enabled.setTextColor(c(TEXT, 0xFF202433)); enabled.setChecked(ext.enabled); enabled.setOnCheckedChangeListener((b, checked) -> extensions.setEnabled(ext.id, checked)); card.addView(enabled); addCard(card);
        }
    }

    private void renderSettings() {
        heading("الإعدادات", "اللغة واتجاه الواجهة والمظهر وإدارة البيانات المحلية.");
        headingSmall("لغة الواجهة");
        List<LanguageCatalog.Language> languages = LanguageCatalog.all(); String[] names = new String[languages.size()]; int selected = 0;
        for (int i = 0; i < names.length; i++) { names[i] = languages.get(i).name; if (languages.get(i).code.equals(localeCode)) selected = i; }
        Spinner language = spinner(names); language.setSelection(selected); content.addView(language);
        Button applyLang = primaryButton("تطبيق اللغة"); applyLang.setOnClickListener(v -> { LanguageCatalog.Language l = languages.get(language.getSelectedItemPosition()); localeCode = l.code; isRtl = l.rtl; getPreferences(MODE_PRIVATE).edit().putString("locale", localeCode).apply(); buildShell(); showTab("settings"); toast("تم تطبيق اتجاه اللغة"); }); content.addView(applyLang);
        gap(12); headingSmall("المظهر");
        Spinner theme = spinner(new String[]{"داكن مريح", "أسود كامل", "فاتح"}); theme.setSelection(themeMode); content.addView(theme);
        Button applyTheme = primaryButton("تطبيق المظهر"); applyTheme.setOnClickListener(v -> { themeMode = theme.getSelectedItemPosition(); getPreferences(MODE_PRIVATE).edit().putInt("theme", themeMode).apply(); buildShell(); showTab("settings"); }); content.addView(applyTheme);
        gap(12); headingSmall("الخصوصية والأمان");
        action("حذف رمز GitHub المحفوظ", () -> { SecureStore.remove(this, "github_token"); toast("تم حذف الرمز المحلي"); });
        action("حذف مفتاح AI المحفوظ", () -> { SecureStore.remove(this, "ai_key"); toast("تم حذف المفتاح المحلي"); });
        action("حذف مفتاح VirusTotal المحفوظ", () -> { SecureStore.remove(this, "vt_key"); toast("تم حذف المفتاح المحلي"); });
        mutedBlock("المفاتيح تحفظ باستخدام AES-GCM بمفتاح محفوظ في Android Keystore. لا تشارك ملفات المشروع أو السجلات التي قد تحتوي بيانات حساسة.");
        gap(12); headingSmall("حول XCode Studio"); mutedBlock("الإصدار 1.0.0 • XIO Core\nالمشاريع والملفات تحفظ في مساحة التطبيق الخاصة. البناء السحابي يتم عبر GitHub Actions بعد رفع المشروع وإعداد الرمز والصلاحيات.");
    }

    private void saveSecret(String name, String value, String message) {
        if (value == null || value.trim().isEmpty()) { toast("الحقل فارغ"); return; }
        try { SecureStore.put(this, name, value.trim()); toast(message); } catch (Exception ex) { toast("تعذر تشفير المفتاح: " + ex.getMessage()); }
    }

    private void background(String title, Work work, Done done) {
        setStatus(title + "…");
        io.execute(() -> {
            try { String result = work.run(); runOnUiThread(() -> { setStatus("اكتمل: " + title); if (done != null) done.accept(result); }); }
            catch (Exception ex) { runOnUiThread(() -> { setStatus("فشل: " + title); showMessage("حدث خطأ", ex.getMessage() == null ? ex.toString() : ex.getMessage()); }); }
        });
    }
    private interface Work { String run() throws Exception; }
    private interface Done { void accept(String value); }

    private void heading(String title, String description) {
        TextView t = new TextView(this); t.setText(title); t.setTextColor(c(TEXT, 0xFF101828)); t.setTextSize(22); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); content.addView(t);
        TextView d = muted(description); d.setPadding(0, dp(5), 0, dp(14)); content.addView(d);
    }
    private void headingSmall(String title) { TextView t = new TextView(this); t.setText(title); t.setTextColor(c(TEXT, 0xFF1F2937)); t.setTextSize(15); t.setTypeface(null, Typeface.BOLD); t.setPadding(0, dp(3), 0, dp(8)); content.addView(t); }
    private TextView muted(String text) { TextView t = new TextView(this); t.setText(text == null ? "" : text); t.setTextColor(MUTED); t.setTextSize(12); return t; }
    private void mutedBlock(String text) { TextView t = muted(text); t.setPadding(dp(12), dp(12), dp(12), dp(12)); t.setBackground(makeBg(c(PANEL, 0xFFFFFFFF), 10, c(PANEL2, 0xFFE1E5ED))); content.addView(t); gap(10); }
    private void emptyState(String title, String subtitle) { LinearLayout box = card(); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); TextView h = new TextView(this); h.setText(title); h.setTextColor(c(TEXT, 0xFF111827)); h.setTextSize(16); h.setTypeface(null, Typeface.BOLD); box.addView(h); TextView s = muted(subtitle); s.setGravity(Gravity.CENTER); s.setPadding(0, dp(8), 0, 0); box.addView(s); addCard(box); }
    private LinearLayout card() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(13), dp(12), dp(13), dp(12)); l.setBackground(makeBg(c(PANEL, 0xFFFFFFFF), 14, c(PANEL2, 0xFFE2E6EF))); return l; }
    private void addCard(View v) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(10); content.addView(v, p); }
    private void gap(int height) { View v = new View(this); content.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }
    private EditText field(String hint, int inputType) { EditText e = new EditText(this); e.setSingleLine(inputType != (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE)); e.setHint(hint); e.setTextColor(c(TEXT, 0xFF1F2937)); e.setHintTextColor(MUTED); e.setTextSize(13); e.setInputType(inputType); e.setPadding(dp(12), dp(8), dp(12), dp(8)); e.setBackground(makeBg(c(PANEL, 0xFFFFFFFF), 10, c(PANEL2, 0xFFD9DEE8))); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(9); e.setLayoutParams(p); return e; }
    private Spinner spinner(String[] items) { Spinner s = new Spinner(this); ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items); a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(a); s.setBackground(makeBg(c(PANEL, 0xFFFFFFFF), 10, c(PANEL2, 0xFFD9DEE8))); s.setPadding(dp(10), dp(5), dp(10), dp(5)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48)); p.bottomMargin = dp(9); s.setLayoutParams(p); return s; }
    private Button primaryButton(String label) { Button b = styleButton(label, ACCENT, 0xFF0B1020); return b; }
    private Button secondaryButton(String label) { return styleButton(label, c(PANEL2, 0xFFE8EBF2), c(TEXT, 0xFF202433)); }
    private Button smallButton(String label) { return styleButton(label, c(PANEL2, 0xFFEBEEF4), c(TEXT, 0xFF202433)); }
    private Button styleButton(String label, int bg, int fg) { Button b = new Button(this); b.setText(label); b.setTextColor(fg); b.setTextSize(12); b.setAllCaps(false); b.setMinHeight(0); b.setMinimumHeight(0); b.setPadding(dp(10), dp(5), dp(10), dp(5)); b.setBackground(makeBg(bg, 10, bg)); return b; }
    private GradientDrawable makeBg(int color, int radius, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), stroke); return d; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private void action(String label, Runnable action) { Button b = secondaryButton(label); b.setGravity(isRtl ? Gravity.RIGHT | Gravity.CENTER_VERTICAL : Gravity.LEFT | Gravity.CENTER_VERTICAL); b.setOnClickListener(v -> action.run()); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(44)); p.bottomMargin = dp(7); content.addView(b, p); }
    private void showMessage(String title, String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message == null ? "لا توجد تفاصيل" : message).setPositiveButton("إغلاق", null).show(); }
    private void toast(String value) { Toast.makeText(this, value == null ? "" : value, Toast.LENGTH_LONG).show(); }
    private void setStatus(String value) { if (statusLine != null) statusLine.setText(value); }
    private String join(List<String> list, String sep) { StringBuilder b = new StringBuilder(); for (String s : list) { if (b.length() > 0) b.append(sep); b.append("• ").append(s); } return b.toString(); }
}