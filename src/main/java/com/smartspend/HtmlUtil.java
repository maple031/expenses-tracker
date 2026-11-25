package com.smartspend;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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

    public static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        DecimalFormatSymbols symbols =
                DecimalFormatSymbols.getInstance(Locale.of("en", "IN"));
        DecimalFormat format = new DecimalFormat("#,##,##0.00", symbols);
        format.setGroupingUsed(true);
        return format.format(amount);
    }
}
