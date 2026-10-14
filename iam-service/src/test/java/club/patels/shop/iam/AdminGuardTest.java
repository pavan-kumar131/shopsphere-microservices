package club.patels.shop.iam;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminGuardTest {

    @Test
    void adminHeaderPasses() {
        assertThatCode(() -> AdminUserController.requireAdmin("CUSTOMER, ADMIN")).doesNotThrowAnyException();
    }

    @Test
    void missingOrNonAdminHeaderIsForbidden() {
        assertThatThrownBy(() -> AdminUserController.requireAdmin(null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AdminUserController.requireAdmin("CUSTOMER,SELLER")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AdminUserController.requireAdmin("SUPERADMIN")).isInstanceOf(ApiException.class);
    }

    @Test
    void emailsAreNormalised() {
        org.assertj.core.api.Assertions.assertThat(UserService.normalize("  Ankit@Gmail.COM ")).isEqualTo("ankit@gmail.com");
    }
}
