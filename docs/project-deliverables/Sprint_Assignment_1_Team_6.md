# Scrum Assignment I — Team 6

COEN/ELEC 390 

## Abstract 

Maintaining an aquarium ecosystem requires tracking of water parameters, a process traditionally done on physical logbooks. This project introduces an automated monitoring system designed to track an aquarium’s key vitals. The integrated solution features a live status dashboard and an alert system that instantly notifies users when vitals deviate from safe, predefined thresholds. 

This report outlines the framework for Scrum Assignment I, establishing a thorough Product Backlog that details features ranging from a privacy page to an events log. Furthermore, it defines the engineering objectives for Sprint 1, which focus on implementing the baseline user interface (main UI design of the dashboard for live readings), establishing the core hardware to software communication, and deploying the start-up of the notification system. Ultimately, this initial phase establishes a hardware and software foundation to support subsequent development sprints, with a focus on the software portion of the project, and gives a baseline for the hardware to connect to and test the system for future sprints. 

## Contents

- List of Figures
- List of Tables
- List of Terms
- 1. Introduction
  - 1.1. Sprint Goal
- 2. Requirements (Version 1)
  - 2.1. Product Backlog
- 3. Design (Version 1)
  - 3.1. Android Application Wireframes
- 4. Sprints (Version 1)
  - 4.1. Sprint 1 Backlog

## List of Figures

- **Figure 1:** Potential navigation bar design
- **Figure 2:** Initial application layout or dashboard
- **Figure 3:** Parameter Information and Advice
- **Figure 4:** Potential push notification design

## List of Tables 

- **Table 1:** Product Backlog expressed at User Story level.
- **Table 2:** Sprint 1 Backlog expressed at Task level. 

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

### 1.1. Sprint Goal 

The goal of Sprint 1 is threefold: 

1. Develop a basic User Interface with a navigation bar to allow the user to see the dashboard displaying live sensor readings and statuses. 

2. Develop a basic alerting system that notifies users when readings cross safe thresholds or a sensor stops reporting data. 

3. Implement the essential 2 supporting pages: privacy page and support/contact page 

Section 2 outlines, among other sprints’ stories, the user stories conceived to precisely fulfill the goal of Sprint 1. These stories are further broken down into tasks (issues) in Section 4. The aspects of the goal of Sprint 1 were selected on the following bases: 

- The navigation bar will be used to navigate pages that will be implemented in future sprints, and the basic pages, expected of any app, can be accessed through the navigation bar. 

- The dashboard serves as the front page of the app and thus a major chunk of the UI, as well as arguably being the activity that the user will visit and interact with the most. 

- The live sensor readings elements are merely an extension of printing sensor readings in the console, while also serving to familiarize the team with how a hardware back-end communicates with a software front-end. 

- Positive notifications, defined as notifications triggered by an online sensor, will establish the core requirement of our application: notifying the user when some threshold is crossed. Part of implementing this will also likely be implementing a reusable notification system. 

- Alerts or Negative notifications, defined as notifications triggered by an offline sensor, are simply an extension of exceptions that would be printed in the console. Negative notifications will trigger alert notifications immediately, helping the user identify hardware issues and detect state data. 

- The Support Activity page will provide usability and reliability, given that the user will be able to contact the support team for critical device issues that require manual intervention. The user will also be able to give feedback on app issues/improvements that will be of help to the developers. 

- The Privacy Activity page will provide reliability and information, and enforce strict privacy policies for the ease of mind of privacy-concerned users. 

By the end of this sprint, the aim is that: 

- The team will have a working app that monitors sensor readings to display live graphical output as well as push notifications to the user. 

- The team will have implemented the basics and will have interfaced with both the hardware and software systems that make up the product. 

- The team will be well-set for the coming stories of Sprint 2, which will likely revolve around aquarium templates and instances, as well as data collection and persistence. 

## 2. Requirements (Version 1) 

At this stage of the project, this section only presents the Product Backlog, shown in Table 1, which tentatively outlines all items, as of this sprint, involved in the realization of our product. 

### 2.1. Product Backlog 

Table 1 shows the Product Backlog at the story-level, meaning that all items are broken down into stories. The **ID** column lists the identifier of the story. The **PRIO** column lists the priority of the story: `MUST` denoting must-have and `NICE` denoting nice-to-have. The **TITLE** column provides a descriptive title for the story. The **EPIC** column classifies the story into the epic it 

belongs to. The **CARD** , **CONVERSATION** and **CONFIRMATION** columns collectively tell the story. **POINTS** assigns a number of story points to the story, **SPRINT** classifies the story under a sprint, and **STATUS** provides the current status of the story. 

| ID | PRIO | TITLE | EPIC | CARD | POINTS | SPRINT | STATUS | CONVERSATION | CONFIRMATION |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| US-01 | MUST | Live Status Dashboard | Dashboard | As a curious user, I want to access a dashboard showing the live statuses and readings of all sensors, so that I can stay informed on my system at any time. | 8 | 1 | TODO | This could be the landing page of the app, playing both as a good first feature and as a convenience for the user. | Does the dashboard show all sensors? Are the sensor readings live-updating without any user input required? |
| US-02 | MUST | Threshold Triggered Notifications | Monitoring | As a careful user, I want the option to receive customizable threshold-triggered notifications, so that I can attend to my aquarium if my threshold is exceeded. | 5 | 1 | TODO | We could make sensor threshold customization possible by simply clicking the sensor on the dashboard. | Can the user change threshold conditions? Does the user receive notifications when threshold conditions are met? |
| US-03 | MUST | Sensor Reading Tooltips | Dashboard | As a layman hobbyist, I want to be able to view descriptions of what sensor readings mean, so that I can better understand what I’m seeing. | 2 | 1 | TODO | Sensor readings should have an information icon that displays a tooltip when clicked, which explains how to interpret readings. | Does clicking the information icon show the right tooltip? Are all tooltips intuitive? Can the tooltip be closed? |
| US-04 | MUST | Privacy Page | Misc Pages | As a user that is privacy-conscious, I want to clearly let it be known that none of my data is shared with anyone, so that I can rest easy using the app. | 2 | 1 | TODO | A privacy activity should be clearly accessible and should explain to the user what data is collected and how it is used. | Is the privacy activity easy to find? Is it concise? |
| US-05 | MUST | Contact Page | Misc Pages | As a user, I want the option to contact support for help, so I can resolve complicated issues with the app and/or the hardware. | 2 | 1 | TODO | A simple contact form that sends an email to our organization suffices. The user should be able to select between reporting an issue or giving feedback. | Does the contact form send both issues and feedback? Are they received by us? Does the user get a “sent” toast? |
| US-06 | MUST | Sensor & Hub Offline Alerts | Monitoring | As a careful user, I want to be notified when a sensor or the system stops reporting values, so that silent failures don’t go unnoticed. | 5 | 1 | TODO | Threshold alerts are useless if a dead sensor silently stops reporting. Dashboard should visually flag stale readings (e.g., greyed out with a “last seen” timestamp). Timeout duration before a sensor counts as offline could be configurable. | Does a disconnected sensor trigger a notification? Does a disconnected hub trigger a notification? Does the dashboard flag stale readings? Is the offline timeout configurable? |
| US-07 | MUST | Aquarium Sensor Hardware Assembly & Enclosure | Hardware | As a user setting up my system, I want the sensors and hub assembled in a safe, water-resistant enclosure that mounts inside or around my aquarium, so that I get accurate readings without damaging my equipment or aquatic animals. | 8 | 2 | TODO | Need to sketch hardware designs using CAD tools, keeping in mind a waterproof enclosure with all sensors, visual appeal, and optimal sensor placements. This user story involves designing and 3D-printing an enclosure/mount, waterproofing, and verifying live readings flow to the back-end. | Are all sensors wired correctly and reporting to the microcontroller? Do live readings reach the dashboard? Is the enclosure water-resistant and safely mountable? Has the 3D-printed housing been fit-tested? |
| US-08 | MUST | Hardware Setup & Pairing Wizard | Aquarium Templates & Instances | As a newbie, I want a guided flow in app to connect my hardware and create my first aquarium, so that I can get started without reading any docs. | 5 | 2 | TODO | Should launch automatically on first install since no aquariums are defined yet. Walks the user through entering the aquarium “address”, verifying the connection, and naming the aquarium. | Does the wizard launch on first install? Can a user pair hardware and create an aquarium end-to-end? Are connection errors communicated clearly? |
| US-09 | MUST | Historical Sensor Data Retention | Data Handling | As a savvy user, I want the option to enable and access per-sensor historical monitoring data of my system, so that I can study trends and diagnose complicated problems. | 12 | 2 | TODO | Could be accessible from the dashboard by clicking on the sensor. Can be shown as a graph with selectable time windows. Users can enable retention per sensor in settings. | Can a user enable history per-sensor? Can users define a retention time window? Is the retention time window respected? |
| US-10 | MUST | Multiple Aquarium Instances | Aquarium Templates & Instances | As a savvy user, I want the option to define aquariums and switch between them, so that I can have multiple aquarium systems interface with my app. | 8 | 2 | TODO | Probably should be selectable from the header bar of the app. Advanced settings for defining aquariums should be collapsed unless the user explicitly wants them. | Can users define an aquarium? Can users define multiple aquariums? Can users select between aquariums? |
| US-11 | MUST | Notification History | Monitoring | As a busy user, I want an in-app log of past alerts, so that I can review notifications I missed or dismissed. | 3 | 2 | TODO | Should list all sent notifications with timestamp, sensor, and threshold level/color. Filterable per aquarium and per sensor. | Are all notification types logged? Can the log be filtered by aquarium and sensor? |
| US-12 | MUST | Maintenance Mode / Snooze Alerts | Monitoring | As a user doing an aquarium water change, I want to temporarily pause notifications per aquarium, so that expected parameter swings don’t spam me. | 3 | 2 | TODO | Toggle from the dashboard or aquarium header. Should auto-resume after a selectable duration (15m, 30m, 1h, 2h). | Can the user enable maintenance mode per aquarium? Are notifications suppressed while active? |
| US-13 | MUST | Multiple Aquarium Templates | Aquarium Templates & Instances | As a savvy user, I want the option to define aquarium templates for use in defining new aquariums, so that I can easily define new aquariums with old preferences. | 5 | 2 | TODO | Basically aggregates things like threshold levels and other settings. There should be newbie-ready profiles defined as well. | Can users define aquarium templates? Can a user create an aquarium from an aquarium profile? |
| US-14 | MUST | Customizable Threshold Levels | Monitoring | As a savvy user, I want the option to define threshold levels and actions associated with them, so that I can receive more complicated and meaningful responses. | 3 | 3 | TODO | Dashboard status can show current threshold level and color. | Can users define multiple threshold levels for every sensor? |
| US-15 | MUST | Notify Additional Emails | Monitoring | As a user that travels a lot, I want to be able to add emails to be sent notifications, so that they can tend to my aquarium if needed. | 2 | 3 | TODO | Every notification should be sent to these emails as well. | Can emails be added to the list? Can emails be removed from the list? |
| US-16 | NICE | Link Cloud Accounts and Backup Data | Data Handling | As a careful user, I want the option to backup my sensor history data to the cloud, so that I can always have my data if my phone ever dies. | 8 | 3 | TODO | Probably should be automatic and enabled from settings after an account is linked. Backup should not occur on mobile data. | Can users link cloud provider accounts? Can users enable backup for selected sensors? |
| US-17 | NICE | Date-Time Triggered Notifications | Monitoring | As a busy user, I want the option to receive customizable time-triggered notifications, so that I can stay on top of my periodic aquarium-related tasks. | 3 | 3 | TODO | Nice to have everything aquarium-related achievable by our app, in one place. | Can the user define multiple time-triggered notifications? |
| US-18 | NICE | Events Log | Monitoring | As a user, I want the option to indicate additions to and removals from the aquarium ecosystem, so that I can better identify when events or issues happen and diagnose them. | 5 | 3 | TODO | Should be a form with buttons, not just a text-editor window. | Can users log additions? Can users log removals? Do logged events integrate into graphs? |
| US-19 | NICE | Compare Two Aquarium Histories | Data Handling | As a user with multiple aquariums, I want the option to compare my different aquariums, so that I can glean insights from my different setups. | 5 | 3 | TODO | Requires both the additions/removals functionality and the history functionality. | Can users compare two aquariums? |

*Table 1: Product Backlog expressed at User Story level.*

## 3. Design (Version 1)

### 3.1. Android Application Wireframes

*[Wireframe figures were lost in PDF→Markdown conversion. See the original Scrum Assignment I PDF for Figures 1–4.]*

## 4. Sprints (Version 1) 

At this stage of the project, this section only presents the Sprint 1 Backlog, shown in Table 2, which outlines the tasks of Sprint 1 required to fulfill all the user stories of Sprint 1. 

### 4.1. Sprint 1 Backlog 

Table 2 shows the Sprint 1 Backlog at the task-level, meaning that all user stories are broken down into smaller assignable tasks. The **STORY ID** column lists the User Story ID, which corresponds to the User Story IDs in the product backlog. This enables the Sprint 1 Backlog to be cross-referenced against the Product Backlog. The **TASK ID** column lists the lowerlevel tasks broken down under each user story. The **TITLE** column provides a descriptive title for each task. The **DESCRIPTION** column provides a brief description of each task. **IDEAL HOURS** provides the estimated time (in hours) required to do the task, **STATUS** provides the current status of the task (e.g.: Planned, In Progress, etc.), and **COMMENTS** lists if the task is hardware-based or software-based and also briefly lists the dependencies if it has any. Finally, **ASSIGNEE(s)** specifies the person/(s) currently assigned to the task. 

| STORY ID | TASK ID | TITLE | DESCRIPTION | IDEAL HOURS | STATUS | COMMENTS | ASSIGNEE(S) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| US-01 | US-01.1 | Dashboard UI (Main Activity Page) | Build the dashboard UI as the app landing page with a card/grid layout, one tile per sensor. Build the navigation bar. | 5 | Planned | Software. Foundation for US-02/03/06. | TBD |
| US-01 | US-01.2 | Sensor Data Integration | Fetch current statuses and readings from the sensors and integrate them into the UI tiles. | 5 | Planned | Software. | TBD |
| US-01 | US-01.3 | Live Auto-Refresh | Auto-update readings every few seconds (polling or web-socket). | 4 | Planned | Software. | TBD |
| US-01 | US-01.4 | Per-sensor Status Indicator | Show status per sensor tile (stable, critical, warning, etc.) | 3 | Planned | Software. | TBD |
| US-01 | US-01.5 | Primary Hardware Integration | Wire physical sensors through the micro-controller so live values flow to the dashboard. | 5 | Planned | Hardware. Pair with US-01.2. | TBD |
| US-02 | US-02.1 | Threshold Configuration UI | Click a sensor on the dashboard to set/edit threshold conditions. | 4 | Planned | Software. Depends on US-01. | TBD |
| US-02 | US-02.2 | Threshold Evaluation Logic | Compare incoming readings against default/configured thresholds and detect when readings exceed safe levels. | 3 | Planned | Software. | TBD |
| US-02 | US-02.3 | Notification delivery | Trigger and send a notification when a threshold condition is exceeded. | 4 | Planned | Software. | TBD |
| US-03 | US-03.1 | Tooltip (i) Component | Add an info (i) icon to each sensor reading card/tile such that, on click, it shows a tooltip explaining how to interpret it. | 3 | Planned | Software. Depends on US-01. | TBD |
| US-03 | US-03.2 | Tooltip Content Writing | Write clear, simple, intuitive descriptions for each sensor reading type. | 2 | Planned | Software. | TBD |
| US-04 | US-04.1 | Privacy Activity Page | Easy-to-find, concise page explaining the app’s privacy policy and which data are collected and how they are used. | 3 | Planned | Software. | TBD |
| US-05 | US-05.1 | Support Activity Page | Support/Contact Form with a toggle feature to select ‘report an issue’ or ‘give feedback’. | 3 | Planned | Software. | TBD |
| US-05 | US-05.2 | Email Confirmation | Send form submission as an email to the support team and display a ‘successfully sent’ toast. | 3 | Planned | Software. | TBD |
| US-06 | US-06.1 | Sensor Offline Detection | Detect when a sensor stops reporting after a default/configurable timeout period. | 4 | Planned | Software. | TBD |
| US-06 | US-06.2 | Sensor Stale Data Flagging | Grey out stale sensor cards/tiles and show a ‘last seen’ timestamp with last valid captured data. | 3 | Planned | Software. | TBD |
| US-06 | US-06.3 | Critical Notification Delivery | Notify the user when a sensor goes offline and notify them to navigate to hardware connection troubleshooting guides. | 3 | Planned | Software. | TBD |
| US-06 | US-06.4 | Hardware Troubleshooting Guide | Write a simple, concise Hardware Troubleshooting guide; will be polished after US-07. | 3 | Planned | Software and Hardware. Depends on US-05. | TBD |
| US-06 | US-06.5 | Configurable Timeout Period | Design UI so that the user can set the timeout duration for a sensor to be flagged offline. | 1 | Planned | Software. | TBD |

**Total Ideal Hours: 61**

*Table 2: Sprint 1 Backlog expressed at Task level.*

