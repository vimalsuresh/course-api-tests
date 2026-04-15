# Course Enrollment API - Automated Test Suite

Automated API tests for the Course Enrollment System built as part of the DWP Senior Test Engineer technical assessment.

## Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 21 | Programming language |
| Maven | 3.9+ | Build and dependency management |
| REST Assured | 5.4.0 | API testing DSL |
| JUnit 5 | 5.10.2 | Test framework |
| AssertJ | 3.25.3 | Fluent assertions |
| Jackson Databind | 2.17.0 | JSON serialisation |

## Project Structure
course-api-tests/
├── pom.xml
└── src/
└── test/
└── java/
└── com/enrollment/api/
├── support/
│   ├── TestConfig.java       # Base class, shared config, server bootup
│   └── Auth.java             # Login helpers for instructor and student
└── tests/
├── HealthCheckTest.java        # Server health check
├── AuthenticationTest.java     # Login tests for both roles
├── CourseTests.java            # Course endpoint tests
└── EnrolmentJourneyTest.java   # End to end enrolment journey

## How to Run

### Prerequisites
- Java 21
- Maven 3.9+

### Run all tests
mvn clean test

### Run a specific test class
mvn clean test -Dtest=HealthCheckTest
mvn clean test -Dtest=AuthenticationTest
mvn clean test -Dtest=CourseTests
mvn clean test -Dtest=EnrolmentJourneyTest

> The first request may take up to 60 seconds as the server cold starts on Render's free tier. The base class handles this automatically by polling the /status endpoint before tests begin.

## Test Coverage

### HealthCheckTest
| Test | Description |
|---|---|
| serverIsRunning | GET /status returns 200 and a status message |

### AuthenticationTest
| Test | Description |
|---|---|
| instructorLoginSuccess | Valid instructor credentials return 200 and a token |
| instructorLoginWrongPassword | Wrong password returns 401 |
| instructorLoginUnknownUser | Unknown instructor username returns 401 |
| studentLoginSuccess | Valid student credentials return 200 and a token |
| studentLoginWrongPassword | Wrong password returns 401 |
| studentLoginUnknownUser | Unknown student username returns 401 |
| eachLoginProducesANewToken | Each login returns a different token |

### CourseTests
| Test | Description |
|---|---|
| instructorCreatesCourse | Instructor can create a course successfully |
| duplicateCourseCodeRejected | Duplicate course code returns 400 |
| invalidDateRangeRejected | Start date after end date returns 400 |
| missingRequiredFieldsRejected | Missing fields returns 400 |
| createCourseWithoutTokenReturns401 | No token returns 401 |
| studentTokenCannotCreateCourse | Student token returns 403 |
| getAllCoursesReturnsList | GET /courses/all returns a non-empty list |
| allCoursesHaveExpectedFields | Each course has title, courseCode and instructor fields |
| searchByTitleReturnsMatchingCourses | Title search returns matching results |
| titleSearchIsCaseInsensitive | JAVA and java return the same number of results |
| searchByTitleNoMatch | Non-existent title returns empty list or 404 |
| checkAvailabilityKnownCourse | Availability returns courseCode, title and availableSlots |
| newCourseHasFullCapacity | Newly created course shows full capacity |
| checkAvailabilityUnknownCode | Unknown course code returns 404 |
| instructorDeletesCourse | Instructor can delete their own course |
| deletedCourseIsGone | Deleted course returns 404 on availability check |
| deleteWithoutTokenReturns401 | No token returns 401 |
| deleteNonExistentCourse | Non-existent course ID returns 404 |

### EnrolmentJourneyTest — End to End
| Step | Description |
|---|---|
| Step 1 | Instructor logs in and receives a token |
| Step 2 | Instructor creates a course |
| Step 3 | Student logs in and receives a token |
| Step 4 | Student checks availability — full capacity confirmed |
| Step 5 | Student enrols in the course — 201 returned |
| Step 6 | Available slots decrement by 1 after enrolment |
| Step 7 | Enrolling in the same course again returns 400 |
| Step 8 | Enrolment without a token returns 401 |
| Step 9 | Enrolment in a non-existent course returns 404 |
| Step 10 | Enrolled course appears in active enrolments |
| Step 11 | Active enrolments endpoint requires a valid token |
| Step 12 | Enrolled course appears in history with active status |
| Step 13 | Enrolment history endpoint requires a valid token |
| Step 14 | Student drops the course — 200 returned |
| Step 15 | Available slots increment back after drop |
| Step 16 | Dropping an already dropped course returns 404 |
| Step 17 | Drop without a token returns 401 |
| Step 18 | History shows the course as dropped |
| Step 19 | Dropped course no longer appears in active enrolments |
| Step 20 | Instructor deletes the test course (cleanup) |