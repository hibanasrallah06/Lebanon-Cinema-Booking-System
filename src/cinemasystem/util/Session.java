package cinemasystem.util;

public class Session {

    private static int employeeId;
    private static String fullName;
    private static String role;
    private static int cinemaId;

    private Session() {
        // Prevent creating objects
    }

    public static void setEmployee(
            int id,
            String name,
            String employeeRole,
            int employeeCinemaId) {

        employeeId = id;
        fullName = name;
        role = employeeRole;
        cinemaId = employeeCinemaId;
    }

    public static int getEmployeeId() {
        return employeeId;
    }

    public static String getFullName() {
        return fullName;
    }

    public static String getRole() {
        return role;
    }

    public static int getCinemaId() {
        return cinemaId;
    }
    
    // =========================================================
    // ROLE CHECKS
    // =========================================================

    public static boolean isAdmin() {

        return role != null &&
               role.equalsIgnoreCase("admin");
    }


    public static boolean isManager() {

        return role != null &&
               role.equalsIgnoreCase("manager");
    }


    public static boolean isEmployee() {

        return role != null &&
               role.equalsIgnoreCase("employee");
    }

    // =========================================================
    // LOGOUT
    // =========================================================
    
    
    public static void clear() {
        employeeId = 0;
        fullName = null;
        role = null;
        cinemaId = 0;
    }
}