package com.elmeftouhi.facturesimple.user;

import com.elmeftouhi.facturesimple.security.JwtPrincipal;
import com.elmeftouhi.facturesimple.tenant.dto.TenantMembershipResponse;
import com.elmeftouhi.facturesimple.user.dto.MeResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserProfileService userProfileService;

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal JwtPrincipal principal) {
        return userProfileService.me(principal);
    }

    @GetMapping("/tenants")
    public List<TenantMembershipResponse> myTenants(@AuthenticationPrincipal JwtPrincipal principal) {
        return userProfileService.myTenants(principal);
    }
}

