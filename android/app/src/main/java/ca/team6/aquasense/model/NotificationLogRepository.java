package ca.team6.aquasense.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

public class NotificationLogRepository {
    private static final String KEY_NOTIFICATION_LOG = "notificationLog";
    private static final String KEY_NOTIFICATION_LOG_MIGRATED = "notificationLogMigratedToLocalStore";
    private static final String DATABASE_NAME = "notification_history.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE = "notification_history";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Set<HistoryObserver> OBSERVERS = new CopyOnWriteArraySet<>();

    private final SharedPreferenceHelper prefs;
    private final LocalStore store;
    @Nullable
    private final String userId;

    public NotificationLogRepository(Context context) {
        Context appContext = context.getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : null;
        prefs = SharedPreferenceHelper.getInstance(appContext);
        store = new LocalStore(appContext);
        discardUnscopedLegacyHistory();
    }

    public List<NotificationLogEntry> loadPage(
            int page, int pageSize, String aquariumId, SensorType sensorType) {
        if (userId == null) return new ArrayList<>();

        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, pageSize);
        Selection selection = Selection.forFilters(userId, aquariumId, sensorType);
        List<NotificationLogEntry> entries = new ArrayList<>();

        try (Cursor cursor = store.getReadableDatabase().query(
                TABLE,
                new String[]{"id", "aquarium_id", "sensor_type", "trigger", "severity", "timestamp"},
                selection.clause,
                selection.args,
                null,
                null,
                "timestamp DESC, id DESC",
                safePageSize + " OFFSET " + ((long) safePage * safePageSize))) {
            while (cursor.moveToNext()) {
                try {
                    entries.add(new NotificationLogEntry(
                            cursor.getLong(0),
                            cursor.getString(1),
                            SensorType.valueOf(cursor.getString(2)),
                            NotificationTrigger.valueOf(cursor.getString(3)),
                            SensorStatus.valueOf(cursor.getString(4)),
                            cursor.getLong(5)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return entries;
    }

    public long countEntries(String aquariumId, SensorType sensorType) {
        if (userId == null) return 0;

        Selection selection = Selection.forFilters(userId, aquariumId, sensorType);
        try (Cursor cursor = store.getReadableDatabase().query(
                TABLE, new String[]{"COUNT(*)"}, selection.clause, selection.args,
                null, null, null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    public void addEntry(NotificationLogEntry entry) {
        if (userId == null) return;

        store.getWritableDatabase().insertOrThrow(TABLE, null, valuesFor(userId, entry));
        notifyObservers();
    }

    public void deleteEntry(long localId) {
        if (localId < 0 || userId == null) return;
        store.getWritableDatabase().delete(
                TABLE,
                "id = ? AND user_id = ?",
                new String[]{Long.toString(localId), userId});
    }

    public void clearAll() {
        if (userId == null) return;
        store.getWritableDatabase().delete(TABLE, "user_id = ?", new String[]{userId});
    }

    public void addObserver(HistoryObserver observer) {
        OBSERVERS.add(observer);
    }

    public void removeObserver(HistoryObserver observer) {
        OBSERVERS.remove(observer);
    }

    private static void notifyObservers() {
        for (HistoryObserver observer : OBSERVERS) {
            MAIN_HANDLER.post(() -> {
                if (OBSERVERS.contains(observer)) observer.onHistoryChanged();
            });
        }
    }

    public interface HistoryObserver {
        void onHistoryChanged();
    }

    private void discardUnscopedLegacyHistory() {
        if (prefs == null || prefs.getBoolean(KEY_NOTIFICATION_LOG_MIGRATED, false)) return;

        prefs.removeSync(KEY_NOTIFICATION_LOG);
        prefs.setBooleanSync(KEY_NOTIFICATION_LOG_MIGRATED, true);
    }

    private static ContentValues valuesFor(String userId, NotificationLogEntry entry) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("aquarium_id", entry.aquariumId);
        values.put("sensor_type", entry.sensorType.name());
        values.put("trigger", entry.trigger.name());
        values.put("severity", entry.severity.name());
        values.put("timestamp", entry.timestamp);
        return values;
    }

    private static final class Selection {
        final String clause;
        final String[] args;

        private Selection(String clause, String[] args) {
            this.clause = clause;
            this.args = args;
        }

        static Selection forFilters(String userId, String aquariumId, SensorType sensorType) {
            List<String> clauses = new ArrayList<>();
            List<String> args = new ArrayList<>();
            clauses.add("user_id = ?");
            args.add(userId);
            if (aquariumId != null) {
                clauses.add("aquarium_id = ?");
                args.add(aquariumId);
            }
            if (sensorType != null) {
                clauses.add("sensor_type = ?");
                args.add(sensorType.name());
            }
            return new Selection(
                    clauses.isEmpty() ? null : String.join(" AND ", clauses),
                    args.isEmpty() ? null : args.toArray(new String[0]));
        }
    }

    private static final class LocalStore extends SQLiteOpenHelper {
        LocalStore(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id TEXT NOT NULL,"
                    + "aquarium_id TEXT NOT NULL,"
                    + "sensor_type TEXT NOT NULL,"
                    + "trigger TEXT NOT NULL,"
                    + "severity TEXT NOT NULL,"
                    + "timestamp INTEGER NOT NULL)");
            createIndexes(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN user_id TEXT");
                db.delete(TABLE, null, null);
                db.execSQL("DROP INDEX IF EXISTS notification_history_timestamp_idx");
                db.execSQL("DROP INDEX IF EXISTS notification_history_aquarium_sensor_idx");
                createIndexes(db);
            }
        }

        private static void createIndexes(SQLiteDatabase db) {
            db.execSQL("CREATE INDEX notification_history_user_timestamp_idx ON " + TABLE
                    + " (user_id, timestamp DESC)");
            db.execSQL("CREATE INDEX notification_history_user_aquarium_sensor_idx ON " + TABLE
                    + " (user_id, aquarium_id, sensor_type, timestamp DESC)");
        }
    }
}
