# Walkthrough: Corrected Temperature Calibration Image Mapping

I have updated the Temperature sensor calibration guide to use your specific PNG images in the correct sequence. This ensures the visual context matches the instructions perfectly, especially for the new branching logic step.

## Changes Made

### 1. Temperature Guide Image Update
- **[SensorCalibrationFragment.java](file:///C:/Users/Dom%20Reynolds-Sandy/engr390-software/android/app/src/main/java/ca/team6/aquasense/settings/SensorCalibrationFragment.java):** Corrected the resource mapping for the 4-step Temperature guide:
    - **Step 1 (Reference):** `twotemps.png`
    - **Step 2 (Installation):** `holdingtemp.png`
    - **Step 3 (Testing):** `tempsincup.png`
    - **Step 4 (Result/Deviation):** Reused `tempsincup.png` to maintain context during the match check.

## Verification Results

### Automated Verification
- Successfully performed build to confirm resource mapping.

### Manual Verification Steps
1. Open **Settings -> Sensor Calibration**.
2. Launch the **Temperature Sensor** guide.
3. Swipe through the steps and verify the images appear in this order:
    - **Step 1:** Two thermometers.
    - **Step 2:** Hand holding/installing the sensor.
    - **Step 3:** Sensors in a cup of water.
    - **Step 4:** Sensors in a cup of water (while asking if readings match).
