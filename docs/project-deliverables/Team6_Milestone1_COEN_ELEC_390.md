## Computer and Electrical Engineering Product Design Project COEN/ELEC 390

TEAM 6

Milestone 1

|Armaan Khan|40235610|
|-|-|
|Bilal Samee|27286295|
|Dipita Sinha|40273009|
|Dominique Reynolds-Sandy|40241168|
|Navraj Jhajj|40129282|

Team Project

29 May, 2026

Dr. Wahab Hamou-Lhadj

## Table of Contents

- Opportunity Statements
- Evaluation of Opportunity Statements
- Ranking of Opportunity Statements
- Mission Statement: Aquarium Monitor
- Team Blog
- Expectations of Originality Form

## Opportunity Statements

1. Aquarium Monitor: Develop an aquarium management application for hobbyists and professional aquarium owners, which continuously monitors water quality metrics (salinity, pH, temperature, water level) and sends warning notifications if any readings rise above or drop below the recommended levels tailored to specific aquatic species. The app will also maintain a log of daily readings so users can monitor trends and adjust the water conditions accordingly.
2. Sleep Quality Tracker & Environment Optimizer: Develop a sleep tracking and optimization system for individuals suffering from poor sleep quality, which tracks and correlates bedroom ambient metrics (CO₂, humidity, temperature, light) with physical sleep metrics (heart rate, movement, oxygen pulse) to identify sleep disturbance patterns, find the optimal bedroom environment for each user, deliver data-driven insights and AI-based personalized recommendations for optimizing sleep environments.
3. Clean Environment Monitoring System: Develop a high-precision environmental monitoring and control system for technicians and researchers working in integrated circuit labs and test facilities, which continuously monitors temperature, humidity, air quality, and particulate levels to ensure strict environmental compliance and alerts operators when any parameter drifts outside acceptable thresholds.
4. Shower Water Quality & Usage Tracker: Develop a smart shower water monitoring application for individuals with sensitive skin or hair health concerns and eco-conscious consumers, which tracks water hardness, temperature, shower duration, and volume of water used per shower to provide real-time water quality alerts and shower filter installation or replacement notifications. The application will also log shower water usage data to generate daily environmental and financial cost reports, which can help low-income individuals (e.g., students) manage their water bills.
5. Athlete Biometrics Monitor: Develop a real-time biometrics monitoring application for athletes, sports coaches and fitness enthusiasts, which tracks key physiological metrics (such as heart rate, oxygen pulse, etc.) during training sessions and sends warning notifications if any readings rise above or drop below recommended safe levels. The app will also maintain a log of daily training sessions so users can monitor fitness trends and adjust their training intensity accordingly.

6. Grocery Cost Optimizer: Develop a grocery price comparison application for students, budget-conscious consumers and small business owners, which aggregates and matches comparable food items across local stores based on the scanned barcode to find similar items at lower prices, helping users optimize their grocery spending and reduce expenses.
7. Smart Pill Dispenser: Develop a weight-sensing smart pill dispenser for elderly patients struggling with managing timely medication consumption and their caregivers and healthcare providers, which verifies actual pill consumption via weight changes and computer vision, sends dose reminders and missed-dose alerts to both patients and caregivers, logs timestamped pill consumption data, and provides doctors with a reviewable medication consumption history.

## Evaluation of Opportunity Statements

## Product 1: Aquarium Monitor

## Pros:

1. A clear, passionate target market since aquarium hobbyists are usually willing to spend money on premium solutions.
2. Low competitive intensity in the market.
3. Straightforward sensor integration makes the hardware product highly feasible within our existing skillset, capabilities, and project deadline.

## Cons:

1. Relatively small total market size as the product is specifically geared towards aquarium hobbyists; therefore, there is limited mainstream appeal beyond hobbyists.
2. Sensors may require frequent maintenance and continuous calibration due to continuous water exposure; high risk of hardware failure due to water seepage.

## Product 2: Sleep Quality Tracker & Environment Optimizer

## Pros:

1. Large and growing target consumer market since people of all ages in every sector can benefit from this product; Health and wellness is a major market.
2. Highly innovative and novel idea. Most of the available sleep quality tracker apps are generalized, but this product provides a unique edge with personalized sleep optimization and insights tailored to each user, not just a general suggestion.

## Cons:

1. Correlating biometric sleep data with environmental data accurately is technically complex and requires correct AI integration for personalized data-driven suggestions.
2. High hardware development complexity due to integrating multiple (> 6) distinct sensors for two separate purposes (environmental data + biometric data) using one microcontroller board, making it less feasible for our team.

## Product 3: Clean Environment Monitoring System

## Pros:

1. Very well-defined professional target market with enterprise clients and low competition.
2. More affordable than existing market products, giving it a positive edge in the market’s competitive intensity.
3. Centralizes many complicated clean-room monitoring systems into one singular application for more efficient data acquisition.

## Cons:

1. Proximity-dependent data acquisition may be limited (per sq. inch vs per sq. foot), and measurement precision for strict industry adherence may be difficult to guarantee.
2. Narrow market since the target consumer is limited to labs and industrial facilities.
3. Significant liability if a system failure or delayed notification ruins ICs, ASICs, etc.

## Product 4: Shower Water Quality & Usage Tracker

## Pros:

1. Broad consumer market with strong appeal to people of all ages concerned with skin and health issues or water usage with high growth.
2. Addresses a broad range of issues: skin and hair health, eco-consciousness, and financial savings value proposition that directly maps to utility bill reduction; high market value.

## Cons:

1. Risk of hardware failure due to water contamination or high levels of humidity.
2. Physical installation at the showerhead adds high hardware complexity as well as difficulty in usability.
3. High market competition from cheap, purely mechanical, basic shower filters and products.

## Product 5: Athlete Biometrics Monitor

## Pros:

1. Large, growing market with a highly defined target audience of athletes and coaches.
2. Real-time alert mechanics for heart rate and safety thresholds are technically straightforward to map out, easy to demonstrate, and test.

## Cons:

1. Connecting the sensors to the user will prove difficult and uncomfortable; implementing a wearable form is difficult to prototype properly without manufacturing a custom chip, which is extremely complex given the scope and deadline of the project.
2. Very intense market competition dominated by massive commercial fitness trackers such as Apple Watch, Fitbit, etc.; is less novel and innovative.

## Product 6: Grocery Cost Optimizer

## Pros:

1. Very large and relatable target market, especially students and budget-conscious families, in the context of inflation at present.
2. High immediate utility; decrease in purchase deliberation time of consumers.
3. No specialized hardware sensors are needed except a camera, which eliminates the overhead, costs, and manufacturing constraints of physical hardware and makes it easier to implement.

## Cons:

1. Massive technical challenge in scraping, maintaining, and syncing accurate, real-time localized pricing data across multiple competitive grocery networks; significant backend challenge.
2. Limited store availability depending on the region of residence.
3. No IoT/embedded component, which may be a poor fit for this course’s focus.
4. High market competition from available commercial platforms such as Flipp, Reebee, etc.

## Product 7: Smart Pill Dispenser

## Pros:

1. High impact product proposition for elderly people; strong focused target market with multi-stakeholders (patient, caregivers, healthcare providers).
2. More affordable than the leading market product.
3. Leads to an increase in efficiency of patient care.

## Cons:

1. Extremely difficult to precisely and accurately implement the weight system to detect a single pill insertion or removal; the available sensor in the provided sheet “Load Cell with HX711 AD load cell amplifier. 1KG” cannot precisely detect weight changes in grams or milligrams.
2. High regulatory and liability sensitivity due to strict medical data privacy regulations and security constraints regarding patient health information.

## Ranking of Opportunity Statements

In order to evaluate and select a final product, each product opportunity is ranked from 1 (Poor) to 5 (Excellent) across 9 key criteria. The criteria are listed below:

1. Market Size: The total estimated revenue potential of the target market (units/year \* average price). This criterion takes into account the product’s market attractiveness as well.
2. Market Growth: The projected rate at which the target market is expanding every year.
3. Competitive Intensity: How easy it will be to compete against existing competitors already in this market. For this criterion, the lesser the competition, the greater the chance for product success. Therefore, the most competitive market is ranked 1, and the least competitive market is ranked 5.
4. Team’s Market Knowledge: The team’s understanding of the target customers, their needs, and the target market.
5. Team’s Technological Knowhow: The team's existing familiarity with the technologies and sensors required to build the product.
6. Fit with Existing/Future Products: How well the product complements other existing or potential product lines or platforms.
7. Fit with Existing Capabilities: How closely the product’s required hardware and software align with the team's available tools, sensors, and skill set. This criterion also takes into account the overall complexity and feasibility of completing a functional prototype within a single semester.
8. Patent Potential: The degree to which the product's novel features could be protected from competitors through patents or other barriers.
9. Access to Customers: The ease with which the team can reach and interview target users and stakeholders for validation and feedback.

The ranking of the product opportunities based on these 9 criteria is summarized below:

*Table 1: Ranking of Opportunity Statements*

| Criterion ↓ | Aquarium Monitor | Sleep Quality Tracker | Clean Environment Monitor | Shower Water Quality Tracker | Athlete Biometrics Monitor | Grocery Cost Optimizer | Smart Pill Dispenser |
| --- | --- | --- | --- | --- | --- | --- | --- |
|Market Size|3|5|2|5|4|5|3|
|Market Growth|4|4|3|4|4|3|4|
|Competitive Intensity (1 → most competitive)|3|3|3|2|1|2|3|
|Team’s Market Knowledge|5|4|2|4|3|3|2|
|Team’s Technological Knowhow|5|4|5|4|3|3|3|
|Fit w/ Existing or Future Products|5|5|4|4|3|3|4|
|Fit w/ Existing Capabilities|5|2|5|3|3|3|2|
|Patent Potential|3|3|3|3|2|2|3|
|Access to Customers|4|5|3|5|4|5|3|
|Total (/45)|37|35|30|34|27|29|26|
|Rank|1st|2nd|4th|3rd|6th|5th|7th|

From the table above, we can see that taking into account all criteria, the Aquarium Monitor ranks first among the seven product opportunities.

We have chosen the Aquarium monitoring system as our first-ranked choice. Although this product may have a smaller consumer reach in comparison to the 2nd and 3rd ranked choices (Sleep Quality Tracker & Shower Quality Tracker), the team’s technological and market knowledge will accelerate profitability, decrease developmental risks from hardware failure, and guarantee a higher quality product. Furthermore, the potential for integration with already existing monitoring devices is very high, which makes this product much more appealing than the other choices listed.

## Mission Statement: Aquarium Monitor

## Product Description:

A monitoring app that tracks the pH, temperature, and water level of aquariums.

## Benefit Proposition:

Aquarium owners need to track several water parameters to keep their fish healthy. If these parameters are not met, it could endanger the fish. Typically, owners must perform physical water tests and record the information in a logbook. The app will fully automate this process by sending the data directly to the user’s devices. In addition, the app will send a warning message if any parameters fall below or rise above the desired values.

## Key Business Goals:

The app will require sensors totalling $40. We plan to sell the app for 50$ yielding a 10$ per unit sold gross margin. Our team will spend 300 hrs, at a rate of 40$/hr. We will need to sell 240 units to break even.

## Target Market:

The target market consists of aquarium owners and hobbyists looking to enter the hobby. The app will provide an easier entry point to the hobby by automating the tracking of water parameters.

## Assumptions:

Assumption 1: Users will be willing to attach the sensors to their tanks.

Assumption 2: Aquatic species-specific threshold data will be sourced from established aquaculture and fishkeeping databases, which are assumed to be correct.

## Constraints:

Constraint 1: There are different types of aquariums (freshwater, saltwater, reef, etc.), each having its own needs. The prototype will target a subset of the most common freshwater species for initial validation, with saltwater species support planned as a future extension.

## Stakeholders:

1. Aquarium business owners
2. Hobbyists for their personal tanks
3. Suppliers for the sensors

## Team Blog

*(Team blog hours table could not be recovered cleanly from the PDF conversion. See the original Milestone 1 PDF for the completed ENCS team blog table.)*

## Expectations of Originality Form

## Faculty of Engineering and Computer Science Expectations of Originality

This form has been created to ensure that all students in the Faculty of Engineering and Computer Science comply with principles of academic integrity prior to submitting coursework to their instructors for evaluation: namely reports, assignments, lab reports and/or software. All students should become familiar with the

University’s Code of Conduct (Academic) located at http://web2.concordia.ca/Legal_Counsel/policies/english/AC/Code.html

Please read the back of this document carefully before completing the section below. This form must be attached to the front of all coursework submitted to instructors in the Faculty of Engineering and Computer Science.

ELEC/COEN 390 - BB

Course Number: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_

Type of Submission (Please check off responses to both a & b)

a. \_\_ Report \_\_ Assignment

b. \_\_ Individual submission

Having read both sides of this form, I certify that I/we have conformed to the Faculty’s expectations of originality and standards of academic integrity.

BILAL SAMEE

27286295

Name: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ ID No: \_\_\_\_\_\_\_\_\_\_ Signature: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_Date: \_\_\_\_\_\_\_\_

(please print clearly)

DOMINIQUE REYNOLDS-SANDY

40241168

Name: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ ID No: \_\_\_\_\_\_\_\_\_\_ Signature: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_Date: \_\_\_\_\_\_\_\_

(please print clearly)

40129282

NAVRAJ JHAJJ

Name: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ ID No: \_\_\_\_\_\_\_\_\_\_ Signature: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ \_ Date: \_\_\_\_\_\_\_\_

(please print clearly)

40235610

ARMAAN KHAN

Name: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ ID No: \_\_\_\_\_\_\_\_\_\_ Signature: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ Date: \_\_\_\_\_\_\_\_

(please print clearly)

DIPITA SINHA

40273009

Name: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ ID No: \_\_\_\_\_\_\_\_\_\_ Signature: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ Date: \_\_\_\_\_\_\_\_

(please print clearly)

Name: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ ID No: \_\_\_\_\_\_\_\_\_\_ Signature: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ Date: \_\_\_\_\_\_\_\_

(please print clearly)

Do Not Write in this Space – Reserved for Instructor

DR. WAHAB HAMOU-LHADJ

\_\_ Lab Report

Instructor: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_

\_\_ Software

\_\_ Group Submission (All members of the team must sign below)

25-MAY-2026

25-MAY-2026

25-MAY-2026

25-MAY-2026

25-MAY-2026

1/2

## & STANDARDS OF ACADEMIC INTEGRITY EXPECTATIONS OF ORIGINALITY

## ALL SUBMISSIONS must meet the following requirements:

1. The decision on whether a submission is a group or individual submission is determined by the instructor. Individual submissions are done alone and should not be identical to the submission made by any other student. In the case of group submissions, all individuals in the group must be listed on and must sign this form prior to its submission to the instructor.
2. All individual and group submissions constitute original work by the individual(s) signing this form.
3. Direct quotations make up a very small proportion of the text, i.e., not exceeding 5% of the word count.
4. Material paraphrased from a source (e.g., print sources, multimedia sources, web-based sources, course notes or personal interviews) has been identified by a numerical reference citation.
5. All of the sources consulted and/or included in the report have been listed in the Reference section of the document.
6. All drawings, diagrams, photos, maps or other visual items derived from other sources have been identified by numerical reference citations in the caption.
7. No part of the document has been submitted for any other course.
8. Any exception to these requirements are indicated on an attached page for the instructor’s review.

## REPORTS and ASSIGNMENTS must also meet the following additional requirements:

1. A report or assignment consists entirely of ideas, observations, information and conclusions composed by the student(s), except for statements contained within quotation marks and attributed to the best of the student’s/students’ knowledge to their proper source in footnotes or references.
2. An assignment may not use solutions to assignments of other past or present students/instructors of this course or of any other course.
3. The document has not been revised or edited by another student who is not an author.
4. For reports, the guidelines found in Form and Style, by Patrick MacDonagh and Jack Borden (Fourth Edition: May 2000, available at http://www.encs.concordia.ca/scs/Forms/Form&Style.pdf) have been used for this submission.

## LAB REPORTS must also meet the following requirements:

1. The data in a lab report represents the results of the experimental work by the student(s), derived only from the experiment itself. There are no additions or modifications derived from any outside source.
2. In preparing and completing the attached lab report, the labs of other past or present students of this course or any other course have not been consulted, used, copied, paraphrased or relied upon in any manner whatsoever.

## SOFTWARE must also meet the following requirements:

1. The software represents independent work of the student(s).
2. No other past or present student work (in this course or any other course) has been used in writing this software, except as explicitly documented.
3. The software consists entirely of code written by the undersigned, except for the use of functions and libraries in the public domain, all of which have been documented on an attached page.
4. No part of the software has been used in previous submissions except as identified in the documentation.
5. The documentation of the software includes a reference to any component that the student(s) did not write.
6. All of the sources consulted while writing this code are listed in the documentation.

