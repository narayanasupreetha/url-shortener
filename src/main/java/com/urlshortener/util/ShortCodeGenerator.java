package com.urlshortener.util;

import com.urlshortener.constants.ApplicationConstants;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility class for generating unique short codes.
 * <p>
 * Uses SecureRandom to generate random alphanumeric strings
 * based on configured length and alphabet.
 */
@Component
public class ShortCodeGenerator {

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a random short code.
     * <p>
     * The length and allowed characters are defined in
     * ApplicationConstants.
     *
     * @return generated short code
     */
    public String generate() {
        StringBuilder sb = new StringBuilder(ApplicationConstants.LENGTH);
        for (int i = 0; i < ApplicationConstants.LENGTH; i++) {
            sb.append(ApplicationConstants.ALPHABET.charAt(random.nextInt(ApplicationConstants.ALPHABET.length())));
        }
        return sb.toString();
    }

}
