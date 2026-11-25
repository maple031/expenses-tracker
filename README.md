# SmartSpend — Phases 1–2

Expense management project using Java 27, Maven, JSP/Jakarta Servlets, JDBC, MySQL and Tomcat 10.1.

## Requirements
- JDK 27, Maven 3.9+, MySQL Server 8, Apache Tomcat 10.1

## 1. Open
Extract the ZIP and open the `SmartSpend_Phase1` folder (the one containing `pom.xml`) in VS Code.

## 2. Create database tables
Sign in to MySQL Command Line, then run (replace the example path):
```sql
SOURCE C:/path/to/SmartSpend_Phase1/database/schema.sql;
```
Use forward slashes in the path.

## 3. Configure database credentials
In a PowerShell terminal, set these values. Replace the password with your own MySQL password. Do not share it in chat.
```powershell
$env:SMARTSPEND_DB_URL = "jdbc:mysql://localhost:3306/smartspend?serverTimezone=Asia/Kolkata"
$env:SMARTSPEND_DB_USER = "YOUR_MYSQL_USERNAME"
$env:SMARTSPEND_DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
```
All three environment variables are required. An empty database password is not accepted.
Start Tomcat from this same PowerShell session so it inherits these settings.

For a verified TLS deployment, configure the MySQL JDBC URL explicitly with Connector/J's TLS settings and a trusted certificate configuration, for example:
```text
jdbc:mysql://HOST:3306/smartspend?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.3,TLSv1.2&serverTimezone=Asia/Kolkata
```
Use only a URL whose certificate trust and server identity have been verified for your MySQL deployment. SmartSpend does not claim that TLS is active unless the deployed JDBC URL explicitly enables and verifies it.

## 4. Build
From the folder containing `pom.xml`:
```powershell
mvn clean package
```
The first build downloads dependencies and needs internet access. It creates `target/SmartSpend.war`.

## 5. Deploy
1. Stop Tomcat.
2. Copy `target/SmartSpend.war` into Tomcat's `webapps` folder.
3. In the PowerShell session with the database variables set, start Tomcat using its `bin\\catalina.bat run` command.
4. Open http://localhost:8080/SmartSpend/
5. Sign in, then test the authenticated database status endpoint at http://localhost:8080/SmartSpend/db-test

## Notes
- Tomcat 10.1 uses Jakarta Servlet 6.0 (`jakarta.*` imports).
- Phase 1 provides the welcome page and database connection test.
- Phase 2 provides BCrypt password hashing, registration, login, logout and a session-protected dashboard.

## Phase 2 test steps

1. Build the WAR:
   ```powershell
   mvn clean package
   ```
2. Stop Tomcat, copy `target\SmartSpend.war` to Tomcat's `webapps` folder, and start Tomcat from a PowerShell session that contains the database environment variables.
3. Open `http://localhost:8080/SmartSpend/`.
4. Select **Create account**, enter a name, email and matching password (minimum 8 characters), then submit.
5. Open **Sign in**, enter the registered email and password.
6. Confirm that the dashboard shows the registered name and email.
7. Open `http://localhost:8080/SmartSpend/dashboard` in an incognito or private window. It should redirect to the login page.
8. Use **Log out** on the dashboard and confirm that subsequent dashboard access requires login again.

## Phase 3 test steps

1. Sign in, then open `http://localhost:8080/SmartSpend/dashboard`.
2. Confirm the monthly total, transaction count, highest expense and category summary.
3. Open **Expenses** or `http://localhost:8080/SmartSpend/expenses`.
4. Select **Add expense**, enter a description, amount, category and date, then save.
5. Confirm the expense appears in the table and dashboard summary.
6. Edit the expense using its **Edit** link, change one or more fields and save.
7. Test description search, category, date range and month filters.
8. Delete an expense and confirm the browser confirmation dialog.
9. Sign out and confirm expense URLs redirect to login.

## Phase 4 test steps

1. Sign in and open `http://localhost:8080/SmartSpend/budgets`.
2. Select a month and enter a monthly budget amount, then save.
3. Add expenses dated within the selected month and reopen the budget page.
4. Confirm the dashboard and budget page show budget, spent, remaining and percentage values.
5. Increase spending to at least 80% and verify the warning status and progress bar.
6. Reach or exceed 100% and verify the exceeded status.
7. Refresh the page several times and confirm duplicate budget alerts are not added.
8. Choose an existing month and update the amount to confirm the budget is updated rather than duplicated.
9. Open `/budgets` after signing out and confirm it redirects to login.

## Phase 5 test steps

1. Sign in and open `http://localhost:8080/SmartSpend/goals`.
2. Select **Create goal** and enter a name, target, optional initial savings, target date, category and description.
3. Confirm the goal card displays saved, remaining, target date and progress.
4. Add savings and confirm the saved amount and progress increase.
5. Try an amount that would exceed the target and confirm it is rejected.
6. Edit a goal and confirm a reduced target cannot be below the amount already saved.
7. Delete a goal and confirm the browser confirmation dialog.
8. Open the dashboard and confirm active goals, saved amount, target amount and recent goals appear.
9. Sign out and confirm `/goals` redirects to login.

## Phase 6 test steps

1. Sign in and open `http://localhost:8080/SmartSpend/reminders`.
2. Create a reminder with title, category, date and optional description, amount or time.
3. Confirm it appears in Today, Upcoming or Past sections.
4. Edit and mark a reminder complete.
5. Delete a reminder and confirm the browser confirmation dialog.
6. Test search, category, date and status filters.
7. Create or set a reminder due at the current date/time and check the dashboard notification badge.
8. Refresh the dashboard and confirm the unread notification badge does not repeat indefinitely.
9. Confirm the dashboard shows upcoming, today's and completed reminder counts plus the next reminder.
10. Sign out and confirm `/reminders` redirects to login.

## Phase 7 test steps

1. Sign in and open `http://localhost:8080/SmartSpend/planner`.
2. Enter a Travel, Shopping or other activity, estimated cost and planned date.
3. Select **Analyze activity** and review affordability, remaining budget, spending limit and monthly savings suggestion.
4. For a travel activity, confirm the practical lower-cost and postponement suggestions.
5. Select **Save plan** and confirm the analyzed plan appears in saved plans.
6. Edit a saved plan and confirm it is recalculated and updated.
7. Delete a saved plan and confirm the browser confirmation dialog.
8. Open the dashboard and confirm the Smart Planner summary and saved-plan link.
9. Sign out and confirm `/planner` redirects to login.

## Phase 8 test steps

1. Sign in and open `http://localhost:8080/SmartSpend/reports`.
2. Select a month and view the monthly total, transaction count, highest expense and category chart.
3. Select a custom start and end date and confirm the report validates reversed ranges.
4. Confirm the expense table shows only the logged-in user's expenses.
5. Download PDF and open the file to verify period, summary, categories and expense details.
6. Download Excel and confirm the Summary and Expense Details sheets contain matching data.
7. Change report parameters and confirm the downloaded filename and contents match the period.
8. Sign out and confirm `/reports` and `/reports/pdf` or `/reports/excel` redirect to login.
