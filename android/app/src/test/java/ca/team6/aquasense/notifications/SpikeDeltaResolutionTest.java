package ca.team6.aquasense.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import ca.team6.aquasense.aquarium.Aquarium;
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.ThresholdBand;

public class SpikeDeltaResolutionTest {

    private static Aquarium aquariumWithDeltas(Map<String, Double> spikeDeltas) {
        return new Aquarium(
                "tank-1",
                "Tank 1",
                DatabaseSchema.WATER_TYPE_FRESHWATER,
                Collections.<String, ThresholdBand>emptyMap(),
                spikeDeltas);
    }

    @Test
    public void theStoredDeltaWinsOverTheBuiltInDefault() {
        Aquarium aquarium = aquariumWithDeltas(
                Collections.singletonMap(DatabaseSchema.TEMPERATURE_KEY, 0.25));

        assertEquals(
                0.25,
                SensorThresholds.resolveSpikeDelta(aquarium, DatabaseSchema.TEMPERATURE_KEY),
                0.0);
    }

    @Test
    public void eachSensorResolvesIndependently() {
        Aquarium aquarium = aquariumWithDeltas(
                Collections.singletonMap(DatabaseSchema.PH_LEVEL_KEY, 0.1));

        assertEquals(
                0.1,
                SensorThresholds.resolveSpikeDelta(aquarium, DatabaseSchema.PH_LEVEL_KEY),
                0.0);
        assertEquals(
                SensorThresholds.DEFAULT_TEMPERATURE_SPIKE_C,
                SensorThresholds.resolveSpikeDelta(aquarium, DatabaseSchema.TEMPERATURE_KEY),
                0.0);
    }

    @Test
    public void anAquariumWithNoStoredDeltaFallsBack() {
        Aquarium aquarium = aquariumWithDeltas(Collections.emptyMap());

        assertEquals(
                SensorThresholds.DEFAULT_TDS_SPIKE_PPM,
                SensorThresholds.resolveSpikeDelta(
                        aquarium, DatabaseSchema.DISSOLVED_SOLIDS_KEY),
                0.0);
    }

    @Test
    public void noAquariumAtAllFallsBack() {
        assertEquals(
                SensorThresholds.DEFAULT_PH_SPIKE,
                SensorThresholds.resolveSpikeDelta(null, DatabaseSchema.PH_LEVEL_KEY),
                0.0);
    }

    @Test
    public void aNonPositiveStoredDeltaIsIgnored() {
        Aquarium zero = aquariumWithDeltas(
                Collections.singletonMap(DatabaseSchema.TEMPERATURE_KEY, 0.0));
        Aquarium negative = aquariumWithDeltas(
                Collections.singletonMap(DatabaseSchema.TEMPERATURE_KEY, -1.0));

        assertEquals(
                SensorThresholds.DEFAULT_TEMPERATURE_SPIKE_C,
                SensorThresholds.resolveSpikeDelta(zero, DatabaseSchema.TEMPERATURE_KEY),
                0.0);
        assertEquals(
                SensorThresholds.DEFAULT_TEMPERATURE_SPIKE_C,
                SensorThresholds.resolveSpikeDelta(negative, DatabaseSchema.TEMPERATURE_KEY),
                0.0);
    }

    @Test
    public void aSensorThatCannotSpikeResolvesToNaN() {
        assertTrue(Double.isNaN(SensorThresholds.resolveSpikeDelta(
                aquariumWithDeltas(Collections.emptyMap()),
                DatabaseSchema.WATER_LEVEL_KEY)));
    }
}
