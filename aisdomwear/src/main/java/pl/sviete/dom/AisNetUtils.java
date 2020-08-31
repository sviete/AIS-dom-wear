package pl.sviete.dom;

public final class AisNetUtils {
    // OS version
    public static String getOsVersion(){
        return System.getProperty("os.version");
    }
    // API Level
    public static int getApiLevel(){
        return android.os.Build.VERSION.SDK_INT;
    }
    // Device
    public static String getDevice(){
        return android.os.Build.DEVICE;
    }
    // Model
    public static String getModel(){
        return android.os.Build.MODEL;
    }
    // Product
    public static String getProduct(){
        return android.os.Build.PRODUCT;
    }
    // Manufacturer
    public static String getManufacturer(){
        return android.os.Build.MANUFACTURER;
    }

}