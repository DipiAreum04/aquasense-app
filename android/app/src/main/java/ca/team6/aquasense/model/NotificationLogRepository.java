package ca.team6.aquasense.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

/** Stores notification history only on this device and reads it in bounded pages. */
public class NotificationLogRepository {
    private static final String KEY_NOTIFICATION_LOG = "notificationLog";
    private static final String KEY_NOTIFICATION_LOG_MIGRATED = "notificationLogMigratedToLocalStore";
    private static final String DATABASE_NAME = "notification_history.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE = "notification_history";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Set<HistoryObserver> OBSERVERS = new CopyOnWriteArraySet<>();

    private final SharedPreferenceHelper prefs;
    private final LocalStore store;

    public NotificationLogRepository(Context context) {
        Context appContext = context.getApplicationContext();
        prefs = SharedPreferenceHelper.getInstance(appContext);
        store = new LocalStore(appContext);
        migrateLegacyHistory();
    }

    /** Returns one newest-first page. Neither this method nor the UI loads the full history. */
    public List<NotificationLogEntry> loadPage(
            int page, int pageSize, String aquariumId, SensorType sensorType) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, pageSize);
        Selection selection = Selection.forFilters(aquariumId, sensorType);
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
                    // Ignore records from an app version with values this build does not know.
                }
            }
        }
        return entries;
    }

    public long countEntries(String aquariumId, SensorType sensorType) {
        Selection selection = Selection.forFilters(aquariumId, sensorType);
        try (Cursor cursor = store.getReadableDatabase().query(
                TABLE, new String[]{"COUNT(*)"}, selection.clause, selection.args,
                null, null, null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    /** Appends without deleting older entries; history has no application-defined maximum. */
    public void addEntry(NotificationLogEntry entry) {
        store.getWritableDatabase().insertOrThrow(TABLE, null, valuesFor(entry));
        notifyObservers();
    }

    public void deleteEntry(long localId) {
        if (localId < 0) return;
        store.getWritableDatabase().delete(
                TABLE, "id = ?", new String[]{Long.toString(localId)});
    }

    /** Deletes the complete device-local history, independent of active screen filters. */
    public void clearAll() {
        store.getWritableDatabase().delete(TABLE, null, null);
    }

    /** Observes successful history inserts from this app process on the main thread. */
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

    private void migrateLegacyHistory() {
        if (prefs == null || prefs.getBoolean(KEY_NOTIFICATION_LOG_MIGRATED, false)) return;

        String json = prefs.getString(KEY_NOTIFICATION_LOG, "");
        SQLiteDatabase db = store.getWritableDatabase();
        boolean migrationFinished = false;
        db.beginTransaction();
        try {
            if (!json.isEmpty()) {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject obj = array.getJSONObject(i);
                        String aquariumId = obj.optString(
                                "aquariumId", obj.optString("aquariumHardwareAddress", ""));
                        NotificationLogEntry entry = new NotificationLogEntry(
                                aquariumId,
                                SensorType.valueOf(obj.getString("sensorType")),
                                NotificationTrigger.valueOf(obj.getString("trigger")),
                                SensorStatus.valueOf(obj.getString("severity")),
                                obj.getLong("timestamp"));
                        db.insertOrThrow(TABLE, null, valuesFor(entry));
                    } catch (JSONException | IllegalArgumentException ignored) {
                        // Preserve every readable legacy entry even if another is malformed.
                    }
                }
            }
            db.setTransactionSuccessful();
            migrationFinished = true;
        } catch (JSONException ignored) {
            // A malformed top-level legacy value has no recoverable entries; do not retry it.
            migrationFinished = true;
        } finally {
            db.endTransaction();
        }
        if (migrationFinished) {
            // Mark it only after the local transaction commits, so a crash cannot lose history.
            prefs.setBooleanSync(KEY_NOTIFICATION_LOG_MIGRATED, true);
            prefs.removeSync(KEY_NOTIFICATION_LOG);
        }
    }

    private static ContentValues valuesFor(NotificationLogEntry entry) {
        ContentValues values = new ContentValues();
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

        static Selection forFilters(String aquariumId, SensorType sensorType) {
            List<String> clauses = new ArrayList<>();
            List<String> args = new ArrayList<>();
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
                    + "aquarium_id TEXT NOT NULL,"
                    + "sensor_type TEXT NOT NULL,"
                    + "trigger TEXT NOT NULL,"
                    + "severity TEXT NOT NULL,"
                    + "timestamp INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX notification_history_timestamp_idx ON " + TABLE
                    + " (timestamp DESC)");
            db.execSQL("CREATE INDEX notification_history_aquarium_sensor_idx ON " + TABLE
                    + " (aquarium_id, sensor_type, timestamp DESC)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // No schema upgrades yet.
        }
    }
}
