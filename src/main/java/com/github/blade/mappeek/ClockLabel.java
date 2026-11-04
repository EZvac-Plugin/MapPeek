package com.github.blade.mappeek;

import com.github.blade.hvac.api.SeasonView;

import java.util.Locale;
import java.util.Optional;

/**
 * A world's time of day and season as the map shows them (0.2.0):
 * {@code 14:30 · Summer, day 3 of 15}, on a 24-hour clock with sunrise at
 * 06:00, as EZvac's HUD shows it. Pure.
 */
public final class ClockLabel {
    private ClockLabel() {}

    /** {@code 06:00} at sunrise (tick 0), {@code 00:00} at midnight. */
    public static String clock(long dayTime) {
        long ticks = Math.floorMod(dayTime + 6_000L, 24_000L);
        long hour = ticks / 1_000L;
        long minute = (ticks % 1_000L) * 60L / 1_000L;
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    /** {@code Summer, day 3 of 15}; empty where the world has no seasons. */
    public static String season(Optional<SeasonView> season) {
        return season.map(view -> name(view.season()) + ", day " + view.dayOfSeason() + " of "
                + view.daysPerSeason() + (view.paused() ? " (paused)" : "")).orElse("");
    }

    /** The whole label: the clock, then the season where there is one. */
    public static String label(long dayTime, String season) {
        return season == null || season.isEmpty() ? clock(dayTime) : clock(dayTime) + " · " + season;
    }

    private static String name(SeasonView.Season season) {
        String lower = season.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
