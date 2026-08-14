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
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.ReadingFormatter;
import ca.team6.aquasense.aquarium.ThresholdBand;
import ca.team6.aquasense.aquarium.WaterType;
import ca.team6.aquasense.aquarium.templates.AquariumTemplate;

public final class AquariumTemplateCardBinder {

    private static final DecimalFormat BOUND_FORMAT = new DecimalFormat("0.##");

    private static final int ACCENT_FILL_ALPHA = 30;

    private AquariumTemplateCardBinder() {}

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

        bindDetails(inflater, card, template, accent, accentFill);

        return card;
    }

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

        boolean fahrenheit = ReadingFormatter.isFahrenheit(context);

        LinearLayout parameters = root.findViewById(R.id.containerParameters);
        addParameter(inflater, parameters, template, context,
                DatabaseSchema.TEMPERATURE_KEY, fahrenheit);
        addParameter(inflater, parameters, template, context,
                DatabaseSchema.PH_LEVEL_KEY, fahrenheit);
        addParameter(inflater, parameters, template, context,
                DatabaseSchema.DISSOLVED_SOLIDS_KEY, fahrenheit);

        int disabledNoteResId = template.getDisabledNoteResId();
        if (disabledNoteResId != 0) {
            TextView note = root.findViewById(R.id.tvDisabledNote);
            note.setText(disabledNoteResId);
            note.setVisibility(View.VISIBLE);
        }
    }

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

        String unit = context.getString(
                ReadingFormatter.unitResIdFor(sensorId, fahrenheit)).trim();
        String sensorName = context.getString(ReadingFormatter.nameResIdFor(sensorId));
        String name = unit.isEmpty()
                ? sensorName
                : context.getString(R.string.template_parameter_with_unit, sensorName, unit);
        ((TextView) row.findViewById(R.id.tvParameterName)).setText(name);

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

    private static String format(String sensorId, double bound, boolean fahrenheit) {
        double displayValue = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? ReadingFormatter.toDisplayTemperature(bound, fahrenheit)
                : bound;
        return BOUND_FORMAT.format(displayValue);
    }
}
