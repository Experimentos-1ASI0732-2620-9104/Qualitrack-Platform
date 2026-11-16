package com.iotech.qualitrack.platform.shared.interfaces.rest;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageLocalizationTests {
    private final Locale machineLocale = Locale.getDefault();

    @AfterEach
    void restoreLocales() {
        LocaleContextHolder.resetLocaleContext();
        Locale.setDefault(machineLocale);
    }

    @Test
    void spanishRequestsReceiveTheGeneralMessageInSpanishAndKeepTheDetails() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        var conflict = ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.conflict("Batch", "Released or rejected batches cannot consume raw materials"));
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        assertThat(conflict.getBody().message()).isEqualTo("Conflicto con el estado del recurso");
        assertThat(conflict.getBody().details()).isEqualTo("Released or rejected batches cannot consume raw materials");

        var missing = ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("Batch", 7L));
        assertThat(missing.getBody().message()).isEqualTo("No se encontró el recurso");
    }

    @Test
    void englishRequestsKeepTheMessageOfTheErrorEvenOnASpanishMachine() {
        Locale.setDefault(Locale.forLanguageTag("es-PE"));
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        var missing = ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("Batch", 7L));
        assertThat(missing.getStatusCode().value()).isEqualTo(404);
        assertThat(missing.getBody().message()).isEqualTo("Batch not found: 7");
    }
}
