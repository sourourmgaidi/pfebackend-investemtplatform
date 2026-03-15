package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.Notification;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Service.NotificationService;
import tn.iset.investplatformpfe.Service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Role role = extractRoleFromJwt(jwt);
            Long userId = userService.getUserIdByEmailAndRole(email, role);

            List<Notification> notifications = notificationService.getUserNotifications(role, userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Role role = extractRoleFromJwt(jwt);
            Long userId = userService.getUserIdByEmailAndRole(email, role);

            List<Notification> unread = notificationService.getUnreadNotifications(role, userId);
            return ResponseEntity.ok(unread);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/unread/count")
    public ResponseEntity<?> countUnreadNotifications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Role role = extractRoleFromJwt(jwt);
            Long userId = userService.getUserIdByEmailAndRole(email, role);

            long count = notificationService.countUnread(role, userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Role role = extractRoleFromJwt(jwt);
            Long userId = userService.getUserIdByEmailAndRole(email, role);

            notificationService.markAllAsRead(role, userId);
            return ResponseEntity.ok(Map.of("message", "Toutes les notifications ont été marquées comme lues"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Role extractRoleFromJwt(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                if (roles.contains("ADMIN")) return Role.ADMIN;
                if (roles.contains("LOCAL_PARTNER")) return Role.LOCAL_PARTNER;
                if (roles.contains("PARTNER")) return Role.PARTNER;
                if (roles.contains("INTERNATIONAL_COMPANY")) return Role.INTERNATIONAL_COMPANY;
                if (roles.contains("INVESTOR")) return Role.INVESTOR;
                if (roles.contains("TOURIST")) return Role.TOURIST;
            }
        }
        throw new RuntimeException("Rôle non trouvé dans le token");
    }
}