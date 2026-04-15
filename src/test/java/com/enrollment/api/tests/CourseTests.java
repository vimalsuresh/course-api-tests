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
@DisplayName("Course Endpoint Tests")
public class CourseTests extends TestConfig {

    private static final String COURSE_CODE  = "QA-" + (System.currentTimeMillis() % 100000);
    private static final String COURSE_TITLE = "API Testing with Java";
    private static String courseId;

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("POST /courses - instructor can create a course successfully")
    void instructorCreatesCourse() {
        Response response = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .body(Map.of(
                        "title",         COURSE_TITLE,
                        "instructor",    Auth.INSTRUCTOR,
                        "courseCode",    COURSE_CODE,
                        "category",      "Quality Assurance",
                        "totalCapacity", 25,
                        "startDate",     LocalDate.now().plusDays(1).toString(),
                        "endDate",       LocalDate.now().plusDays(60).toString()
                ))
                .post("/courses")
                .then().extract().response();

        assertThat(response.statusCode())
                .as("Valid course creation should return 201")
                .isEqualTo(201);

        courseId = response.jsonPath().getString("newCourse._id");
        assertThat(courseId)
                .as("Response should include the created course ID")
                .isNotBlank();

        System.out.println("Created course ID: " + courseId);
    }

    @Test
    @Order(2)
    @DisplayName("POST /courses - duplicate course code returns 400")
    void duplicateCourseCodeRejected() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .body(Map.of(
                        "title",         "Duplicate Course",
                        "instructor",    Auth.INSTRUCTOR,
                        "courseCode",    COURSE_CODE,
                        "category",      "Quality Assurance",
                        "totalCapacity", 10,
                        "startDate",     LocalDate.now().plusDays(1).toString(),
                        "endDate",       LocalDate.now().plusDays(30).toString()
                ))
                .post("/courses")
                .then().extract().statusCode();

        assertThat(status)
                .as("Duplicate course code should be rejected with 400")
                .isEqualTo(400);
    }

    @Test
    @Order(3)
    @DisplayName("POST /courses - start date after end date returns 400")
    void invalidDateRangeRejected() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .body(Map.of(
                        "title",         "Bad Dates Course",
                        "instructor",    Auth.INSTRUCTOR,
                        "courseCode",    "BADDATE-" + System.currentTimeMillis() % 10000,
                        "category",      "Test",
                        "totalCapacity", 5,
                        "startDate",     "2026-12-01",
                        "endDate",       "2026-01-01"
                ))
                .post("/courses")
                .then().extract().statusCode();

        assertThat(status)
                .as("Start date after end date should return 400")
                .isEqualTo(400);
    }

    @Test
    @Order(4)
    @DisplayName("POST /courses - missing required fields returns 400")
    void missingRequiredFieldsRejected() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .body(Map.of("title", "Incomplete Course"))
                .post("/courses")
                .then().extract().statusCode();

        assertThat(status)
                .as("Request missing required fields should return 400")
                .isEqualTo(400);
    }

    @Test
    @Order(5)
    @DisplayName("POST /courses - request without token returns 401")
    void createCourseWithoutTokenReturns401() {
        int status = given().spec(spec)
                .body(Map.of(
                        "title",         "No Token Course",
                        "instructor",    Auth.INSTRUCTOR,
                        "courseCode",    "NOTOKEN-01",
                        "category",      "Test",
                        "totalCapacity", 5,
                        "startDate",     LocalDate.now().plusDays(1).toString(),
                        "endDate",       LocalDate.now().plusDays(30).toString()
                ))
                .post("/courses")
                .then().extract().statusCode();

        assertThat(status)
                .as("Creating a course without a token should return 401")
                .isEqualTo(401);
    }

    @Test
    @Order(6)
    @DisplayName("POST /courses - student token cannot create a course, returns 403")
    void studentTokenCannotCreateCourse() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.studentToken())
                .body(Map.of(
                        "title",         "Student Creating Course",
                        "instructor",    Auth.INSTRUCTOR,
                        "courseCode",    "STUDENTCREATE-01",
                        "category",      "Test",
                        "totalCapacity", 5,
                        "startDate",     LocalDate.now().plusDays(1).toString(),
                        "endDate",       LocalDate.now().plusDays(30).toString()
                ))
                .post("/courses")
                .then().extract().statusCode();

        assertThat(status)
                .as("A student token should not be able to create a course, expect 403")
                .isEqualTo(403);
    }

    // -------------------------------------------------------------------------
    // SEARCH ALL
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("GET /courses/all - returns full list of courses")
    void getAllCoursesReturnsList() {
        Response response = given().spec(spec)
                .get("/courses/all")
                .then().extract().response();

        assertThat(response.statusCode())
                .as("GET /courses/all should return 200")
                .isEqualTo(200);

        List<?> courses = response.jsonPath().getList("$");
        assertThat(courses)
                .as("Course list should not be empty")
                .isNotEmpty();

        System.out.println("Total courses in catalog: " + courses.size());
    }

    @Test
    @Order(8)
    @DisplayName("GET /courses/all - each course has expected fields")
    void allCoursesHaveExpectedFields() {
        List<Map<String, Object>> courses = given().spec(spec)
                .get("/courses/all")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");

        assertThat(courses).isNotEmpty();

        courses.forEach(course -> {
            assertThat(course).containsKey("title");
            assertThat(course).containsKey("courseCode");
            assertThat(course).containsKey("instructor");
        });
    }

    // -------------------------------------------------------------------------
    // SEARCH BY TITLE
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("GET /courses/title/{title} - returns courses matching the title")
    void searchByTitleReturnsMatchingCourses() {
        Response response = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .get("/courses/title/{title}", "Java")
                .then().extract().response();

        assertThat(response.statusCode())
                .as("Title search should return 200")
                .isEqualTo(200);

        List<Map<String, Object>> results = response.jsonPath().getList("$");
        assertThat(results)
                .as("Search by 'Java' should return at least one course")
                .isNotEmpty();

        results.forEach(course ->
                assertThat(course.get("title").toString().toLowerCase())
                        .as("Every returned course title should contain the search term")
                        .contains("java")
        );
    }

    @Test
    @Order(10)
    @DisplayName("GET /courses/title/{title} - search is case insensitive")
    void titleSearchIsCaseInsensitive() {
        Response upperCase = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .get("/courses/title/{title}", "JAVA")
                .then().extract().response();

        Response lowerCase = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .get("/courses/title/{title}", "java")
                .then().extract().response();

        assertThat(upperCase.statusCode()).isEqualTo(200);
        assertThat(lowerCase.statusCode()).isEqualTo(200);

        List<?> upperResults = upperCase.jsonPath().getList("$");
        List<?> lowerResults = lowerCase.jsonPath().getList("$");

        assertThat(upperResults.size())
                .as("Case should not affect the number of results returned")
                .isEqualTo(lowerResults.size());
    }

    @Test
    @Order(11)
    @DisplayName("GET /courses/title/{title} - no match returns empty list or 404")
    void searchByTitleNoMatch() {
        Response response = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .get("/courses/title/{title}", "ZZZNOMATCHCOURSE999")
                .then().extract().response();

        assertThat(response.statusCode())
                .as("No match should return 200 with empty list or 404")
                .isIn(200, 404);

        if (response.statusCode() == 200) {
            List<?> results = response.jsonPath().getList("$");
            assertThat(results)
                    .as("No matching title should return an empty list")
                    .isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // CHECK AVAILABILITY
    // -------------------------------------------------------------------------

    @Test
    @Order(12)
    @DisplayName("GET /courses/availability/{code} - returns availability info for a known course")
    void checkAvailabilityKnownCourse() {
        Response response = given().spec(spec)
                .get("/courses/availability/{code}", COURSE_CODE)
                .then().extract().response();

        assertThat(response.statusCode())
                .as("Availability check for a valid course code should return 200")
                .isEqualTo(200);

        assertThat(response.jsonPath().getString("courseCode"))
                .as("Returned courseCode should match the requested code")
                .isEqualTo(COURSE_CODE);

        assertThat(response.jsonPath().getString("title"))
                .as("Response should include the course title")
                .isNotBlank();

        assertThat((Object) response.jsonPath().get("availableSlots"))
                .as("Response should include an availableSlots field")
                .isNotNull();
    }

    @Test
    @Order(13)
    @DisplayName("GET /courses/availability/{code} - newly created course has full capacity available")
    void newCourseHasFullCapacity() {
        Response response = given().spec(spec)
                .get("/courses/availability/{code}", COURSE_CODE)
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        Object slots = response.jsonPath().get("availableSlots");
        assertThat(slots)
                .as("A brand new course with no enrolments should show slots available")
                .isNotNull();

        if (slots instanceof Integer) {
            assertThat((Integer) slots)
                    .as("Available slots should equal total capacity for a new course")
                    .isEqualTo(25);
        }
    }

    @Test
    @Order(14)
    @DisplayName("GET /courses/availability/{code} - unknown course code returns 404")
    void checkAvailabilityUnknownCode() {
        int status = given().spec(spec)
                .get("/courses/availability/{code}", "INVALID-000")
                .then().extract().statusCode();

        assertThat(status)
                .as("Unknown course code should return 404")
                .isEqualTo(404);
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Test
    @Order(15)
    @DisplayName("DELETE /courses/{id} - instructor can delete their own course")
    void instructorDeletesCourse() {
        assertThat(courseId)
                .as("Course ID should have been captured in the create test")
                .isNotBlank();

        int status = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .delete("/courses/{id}", courseId)
                .then().extract().statusCode();

        assertThat(status)
                .as("Deleting an existing course should return 200")
                .isEqualTo(200);
    }

    @Test
    @Order(16)
    @DisplayName("DELETE /courses/{id} - deleted course is no longer available")
    void deletedCourseIsGone() {
        int status = given().spec(spec)
                .get("/courses/availability/{code}", COURSE_CODE)
                .then().extract().statusCode();

        assertThat(status)
                .as("Deleted course should no longer be found")
                .isEqualTo(404);
    }

    @Test
    @Order(17)
    @DisplayName("DELETE /courses/{id} - no token returns 401")
    void deleteWithoutTokenReturns401() {
        int status = given().spec(spec)
                .delete("/courses/{id}", "000000000000000000000000")
                .then().extract().statusCode();

        assertThat(status)
                .as("Delete without a token should return 401")
                .isEqualTo(401);
    }

    @Test
    @Order(18)
    @DisplayName("DELETE /courses/{id} - non-existent course ID returns 404")
    void deleteNonExistentCourse() {
        int status = given().spec(spec)
                .header("Authorization", "Bearer " + Auth.instructorToken())
                .delete("/courses/{id}", "000000000000000000000000")
                .then().extract().statusCode();

        assertThat(status)
                .as("Deleting a course that does not exist should return 404")
                .isEqualTo(404);
    }
}