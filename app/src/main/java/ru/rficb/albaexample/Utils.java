package ru.rficb.albaexample;


import java.util.Iterator;
import java.util.List;

public class Utils {


    public static int safeParseInt(String number, int defaultValue) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static <T> String implode(String glue, List<T> list) {

        if (list == null || list.isEmpty()) {
            return "";
        }

        Iterator<T> iterator = list.iterator();

        StringBuilder sb = new StringBuilder();
        sb.append(iterator.next());

        while (iterator.hasNext()) {
            sb.append(glue).append(iterator.next());
        }

        return sb.toString();
    }

}
