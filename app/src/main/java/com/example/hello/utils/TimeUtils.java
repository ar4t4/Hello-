package com.example.hello.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    
    /**
     * Convert timestamp to "time ago" format (e.g. "2 hours ago", "5 days ago")
     */
    public static String getTimeAgo(long timestamp) {
        Date now = new Date();
        long diff = now.getTime() - timestamp;
        
        if (diff < 0) {
            return "just now";
        }
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        
        if (seconds < 60) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
        } else if (days < 365) {
            long months = days / 30;
            return months + " month" + (months > 1 ? "s" : "") + " ago";
        } else {
            long years = days / 365;
            return years + " year" + (years > 1 ? "s" : "") + " ago";
        }
    }
} 