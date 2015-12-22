package jacz.peerengineclient.test_old;

import jacz.peerengineclient.file_system.DateFormatting;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Alberto
 * Date: 20/05/14
 * Time: 11:39
 * To change this template use File | Settings | File Templates.
 */
public class TestDateFormat {

    public static void main(String[] args) {

        Date date = new Date();

        System.out.println(DateFormatting.SIMPLE_DATE_FORMAT.format(date));
    }
}
