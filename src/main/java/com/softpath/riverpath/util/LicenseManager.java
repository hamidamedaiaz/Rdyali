package com.softpath.riverpath.util;

import com.cryptlex.lexactivator.LexActivator;
import com.cryptlex.lexactivator.LexActivatorException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Cryptlex license manager for CimCFD
 * @author Max
 */
@Slf4j
public class LicenseManager {

    private static final String PRODUCT_DATA = "RTIxQzUzNzg4QTk3QkI0OTI4OEMzNzBCNjgyQjhFNzQ=.esSsCynB+bOhRp5HzmMZfkjTOkzzon2aI3sZSzrPH+kACwTUtusZUAcA7NYVtYc4VascxZwrB4gqcEyRytkVuzC6e17+3yPwr+7YV8lvQjeDDol82Q6cHKjpAf9+Y4hDSs9+MWVvvbHmRCwgZOjhW3FAYam5tgPbUKRYOrUak/BfOEkGZUYAKbDHK0Jra0+zHHZ+jX4gwiSvoxPTYJ1E/drN+5SWACrIJZHvMtQBGh9zsBrhOXX2x9/oxiHPShjjAXwvgxVLI+m8RNbeYpPxaLKR69DIux4Ppcs3N5tFN+shilg9LnKAHsWxU/28NHlKsghxZIncqnypiMWJRhoV3XGcMe/LgB6TIJ8TtWYotzjx6Uy/CqJVLlYRyKmna6N5naMTXTFjL7YA/K/3aqqtHfpW1ZVZ4cn5kKRVhiRpodE9ZxwDqfftB14fdH+COURQ4JrXtY/sJpsJ39EHO09LEeK2cqauSi7gaE55agfK0cH56G20jN37TcZRkDJHhRVzw38Gf+6ZeElH7XJ3pJR+XHEM8bkmaOVdGBjvsyq26MxKnZjgt8mbkQfQ611AaQqZVPktu5qeNENgZ4C7LVd3hVCVVRuGn/uCnb1QncdgZbOaOUHdPT2T5JtAMS542QCtWOYYJacpphCJfhZuY0fOKdhAd0Kz1N3sZKS3oAHWA9MXQIVeagLyUQXyViLrbEQpO/1wxnfyjRYq9/9vaANuBCElZRlr3E+8Y+/BBh9yOqX2/UAOJArUVT1A4c/MFDHvU+LcrRSmEKnb0tfp0A4ulCqSeKrV+Dt0tSvGIZXyGlU0a9XflQ9ey+bm9CJtuFoY";
    private static final String PRODUCT_ID = "01979ce0-47aa-7a08-82ce-f93bed398e0f";

    // Cryptlex status codes
    private static final int LA_OK = 0;
    private static final int LA_USER = 1;
    private static final int LA_E_PRODUCT_ID = 40;
    private static final int LA_E_PRODUCT_DATA = 41;
    private static final int LA_E_LICENSE_KEY = 42;
    private static final int LA_E_INET = 43;
    private static final int LA_E_SERVER = 44;
    private static final int LA_E_TIME_MODIFIED = 45;
    private static final int LA_E_TRIAL_EXPIRED = 46;
    private static final int LA_E_LICENSE_EXPIRED = 47;
    private static final int LA_E_MACHINE_FINGERPRINT = 48;
    private static final int LA_E_ACTIVATION_LIMIT = 49;
    private static final int LA_E_ACTIVATION_NOT_FOUND = 50;
    private static final int LA_E_DEACTIVATION_LIMIT = 51;
    private static final int LA_E_COUNTRY = 52;
    private static final int LA_E_IP = 53;

    private static final Preferences prefs = Preferences.userNodeForPackage(LicenseManager.class);
    private static final String PREF_LICENSE_KEY = "licenseKey";
    private static final String PREF_LAST_CHECK = "lastCheck";

    private static LicenseManager instance;
    private final ScheduledExecutorService scheduler;
    private boolean isValid = false;

    private LicenseManager() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LicenseChecker");
            t.setDaemon(true);
            return t;
        });
    }

    public static LicenseManager getInstance() {
        if (instance == null) {
            instance = new LicenseManager();
        }
        return instance;
    }

    /**
     * Initializes and verifies the existing license.
     * @return true if a valid license exists.
     */
    public boolean initialize() {
        try {
            LexActivator.SetProductData(PRODUCT_DATA);
            LexActivator.SetProductId(PRODUCT_ID, LA_USER);

            String savedKey = prefs.get(PREF_LICENSE_KEY, "");
            if (!savedKey.isEmpty()) {
                LexActivator.SetLicenseKey(savedKey);
                int result = LexActivator.IsLicenseGenuine();
                if (result == LA_OK) {
                    isValid = true;
                    startPeriodicCheck();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("License initialization failed: ", e);
            return false;
        }
    }

    /**
     * Activates a new license
     * @param licenseKey the license key
     * @return true if activation is successful
     */
    public boolean activateLicense(String licenseKey) {
        try {
            LexActivator.SetProductData(PRODUCT_DATA);
            LexActivator.SetProductId(PRODUCT_ID, LA_USER);
            LexActivator.SetLicenseKey(licenseKey);

            int result = LexActivator.ActivateLicense();
            if (result == LA_OK) {
                int genuine = LexActivator.IsLicenseGenuine();
                if (genuine == LA_OK) {
                    prefs.put(PREF_LICENSE_KEY, licenseKey);
                    prefs.putLong(PREF_LAST_CHECK, System.currentTimeMillis());
                    isValid = true;
                    startPeriodicCheck();
                    return true;
                }
            }
            return false;
        } catch (LexActivatorException e) {
            log.error("License activation failed: ", e);
            return false;
        }
    }

    /**
     * Starts periodic verification (every 15 days)
     */
    private void startPeriodicCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int result = LexActivator.IsLicenseGenuine();
                if (result == LA_OK) {
                    prefs.putLong(PREF_LAST_CHECK, System.currentTimeMillis());
                } else {
                    isValid = false;
                    javafx.application.Platform.runLater(() -> {
                        System.err.println("LICENCE EXPIRÉE - Fermeture de l'application");
                        System.exit(1);
                    });
                }
            } catch (Exception e) {
                // In case of a network error, do not close immediately.
                // We will try again in the next cycle.
                log.error("Error during license verification: ", e);
            }
        }, 15, 15, TimeUnit.DAYS);
    }

    /**
     * @return true if the license is valid
     */
    public boolean isLicenseValid() {
        return isValid;
    }

    /**
     * Closes the handler properly
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Converts an error code into a readable message.
     */
    private String getErrorMessage(int errorCode) {
        return switch (errorCode) {
            case LA_E_PRODUCT_ID -> "ID produit invalide";
            case LA_E_PRODUCT_DATA -> "Données produit invalides";
            case LA_E_LICENSE_KEY -> "Clé de licence invalide";
            case LA_E_INET -> "Erreur de connexion Internet";
            case LA_E_SERVER -> "Erreur serveur";
            case LA_E_TIME_MODIFIED -> "Horloge système modifiée";
            case LA_E_TRIAL_EXPIRED -> "Période d'essai expirée";
            case LA_E_LICENSE_EXPIRED -> "Licence expirée";
            case LA_E_MACHINE_FINGERPRINT -> "Empreinte machine invalide";
            case LA_E_ACTIVATION_LIMIT -> "Limite d'activation atteinte";
            case LA_E_ACTIVATION_NOT_FOUND -> "Activation non trouvée";
            case LA_E_DEACTIVATION_LIMIT -> "Limite de désactivation atteinte";
            case LA_E_COUNTRY -> "Pays non autorisé";
            case LA_E_IP -> "Adresse IP non autorisée";
            default -> "Erreur inconnue (code: " + errorCode + ")";
        };
    }
}