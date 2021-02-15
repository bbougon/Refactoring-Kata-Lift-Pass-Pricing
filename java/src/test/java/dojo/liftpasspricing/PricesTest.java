package dojo.liftpasspricing;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import spark.Spark;

import java.sql.Connection;


public class PricesTest {

    public static void main(String[] args) {
        Connection connection = null;
        try {
            connection = Prices.createApp();
            JsonPath response = RestAssured.
                    given().
                    port(4567).
                    when().
                    // construct some proper url parameters
                            get("/prices").
                            then().
                            assertThat().
                            statusCode(200).
                            assertThat().
                            contentType("application/json").
                            extract().jsonPath();

            System.out.println("test result:" + response.getString("cost"));
        } catch (Exception e) {
            Spark.stop();
            System.out.println(e.getMessage());
        } finally {
            stop(connection);
        }
    }

    private static void stop(final Connection connection) {
        Spark.stop();
        try{
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
