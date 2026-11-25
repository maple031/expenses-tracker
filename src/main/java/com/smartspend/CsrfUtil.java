package com.smartspend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfUtil {
    public static final String SESSION_ATTRIBUTE = "csrfToken";
    public static final String REQUEST_PARAMETER = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {}

    public static String getToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = (String) session.getAttribute(SESSION_ATTRIBUTE);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_ATTRIBUTE, token);
        }
        return token;
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(SESSION_ATTRIBUTE);
        String submittedToken = request.getParameter(REQUEST_PARAMETER);
        return sessionToken != null
                && submittedToken != null
                && constantTimeEquals(sessionToken, submittedToken);
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int mismatch = 0;
        for (int i = 0; i < left.length(); i++) {
            mismatch |= left.charAt(i) ^ right.charAt(i);
        }
        return mismatch == 0;
    }
}
