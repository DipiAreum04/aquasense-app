package ca.team6.aquasense.model;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import ca.team6.aquasense.R;

/**
 * One titled block of the sensor info sheet: a heading and the bullets underneath it.
 *
 * <p>The text arrives already resolved rather than as string resource IDs, because a bullet
 * quotes the aquarium's configured thresholds and the sensor's latest reading, and neither is
 * known until the sheet is opened.
 */
public class InfoSheetSection implements Serializable {
    private final String title;
    private final String[] items;

    public InfoSheetSection(String title, String... items) {
        this.title = title;
        this.items = items;
    }

    public void applyTo(BottomSheetDialogFragment fragment, LinearLayout view) {
        TextView section = (TextView) fragment.getLayoutInflater().inflate(
                R.layout.sensor_info_section,
                view,
                false
        );

        section.setText(this.title);
        view.addView(section);

        for (String item : this.items) {
            View bullet = fragment.getLayoutInflater().inflate(
                    R.layout.sensor_info_bullet,
                    view,
                    false
            );

            TextView bulletText = bullet.findViewById(R.id.text);
            bulletText.setText(item);

            view.addView(bullet);
        }
    }

    public static byte[] serialize(Serializable object) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
        } catch (IOException e) {
            ScopedLogger.error("Serializing InfoSheetSection.");
        }
        return bos.toByteArray();
    }

    public static <T> T deserialize(
            byte[] bytes,
            Class<T> clazz
    ) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return clazz.cast(in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            ScopedLogger.error("Deserializing InfoSheetSection.");
        }
        return null;
    }
}
