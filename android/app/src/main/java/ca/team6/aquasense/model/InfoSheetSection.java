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

public class InfoSheetSection implements Serializable {
    private final int titleResourceId;
    private final int[] itemResourceIds;

    public InfoSheetSection(int titleResourceId, int[] itemResourceIds) {
        this.titleResourceId = titleResourceId;
        this.itemResourceIds = itemResourceIds;
    }

    public void applyTo(BottomSheetDialogFragment fragment, LinearLayout view) {
        TextView section = (TextView) fragment.getLayoutInflater().inflate(
                R.layout.sensor_info_section,
                view,
                false
        );

        section.setText(this.titleResourceId);
        view.addView(section);

        for (int itemResourceId : this.itemResourceIds) {
            View bullet = fragment.getLayoutInflater().inflate(
                    R.layout.sensor_info_bullet,
                    view,
                    false
            );

            TextView bulletText = bullet.findViewById(R.id.text);
            bulletText.setText(itemResourceId);

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
