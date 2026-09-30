# Training Center Management: User Guide & Scenarios

This guide covers every use case in the application, one step at a time. It
is written for the people who use the app (administrators, trainers and
students), not for developers.

Every scenario uses the **demo data** that comes with a fresh install, so
you can follow along and see the same results described here.

---

## Contents

0. [Before you start](#0-before-you-start)
1. [Signing in and out](#1-signing-in-and-out)
2. [Administrator scenarios](#2-administrator-scenarios)
3. [Trainer scenarios](#3-trainer-scenarios)
4. [Student scenarios](#4-student-scenarios)
5. [End-to-end walkthrough: from new course to certificate](#5-end-to-end-walkthrough-from-new-course-to-certificate)
6. [Rules at a glance](#6-rules-at-a-glance)
7. [Error messages and what they mean](#7-error-messages-and-what-they-mean)
8. [Troubleshooting and FAQ](#8-troubleshooting-and-faq)

---

## 0. Before you start

### Start the application

```bash
cp .env.example .env
docker compose up --build
```

Open **http://localhost:5173** in your browser.

To start again from a clean demo dataset at any point:

```bash
docker compose down -v
docker compose up --build
```

### Demo accounts

Every demo account uses the password **`ChangeMe123!`**.

| Role | Name | Email | Situation in the demo |
|---|---|---|---|
| Administrator | System Administrator | `admin@tcm.local` | Sees and manages everything |
| Trainer | Tina Trainer | `tina.trainer@tcm.local` | Teaches **Java Fundamentals** |
| Trainer | Tom Teacher | `tom.teacher@tcm.local` | Teaches **React in Practice** (sessions still to come) |
| Student | Sam Student | `sam.student@tcm.local` | Approved on Java, pending request on React, unpaid invoice |
| Student | Sara Sassi | `sara.sassi@tcm.local` | One absence, a partly paid invoice and an overdue invoice |
| Student | Sofia Ben Ali | `sofia.benali@tcm.local` | Finished Java with full attendance and paid in full, so she can be certified |
| Student | Sami Khelifi | `sami.khelifi@tcm.local` | **Inactive** account, so sign-in is refused |

### Demo courses

| Code | Name | Trainer | Price | Capacity | Status |
|---|---|---|---|---|---|
| JAVA-101 | Java Fundamentals | Tina Trainer | 500 | 20 | Published |
| REACT-201 | React in Practice | Tom Teacher | 750 | 15 | Published |
| DATA-301 | Data Analysis with SQL | *(none)* | 900 | 12 | Draft |

### Demo schedule

Dates are relative to the day the database was created.

| Course | Room | Time | Sessions |
|---|---|---|---|
| Java Fundamentals | Room A | 09:00 to 12:00 | 7 days ago (completed), 3 days ago (completed), in 2 days (scheduled) |
| React in Practice | Room B | 14:00 to 17:00 | in 3 days (scheduled), in 6 days (scheduled) |

---

## 1. Signing in and out

### Scenario 1.1: Sign in

1. Go to http://localhost:5173. You are sent to the **Sign in** page.
2. Enter your **Email** and **Password**.
3. Click **Sign in**.
4. You land on your role's dashboard:
   - Administrators go to `/admin`.
   - Trainers go to `/trainer`.
   - Students go to `/student`.

### Scenario 1.2: Wrong password or inactive account

1. Sign in with `admin@tcm.local` and the password `wrong`.
2. The message **"Invalid email or password"** appears.
3. Try again with `sami.khelifi@tcm.local` / `ChangeMe123!`. You get the same message, because Sami's account is inactive.

> For security, the app never says whether the email exists, the password was wrong, or the account is deactivated.

### Scenario 1.3: Sign out

1. Open the user menu in the top bar.
2. Choose **Log out**. You return to the **Sign in** page.

> There is no "Sign up" page. Only an administrator can create accounts (see [2.1](#21-user-management)).
> There is also no way to change your own password. Ask an administrator to reset it ([Scenario 2.1.5](#scenario-215-reset-a-users-password)).

---

## 2. Administrator scenarios

Sign in as **`admin@tcm.local`**. The left navigation shows **Dashboard**,
**Users**, **Courses**, **Students**, **Enrollments**, **Schedule** and
**Payments**.

### 2.0 Admin dashboard

#### Scenario 2.0.1: Read the dashboard

1. Click **Dashboard**. The tiles are grouped into three sections:
   - **People and courses**:
     - **Active students**
     - **Active trainers**
     - **Published courses**
     - **Pending enrollments**
   - **Teaching**:
     - **Upcoming sessions**: scheduled sessions in the next 7 days
     - **Average attendance**: present + late, out of all marked records
     - **Certificates issued**: counted since the 1st of this month
   - **Money**:
     - **Outstanding balance**: everything still owed, across all invoices
     - **Overdue invoices**
2. Click a tile to open the related page. For example, **Active students** opens the Students list.

*What you should see with the demo data:* 3 active students, 2 active
trainers, 2 published courses, 1 pending enrollment (Sam on React), and at
least 1 overdue invoice (Sara on React).

---

### 2.1 User management

#### Scenario 2.1.1: Browse and filter users

1. Click **Users**.
2. Filter the list:
   - Type in **Search by name…** to find someone.
   - Use **All roles** to show only Admins, Trainers or Students.
   - Use **All statuses** to show only Active or Inactive accounts.
3. Use **Previous** / **Next** to move between pages.

#### Scenario 2.1.2: Create a new account

1. On **Users**, click **New User**.
2. Fill in:
   - **First name**, **Last name** and **Email** (required).
   - **Phone** (optional).
   - **Role**: Admin, Trainer or Student.
   - **Temporary password** (required).
3. Click **Save**. The new user appears in the list as **Active**.
4. Give the new person their email and temporary password. They can sign in right away.

*Try it:* create a student `lina.new@tcm.local` with the password
`Welcome123!`. You will use this student in [section 5](#5-end-to-end-walkthrough-from-new-course-to-certificate).

**If something goes wrong:** an email that is already used gives
**"A user with this email already exists"**.

#### Scenario 2.1.3: View or edit a user

1. On **Users**, open the **⋯** menu on a row.
2. Choose **View details** to see the profile in a side panel.
3. Choose **Edit** to change the name, email, phone or role, then click **Save**.

> Editing does not change the password. Use **Reset password** for that.

#### Scenario 2.1.4: Deactivate or reactivate a user

1. On **Users**, open the **⋯** menu on a row and choose **Deactivate**.
2. Confirm in the **Deactivate user?** dialog. It warns that the person "will no longer be able to sign in".
3. The user is marked **Inactive**. If they are signed in, they are cut off at once.
4. To undo, open the menu on the same row, choose **Activate**, and confirm.

*Try it:* reactivate **Sami Khelifi**, then sign in as `sami.khelifi@tcm.local`.

> ⚠️ Nothing stops an administrator from deactivating their own account. Don't do it unless another active admin exists.

#### Scenario 2.1.5: Reset a user's password

1. On **Users**, open the **⋯** menu on a row and choose **Reset password**.
2. Confirm in the **Reset password?** dialog. The user's current password stops working.
3. A **Temporary password** dialog shows a new 12-character password. Copy it and give it to the user.
4. Click **Done**. The password is not shown again.

---

### 2.2 Course management

#### Scenario 2.2.1: Browse courses

1. Click **Courses**.
2. Filter the list:
   - Type in **Search by name or code…**.
   - Type in **Category…** to filter by category.
   - Use **All statuses** to filter by Draft, Published or Archived.

> Administrators see courses in every status. Trainers and students only see **Published** courses in the catalog.

#### Scenario 2.2.2: Create a course

1. On **Courses**, click **New Course**.
2. Fill in:
   - **Code** (must be unique), e.g. `PY-101`.
   - **Name**, e.g. `Python Basics`.
   - **Description**.
   - **Duration (hours)** and **Capacity** (both must be greater than 0).
   - **Price** (0 or more; a price of 0 makes the course free).
   - **Category** (optional).
   - **Trainer**: pick a trainer, or leave **No trainer assigned**.
3. Click **Save**. Every new course starts as **Draft**, so students can't see it until you publish it ([2.2.4](#scenario-224-publish-archive-or-republish-a-course)).

**If something goes wrong:** a code that already exists gives **"A course with this code already exists"**.

#### Scenario 2.2.3: Edit a course

1. On **Courses**, open the **⋯** menu on a row and choose **Edit**.
2. Change the fields you need, then click **Save**.

> Editing keeps the course's current status. Price changes only affect invoices created afterwards; existing invoices keep their amount.
> There is no **Delete** action in the app. To retire a course, **Archive** it ([2.2.4](#scenario-224-publish-archive-or-republish-a-course)).

#### Scenario 2.2.4: Publish, archive or republish a course

The **⋯** menu offers the next logical step for the course's current status:

| Current status | Menu action | Result |
|---|---|---|
| Draft | **Publish** | Visible in the catalog; students can enroll |
| Published | **Archive** | Removed from the catalog; no new enrollments |
| Archived | **Republish** | Back in the catalog |

*Try it:*
1. Open the menu on **Data Analysis with SQL** (Draft) and choose **Publish**.
2. Confirm. The course now shows in the student catalog.

Archiving a course only stops **new** enrollments. Students already on it
keep their enrollment, sessions, attendance, grades and invoices, and can
still be completed and certified.

#### Scenario 2.2.5: Open a course's detail page

1. On **Courses**, click a course name, or choose **View details** from its menu.
2. The page shows the trainer, category, duration, capacity, price and creation date.
3. It has three tabs:
   - **Schedule**: the course's sessions, with **New Session** to add one.
   - **Enrollments**: who is enrolled, with **Approve**, **Reject** and **Mark completed** actions.
   - **Attendance**: the attendance report for the course.
4. Click **Gradebook** at the top to open the course's grades.

---

### 2.3 Student directory and student profile

#### Scenario 2.3.1: Find a student

1. Click **Students**.
2. Type in **Search by name…** or filter with **All statuses**.
3. The list shows each student's number of active (approved) enrollments.

#### Scenario 2.3.2: Review a student's complete record

1. On **Students**, click **Sofia**. Her summary page opens.
2. The **Overview** tab shows:
   - **Total Enrollments** and **Active Enrollments**
   - **Attendance Rate** (Sofia: **100%**)
   - **Overall Grade** (Sofia: **88%**)
   - **Payment Balance** (Sofia: **0**)
3. Go through the other tabs:
   - **Enrollments**: every course with its status, dates, and who decided it. Approved rows have a **Mark completed** button.
   - **Schedule**: **Upcoming** and **Past** sessions of every course she is approved on or has completed.
   - **Attendance**: her record on each approved or completed course.
   - **Grades**: every assessment from every course, **including completed courses**, with the weighted average per course.
   - **Payments**: her invoices and outstanding balance (administrators only).
   - **Certificates**: issued certificates, plus her other courses under **Eligible for certification**.

This page is the place to look up a student's full history, especially
after a course is finished: completed students no longer appear in the
course **Gradebook** or on the student's own **Schedule** page, but
everything is still here.

*Compare with Sara Sassi:* her attendance rate is 50% (one late, one
absent), her overall grade is 55%, and she owes 1,050 (300 on Java plus 750
overdue on React).

---

### 2.4 Enrollment management

#### Scenario 2.4.1: Approve a pending enrollment request

1. Click **Enrollments**. The page opens filtered to **Pending** requests, newest first. Sam Student's request for **React in Practice** is listed.
2. Use the status filter to see other statuses, or **All statuses** for the full history (with **Requested** date and **Decided By**).
3. Click **Approve** and confirm in **Approve enrollment?**.
4. The status changes to **Approved**. At the same moment, the app automatically creates an invoice:
   - Amount: the course price (750).
   - Status: Pending.
   - Due: 30 days from today.
5. Go to **Payments** to check that the new invoice for Sam is there.

> Free courses (price 0) don't get an invoice.

#### Scenario 2.4.2: Reject a pending enrollment request

1. On **Enrollments**, find a **Pending** request.
2. Click **Reject** and confirm in **Reject enrollment?**.
3. The status changes to **Rejected**. No invoice is created.

> ⚠️ A rejected enrollment is final. The same student can **never** enroll in that course again.

#### Scenario 2.4.3: Enroll a student on their behalf

There is no button for this in the app. An administrator can register a
student through the API, for example `POST /api/v1/enrollments` with a
`studentId`. The simpler route is to sign in as the student (or ask them) and
use **Enroll** in the catalog.

The same checks apply as when a student enrolls themselves:
- The course must be published.
- The course must not be full.
- The student must not already have an enrollment in it.

The request starts as **Pending**, and you approve it as in [2.4.1](#scenario-241-approve-a-pending-enrollment-request).

#### Scenario 2.4.4: Mark an enrollment completed

Do this once a student has finished a course. It is the step that makes them eligible for a certificate.

**⚠️ Before you click, check that everything is recorded.** Completing is
permanent (there's no way back to Approved), and afterwards:
- The trainer **can't add grades** for the student, and the student disappears from the course **Gradebook**.
- The student **can't check in by QR code** for that course.
- The course's sessions disappear from the student's own **Schedule** page.

So first confirm, on the student's profile:
1. **Grades** tab: all assessments for the course are there.
2. **Attendance** tab: every session is marked, and the rate is at least 75% if they need a certificate.

Then:
1. Open the student's summary: **Students** → click the student.
2. In the **Enrollments** tab, or in **Certificates** under the course, click **Mark completed**. You can also do this from **Enrollments** in the menu (filter **Approved**), or from the course's **Enrollments** tab.
3. Confirm in **Mark enrollment completed?**. The status becomes **Completed**.

Only an **Approved** enrollment can be marked completed. Grades and
attendance already recorded stay visible on the student's profile and in
their **My Grades** / **My Attendance** pages. Grades are edited from the
Gradebook, which no longer lists the student, so fix grades **before**
completing. Attendance can still be corrected afterwards.

#### Scenario 2.4.5: Cancel an enrollment on a student's behalf

There is no button for this in the admin screens; students cancel their own
from **My Enrollments** ([Scenario 4.1.3](#scenario-413-track-or-cancel-your-enrollments)).
An administrator can cancel any **Pending** or **Approved** enrollment through
the API (`POST /api/v1/enrollments/{id}/cancel`).

> Cancelling doesn't remove the invoice. Adjust it with the student separately.

---

### 2.5 Scheduling

#### Scenario 2.5.1: View the full schedule

1. Click **Schedule**.
2. Filter by **Course** (All courses), **Trainer** (All trainers) and a **From** / **To** date range.

#### Scenario 2.5.2: Create a class session

1. On **Schedule**, click **New Session**. You can also click it from a course's **Schedule** tab.
2. Fill in:
   - **Course**.
   - **Trainer**.
   - **Classroom**, e.g. `Room C`.
   - **Date**, **Start time** and **End time**.
3. Click **Save**. The session appears as **Scheduled**.

**Conflicts are blocked.** The app checks for overlaps on the same date and refuses a session that clashes:

| Situation | Message |
|---|---|
| End time is not after start time | **"endTime must be after startTime"** |
| The trainer already has an overlapping session | **"That trainer is already booked for an overlapping session"** |
| The classroom is already in use | **"That classroom is already booked for an overlapping session"** |
| Both of the above | **"That trainer and classroom are both already booked for an overlapping session"** |

Back-to-back sessions, where one ends exactly when the next starts, are allowed. Cancelled sessions don't count as conflicts.

*Try it:* create a Java Fundamentals session in **Room A**, 10:00 to 11:00,
two days from now. It is refused because it overlaps the existing
09:00 to 12:00 session. Change the room to **Room C**. It is still refused,
because Tina is booked then. Change the trainer to **Tom Teacher** and it
saves.

> A session's trainer doesn't have to be the course's trainer. A trainer who
> only teaches a session can take attendance and show the QR code for it, but
> only the **course's** trainer can grade students and issue certificates.
> The app also doesn't stop you scheduling a session in the past, or for a
> Draft course.

#### Scenario 2.5.3: Reschedule a session

1. On **Schedule**, open the **⋯** menu on a **Scheduled** session and choose **Edit**.
2. Change the date, time, room or trainer, then click **Save**. The same conflict checks apply.

#### Scenario 2.5.4: Cancel a session

1. On **Schedule**, open the **⋯** menu on a **Scheduled** session and choose **Cancel session**.
2. Confirm in **Cancel session?**. The session becomes **Cancelled**:
   - Attendance can no longer be taken for it.
   - Its QR code no longer works.
   - Its time slot and room are free to book again.
   - Attendance already recorded for it is kept.

> A cancelled or completed session can't be edited or reopened. To move a
> session, **Edit** it while it is still Scheduled instead of cancelling it.

#### Scenario 2.5.5: Mark a session completed

1. Open the **⋯** menu on a **Scheduled** session and choose **Mark completed**.
2. Confirm in **Mark session completed?**.

Attendance can still be taken or corrected after a session is completed.
A completed session with nobody marked shows up in the trainer's
**Sessions to mark** tile.

---

### 2.6 Attendance (administrator)

#### Scenario 2.6.1: Take attendance for any session

1. On **Schedule**, open the **⋯** menu on a session and choose **Take attendance**.
2. Follow the same steps as the trainer ([Scenario 3.3.1](#scenario-331-take-attendance-manually)).

#### Scenario 2.6.2: Read a course attendance report

1. Go to **Courses**, click **Java Fundamentals**, and open the **Attendance** tab.
2. The report shows each student's attendance rate over the recorded sessions:
   - Sam: **100%**
   - Sara: **50%**
   - Sofia: **100%**

> The attendance rate counts **Present** and **Late** as attended, out of all recorded marks. Sessions nobody marked don't count against the student. If nothing is marked yet, the rate shows as **—** rather than 0%.
> Completed students stay on the roster and in the report, so their attendance can still be corrected.

---

### 2.7 Payments

Invoices are created **automatically** when an administrator approves an
enrollment on a paid course: amount = course price, due in 30 days. There is
no button to create an invoice by hand, to change an invoice's amount, or to
delete one. (Administrators can create extra invoices through the API,
`POST /api/v1/payments`.)

#### Scenario 2.7.1: Review the payments ledger

1. Click **Payments**.
2. Filter with **Student** (All students), **Course** (All courses) and **Status** (All statuses).
3. The footer shows the number of invoices and the total outstanding for the current filter.

Invoice statuses:

| Status | Meaning |
|---|---|
| **Pending** | Nothing paid yet, not yet due |
| **Partial** | Something paid, a balance remains |
| **Paid** | Fully settled |
| **Overdue** | Past its due date with a balance remaining; the app updates this automatically when the page loads |

#### Scenario 2.7.2: Record a partial payment

1. On **Payments**, find **Sara Sassi / Java Fundamentals** (500 due, 200 paid, **Partial**).
2. Click **Record payment**.
3. Enter:
   - **Amount**: `100`.
   - **Method**: e.g. `Cash`.
   - **Notes** (optional).
4. Click **Record payment**. The invoice now shows 300 paid and 200 outstanding, and stays **Partial**.

#### Scenario 2.7.3: Settle an invoice in full

1. On the same invoice, click **Record payment** again and enter `200`.
2. Save. The status becomes **Paid** and the paid date is recorded.

**Overpaying is blocked.** Entering more than the balance shows
**"Only … is still owed on this invoice"**.

#### Scenario 2.7.4: Follow up an overdue invoice

1. Filter **Status** to **Overdue**. **Sara Sassi / React in Practice** is listed: 750, due 5 days ago, with the note "Chased once by email."
2. Record whatever Sara pays.
   - While a balance remains, the invoice returns to **Overdue** the next time the list refreshes, because the due date has passed.
   - Once the invoice is fully paid, it becomes **Paid**.

> Paying doesn't affect enrollment or certificates. A student with an unpaid or overdue invoice can still attend, be graded, completed and certified; follow up payments separately.

---

### 2.8 Grades (administrator)

Administrators can open any course's **Gradebook** (**Courses** → a course →
**Gradebook**) and add, edit or remove any grade, including grades recorded
by trainers. The steps are the same as for trainers ([3.4](#34-grades)).

To see the grades of a student who has **finished** a course, use their
profile: **Students** → the student → **Grades** tab ([Scenario 3.4.3](#scenario-343-see-the-grades-of-a-student-who-finished-the-course)).

### 2.9 Certificates

#### Scenario 2.9.1: Issue a certificate

Sofia Ben Ali is the ready-made example.

1. Go to **Students** and click **Sofia**.
2. Open the **Certificates** tab. Under **Eligible for certification**, **Java Fundamentals** is listed.
3. Click **Generate certificate**.
4. The certificate appears under **Issued**. It has a number like **CERT-2026-000001**.
5. Click **Download PDF** to open the certificate. It is an A4 landscape "Certificate of Completion".

A student must meet **both** of these conditions to get a certificate:
1. Their enrollment is **Completed** ([Scenario 2.4.4](#scenario-244-mark-an-enrollment-completed)).
2. Their attendance on the course is **at least 75%**, and at least one session has been recorded.

Payment status and grades do **not** affect eligibility. A course can only be certified once per student, and an issued certificate can't be revoked or deleted.

**Eligible for certification** lists every course of the student's that has
no certificate yet, whatever its status. When **Generate certificate** is
greyed out, the reason is written under the course name:
- *"This enrollment is approved. It must be marked completed before a certificate can be issued."* An admin can click **Mark completed** on the same row.
- *"Only this course's trainer or an administrator can issue its certificate."* Shown to trainers on other trainers' courses.

When everything is certified, the section says *"Every course this student is enrolled in has been certified."*

#### Scenario 2.9.2: See why a student can't be certified yet

1. Open **Sara Sassi**'s summary and go to **Certificates**.
2. If you try to generate a Java certificate for her, you get an error. The message depends on what is missing:
   - **"This student's enrollment must be marked COMPLETED before a certificate can be issued (it is currently APPROVED)"**
   - After completing her: **"Attendance on this course is 50.0%, below the 75.0% required for a certificate"**

---

## 3. Trainer scenarios

Sign in as **`tina.trainer@tcm.local`**. The left navigation shows
**Dashboard**, **My Courses**, **Course Catalog**, **Students** and
**Schedule**.

### 3.0 Trainer dashboard

#### Scenario 3.0.1: Read the trainer dashboard

1. Click **Dashboard**. The tiles show only your own figures:
   - **My courses**: courses where you are the assigned trainer.
   - **Upcoming sessions**: your scheduled sessions in the next 7 days.
   - **Sessions to mark**: your completed sessions with no attendance recorded.
   - **Students to grade**: approved students on your courses with no grade yet.
2. Click a tile to go to the related page.

### 3.1 Courses

#### Scenario 3.1.1: See the courses you teach

1. Click **My Courses**. Tina sees **Java Fundamentals**.
2. Click it to open the detail page and its tabs:
   - **Schedule**
   - **Enrollments**: the roster, visible only to this course's trainer and to admins.
   - **Attendance**: the report for the course.
3. Click **Gradebook** to grade your students.

#### Scenario 3.1.2: Browse the shared catalog

1. Click **Course Catalog** to see every published course.
2. Type in **Search courses…** to narrow the list.

Opening another trainer's course shows its details, but its **Enrollments**
tab says *"Only this course's trainer and administrators can see who's
enrolled."*, and its Gradebook refuses you.

> Trainers can't create or edit courses, or create sessions. Those are administrator tasks.

### 3.2 Schedule

#### Scenario 3.2.1: View your schedule

1. Click **Schedule**. You only see your own sessions. Tina sees her three Java sessions in **Room A**.

#### Scenario 3.2.2: Mark your session completed

1. Open the **⋯** menu on one of your **Scheduled** sessions and choose **Mark completed**.
2. Confirm.

> Only an administrator can cancel a session.

### 3.3 Attendance

#### Scenario 3.3.1: Take attendance manually

1. On **Schedule**, open the **⋯** menu on the **upcoming Java session** (in 2 days) and choose **Take attendance**.
2. The roster lists every approved or completed student on the course: Sam, Sara and Sofia.
3. For each student, choose **Present**, **Absent** or **Late**. You can also click **Mark all present** and then change individual students.
4. The footer shows "*X of Y marked · unsaved changes*".
5. Click **Save**. A confirmation says attendance was saved.
6. The **Recorded** column shows each saved status. Students who checked in by QR code have a **QR** badge.

You can come back and change marks at any time. Saving again replaces the
earlier mark. Attendance can't be taken for a **cancelled** session.

#### Scenario 3.3.2: Let students check in with a QR code

1. On the **Take attendance** page for a session, click **Show QR**.
2. The **Scan to check in** dialog shows:
   - The QR code.
   - A live countdown, **Expires in mm:ss**. A code is valid for **15 minutes**.
   - The check-in link, for students who can't scan.
3. Put the dialog on the projector. Students scan the code with their phone ([Scenario 4.5](#45-qr-code-check-in)).
4. Click **New code** when the countdown runs out, or if the code might have been shared outside the room. The old code stops working immediately.
5. Click **Close**, then refresh the page. Students who checked in are listed as **Present** with a **QR** badge.

> For phones to reach the check-in link, the server's `FRONTEND_BASE_URL` must be the computer's network address (e.g. `http://192.168.1.20:5173`), not `localhost`. Ask whoever installed the app.

### 3.4 Grades

#### Scenario 3.4.1: Record an assessment

1. Open **My Courses**, click **Java Fundamentals**, then click **Gradebook**.
2. Each student with an **Approved** enrollment is listed with their number of assessments and weighted average. Students who are pending, rejected, cancelled **or completed** are not listed. If nobody is approved yet, the page says *"Nobody is approved on this course yet."*
3. Next to **Sara Sassi**, click **Add assessment**.
4. Fill in:
   - **Type**: Exam, Assignment, Quiz or Project.
   - **Title**, e.g. `Final exam`.
   - **Score**, e.g. `80`.
   - **Out of**, e.g. `100`.
   - **Weight**, e.g. `40`. This is the assessment's importance in the final grade.
   - **Comments** (optional).
5. Click **Add assessment**. Sara's weighted average updates.

**How the final grade is calculated:** each assessment's percentage
(score ÷ out of) is multiplied by its weight, the results are added up, and
the total is divided by the sum of the weights.

For example, Sam has a quiz of 17/20 (weight 20) and an exam of 72/100
(weight 40):
(85% × 20 + 72% × 40) ÷ 60 = **76.3%**.

**Rules:**
- Score can't be negative or higher than **Out of**. Otherwise you see **"score must not exceed maxScore"**.
- Weights don't have to add up to 100.

#### Scenario 3.4.2: Edit or remove an assessment

1. In the **Gradebook**, click the arrow next to a student to expand their assessments.
2. To change one, use its edit action, change the values, and click **Save changes**.
3. To delete one, use its delete action and confirm **Remove this assessment?** with **Remove**.

> You can only change grades you recorded yourself. Administrators can change any grade.
> Only students with an **Approved** enrollment can receive new grades.
> Only the course's assigned trainer can open its Gradebook. Teaching one of its sessions isn't enough.

#### Scenario 3.4.3: See the grades of a student who finished the course

Once an administrator marks an enrollment **Completed**, the student drops
out of the Gradebook. Their grades are kept and are still visible:

1. Click **Students** and click the student's name.
2. Open the **Grades** tab. Every assessment is listed per course with the weighted average, including completed courses. The **Overview** tab shows their **Overall Grade**.

*Try it:* as Tina, open **Students → Sofia → Grades**. Her Java
"Final project" (88/100, **88%**) is there, although Sofia doesn't appear in
the Java Gradebook because her enrollment is Completed.

If the tab says *"Nothing has been graded for this student yet."*, no grade
was ever recorded. A completed student can't be graded any more, so ask an
administrator (see [FAQ](#8-troubleshooting-and-faq)).

### 3.5 Students

#### Scenario 3.5.1: Look up one of your students

1. Click **Students** and click a student's name.
2. The student's summary opens with the same tabs as for administrators, with two differences:
   - **Grades** only shows your own courses.
   - **Payments** says *"Only administrators can see a student's invoices."*

> You can only open the profile of a student enrolled in a course you teach. For anyone else you see **"You do not have permission to perform this action"**.

#### Scenario 3.5.2: Issue a certificate for your student

As the trainer of a course, you can issue its certificates. The steps are
the same as [Scenario 2.9.1](#scenario-291-issue-a-certificate).

Only an administrator can mark the enrollment **Completed** beforehand. Before
asking them to, make sure your grades and attendance for the student are
complete (see the checklist in [Scenario 2.4.4](#scenario-244-mark-an-enrollment-completed)).

---

## 4. Student scenarios

Sign in as **`sam.student@tcm.local`**. The left navigation shows
**Dashboard**, **Course Catalog**, **My Enrollments**, **Schedule**,
**My Payments**, **Check In**, **My Attendance**, **My Grades** and
**My Certificates**.

### 4.0 Student dashboard

#### Scenario 4.0.1: Read your dashboard

1. Click **Dashboard**. The tiles show:
   - **Active enrollments**
   - **Attendance**
   - **Overall grade** (Sam: **76.3%**)
   - **Balance owed**
   - **Certificates**
2. Click a tile to see the details.

### 4.1 Browsing and enrolling

#### Scenario 4.1.1: Browse the catalog

1. Click **Course Catalog**. Every published course is shown as a card.
2. Type in **Search courses…** to narrow the list.
3. Click a card to see the full description, trainer, category, duration, capacity and price.

> The course's **Schedule** tab stays empty (*"No sessions scheduled for this course yet."*) until you are approved on the course, and its **Enrollments** tab only shows your own status.

#### Scenario 4.1.2: Enroll in a course

1. On a course card or its detail page, click **Enroll**.
2. The button changes to **Pending approval**. An administrator now has to approve the request ([Scenario 2.4.1](#scenario-241-approve-a-pending-enrollment-request)).

If you can't enroll, the button is greyed out and shows why:

| Button says | Why |
|---|---|
| **Pending approval** | You've already asked; waiting for an administrator |
| **Already enrolled** | You're approved on this course |
| **Completed** | You've already finished this course |
| **Rejected** / **Cancelled** | An earlier request was rejected or cancelled; you can't apply again |
| **Full** | The course has reached its capacity |

*Try it as Sam:* the **React in Practice** card says **Pending approval**,
and **Java Fundamentals** says **Already enrolled**. If an admin has
published **Data Analysis with SQL**, Sam can click **Enroll** on it.

#### Scenario 4.1.3: Track or cancel your enrollments

1. Click **My Enrollments** to see every course you've requested and its status: **Pending** (waiting for approval), **Approved** (you're in), **Rejected**, **Cancelled** or **Completed**.
2. To withdraw a **Pending** or **Approved** enrollment, click **Cancel**.
3. Confirm with **Cancel enrollment**.

> ⚠️ Cancelling is final. You won't be able to enroll in that course again.
> An invoice already raised for the course stays in place. Contact the administration about refunds.

### 4.2 Your schedule

#### Scenario 4.2.1: View your sessions

1. Click **Schedule**. You see the sessions of every course you're **Approved** on, with date, time, room and trainer.
2. Sam sees the Java sessions.
3. Sam doesn't see the React sessions until an administrator approves his request.

> Once a course is marked **Completed**, its sessions no longer show here.
> Your attendance and grades for it stay in **My Attendance** and **My Grades**.

### 4.3 Your payments

#### Scenario 4.3.1: Check what you owe

1. Click **My Payments**. Each invoice shows the amount due, the amount paid, the balance, the due date and the status.
2. Sam sees one **Pending** invoice of 500 for Java Fundamentals.

*Sign in as Sara to compare:* a red **Payment overdue** notice appears for
React in Practice, and her Java invoice is **Partial** (200 of 500 paid).

> Payments are recorded by the training center when you pay (cash, card, transfer…). You can't pay online through the app.
> An invoice appears when your enrollment is **approved** (not when you request it), and free courses have none. With no invoices, the page says *"You have no invoices - nothing is owed."*

### 4.4 Your attendance and grades

#### Scenario 4.4.1: Check your attendance

1. Click **My Attendance**.
2. **By course** shows your attendance rate on each course.
3. **Sessions** lists each session: Present, Absent or Late, and whether it was recorded by QR code.
   - Sam: 100% on Java, including one QR check-in.
   - Sara: 50% (one late, one absent).
4. Before anything is recorded, the page says *"No attendance has been recorded for you yet."*

#### Scenario 4.4.2: Check your grades

1. Click **My Grades**. Assessments are grouped by course, with your weighted average for each.
2. Sam sees his **Collections quiz** (17/20) and **Midterm exam** (72/100), giving **76.3%** on Java Fundamentals.
3. Courses you have **completed** stay on this page with all their grades. Sign in as Sofia to see her Java "Final project" (88%).

If the page says *"Nothing has been graded for you yet."*, no trainer has
recorded any assessment for you on any course. See the [FAQ](#8-troubleshooting-and-faq).

### 4.5 QR code check-in

#### Scenario 4.5.1: Check in by scanning

1. Your trainer shows a QR code in class ([Scenario 3.3.2](#scenario-332-let-students-check-in-with-a-qr-code)).
2. Scan it with your phone's camera and open the link.
   - If you're not signed in on your phone, you're asked to sign in first, then taken straight to the check-in.
3. The page shows **You're marked present**, with the time it was recorded. Nothing else to do.

#### Scenario 4.5.2: Check in from the app

1. Click **Check in** in the menu.
2. Allow camera access and point it at the code on screen.
3. If scanning doesn't work, paste the full check-in link (shown under the QR code) into **Can't scan? Paste the check-in link** and click **Check in**.

#### Scenario 4.5.3: When check-in fails

| Message | What to do |
|---|---|
| **That code is no longer valid** (expired, or replaced by a newer code) | Ask the trainer to show a fresh code |
| **You're not on this course** | You need an **Approved** enrollment on the course |
| **That code doesn't belong to this session** | Scan the code on screen again |
| **Check-in failed** | Try again in a moment |

> A QR check-in always records you as **Present**, even if the trainer marked you differently earlier. Scanning twice is harmless.
> Check-in only works while your enrollment is **Approved**. It isn't possible after the course is marked Completed.
> Trainers and administrators who open **/attend** see *"Check-in is for students"* with a **Go to schedule** button.

### 4.6 Your certificates

#### Scenario 4.6.1: Download your certificate

1. Sign in as **`sofia.benali@tcm.local`** after the certificate has been issued ([Scenario 2.9.1](#scenario-291-issue-a-certificate)).
2. Click **My Certificates**. Your certificate is listed with its **CERT-…** number and issue date.
3. Click **Download PDF**. A file named after the certificate number, e.g. `CERT-2026-000001.pdf`, downloads.

Before any certificate is issued, the page says *"You haven't been awarded a
certificate yet."* Certificates are issued by an administrator or the course's
trainer; they are not automatic when a course ends.

---

## 5. End-to-end walkthrough: from new course to certificate

This scenario follows one student through the whole lifecycle. Use one
browser for the admin and a private window for the other roles, so you can
stay signed in to both.

| # | Who | Step |
|---|---|---|
| 1 | Admin | **Users → New User**: create student **Lina New** (`lina.new@tcm.local`, temporary password `Welcome123!`). |
| 2 | Admin | **Courses → New Course**: code `PY-101`, name `Python Basics`, 20 h, capacity 10, price 300, trainer **Tom Teacher**. Click **Save**. The course is created as **Draft**. |
| 3 | Admin | In the `PY-101` **⋯** menu, choose **Publish** and confirm. |
| 4 | Admin | **Schedule → New Session**: Python Basics, Tom Teacher, `Room C`, **today**, 09:00 to 10:00. Add a second session **tomorrow** at the same time. |
| 5 | Lina | Sign in → **Course Catalog** → **Python Basics** → **Enroll**. The button shows **Pending approval**. |
| 6 | Admin | **Dashboard**: **Pending enrollments** has gone up by 1. Go to **Enrollments**, filter **Pending**, **Approve** Lina and confirm. |
| 7 | Admin | **Payments**: a new **Pending** invoice of 300 for Lina / Python Basics, due in 30 days. |
| 8 | Lina | **Schedule** now shows both Python sessions. **My Payments** shows the 300 invoice. |
| 9 | Tom | Sign in → **Schedule** → today's Python session **⋯** → **Take attendance** → **Show QR**. |
| 10 | Lina | **Check In**, then scan the code or paste the link shown under it. The page shows **You're marked present**. |
| 11 | Tom | Close the QR dialog and refresh. Lina is **Present** with a **QR** badge. For the second session: **Take attendance** → mark Lina **Present** → **Save**. |
| 12 | Tom | **My Courses → Python Basics → Gradebook** → **Add assessment** for Lina: Project, `Final project`, 45 out of 50, weight 100. Her average shows **90%**. |
| 13 | Tom | **Schedule**: **Mark completed** on both Python sessions. |
| 14 | Admin | **Payments** → Lina's invoice → **Record payment** `300`, method `Card`. The status becomes **Paid**. (Not required for the certificate, but this closes the account.) |
| 15 | Admin | **Students → Lina New → Enrollments** → **Mark completed** on Python Basics and confirm. |
| 16 | Admin or Tom | **Students → Lina New → Certificates**. Python Basics is under **Eligible for certification**. Click **Generate certificate**. |
| 17 | Lina | **My Certificates** → **Download PDF**. |
| 18 | Admin | **Dashboard**: **Certificates issued** has gone up by 1. |

**Why this order matters:** grades (step 12) and attendance (steps 10–11)
must be recorded **before** the enrollment is marked completed (step 15).
After step 15, Lina leaves the Gradebook and can no longer be graded or check
in by QR code.

**Check along the way:**
- After step 11, Lina's attendance rate is 100%.
- After step 14, her balance owed is 0.
- If you run step 16 before step 15, the button is greyed out: "This enrollment is approved. It must be marked completed…".
- After step 15, Lina's grades are still on **Students → Lina New → Grades** and on her **My Grades** page.

### Short version: closing out a course for a student

1. **Trainer:** all sessions marked (attendance ≥ 75% for a certificate).
2. **Trainer:** all assessments recorded in the Gradebook.
3. **Admin:** **Mark completed** on the enrollment.
4. **Admin or course trainer:** **Generate certificate**.
5. **Admin (any time):** record payments until the invoice is **Paid**.

---

## 6. Rules at a glance

### Who can do what

| Action | Admin | Trainer | Student |
|---|:-:|:-:|:-:|
| Create, edit or deactivate users; reset passwords | ✅ | | |
| Create, edit, publish or archive courses | ✅ | | |
| Browse published courses | ✅ | ✅ | ✅ |
| Enroll in a course | ✅ (for a student) | | ✅ (self) |
| Approve or reject enrollments; mark enrollments completed | ✅ | | |
| Cancel an enrollment | ✅ | | ✅ (own) |
| Create, edit or cancel sessions | ✅ | | |
| Mark a session completed | ✅ | ✅ (own sessions) | |
| Take attendance or show the QR code | ✅ | ✅ (own sessions) | |
| Check in by QR code | | | ✅ |
| Record, edit or remove grades | ✅ | ✅ (course's assigned trainer; own entries) | |
| View a student's profile | ✅ | ✅ (students on their courses) | |
| View a student's invoices | ✅ | | ✅ (own) |
| Record payments | ✅ | | |
| Issue certificates | ✅ | ✅ (course's assigned trainer) | |
| Download a certificate | ✅ | ✅ (own courses) | ✅ (own) |
| View own schedule, attendance, grades, payments and certificates | | ✅ | ✅ |

### Lifecycles

**Course:** Draft → Published ⇄ Archived. Only **Published** courses accept enrollments. Courses can't be deleted in the app.

**Enrollment:**
```
Pending ──► Approved ──► Completed
   │           │
   ├─► Rejected └─► Cancelled
   └─► Cancelled
```
Rejected, Cancelled and Completed are final. A student gets **one**
enrollment per course, ever. **Approving** an enrollment on a paid course
creates its invoice.

What each enrollment status allows:

| | Pending | Approved | Completed | Rejected / Cancelled |
|---|:-:|:-:|:-:|:-:|
| Sees the course's sessions in **Schedule** | | ✅ | | |
| On the attendance roster | | ✅ | ✅ | |
| Can check in by QR code | | ✅ | | |
| Listed in the Gradebook / can receive new grades | | ✅ | | |
| Past grades visible (profile, **My Grades**) | ✅ | ✅ | ✅ | ✅ |
| Can be certified | | | ✅ | |
| Counts toward course capacity | | ✅ | | |

**Session:** Scheduled → Completed, or Scheduled → Cancelled. Only scheduled sessions can be edited.

**Invoice:** Pending → Partial → Paid. Pending or Partial becomes **Overdue** once the due date has passed.

### Key numbers

| Rule | Value |
|---|---|
| Invoice due date (automatic) | 30 days after approval |
| QR code validity | 15 minutes, one active code per session |
| Minimum attendance for a certificate | 75% |
| Attendance rate | (Present + Late) ÷ all recorded marks |
| Final grade | Σ(score ÷ max × weight) ÷ Σ weight |
| Course capacity | Counts approved enrollments only; checked when a student enrolls and when an admin approves |

---

## 7. Error messages and what they mean

| Message | Cause |
|---|---|
| Invalid email or password | Wrong credentials, or the account is deactivated |
| You do not have permission to perform this action | Your role (or your link to that course or student) doesn't allow it |
| A user with this email already exists | Email is taken |
| A course with this code already exists | Course code is taken |
| Course is not open for enrollment | The course is Draft or Archived |
| Course has reached its capacity | No seats left |
| Student is already enrolled in this course | Any earlier enrollment (including rejected or cancelled) blocks a new one |
| Only PENDING enrollments can be approved or rejected | It has already been decided |
| Only APPROVED enrollments can be marked completed | Approve it first |
| endTime must be after startTime | Fix the session times |
| That trainer / classroom is already booked for an overlapping session | Choose another time, room or trainer |
| Only SCHEDULED sessions can be rescheduled / cancelled / marked completed | The session is already closed |
| Attendance cannot be marked for a cancelled session | — |
| Only … is still owed on this invoice / That payment exceeds the … still owed on this invoice | Enter no more than the balance |
| score must not exceed maxScore | Score is higher than **Out of** |
| Student … has no APPROVED enrollment in this course | Only approved students can be graded |
| This student's enrollment must be marked COMPLETED before a certificate can be issued / This enrollment is approved. It must be marked completed… | Mark the enrollment completed first |
| Attendance on this course is X%, below the 75.0% required for a certificate | Attendance too low |
| No attendance has been recorded for this student on this course… | Take attendance first |
| Certificate CERT-… already issued to this student for this course | One certificate per student per course |
| That QR code has expired / has been replaced by a newer one | Ask for the current code |
| That QR code is not valid for this session | Wrong or damaged code |

---

## 8. Troubleshooting and FAQ

**A student finished a course but "My Grades" says "Nothing has been graded for you yet."**
No assessment was ever recorded for them. Usually the enrollment was marked
**Completed** before the trainer added grades. A completed student can't be
graded any more and doesn't appear in the Gradebook, and the app can't move
an enrollment back to Approved. Ask whoever runs the system to correct the
enrollment in the database. To avoid it, follow the order in
[Closing out a course](#short-version-closing-out-a-course-for-a-student).

**Where are a completed student's grades?**
Not in the Gradebook. Look on **Students → the student → Grades** (admin or
the course's trainer), or on the student's own **My Grades** page
([Scenario 3.4.3](#scenario-343-see-the-grades-of-a-student-who-finished-the-course)).

**A student's course sessions vanished from their Schedule.**
Their enrollment was marked Completed (or cancelled). Completed courses keep
their attendance and grades; only the schedule view drops them.

**A student can't enroll again after being rejected or cancelling.**
By design, one enrollment per student per course. There is no re-apply flow.

**The student cancelled, but their invoice is still there.**
Cancelling doesn't touch invoices. Settle or waive it with the student
outside the app.

**A student approved on a course has no invoice.**
The course is free (price 0), or they already had an invoice for that course.

**An invoice shows Overdue even after a partial payment.**
Any invoice past its due date with a balance left goes back to Overdue.
It becomes Paid only when fully settled.

**The Generate certificate button is greyed out.**
Read the line under the course name: the enrollment isn't Completed, or you
aren't the course's trainer. If the button is active but you get an error,
attendance is below 75% or wasn't recorded.

**Sofia has no Generate certificate button.**
Her certificate has already been issued on this database. Reset the demo data
(`docker compose down -v`, then `docker compose up --build`) to try again.

**A trainer can take attendance but can't open the Gradebook.**
They teach a session of the course but aren't its assigned trainer. Ask an
administrator to set them as the course's **Trainer** (**Courses → ⋯ → Edit**).

**A trainer sees "You do not have permission to perform this action" on a student.**
The student isn't enrolled in any course that trainer teaches.

**Students scanning the QR code with a phone get "site can't be reached".**
The QR link points to `localhost`. Set `FRONTEND_BASE_URL` in `.env` to the
computer's network address (e.g. `http://192.168.1.20:5173`) and restart.

**"That code is no longer valid" right after scanning.**
The code expired (15 minutes) or the trainer clicked **New code**. Scan the
code currently on screen.

**Someone was deactivated by mistake / can't sign in.**
An administrator reactivates them from **Users → ⋯ → Activate**, and can
**Reset password** if they've forgotten it. Deactivated users see the same
"Invalid email or password" message as a wrong password.

**Can a mistake in attendance be fixed?**
Yes. Open **Take attendance** on the session (even if it's completed), change
the mark and click **Save**. Only cancelled sessions are locked.

**Can attendance be taken before a session happens?**
The app allows it, so be careful to use the right session's page.
