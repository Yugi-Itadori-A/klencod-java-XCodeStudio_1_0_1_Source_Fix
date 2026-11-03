package com.xio.xcodestudio;

import java.util.Arrays;
import java.util.List;

public final class LanguageCatalog {
    public static final class Language {
        public final String name, code;
        public final boolean rtl;
        Language(String name, String code, boolean rtl) { this.name = name; this.code = code; this.rtl = rtl; }
        @Override public String toString() { return name; }
    }
    private static final List<Language> ITEMS = Arrays.asList(
        new Language("العربية", "ar", true), new Language("English", "en", false), new Language("Français", "fr", false),
        new Language("Deutsch", "de", false), new Language("Español", "es", false), new Language("Português", "pt", false),
        new Language("Русский", "ru", false), new Language("Türkçe", "tr", false), new Language("中文（简体）", "zh", false),
        new Language("日本語", "ja", false), new Language("한국어", "ko", false), new Language("हिन्दी", "hi", false),
        new Language("اردو", "ur", true), new Language("فارسی", "fa", true), new Language("Bahasa Indonesia", "id", false),
        new Language("বাংলা", "bn", false)
    );
    private LanguageCatalog() {}
    public static List<Language> all() { return ITEMS; }
    public static Language byCode(String code) { for (Language l : ITEMS) if (l.code.equals(code)) return l; return ITEMS.get(0); }
}
