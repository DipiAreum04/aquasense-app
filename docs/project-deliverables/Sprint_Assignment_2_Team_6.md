# Scrum Assignment II — Team 6

COEN/ELEC 390 

## Abstract

Maintaining an aquarium ecosystem requires tracking of water parameters, a process traditionally done on physical logbooks. This project introduces an automated monitoring system designed to track an aquarium’s key vitals. The integrated solution features a live status dashboard and an alert system that instantly notifies users when vitals deviate from safe, predefined thresholds. 

This report covers Scrum Assignment II. It updates the Product Backlog and defines Sprint 2 objectives: completing Sprint 1 carry-over work, notification management (history and snooze), multi-aquarium support, hardware pairing/templates, Firebase integration with login/registration, historical sensor trends, and an at-a-glance aquarium status summary.

## Contents

- List of Figures
- List of Tables
- List of Terms
- 1. Introduction
- 2. Requirements (Version 2)
- 3. Design (Version 2)
- 4. Sprints (Version 2)
- 5. Test Documents (Version 2)
- 6. Definition of Done (DoD) Checklist Validation

|5.7. Test Plan 7: Sensor Reading Tooltips . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 43|
|---|
|5.7.1. Summary . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 43|
|5.7.2. Test Cases . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 43|
|5.8. Test Plan 8: Settings Page . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 44|
|5.8.1. Summary . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 44|
|5.9. Test Plan 9: Contact Page . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 48|
|5.9.1. Summary . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 48|
|5.9.2. Test Cases . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 49|
|**6. Definition of Done (DoD) Checklist Validation . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 51**|
|6.1. Definition of Done Criteria . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 51|

## List of Figures

*[Figure list cleaned; original figures may appear as placeholders where OCR dumps were removed.]*

## List of Tables

- **Table 1:** Product Backlog (User Story level)
- **Table 2:** Sprint 1 Backlog (Task level)
- **Table 4:** Sprint 2 Backlog (Task level)
- Test plan and Definition of Done tables (Sections 5–6)

|**Table 35 Test cases for ST-2.3 . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 50**|
|---|
|**Table 36 Test cases for ST-2.3 . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 50**|
|**Table 37 Test cases for ST-2.3 . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 50**|
|**Table 38 COM-01. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 51**|
|**Table 39 SENSOR-01. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 52**|
|**Table 40 SENSOR-02. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 52**|
|**Table 41 SENSOR-03. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 53**|
|**Table 42 PRIVSEC-01. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 53**|
|**Table 43 DASH-01. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 54**|
|**Table 44 DASH-02. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 54**|
|**Table 45 SAFETY-01. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 55**|
|**Table 46 SETTINGS-01. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 55**|
|**Table 47 SETTINGS-02. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 55**|

## List of Terms 

|**HW**|Hardware|
|---|---|
|**SW**|Software|
|**UI**|User Interface|
|**ID**|Identifier|
|**PRIO**|Priority|
|**TBD**|To Be Declared|

## 1. Introduction 

Maintaining an aquarium requires tracking of critical water parameters to ensure the health of fish and other aquatic life. Traditionally, aquarium owners rely on manual testing and physical logbooks to keep track of data. This project introduces an automated solution that streamlines the monitoring process by sending sensor data directly to a user’s device. Additionally, the system features an automated alert system that instantly notifies users if any parameter deviates from safe thresholds, giving users time to mediate any issues. 

### 1.1. Sprint 2 Goal 

The goal of sprint 2 is to focus on delivering application usability. We plan on developing a multiple aquarium ecosystem, where users could manage multiple aquariums at the same time. In addition, the dashboard will be expanded by including a deep history log of past notifications so users can review data about their respective aquariums. We also plan on completing all of the user stories and tasks carried over to sprint 2 from sprint 1 including sensor timestamping, sensor-offline detection, enclosure printing, and threshold/status notifications. 

## 2. Requirements (Version 2) 

### 2.1. Product Backlog 

| Story ID | Story Title | Card | Story Points | Sprint | Status | Priority | Conversation | Confirmation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SENSOR-02 | Data Collection Evaluation | As a user, I would like to know that my sensors are relaying the correct and up to date information to my application, so that I can have the most accurate data in a timely manner. | 8 | Sprint 1 & Sprint 2 | PUSHED (Partially Completed) | High | Write a script that relays all necessary sensor information and flags any anomalies (sudden spikes or depressions during data collection, stale data). | 1. Does the script flag any sudden anomalies within the data? 2. Is there a timestamp tracking mechanism to ensure real-time data is being properly relayed? |
| SAFETY-01 | Creating Waterproofing Compartments for the Sensors | As a user, I want the sensors and hub assembled in a safe, water-resistant enclosure that mounts inside or around my aquarium, so that I get accurate readings without damaging my equipment or aquatic animals. | 8 | Sprint 1 & Sprint 2 | PUSHED (Partially Completed) | High | Obtain the dimensions of the sensors and design potential waterproof holder for 3D printing. | 1. Does the sensor fit inside the designed holder? 2. Can the holder independently attach to the Aquarium or does it need supports (ex. Tape)? 3. Is the enclosure water-resistant and safely mountable? 4. Has the 3D-printed housing been fit-tested? |
| NOTIF-01 | Sensor Notifications | As a user, I want the option to receive customizable threshold-triggered and status-triggered notifications, so that I can attend to the aquarium if it demands my attention. | 5 | Sprint 2 | PUSHED | High | Make threshold customization possible through the dashboard. Threshold alerts are useless if a dead sensor silently stops reporting so dashboard should visually flag stale readings (e.g., greyed out with a "last seen" timestamp). Timeout duration before a sensor is deemed as offline could be configurable. | 1. Can the user change threshold conditions? 2. Does the user receive notifications when threshold conditions are met? 3. Does the user receive notifications when sensor status changes? 4. Does a disconnected hub trigger a notification? 5. Does the dashboard flag stale readings? 6. Is the offline timeout configurable? |
| SENSOR-04 | Sensor Detection Defect | As a user, I want to be notified when my sensors are not connecting to my application, so that I can address the issue immediately. | 5 | Sprint 2 | PUSHED | High | Create a script that monitors the active connections between sensor and application, while giving push-notifications when the connection is lost. | 1. Does the application trigger an immediate system alert when a sensor connection fails? |
| NOTIF-02 | Maintenance Mode / Snooze Alerts | As a user, I want to temporarily pause notifications per aquarium, so that expected parameter swings don't spam me. | 3 | Sprint 2 | TODO | High | Toggle from the dashboard or aquarium header. Should auto-resume after a selectable duration (15m, 30m, 1h, 2h) so users can't forget alert snoozes. Dashboard should clearly show when maintenance mode is active. | 1. Can the user enable maintenance mode per aquarium? 2. Are notifications suppressed while active? 3. Does it auto-resume after the selected duration? 4. Is the active state clearly visible? |
| NOTIF-03 | Notification History | As a user, I want an in-app log of past alerts, so that I can review notifications I missed or dismissed. | 3 | Sprint 2 | TODO | High | List all sent notifications (threshold, offline, time-triggered) with timestamp, sensor, and threshold level/color. Filterable per aquarium and per sensor. Retention could follow the sensor history settings. | 1. Are all notification types logged? 2. Can the log be filtered by aquarium and sensor? 3. Do entries show timestamp and trigger reason? |
| FIREBASE-01 | Firebase Database & Hardware/App Integration | As a user, I want my aquarium's sensor data to be reliably captured, structured, and synced in real time between my hardware and my app, so that I always see accurate, up-to-date readings. | 8 | Sprint 2 | TODO | High | Design the Firebase database schema (aquariums, sensors, readings, users). Connect the hardware device to Firebase to read and periodically write/update sensor values. Build reusable Android wrapper/helper functions to fetch and listen to sensor values from Firebase for use across the app. | 1. Is there a clearly defined database schema? 2. Does the hardware write sensor readings to Firebase on a periodic basis? 3. Can the app fetch/listen to live sensor values via the helper functions? 4. Do the hardware and app now interface end-to-end through Firebase? |
| FIREBASE-02 | Login / Registration | As a user, I want to create an account and log in securely, so that my aquarium data, settings, and history are saved to my profile and only accessible by me. | 5 | Sprint 2 | TODO | High | Set up Firebase Authentication (email/password). Build registration and login screens, handle validation errors, and restrict app access within an authenticated session. | 1. Can a user register a new account? 2. Can a user log in with valid credentials? 3. Are invalid login/registration attempts handled with clear error messages? 4. Is the app inaccessible without a logged-in session? |
| SETTINGS-03 | Hardware Setup & Pairing Wizard | As a user, I want a guided flow in app to connect my hardware and create my first aquarium, so that I can get running without reading docs. | 5 | Sprint 2 | TODO | High | Launch automatically on first install since no aquariums are defined yet. Walks the user through entering the aquarium "address", verifying the connection, and naming the aquarium. | 1. Does the wizard launch on first install? 2. Can a user pair hardware and create an aquarium end-to-end? 3. Are connection errors communicated clearly? |
| SETTINGS-04 | Default Aquarium Template | As a user, I want the option to select an aquarium definition template (beginner, intermediate, expert) so that the amount of things I need to customize before I can start are suited to my level. | 5 | Sprint 2 | TODO | High | Define 3 built-in predefined aquarium templates: beginner, intermediate and expert, with the beginner template containing 2 or 3 parameters and the expert template containing all of the water parameters. Upon default template selection, the settings configured by the template should be potentially greyed out (finalize in team meetings), and the user should be able to configure the rest of the settings. | 1. Can users see the 3 built-in aquarium templates? 2. Can users select the aquarium templates when adding a new aquarium? 3. Does selecting an aquarium template, automatically configure the settings included in the template? 4. Are the users able to configure the extra settings? |
| DASH-04 | Multiple Aquarium Instances | As a user, I want the option to define aquariums and switch between them, so that I can have multiple aquarium systems interface with my app. | 8 | Sprint 2 | TODO | High | Should be selectable from the header bar of the app. The aquarium "address" will be used to connect the app to the hardware. First-install should start with no aquariums defined. Advanced settings for defining aquariums should be collapsed unless the user explicitly wants them. When user wants to define a new aquarium, the user should be given 4 options: use one of the three built-in templates or custom (define all settings themselves). | 1. Can users add and configure an aquarium? 2. Can users add multiple aquariums? 3. Can users select between aquariums? 4. Are settings and reports of the selected aquarium respected? 5. When adding an aquarium, do the users see 4 options to select from: beginner, intermediate, expert and custom? |
| SETTINGS-05 | Hardware Troubleshooting Guide | As a user, I want an in-app hardware troubleshooting guide, so that I can fix connection or sensor issues myself before contacting support. | 3 | Sprint 2 | TODO | Medium | Should be accessible from the navigation menu and linked from offline-sensor notifications and the contact page. Write a simple, concise, step-by-step guide for common connection and sensor faults. Polish after the enclosure (SAFETY-01) is finalized. | 1. Is the guide reachable from the nav menu and from offline alerts? 2. Does it cover the common connection/sensor faults? 3. Is it concise and easy to follow? |
| AESTHETICS-01 | App Icon & Loading Screen Design | As a user, I want a polished app icon and an engaging loading screen when I launch the app, so that I get a professional first impression before reaching the dashboard. | 3 | Sprint 2 | TODO | Low | Design an app icon reflecting the aquarium monitoring theme as a drawable/vector and export it in required resolutions. Design a loading screen with a transition animation effect that plays on launch before transitioning into the main dashboard for visual aesthetics. | 1. Is the app icon displayed correctly across required device resolutions? 2. Does the loading screen appear on every app launch? 3. Does the loading screen-to-dashboard transition play smooth and seamlessly? |
| DASH-05 | Aquarium Overall Status Summary | As a user, I want a clickable at-a-glance overall status for my aquarium, so that I can quickly tell if everything is fine or if something needs my attention. | 5 | Sprint 2 | TODO | High | Add an overall status indicator on the dashboard with written status (e.g. Normal / Warning / Critical) with a short written summary. Tapping it should show which sensor(s) are abnormal and their current values. The overall status indicator can be implemented with emojis or weather indicators, but further discussion on this matter is required with the team. | 1. Does the dashboard show an overall status summary? 2. Does the status reflect the worst current sensor condition? 3. Is the summary clickable? 4. Does clicking it list the abnormal sensor(s) with their values? |
| DASH-03 | Historical Sensor Data Retention | As a user, I want the option to enable and access per-sensor historical monitoring data of my system, so that I can study trends and diagnose complicated problems. | 8 | Sprint 2 | TODO | High | Should be accessible from the dashboard by clicking on the sensor. Can be shown in the form of a graph with selectable 1d, 7d, 1m, 3m, 6m, 1y. In settings, users can enable this and define how much data to retain and for which sensors. Should show an average reading value associated with the selectable time window. Should highlight spurs on graphs if feasible. Should be able to download sensor history as a plaintext report. | 1. Can a user enable history per-sensor? 2. Can users define a retention time window? 3. Is the retention time window respected? 4. Does the sensor retain data when enabled? 5. Does the graph show up for sensors with enabled history? 6. Does nothing show up for sensors with disabled history? |
| SETTINGS-06 | First-Launch App Walkthrough | As a user, I want a short guided walkthrough of the app's features the first time I install it, so that I understand how to use the app itself. | 3 | Sprint 3 | TODO | Medium | Launch automatically only on the first install, before the registration/login page. Highlight core app features (dashboard, notifications, settings) with tooltips or a short slide/carousel. Should be dismissible and not re-triggered on subsequent launches, with an option to replay it from Settings. | 1. Does the walkthrough launch on first install, after the hardware pairing wizard? 2. Does it cover the core app features clearly? 3. Can it be dismissed and skipped? 4. Does it avoid re-triggering on later launches? 5. Can the user replay it from Settings? |
| PRIVSEC-02 | Sensor Calibration Workflow | As a user, I want to be able to perform sensor calibration directly, with clear guided steps, so that my readings stay accurate and trustworthy over time. | 3 | Sprint 3 | TODO | Low | Implement an actual calibration action per sensor type, which can be triggerable from an in-app button and/or a physical button on the hardware hub, not just instructional text. Pair the action with step-by-step on-screen guidance and a configurable reminder period. Last-calibrated date should show in the sensor detail view. | 1. Can a user trigger calibration for a sensor from the app? 2. Can calibration also be triggered from a physical button on the hardware? 3. Is the user guided step-by-step during calibration? 4. Can reminder periods be configured? 5. Is the last calibration date displayed? |
| PRIVSEC-03 | Encrypt All Data In-Transit & At-Rest | As a user, I want all my data to be encrypted at rest and in transit, so that I can rest easy. | 5 | Sprint 3 | TODO | Low | Encrypt data in transit by forcing TLS/HTTPS on both the ESP32 -> Firebase and app -> Firebase links. Rely on Firebase's default AES-256 at-rest encryption and harden Security Rules so only the owner can access their data. | 1. Is data encrypted at rest? 2. Is data encrypted in transit? |
| NOTIF-04 | Date & Time Notifications | As a user, I want the option to receive customizable time-triggered notifications, so that I can stay on top of my periodic aquarium-related tasks. | 3 | Sprint 3 | TODO | Low | Implement alarm-setting features so that the user can set customized alarms, related tasks and implement time-triggered alerts or notifications. Alarms should appear as push notifications with sounds. Users should be able to snooze or dismiss the alarms. | 1. Can the user define multiple time-triggered notifications? 2. Do all time-triggered notifications for all sensors trigger successfully? |
| NOTIF-05 | Notify Emails | As a user, I want to be able to add emails to be sent notifications, so that they can tend to my aquarium if needed. | 3 | Sprint 3 | TODO | High | Add the feature for users to be able to sign up for email notification alerts. For signed-up users, send notifications to their email. Send notifications in a batched manner to avoid excessive emails. | 1. Can emails be added to the list? 2. Can emails be removed from the list? 3. Do all triggered notifications get sent by email? 4. Is the batch size / email frequency configurable? |
| SETTINGS-07 | Customizable Threshold Levels | As a user, I want the option to define threshold levels and actions associated with them, so that I can receive more complicated and meaningful responses. | 3 | Sprint 3 | TODO | Medium | Implement options for users to define customized threshold levels for each of the sensor parameters. Implement options for the users to flag thresholds as critical or moderate or stable. | 1. Can users define multiple threshold levels for every sensor? 2. Are threshold levels respected in that their threshold-triggered notifications are pushed? |
| DASH-06 | Compare Two Aquarium Histories | As a user, I want the option to compare my different aquariums, so that I can glean insights from my different setups for improvement. | 5 | Sprint 3 | TODO | Low | Implement aquarium comparison in the app using charts, tables, graphs, etc with side by side simple parameter comparison. Requires both the additions/removals functionality and the history functionality. | 1. Can users compare two aquariums? 2. Does it show up in a UX friendly manner? |
| DASH-07 | Link Google Account & Backup Data | As a user, I want the option to back up my sensor history to my Google account, so that I do not lose my data if my phone dies. | 8 | Sprint 3 | TODO | Low | Should be enabled from settings after linking a Google account. Selectable per sensor. Should run automatically and periodically, but never on mobile data. Backup should respect each sensor history retention window. | 1. Can users link a Google account? 2. Can backup be enabled per sensor? 3. Does backup run automatically and periodically? 4. Is backup skipped on mobile data? 5. Is the backup period configurable? |
| DASH-08 | Events Log | As a user, I want to log additions to and removals from my aquarium ecosystem, so that I can correlate events with sensor changes and diagnose issues. | 5 | Sprint 3 | TODO | Low | Implement a UX-friendly form (buttons, not a raw log window) to record events. Logged events should integrate onto the history graphs as markers so users can correlate them with readings. | 1. Can users log additions? 2. Can users log removals? 3. Do logged events appear on the history graphs? |
| COM-01 | Selection and Research of Additional Sensors | As a user, I want to easily add individual, secondary sensors to my existing app profile, so that I can increase my monitoring setup beyond the factory preset configuration. | 3 | Sprint 1 | COMPLETED | High | Research secondary sensors, aside from the ones initially collected; ensuring that they are also compatible with the current developer board. | 1. Is a list of approved secondary sensors with technical specifications and compatibility with developer board created? 2. Are the extra sensors chosen and purchased? |
| SENSOR-01 | Circuit Design & Simulation | As a user, I want the sensor circuitry to be wired correctly and validated through simulation before physical assembly, so that hardware faults are caught early and don't damage components or produce inaccurate readings. | 3 | Sprint 1 | COMPLETED | High | Design the circuit wiring layout connecting each sensor to the ESP32, then validate it in Falstad's online circuit simulator before physical assembly. | 1. Does the wiring diagram correctly map every sensor pin to a valid ESP32 pin without conflicts? 2. Does the Falstad simulation run without errors before physical wiring begins? |
| SENSOR-03 | Sensor to Cloud Connectivity | As a user, I want to be able to relay my data onto my application and save it, so that I can use it for future referencing when needed. | 5 | Sprint 1 | COMPLETED | High | Configure communication between the sensor and chosen cloud-based application ensuring real-time data is being relayed and saved. | 1. Is the sensor readings, saving correctly into the cloud database? 2. Can we see previous readings from the cloud database in our application? |
| PRIVSEC-01 | Privacy Page | As a user, I want none of my data to be shared with anyone, so that I can rest easy. | 3 | Sprint 1 | COMPLETED | High | A privacy activity should be clearly accessible and should explain to the user what data is collected and how it is used. | 1. Is the privacy activity easy to find? 2. Is it concise? |
| DASH-01 | Live Status Dashboard | As a user, I want to access a dashboard showing the live statuses and readings of all sensors, so that I can stay informed at all times on how my system is doing. | 8 | Sprint 1 | COMPLETED | High | Design dashboard layout, including an aquarium selector with connection status, the sensor grid body, and a navigation bar. Update live using real-time sensor data from Firebase. | 1. Does the dashboard have an aquarium selector with status? 2. Does the dashboard have a live sensor grid with indicators? 3. Does the dashboard have a navigation bar? |
| DASH-02 | Sensor Reading Tooltips | As a user, I want to be able to view descriptions of what sensor readings mean in the dashboard, so that I can better understand what I'm seeing. | 3 | Sprint 1 | COMPLETED | High | Create an (i) tooltip button that, when clicked, displays a tooltip message explaining how to interpret the reading. | 1. Does clicking the (i) show the right tooltip? 2. Are all tooltips intuitive? |
| SETTINGS-01 | Settings Page | As a user, I want to have a dedicated settings page, so that I can manage my profile, security preferences, and notification options, privacy page, contact support page etc. in one centralized location | 5 | Sprint 1 | COMPLETED | High | Design and implement the Settings view UI layout. Integrate navigation hooks from the dashboard. Implement toggle switches for notification preferences (e.g., enabling/disabling push alerts). Integrate links to Contact, notification, security, and privacy pages. | 1. Can the user access the settings page from the navigation? 2. Can the Privacy page be accessed? 3. Can the Contact Support page be accessed? 4. Can the Tutorial page be accessed? 5. Can the Security page be accessed? |
| SETTINGS-02 | Contact Page | As a user, I want the option to contact support, so that I can rest easy if I face issues with the app and/or the hardware. | 3 | Sprint 1 | COMPLETED | High | Implement a simple contact form that sends an email to our organization's support team. The user should be able to select between reporting an issue or giving feedback. | 1. Does the contact form send both issues and feedback? 2. Are they received by us? 3. Does the user get a “sent” toast? 4. Does the page have a dedicated email or description box to write in? |

Table 1: Product Backlog expressed at User Story level. 

## 3. Design (Version 2) 

This section presents the design of the application at this stage. The industry-standard tool Figma was used to assist in coming up with the app sketches. 

*[Wireframe and architecture figures were lost in PDF→Markdown conversion. See the original Scrum Assignment II PDF for Figures 1–17.]*

### 3.4.2. Component Functionality & Data Interface

**Digital Temperature Sensor Probe**

**Data Output:** Transmits a continuous, factory-calibrated digital floating-point value in degrees Celsius (°C) over a 1-Wire protocol, requiring an external 4.7 k-Ohm pull-up resistor to the 3.3V logic rail. 

- **Analog Total Dissolved Solids (TDS) Sensor Probe** 

**Function:** Measures fluid electrical conductivity by applying a small alternating current (AC) across two submerged electrodes to evaluate overall water purity. 

**Data Output:** Passes a variable, raw analog voltage stream scaled safely between 0V and 2.4V representing physical water resistance directly to the microcontroller’s internal Analog-to-Digital Converter (ADC) input pin which operates up to a native 3.3V maximum threshold. 

- **Analog pH Sensor Probe Kit** 

**Function:** Quantifies the hydrogen-ion activity in the water using a glass bulb electrode and an operational-amplifier conditioning module to track the acidity or alkalinity of the aquatic ecosystem on a scale from 0 to 14. 

**Data Output:** Emits a variable analog voltage stream derived from a minute electrical potential signal, routed through its dedicated signal-conditioning module directly to one of the microcontroller’s native 3.3V ADC input pins. 

### 3.4.3. Inter-Component Signal Chain & Data Flow 

Rather than operating in isolation, the hardware sensors form an interconnected data pipeline where individual outputs directly condition the calculation matrices of subsequent components. 

1. **The Level Sensor to Microcontroller Chain :** The liquid level sensor acts as a hardwarelevel safety switch for the remaining active components. When it drops to a LOW state (0V), the microcontroller reads this input and immediately executes an automated safety override: it stops sampling data from the submerged TDS and pH electrodes. This prevents the chemical probes from firing in dry air, protecting the hardware elements from permanent scoring and electrical damage. 

2. **The Temperature Sensor to TDS Probe Chain :** Fluid electrical conductivity fluctuates significantly based on thermal dynamics, introducing an average error of 2% per °C. The digital temperature sensor resolves this problem by feeding its live Celsius float data directly into the mathematical function of the TDS sensor. The microcontroller captures the temperature value and calculates a dynamic compensation coefficient, matching the raw incoming analog voltage to the current thermal profile of the tank. 

3. **The Temperature Sensor to pH Probe Chain :** The electrochemical properties of a pH glass electrode are temperature-dependent, as defined by the Nernst equation. As water temperature rises or falls, the millivolt output generated by the pH sensor changes for the exact same chemical acidity level. The microcontroller combines the data streams by passing the digital temperature value into the pH conversion algorithm, automatically adjusting the slope calculation to yield a perfectly stable, temperature-compensated pH metric. 

4. **The Analog Probe to Microcontroller Conversion Core:** The final stage of the hardware architecture process converts the raw electrical footprints of the water quality sensors into clean data. The microcontroller captures the incoming voltage from both the TDS driver board and the pH conditioning module. By running these inputs through its internal Analog-to-Digital Converter (ADC) alongside the active temperature calibration variables, it transforms raw electricity into high-fidelity parts-per-million (PPM) and pH index values, which are then passed forward to the software visualization layers. 

### 3.5. Database Architecture 

Figure 15 depicts the hierarchical diagram for the Firebase cloud architecture. The root of the structure is the database URL that the board/application uses to connect to the Firebase Realtime Database. Below the URL are application nodes that hold child fields for sensor readings and device status (stored as ints, strings, or floats).

*[Firebase schema figure was lost in PDF→Markdown conversion. See the original PDF, or `database/schema.json` in this repo.]*

## 4. Sprints (Version 2)

### 4.1. Sprint 1 Backlog (Completed)

## Sprint 1 Goals

1. Establish the hardware-to-cloud data foundation: research and buy sensors, read live data from the ESP32, and relay and store it in Firebase in real time. (COM-01, SENSOR-01, SENSOR-02, SENSOR-03)

2. Build a basic dashboard UI with a navigation bar that displays live sensor readings and statuses. (DASH-01, DASH-02)

3. Develop a basic alerting system that notifies users when readings cross safe thresholds or a sensor stops reporting. (NOTIF-01, SENSOR-04)

4. Design and 3D-print a safe, water-resistant enclosure to mount the sensors on the aquarium. (SAFETY-01)

5. Implement the essential supporting pages: privacy, contact, and settings. (PRIVSEC-01, SETTINGS-01, SETTINGS-02)

| Story ID | Task ID | Task Title | Task Description | Ideal Hours | Actual Hours | Status | Comments | Assignee(s) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| COM-01 | CM-1.1 | Research extra sensors not provided by uni | Search for extra sensors not listed in the uni-provided hardware list that are typically associated with aquarium regulation and create a list. | 2 | 2 | DONE | Ensure the additional sensors are compatible with our development board. | ALL |
| COM-01 | CM-1.2 | Purchase Extra Materials | Discuss and purchase any extra materials not provided by the university. | 3 | 10 | DONE | Ensure the purchased sensors are within an appropriate price range. | ARMAAN |
| SENSOR-01 | SR-1.1 | Circuit Wiring Design | Design the circuit wiring layout connecting each sensor to the ESP32 microcontroller, including power, ground, and signal/data lines. | 3 | 12 | DONE | Confirm pin assignments against the ESP32 pinout and each sensor's datasheet to avoid conflicts before any physical wiring. | ARMAAN |
| SENSOR-01 | SR-1.2 | Circuit Simulation in Falstad | Build the wired circuit in Falstad's online circuit simulator to validate the design before physical assembly. | 3 | 3 | DONE | Check wiring or component errors, note any simulator limitations for digital/I2C sensors and use an equivalent representation where needed. | ARMAAN |
| SENSOR-02 | SR-2.1 | Sensor baseline data parsing firmware | Write a core firmware script to read raw analog and digital inputs from the physical sensors plugged into the ESP32 microcontroller | 4 | 12 | DONE | Ensure data parameters are printed cleanly to the serial monitor without connection drops | ARMAAN & DOM |
| SENSOR-02 | SR-2.2 | Data anomaly tracking logic | Develop threshold checking loops in firmware to flag sudden parameter spikes or drops instantly | 3 | 4 | DONE | Flag data as "stale" or "anomalous" immediately if transmission halts mid-stream. | DOM |
| SENSOR-02 | SR-2.3 | Real-time parameter streaming pipeline | Configure the ESP32 Wi-Fi module to transmit sensor data packets. | 6 | 7 | DONE | Verify data packets broadcast successfully over the local network without losing packets. | DOM |
| SENSOR-02 | SR-2.4 | Hardware time-stamping setup | Implement a local timestamping protocol on the hardware node to track data transmission intervals. | 6 | 0 | PUSH | Ensure the mobile application can fetch historical and live records smoothly from the cloud database. | ARMAAN |
| SENSOR-03 | SR-3.1 | Firebase database schema configuration | Setup the real-time database JSON tree structure within Firebase to store inbound hardware parameters. | 4 | 2 | DONE | Confirm incoming data values append securely to the corresponding cloud data tables. | NAV |
| SENSOR-03 | SR-3.2 | Inbound cloud telemetry verification | Verify continuous communication between the active physical hardware node and the Firebase cloud server. | 4 | 3 | DONE | Ensure the mobile application can fetch historical and live records smoothly from the cloud database. | NAV & DOM |
| SENSOR-04 | SR-4.1 | Application system alert hooks | Create background logic hooks in the app to listen for real-time Firebase threshold breaches. | 6 | 0 | PUSH | Trigger an immediate local system push-notification if a threshold failure state is observed. | ARMAAN |
| SENSOR-04 | SR-4.2 | Connection heartbeat listener | Implement a network heartbeat function that checks if the hardware hub has stopped transmitting. | 4 | 0 | PUSH | Issue an intrusive critical warning notification if the hub connection falls completely silent. | ARMAAN |
| SAFETY-01 | SF-1.1 | Sensor Measurements | Obtain the dimensions of each sensor and determine an appropriate tolerance. | 2 | 5 | DONE | Measure the physical dimensions of each sensor and account for a small tolerance so they can be securely mounted onto the breadboard and around the aquarium | ARMAAN |
| SAFETY-01 | SF-1.2 | Determine placements | Find the best placements for the sensors/holders | 2 | 4 | DONE | Verify that the chosen spacing provides enough clearance for wiring and prevents interference between components | NAV |
| SAFETY-01 | SF-1.3 | Design schematics for the Sensor Holder | Design schematics for the Sensor Holder | 4 | 6 | DONE | Ensure the design accounts for proper cable management, and an easy installation (keeping in mind dimensions and placement). The design should be similar to a shelf placed against the aquarium, holding the sensor on top of the shelf. | NAV |
| SAFETY-01 | SF-1.4 | 3D CAD software | Create schematic in a 3D CAD software | 6 | 6 | DONE | Develop a 3D model based on the schematics, ensuring all sensor dimensions, tolerances, and mounting locations are accurately accounted for. | NAV |
| SAFETY-01 | SF-1.5 | Print Casing & Holders | Use a 3D printer to print all sensor holders | 8 | 0 | PUSH | After developing the 3D model, create the model using a 3D printer and ensure the creation accurately matches the design | TBD |
| NOTIF-01 | NF-1.1 | Threshold triggered notifications | Implement notifications triggers for sensors when their respective set thresholds are exceeded | 4 | 0 | PUSH | Notify the user whenever a parameter falls out of it's respective range.Notifications should include the affected sensor and its current reading | TBD |
| NOTIF-01 | NF-1.2 | Status triggered notifications | Implement notifications triggered for sensors when they go offline/online | 4 | 0 | PUSH | Design a cloud function that checks for device updates. If an update hasn't been sent in 15 minutes, It automatically sends the user a notification | TBD |
| PRIVSEC-01 | PS-1.1 | Privacy Page Research | Do some research on how privacy pages are typically written, for inspiration/reference. | 2 | 2 | DONE | Look into legal documentation regarding user privacy, as well as research into other companies for their privacy measures | DOM |
| PRIVSEC-01 | PS-1.2 | Privacy Page Contents | Write a draft for the privacy page. | 2 | 2 | DONE | Include in the privacy page; the information we collect, how we use their information, data sharing, data storing, and user's rights | DOM |
| PRIVSEC-01 | PS-1.3 | Page Redirection | Implement a button from the settings page to access the privacy policy page. | 2 | 3 | DONE | Include in the Settings page UI, an icon that says PRIVACY PAGE directing user's to all information regarding the app's privacy policy | DIPI |
| DASH-01 | DS-1.1 | Dashboard Design | Design a UI for the Dashboard (Main Activity page) | 3 | 3 | DONE | Utilize Figma to design the dashboard UI. | DOM |
| DASH-01 | DS-1.2 | Aquarium Selector with Connection Status | Implement the header aquarium selector widget. Implement the connection status indicator, which should be an aggregate of all sensor statuses. | 4 | 6 | DONE | Indicator should be online/green if board is still communicating, offline/gray otherwise. | BILAL |
| DASH-01 | DS-1.3 | Live Sensor Grid with Danger Indicators | Implement a sensor reading card element with danger indicators, implement a grid that can spawn sensor reading cards and implement an overall aquarium health summary. | 4 | 6 | DONE | Use emojis to indicate health (happy, smiley, normal, sad, crying) and user-defined sensor weights for aggregation. | BILAL |
| DASH-01 | DS-1.4 | Navigation Bar with Aquarium-Specific Results | Implement a navigation bar with dashboard, graphs, alerts, and settings. | 4 | 6 | DONE | The results shown by these navigation options should be specific to the selected aquarium. | BILAL |
| DASH-02 | DS-2.1 | (i) Tool Tip Button | Implement an info button that will show tips, which change depending on the parameter's state (normal, warning, critical) | 2 | 4 | DONE | Ideally, standardize the tooltip to show all info, but it would selectively highlight info depending on state. | BILAL |
| DASH-02 | DS-2.2 | Tool Tip Content Writing | Write tool tip information for each parameter and its respective states | 3 | 4 | DONE | All tooltip contents should be simple and easy to understand for users.Write in simple words and highlight or color important parts/thresholds based on parameter states | BILAL |
| SETTINGS-01 | ST-1.1 | Settings Page Layout & UI Design | Design the layout for the centralized settings page, grouping sections for profile, security, notifications, privacy, and support into a single screen with clear navigation. | 2 | 6 | DONE | Utilize Figma to design the settings UI. Look at the product backlog to figure out what needs to be there. | DOM, DIPI |
| SETTINGS-01 | ST-1.2 | Settings Hub & Persistence | Build the main settings screen with grouped card lists where each card opens its panel inline, and save all settings to the backend so they persist. | 4 | 8 | DONE | Cards open inside settings (no redirect), and settings persist after restart. Ensure the layout is responsive and every section is reachable without leaving the settings page. | DIPI |
| SETTINGS-01 | ST-1.3 | Profile & Account Management Section | Build the profile section so users can view and edit their profile details (name, email, avatar etc) and persist the changes. Implement options for users to set their cloud backup status, and data actions. | 3 | 5 | DONE | Confirm edited profile data saves correctly and is reflected immediately on reload. Delete Account should ask for confirmation. | DIPI |
| SETTINGS-01 | ST-1.4 | Security Preferences Section | Build the security section for changing password, toggling 2FA, and viewing/revoking active sessions. Build the privacy and Firebase sync panels with their toggles and controls. | 3 | 3 | DONE | Ensure password changes enforce validation rules and all security updates persist securely. Toggles reflect live state and sync controls work. | DIPI |
| SETTINGS-01 | ST-1.5 | Notifications Section | Build the notifications panel with alert channels, filters, and per-type toggles. | 2 | 2 | DONE | Each toggle saves and a disabled channel stops its alerts. | DIPI |
| SETTINGS-01 | ST-1.6 | Display & Calibration Section | Build the display/units panel and the sensor calibration guide. Add an option to contact support or redirect to the support contact form | 3 | 5 | DONE | Unit changes update displayed values and the Calibration guide should accurately help the users calibrate the sensors. The support page redirection should take users to the support form. | DIPI |
| SETTINGS-02 | ST-2.1 | Contact form without send | Implement a simple contact form UI. | 1 | 1 | DONE | The purpose of this task is to put in place the UI itself. | DIPI |
| SETTINGS-02 | ST-2.2 | Sending logic | Implement the form submission logic to send an email to support. | 2 | 2 | DONE | The email can be in plaintext format for now. | DIPI |
| SETTINGS-02 | ST-2.3 | Formatting based on contact purpose | Implement a way whereby the user can indicate the purpose of contact, feedback or support, and have the email be sent formatted with that purpose in mind. | 3 | 4 | DONE | Look into how emails can be formatted using HTML. The subject of the email should also reflect the purpose. | DIPI |
| ADMIN-01 | AT-1 | Product Backlog Planning | Write and revise the product backlog. Divide user stories based on components such as sensors, notification, authentication, settings etc. | 4 | 10 | DONE | Story IDs should reflect component-based divisions. Make sure cards remain user-centric, stories are sprint-ordered, conversations with action verbs and goal, confirmations in question format, story points are either 3, 5, 8 or 13. | ALL |
| ADMIN-02 | AT-2 | Sprint 1 Backlog Planning | Plan, write and revise the sprint backlog. Divide sprint 1 User Stories into smaller delegable tasks with clear descriptions and comments. | 4 | 8 | DONE | Tasks should just enough for one person to handle. Comments should help the person doing the task. | ALL |
| ADMIN-03 | AT-3 | Milestone 2 Report | Compile a report that includes a list of figures, list of tables, list of terms, introduction with sprint goal, requirements with product backlog, design with wireframes, and sprint backlog. | 4 | 6 | DONE | A typesetting engine like Typst or LaTeX must be used, as is standard for writing reports. | ALL |
| ADMIN-04 | AT-4 | Project Environment & Repository Setup | Set up the shared Git repository (branching strategy, README, .gitignore), the Firebase project, and the shared Figma workspace so the whole team can collaborate. | 2 | 2 | DONE | Give every member repo and Firebase access; document the local setup steps in the README for everyone. | BILAL |
| ADMIN-05 | AT-5 | Sprint Review & Update | Review the Sprint 1 backlog every 3 days to update sprint progress, log real hours, and ensure that everything is going according to the set timeline | 3 | 3 | DONE | Update sprint backlog with real hours, check if everyone is ahead or behind the set deadlines, add or remove tasks as sprint progresses. | ALL |
| ADMIN-06 | AT-6 | Design Documentation | Update wireframes and UML/architecture diagrams for the Sprint 2 features (history graphs, multi-aquarium, pairing wizard) | 4 | 5.5 | DONE | Update the Figma wireframes and the UML/architecture diagrams (use cases, data flow) to cover all Sprint 1 page and the sensor to Firebase pipeline. Keep diagrams consistent with the actual Figma designs and store the exported figures in the shared repo for the report. | BILAL & NAV |
| ADMIN-07 | AT-7 | Testing Documentation | Write and validate test cases for new HW (battery) and SW (history, instances) additions | 4 | 5 | DONE | Derive test cases directly from each story's confirmation criteria so they stay traceable to the Story IDs. Cover both HW additions (sensors, enclosure, connectivity) and SW additions (dashboard, notifications, pages), then validate each case and record pass/fail. | ALL |
| ADMIN-08 | AT-8 | Definition of Done Checklist | Write Definition of Done checklist for all of the user stories in sprint 1 backlog | 3 | 3 | DONE | Definition of Done cheklist must be consistent within our team throughout the duration of the project. The checklist should include writing code, unit testing, integration testing, design documents, etc. that demonstrates that the PBI has been completed and can be marked as Done. | DOM |
| ADMIN-09 | AT-9 | Sprint 2 Backlog Planning | Plan and write the sprint backlog. Divide sprint 2 User Stories into smaller delegable tasks with clear descriptions and comments. | 3 | 7 | DONE | Include any user stories that could not be completed in Sprint 1. Tasks should just enough for one person to handle. Comments should help the person doing the task. | DIPI |

## Sprint Totals

- **Ideal Hours:** 158
- **Actual Hours:** 197.5
- **Completed Story Points:** 33

*Table 2: Sprint 1 Backlog expressed at Task level.* 

|**Metric**|**Value**|
|---|---|
|Ideal Hours|158|
|Actual Hours|160|
|Completed Story Points|33|

Table 3: Sprint 1 hours. 

### 4.2. Sprint 2 Backlog 

## Sprint 2 Goals

1. Complete work carried over from Sprint 1: sensor timestamping, sensor-offline detection, enclosure printing, and threshold/status notifications. (SENSOR-02, SENSOR-04, SAFETY-01, NOTIF-01)

2. Improve hardware usability by monitoring battery/power state and warning users before hardware shutdown. (SENSOR-05)

3. Implement user notification management with notification snoozing options and a reviewable past notifications history. (NOTIF-02, NOTIF-03)

4. Streamline first app installation with a guided hardware setup wizard and reusable aquarium templates. (SETTINGS-03, SETTINGS-04)

5. Extend the main dashboard to support defining and switching between multiple aquariums. (DASH-04)

6. Implement sensor troubleshooting guides and long-term sensor data trend analysis. (SETTINGS-06, DASH-03)

7. Add Firebase database integration, hardware/frontend connectivity, and login/registration; add an at-a-glance overall aquarium status summary. (FIREBASE-01, FIREBASE-02, SETTINGS-06, DASH-05)

| Story ID | Task ID | Task Title | Task Description | Ideal Hours | Actual Hours | Status | Comments | Assignee(s) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SENSOR-02 | SR-2.4 | Hardware time-stamping setup | Implement a local timestamping protocol on the hardware node to track data transmission intervals. | 6 |  | DONE | Ensure the mobile application can fetch historical and live records smoothly from the cloud database. | DOM |
| SENSOR-04 | SR-4.1 | Application system alert hooks | Create background logic hooks in the app to listen for real-time Firebase threshold breaches. | 6 |  | PROG | Trigger an immediate local system push-notification if a threshold failure state is observed. | DOM |
| SENSOR-04 | SR-4.2 | Connection heartbeat listener | Implement a network heartbeat function that checks if the hardware hub has stopped transmitting. | 4 |  | PROG | Issue an intrusive critical warning notification if the hub connection falls completely silent. | DOM |
| SAFETY-01 | SF-1.5 | Print Casing & Holders | Use a 3D printer to print all sensor holders. | 2 |  | TODO | After developing the 3D model, create the model using a 3D printer and ensure the creation accurately matches the design. Assemble all the sensors inside the 3D casing and test the connection flow. It is optimal if the casing is the minimum size possible for ease of handling and aquarium aesthetics. Make sure the casing color is aesthetically pleasing. | ARMAAN/NAV |
| SAFETY-01 | SF-1.6 | Buy Aquarium Tank & Accessories | Purchase a transparent aquarium tank (plastic or glass) and the relevant accessories for demo | 3 |  | TODO | Confirm tank size against sensor/casing dimensions from SF-1.5 before buying, so holders fit without modification. Keep the receipt for budget tracking. Relevant accessories may include cheap fake plants, rocks, fish stickers and other aesthetic items. | TBD |
| NOTIF-01 | NF-1.1 | Threshold triggered notifications | Implement notification triggers for sensors when their respective set thresholds are exceeded. | 4 |  | TODO | Notify the user whenever a parameter falls out of its respective range. Notifications should include the affected sensor and its current reading. | ARMAAN |
| NOTIF-01 | NF-1.2 | Status triggered notifications | Implement notifications triggered for sensors when they go offline/online. | 4 |  | TODO | Design a cloud function that checks for device updates. If an update hasn't been sent in 15 minutes, it automatically sends the user a notification. | ARMAAN |
| NOTIF-01 | NF-1.3 | Per-sensor notification toggle | Add an on/off toggle for abnormal-value jump notifications even within the good range, plus a sensor picker so users can choose which sensors trigger these alerts. | 3 |  | TODO | Toggle and sensor selection should live in Settings under Notification settings. Should work alongside existing threshold/status notification logic, not replace it. | TBD |
| NOTIF-02 | NF-2.1 | Per-aquarium Maintenance Mode UI | Add a toggle in the dashboard and aquarium header to enable maintenance mode for a selected aquarium. | 3 |  | TODO | Toggle state should be scoped per aquarium, not global. Enabling maintenance mode will turn off all notifications for the aquarium's water parameter changes until maintenance mode is toggled off. | ARMAAN |
| NOTIF-02 | NF-2.2 | Snooze duration timer | Let users pick a duration (15m, 30m, 1h, 2h) for maintenance mode or "snooze notifications" option and automatically resume alerts once it elapses. | 4 |  | TODO | Use a background timer; resume must run even if app is running in the background. | ARMAAN |
| NOTIF-02 | NF-2.3 | Active maintenance-mode indicator | Clearly display on the dashboard when maintenance mode is active and remaining time. | 2 |  | TODO | Use a banner or badge near the aquarium header displaying when maintenance mode is on. | ARMAAN |
| NOTIF-03 | NF-3.1 | Notification log data model | Design a data structure to log every sent notification (threshold, offline, time-triggered) with timestamp and sensor. | 3 |  | TODO | Store trigger reason and threshold/color level with each entry. | TBD |
| NOTIF-03 | NF-3.2 | Notification history list UI | Build the UI listing past notifications, filterable by aquarium and sensor. | 4 |  | TODO | Reuse dashboard filter components where possible. This past notifications history list should be accessible from the settings page. | TBD |
| NOTIF-03 | NF-3.3 | Link Notification Retention to Sensor History Settings | When a user sets a retention period for a sensor's historical data (e.g., 30 days), automatically apply that same time window to how long related notifications are kept in the log, so old alerts don't outlive the data they refer to. | 2 |  | TODO | Depends on DASH-03 (Historical Sensor Data Retention) being built first, since the notification history retention duration comes from there. | TBD |
| DASH-03 | DS-3.1 | Per-sensor history retention option | Add an option to enable history per sensor and define a retention time window. | 3 |  | PROG | The retention duration set here is used in NOTIF-03. | BILAL |
| DASH-03 | DS-3.2 | Historical data storage & retention in database | Store sensor readings over time in Firebase and delete/remove data beyond the configured retention window. | 5 |  | PROG | Ensure all sensor readings are being stored correctly in Firebase with accurate timestamps. Make sure outdated data are deleted properly. | BILAL |
| DASH-03 | DS-3.3 | Sensor history graph view UI | Build a graph view of each sensor's history accessible by clicking a sensor, with 1d/7d/1m/3m/6m/1y selectable ranges. | 5 |  | PROG | Only render graph/chart for sensors with history retention option enabled. Preferably use line graphs for numerical data. | BILAL |
| DASH-03 | DS-3.4 | Average value & spike highlighting | Show an average reading for the selected time window and highlight spurs/spikes on the graph if feasible. | 3 |  | PROG | Spike highlighting is a stretch goal; descope if time-constrained. For spike highlighting, use red circles or symbols for visual detection of abnormal increases. | BILAL |
| DASH-03 | DS-3.5 | History report export | Allow users to download a sensor's history as a plaintext/pdf report. | 3 |  | PROG | Confirm desired file format (.txt vs .csv vs .pdf) with team. | BILAL |
| DASH-04 | DS-4.1 | Aquarium Switcher UI | Add a selector in the app header bar to switch between defined aquariums, using each aquarium's hardware "address" to connect to the right device. | 4 |  | PROG | Should scale properly as aquarium count grows. A maximum number of allowed aquariums should be defined and set to avoid unrealistic number of aquariums. | NAV |
| DASH-04 | DS-4.2 | First installation routing for zero aquariums | For the first-install experience when no aquariums are defined yet, route the user into adding their first aquarium. | 2 |  | TODO | Should route into the SETTINGS-03 pairing wizard. | NAV |
| DASH-04 | DS-4.3 | New Aquarium Creation | When adding a new aquarium, present the user with 4 choices: Beginner, Intermediate, Expert (built-in templates), or Custom (define all settings manually). | 4 |  | TODO | Connected directly with SETTINGS-04 built-in templates; selecting "Custom" should skip template auto-config entirely. | NAV |
| DASH-04 | DS-4.4 | Advanced Settings Option | Keep advanced aquarium-definition settings collapsed by default, only expanding them if the user explicitly clicks it. | 3 |  | TODO | Applies regardless of which of the 4 creation options is chosen. | NAV |
| DASH-04 | DS-4.5 | Per-aquarium state/context management | Refactor app state so the dashboard, settings and reports are scoped to the currently selected aquarium. | 6 |  | TODO | Discuss feasibility of this task with team; if difficult, reduce the context retention scope of each aquarium. | NAV |
| DASH-05 | DS-5.1 | Overall status indicator design | Design dashboard UI with an overall status indicator alongside sensor cards | 1 | 1 | DONE | Utilize Figma to design the overall status integrated dashboard UI | DIPI |
| DASH-05 | DS-5.2 | Implement overall status indicator in dashboard | Define the status states (Normal/Warning/Critical) with visual indicator and a short written summary shown on the dashboard. | 2 |  | TODO | Status should reflect the worst current sensor condition across the selected aquarium. Some parameters will have more weight than others, which is to be determined and discussed with team. For visual indicators, use emojis/ weather indicators and make the size span half of the screen for visual ease. | TBD |
| DASH-05 | DS-5.3 | Clickable status detail view | Make the status indicator clickable, opening a view listing abnormal sensor(s) and their current values. | 3 |  | TODO | Reuse dashboard sensor components where possible for the abnormal-sensor list. | TBD |
| FIREBASE-01 | FB-1.1 | Design Firebase database schema | Define the Firebase collections/structure for aquariums, sensors, readings, and users. | 3 |  | TODO | Finalize field names and structure with the team before hardware/app work depends on it. | BILAL |
| FIREBASE-01 | FB-1.2 | Hardware-to-Firebase sensor sync | Connect the hardware device to Firebase so it reads sensor values and writes/updates them periodically. | 5 |  | PROG | Confirm the update interval with the team; ensure timestamps are included with each write. | DOM & ARM |
| FIREBASE-01 | FB-1.3 | Android Firebase helper functions | Build reusable wrapper/helper functions in the Android app to fetch and listen to sensor values from Firebase. | 4 |  | TODO | These helpers should be reused by the dashboard, history graphs, and notification hooks. | DIPI |
| FIREBASE-02 | FB-2.1 | Firebase Auth setup | Enable email/password authentication in Firebase and configure security rules scoped to the logged-in user. | 2 |  | TODO | Coordinate with FIREBASE-01 schema so data is scoped per user/account. | TBD |
| FIREBASE-02 | FB-2.2 | Registration & Login UI | Build sign-up and log-in screens wired to Firebase Auth, with validation and error messaging. | 3 |  | TODO | Restrict app access behind a valid session; route to SETTINGS-03 guided wizard after first login. | TBD |
| SETTINGS-03 | ST-3.1 | First-install detection & wizard launch | Detect when no aquariums are defined and automatically launch the setup wizard on first install of the app. | 2 |  | PROG | Should not re-trigger once at least one aquarium exists. | DIPI |
| SETTINGS-03 | ST-3.2 | Device pairing & connection with Hardware | Build wizard steps for connecting to the hardware hub and verifying the connection before proceeding. | 5 |  | PROG | Show clear error messages or appropriate redirections/retries for failed connections. Users may input hardware/sensor IP address to connect to the hub; finalize in team meeting. | DIPI |
| SETTINGS-03 | ST-3.3 | Aquarium selection & setup step | Add a wizard step to name and set up an aquarium and optionally apply a default aquarium template. | 3 |  | TODO | Depends on SETTINGS-04 default aquarium templates being available. | DIPI |
| SETTINGS-04 | ST-4.1 | Define built-in templates | Create the 3 built-in templates (Beginner, Intermediate, Expert) with their preset parameter sets | 3 |  | TODO | Finalize exact parameter lists per tier with team before implementation. Beginner template should include 2-3 core parameters, Expert should include all water parameters. | DIPI |
| SETTINGS-04 | ST-4.2 | Template Description UI | Create an aquarium template section in Settings page and upon clicking that, display the default settings configured for Beginner, Intermediate, and Expert templates. | 3 |  | TODO | Make sure the user can navigate to aquarium template section from the settings page to view the built-in template settings to have knowledge about each template. | DIPI |
| SETTINGS-04 | ST-4.3 | Auto-configure Template Settings | When user selects a template when adding a new aquarium, apply the selected template's parameters automatically and grey them out as finalized/locked settings. | 5 |  | TODO | Confirm with the team whether greyed-out settings are fully locked or editable with a warning. | TBD |
| SETTINGS-04 | ST-4.4 | Configure remaining settings | Allow users to set up any additional parameters or settings not covered by their selected template. | 3 |  | TODO | Only display parameters or settings options not already included in the chosen template. | TBD |
| SETTINGS-05 | ST-5.1 | Draft troubleshooting content | Write a simple, concise step-by-step troubleshooting guide covering common connection and sensor faults. | 3 |  | TODO | Polish after the 3D enclosure (SAFETY-01) is finalized. | DOM |
| SETTINGS-05 | ST-5.2 | Integrate troubleshooting guide into the app | Add access to the guide from the navigation menu or settings page. The sensor offline critical notifications should also direct the users to the troubleshooting guide on click. | 2 |  | TODO | Ensure that the guide is accessible easily and that the critical sensor failure notifications redirect the users to the guide. | DOM |
| AESTHETICS-01 | AE-1.1 | Design App Icon | Design an app icon reflecting our project's aquarium monitoring theme as a drawable/vector. | 2 |  | TODO | Keep the icon simple and legible at small sizes (home screen, notification pop-ups). Use a color palette consistent with the dashboard's aquatic theme. Export in required Android resolutions. The icon can be a drawable (.xml) or an svg vector. | TBD |
| AESTHETICS-01 | AE-1.2 | Design Loading Screen | Design the loading screen shown on app launch with logo and loading indicator | 2 |  | TODO | Loading screen visuals should match the app icon's color palette and theme for consistency. Keep the loading indicator simple, for example, a wave/bubble motif may fit the aquarium theme well. Discuss with team before design finalization. | TBD |
| AESTHETICS-01 | AE-1.3 | Loading Screen to Dashboard transition | Implement an animated transition (e.g.: wave splash animation) to transition from the loading screen into the main dashboard. | 4 |  | TODO | Test the transition timing so it does not feel sluggish/lagging on slower devices. Ensure the animation effect completes before the main dashboard finishes loading live sensor data, so there's no visual gap or flash. Make sure the transition is smooth and seamless. | TBD |
| AESTHETICS-01 | AE-1.4 | Define & Implement Color Scheme/Theme | Decide on the app's overall color scheme/theme and apply it consistently across dashboard, settings, and other screens. | 3 |  | TODO | Research Google Gemini Stitch for generating UI design/theme direction and code. Keep the palette consistent with the app icon and loading screen (AE-1.1, AE-1.2). TA-recommended color-scheme: blueish, aquatic-themed | TBD |
| AESTHETICS-01 | AE-1.5 | Dark Mode Theme | Add a dark mode color theme/toggle so users can switch between light and dark appearance based on their preference. | 3 | 3 | DONE | Make sure the light and dark modes are consistent across all components of the app with proper aesthetic themes. | DIPITA |
| ADMIN-01 | AT-1 | Backlog Planning | Revise the product backlog and sprint backlogs. | 4 |  | TODO | Cross-check Story IDs and Task IDs between the Product Backlog and Sprint Backlog tabs after any renumbering or scope changes (e.g. pushed Sprint 1 items) so both stay in sync | ALL |
| ADMIN-02 | AT-2 | Sprint Review & Update | Review the Sprint 2 backlog every 3 days to update sprint progress, log real hours, and ensure that everything is going according to the set timeline | 3 |  | TODO | Update sprint backlog with real hours, check if everyone is ahead or behind the set deadlines, add or remove tasks as sprint progresses. | ALL |
| ADMIN-03 | AT-3 | Design Documentation | Update wireframes and UML/architecture diagrams for the Sprint 2 features (history graphs, multi-aquarium, pairing wizard) | 4 |  | TODO | Update the Figma wireframes and the UML/architecture diagrams (use cases, data flow) to cover all Sprint 2 page and the sensor to Firebase pipeline. Keep diagrams consistent with the actual Figma designs and store the exported figures in the shared repo for the report. | TBD |
| ADMIN-04 | AT-4 | Testing Documentation | Write and validate test cases for new HW (battery) and SW (history, instances) additions | 4 |  | TODO | Derive test cases directly from each story's confirmation criteria so they stay traceable to the Story IDs. Cover both HW additions (sensors, enclosure, connectivity) and SW additions (dashboard, notifications, pages), then validate each case and record pass/fail. | TBD |
| ADMIN-05 | AT-5 | Definition of Done Checklist | Write Definition of Done checklist for all of the user stories in sprint 2 backlog | 3 |  | TODO | Definition of Done cheklist must be consistent within our team throughout the duration of the project. The checklist should include writing code, unit testing, integration testing, design documents, etc. that demonstrates that the PBI has been completed and can be marked as Done. | TBD |
| ADMIN-06 | AT-6 | Sprint 3 Backlog Planning | Plan and write the sprint 3 backlog. Divide sprint32 User Stories into smaller delegable tasks with clear descriptions and comments. | 3 |  | TODO | Include any user stories that could not be completed in Sprint 2. Tasks should just enough for one person to handle. Comments should help the person doing the task. | TBD |

## Sprint Totals

- **Ideal Hours:** 175
- **Actual Hours:** 4
- **Completed Story Points:** —

Table 4: Sprint 2 Backlog expressed at Task level. 

|**Metric**|**Value**|
|---|---|
|Ideal Hours|147|
|Actual Hours|TBD|
|Completed Story Points|TBD|

Table 5: Sprint 2 hours. 

## 5. Test Documents (Version 2) 

### 5.1. Test Plan 1: Circuit Design & Simulation 

### 5.1.1. Summary 

The tests being run are focused on testing the stability and power of the sensors to ensure the components won’t be damaged. 

### 5.1.2. Test Cases 

#### Test Case: SR-1.1 

#### Pre-Condition: 

_Sensors wired to the breadboard_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Recreate the circuit in KiCad|The circuit should match the real one|The circuit matched the real one|
|**Result:** `PASS`|||

Table 6: Test cases for SR-1.1 

|**Test Case: SR-1.2** |||
|---|---|---|
|**Pre-Condition:** _Sensor Simulation_|||
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Verify any power spikes to ensure it won't damage the microcontroller|The input voltage into the microcontroller should not go above 3.3v|The input voltage only reached a max 3.13v|
|**Result:** `PASS`|||

Table 7: Test cases for SR-1.2 

### 5.2. Test Plan 2: Data Collection Evaluation 

### 5.2.1. Summary 

Database testing has been done to ensure that data is being properly sent to the Firebase and being received on the app. 

### 5.2.2. Test Cases 

**Test Case: SR-2.1** 

#### Pre-Condition: 

_Application launched. Local Wi-Fi network is active. ESP32 module powered on and firmware running._ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Access the Firebase Console web dashboard and inspect the root directory data nodes|The database JSON tree structure should display active keys for aquarium_simulation/ live_telemetry|The directory correctly structured nodes for temperature_C, device_status, and system alert paths|
|**2.**Verify inbound dummy payload updates manually pushed to the cloud server routing endpoints|The cloud server should securely append the testing entries directly into the active database console rows|Confirm incoming data values append securely to the corresponding cloud data tables|
|**Result:** `PASS`|||

Table 8: Test cases for SR-2.1 

**Test Case: SR-2.2** 

#### Pre-Condition: 

_Application launched. Local Wi-Fi network is active. ESP32 module powered on and firmware running._ **Steps: Expected Results Actual Results** 

|**1.**Power on the physical hardware hub node running the telemetry client firmware and observe the terminal logs|The network client should log a successful local Wi-Fi handshake and lock to the database URL|Verify continuous communication between the active physical hardware node and the Firebase cloud server|
|---|---|---|
|**2.**Simulate physical thermal variance triggers and look for automated spike routine evaluations|The ESP32 should calculate changes, trigger an alert path variable if variance exceeds 2.0C, and push the JSON bundle|The console updated felds live in green and the connected mobile client fetched live records smoothly|
|**Result:** `PASS`|||

Table 9: Test cases for SR-2.2 

#### Test Case: SR-2.3 

|**Pre-Condition:** _Application launched._ _Local Wi-Fi network is ac_ _ESP32 module powered on_ |_tive._ _and firmware running._ ||
|---|---|---|
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Observe the Serial Output of the ESP32 Monitor|The data will be successfully transmitted from the sensors on the ESP32 to the serial monitor|As expected|
|**2.**Trigger Real-time Sensor Capturing|The data is accurately being transmitted with no hardware timeouts  as the values are actively changing|Not timeouts occured while all sensors are properly connected.|
|**Result:** `PASS`|||

Table 10: Test cases for SR-2.3 

### 5.3. Test Plan 3: Sensor to Cloud Connectivity 

### 5.3.1. Summary 

The tests being done are to ensure the cloud database can receive the proper data from the 

sensors. 

### 5.3.2. Test Cases 

#### Test Case: SR-3.1 

**Pre-Condition:** _Application launched. Local Wi-Fi network is active. ESP32 module powered on and firmware running._ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Open the Firebase Console and inspect the root database URL path.|The structured JSON tree hierarchy (alerts, aquarium_simulation, live_telemetry) is deployed and active.|As Expected|
|**Result:** `PASS`|||

Table 11: Test cases for SR-3.1 

|**Test Case: SR-3.2**|
|---|
|**Pre-Condition:**|
|_Application launched._|
|_Local Wi-Fi network is active._ |
|_ESP32 module powered on and firmware running._|
|_Data is actively transmitting to Firebase_|

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Observe the real-time dashboard data changes within the Firebase console tree.|Incoming data values automatically append and update under their corresponding.|As Expected|
|**Result:** `PASS`|||

#### Table 12: Test cases for SR-3.2 

### 5.4. Test Plan 4: Creating Waterproofing Compartments for the Sensors 

### 5.4.1. Summary 

The breadboard components will need proper protection to prevent damage from the water. These tests were done to verify that any liquids will not impact the components. 

### 5.4.2. Test Cases 

**Test Case: SF-1.1** 

**Pre-Condition:** _Dry deck level alignment and structural stability verification_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Place a standard surface spirit level across the center face of the primary dry deck platform|The indicator bubble should center completely, confrming the main shelf deck rests perfectly horizontal|Surface level test confrmed complete horizontal alignment relative to the container rim structure|
|**2.**Drop a standard half- size 6 1/32 in x 2 in electronics breadboard into the upper shelf tray compartment|The breadboard should sit entirely fat, fush, and secure within the raised perimeter lips of the tray|The breadboard stayed frmly seated fat on the dry deck without shifing or pivoting|
|**Result:** `PASS`|||

Table 13: Test cases for SF-1.1 

**Test Case: SF-1.2** 

#### Pre-Condition: 

_Sensor Placement Strategy and Geometry Verification_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|

|**1.**Measure physical center-to-center distance between adjacent 3D- printed socket openings|The clearance distance between probe holes must measure greater than or equal to 20 mm to prevent cross-talk|The clearance distance measured at 22.7 mm, satisfying the physical safety baseline|
|---|---|---|
|**2.**Insert the physical DS18B20 temperature probe, pH probe, and TDS probe into their designated sockets|The probes slide through smoothly without forcing and remain securely seated upright|All three probes clear the structural tolerances perfectly and hang securely via gravity|
|**Result:** `PASS`|||

Table 14: Test cases for SF-1.2 

#### Test Case: SF-1.3 

**Pre-Condition:** _Mechanical Bracket Mounting and Component Protection_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Lower the inverted U- channel hook of the shelf over the top edge of the container wall and check alignment|The bracket slides over the rim easily without tools and holds the primary dry deck perfectly level|Bracket drops securely over the rim; surface level test confrms complete horizontal alignment|
|**2.**Drop the SparkFun ESP32 Thing Plus breadboard setup into the tray and simulate fuid surface ripples|The breadboard sits completely fat within the perimeter lips, and zero water splash reaches the upper deck|Breadboard stays frmly seated, and active fuid surface movement causes zero top-deck splash contamination|
|**Result:** `PASS`|||

Table 15: Test cases for SF-1.3 

**Test Case: SF-1.4** 

#### Pre-Condition: 

_3D CAD modeling software is launched. Schematic layout files & component datasheets are available._ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Verify the generation of the schematic design inside the CAD workspace.|The initial schematics are successfully drafted and properly constrained within the 3D environment.|As Expected|
|**2.**Extrude and develop the full 3D assembly model directly from the generated schematics.|The 3D model is rendered correctly.|As Expected|
|**Result:** `PASS`|||

Table 16: Test cases for SF-1.4 

### 5.5. Test Plan 5: Privacy Page 

### 5.5.1. Summary 

A privacy page is needed to ensure user’s information will be kept private. Verifying the privacy documents don’t leak information was done to protect users. 

### 5.5.2. Test Cases 

|**Test Case: PS-1.3** |||
|---|---|---|
|**Pre-Condition:** _None_|||
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Check that privacy page is accessible from settings and is displayed correctly|Privacy page is accessible from settings and is displayed correctly|Privacy page is accessible from settings and is displayed correctly|
|**Result:** `PASS`|||

#### Table 17: Test cases for PS-1.3 

### 5.6. Test Plan 6: Live Status Dashboard 

### 5.6.1. Summary 

A dashboard is created to give the user’s a platform to input their aquariums. The various dashboard buttons were verified to ensure user’s will not encounter any issues. 

### 5.6.2. Test Cases 

|**Test Case: DS-1.2** |||
|---|---|---|
|**Pre-Condition:** _None_|||
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Rerun app|App runs|App runs|
|**2.**Check that aquarium selector header displays|Aquarium selector header displays|Aquarium selector header displays|
|**3.**Check that aquarium status indicator displays|Aquarium status indicator displays|Aquarium status indicator displays|
|**Result:** `PASS`|||

Table 18: Test cases for DS-1.2 

|**Test Case: DS-1.3** **Pre-Condition:**|||
|---|---|---|
|_None_|||
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Rerun app|App runs|App runs|

|**2.**Check that aquarium sensor grid with all card elements displays|Aquarium sensor grid with all card elements displays|Aquarium sensor grid with all card elements displays|
|---|---|---|
|**3.**Check that aquarium aggregate score emoji displays|Aquarium aggregate score emoji displays|Aquarium aggregate score emoji displays|
|**Result:** `PASS`|||

Table 19: Test cases for DS-1.3 

|**Test Case: DS-1.4** |||
|---|---|---|
|**Pre-Condition:** _None_|||
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Rerun app|App runs|App runs|
|**2.**Check that navigation bar with all tabs displays|Navigation bar with all tabs displays|Navigation bar with all tabs displays|
|**Result:** `PASS`|||

Table 20: Test cases for DS-1.4 

### 5.7. Test Plan 7: Sensor Reading Tooltips 

### 5.7.1. Summary 

A dashboard is created to give the user’s a platform to input their aquariums. The various dashboard buttons were verified to ensure user’s will not encounter any issues. 

### 5.7.2. Test Cases 

**Test Case: DS-2.1** 

|**Pre-Condition:** _None_|||
|---|---|---|
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Rerun app|App runs|App runs|
|**2.**Check that tooltip icon displays|Tooltip icon displays|Tooltip icon displays|
|**3.**Check that tooltip icon is clickable and shows bottom sheet|Tooltip icon is clickable and shows bottom sheet|Tooltip icon is clickable and shows bottom sheet|
|**Result:** `PASS`|||

Table 21: Test cases for DS-2.1 

### 5.8. Test Plan 8: Settings Page 

### 5.8.1. Summary 

**Requirement ID:** SETTINGS-01 

A settings page gives user’s the option to modify their aquarium app. It was tested by changing various settings to ensure no errors would occur. 

|**Test Case: ST-1.2** |||
|---|---|---|
|**Pre-Condition:** _User is logged in and on th_|_e Settings hub screen_||
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Open a settings card (e.g. Notifications)|Panel opens inline within the settings screen, no redirect|Panel opened inline within the settings screen|
|**2.**Change a value and close the panel|Value is saved|Value is saved|

|**3.**Restart/reload the app|Settings hub still shows every section, and the changed value persists|Settings hub changed value persists|
|---|---|---|
|**Result:** `PASS`|||

Table 22: Test cases for ST-1.2 

#### Test Case: ST-1.2 

#### Pre-Condition: 

_User is on a small/narrow screen (mobile viewport)_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Open Settings hub|Layout is responsive, all sections reachable without leaving the page|Layout is responsive and all sections are reachable|
|**2.**Navigate to each section from this viewport|No section is hidden, cut of, or inaccessible|No section is hidden or inaccessible|
|**Result:** `PASS`|||

Table 23: Test cases for ST-1.2 

#### Test Case: ST-1.3 

#### Pre-Condition: 

_User is logged in with an existing profile (name, email, avatar)_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Edit name, email, and avatar|Fields update and save|Fields update and save|
|**2.**Reload the app|Edited values are reflected immediately, no reversion to old data|Edited values are reflected immediately|
|**Result:** `PASS`|||

Table 24: Test cases for ST-1.3 

|**Test Case: ST-1.3**|
|---|

|**Pre-Condition:** _User is on the Account sectio_ |_n_ ||
|---|---|---|
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Enter an invalid email (e.g. no "@", 300+ characters, emoji-only)|Save is rejected with a validation error, no corrupted data stored|Save is rejected with a toast message and data is not stored|
|**2.**Enter a name feld with an extremely long string (e.g. 5000 characters)|App does not crash; input is rejected or truncated gracefully|App does not crash and input is rejected|
|**Result:** `PASS`|||

Table 25: Test cases for ST-1.3 

|**Test Case: ST-1.3** |||
|---|---|---|
|**Pre-Condition:** _User selects "Delete Account"_|||
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Tap Delete Account|A confrmation prompt appears|A confrmation prompt appears|
|**2.**Cancel the confrmation|Account is NOT deleted|Account is NOT deleted|
|**3.**Confirm deletion|Account is deleted and user is logged out/ redirected|Account is deleted and user is logged out|
|**Result:** `PASS`|||

Table 26: Test cases for ST-1.3 

**Test Case: ST-1.4** 

**Pre-Condition:** _User is on the Security section_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Enter a new password that doesn't meet complexity rules (e.g. "123")|Change is rejected with a validation message|Change is rejected with a validation toast message|
|**2.**Enter a valid new password and confrm|Password updates successfully|Password updates successfully with toast|
|**Result:** `PASS`|||

Table 27: Test cases for ST-1.4 

#### Test Case: ST-1.4 

#### Pre-Condition: 

_User is on Privacy/Firebase sync controls_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Toggle a sync/privacy control of|State updates and persists after reload|State updates and persists after reload|
|**2.**Toggle it back on|Sync resumes and reflects live state correctly|Sync resumes and reflects live state correctly|
|**Result:** `PASS`|||

Table 28: Test cases for ST-1.4 

|**Test Case: ST-1.6**|
|---|

|**Pre-Condition:** _User is on the Display/Units_|_panel_||
|---|---|---|
|**Steps:**|**Expected Results**|**Actual Results**|
|**1.**Change units (e.g. °C to °F, or metric to imperial)|Displayed values throughout the app update to match the new unit|Displayed values throughout the app update to match the new unit|
|**Result:** `PASS`|||

Table 29: Test cases for ST-1.6 

#### Test Case: ST-1.6 

#### Pre-Condition: 

_User opens the sensor calibration guide_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Follow calibration guide steps|Guide accurately walks the user through calibrating the sensor|Guide accurately walks the user through calibrating the sensor|
|**Result:** `PASS`|||

Table 30: Test cases for ST-1.6 

#### Test Case: ST-1.6 

#### Pre-Condition: 

_User taps "Contact Support" from the Display panel_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Tap Contact Support|User is redirected to the support contact form|User is redirected to the support contact form|
|**Result:** `PASS`|||

Table 31: Test cases for ST-1.6 

### 5.9. Test Plan 9: Contact Page 

### 5.9.1. Summary 

#### Requirement ID: SENSOR-02 

Tests were performed to ensure that user’s could contact support in case of issues with the app. The tests were done to make sure that customer support will receive these alerts. 

### 5.9.2. Test Cases 

#### Test Case: ST-2.2 

#### Pre-Condition: 

_User has filled out the contact form with valid input_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Submit the form|An email is sent to support with the form's content|An email is sent to support with the form's content|
|**2.**Check support inbox|Email arrives, contains submitted content in plaintext|An email is sent to support with the form's content|
|**Result:** `PASS`|||

Table 32: Test cases for ST-2.2 

#### Test Case: ST-2.2 

**Pre-Condition:** _User submits the form with an empty message field_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Leave message blank, tap submit|Submission is blocked/ validated, no empty email is sent|Submission is blocked/ validated, no empty email is sent|
|**Result:** `PASS`|||

Table 33: Test cases for ST-2.2 

#### Test Case: ST-2.2 

#### Pre-Condition: 

_User submits the form, then network drops (or backend is unreachable)_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|

|**1.**Submit form while offline|App shows a failure/error state, does not silently lose the message or crash|App shows a failure/error state, does not silently lose the message|
|---|---|---|
|**Result:** `PASS`|||

Table 34: Test cases for ST-2.2 

#### Test Case: ST-2.3 

#### Pre-Condition: 

_User selects "Feedback" as the purpose and submits_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Submit with purpose = Feedback|Email is formatted (HTML) and subject line reflects "Feedback"|Email is formatted (HTML) and subject line reflects "Feedback"|
|**Result:** `PASS`|||

Table 35: Test cases for ST-2.3 

#### Test Case: ST-2.3 

#### Pre-Condition: 

_User selects "Support" as the purpose and submits_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Submit with purpose = Support|Email is formatted (HTML) and subject line reflects "Support"|Email is formatted (HTML) and subject line reflects "Support"|
|**Result:** `PASS`|||

Table 36: Test cases for ST-2.3 

**Test Case: ST-2.3** 

**Pre-Condition:** 

_User submits without selecting a purpose (if not mandatory)_ 

|**Steps:**|**Expected Results**|**Actual Results**|
|---|---|---|
|**1.**Submit with no purpose selected|App either forces a default purpose or blocks submission; email is never sent with a blank/ malformed subject|App blocks a submission without a default purpose|
|**Result:** `PASS`|||

Table 37: Test cases for ST-2.3 

## 6. Definition of Done (DoD) Checklist Validation 

### 6.1. Definition of Done Criteria 

- _✓ Produced artifact (code/document) for the PBI_ 

- _✓ Hardware tests performed and passed_ 

- _✓ Software tests performed and passed_ 

- _✓ Project builds without errors_ 

- _✓ Peer code review performed_ 

- _✓ Backlog Document updated_ 

- _✓ Design Document updated_ 

- _✓ Test Document updated_ 

- _✓ Test Case Document updated_ 

- _✓ All Tasks were Completed_ 

#### _Story ID: COM-01_ 

#### _User Story:_ 

_"As a user, I want to easily add individual, secondary sensors to my existing app profile, so that I can increase my monitoring setup beyond the factory preset configuration.“_ 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Not Applicable>_**|
|2. Software tests performed and passed|**_<Not Applicable>_**|
|3. Project builds without errors|**_<Not Applicable>_**|
|4. Peer code review performed|**_<Not Applicable>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Done>_**|

_Table 38: COM-01._ 

#### _Story ID: SENSOR-01_ 

#### _User Story:_ 

_As a user, I want the sensor circuitry to be wired correctly and validated through simulation before physical assembly, so that hardware faults are caught early and don’t damage components or produce inaccurate readings._ 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Done>_**|
|2. Software tests performed and passed|**_<Not Applicable>_**|
|3. Project builds without errors|**_<Not Applicable>_**|
|4. Peer code review performed|**_<Not Applicable>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Done>_**|

_Table 39: SENSOR-01._ 

#### _Story ID: SENSOR-02_ 

#### _User Story:_ 

_As a user, I would like to know that my sensors are relaying the correct and up to date information to my application, so that I can have the most accurate data in a timely manner._ 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|

|1. Hardware tests performed and passed |**_<Done>_**|
|---|---|
|2. Software tests performed and passed|**_<Done>_**|
|3. Project builds without errors|**_<Done>_**|
|4. Peer code review performed|**_<Done>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Partially Pushed>_**|

_Table 40: SENSOR-02._ 

#### _Story ID: SENSOR-03_ 

#### _User Story:_ 

_As a user, I want to be able to relay my data onto my application and save it, so that I can use it for future referencing when needed._ 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Done>_**|
|2. Software tests performed and passed|**_<Done>_**|
|3. Project builds without errors|**_<Done>_**|
|4. Peer code review performed|**_<Done>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Done>_**|

_Table 41: SENSOR-03._ 

#### _Story ID: PRIVSEC-01_ 

#### _User Story:_ 

_As a user, I want none of my data to be shared with anyone, so that I can rest easy._ 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Not Applicable>_**|
|2. Software tests performed and passed|**_<Done>_**|
|3. Project builds without errors|**_<Done>_**|
|4. Peer code review performed|**_<Done>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done/Not Applicable>_**|

7. Test Case Document updated **_<Done>_** 8. All Tasks were Completed **_<Done>_** 

_Table 42: PRIVSEC-01._ 

#### _Story ID: DASH-01_ 

#### _User Story:_ 

_As a user, I want to access a dashboard showing the live statuses and readings of all sensors, so that I can stay informed at all times on how my system is doing._ 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Not Applicable>_**|
|2. Software tests performed and passed|**_<Done>_**|
|3. Project builds without errors|**_<Done>_**|
|4. Peer code review performed|**_<Done>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Done>_**|

_Table 43: DASH-01._ 

#### _Story ID: DASH-02_ 

#### _User Story:_ 

_As a user, I want to be able to view descriptions of what sensor readings mean in the dashboard, so that I can better understand what I’m seeing._ 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Not Applicable>_**|
|2. Software tests performed and passed|**_<Done>_**|
|3. Project builds without errors|**_<Done>_**|
|4. Peer code review performed|**_<Done>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Done>_**|

_Table 44: DASH-02._ 

#### _Story ID: SAFETY-01_ 

#### _User Story:_ 

_As a user, I want the sensors and hub assembled in a safe, water-resistant enclosure that mounts inside or around my aquarium, so that I get accurate readings without damaging my equipment or aquatic animals._ 

**_DoD checklist for this PBI:_** 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Done>_**|
|2. Software tests performed and passed|**_<Done>_**|
|3. Project builds without errors|**_<Not Applicable>_**|
|4. Peer code review performed|**_<Not Applicable>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Partially Pushed>_**|

_Table 45: SAFETY-01._ 

#### _Story ID: SETTINGS-01_ 

#### _User Story:_ 

_As a user, I want to have a dedicated settings page, so that I can manage my profile, security preferences, and notification options, privacy page, contact support page etc. in one centralized location_ 

**_DoD checklist for this PBI:_** 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Not Applicable>_**|
|2. Software tests performed and passed|**_<Done>_**|
|3. Project builds without errors|**_<Done>_**|
|4. Peer code review performed|**_<Done>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Done>_**|

_Table 46: SETTINGS-01._ 

#### _Story ID: SETTINGS-02_ 

#### _User Story:_ 

_As a user, I want the option to contact support, so that I can rest easy if I face issues with the app and/or the hardware._ 

|**_DoD checklist for this PBI:_**|**_Status_**|
|---|---|
|1. Hardware tests performed and passed |**_<Not Applicable>_**|
|2. Software tests performed and passed|**_<Done>_**|
|3. Project builds without errors|**_<Done>_**|
|4. Peer code review performed|**_<Done>_**|
|5. Backlog Document updated|**_<Done>_**|
|6. Design Document updated|**_<Done>_**|
|7. Test Case Document updated|**_<Done>_**|
|8. All Tasks were Completed|**_<Done>_**|

_Table 47: SETTINGS-02._ 

