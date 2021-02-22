package dojo.liftpasspricing;

import static spark.Spark.after;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.put;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Prices {

    public static Connection createApp() throws SQLException {

        final Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/lift_pass", "root", "mysql");

        port(4567);

        put("/prices", (req, res) -> {
            int liftPassCost = Integer.parseInt(req.queryParams("cost"));
            String liftPassType = req.queryParams("type");

            try (PreparedStatement stmt = connection.prepareStatement( //
                    "INSERT INTO base_price (type, cost) VALUES (?, ?) " + //
                            "ON DUPLICATE KEY UPDATE cost = ?")) {
                stmt.setString(1, liftPassType);
                stmt.setInt(2, liftPassCost);
                stmt.setInt(3, liftPassCost);
                stmt.execute();
            }

            return "";
        });

        get("/prices", (req, res) -> {
            final Integer age = req.queryParams("age") != null ? Integer.valueOf(req.queryParams("age")) : null;

            try (PreparedStatement costStmt = connection.prepareStatement( //
                    "SELECT cost FROM base_price " + //
                            "WHERE type = ?")) {
                costStmt.setString(1, req.queryParams("type"));
                try (ResultSet result = costStmt.executeQuery()) {
                    result.next();

                    int reduction;
                    boolean isHoliday = false;
                    int tmpCost = 0;

                    if (age != null && age < 6) {
                        return displayCost(tmpCost);
                    } else {
                        reduction = 0;

                        if (!req.queryParams("type").equals("night")) {
                            DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

                            try (PreparedStatement holidayStmt = connection.prepareStatement( //
                                    "SELECT * FROM holidays")) {
                                try (ResultSet holidays = holidayStmt.executeQuery()) {

                                    while (holidays.next()) {
                                        Date holiday = holidays.getDate("holiday");
                                        if (req.queryParams("date") != null) {
                                            Date d = isoFormat.parse(req.queryParams("date"));
                                            if (d.getYear() == holiday.getYear() && //
                                                    d.getMonth() == holiday.getMonth() && //
                                                    d.getDate() == holiday.getDate()) {
                                                isHoliday = true;
                                            }
                                        }
                                    }

                                }
                            }

                            if (req.queryParams("date") != null) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(isoFormat.parse(req.queryParams("date")));
                                if (!isHoliday && calendar.get(Calendar.DAY_OF_WEEK) == 2) {
                                    reduction = 35;
                                }
                            }

                            // TODO apply reduction for others
                            if (age != null && age < 15) {
                                tmpCost = (int) Math.ceil(result.getInt("cost") * .7);
                                return displayCost(tmpCost);
                            } else {
                                if (age == null) {
                                    double cost = result.getInt("cost") * (1 - reduction / 100.0);
                                    tmpCost = (int) Math.ceil(cost);
                                    return displayCost(tmpCost);
                                } else {
                                    if (age > 64) {
                                        double cost = result.getInt("cost") * .75 * (1 - reduction / 100.0);
                                        tmpCost = (int) Math.ceil(cost);
                                        return displayCost(tmpCost);
                                    } else {
                                        double cost = result.getInt("cost") * (1 - reduction / 100.0);
                                        tmpCost = (int) Math.ceil(cost);
                                        return displayCost(tmpCost);
                                    }
                                }
                            }
                        } else {
                            if (age != null && age >= 6) {
                                if (age > 64) {
                                    tmpCost = (int) Math.ceil(result.getInt("cost") * .4);
                                    return displayCost(tmpCost);
                                } else {
                                    tmpCost = result.getInt("cost");
                                    return displayCost(tmpCost);
                                }
                            } else {
                                tmpCost = 0;
                                return displayCost(tmpCost);
                            }
                        }
                    }
                }
            }
        });

        after((req, res) -> {
            res.type("application/json");
        });

        return connection;
    }

    private static String displayCost(final int tmpCost) {
        return "{ \"cost\": " + tmpCost + "}";
    }

}
