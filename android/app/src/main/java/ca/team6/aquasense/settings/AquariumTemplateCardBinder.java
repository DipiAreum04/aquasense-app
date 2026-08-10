package ca.team6.aquasense.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.ImageViewCompat;

import java.text.DecimalFormat;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;

/**
 * Inflates the views that explain an {@link AquariumTemplate}
 */
public final class AquariumTemplateCardBinder {

    private static final DecimalFormat BOUND_FORMAT = new DecimalFormat("0.##");

    // Opacity of the pale background behind the icon tile and the badge, as a fraction of the template's
    // accent colour.
    private static final int ACCENT_FILL_ALPHA = 30;

    private AquariumTemplateCardBinder() {}

    /** Inflates and fills a card. The caller adds it to its own container. */
    public static View createCard(
            LayoutInflater inflater,
            ViewGroup parent,
            AquariumTemplate template
    ) {
        View card = inflater.inflate(R.layout.item_aquarium_template, parent, false);
        Context context = card.getContext();

        int accent = ContextCompat.getColor(context, template.getAccentColorResId());
        int accentFill = ColorUtils.setAlphaComponent(accent, ACCENT_FILL_ALPHA);

        bindIconTile(card.findViewById(R.id.ivTemplateIcon), template);

        TextView badge = card.findViewById(R.id.tvTemplateWaterType);
        badge.setText(waterTypeResId(template.getWaterType()));
        badge.setBackgroundResource(waterTypeBadgeResId(template.getWaterType()));

        ((TextView) card.findViewById(R.id.tvTemplateName)).setText(template.getNameResId());
        ((TextView) card.findViewById(R.id.tvTemplateDescription))
                .setText(template.getDescriptionResId());

        // The card includes the details block, so its views are found on the card itself.
        bindDetails(inflater, card, template, accent, accentFill);

        return card;
    }

    /**
     * Fills a template's icon tile.
     */
    public static void bindIconTile(ImageView tile, AquariumTemplate template) {
        bindIconTile(tile, template.getIconResId(), template.getAccentColorResId());
    }

    public static void bindIconTile(
            ImageView tile,
            @DrawableRes int iconResId,
            @ColorRes int accentColorResId
    ) {
        int accent = ContextCompat.getColor(tile.getContext(), accentColorResId);
        tile.setImageResource(iconResId);
        tile.getBackground().mutate()
                .setTint(ColorUtils.setAlphaComponent(accent, ACCENT_FILL_ALPHA));
    }

    /**
     * Inflates and fills the details block on its own, for a screen that already names the template
     * and only needs the species and threshold bars.
     */
    public static View createDetails(
            LayoutInflater inflater,
            ViewGroup parent,
            AquariumTemplate template
    ) {
        View details = inflater.inflate(R.layout.item_template_details, parent, false);

        int accent = ContextCompat.getColor(
                details.getContext(), template.getAccentColorResId());
        bindDetails(inflater, details, template, accent,
                ColorUtils.setAlphaComponent(accent, ACCENT_FILL_ALPHA));

        return details;
    }

    /**
     * Fills the species chips, band bars and disabled note.
     */
    private static void bindDetails(
            LayoutInflater inflater,
            View root,
            AquariumTemplate template,
            int accent,
            int accentFill
    ) {
        Context context = root.getContext();

        ImageViewCompat.setImageTintList(
                root.findViewById(R.id.ivThresholdsIcon), ColorStateList.valueOf(accent));

        addSpeciesChips(inflater, root.findViewById(R.id.containerSpecies),
                context.getResources().getStringArray(template.getExampleSpeciesResId()),
                accent, accentFill);

        // Read once for the card rather than per row: all three rows are drawn in the same unit,
        // and only the temperature one has two to choose between.
        boolean fahrenheit = ReadingFormatter.isFahrenheit(context);

        LinearLayout parameters = root.findViewById(R.id.containerParameters);
        addParameter(inflater, parameters, template, context,
                DatabaseSchema.TEMPERATURE_KEY, fahrenheit);
        addParameter(inflater, parameters, template, context,
                DatabaseSchema.PH_LEVEL_KEY, fahrenheit);
        addParameter(inflater, parameters, template, context,
                DatabaseSchema.DISSOLVED_SOLIDS_KEY, fahrenheit);

        // Only saltwater carries a note for TDS, so the view stays gone for the other three.
        int disabledNoteResId = template.getDisabledNoteResId();
        if (disabledNoteResId != 0) {
            TextView note = root.findViewById(R.id.tvDisabledNote);
            note.setText(disabledNoteResId);
            note.setVisibility(View.VISIBLE);
        }
    }

    /**
     * One chip per example species, wrapping onto as many lines as the card needs.
     */
    private static void addSpeciesChips(
            LayoutInflater inflater,
            ViewGroup container,
            String[] species,
            int accent,
            int accentFill
    ) {
        for (String name : species) {
            TextView chip = (TextView)
                    inflater.inflate(R.layout.item_species_chip, container, false);
            chip.setText(name);
            chip.setTextColor(accent);
            chip.getBackground().mutate().setTint(accentFill);
            container.addView(chip);
        }
    }

    @StringRes
    private static int waterTypeResId(WaterType waterType) {
        return waterType == WaterType.SALTWATER
                ? R.string.water_type_saltwater
                : R.string.water_type_freshwater;
    }

    @DrawableRes
    private static int waterTypeBadgeResId(WaterType waterType) {
        return waterType == WaterType.SALTWATER
                ? R.drawable.bg_water_badge_saltwater
                : R.drawable.bg_water_badge_freshwater;
    }

    /**
     * Appends one sensor's band bar, or nothing when the template has no range for it.
     *
     * @param fahrenheit whether the user reads the app in Fahrenheit, which only the temperature
     *     row acts on.
     */
    private static void addParameter(
            LayoutInflater inflater,
            LinearLayout parent,
            AquariumTemplate template,
            Context context,
            String sensorId,
            boolean fahrenheit
    ) {
        ThresholdBand bands = template.getThresholds(sensorId);
        if (bands == null) {
            return;
        }

        View row = inflater.inflate(R.layout.item_template_parameter, parent, false);

        // Name and unit both come from ReadingFormatter, so a template's rows are labelled exactly
        // as the dashboard labels the cards these thresholds go on to colour.
        String unit = context.getString(
                ReadingFormatter.unitResIdFor(sensorId, fahrenheit)).trim();
        String sensorName = context.getString(ReadingFormatter.nameResIdFor(sensorId));
        String name = unit.isEmpty()
                ? sensorName
                : context.getString(R.string.template_parameter_with_unit, sensorName, unit);
        ((TextView) row.findViewById(R.id.tvParameterName)).setText(name);

        // Deliberately the stored bounds, not the displayed ones. Fahrenheit scales every band by
        // the same 9/5, so the proportions the bar is drawn from are identical either way, and
        // converting first would only invite the offset to creep into a width.
        ThresholdBandBar.apply(row, bands);

        ((TextView) row.findViewById(R.id.tvBoundLow)).setText(
                context.getString(R.string.template_bound_below,
                        format(sensorId, bands.getWarnLow(), fahrenheit)));
        ((TextView) row.findViewById(R.id.tvBandSafe)).setText(
                context.getString(R.string.template_band_range,
                        format(sensorId, bands.getSafeLow(), fahrenheit),
                        format(sensorId, bands.getSafeHigh(), fahrenheit)));
        ((TextView) row.findViewById(R.id.tvBoundHigh)).setText(
                context.getString(R.string.template_bound_above,
                        format(sensorId, bands.getWarnHigh(), fahrenheit)));

        parent.addView(row);
    }

    /**
     * Renders one bound in the unit the user reads. Templates declare their temperatures in
     * Celsius, which is also how telemetry is stored, so the conversion belongs here at the
     * display edge and nowhere earlier.
     */
    private static String format(String sensorId, double bound, boolean fahrenheit) {
        double displayValue = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? ReadingFormatter.toDisplayTemperature(bound, fahrenheit)
                : bound;
        return BOUND_FORMAT.format(displayValue);
    }
}
