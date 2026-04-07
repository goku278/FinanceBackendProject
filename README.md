Finance Backend System Documentation
Data Initialization (Important)
This project uses a DataInitializer class to automatically insert demo data when the
application starts. This ensures that all APIs work without errors during testing.
1 Creates a default ADMIN user (username: admin, password: admin123)
2 Creates sample financial records (income and expenses)
3 Helps test APIs immediately without manual database setup
4 Ensures system is ready to use after startup
Why This is Useful
1 No need to manually insert data
2 Prevents empty database issues
3 Helps testers and developers quickly verify APIs
4 Improves development and demo experience
Authentication APIs
1 POST /auth/login → Login and get access + refresh token
2 POST /auth/refresh → Refresh access token
3 POST /auth/logout → Logout user
Access Token & Refresh Token (Simple Explanation)
Access Token: A short-lived token used to access APIs. You must send it in every
request in the Authorization header.
Refresh Token: A long-lived token used to get a new access token when the old one
expires. It helps users stay logged in.
Login Flow
1 User sends username and password
2 Server verifies credentials
3 Server returns accessToken and refreshToken
4 Client uses accessToken to call APIs
When Access Token Expires
1 Access token expires after some time
2 Client calls /auth/refresh with refresh token
3 Server generates new access token
4 User continues without logging in again
What Happens on Logout
1 User calls /auth/logout
2 Refresh token is deleted from database
3 User cannot generate new access tokens
4 User must login again
Financial Records APIs
1 GET /api/records
2 POST /api/records (ADMIN)
3 PUT /api/records/{id} (ADMIN)
4 DELETE /api/records/{id} (ADMIN)
Dashboard APIs
1 GET /api/dashboard/summary
2 GET /api/dashboard/category-totals
3 GET /api/dashboard/recent-activity
4 GET /api/dashboard/monthly-trends
Admin APIs
1 GET /api/admin/users
2 POST /api/admin/users
3 PUT /api/admin/users/{id}/roles
4 PATCH /api/admin/users/{id}/status
Role-Based Access (Simple)
Role Access
ADMIN Full access (create, update, delete, manage users)
ADMIN also has privileges to modify roles, modify the session active to true or false
Resulting in providing role privileges to ANALYST, VIEWER, ADMIN (himself / herself)
And based on these modifications, users can view summary, dashboard, finance data.
Also if from the user role, if any role is removed for any specific user, then that particular
user cannot login, cannot perform actions like view data.
ANALYST View and analyze Dashboard and Finance Records data only
VIEWER View Dashboard data only
API Tables
Authentication APIs
Method Endpoint Description
POST /auth/login Login and get tokens
POST /auth/refresh Refresh token
POST /auth/logout Logout
Financial APIs
GET /api/records Fetch recordsPOST PUT DELETE /api/records /api/records/{id}
/api/records/{id}
Create record
Update record
Delete record
Dashboard APIs
GET /api/dashboard/summary Summary
GET /api/dashboard/category-to
tals
Category totals
GET /api/dashboard/recent-activ
ity
Recent activity
GET /api/dashboard/monthly-tre
nds
Monthly trends
Admin APIs
GET /api/admin/users Get users
POST /api/admin/users Create user
PUT /api/admin/users/{id}/roles Update roles
PATCH /api/admin/users/{id}/status Update status
🚀 Project Setup & Execution Guide
🔹 1. Prerequisites
Make sure you have the following installed:
●
✅ Java 17+
●
✅ Gradle
●
✅ MySQL Server
●
✅ Postman (for testing APIs)
●
✅ IDE (IntelliJ / VS Code / Eclipse)
🔹 2. Clone the Project
git clone https://github.com/goku278/FinanceBackendProject.git
cd finance_app
🔹 3. Configure MySQL Database
Step 1: Start MySQL
mysql.server start # Mac
or
sudo service mysql start # Linux
Step 2: Login to MySQL
mysql -u root -p
Step 3: Create Database
CREATE DATABASE finance_db;
🔹 4. Configure Application Properties
Open:
src/main/resources/application.properties
Update:
spring.datasource.url=jdbc:mysql://localhost:3306/finance_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
server.port=8181
🔹 5. Run the Application
Using IDE:
●
Click Run
OR using terminal:
mvn spring-boot:run
🔹 6. Data Initialization (IMPORTANT)
When the app starts:
✔ Automatically creates:
●
Admin user
username: admin
password: admin123
✔ Inserts sample financial records
👉 So you can test APIs immediately without manual DB setup
🔹 7. Test APIs Using Postman
Step 1: Login
POST http://localhost:8181/auth/login
Body:
{
"username": "admin",
"password": "admin123"
}
Step 2: Copy Access Token
Response:
{
"accessToken": "...",
"refreshToken": "...",
"tokenType": "Bearer"
}
Step 3: Use Token in Requests
Header:
Authorization: Bearer <accessToken>
Step 4: Call APIs
Example:
GET http://localhost:8181/api/records?page=0&size=10
🔹 8. Refresh Token Flow
POST /auth/refresh
Body:
{
}
"refreshToken": "your_refresh_token"
🔹 9. Logout
POST /auth/logout
👉 This invalidates the refresh token
🧠 Quick Summary
1. Start MySQL
2. Create finance_db
3. Run app
4. Login using admin/admin123
5. Use token to call APIs
