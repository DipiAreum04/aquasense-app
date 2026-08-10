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

import ca.team6.aquasense.model.ScopedLogger;

/**
 * Writes a file into the device's Downloads folder.
 *
 * <p>Through MediaStore rather than to a path. Since Android 10 the app cannot write into shared
 * storage directly and has no permission to ask for that would let it: it inserts a row into the
 * Downloads collection, is handed a stream for it, and the file appears where the user's Downloads
 * app can see it. That also means there is nothing to request at runtime, which is why this screen
 * asks for no permission before saving.
 *
 * <p>The row is inserted pending and only published once the bytes are down, so a save that fails
 * part way leaves no half-written file in the folder for someone to open.
 */
public final class DownloadsWriter {

    /** Called on the main thread, whichever way the write went. */
    public interface SaveCallback {
        void onSaved();

        void onError();
    }

    private DownloadsWriter() {}

    /**
     * Saves text as a file in Downloads, off the main thread.
     *
     * <p>A period's JSON is only a few kilobytes, but the write is a content-provider call and a
     * file system write, and neither is something to do on the thread drawing the graph.
     *
     * @param fileName the name to save under, extension included. MediaStore resolves a collision
     *                 by numbering the new file rather than overwriting the old one, so a second
     *                 save of the same window keeps the first.
     */
    public static void saveText(@NonNull Context context,
                                @NonNull String fileName,
                                @NonNull String mimeType,
                                @NonNull String text,
                                @NonNull SaveCallback callback) {
        // The context outlives the screen that asked, so a save still lands if the user leaves
        // while it is in flight - and the callback below only touches the caller's own state.
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
        // Hides the row until the bytes are written; see the class comment.
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
            // The row exists and is still pending, so it is invisible to the user but not gone.
            // Dropping it is what stops a failed save accumulating dead entries in the collection.
            resolver.delete(item, null, null);
            return false;
        }

        ContentValues published = new ContentValues();
        published.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(item, published, null, null);
        return true;
    }
}
