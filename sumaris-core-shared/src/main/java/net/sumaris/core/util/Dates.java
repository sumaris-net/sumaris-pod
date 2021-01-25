package net.sumaris.core.util;

/*-
 * #%L
 * Quadrige3 Core :: Quadrige3 Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2017 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.base.Preconditions;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.util.DateUtil;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * <p>Dates class.</p>
 */
public class Dates extends org.apache.commons.lang3.time.DateUtils{

    // See https://www.w3.org/TR/NOTE-datetime
    // Full precision (with millisecond and timezone)
    public static String ISO_TIMESTAMP_REGEXP = "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d\\.\\d+([+-][0-2]\\d:[0-5]\\d|Z)";
    public static Pattern ISO_TIMESTAMP_PATTERN = Pattern.compile("^" + ISO_TIMESTAMP_REGEXP + "$");

    public static String ISO_TIMESTAMP_SPEC = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /**
     * Remove a amount of month to a date
     *
     * @param date a {@link Date} object.
     * @param amount the amount to remove, in month
     * @return a new date (= the given date - amount in month)
     */
    public static Date removeMonth(Date date, int amount) {
    	Preconditions.checkNotNull(date);
    	Preconditions.checkArgument(amount > 0);

    	// Compute the start date
        Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(date.getTime());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH)-amount);
		return calendar.getTime();
    }

    /**
     * Get the number of days between two dates
     *
     * @param startDate a {@link Date} object.
     * @param endDate a {@link Date} object.
     * @return a number of hours
     */
    public static double hoursBetween(Date startDate, Date endDate){
    	double millis = endDate.getTime() - startDate.getTime();
        return millis / (1000 * 60 * 60);
    }

    /**
     * Add to date some hours
     *
     * @param date a {@link Date} object.
     * @param amount a {@link Double} object.
     * @return a date (= date + amount)
     */
    public static Date addHours(Date date, Double amount){
    	long millis = (long) (date.getTime() + amount * (1000 * 60 * 60));
        return new Date(millis);
    }


    /**
     * Get the last second time of a day: 23:59:59 (0 millisecond)
     *
     * @param date a {@link Date} object.
     * @return a {@link Date} object.
     */
    public static Date lastSecondOfTheDay(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * reset to 00h00m00s (and 0 millisecond)
     *
     * @param date a {@link Date} object.
     * @param timezone a {@link TimeZone} object.
     * @return a {@link Date} object.
     */
    public static Date resetTime(Date date, TimeZone timezone) {
        if (date == null) {
            return null;
        }
        Calendar localCalendar = Calendar.getInstance();
        localCalendar.setTime(date);

        Calendar calendar = timezone != null ? Calendar.getInstance(timezone) : Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.MONTH, localCalendar.get(Calendar.MONTH));
        calendar.set(Calendar.YEAR, localCalendar.get(Calendar.YEAR));
        resetTime(calendar);

        return calendar.getTime();
    }

    /**
     * reset to 00h00m00s (and 0 millisecond)
     *
     * @param date a {@link Date} object.
     * @return a {@link Date} object.
     */
    public static Date resetTime(Date date) {
        return resetTime(date, null);
    }

    /**
     * reset to 00h00m00s (and 0 millisecond)
     *
     * @param calendar a {@link Calendar} object.
     * @return a {@link Calendar} object.
     */
    public static Calendar resetTime(Calendar calendar) {
        if (calendar == null) return null;
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    /**
     * reset to 0 millisecond
     *
     * @param date a {@link Timestamp} object.
     * @return a {@link Timestamp} object.
     */
    public static Timestamp resetMillisecond(Timestamp date) {
        if (date == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MILLISECOND, 0);

        return new Timestamp(calendar.getTimeInMillis());
    }

    /**
     * reset to 0 millisecond
     *
     * @param date a {@link Date} object.
     * @return a {@link Timestamp} object.
     */
    public static Timestamp resetMillisecond(Date date) {
        if (date == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MILLISECOND, 0);

        return new Timestamp(calendar.getTime().getTime());
    }

    /**
     * reset to 0 millisecond
     *
     * @param calendar a {@link Calendar} object.
     * @return a {@link Calendar} object.
     */
    public static Calendar resetMillisecond(Calendar calendar) {
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    /**
     * <p>getDifferenceInDays.</p>
     *
     * @param startDate a {@link Date} object.
     * @param endDate a {@link Date} object.
     * @return a int.
     */
    public static int getDifferenceInDays(Date startDate, Date endDate) {
    	return DateUtil.getDifferenceInDays(startDate, endDate);
    }

    /**
     * <p>formatDate.</p>
     *
     * @param date a {@link Date} object.
     * @param pattern a {@link String} object.
     * @return a {@link String} object.
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null) return null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(date);
    }

    /**
     * Convert to a date, or return null if parse error
     *
     * @param date a {@link String} object.
     * @param pattern a {@link String} object.
     * @return a {@link Date} object.
     */
    public static Date safeParseDate(String date, String pattern) {
        Date result = null;
        if (StringUtils.isNotBlank(date)) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                result = simpleDateFormat.parse(date);
            } catch (ParseException ignored) {
            }
        }
        return result;
    }

    /**
     * Convert to a date, or return null if parse error
     *
     * @param date a {@link String} object.
     * @param patterns a {@link String} object.
     * @return a {@link Date} object.
     */
    public static Date safeParseDate(String date, String... patterns) {
        Date result = null;
        if (StringUtils.isNotBlank(date)) {
            for (String pattern: patterns) {
                try {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                    result = simpleDateFormat.parse(date);
                } catch (ParseException ignored) {
                    // Continue: try next pattern
                }
            }
        }
        return result;
    }

    /**
     * Adds a number of seconds to a date returning a new object.
     * The original {@code Timestamp} is unchanged.
     *
     * @param date  the Timestamp, not null
     * @param amount  the amount to add, may be negative
     * @return the new {@code Timestamp} with the amount added
     * @throws IllegalArgumentException if the date is null
     */
    public static Timestamp addSeconds(Timestamp date, int amount) {
        if(date == null) {
            throw new IllegalArgumentException("The date must not be null");
        } else {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(Calendar.SECOND, amount);
            return new Timestamp(c.getTimeInMillis());
        }
    }

    /**
     * <p>newCreateDate.</p>
     *
     * @return a {@link Date} object.
     */
    protected Date newCreateDate() {
        return dateWithNoTime(new Date());
    }

    /**
     * <p>newUpdateTimestamp.</p>
     *
     * @return a {@link Timestamp} object.
     */
    protected Timestamp newUpdateTimestamp() {
        return new Timestamp((new Date()).getTime());
    }

    /**
     * <p>dateWithNoTime.</p>
     *
     * @param date a {@link Date} object.
     * @return a {@link Date} object.
     */
    protected Date dateWithNoTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * <p>dateWithNoMillisecond.</p>
     *
     * @param date a {@link Date} object.
     * @return a {@link Date} object.
     */
    protected Date dateWithNoMillisecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * <p>dateWithNoSecondAndMillisecond.</p>
     *
     * @param date a {@link Date} object.
     * @return a {@link Date} object.
     */
    protected Date dateWithNoSecondAndMillisecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * <p>dateWithNoSecondAndOneMillisecond.</p>
     *
     * @param date a {@link Date} object.
     * @return a {@link Date} object.
     */
    protected Date dateWithNoSecondAndOneMillisecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, 0);
        calendar.add(Calendar.MILLISECOND, 1);
        return calendar.getTime();
    }

    /**
     * <p>dateWithOneMillisecond.</p>
     *
     * @param date a {@link Date} object.
     * @return a {@link Date} object.
     */
    protected Date dateWithOneMillisecond(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MILLISECOND, 1);
        return calendar.getTime();
    }

    /**
     * <p>dateOfYearWithOneMillisecond.</p>
     *
     * @param year a int.
     * @return a {@link Date} object.
     */
    protected Date dateOfYearWithOneMillisecond(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MILLISECOND, 1);
        return calendar.getTime();
    }

    /**
     * <p>dateOfYearWithOneMillisecondInMillisecond.</p>
     *
     * @param year a int.
     * @return a long.
     */
    protected long dateOfYearWithOneMillisecondInMillisecond(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MILLISECOND, 1);
        return calendar.getTimeInMillis();
    }

    /**
     * Test if the date has millisecond set. This yes, return null, then return the date itself.
     *
     * @param databaseValue the date stored in the database (could be fake date, not null only because of database constraints)
     * @return null if the date is a fake date
     */
    protected Date convertDatabase2UI(Timestamp databaseValue) {
        Date result;
        if (databaseValue == null) {
            result = null;
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(databaseValue.getTime());
            if (calendar.get(Calendar.MILLISECOND) != 0) {
                result = null;
            } else {
                result = calendar.getTime();
            }
        }
        return result;
    }

    /**
     * Convert a UI date, when the database value is mandatory.
     * If the given value is null, use the default date, then set millisecond to '1', to be able to retrieve the null value later.
     *
     * @param uiValue the date used in the UI
     * @return null if the date is a fake date
     * @param defaultNotEmptyDate a {@link Date} object.
     * @param addOneSecondToDefaultDate a boolean.
     */
    protected Date convertUI2DatabaseMandatoryDate(Date uiValue,
                                                   Date defaultNotEmptyDate,
                                                   boolean addOneSecondToDefaultDate) {
        Date result;

        // if ui date is not empty, then use it (but reset millisecond)
        if (uiValue == null) {

            Preconditions.checkState(
                    defaultNotEmptyDate != null,
                    "'defaultNotEmptyDate' could not be null.");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(defaultNotEmptyDate);
            if (addOneSecondToDefaultDate) {
                calendar.add(Calendar.SECOND, 1);
            }
            calendar.set(Calendar.MILLISECOND, 1);
            result = calendar.getTime();
        } else {

            result = dateWithNoMillisecond(uiValue);
        }

        return result;
    }

    public static Date getFirstDayOfYear(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        resetTime(calendar);
        return calendar.getTime();
    }
    public static Date getLastSecondOfYear(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year+1);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        resetTime(calendar);
        calendar.add(Calendar.SECOND, -1);
        return calendar.getTime();
    }

    public static String elapsedTime(long timeInMs) {
        long elapsedTime = System.currentTimeMillis() - timeInMs;
        double seconds = (double) elapsedTime / 1_000;
        return "in " + seconds + "s";
    }

    public static String checkISODateTimeString(String isoDate) throws SumarisTechnicalException {
        if (isoDate == null) return null;
        if (!ISO_TIMESTAMP_PATTERN.matcher(isoDate).matches()) {
            throw new SumarisTechnicalException(String.format("Invalid date time '%s'. Expected ISO format 'YYYY-MM-DDThh:mm:ss.sssZ'.", isoDate));
        }
        return isoDate;
    }

    public static String toISODateTimeString(Date date) {
        return formatDate(date, ISO_TIMESTAMP_SPEC);
    }
}
