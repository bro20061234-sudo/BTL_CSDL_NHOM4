package vn.ktx.model;

public record UserSession(String username, String role, String studentId) {
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isStaff() { return isAdmin() || "STAFF".equals(role); }
}
