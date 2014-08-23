/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.timer.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.junit.Assert;
import org.junit.Test;

/**
 * CalendarBasedTimeoutTestCase
 *
 * @author Jaikiran Pai
 * @author Eduardo Martins
 * @author "<a href=\"mailto:wfink@redhat.com\">Wolf-Dieter Fink</a>"
 */
public class CalendarBasedTimeoutTestCase {

    /**
     * Logger
     */
//    private static Logger logger = Logger.getLogger(CalendarBasedTimeoutTestCase.class);

    /**
     * The timezone which is in use
     */
    private TimeZone timezone;
    private String timeZoneDisplayName;

    /**
     * This method returns a collection of all available timezones in the system.
     * The tests in this {@link CalendarBasedTimeoutTestCase} will then be run
     * against each of these timezones
     */
    private static List<TimeZone> getTimezones() {
        String[] candidates = TimeZone.getAvailableIDs();
        List<TimeZone> timeZones = new ArrayList<TimeZone>(candidates.length);
        for (String timezoneID : candidates) {
            TimeZone timeZone = TimeZone.getTimeZone(timezoneID);
            boolean different = true;
            for (int i = timeZones.size() - 1; i >= 0; i--)  {
                TimeZone testee = timeZones.get(i);
                if (testee.hasSameRules(timeZone)) {
                    different = false;
                    break;
                }
            }
            if (different) {
                timeZones.add(timeZone);
            }
        }
        return timeZones;
    }

    /**
     * Asserts timeouts based on next day of week.
     * Uses expression dayOfWeek=saturday hour=3 minute=21 second=50.
     * Expected next timeout is SAT 2014-03-29 3:21:50
     */
    @Test
    public void testNextDayOfWeek() {
        // start date is SAT 2014-03-22 4:00:00, has to advance to SAT of next week
        testNextDayOfWeek(new GregorianCalendar(2014,2,22,4,0,0).getTime());
        // start date is TUE 2014-03-25 2:00:00, has to advance to SAT of same week
        testNextDayOfWeek(new GregorianCalendar(2014,2,25,2,0,0).getTime());
    }

    private void testNextDayOfWeek(Date start) {
        ScheduleExpression expression = new ScheduleExpression();
        expression.dayOfWeek("6");
        expression.hour("3");
        expression.minute("21");
        expression.second("50");
        expression.start(start);
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(firstTimeout);
        Assert.assertEquals(50, firstTimeout.get(Calendar.SECOND));
        Assert.assertEquals(21, firstTimeout.get(Calendar.MINUTE));
        Assert.assertEquals(3, firstTimeout.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(7, firstTimeout.get(Calendar.DAY_OF_WEEK));
        Assert.assertEquals(29, firstTimeout.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testCalendarBasedTimeout() {
        for (TimeZone tz : getTimezones()) {
            this.timezone = tz;
            this.timeZoneDisplayName = this.timezone.getDisplayName();

            testEverySecondTimeout();
            testEveryMinuteEveryHourEveryDay();
            testEveryMorningFiveFifteen();
            testEveryWeekdayEightFifteen();
            testEveryMonWedFriTwelveThirtyNoon();
            testEvery31stOfTheMonth();
            testRun29thOfFeb();
            testSomeSpecificTime();
            testEvery10Seconds();
        }
    }

    //@Test
    public void testEverySecondTimeout() {
        ScheduleExpression everySecondExpression = this.getTimezoneSpecificScheduleExpression();
        everySecondExpression.second("*");
        everySecondExpression.minute("*");
        everySecondExpression.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everySecondExpression);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Calendar nextTimeout = calendarTimeout.getNextTimeout(firstTimeout);

        Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
        Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(firstTimeout));
//        logger.debug("Previous timeout was: " + firstTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());
        long diff = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
        Assert.assertEquals(timeZoneDisplayName, 1000, diff);
    }

    //@Test
    public void testEveryMinuteEveryHourEveryDay() {
        ScheduleExpression everyMinEveryHourEveryDay = this.getTimezoneSpecificScheduleExpression();
        everyMinEveryHourEveryDay.minute("*");
        everyMinEveryHourEveryDay.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMinEveryHourEveryDay);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 65; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));
//            logger.debug("First timeout was: " + firstTimeout.getTime() + " Previous timeout was: "
//                    + previousTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());
            long diff = nextTimeout.getTimeInMillis() - previousTimeout.getTimeInMillis();
            long diffWithFirstTimeout = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
            Assert.assertEquals(timeZoneDisplayName, 60 * 1000, diff);
            Assert.assertEquals(timeZoneDisplayName, 60 * 1000 * i, diffWithFirstTimeout);

            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testEveryMorningFiveFifteen() {
        ScheduleExpression everyMorningFiveFifteen = this.getTimezoneSpecificScheduleExpression();
        everyMorningFiveFifteen.minute(15);
        everyMorningFiveFifteen.hour(5);

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMorningFiveFifteen);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        Assert.assertEquals(timeZoneDisplayName, 0, second);
        Assert.assertEquals(timeZoneDisplayName, 15, minute);
        Assert.assertEquals(timeZoneDisplayName, 5, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.AM, amOrPm);

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 370; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));
//            logger.debug("First timeout was: " + firstTimeout.getTime() + " Previous timeout was: "
//                    + previousTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());

            Assert.assertEquals(timeZoneDisplayName, 0, nextTimeout.get(Calendar.SECOND));
            Assert.assertEquals(timeZoneDisplayName, 15, nextTimeout.get(Calendar.MINUTE));

            Assert.assertEquals(timeZoneDisplayName, 5, nextTimeout.get(Calendar.HOUR_OF_DAY));
            Assert.assertEquals(timeZoneDisplayName, Calendar.AM, nextTimeout.get(Calendar.AM_PM));

            previousTimeout = nextTimeout;

        }
    }

    //@Test
    public void testEveryWeekdayEightFifteen() {
        ScheduleExpression everyWeekDayThreeFifteen = this.getTimezoneSpecificScheduleExpression();
        everyWeekDayThreeFifteen.minute(15);
        everyWeekDayThreeFifteen.hour(8);
        everyWeekDayThreeFifteen.dayOfWeek("Mon-Fri");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyWeekDayThreeFifteen);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        Assert.assertEquals(timeZoneDisplayName, 0, second);
        Assert.assertEquals(timeZoneDisplayName, 15, minute);
        Assert.assertEquals(timeZoneDisplayName, 8, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.AM, amOrPm);
        Assert.assertTrue(timeZoneDisplayName, this.isWeekDay(firstTimeout));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 180; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));

//            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
//                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            Assert.assertEquals(timeZoneDisplayName, 0, nextSecond);
            Assert.assertEquals(timeZoneDisplayName, 15, nextMinute);
            Assert.assertEquals(timeZoneDisplayName, 8, nextHour);
            Assert.assertEquals(timeZoneDisplayName, Calendar.AM, nextAmOrPm);
            Assert.assertTrue(timeZoneDisplayName, this.isWeekDay(nextTimeout));

            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testEveryMonWedFriTwelveThirtyNoon() {
        ScheduleExpression everyMonWedFriTwelveThirtyNoon = this.getTimezoneSpecificScheduleExpression();
        everyMonWedFriTwelveThirtyNoon.hour(12);
        everyMonWedFriTwelveThirtyNoon.second("30");
        everyMonWedFriTwelveThirtyNoon.dayOfWeek("Mon,Wed,Fri");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMonWedFriTwelveThirtyNoon);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfWeek = firstTimeout.get(Calendar.DAY_OF_WEEK);
        Assert.assertEquals(timeZoneDisplayName, 30, second);
        Assert.assertEquals(timeZoneDisplayName, 0, minute);
        Assert.assertEquals(timeZoneDisplayName, 12, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.PM, amOrPm);
        List<Integer> validDays = new ArrayList<Integer>();
        validDays.add(Calendar.MONDAY);
        validDays.add(Calendar.WEDNESDAY);
        validDays.add(Calendar.FRIDAY);
        Assert.assertTrue(timeZoneDisplayName, validDays.contains(dayOfWeek));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 180; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));

//            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
//                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfWeek = nextTimeout.get(Calendar.DAY_OF_WEEK);
            Assert.assertEquals(timeZoneDisplayName, 30, nextSecond);
            Assert.assertEquals(timeZoneDisplayName, 0, nextMinute);
            Assert.assertEquals(timeZoneDisplayName, 12, nextHour);
            Assert.assertEquals(timeZoneDisplayName, Calendar.PM, nextAmOrPm);
            Assert.assertTrue(timeZoneDisplayName, validDays.contains(nextDayOfWeek));
            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testEvery31stOfTheMonth() {
        ScheduleExpression every31st9_30_15_AM = this.getTimezoneSpecificScheduleExpression();
        every31st9_30_15_AM.dayOfMonth(31);
        every31st9_30_15_AM.hour(9);
        every31st9_30_15_AM.minute("30");
        every31st9_30_15_AM.second(15);

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every31st9_30_15_AM);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        Assert.assertEquals(timeZoneDisplayName, 15, second);
        Assert.assertEquals(timeZoneDisplayName, 30, minute);
        Assert.assertEquals(timeZoneDisplayName, 9, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.AM, amOrPm);
        Assert.assertEquals(timeZoneDisplayName, 31, dayOfMonth);

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 18; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));

//            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
//                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfMonth = nextTimeout.get(Calendar.DAY_OF_MONTH);
            Assert.assertEquals(timeZoneDisplayName, 15, nextSecond);
            Assert.assertEquals(timeZoneDisplayName, 30, nextMinute);
            Assert.assertEquals(timeZoneDisplayName, 9, nextHour);
            Assert.assertEquals(timeZoneDisplayName, Calendar.AM, nextAmOrPm);
            Assert.assertEquals(timeZoneDisplayName, 31, nextDayOfMonth);

            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testRun29thOfFeb() {
        ScheduleExpression everyLeapYearOn29thFeb = this.getTimezoneSpecificScheduleExpression();
        everyLeapYearOn29thFeb.dayOfMonth(29);
        everyLeapYearOn29thFeb.month("fEb");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyLeapYearOn29thFeb);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        int month = firstTimeout.get(Calendar.MONTH);

        Assert.assertEquals(timeZoneDisplayName, 0, second);
        Assert.assertEquals(timeZoneDisplayName, 0, minute);
        Assert.assertEquals(timeZoneDisplayName, 0, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.AM, amOrPm);
        Assert.assertEquals(timeZoneDisplayName, 29, dayOfMonth);
        Assert.assertEquals(timeZoneDisplayName, Calendar.FEBRUARY, month);
        Assert.assertTrue(timeZoneDisplayName, this.isLeapYear(firstTimeout));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 2; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));

//            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
//                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfMonth = nextTimeout.get(Calendar.DAY_OF_MONTH);
            int nextMonth = nextTimeout.get(Calendar.MONTH);

            Assert.assertEquals(timeZoneDisplayName, 0, nextSecond);
            Assert.assertEquals(timeZoneDisplayName, 0, nextMinute);
            Assert.assertEquals(timeZoneDisplayName, 0, nextHour);
            Assert.assertEquals(timeZoneDisplayName, Calendar.AM, nextAmOrPm);
            Assert.assertEquals(timeZoneDisplayName, 29, nextDayOfMonth);
            Assert.assertEquals(timeZoneDisplayName, Calendar.FEBRUARY, nextMonth);
            Assert.assertTrue(timeZoneDisplayName, this.isLeapYear(nextTimeout));

            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testSomeSpecificTime() {
        ScheduleExpression every0_15_30_Sec_At_9_30_PM = this.getTimezoneSpecificScheduleExpression();
        every0_15_30_Sec_At_9_30_PM.dayOfMonth(31);
        every0_15_30_Sec_At_9_30_PM.month("Nov-Feb");
        every0_15_30_Sec_At_9_30_PM.second("0,15,30");
        every0_15_30_Sec_At_9_30_PM.minute(30);
        every0_15_30_Sec_At_9_30_PM.hour("21");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every0_15_30_Sec_At_9_30_PM);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
//        logger.debug("First timeout is " + firstTimeoutDate);

        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        int month = firstTimeout.get(Calendar.MONTH);

        Assert.assertEquals(timeZoneDisplayName, 0, second);
        Assert.assertEquals(timeZoneDisplayName, 30, minute);
        Assert.assertEquals(timeZoneDisplayName, 21, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.PM, amOrPm);
        Assert.assertEquals(timeZoneDisplayName, 31, dayOfMonth);
        List<Integer> validMonths = new ArrayList<Integer>();
        validMonths.add(Calendar.NOVEMBER);
        validMonths.add(Calendar.DECEMBER);
        validMonths.add(Calendar.JANUARY);
        validMonths.add(Calendar.FEBRUARY);
        Assert.assertTrue(timeZoneDisplayName, validMonths.contains(month));

        Calendar nextTimeout = calendarTimeout.getNextTimeout(firstTimeout);
        long diff = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
        Assert.assertEquals(timeZoneDisplayName, 15 * 1000, diff);

        Calendar date = new GregorianCalendar(2014,3,18);
        Calendar nextTimeoutFromNow = calendarTimeout.getNextTimeout(date);
//        logger.debug("Next timeout from now is " + nextTimeoutFromNow.getTime());
        int nextMinute = nextTimeoutFromNow.get(Calendar.MINUTE);
        int nextSecond = nextTimeoutFromNow.get(Calendar.SECOND);
        int nextHour = nextTimeoutFromNow.get(Calendar.HOUR_OF_DAY);
        int nextAmOrPM = nextTimeoutFromNow.get(Calendar.AM_PM);
        int nextDayOfMonth = nextTimeoutFromNow.get(Calendar.DAY_OF_MONTH);
        int nextMonth = nextTimeoutFromNow.get(Calendar.MONTH);

        List<Integer> validSeconds = new ArrayList<Integer>();
        validSeconds.add(0);
        validSeconds.add(15);
        validSeconds.add(30);

        Assert.assertTrue(timeZoneDisplayName, validSeconds.contains(nextSecond));
        Assert.assertEquals(timeZoneDisplayName, 30, nextMinute);
        Assert.assertEquals(timeZoneDisplayName, 21, nextHour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.PM, nextAmOrPM);
        Assert.assertEquals(timeZoneDisplayName, 31, nextDayOfMonth);
        Assert.assertTrue(timeZoneDisplayName, validMonths.contains(nextMonth));

    }

    //@Test
    public void testEvery10Seconds() {
        ScheduleExpression every10Secs = this.getTimezoneSpecificScheduleExpression();
        every10Secs.second("*/10");
        every10Secs.minute("*");
        every10Secs.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every10Secs);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        int firstTimeoutSecond = firstTimeout.get(Calendar.SECOND);
        Assert.assertTrue(timeZoneDisplayName, firstTimeoutSecond % 10 == 0);

        Calendar previousTimeout = firstTimeout;
        for (int i = 0; i < 5; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);
            int nextTimeoutSecond = nextTimeout.get(Calendar.SECOND);
            Assert.assertTrue(timeZoneDisplayName, nextTimeoutSecond % 10 == 0);

            previousTimeout = nextTimeout;

        }

    }


    /**
     * WFLY-1468
     * Create a Timeout with a schedule start date in the past (day before) to ensure the time is set correctly.
     * The schedule is on the first day of month to ensure that the calculated time must be moved to the next month.
     *
     * The test is run for each day of a whole year.
     */
    @Test
    public void testWFLY1468() {
        ScheduleExpression schedule = new ScheduleExpression();
        int year = 2013;
        int month = Calendar.JUNE;
        int dayOfMonth = 3;
        int hourOfDay = 2;
        int minutes = 0;
        Calendar start = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minutes);
        schedule.hour("0-12")
                .month("*")
                .dayOfMonth("3")
                .minute("0/5")
                .second("0")
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        // assert first timeout result
        if(firstTimeout.get(Calendar.DAY_OF_MONTH) != 3 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 2 ||
                firstTimeout.get(Calendar.MINUTE) != 0 ||
                firstTimeout.get(Calendar.SECOND) != 0) {
            Assert.fail(firstTimeout.toString());
        }
    }

    /**
     * Tests correct timeouts going through a DST hour rollback (Europe/Lisbon, 27 Oct 2013, 02:00)
     */
    @Test
    public void testDSTRollback() {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Lisbon");
        int year = 2013;
        int month = Calendar.OCTOBER;
        int dayOfMonth = 27;
        int hourOfDay = 0;
        int minute = 0;
        int second = 0;

        Calendar start = new GregorianCalendar(timeZone);
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);
        Assert.assertEquals(year, start.get(Calendar.YEAR));
        Assert.assertEquals(month, start.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, start.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, start.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, start.get(Calendar.MINUTE));
        Assert.assertEquals(second, start.get(Calendar.SECOND));

        ScheduleExpression expression = new ScheduleExpression();
        expression.timezone(timeZone.getID());
        expression.dayOfMonth("*");
        expression.hour("*");
        expression.minute("0, 30");
        expression.second("0");
        expression.start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);

        // 1st timeout should equal to start i.e. 00:00
        Calendar timeout1 = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeout1);
        Assert.assertEquals(year, timeout1.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout1.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout1.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, timeout1.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, timeout1.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout1.get(Calendar.SECOND));

        // 2nd timeout should advance to 00:30
        Calendar timeout2 = calendarTimeout.getNextTimeout(timeout1);
        Assert.assertNotNull(timeout2);
        Assert.assertEquals(year, timeout2.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout2.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout2.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, timeout2.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute + 30, timeout2.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout2.get(Calendar.SECOND));
        Assert.assertEquals(timeout1.getTimeInMillis() + (30 * 60 * 1000), timeout2.getTimeInMillis());

        // 3rd timeout should advance to 01:00
        Calendar timeout3 = calendarTimeout.getNextTimeout(timeout2);
        Assert.assertNotNull(timeout3);
        Assert.assertEquals(year, timeout3.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout3.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout3.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay + 1, timeout3.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, timeout3.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout3.get(Calendar.SECOND));
        Assert.assertEquals(timeout2.getTimeInMillis() + (30 * 60 * 1000), timeout3.getTimeInMillis());

        // 4th timeout should advance to 01:30
        Calendar timeout4 = calendarTimeout.getNextTimeout(timeout3);
        Assert.assertNotNull(timeout4);
        Assert.assertEquals(year, timeout4.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout4.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout4.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay + 1, timeout4.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute + 30, timeout4.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout4.get(Calendar.SECOND));
        Assert.assertEquals(timeout3.getTimeInMillis() + (30 * 60 * 1000), timeout4.getTimeInMillis());

        // 5th timeout should go back to 01:00 (dst rollback) but time in ms should have advanced)
        Calendar timeout5 = calendarTimeout.getNextTimeout(timeout4);
        Assert.assertNotNull(timeout5);
        Assert.assertEquals(year, timeout5.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout5.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout5.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay + 1, timeout5.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, timeout5.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout5.get(Calendar.SECOND));
        Assert.assertEquals(timeout4.getTimeInMillis() + (30 * 60 * 1000), timeout5.getTimeInMillis());

        // 6th timeout should move to 01:30
        Calendar timeout6 = calendarTimeout.getNextTimeout(timeout5);
        Assert.assertNotNull(timeout6);
        Assert.assertEquals(year, timeout6.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout6.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout6.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay + 1, timeout6.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute + 30, timeout6.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout6.get(Calendar.SECOND));
        Assert.assertEquals(timeout5.getTimeInMillis() + (30 * 60 * 1000), timeout6.getTimeInMillis());

        // 7th timeout should move to 02:00
        Calendar timeout7 = calendarTimeout.getNextTimeout(timeout6);
        Assert.assertNotNull(timeout7);
        Assert.assertEquals(year, timeout7.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout7.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout7.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay + 2, timeout7.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, timeout7.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout7.get(Calendar.SECOND));
        Assert.assertEquals(timeout6.getTimeInMillis() + (30 * 60 * 1000), timeout7.getTimeInMillis());
    }

    /**
     * Tests correct timeouts going through a DST hour rollback (Europe/Lisbon, 27 Oct 2013, 02:00)
     */
    @Test
    public void testDSTRollbackWithSpecificHour() {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Lisbon");
        int year = 2013;
        int month = Calendar.OCTOBER;
        int dayOfMonth = 27;
        int hourOfDay = 1;
        int minute = 10;
        int second = 0;

        Calendar start = new GregorianCalendar(timeZone);
        start.clear();
        // set DST offset to ensure start is the 1:10 DST, not STD
        start.set(Calendar.DST_OFFSET, 60*60*1000);
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);
        Assert.assertEquals(year, start.get(Calendar.YEAR));
        Assert.assertEquals(month, start.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, start.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, start.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, start.get(Calendar.MINUTE));
        Assert.assertEquals(second, start.get(Calendar.SECOND));

        // any day of year, time allowed is 01:00, 01:30, 02:00 or 02:30
        ScheduleExpression expression = new ScheduleExpression();
        expression.timezone(timeZone.getID());
        expression.dayOfMonth("*");
        expression.hour("1, 2");
        expression.minute("0, 30");
        expression.second("0");
        expression.start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);

        // 1st timeout should be 01:30 DST
        Calendar timeout1 = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeout1);
        Assert.assertEquals(year, timeout1.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout1.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout1.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(1, timeout1.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(30, timeout1.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout1.get(Calendar.SECOND));

        // 2nd timeout should be 01:00 STD, 30 minutes after 1st timeout
        Calendar timeout2 = calendarTimeout.getNextTimeout(timeout1);
        Assert.assertNotNull(timeout2);
        Assert.assertEquals(year, timeout2.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout2.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout2.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(1, timeout2.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, timeout2.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout2.get(Calendar.SECOND));
        Assert.assertEquals(timeout1.getTimeInMillis() + (30 * 60 * 1000), timeout2.getTimeInMillis());

        // 3nd timeout should advance to 02:00 STD, 1 hour after 2nd timeout, skipping 01:30 STD
        Calendar timeout3 = calendarTimeout.getNextTimeout(timeout2);
        Assert.assertNotNull(timeout3);
        Assert.assertEquals(year, timeout3.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout3.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout3.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(2, timeout3.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, timeout3.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout3.get(Calendar.SECOND));
        Assert.assertEquals(timeout2.getTimeInMillis() + (60 * 60 * 1000), timeout3.getTimeInMillis());
    }

    /**
     * Tests correct timeouts going through a DST rollback different than 1 hour (Australia/Lord_Howe, 7 Apr 2013, 02:00)
     */
    @Test
    public void testDSTRollbackWithSpecificMinute() {
        TimeZone timeZone = TimeZone.getTimeZone("Australia/Lord_Howe");
        int year = 2013;
        int month = Calendar.APRIL;
        int dayOfMonth = 7;
        int hourOfDay = 1;
        int minute = 50;
        int second = 0;

        Calendar start = new GregorianCalendar(timeZone);
        start.clear();
        // set DST offset to ensure start is the 1:50 DST, not STD
        start.set(Calendar.DST_OFFSET, 30*60*1000);
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);
        Assert.assertEquals(year, start.get(Calendar.YEAR));
        Assert.assertEquals(month, start.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, start.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, start.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, start.get(Calendar.MINUTE));
        Assert.assertEquals(second, start.get(Calendar.SECOND));

        // any day of year, any hour, minutes allowed are 0 and 30
        ScheduleExpression expression = new ScheduleExpression();
        expression.timezone(timeZone.getID());
        expression.dayOfMonth("*");
        expression.hour("*");
        expression.minute("0, 10, 20, 30, 40, 50");
        expression.second("0");
        expression.start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);

        // 1st timeout should be 01:50 DST
        Calendar timeout1 = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeout1);
        Assert.assertEquals(year, timeout1.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout1.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout1.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(1, timeout1.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(50, timeout1.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout1.get(Calendar.SECOND));

        // 2nd timeout should be 01:30 STD after 30 min DST rollback, yet 10 minutes after 1st timeout
        Calendar timeout2 = calendarTimeout.getNextTimeout(timeout1);
        Assert.assertNotNull(timeout2);
        Assert.assertEquals(year, timeout2.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout2.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout2.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(1, timeout2.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(30, timeout2.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout2.get(Calendar.SECOND));
        Assert.assertEquals(timeout1.getTimeInMillis() + (10 * 60 * 1000), timeout2.getTimeInMillis());

        // 3rd timeout should be 01:40 STD, 10 minutes after 2nd timeout
        Calendar timeout3 = calendarTimeout.getNextTimeout(timeout2);
        Assert.assertNotNull(timeout3);
        Assert.assertEquals(year, timeout3.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout3.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout3.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(1, timeout3.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(40, timeout3.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout3.get(Calendar.SECOND));
        Assert.assertEquals(timeout2.getTimeInMillis() + (10 * 60 * 1000), timeout3.getTimeInMillis());

        // 4th timeout should advance to 02:00 STD, 20 min after 3rd timeout, skipping 01:50 STD
        Calendar timeout4 = calendarTimeout.getNextTimeout(timeout3);
        Assert.assertNotNull(timeout4);
        Assert.assertEquals(year, timeout4.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout4.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout4.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(2, timeout4.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, timeout4.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout4.get(Calendar.SECOND));
        Assert.assertEquals(timeout3.getTimeInMillis() + (20 * 60 * 1000), timeout4.getTimeInMillis());
    }

    /**
     * Tests correct timeouts going through a DST hour forward (Europe/Lisbon, 31 Mar 2013, 01:00)
     */
    @Test
    public void testDSTForward() {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Lisbon");
        int year = 2013;
        int month = Calendar.MARCH;
        int dayOfMonth = 31;
        int hourOfDay = 0;
        int minute = 0;
        int second = 0;

        Calendar start = new GregorianCalendar(timeZone);
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);
        Assert.assertEquals(year, start.get(Calendar.YEAR));
        Assert.assertEquals(month, start.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, start.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, start.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, start.get(Calendar.MINUTE));
        Assert.assertEquals(second, start.get(Calendar.SECOND));

        ScheduleExpression expression = new ScheduleExpression();
        expression.timezone(timeZone.getID());
        expression.dayOfMonth("*");
        expression.hour("*");
        expression.minute("0, 30");
        expression.second("0");
        expression.start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);

        // 1st timeout should be 00:00
        Calendar timeout1 = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeout1);
        Assert.assertEquals(year, timeout1.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout1.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout1.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, timeout1.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, timeout1.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout1.get(Calendar.SECOND));

        // 2nd timeout should advance to 00:30
        Calendar timeout2 = calendarTimeout.getNextTimeout(timeout1);
        Assert.assertNotNull(timeout2);
        Assert.assertEquals(year, timeout2.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout2.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout2.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, timeout2.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute + 30, timeout2.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout2.get(Calendar.SECOND));
        Assert.assertEquals(timeout1.getTimeInMillis() + (30 * 60 * 1000), timeout2.getTimeInMillis());

        // 3rd timeout should advance to 02:00 but time in ms should advance only 30min
        Calendar timeout3 = calendarTimeout.getNextTimeout(timeout2);
        Assert.assertNotNull(timeout3);
        Assert.assertEquals(year, timeout3.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout3.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout3.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay + 2, timeout3.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, timeout3.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout3.get(Calendar.SECOND));
        Assert.assertEquals(timeout2.getTimeInMillis() + (30 * 60 * 1000), timeout3.getTimeInMillis());

        // 4th timeout should advance to 02:30
        Calendar timeout4 = calendarTimeout.getNextTimeout(timeout3);
        Assert.assertNotNull(timeout4);
        Assert.assertEquals(year, timeout4.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout4.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout4.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay + 2, timeout4.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute + 30, timeout4.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout4.get(Calendar.SECOND));
        Assert.assertEquals(timeout3.getTimeInMillis() + (30 * 60 * 1000), timeout4.getTimeInMillis());

        // 5th timeout should advance to 03:00
        Calendar timeout5 = calendarTimeout.getNextTimeout(timeout4);
        Assert.assertNotNull(timeout5);
        Assert.assertEquals(year, timeout5.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout5.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout5.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay + 3, timeout5.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, timeout5.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout5.get(Calendar.SECOND));
        Assert.assertEquals(timeout4.getTimeInMillis() + (30 * 60 * 1000), timeout5.getTimeInMillis());
    }

    /**
     * Tests correct timeouts going through a DST hour forward using specific hour (Europe/Lisbon, 31 Mar 2013, 01:00)
     */
    @Test
    public void testDSTForwardWithSpecificHour() {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Lisbon");
        int year = 2013;
        int month = Calendar.MARCH;
        int dayOfMonth = 31;
        int hourOfDay = 0;
        int minute = 30;
        int second = 0;

        Calendar start = new GregorianCalendar(timeZone);
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);
        Assert.assertEquals(year, start.get(Calendar.YEAR));
        Assert.assertEquals(month, start.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, start.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, start.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, start.get(Calendar.MINUTE));
        Assert.assertEquals(second, start.get(Calendar.SECOND));

        ScheduleExpression expression = new ScheduleExpression();
        expression.timezone(timeZone.getID());
        expression.dayOfMonth("*");
        expression.hour("0, 1, 3");
        expression.minute("0, 30");
        expression.second("0");
        expression.start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);

        // 1st timeout should be 00:30
        Calendar timeout1 = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeout1);
        Assert.assertEquals(year, timeout1.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout1.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout1.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(0, timeout1.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(30, timeout1.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout1.get(Calendar.SECOND));

        // 2nd timeout should advance to 02:00, due to DST forward
        Calendar timeout2 = calendarTimeout.getNextTimeout(timeout1);
        Assert.assertNotNull(timeout2);
        Assert.assertEquals(year, timeout2.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout2.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout2.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(2, timeout2.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, timeout2.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout2.get(Calendar.SECOND));
        Assert.assertEquals(timeout1.getTimeInMillis() + (30 * 60 * 1000), timeout2.getTimeInMillis());

        // 3rd timeout should advance to 02:30, due to DST forward
        Calendar timeout3 = calendarTimeout.getNextTimeout(timeout2);
        Assert.assertNotNull(timeout3);
        Assert.assertEquals(year, timeout3.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout3.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout3.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(2, timeout3.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(30, timeout3.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout3.get(Calendar.SECOND));
        Assert.assertEquals(timeout2.getTimeInMillis() + (30 * 60 * 1000), timeout3.getTimeInMillis());

        // 4th timeout should advance to 03:00
        Calendar timeout4 = calendarTimeout.getNextTimeout(timeout3);
        Assert.assertNotNull(timeout4);
        Assert.assertEquals(year, timeout4.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout4.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout4.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(3, timeout4.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, timeout4.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout4.get(Calendar.SECOND));
        Assert.assertEquals(timeout3.getTimeInMillis() + (30 * 60 * 1000), timeout4.getTimeInMillis());
    }

    /**
     * Tests correct timeouts going through a DST hour forward using specific minute (Australia/Lord_Howe, 6 Oct 2013, 02:00)
     */
    @Test
    public void testDSTForwardWithSpecificMinute() {
        TimeZone timeZone = TimeZone.getTimeZone("Australia/Lord_Howe");
        int year = 2013;
        int month = Calendar.OCTOBER;
        int dayOfMonth = 6;
        int hourOfDay = 1;
        int minute = 30;
        int second = 0;

        Calendar start = new GregorianCalendar(timeZone);
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);
        Assert.assertEquals(year, start.get(Calendar.YEAR));
        Assert.assertEquals(month, start.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, start.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(hourOfDay, start.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(minute, start.get(Calendar.MINUTE));
        Assert.assertEquals(second, start.get(Calendar.SECOND));

        ScheduleExpression expression = new ScheduleExpression();
        expression.timezone(timeZone.getID());
        expression.dayOfMonth("*");
        expression.hour("*");
        expression.minute("0, 15, 30");
        expression.second("0");
        expression.start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);

        // 1st timeout should be 01:30
        Calendar timeout1 = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeout1);
        Assert.assertEquals(year, timeout1.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout1.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout1.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(1, timeout1.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(30, timeout1.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout1.get(Calendar.SECOND));

        // 2nd timeout should advance to 02:30, due to DST forward
        Calendar timeout2 = calendarTimeout.getNextTimeout(timeout1);
        Assert.assertNotNull(timeout2);
        Assert.assertEquals(year, timeout2.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout2.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout2.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(2, timeout2.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(30, timeout2.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout2.get(Calendar.SECOND));
        Assert.assertEquals(timeout1.getTimeInMillis() + (30 * 60 * 1000), timeout2.getTimeInMillis());

        // 3rd timeout should advance to 02:45, due to DST forward
        Calendar timeout3 = calendarTimeout.getNextTimeout(timeout2);
        Assert.assertNotNull(timeout3);
        Assert.assertEquals(year, timeout3.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout3.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout3.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(2, timeout3.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(45, timeout3.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout3.get(Calendar.SECOND));
        Assert.assertEquals(timeout2.getTimeInMillis() + (15 * 60 * 1000), timeout3.getTimeInMillis());

        // 4th timeout should advance to 03:00
        Calendar timeout4 = calendarTimeout.getNextTimeout(timeout3);
        Assert.assertNotNull(timeout4);
        Assert.assertEquals(year, timeout4.get(Calendar.YEAR));
        Assert.assertEquals(month, timeout4.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, timeout4.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(3, timeout4.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, timeout4.get(Calendar.MINUTE));
        Assert.assertEquals(second, timeout4.get(Calendar.SECOND));
        Assert.assertEquals(timeout3.getTimeInMillis() + (15 * 60 * 1000), timeout4.getTimeInMillis());
    }

    private ScheduleExpression getTimezoneSpecificScheduleExpression() {
        ScheduleExpression scheduleExpression = new ScheduleExpression().timezone(this.timezone.getID());
        GregorianCalendar start = new GregorianCalendar(this.timezone);
        start.clear();
        start.set(2014,0,1,1,0,0);
        return scheduleExpression.start(start.getTime());
    }

    private boolean isLeapYear(Calendar cal) {
        int year = cal.get(Calendar.YEAR);
        return year % 4 == 0;
    }

    private boolean isWeekDay(Calendar cal) {
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        switch (dayOfWeek) {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
                return false;
            default:
                return true;
        }
    }

}
