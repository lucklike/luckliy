package com.luckyframework.common;


import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public final static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public final static String YYYY_MM_DD = "yyyy-MM-dd";


    public static String showtime() {
        String id=null;
        id="["+time()+"]  ";
        return id;
    }

    /**
     * 按照指定的格式获取当前时间的字符串
     * @param format 格式（YYYY-MM-DD HH:MM:SS）
     * @return
     */
    public static String time(String format) {
        Date date=new Date();
        SimpleDateFormat sf=
                new SimpleDateFormat(format);
        return sf.format(date);
    }

    /**
     * 获取当前时间
     * @return
     */
    public static String time() {
        Date date=new Date();
        SimpleDateFormat sf=
                new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
        return sf.format(date);
    }

    /**
     * 获取当前时间
     * @return
     */
    public static String date() {
        Date date=new Date();
        SimpleDateFormat sf=
                new SimpleDateFormat(YYYY_MM_DD);
        return sf.format(date);
    }

    /**
     * 将Date按照格式转化为String
     * @param date Data对象
     * @param format (eg:yyyy-MM-dd HH:mm:ss)
     * @return
     */
    public static String time(Date date,String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(date);
    }

    /**
     * 按照指定格式将字符串转化为Date对象
     * @param dateStr
     * @param format
     * @return
     * @throws ParseException
     */
    public static Date getDate(String dateStr,String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getDate(Date date,String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    public static String getDate(Date date) {
        return getDate(date,"yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 时间运算
     * @param dateStr
     * @param format
     * @param calendarField
     * @param amount
     * @return
     */
    public static Date addDate(String dateStr,String format,int calendarField,int amount) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(getDate(dateStr,format));
        instance.add(calendarField, amount);
        return instance.getTime();
    }

    /**
     * 时间运算
     * @param dateStr
     * @param calendarField
     * @param amount
     * @return
     */
    public static Date addDate(String dateStr,int calendarField,int amount) {
        return addDate(dateStr,"yyyy-MM-dd",calendarField,amount);
    }

    /**
     * 基于当前时间的基础上的时间运算
     * @param calendarField
     * @param amount
     * @return
     */
    public static Date currAddDate(int calendarField,int amount) {
        Calendar instance = Calendar.getInstance();
        instance.add(calendarField, amount);
        return instance.getTime();
    }


    /**
     * 年月日转Date
     * @param dateStr (eg:2020-06-31)
     * @return
     */
    public static Date getDate(String dateStr) {
        return getDate(dateStr,"yyyy-MM-dd");
    }

    /**
     * 年月日时分秒转Date
     * @param dateTimeStr (eg:2020-06-31 12:23:06)
     * @return
     */
    public static Date getDateTime(String dateTimeStr) {
        return getDate(dateTimeStr,"yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 将String转化为java.sqlActuator.Date
     * @param dateStr
     * @return
     */
    public static java.sql.Date getSqlDate(String dateStr){
        return new java.sql.Date(getDate(dateStr,"yyyy-MM-dd").getTime());
    }

    /**
     * java.sqlActuator.Date的运算
     * @param dateStr
     * @param calendarField
     * @param amount
     * @return
     */
    public static java.sql.Date addSqlDate(String dateStr,int calendarField,int amount){
        return new java.sql.Date(addDate(dateStr,calendarField,amount).getTime());
    }

    /**
     * java.sqlActuator.Date的运算
     * @param dateStr
     * @param format
     * @param calendarField
     * @param amount
     * @return
     */
    public static java.sql.Date addSqlDate(String dateStr,String format,int calendarField,int amount){
        return new java.sql.Date(addDate(dateStr,format,calendarField,amount).getTime());
    }

    /**
     * 基于当前时间java.sqlActuator.Date的运算
     * @param calendarField
     * @param amount
     * @return
     */
    public static java.sql.Date currAddSqlDate(int calendarField,int amount){
        return new java.sql.Date(currAddDate(calendarField, amount).getTime());
    }

    /**
     * 获取当前时间的java.sqlActuator.Date
     * @return
     */
    public static java.sql.Date getSqlDate(){
        return new java.sql.Date(new Date().getTime());
    }

    /**
     * 将String转化为java.sqlActuator.Time
     * @param timeStr
     * @return
     */
    public static java.sql.Time getSqlTime(String timeStr){
        return new java.sql.Time(getDate(timeStr,"HH:mm:ss").getTime());
    }

    /**
     * 获取当前时间的java.sqlActuator.Time
     * @return
     */
    public static java.sql.Time getSqlTime(){
        return new java.sql.Time(new Date().getTime());
    }

    /**
     * 将String转化为java.sqlActuator.Timestamp
     * @param timestampStr
     * @return
     */
    public static Timestamp getTimestamp(String timestampStr) {
        return new Timestamp(getDate(timestampStr,"yyyy-MM-dd HH:mm:ss").getTime());
    }

    /**
     * 获取当前时间的java.sqlActuator.Timestamp
     * @return
     */
    public static Timestamp getTimestamp() {
        return new Timestamp(new Date().getTime());
    }

}
