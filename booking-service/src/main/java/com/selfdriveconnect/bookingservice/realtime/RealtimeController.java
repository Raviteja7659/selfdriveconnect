package com.selfdriveconnect.bookingservice.realtime;

import com.selfdriveconnect.bookingservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class RealtimeController {

    private final RealtimeBroadcaster broadcaster;

    /**
     * The browser's native EventSource API can't attach an
     * Authorization header, so this endpoint is authenticated the
     * same way as everywhere else (JwtAuthFilter), but the filter has
     * a fallback: if there's no Authorization header, it also checks
     * a `token` query parameter. See JwtAuthFilter for that fallback
     * — this controller itself just relies on @AuthenticationPrincipal
     * like any other protected endpoint.
     */
    @GetMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser user) {
        return broadcaster.register(user.userId());
    }
}
