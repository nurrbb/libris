package com.nurbb.libris.util;

import com.nurbb.libris.model.entity.valueobject.Level;

/**
 * Utility class for determining user level and borrow duration rules based on score.
 */

public class LevelUtils {
    public static Level determineLevel(int score) {
        if (score < 50) {
            return Level.NOVICE;
        } else if (score < 100) {
            return Level.READER;
        } else if (score < 200) {
            return Level.BOOKWORM;
        } else {
            return Level.BIBLIOPHILE;
        }
    }

    /**
     * Returns the maximum total borrowable days for the given user level.
     */

    public static int getMaxTotalBorrowDays(Level level) {
        return switch (level) {
            case NOVICE -> 15;
            case READER -> 30;
            case BOOKWORM -> 45;
            case BIBLIOPHILE -> 60;
        };
    }

    /**
     * Returns the default borrow period (in days) for a new borrow based on user level.
     */

    public static int getDefaultBorrowDays(Level level) {
        return switch (level) {
            case NOVICE -> 5;
            case READER -> 7;
            case BOOKWORM -> 10;
            case BIBLIOPHILE -> 14;
        };
    }
}
