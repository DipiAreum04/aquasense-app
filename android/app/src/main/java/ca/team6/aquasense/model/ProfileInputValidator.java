package ca.team6.aquasense.model;

import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.regex.Pattern;

import ca.team6.aquasense.R;

/**
 * Shared validator class for profile/contact form fields.
 */
public final class ProfileInputValidator {

    public static final int MAX_LENGTH = 100;

    // Name may contain letters, spaces, hyphens, or apostrophes.
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^\\p{L}+([ '\\-]\\p{L}+)*$");

    private static final Pattern AQUARIUM_NAME_PATTERN =
            Pattern.compile("^[\\p{L}\\p{N}]+([ '\\-][\\p{L}\\p{N}]+)*$");

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

    public static boolean isInvalidAquariumName(@Nullable String name) {
        if (TextUtils.isEmpty(name)) {
            return true;
        }
        String trimmed = name.trim();
        return trimmed.length() > MAX_LENGTH || !AQUARIUM_NAME_PATTERN.matcher(trimmed).matches();
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

    // Minimum password length is 6 characters (Firebase Auth requirement)
    // Maximum password length is 100 characters
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 100;

    private static final Pattern PASSWORD_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_LOWER = Pattern.compile("[a-z]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[^A-Za-z0-9]");


    // Returns a string resource for the first failed password rule, or 0 if valid.
    // Password must be 6–100 characters and include at least
    // one uppercase letter, one lowercase letter, one digit, and one special character.
    @StringRes
    public static int getPasswordErrorResId(@Nullable String password) {
        if (TextUtils.isEmpty(password)) {
            return R.string.auth_password_required;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return R.string.auth_password_too_short;
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return R.string.auth_password_too_long;
        }
        if (!PASSWORD_UPPER.matcher(password).find()) {
            return R.string.auth_password_needs_upper;
        }
        if (!PASSWORD_LOWER.matcher(password).find()) {
            return R.string.auth_password_needs_lower;
        }
        if (!PASSWORD_DIGIT.matcher(password).find()) {
            return R.string.auth_password_needs_number;
        }
        if (!PASSWORD_SPECIAL.matcher(password).find()) {
            return R.string.auth_password_needs_special;
        }
        return 0;
    }

    // Confirm password must match the password field
    public static boolean passwordsDoNotMatch(@Nullable String password, @Nullable String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return true;
        }
        return !password.equals(confirmPassword);
    }
}
