package com.smartspend;

public final class HtmlUtil {
    private HtmlUtil() {}

    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&#34;")
                .replace("'", "&#39;");
    }
}
