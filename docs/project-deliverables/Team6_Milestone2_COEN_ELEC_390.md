# Milestone 2 — Team 6

COEN/ELEC 390

### Table of Contents

- Interview: Stakeholder Information
- Interview: Consent Form
- Interview: Global Script Template
- Interview: Three Most Important Takeaways
- Ethical Dimensions
- Team Blog
- Expectation of Originality
- Product Backlog

## Interview: Stakeholder Information 

###### **Stakeholder #1** 

###### Stakeholder Reference: **"The Fish Connection" (Advanced Hobbyist & Content Creator)** 

Location: Barcelona, Spain 

Interview Date & Time: Friday, June 5, 2026, at 3:45 PM Eastern 

Interview Channel: Video Call (Zoom) 

###### **Stakeholder #2** 

Stakeholder Reference: **Casual Hobbyist “Nikita”** 

Location: Montreal, Canada 

Interview Date & Time: Wednesday, June 11, 2026, at 6:00 PM Eastern 

Interview Channel: In-Person Interview 

###### **Stakeholder #3** 

###### Stakeholder Reference: **Casual Hobbyist “Alyssa”** 

Location: Montreal, Canada 

Interview Date & Time: Tuesday, June 10, 2026, at 5:15 PM Eastern 

Interview Channel: In-Person Interview 

**N.B:** To maintain the privacy and confidentiality of the interviewees, the names listed in the stakeholder references are fake. 

## Interview: Consent Form 

COEN 390 / ELEC 390 – Computer and Electrical Engineering Product Design Project 

###### **Course Section:** Summer 2026 

###### **Project Title: Automated Smart Aquarium Monitoring System** 

###### **Purpose of the Research** 

You are being invited to participate in a stakeholder requirement interview conducted by an undergraduate engineering team at Concordia University. The objective of this project is to elicit user requirements to guide the software development and hardware integration of an Android application connected to external water-quality monitoring sensors. Your insights will directly help our product backlog, threshold logic, and user interface design. 

###### **Data Privacy and Confidentiality** 

To protect your privacy and maintain absolute confidentiality, your full legal name, contact information, and personal identity will remain completely anonymous in all course submissions. In compliance with course criteria, stakeholders will be referred to strictly by a first name or a generic operational title. 

###### **Voluntary Participation** 

Your participation in this requirement elicitation process is entirely voluntary. You reserve the right to decline to answer any specific inquiry or terminate the interview at any moment without penalty. 

###### **Consent Declaration** 

By engaging with our interview script, you acknowledge that you understand the academic scope of this project and consent to your anonymised insights being aggregated and summarized in our engineering documentation. 

**X Please Sign Here** 

**Before signing this document, please make sure that you have read all the clauses listed above.** 

## Interview: Global Script Template 

- **Question 1 (Core Aquarium Parameter):** In your operational experience, what is the single most frustrating water parameter to maintain, or the one that causes the most sudden environmental emergencies (e.g., pH shifts, temperature drops) in the aquarium? 

- **Question 2 (Current Tracking Methodology):** How do you currently monitor your aquarium’s water quality parameters? Do you utilize manual chemical test kits, stand-alone digital meters, or rely on visual observations of the ecosystem? 

- **Question 3 (App Notification Thresholds):** If a mobile application were configured to send you a push notification regarding a parameter deviation, what specific threshold shifts would you classify as 'critical' enough to warrant an instant alert? 

- **Question 4 (Hardware Integration):** Since our system requires a physical external sensor device submerged in the tank, where would a device like this be least intrusive to your setup? (e.g., floating on the surface, clipped securely to the filter rim, or completely submerged at the substrate level)? 

## Interview: Three Most Important Takeaways 

##### **Stakeholder #1** 

- 1) Geographic & Seasonal Environmental Variables: Water baseline characteristics fluctuate heavily based on regional infrastructure and season. For example, tap water in metropolitan Barcelona is natively hard, and municipalities heavily increase chemical/chlorine treatments during summer months, requiring targeted filtration countermeasures. 

- 2) The "Balanced Ecosystem" Market Philosophy: Advanced hobbyists actively prioritize long-term biological stability over daily chemical micromanagement. Consequently, a continuous data tracking log is not highly valued by this segment for routine operations, as they view a mature aquarium as a self-sustaining ecosystem. 

- 3) Pivoting App Value to Emergency Fallbacks: The primary commercial value of the system for advanced aquarists lies in acting as an emergency "safety net" rather than a daily dashboard. The software must prioritize instant, automated alert triggers for catastrophic, unpredictable hardware failures (such as a broken heater causing a temperature plunge, severe evaporation spikes, or rapid nitrate spikes following livestock additions). 

##### **Stakeholder #2** 

- 1) Ammonia Is A Critical Priority: Ammonia is considered to be the most important parameter in aquatic regulation. This is due to its rapid build up from both food and waste; threatening fish health and room air quality. Immediate attention/notification is greatly desired when shifts occur. 

- 2) Device Placement Should Not Be Visually Obscured: Keeping the electrical sensors visible within the tank (to the user/hobbyist) is crucial for maintenance. This clear line of sight allows you to quickly diagnose and troubleshoot any hardware issues that might arise. 

- 3) Changes Happen Rapidly So Notification Should Be Rapid: There should be notifications when sensor connection is severed as there can be a cause for unknown parameter changes, which may be addressed immediately. 

##### **Stakeholder #3** 

- 1) Placement Must Prioritize High Water Flow And Low Visibility: The ideal device location is fully submerged and placed where water circulation is strong; This ensures the sensors get an accurate, representative reading of the whole tank. It is also very important to keep the sensors out of the fishes’ visibility, as this can disturb their natural environment/behaviours. 

- 2) Alerts Should Only Be For Emergencies: The user does not want to be disturbed for minor changes in parameters. There should be an option to only get notifications for the most critical changes. 

- 3) Temperature Is A Top Priority Parameter: Temperature can fluctuate rapidly due to factors such as external weather and power outages, instantly causing fish stress and depleting tank oxygen levels. 

## Ethical Dimensions 

**1. Privacy Issues:** Although water-quality readings collected by the app are not sensitive in themselves, our system will keep data that can reveal information about the user's home. Sensor timestamps and alert activity can reveal daily routines and whether a home is occupied. The device will also store the user's home Wi-Fi credentials. These sensitive information can cause extreme consequences if leaked. Strong system security (encrypted storage and transmission) will need to be in place to prevent data leaks, the app should be password-protected, and data must never be sold or shared with other parties without consent. The type of data being tracked will be clearly disclosed. We will collect only what is needed for tank monitoring (excluding location or personal details). 

**2. Potential for Misuse and Security:** Poorly secured IoT devices are a known attack target as they can be hijacked into botnets. While the device only monitors water, a compromised sensor could leak occupancy patterns or network access. We will mitigate this by ensuring no default passwords, no open ports, and signed firmware updates. We will also try to offer a fully local option (MQTT on LAN) for app users who don't want cloud connectivity. 

**3. Animal Protection:** Users will rely on the app to safeguard living aquatic animals; therefore, sensor failures or inaccurate readings could lead to the animal's death. So, the app and sensors will require proper calibration. Sensors will be given a replacement date, and the app will remind users to calibrate and visually verify the sensors' conditions. If a sensor dies or the device goes offline, the app will raise a fault alert immediately rather than showing stale good data. The app will also state clearly what is and is not measured by the device and prompt users to log manual test-kit results, preventing a false sense of security. 

**4. Transparency and Accountability:** Users will be made aware of how the data is collected and how accurate each reading is. It will be made clear that the product's intended purpose is to collect data and alert users; it is a decision aid, not a guarantee of fish safety. Alert thresholds (pH, temperature, etc.) are user-configurable, with system defaults that favor false alarms over missed emergencies. However, the final care decision rests fully with the user. 

**5. Data ownership and third parties:** Cloud notifications rely on third-party infrastructure (e.g., Firebase). Users will be told what is stored, for how long, and data will never be sold or used beyond the service. Users can delete their data and run the device offline if they prefer. 

**6. Environmental:** The product involves electronics, probes with consumable parts, and batteries. We will use replaceable probes and will document proper battery disposal. 

*[Figure: OCR dump removed — see original PDF for image.]*

Form ENCS-SAS (03/04) 

### Faculty of Engineering and Computer Science Expectations of Originality 

This form has been created to ensure that all students in the Faculty of Engineering and Computer Science comply with principles of academic integrity prior to submitting coursework to their instructors for evaluation: namely reports, assignments, lab reports and/or software. All students should become familiar with the University’s Code of Conduct (Academic) located at <u>http://web2.concordia.ca/Legal_Counsel/policies/english/AC/Code.html</u> 

**Please read the back of this document carefully before completing the section below. This form must be attached to the front of all coursework submitted to instructors in the Faculty of Engineering and Computer Science.** 

###### ELEC/COEN 390 - BB DR. WAHAB HAMOU-LHADJ **Course Number:** <u>_______________________________</u> **Instructor:** <u>_______________________________</u> 

###### **Type of Submission (Please check off responses to both a & b)** 

a. __ Report __ Assignment __ Lab Report __ Software 

b. __ Individual submission __ Group Submission (All members of the team must sign below) 

Having read both sides of this form, I certify that I/we have conformed to the Faculty’s expectations of originality and standards of academic integrity. 

BILAL SAMEE 27286295 Name: _______________________ ID No: __________ (please print clearly) DOMINIQUE REYNOLDS-SANDY 40241168 Name: _______________________ ID No: __________ (please print clearly) NAVRAJ JHAJJ 40129282 Name: _______________________ ID No: __________ (please print clearly) 

BILAL SAMEE 27286295 12-JUN-2026 Name: _______________________ ID No: __________ Signature: ___________________Date: ________ (please print clearly) DOMINIQUE REYNOLDS-SANDY 40241168 12-JUN-2026 Name: _______________________ ID No: __________ Signature: ___________________Date: ________ (please print clearly) NAVRAJ JHAJJ 40129282 12-JUN-2026 Name: _______________________ ID No: __________ Signature: _________________ _ Date: ________ (please print clearly) ARMAAN KHAN 40235610 12-JUN-2026 Name: _______________________ ID No: __________ Signature: __________________ Date: ________ (please print clearly) DIPITA SINHA 40273009 12-JUN-2026 Name: _______________________ ID No: __________ Signature: __________________ Date: ________ (please print clearly) Name: _______________________ ID No: __________ Signature: __________________ Date: ________ (please print clearly 

###### **<mark>Do Not Write in this Space – Reserved for Instructor</mark>** 

 

#### **EXPECTATIONS OF ORIGINALITY & STANDARDS OF ACADEMIC INTEGRITY** 

###### **<u>ALL SUBMISSIONS must meet the following requirements:</u>** 

1. <u>The decision on whether a submission is a group or individual submission is determined by the instructor.</u> Individual submissions are done alone and should not be identical to the submission made by any other student.  In the case of group submissions, all individuals in the group must be listed on and must sign this form prior to its submission to the instructor. 

2. All individual and group submissions constitute original work by the individual(s) signing this form. 3. Direct quotations make up a very small proportion of the text, i.e., not exceeding 5% of the word count. 4. Material paraphrased from a source (e.g., print sources, multimedia sources, web-based sources, course notes or personal interviews) has been identified by a numerical reference citation. 

5. All of the sources consulted and/or included in the report have been listed in the Reference section of the document. 

6. All drawings, diagrams, photos, maps or other visual items derived from other sources have been identified by numerical reference citations in the caption. 

7. No part of the document has been submitted for any other course. 8. Any exception to these requirements are indicated on an attached page for the instructor’s review. 

###### **<u>REPORTS and ASSIGNMENTS must also meet the following additional requirements:</u>** 

1. A report or assignment consists entirely of ideas, observations, information and conclusions composed by the student(s), except for statements contained within quotation marks and attributed to the best of the student’s/students’ knowledge to their proper source in footnotes or references. 

2. An assignment may not use solutions to assignments of other past or present students/instructors of this course or of any other course. 

3. The document has not been revised or edited by another student who is not an author. 4. For reports, the guidelines found in Form and Style, by Patrick MacDonagh and Jack Borden (Fourth Edition: May 2000, available at <u>http://www.encs.concordia.ca/scs/Forms/Form&Style.pdf) have been used</u> for this submission. 

###### **<u>LAB REPORTS must also meet the following requirements:</u>** 

1. The data in a lab report represents the results of the experimental work by the student(s), derived only from the experiment itself.  There are no additions or modifications derived from any outside source. 

2. In preparing and completing the attached lab report, the labs of other past or present students of this course or any other course have not been consulted, used, copied, paraphrased or relied upon in any manner whatsoever. 

###### **<u>SOFTWARE must also meet the following requirements:</u>** 

1. The software represents independent work of the student(s). 

2. No other past or present student work (in this course or any other course) has been used in writing this software, except as explicitly documented. 

3. The software consists entirely of code written by the undersigned, except for the use of functions and libraries in the public domain, all of which have been documented on an attached page. 

4. No part of the software has been used in previous submissions except as identified in the documentation. 

5. The documentation of the software includes a reference to any component that the student(s) did not write. 

6. All of the sources consulted while writing this code are listed in the documentation. 

**_Important:  Should you require clarification on any of the above items please contact your instructor._** 

##### Product Backlog 

| Story ID | Priority | Story Title | Epic | User Story | Story Points | Sprint | Status | Conversation | Confirmation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| US-01 | Must-Have | Live Status Dashboard | Dashboard | As a curious user, I want to access a dashboard showing the live statuses and readings of all sensors, so that I can stay informed at all times on how my system is doing. | 8 | 1 | TODO | This could be the landing page of the app, playing both as a good first feature and as a convenience for the user. | Does the dashboard show all sensors? Are the sensor readings live-updating without any user input required? |
| US-02 | Must-Have | Threshold Triggered Notifications | Monitoring | As a careful user, I want the option to receive customizable threshold-triggered notifications, so that I can attend to the aquarium if my thresholds are exceeded. | 5 | 1 | TODO | We could make sensor customization possible by simply clicking the sensor on the dashboard. | Can the user change threshold conditions? Does the user receive notifications when threshold conditions are met? |
| US-03 | Must-Have | Sensor Reading Tooltips | Dashboard | As a layman hobbyist, I want to be able to view descriptions of what sensor readings mean, so that I can better understand what I'm seeing. | 2 | 1 | TODO | Sensor readings should have an (i) tooltip that, when clicked, displays a tooltip message explaining how to interpret the reading. | Does clicking the (i) show the right tooltip? Are all tooltips intuitive? |
| US-04 | Must-Have | Privacy Page | Misc Pages | As a user that is privacy-conscious, I want none of my data to be shared with anyone, so that I can rest easy. | 2 | 1 | TODO | A privacy activity should be clearly accessible and should explain to the user what data is collected and how it is used. | Is the privacy activity easy to find? Is it concise? |
| US-05 | Must-Have | Contact Page | Misc Pages | As a newbie, I want the option to contact support, so that I can rest easy if I face issues with the app and/or the hardware. | 2 | 1 | TODO | A simple contact form that sends an email to our organization suffices. The user should be able to select between reporting an issue or giving feedback. | Does the contact form send both issues and feedback? Are they received by us? Does the user get a “sent” toast? |
| US-06 | Must-Have | Hardware Setup & Pairing Wizard | Aquarium Templates & Instances | As a newbie, I want a guided flow in app to connect my hardware and create my first aquarium, so that I can get running without reading docs. | 5 | 1 | TODO | Should launch automatically on first install since no aquariums are defined yet. Walks the user through entering the aquarium "address", verifying the connection, and naming the aquarium. Could offer a newbie ready template at the end. | Does the wizard launch on first install? Can a user pair hardware and create an aquarium end-to-end? Are connection errors communicated clearly? Can the wizard be re-run for additional aquariums? |
| US-07 | Must-Have | Sensor & Hub Offline Alerts | Monitoring | As a careful user, I want to be notified when a sensor or the system stops reporting values, so that silent failures don't go unnoticed. | 5 | 1 | TODO | Threshold alerts are useless if a dead sensor silently stops reporting. Dashboard should visually flag stale readings (e.g., greyed out with a "last seen" timestamp). Timeout duration before a sensor counts as offline could be configurable. | Does a disconnected sensor trigger a notification? Does a disconnected hub trigger a notification? Does the dashboard flag stale readings? Is the offline timeout configurable? |
| US-08 | Must-Have | Historical Sensor Data Retention | Data Handling | As a savvy user, I want the option to enable and access per-sensor historical monitoring data of my system, so that I can study trends and diagnose complicated problems. | 12 | 2 | TODO | Could be accessible from the dashboard by clicking on the sensor. Can be shown in the form of a graph with selectable 1d, 7d, 1m, 3m, 6m, 1y. In settings, users can enable this and define how much data to retain and for which sensors. Should show an average reading value associated with the selectable time window. Should highlight spikes on graphs if feasible. Should be able to download sensor history as a plaintext report. | Can a user enable history per-sensor? Can users define a retention time window? Is the retention time window respected? Does the sensor retain data when enabled? Does the graph show up for sensors with enabled history? Does nothing show up for sensors with disabled history? |
| US-09 | Must-Have | Multiple Aquarium Instances | Aquarium Templates & Instances | As a savvy user, I want the option to define aquariums and switch between them, so that I can have multiple aquarium systems interface with my app. | 8 | 2 | TODO | Probably should be selectable from the header bar of the app. The aquarium "address" will anyways be used to connect the app to the hardware. First-install should start with no aquariums defined. Advanced settings for defining aquariums should be collapsed unless the user explicitly wants them. | Can users define an aquarium? Can users define multiple aquariums? Can users select between aquariums? Are settings and reports of the selected aquarium respected? |
| US-10 | Must-Have | Notification History | Monitoring | As a busy user, I want an in-app log of past alerts, so that I can review notifications I missed or dismissed. | 3 | 2 | TODO | Should list all sent notifications (threshold, offline, time-triggered) with timestamp, sensor, and threshold level/color. Filterable per aquarium and per sensor. Retention could follow the sensor history settings. | Are all notification types logged? Can the log be filtered by aquarium and sensor? Do entries show timestamp and trigger reason? |
| US-11 | Must-Have | Maintenance Mode / Snooze Alerts | Monitoring | As a user doing an aquarium water change, I want to temporarily pause notifications per aquarium, so that expected parameter swings don't spam me. | 3 | 2 | TODO | Toggle from the dashboard or aquarium header. Should auto-resume after a selectable duration (15m, 30m, 1h, 2h) so users can't forget alerts off. Dashboard should clearly show when maintenance mode is active. | Can the user enable maintenance mode per aquarium? Are notifications suppressed while active? Does it auto-resume after the selected duration? Is the active state clearly visible? |
| US-12 | Must-Have | Multiple Aquarium Templates | Aquarium Templates & Instances | As a savvy user, I want the option to define aquarium templates for use in defining new aquariums, so that I can easily define new aquariums with old preferences. | 5 | 2 | TODO | Basically aggregates things like threshold levels and other settings. There should be newbie-ready profiles defined as well. | Can users define aquarium templates? Can a user create an aquarium from an aquarium profile? Can aquarium profiles be duplicated? Can aquarium profiles be deleted? |
| US-13 | Must-Have | Customizable Threshold Levels | Monitoring | As a savvy user, I want the option to define threshold levels and actions associated with them, so that I can receive more complicated and meaningful responses. | 3 | 3 | TODO | Dashboard status can show current threshold level and color. Threshold-triggered notifications can show current threshold level and color. | Can users define multiple threshold levels for every sensor? Are threshold levels respected in that their threshold-triggered notifications are pushed? |
| US-14 | Must-Have | Additional Emails to Notify | Monitoring | As a user that travels a lot, I want to be able to add emails to be sent notifications, so that they can tend to my aquarium if needed. | 2 | 3 | TODO | Every notification should be sent to these emails as well. If feasible, maybe have a user account system and allow users to add users so they can use the app? | Can emails be added to the list? Can emails be removed from the list? Does every notification get sent as an email? |
| US-15 | Nice-Have | Link Google Account and Backup Data | Data Handling | As a careful user, I want the option to backup my sensor history data to my Google account so that I can be at ease if my phone ever dies. | 8 | 3 | TODO | Probably should be automatic. Probably should be enabled from settings after a Google account is linked. Probably should be selectable per sensor. Backup should not occur when on mobile data connection. Backup should occur periodically. All customizable and acquired data should be backed up. | Can users link Google accounts? Can users enable backup for a select number of sensors? Does data for these sensors get backed up automatically? Does backup never happen on mobile data? Can users change the backup period? Does backup respect the sensor's history window? |
| US-16 | Nice-Have | Date-Time Triggered Notifications | Monitoring | As a busy user, I want the option to receive customizable time-triggered notifications, so that I can stay on top of my periodic aquarium-related tasks. | 3 | 3 | TODO | This isn't necessary, as it kind of takes alarm-clock functionality into the app. But it would be nice to have everything aquarium-related achievable by our app, in one place. | Can the user define multiple time-triggered notifications? Do all time-triggered notifications for all sensors trigger successfully? |
| US-17 | Nice-Have | Events Log | Monitoring | As a user, I want the option to indicate additions to and removals from the aquarium ecosystem, so that I can better identify when events or issues happen and diagnose them. | 5 | 3 | TODO | Once again, arguably bring a note/log app functionality into ours. But it's nice to have everything in one place. Should be UX friendly: a form with buttons, not just a log window. | Can users log additions? Can users log removals? Do logged events integrate into graphs? |
| US-18 | Nice-Have | Compare Two Aquarium Histories | Data Handling | As a user with multiple aquariums, I want the option to compare my different aquariums, so that I can glean insights from my different setups for improvement. | 5 | 3 | TODO | Requires both the additions/removals functionality and the history functionality. | Can users compare two aquariums? Does it show up in a UX friendly manner? |
| US-19 | Nice-Have | Encrypt All Data In-Transit & At-Rest | Data Handling | As a user that is security-conscious, I want all my data to be encrypted at rest and in transit, so that I can rest easy. | 5 | 3 | TODO | Nice to have since none of the data involved is sensitive in any way. | Is data encrypted at rest? Is data encrypted in transit? |
| US-20 | Nice-Have | Sensor Calibration Workflow | Monitoring | As a savvy user, I want guided device calibration, so that my readings stay trustworthy. | 3 | 3 | TODO | pH and similar probes drift over time. Step-by-step guided flow per sensor type, with a configurable reminder period. Last-calibrated date could show in the sensor detail view. | Can a user run a guided calibration per sensor? Can reminder periods be configured? Is the last calibration date displayed? |
