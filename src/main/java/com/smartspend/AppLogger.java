package com.smartspend;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppLogger {
    private static final Logger LOGGER = Logger.getLogger("com.smartspend");

    private AppLogger() {}

    public static void logDatabaseError(String operation, SQLException exception) {
        LOGGER.log(Level.SEVERE,
                "{0} failed: {1}; SQLState={2}",
                new Object[]{
                        safeOperation(operation),
                        exception.getClass().getSimpleName(),
                        exception.getSQLState() == null ? "unknown" : exception.getSQLState()
                });
    }

    public static void logUnexpectedError(String operation, Throwable exception) {
        LOGGER.log(Level.SEVERE,
                "{0} failed unexpectedly: {1}",
                new Object[]{
                        safeOperation(operation),
                        exception.getClass().getSimpleName()
                });
    }

    public static String databaseFailureMessage() {
        return "We could not complete the database operation. Please try again.";
    }

    private static String safeOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            return "Application operation";
        }
        return operation.replace('\n', ' ').replace('\r', ' ');
    }
}
