package com.aoo.bcg.admin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Single source of truth for UI button, HTTP permission, target scope and risk level. */
public final class AdminPermissionCatalog {
    public record Route(String button, String method, String pattern, String permission,
            String targetType, AdminAccessPolicy.Risk risk) {
        public Route {
            Objects.requireNonNull(button); Objects.requireNonNull(method); Objects.requireNonNull(pattern);
            Objects.requireNonNull(permission); Objects.requireNonNull(targetType); Objects.requireNonNull(risk);
        }
    }

    private static final List<Route> ROUTES = List.of(
            route("game-profile-list", "GET", "/api/v2/admin/game-profiles", "game-profile.read", "GAME", false),
            route("map-ip-location", "GET", "/api/v2/admin/map/ip-location", "map.location.read", "GLOBAL", false),
            route("game-release-read", "GET", "/api/v2/admin/game-profile-releases/{scope}/{id}", "game-profile.read", "GAME", false),
            route("game-release-draft", "POST", "/api/v2/admin/game-profile-releases/{scope}/{id}/draft", "game-profile.draft", "GAME", false),
            route("game-release-validate", "POST", "/api/v2/admin/game-profile-releases/{scope}/{id}/validate", "game-profile.validate", "GAME", false),
            route("game-release-submit", "POST", "/api/v2/admin/game-profile-releases/{scope}/{id}/submit", "game-profile.submit", "GAME", false),
            route("game-release-approve", "POST", "/api/v2/admin/game-profile-releases/{scope}/{id}/approve", "game-profile.approve", "GAME", false),
            route("game-release-canary", "POST", "/api/v2/admin/game-profile-releases/{scope}/{id}/canary", "game-profile.canary", "GAME", true),
            route("game-release-activate", "POST", "/api/v2/admin/game-profile-releases/{scope}/{id}/activate", "game-profile.activate", "GAME", true),
            route("game-release-rollback", "POST", "/api/v2/admin/game-profile-releases/{scope}/{id}/rollback", "game-profile.rollback", "GAME", true),
            route("game-investigation-read", "GET", "/api/v2/admin/game-investigations/{scope}", "game-investigation.read", "ROOM", false),
            route("operation-switch-list", "GET", "/api/v2/admin/operation-switches", "operation-switch.read", "GAME", false),
            route("operation-switch-update", "PUT", "/api/v2/admin/operation-switches/{id}", "operation-switch.update", "GAME", true),
            route("appeal-list", "GET", "/api/v2/admin/appeals", "appeal.read", "CLUB", false),
            route("appeal-resolve", "PUT", "/api/v2/admin/appeals/{id}/resolve", "appeal.resolve", "CLUB", true),
            route("reconciliation-run", "GET", "/api/v2/admin/reconciliation", "reconciliation.read", "GLOBAL", false),
            route("data-lifecycle-list", "GET", "/api/v2/admin/data-lifecycle", "lifecycle.read", "REGION", false),
            route("data-lifecycle-update", "PUT", "/api/v2/admin/data-lifecycle/{id}", "lifecycle.update", "REGION", true),
            route("sensitive-export-request", "POST", "/api/v2/admin/sensitive-exports/{id}/request", "sensitive-export.request", "EXPORT", false),
            route("sensitive-export-approve", "POST", "/api/v2/admin/sensitive-exports/{id}/approve", "sensitive-export.approve", "EXPORT", true),
            route("sensitive-export-generate", "POST", "/api/v2/admin/sensitive-exports/{id}/generate", "sensitive-export.generate", "EXPORT", true),
            route("sensitive-export-download", "GET", "/api/v2/admin/sensitive-exports/{id}/download", "sensitive-export.download", "EXPORT", false));

    public List<Route> routes() { return ROUTES; }
    public Optional<Route> route(String method, String path) {
        return ROUTES.stream().filter(value -> value.method().equals(method) && matches(value.pattern(), path)).findFirst();
    }
    public String required(String method, String path) {
        return route(method, path).map(Route::permission).orElse("control.denied");
    }

    private static Route route(String button, String method, String pattern, String permission,
            String targetType, boolean highRisk) {
        return new Route(button, method, pattern, permission, targetType,
                highRisk ? AdminAccessPolicy.Risk.HIGH : AdminAccessPolicy.Risk.NORMAL);
    }
    private static boolean matches(String pattern, String path) {
        String regex = pattern.replace("{id}", "[A-Za-z0-9._-]+")
                .replace("{scope}", "[A-Za-z0-9._-]+");
        return path.matches(regex);
    }
}
