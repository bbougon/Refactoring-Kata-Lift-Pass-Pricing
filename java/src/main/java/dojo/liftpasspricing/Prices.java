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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Prices {

    static class Price {
        private static int computePriceDuringNight(final Integer age, final int baseCost) {
            int cost;
            if (age != null && age >= 6) {
                if (age > 64) {
                    cost = (int) Math.ceil(baseCost * .4);
                } else {
                    cost = baseCost;
                }
            } else {
                cost = 0;
            }
            return cost;
        }

        private static int computePriceThatIsNotDuringNight(final Integer age, final int reduction, final int baseCost) {
            int cost;
            if (age != null && age < 15) {
                cost = (int) Math.ceil(baseCost * .7);
            } else {
                if (age == null) {
                    cost = (int) Math.ceil(baseCost * (1 - reduction / 100.0));
                } else {
                    if (age > 64) {
                        cost = (int) Math.ceil(baseCost * .75 * (1 - reduction / 100.0));
                    } else {
                        cost = (int) Math.ceil(baseCost * (1 - reduction / 100.0));
                    }
                }
            }
            return cost;
        }
    }

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

            int cost = 0;
            try (PreparedStatement costStmt = connection.prepareStatement( //
                    "SELECT cost FROM base_price " + //
                            "WHERE type = ?")) {
                costStmt.setString(1, req.queryParams("type"));
                try (ResultSet result = costStmt.executeQuery()) {
                    result.next();
                    int baseCost = result.getInt("cost");

                    int reduction;
                    boolean isHoliday;

                    if (age != null && age < 6) {
                        cost = 0;
                    } else {
                        if (!req.queryParams("type").equals("night")) {
                            DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");

                            String stringDate = req.queryParams("date");
                            Date date = null;
                            if (stringDate != null) {
                                date = isoFormat.parse(stringDate);
                            }
                            try (PreparedStatement holidayStmt = connection.prepareStatement( //
                                    "SELECT * FROM holidays")) {
                                Holidays holidays2 = new Holidays();

                                try (ResultSet holidays = holidayStmt.executeQuery()) {

                                    while (holidays.next()) {
                                        holidays2.add(holidays.getDate("holiday"));

                                    }

                                }
                                final Date date2 = date;
                                isHoliday = holidays2.holidays().stream().anyMatch(holiday ->
                                {
                                    if (date2 != null) {
                                        return date2.getYear() == holiday.getYear() && //
                                                date2.getMonth() == holiday.getMonth() && //
                                                date2.getDate() == holiday.getDate();
                                    }
                                    return false;
                                });
                            }

                            reduction = computeDiscount(date, isHoliday);

                            // TODO apply reduction for others
                            cost = Price.computePriceThatIsNotDuringNight(age, reduction, baseCost);
                        } else {
                            cost = Price.computePriceDuringNight(age, baseCost);
                        }
                    }
                }
            }

            return displayCost(cost);
        });

        after((req, res) -> {
            res.type("application/json");
        });

        return connection;
    }

    private static int computeDiscount(final Date date, final boolean isHoliday) {
        int reduction = 0;
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            if (!isHoliday && calendar.get(Calendar.DAY_OF_WEEK) == 2) {
                reduction = 35;
            }
        }
        return reduction;
    }


    private static String displayCost(final int cost) {
        return "{ \"cost\": " + cost + "}";
    }

}
