package com.shopsphere.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GoogleAuthControllerTest {
    GoogleVerifier google = mock(GoogleVerifier.class);
    IamClient iam = mock(IamClient.class);
    JwtService jwt = mock(JwtService.class);
    GoogleAuthController controller = new GoogleAuthController(google, iam, jwt);
    GoogleVerifier.Identity identity = new GoogleVerifier.Identity("google-sub", "person@example.com", "Person");
    Long id = 42L;
    @BeforeEach void setup() { when(google.verify("valid-google-token")).thenReturn(identity); }

    @Test void customerGoogleLoginCreatesIamIdentityAndReturnsAppToken() {
        when(iam.findByEmail(identity.email())).thenReturn(null);
        when(iam.create(any())).thenReturn(new IamClient.IamUser(id, identity.email(), List.of("CUSTOMER"), true));
        when(jwt.issue(id.toString(), identity.email(), "CUSTOMER")).thenReturn("signed-app-token");
        var result = controller.google(new GoogleAuthController.GoogleRequest("valid-google-token", "customer"));
        assertEquals("signed-app-token", result.get("accessToken"));
        assertEquals("CUSTOMER", result.get("role"));
        verify(iam).create(new IamClient.CreateUserRequest(identity.email(), List.of("CUSTOMER")));
    }

    @Test void adminLoginCannotSelfRegister() {
        when(iam.findByEmail(identity.email())).thenReturn(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.google(new GoogleAuthController.GoogleRequest("valid-google-token", "admin")));
        assertEquals(403, ex.getStatusCode().value());
        verify(iam, never()).create(any());
    }

    @Test void wrongSiteRoleIsRejected() {
        when(iam.findByEmail(identity.email())).thenReturn(new IamClient.IamUser(id, identity.email(), List.of("CUSTOMER"), true));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.google(new GoogleAuthController.GoogleRequest("valid-google-token", "seller")));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test void unknownSiteIsRejectedBeforeCallingGoogle() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.google(new GoogleAuthController.GoogleRequest("valid-google-token", "root")));
        assertEquals(400, ex.getStatusCode().value());
        verifyNoInteractions(google, iam, jwt);
    }
}
