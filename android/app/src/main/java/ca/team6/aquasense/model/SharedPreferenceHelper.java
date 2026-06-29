package ca.team6.aquasense.model;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceHelper {
    private static SharedPreferenceHelper instance;
    private static SharedPreferences sharedPreferences;

    private SharedPreferenceHelper() {}
    
    private SharedPreferenceHelper(Context context) {
        Context appContext = context.getApplicationContext();
        sharedPreferences = appContext.getSharedPreferences("ENGR390-SUMMER2026-TEAM6", Context.MODE_PRIVATE);
    }

    public static synchronized SharedPreferenceHelper getInstance(Context context) {
        if (context == null) return null;
        
        if (instance == null) {
            instance = new SharedPreferenceHelper(context);
        }

        return instance;
    }
    
    private void remove(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }
}
