package org.devlive.metaforge.server.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.enums.NotificationScene;
import org.devlive.metaforge.service.notify.NotificationInbox;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

/** The signed-in user's notification center: list, counts, read state and removal of their own notifications. */
@RestController
@RequestMapping(value = "/api/v1/notification")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Collaborators are Spring-managed singletons")
public class NotificationController
{
    private final NotificationInbox inbox;

    public NotificationController(NotificationInbox inbox)
    {
        this.inbox = inbox;
    }

    @Data
    public static class ReadRequest
    {
        private boolean read = true;
    }

    private static Long user()
    {
        return UserDetailsService.getUser().getId();
    }

    private static boolean knownCategory(String category)
    {
        return category == null || category.isBlank() || NotificationScene.SYSTEM.equals(category)
                || Arrays.stream(NotificationScene.Category.values()).anyMatch(value -> value.name().equals(category));
    }

    @PostMapping(value = "list")
    public CommonResponse<NotificationInbox.Page> list(@RequestBody NotificationInbox.Query query)
    {
        if (!knownCategory(query.getCategory())) {
            return CommonResponse.failure("Unknown notification category: " + query.getCategory());
        }
        return CommonResponse.success(inbox.page(user(), query));
    }

    @GetMapping(value = "summary")
    public CommonResponse<NotificationInbox.Summary> summary()
    {
        return CommonResponse.success(inbox.summary(user()));
    }

    @GetMapping(value = "{code}")
    public CommonResponse<NotificationInbox.Item> info(@PathVariable(value = "code") String code)
    {
        return inbox.find(user(), code)
                .map(CommonResponse::success)
                .orElseGet(() -> CommonResponse.failure("Notification not found"));
    }

    @PutMapping(value = "{code}/read")
    public CommonResponse<Boolean> mark(@PathVariable(value = "code") String code, @RequestBody ReadRequest request)
    {
        return inbox.mark(user(), code, request.isRead()) ? CommonResponse.success(true) : CommonResponse.failure("Notification not found");
    }

    @PutMapping(value = "read-all")
    public CommonResponse<Map<String, Integer>> markAllRead(@RequestParam(value = "category", required = false) String category)
    {
        if (!knownCategory(category)) {
            return CommonResponse.failure("Unknown notification category: " + category);
        }
        return CommonResponse.success(Map.of("updated", inbox.markAllRead(user(), category)));
    }

    @DeleteMapping(value = "{code}")
    public CommonResponse<Boolean> delete(@PathVariable(value = "code") String code)
    {
        return inbox.delete(user(), code) ? CommonResponse.success(true) : CommonResponse.failure("Notification not found");
    }

    @DeleteMapping(value = "read")
    public CommonResponse<Map<String, Integer>> deleteRead()
    {
        return CommonResponse.success(Map.of("deleted", inbox.deleteRead(user())));
    }
}
