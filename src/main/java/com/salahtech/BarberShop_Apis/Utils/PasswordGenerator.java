package com.salahtech.BarberShop_Apis.Utils;

import java.security.SecureRandom;

public final class PasswordGenerator {
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String SYMBOL = "!@$%*?_-";
    private static final String ALL = UPPER + LOWER + DIGIT + SYMBOL;
    private static final SecureRandom RNG = new java.security.SecureRandom();

    public static String generate(int length) {
        if (length < 8)
            length = 8;
        StringBuilder sb = new StringBuilder(length);
        sb.append(randomChar(UPPER));
        sb.append(randomChar(LOWER));
        sb.append(randomChar(DIGIT));
        sb.append(randomChar(SYMBOL));
        for (int i = sb.length(); i < length; i++) {
            sb.append(randomChar(ALL));
        }
        return shuffle(sb.toString());
    }

    private static char randomChar(String src) {
        return src.charAt(RNG.nextInt(src.length()));
    }

    private static String shuffle(String s) {
        var arr = s.toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            char t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
        return new String(arr);
    }

    private PasswordGenerator() {
    }
}
