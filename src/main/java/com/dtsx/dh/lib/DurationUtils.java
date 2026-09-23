package com.dtsx.dh.lib;

import lombok.val;

import java.time.Duration;

/// Human-readable rendering of elapsed times.
public class DurationUtils {
    /// Formats a duration the way a multi-minute client run deserves: seconds alone under a
    /// minute, minutes and seconds up to an hour, hours and minutes beyond that.
    public static String formatDuration(Duration d) {
        val totalSeconds = d.toSeconds();

        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }

        if (totalSeconds < 3600) {
            val minutes = totalSeconds / 60;
            val seconds = totalSeconds % 60;
            return minutes + "m " + String.format("%02d", seconds) + "s";
        }

        val hours = totalSeconds / 3600;
        val minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + String.format("%02d", minutes) + "m";
    }
}
