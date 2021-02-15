package dojo.liftpasspricing;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import spark.Spark;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;


public class PricesTest {

    static List<TestHarness> harness = new ArrayList<>();

    public static class TestHarness {
        String type;
        private Integer age;
        private String date;

        public TestHarness(final String type, final Integer age, final String date) {
            this.type = type;
            this.age = age;
            this.date = date;
        }
    }

    static {
//        yyyy-MM-dd => forma de ma date
//        harness.add(new TestHarness(null));
        harness.add(new TestHarness("night", null, null));
        harness.add(new TestHarness("night", -5, null));
        harness.add(new TestHarness("night", 0, null));
        harness.add(new TestHarness("night", 6, null));
        harness.add(new TestHarness("night", 18, null));
        harness.add(new TestHarness("night", 66, null));
        harness.add(new TestHarness("night", null, "2021-02-14"));
        harness.add(new TestHarness("night", -5, "2021-02-14"));
        harness.add(new TestHarness("night", 0, "2021-02-14"));
        harness.add(new TestHarness("night", 6, "2021-02-14"));
        harness.add(new TestHarness("night", 18, "2021-02-14"));
        harness.add(new TestHarness("night", 66, "2021-02-14"));
        harness.add(new TestHarness("night", null, "2021-02-15"));
        harness.add(new TestHarness("night", -5, "2021-02-15"));
        harness.add(new TestHarness("night", 0, "2021-02-15"));
        harness.add(new TestHarness("night", 6, "2021-02-15"));
        harness.add(new TestHarness("night", 18, "2021-02-15"));
        harness.add(new TestHarness("night", 66, "2021-02-15"));
        harness.add(new TestHarness("night", null, "2019-02-18"));
        harness.add(new TestHarness("night", -5, "2019-02-18"));
        harness.add(new TestHarness("night", 0, "2019-02-18"));
        harness.add(new TestHarness("night", 6, "2019-02-18"));
        harness.add(new TestHarness("night", 18, "2019-02-18"));
        harness.add(new TestHarness("night", 66, "2019-02-18"));

        harness.add(new TestHarness("1jour", null, null));
        harness.add(new TestHarness("1jour", -5, null));
        harness.add(new TestHarness("1jour", 0, null));
        harness.add(new TestHarness("1jour", 6, null));
        harness.add(new TestHarness("1jour", 18, null));
        harness.add(new TestHarness("1jour", 66, null));
        harness.add(new TestHarness("1jour", null, "2021-02-14"));
        harness.add(new TestHarness("1jour", -5, "2021-02-14"));
        harness.add(new TestHarness("1jour", 0, "2021-02-14"));
        harness.add(new TestHarness("1jour", 6, "2021-02-14"));
        harness.add(new TestHarness("1jour", 18, "2021-02-14"));
        harness.add(new TestHarness("1jour", 66, "2021-02-14"));
        harness.add(new TestHarness("1jour", null, "2021-02-15"));
        harness.add(new TestHarness("1jour", -5, "2021-02-15"));
        harness.add(new TestHarness("1jour", 0, "2021-02-15"));
        harness.add(new TestHarness("1jour", 6, "2021-02-15"));
        harness.add(new TestHarness("1jour", 18, "2021-02-15"));
        harness.add(new TestHarness("1jour", 66, "2021-02-15"));
        harness.add(new TestHarness("1jour", null, "2019-02-18"));
        harness.add(new TestHarness("1jour", -5, "2019-02-18"));
        harness.add(new TestHarness("1jour", 0, "2019-02-18"));
        harness.add(new TestHarness("1jour", 6, "2019-02-18"));
        harness.add(new TestHarness("1jour", 18, "2019-02-18"));
        harness.add(new TestHarness("1jour", 66, "2019-02-18"));
    }

    public static void main(String[] args) {
        Connection connection = null;

        try {
            connection = Prices.createApp();
            harness.forEach(testHarness -> {
                String queryParam = "";
                if (testHarness.type != null) {
                    queryParam += "?type=" + testHarness.type;
                }
                if(testHarness.age != null) {
                    queryParam += "&age=" + testHarness.age;
                }
                if(testHarness.date != null) {
                    queryParam += "&date=" + testHarness.date;
                }

                JsonPath response = RestAssured.
                        given().
                        port(4567).
                        when().
                        // construct some proper url parameters
                                get("/prices" + queryParam).
                                then().
                                assertThat().
                                statusCode(200).
                                assertThat().
                                contentType("application/json").
                                extract().jsonPath();

                System.out.println(String.format("Lorsque je choisis un forfait de type '%s', pour une personne de '%s' an(s), en date du '%s', alors mon forfait me coûte '%s'", testHarness.type, testHarness.age, testHarness.date, response.getString("cost")));
            });
        } catch (Exception e) {
            Spark.stop();
            System.out.println(e.getMessage());
        } finally {
            stop(connection);
        }
    }

    private static void stop(final Connection connection) {
        Spark.stop();
        try {
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
