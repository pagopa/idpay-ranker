package it.gov.pagopa.utils;


public class CommonUtils {
    private CommonUtils() {}

    public static String sanitizeString(String str){
        return str == null? null: str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }
}
