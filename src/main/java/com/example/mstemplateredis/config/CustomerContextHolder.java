package com.example.mstemplateredis.config;

public class CustomerContextHolder {
    private static final ThreadLocal<String> customerIdHolder = new ThreadLocal<>();

    public static void setCustomerId(String customerId) {
        customerIdHolder.set(customerId);
    }

    public static String getCustomerId() {
        return customerIdHolder.get();
    }

    public static void clear() {
        customerIdHolder.remove();
    }
}
