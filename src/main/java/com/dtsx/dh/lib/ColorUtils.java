package com.dtsx.dh.lib;

import lombok.val;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Help.Ansi.IStyle;
import picocli.CommandLine.Help.Ansi.Style;

import java.util.regex.Pattern;

import static java.util.regex.Matcher.quoteReplacement;
import static picocli.CommandLine.Help.Ansi.IStyle.CSI;

public class ColorUtils {
    private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("@!(.*?)!@");

    public static IStyle ACCENT_COLOR = new IStyle() {
        public String on() { return CSI + "38;5;110m"; }
        public String off() { return CSI + "39m"; }
    };

    public static String highlight(CharSequence s) {
        return color(ACCENT_COLOR, s);
    }

    public static String color(IStyle color, CharSequence s) {
        val off = (color == Style.bold)
            ? CSI + "22m" // explicitly using CSI+22m b/c PicoCLI tries to use CSI+21m to reset bold which doesn't work
            : color.off();

        return color.on() + s + off;
    }

    public static String format(String str) {
        str = HIGHLIGHT_PATTERN.matcher(str).replaceAll((match) -> quoteReplacement(highlight(match.group(1))));
        str = Ansi.AUTO.text(str).toString();
        return str;
    }

    public static String stripAnsi(String str) {
        return str.replaceAll("\\u001B\\[[;\\d]*m", "");
    }
}
