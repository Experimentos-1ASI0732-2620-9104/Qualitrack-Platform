package com.iotech.qualitrack.platform.iam.infrastructure.notifications;

import com.iotech.qualitrack.platform.iam.application.commandservices.UserCommandService;
import com.iotech.qualitrack.platform.iam.domain.model.commands.CreateStaffAccountCommand;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.Roles;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * The credentials of a staff account are e-mailed when an SMTP server is configured.
 */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:staff_mail;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "application.frontend-url=https://qualitrack.test"})
class StaffCredentialsEmailTests {
    @Autowired UserCommandService users;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void credentialsAreEmailedToTheStaffMember() {
        var email = "mailed-" + UUID.randomUUID() + "@qualitrack.test";
        var result = users.handle(new CreateStaffAccountCommand(7L, email, "Ana Torres", Roles.ROLE_LAB_OPERATOR));

        assertThat(result).isInstanceOf(Result.Success.class);
        var account = ((Result.Success<UserCommandService.StaffAccount, ?>) result).value();
        assertThat(account.credentialsSent()).isTrue();
        assertThat(account.user().isPasswordChangeRequired()).isTrue();
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly(email);
        assertThat(message.getValue().getText()).contains(email).contains(account.temporaryPassword()).contains("Ana Torres")
                .contains("https://qualitrack.test/iam/sign-in");
    }

    @Test
    void aFailedDeliveryLeavesTheTemporaryPasswordForTheQualityManager() {
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
        var result = users.handle(new CreateStaffAccountCommand(7L, "unsent-" + UUID.randomUUID() + "@qualitrack.test",
                "Luis Ramos", Roles.ROLE_AUDITOR));

        var account = ((Result.Success<UserCommandService.StaffAccount, ?>) result).value();
        assertThat(account.credentialsSent()).isFalse();
        assertThat(account.temporaryPassword()).hasSize(12);
    }
}
