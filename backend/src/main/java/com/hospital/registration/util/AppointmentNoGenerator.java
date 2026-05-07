package com.hospital.registration.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class AppointmentNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private AppointmentNoGenerator() {
    }

    public static String next() {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "GH" + FORMATTER.format(LocalDateTime.now()) + random;
    }
}
