package ca.team6.aquasense.model;

import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Shared validator class for profile/contact form fields.
 */
public final class ProfileInputValidator {

    public static final int MAX_LENGTH = 100;

    // Name may contain letters, spaces, hyphens, or apostrophes.
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^\\p{L}+([ '\\-]\\p{L}+)*$");

    private ProfileInputValidator() {}

    // Name validation method for profile/edit profile fragment
    // Name must not be empty and must not exceed 100 characters
    public static boolean isInvalidName(@Nullable String name) {
        if (TextUtils.isEmpty(name)) {
            return true;
        }
        String trimmed = name.trim();
        return trimmed.length() > MAX_LENGTH || !NAME_PATTERN.matcher(trimmed).matches();
    }

    // Optional name validation method for contact form
    // Empty name is OK; otherwise must pass validation check
    public static boolean isInvalidOptionalName(@Nullable String name) {
        if (TextUtils.isEmpty(name) || name.trim().isEmpty()) {
            return false;
        }
        return isInvalidName(name);
    }

    // Email validation method
    // Email must contain an @ symbol, max 100 characters, and must be a valid email address
    public static boolean isInvalidEmail(@Nullable String email) {
        if (TextUtils.isEmpty(email)) {
            return true;
        }
        String trimmed = email.trim();
        return trimmed.length() > MAX_LENGTH
                || !trimmed.contains("@")
                || !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches();
    }
}
