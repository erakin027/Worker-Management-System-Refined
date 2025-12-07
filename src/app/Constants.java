// Refactored to JSON & SOLID — do not change workflow without review
// Constants and enums for Java Admin/Worker modules
package app;

/**
 * Service type enumeration
 */
enum ServiceType {
    IMMEDIATE("Immediate"),
    SCHEDULING("Scheduling");
    
    private final String displayName;
    
    ServiceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static ServiceType fromString(String str) {
        for (ServiceType type : values()) {
            if (type.displayName.equalsIgnoreCase(str)) {
                return type;
            }
        }
        return IMMEDIATE;
    }
}

/**
 * Status enumeration for service requests
 */
enum Status {
    PENDING(0, "Pending"),
    ASSIGNED(1, "Assigned"),
    REJECTED(-1, "Rejected"),
    COMPLETED(2, "Completed");
    
    private final int value;
    private final String displayName;
    
    Status(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static Status fromInt(int value) {
        for (Status status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return PENDING;
    }
}

/**
 * Plan enumeration for customer subscription plans
 */
enum Plan {
    BASIC("Basic"),
    INTERMEDIATE("Intermediate"),
    PREMIUM("Premium");
    
    private final String displayName;
    
    Plan(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static Plan fromString(String str) {
        for (Plan plan : values()) {
            if (plan.displayName.equalsIgnoreCase(str)) {
                return plan;
            }
        }
        return BASIC;
    }
}

/**
 * Gender preference enumeration
 */
enum GenderPref {
    NP("NP"),  // No Preference
    M("M"),    // Male
    F("F");    // Female
    
    private final String value;
    
    GenderPref(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static GenderPref fromString(String str) {
        for (GenderPref pref : values()) {
            if (pref.value.equalsIgnoreCase(str)) {
                return pref;
            }
        }
        return NP;
    }
}

