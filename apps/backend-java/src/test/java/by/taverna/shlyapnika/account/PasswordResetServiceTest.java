package by.taverna.shlyapnika.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import by.taverna.shlyapnika.account.domain.PasswordResetTokenEntity;
import by.taverna.shlyapnika.account.domain.SiteAccountEntity;
import by.taverna.shlyapnika.account.infrastructure.PasswordResetTokenRepository;
import by.taverna.shlyapnika.account.infrastructure.SiteAccountRepository;
import by.taverna.shlyapnika.audit.AuditService;
import by.taverna.shlyapnika.schedule.domain.ConsentSnapshot;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetServiceTest {
  private final SiteAccountRepository accounts = mock(SiteAccountRepository.class);
  private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final PasswordResetMailer mailer = mock(PasswordResetMailer.class);
  private final AuditService auditService = mock(AuditService.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneOffset.UTC);
  private final PasswordResetService service = new PasswordResetService(
      accounts,
      tokens,
      passwordEncoder,
      mailer,
      auditService,
      new SecureRandom(new byte[] {1, 2, 3, 4}),
      clock
  );

  @Test
  void createsHashedResetTokenAndSendsEmailForActiveAccount() {
    var account = account("active");
    when(accounts.findByEmailIgnoreCase("master@example.com")).thenReturn(Optional.of(account));

    var message = service.requestReset("MASTER@example.com");

    var tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
    verify(tokens).save(tokenCaptor.capture());
    assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);
    assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(Instant.parse("2026-08-17T10:45:00Z"));
    verify(mailer).sendResetLink(eq("master@example.com"), any(String.class));
    assertThat(message).isEqualTo(PasswordResetService.PUBLIC_RESPONSE);
  }

  @Test
  void resetPasswordConsumesTokenAndIncrementsSessionVersion() {
    var account = account("active");
    var resetToken = PasswordResetTokenEntity.create(
        "prt_1",
        account,
        PasswordResetService.hashToken("reset-token"),
        Instant.parse("2026-08-17T10:30:00Z")
    );
    when(tokens.findByTokenHash(PasswordResetService.hashToken("reset-token"))).thenReturn(Optional.of(resetToken));
    when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");

    service.resetPassword("reset-token", "new-password", "new-password");

    assertThat(account.getPasswordHash()).isEqualTo("encoded-new-password");
    assertThat(account.getSessionVersion()).isEqualTo(1);
    assertThat(resetToken.getUsedAt()).isNotNull();
    verify(accounts).save(account);
    verify(tokens).save(resetToken);
  }

  private SiteAccountEntity account(String status) {
    return SiteAccountEntity.create(
        "acc_master",
        "Мастер",
        "master@example.com",
        "old-hash",
        "master",
        status,
        "@master",
        "master",
        new ConsentSnapshot("1.0", "1.0", Instant.parse("2026-08-17T09:00:00Z"), "test")
    );
  }
}
