package service;

import java.util.prefs.Preferences;

/**
 * Thread-safe Singleton managing the active user session.
 *
 * Thread safety is achieved via double-checked locking (DCL) with a
 * volatile instance field, preventing race conditions during first
 * initialisation in multi-threaded JavaFX environments.
 */
public class UserSession {

    // volatile guarantees visibility across threads without full synchronisation
    private static volatile UserSession instance;

    private String userName;
    private String password;
    private String privileges;

    private UserSession(String userName, String password, String privileges) {
        this.userName   = userName;
        this.password   = password;
        this.privileges = privileges;
        persistToPreferences(userName, password, privileges);
    }

    // ── Singleton access ────────────────────────────────────────────────────

    /**
     * Returns the singleton, creating it (thread-safely) if needed.
     */
    public static UserSession getInstance(String userName, String password, String privileges) {
        if (instance == null) {                         // first check (no lock)
            synchronized (UserSession.class) {
                if (instance == null) {                 // second check (with lock)
                    instance = new UserSession(userName, password, privileges);
                }
            }
        }
        return instance;
    }

    public static UserSession getInstance(String userName, String password) {
        return getInstance(userName, password, "NONE");
    }

    /**
     * Destroys the current session (logout). Synchronised so concurrent
     * logouts are safe.
     */
    public static synchronized void cleanInstance() {
        if (instance != null) {
            instance.cleanUserSession();
            instance = null;
        }
    }

    // ── Thread-safe getters ─────────────────────────────────────────────────

    public synchronized String getUserName()   { return userName;   }
    public synchronized String getPassword()   { return password;   }
    public synchronized String getPrivileges() { return privileges; }

    // ── Internal helpers ────────────────────────────────────────────────────

    private static void persistToPreferences(String user, String pass, String priv) {
        Preferences p = Preferences.userRoot();
        p.put("USERNAME",   user);
        p.put("PASSWORD",   pass);
        p.put("PRIVILEGES", priv);
    }

    public synchronized void cleanUserSession() {
        this.userName   = "";
        this.password   = "";
        this.privileges = "";
        Preferences p = Preferences.userRoot();
        p.remove("USERNAME");
        p.remove("PASSWORD");
        p.remove("PRIVILEGES");
    }

    @Override
    public String toString() {
        return "UserSession{userName='" + userName + "', privileges='" + privileges + "'}";
    }
}