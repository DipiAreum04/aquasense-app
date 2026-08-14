# Implementation Plan: Corrected Temperature Image Mapping

Update the Temperature Sensor calibration guide with the specific image sequence provided by the user, ensuring the visual continuity for the new branching logic steps.

## User Review Required

> [!NOTE]
> I am mapping your 3 PNG files to the 4-step Temperature guide. Steps 3 and 4 (the physical test and the result check) will both display the `tempsincup.png` visual to maintain focus while the user records any deviations.

## Proposed Changes

### Visual Assets Integration

#### [MODIFY] [SensorCalibrationFragment.java](file:///C:/Users/Dom%20Reynolds-Sandy/engr390-software/android/app/src/main/java/ca/team6/aquasense/settings/SensorCalibrationFragment.java)
- **Temperature Guide:**
    - Step 1: `R.drawable.twotemps`
    - Step 2: `R.drawable.holdingtemp`
    - Step 3: `R.drawable.tempsincup`
    - Step 4 (Match Check): `R.drawable.tempsincup`

---

## Verification Plan

### Manual Verification
1. Open **Settings -> Sensor Calibration**.
2. Launch the **Temperature Sensor** guide.
3. Swipe through all 4 steps:
    - Verify Step 1 shows the two thermometers.
    - Verify Step 2 shows the sensor being held/installed.
    - Verify Step 3 shows the sensors in the cup.
    - Verify Step 4 (The Match Check) also shows the sensors in the cup.
4. Click "No" on Step 4 and verify the image remains while input fields appear.
