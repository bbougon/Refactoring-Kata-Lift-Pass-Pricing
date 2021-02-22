package dojo.liftpasspricing;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class Holidays {

    private List<Date> holidays = new ArrayList<>();

    public void add(final Date holiday) {
        holidays.add(holiday);
    }

    public List<Date> holidays() {
        return holidays;
    }
}
