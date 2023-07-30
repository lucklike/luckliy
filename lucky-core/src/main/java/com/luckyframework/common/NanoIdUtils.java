package com.luckyframework.common;

import java.security.SecureRandom;
import java.util.Random;

/**
 * NanoId生成工具
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/6 11:48
 */
public final class NanoIdUtils {
    public static final SecureRandom DEFAULT_NUMBER_GENERATOR = new SecureRandom();
    public static final char[] DEFAULT_ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static final int DEFAULT_SIZE = 21;

    private NanoIdUtils() {
    }

    public static String randomNanoId() {
        return randomNanoId(DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, DEFAULT_SIZE);
    }

    public static String randomNanoId(int size) {
        return randomNanoId(DEFAULT_NUMBER_GENERATOR, DEFAULT_ALPHABET, size);
    }

    public static String randomNanoId(Random random, char[] alphabet, int size) {
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null.");
        } else if (alphabet == null) {
            throw new IllegalArgumentException("alphabet cannot be null.");
        } else if (alphabet.length != 0 && alphabet.length < 256) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be greater than zero.");
            } else {
                int mask = (2 << (int) Math.floor(Math.log((double) (alphabet.length - 1)) / Math.log(2.0D))) - 1;
                int step = (int) Math.ceil(1.6D * (double) mask * (double) size / (double) alphabet.length);
                StringBuilder idBuilder = new StringBuilder();

                while (true) {
                    byte[] bytes = new byte[step];
                    random.nextBytes(bytes);

                    for (int i = 0; i < step; ++i) {
                        int alphabetIndex = bytes[i] & mask;
                        if (alphabetIndex < alphabet.length) {
                            idBuilder.append(alphabet[alphabetIndex]);
                            if (idBuilder.length() == size) {
                                return idBuilder.toString();
                            }
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("alphabet must contain between 1 and 255 symbols.");
        }
    }
}