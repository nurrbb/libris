package com.nurbb.libris.util;

public class LevelUtils {
    public static String determineLevel(int score) {
        if (score < 50) {
            return "Novice";
        } else if (score < 100) {
            return "Reader";
        } else if (score < 200) {
            return "Bookworm";
        } else {
            return "Bibliophile";
        }
    }

    public static int getMaxTotalBorrowDays(String level) {
        return switch (level) {
            case "Novice" -> 15;
            case "Reader" -> 30;
            case "Bookworm" -> 45;
            case "Bibliophile" -> 60;
            default -> 15;
        };
    }
}
