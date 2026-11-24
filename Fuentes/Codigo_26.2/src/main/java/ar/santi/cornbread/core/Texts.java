package ar.santi.cornbread.core;

/** Mod-specific language, independent of Minecraft's global language setting. */
public final class Texts {
    private static volatile String language = "es";
    private Texts() {}
    public static void setLanguage(String value) { language = "en".equals(value) ? "en" : "es"; }
    public static String language() { return language; }
    public static boolean isEnglish() { return "en".equals(language); }
    public static String t(String spanish, String english) { return isEnglish() ? english : spanish; }
}
