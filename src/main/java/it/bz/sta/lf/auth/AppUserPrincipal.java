package it.bz.sta.lf.auth;

public record AppUserPrincipal(
        String id,
        String email,
        String role,
        String company
) {
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}