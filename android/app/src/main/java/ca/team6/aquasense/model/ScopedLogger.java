package ca.team6.aquasense.model;

import android.util.Log;

public class ScopedLogger {
    private ScopedLogger() {}

    private static String getTag() {
        // SOURCE: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/StackWalker.html
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stream -> stream
                        .skip(1)
                        .findFirst()
                        .map(stackFrame -> stackFrame.getDeclaringClass().getSimpleName()+"::"+stackFrame.getMethodName()+"()")
                        .orElse(null)
                );
    }

    // TODO: REMOVE IF ACTUALLY UNUSED BY END OF SPRINT 3
    @SuppressWarnings("unused")
    public static synchronized void debug(String message) {
        String tag = getTag();
        if (tag == null) return;

        Log.d(tag, message);
    }

    // TODO: REMOVE IF ACTUALLY UNUSED BY END OF SPRINT 3
    @SuppressWarnings("unused")
    public static synchronized void info(String message) {
        String tag = getTag();
        if (tag == null) return;

        Log.i(tag, message);
    }

    // TODO: REMOVE IF ACTUALLY UNUSED BY END OF SPRINT 3
    @SuppressWarnings("unused")
    public static synchronized void warn(String message) {
        String tag = getTag();
        if (tag == null) return;

        Log.w(tag, message);
    }

    public static synchronized void error(String message) {
        String tag = getTag();
        if (tag == null) return;

        Log.e(tag, message);
    }
}
