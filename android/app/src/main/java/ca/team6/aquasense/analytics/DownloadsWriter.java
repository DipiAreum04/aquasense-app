package ca.team6.aquasense.analytics;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import ca.team6.aquasense.logging.ScopedLogger;

public final class DownloadsWriter {

    public interface SaveCallback {
        void onSaved();

        void onError();
    }

    private DownloadsWriter() {}

    public static void saveText(@NonNull Context context,
                                @NonNull String fileName,
                                @NonNull String mimeType,
                                @NonNull String text,
                                @NonNull SaveCallback callback) {
        Context appContext = context.getApplicationContext();
        Handler mainThread = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            boolean saved = write(appContext, fileName, mimeType, text);
            mainThread.post(() -> {
                if (saved) {
                    callback.onSaved();
                } else {
                    callback.onError();
                }
            });
        }, "DownloadsWriter").start();
    }

    private static boolean write(@NonNull Context context,
                                 @NonNull String fileName,
                                 @NonNull String mimeType,
                                 @NonNull String text) {
        ContentResolver resolver = context.getContentResolver();

        ContentValues pending = new ContentValues();
        pending.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        pending.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        pending.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        pending.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri item = resolver.insert(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), pending);
        if (item == null) {
            ScopedLogger.error("MediaStore refused a Downloads entry for " + fileName);
            return false;
        }

        try (OutputStream out = resolver.openOutputStream(item)) {
            if (out == null) {
                throw new IOException("No output stream for " + item);
            }
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | SecurityException exception) {
            ScopedLogger.error("Could not write " + fileName + ": " + exception.getMessage());
            resolver.delete(item, null, null);
            return false;
        }

        ContentValues published = new ContentValues();
        published.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(item, published, null, null);
        return true;
    }
}
