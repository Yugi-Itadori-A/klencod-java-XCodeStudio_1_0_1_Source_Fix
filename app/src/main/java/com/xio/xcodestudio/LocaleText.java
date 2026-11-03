package com.xio.xcodestudio;

import java.util.HashMap;
import java.util.Map;

/** Core UI translations for the 16 selected interface locales. */
public final class LocaleText {
    private static final String[] CODES = {"ar","en","fr","de","es","pt","ru","tr","zh","ja","ko","hi","ur","fa","id","bn"};
    private static final Map<String, String[]> WORDS = new HashMap<>();
    static {
        add("projects", "المشاريع", "Projects", "Projets", "Projekte", "Proyectos", "Projetos", "Проекты", "Projeler", "项目", "プロジェクト", "프로젝트", "परियोजनाएँ", "منصوبے", "پروژه‌ها", "Proyek", "প্রকল্প");
        add("editor", "المحرر", "Editor", "Éditeur", "Editor", "Editor", "Editor", "Редактор", "Düzenleyici", "编辑器", "エディター", "편집기", "संपादक", "ایڈیٹر", "ویرایشگر", "Editor", "এডিটর");
        add("github", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub", "GitHub");
        add("ai", "مساعد AI", "AI Assistant", "Assistant IA", "KI-Assistent", "Asistente IA", "Assistente IA", "ИИ-помощник", "YZ Asistanı", "AI 助手", "AI アシスタント", "AI 도우미", "AI सहायक", "AI اسسٹنٹ", "دستیار هوش مصنوعی", "Asisten AI", "AI সহকারী");
        add("security", "الفحص الأمني", "Security scan", "Analyse sécurité", "Sicherheitsprüfung", "Análisis de seguridad", "Análise de segurança", "Проверка безопасности", "Güvenlik taraması", "安全扫描", "セキュリティ検査", "보안 검사", "सुरक्षा जाँच", "سیکیورٹی اسکین", "بررسی امنیتی", "Pemindaian keamanan", "নিরাপত্তা স্ক্যান");
        add("extensions", "الإضافات", "Extensions", "Extensions", "Erweiterungen", "Extensiones", "Extensões", "Расширения", "Eklentiler", "扩展", "拡張機能", "확장", "एक्सटेंशन", "ایکسٹینشنز", "افزونه‌ها", "Ekstensi", "এক্সটেনশন");
        add("settings", "الإعدادات", "Settings", "Paramètres", "Einstellungen", "Ajustes", "Configurações", "Настройки", "Ayarlar", "设置", "設定", "설정", "सेटिंग्स", "ترتیبات", "تنظیمات", "Pengaturan", "সেটিংস");
        add("new_project", "مشروع جديد", "New project", "Nouveau projet", "Neues Projekt", "Nuevo proyecto", "Novo projeto", "Новый проект", "Yeni proje", "新建项目", "新しいプロジェクト", "새 프로젝트", "नया प्रोजेक्ट", "نیا پروجیکٹ", "پروژه جدید", "Proyek baru", "নতুন প্রকল্প");
        add("import_zip", "استيراد ZIP", "Import ZIP", "Importer ZIP", "ZIP importieren", "Importar ZIP", "Importar ZIP", "Импорт ZIP", "ZIP içe aktar", "导入 ZIP", "ZIP を読み込む", "ZIP 가져오기", "ZIP आयात करें", "ZIP درآمد کریں", "درون‌ریزی ZIP", "Impor ZIP", "ZIP আমদানি");
        add("open", "فتح", "Open", "Ouvrir", "Öffnen", "Abrir", "Abrir", "Открыть", "Aç", "打开", "開く", "열기", "खोलें", "کھولیں", "باز کردن", "Buka", "খুলুন");
        add("save", "حفظ", "Save", "Enregistrer", "Speichern", "Guardar", "Salvar", "Сохранить", "Kaydet", "保存", "保存", "저장", "सहेजें", "محفوظ کریں", "ذخیره", "Simpan", "সংরক্ষণ");
        add("cancel", "إلغاء", "Cancel", "Annuler", "Abbrechen", "Cancelar", "Cancelar", "Отмена", "İptal", "取消", "キャンセル", "취소", "रद्द करें", "منسوخ", "لغو", "Batal", "বাতিল");
        add("delete", "حذف", "Delete", "Supprimer", "Löschen", "Eliminar", "Excluir", "Удалить", "Sil", "删除", "削除", "삭제", "हटाएँ", "حذف", "حذف", "Hapus", "মুছুন");
        add("rename", "إعادة تسمية", "Rename", "Renommer", "Umbenennen", "Renombrar", "Renomear", "Переименовать", "Yeniden adlandır", "重命名", "名前を変更", "이름 변경", "नाम बदलें", "نام بدلیں", "تغییر نام", "Ubah nama", "নাম পরিবর্তন");
        add("create", "إنشاء", "Create", "Créer", "Erstellen", "Crear", "Criar", "Создать", "Oluştur", "创建", "作成", "만들기", "बनाएँ", "بنائیں", "ایجاد", "Buat", "তৈরি");
        add("name", "الاسم", "Name", "Nom", "Name", "Nombre", "Nome", "Имя", "Ad", "名称", "名前", "이름", "नाम", "نام", "نام", "Nama", "নাম");
        add("template", "القالب", "Template", "Modèle", "Vorlage", "Plantilla", "Modelo", "Шаблон", "Şablon", "模板", "テンプレート", "템플릿", "टेम्पलेट", "سانچہ", "قالب", "Templat", "টেমপ্লেট");
        add("no_projects", "لا توجد مشاريع بعد. أنشئ مشروعًا أو استورد ملف ZIP.", "No projects yet. Create one or import a ZIP archive.", "Aucun projet. Créez-en un ou importez une archive ZIP.", "Noch keine Projekte. Erstellen Sie eines oder importieren Sie ein ZIP-Archiv.", "Aún no hay proyectos. Crea uno o importa un ZIP.", "Ainda não há projetos. Crie um ou importe um ZIP.", "Пока нет проектов. Создайте проект или импортируйте ZIP.", "Henüz proje yok. Bir proje oluşturun veya ZIP içe aktarın.", "还没有项目。请创建项目或导入 ZIP。", "プロジェクトはまだありません。作成するか ZIP を読み込んでください。", "프로젝트가 없습니다. 새로 만들거나 ZIP을 가져오세요.", "अभी कोई प्रोजेक्ट नहीं है। नया बनाएँ या ZIP आयात करें।", "ابھی کوئی پروجیکٹ نہیں۔ نیا بنائیں یا ZIP درآمد کریں۔", "هنوز پروژه‌ای نیست. یک پروژه بسازید یا ZIP وارد کنید.", "Belum ada proyek. Buat proyek atau impor ZIP.", "এখনও কোনো প্রকল্প নেই। নতুন তৈরি করুন বা ZIP আমদানি করুন।");
        add("api_key", "مفتاح API", "API key", "Clé API", "API-Schlüssel", "Clave API", "Chave de API", "Ключ API", "API anahtarı", "API 密钥", "API キー", "API 키", "API कुंजी", "API کلید", "کلید API", "Kunci API", "API কী");
    }
    private LocaleText() {}
    private static void add(String key, String... values) { WORDS.put(key, values); }
    public static String get(String code, String key) {
        String[] values = WORDS.get(key); if (values == null) return key;
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return values[i];
        return values[1];
    }
}
