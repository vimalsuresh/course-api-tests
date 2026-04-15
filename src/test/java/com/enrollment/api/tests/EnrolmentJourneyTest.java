package com.enrollment.api.tests;

import com.enrollment.api.support.Auth;
import com.enrollment.api.support.TestConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Enrolment - End to End Journey")
public class EnrolmentJourneyTest extends TestConfig {

    private static final String COURSE_CODE = "E2E-" + (System.currentTimeMillis() % 100000);
    private static final int    CAPACITY    = 5;

    private static String instructorToken;
    private static String studentToken;
    private static String courseId;

    // -------------------------------------------------------------------------
    // Setup: Instructor logs in and creates a course
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Step 1 - Instructor logs in")
    void instructorLogsIn() {
        instructorToken = Auth.instructorToken();
        assertThat(instructorToken)
                .as("Instructor should receive a JWT token on login")
                .isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("Step 2 - Instructor creates a course for the journey")
    void instructorCreatesCourse() {
        Response response = given().spec(spec)
                .header("Authorization", "Bearer " + instructorToken)
                .body(Map.of(
                        "title",         "End to End Test Course",
                        "instructor",    Auth.INSTRUCTOR,
                        "courseCode",    COURSE_CODE,
                        "category",      "QA",
                        "totalCapacity", CAPACITY,
                        "startDate",     LocalDate.now().plusDays(1).toString(),
                        "endDate",       LocalDate.now().plusDays(60).toString()
                ))
                .post("/courses")
                .then().extract().response();

        assertThat(response.statusCode())
                .as("Instructor should be able to create a course")
                .isEqualTo(201);

        courseId = response.jsonPath().getString("newCourse._id");
        assertThat(courseId)
                .as("Create response should include the new course ID")
                .isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Student logs in and checks the course
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("Step 3 - Student logs in")
    void studentLogsIn() {
        studentToken = Auth.studentToken();
        assertThat(studentToken)
                .as("Student should receive a JWT token on login")
                .isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("Step 4 - Student checks course availability before enrolling")
    void studentChecksAvailabilityBeforeEnrolment() {
        Response response = given().spec(spec)
                .get("/courses/availability/{code}", COURSE_CODE)
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("courseCode")).isEqualTo(COURSE_CODE);

        Object slots = response.jsonPath().get("availableSlots");
        assertThat(slots).isNotNull();

        if (slots instanceof Integer) {
            assertThat((Integer) slots)
                    .as("Course should have full capacity available before any enrolments")
                    .isEqualTo(CAPACITY);
        }
    }

    // -------------------------------------------------------------------------
    // Enrolment - happy path
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("Step 5 - Student enrols in the course")
    void studentEnrolsInCourse() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT, "courseCode", COURSE_CODE))
                .post("/enrolments/enrol")
                .then().extract().statusCode();

        assertThat(status)
                .as("Enrolment should return 201 Created")
                .isEqualTo(201);
    }

    @Test
    @Order(6)
    @DisplayName("Step 6 - Available slots decrease by 1 after enrolment")
    void availableSlotsDecrementAfterEnrolment() {
        Response response = given().spec(spec)
                .get("/courses/availability/{code}", COURSE_CODE)
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        Object slots = response.jsonPath().get("availableSlots");
        if (slots instanceof Integer) {
            assertThat((Integer) slots)
                    .as("Available slots should have decremented by 1 after enrolment")
                    .isEqualTo(CAPACITY - 1);
        }
    }

    // -------------------------------------------------------------------------
    // Enrolment - negative paths
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("Step 7 - Enrolling in the same course a second time returns 400")
    void duplicateEnrolmentRejected() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT, "courseCode", COURSE_CODE))
                .post("/enrolments/enrol")
                .then().extract().statusCode();

        assertThat(status)
                .as("Duplicate enrolment should return 400 Bad Request")
                .isEqualTo(400);
    }

    @Test
    @Order(8)
    @DisplayName("Step 8 - Enrolment without a token returns 401")
    void enrolWithoutTokenReturns401() {
        int status = given().spec(spec)
                .body(Map.of("username", Auth.STUDENT, "courseCode", COURSE_CODE))
                .post("/enrolments/enrol")
                .then().extract().statusCode();

        assertThat(status)
                .as("Enrolment without a token should return 401")
                .isEqualTo(401);
    }

    @Test
    @Order(9)
    @DisplayName("Step 9 - Enrolment in a non-existent course returns 404")
    void enrolInNonExistentCourseReturns404() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT, "courseCode", "GHOST-00000"))
                .post("/enrolments/enrol")
                .then().extract().statusCode();

        assertThat(status)
                .as("Enrolment in a non-existent course should return 404")
                .isEqualTo(404);
    }

    // -------------------------------------------------------------------------
    // View enrolments
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("Step 10 - Enrolled course appears in student's active enrolments")
    void courseAppearsInActiveEnrolments() {
        Response response = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT))
                .post("/enrolments/active")
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        List<Map<String, Object>> active = response.jsonPath().getList("$");
        assertThat(active)
                .as("Active enrolments should not be empty after enrolling")
                .isNotEmpty();

        boolean found = active.stream()
                .anyMatch(e -> COURSE_CODE.equals(e.get("courseCode")));
        assertThat(found)
                .as("The enrolled course should appear in active enrolments")
                .isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("Step 11 - Active enrolments requires a valid token")
    void activeEnrolmentsWithoutTokenReturns401() {
        int status = given().spec(spec)
                .body(Map.of("username", Auth.STUDENT))
                .post("/enrolments/active")
                .then().extract().statusCode();

        assertThat(status)
                .as("Active enrolments endpoint should require authentication")
                .isEqualTo(401);
    }

    @Test
    @Order(12)
    @DisplayName("Step 12 - Enrolled course appears in enrolment history with active status")
    void courseAppearsInHistoryAsActive() {
        Response response = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT))
                .post("/enrolments/history")
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        List<Map<String, Object>> history = response.jsonPath().getList("$");
        assertThat(history).isNotEmpty();

        boolean activeRecord = history.stream()
                .anyMatch(e ->
                        COURSE_CODE.equals(e.get("courseCode")) &&
                        "active".equalsIgnoreCase(String.valueOf(e.get("status")))
                );
        assertThat(activeRecord)
                .as("History should show the course with an 'active' status")
                .isTrue();
    }

    @Test
    @Order(13)
    @DisplayName("Step 13 - Enrolment history requires a valid token")
    void historyWithoutTokenReturns401() {
        int status = given().spec(spec)
                .body(Map.of("username", Auth.STUDENT))
                .post("/enrolments/history")
                .then().extract().statusCode();

        assertThat(status)
                .as("History endpoint should require authentication")
                .isEqualTo(401);
    }

    // -------------------------------------------------------------------------
    // Drop - happy path
    // -------------------------------------------------------------------------

    @Test
    @Order(14)
    @DisplayName("Step 14 - Student drops the course")
    void studentDropsCourse() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT, "courseCode", COURSE_CODE))
                .post("/enrolments/drop")
                .then().extract().statusCode();

        assertThat(status)
                .as("Dropping an active enrolment should return 200")
                .isEqualTo(200);
    }

    @Test
    @Order(15)
    @DisplayName("Step 15 - Available slots increment back after dropping the course")
    void slotsIncrementAfterDrop() {
        Response response = given().spec(spec)
                .get("/courses/availability/{code}", COURSE_CODE)
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        Object slots = response.jsonPath().get("availableSlots");
        if (slots instanceof Integer) {
            assertThat((Integer) slots)
                    .as("Available slots should return to full capacity after the only student drops")
                    .isEqualTo(CAPACITY);
        }
    }

    // -------------------------------------------------------------------------
    // Drop - negative paths
    // -------------------------------------------------------------------------

    @Test
    @Order(16)
    @DisplayName("Step 16 - Dropping a course already dropped returns 404")
    void dropAlreadyDroppedCourseReturns404() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT, "courseCode", COURSE_CODE))
                .post("/enrolments/drop")
                .then().extract().statusCode();

        assertThat(status)
                .as("Dropping a course with no active enrolment should return 404")
                .isEqualTo(404);
    }

    @Test
    @Order(17)
    @DisplayName("Step 17 - Drop without a token returns 401")
    void dropWithoutTokenReturns401() {
        int status = given().spec(spec)
                .body(Map.of("username", Auth.STUDENT, "courseCode", COURSE_CODE))
                .post("/enrolments/drop")
                .then().extract().statusCode();

        assertThat(status)
                .as("Drop without a token should return 401")
                .isEqualTo(401);
    }

    // -------------------------------------------------------------------------
    // Post-drop history verification
    // -------------------------------------------------------------------------

    @Test
    @Order(18)
    @DisplayName("Step 18 - History shows the course as dropped after dropping")
    void historyShowsDroppedStatus() {
        Response response = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT))
                .post("/enrolments/history")
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        List<Map<String, Object>> history = response.jsonPath().getList("$");
        boolean droppedRecord = history.stream()
                .anyMatch(e ->
                        COURSE_CODE.equals(e.get("courseCode")) &&
                        "dropped".equalsIgnoreCase(String.valueOf(e.get("status")))
                );
        assertThat(droppedRecord)
                .as("History should show the course status as 'dropped'")
                .isTrue();
    }

    @Test
    @Order(19)
    @DisplayName("Step 19 - Dropped course no longer appears in active enrolments")
    void droppedCourseNotInActiveEnrolments() {
        Response response = given().spec(spec)
                .header("Authorization", "Bearer " + studentToken)
                .body(Map.of("username", Auth.STUDENT))
                .post("/enrolments/active")
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        List<Map<String, Object>> active = response.jsonPath().getList("$");
        boolean stillActive = active.stream()
                .anyMatch(e -> COURSE_CODE.equals(e.get("courseCode")));
        assertThat(stillActive)
                .as("Dropped course should not appear in active enrolments")
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    @Test
    @Order(20)
    @DisplayName("Step 20 - Instructor deletes the test course (cleanup)")
    void instructorDeletesTestCourse() {
        assertThat(courseId)
                .as("Course ID should have been captured in Step 2")
                .isNotBlank();

        int status = given().spec(spec)
                .header("Authorization", "Bearer " + instructorToken)
                .delete("/courses/{id}", courseId)
                .then().extract().statusCode();

        assertThat(status)
                .as("Instructor should be able to delete the course")
                .isEqualTo(200);
    }
}