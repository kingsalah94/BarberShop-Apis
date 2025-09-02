package com.salahtech.BarberShop_Apis.security.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Csvs {
    private Csvs() {
    }

    public static String toCsv(List<String> list) {
        if (list == null || list.isEmpty())
            return null;
        return list.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
    }

    public static List<String> toList(String csv) {
        if (csv == null || csv.isBlank())
            return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
