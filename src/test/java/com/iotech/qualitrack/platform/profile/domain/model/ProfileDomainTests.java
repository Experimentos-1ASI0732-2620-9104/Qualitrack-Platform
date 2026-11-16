package com.iotech.qualitrack.platform.profile.domain.model;

import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;
import com.iotech.qualitrack.platform.profile.domain.model.commands.UpdateProfileCommand;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.Dni;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PersonName;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhoneNumber;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rules of the profile a person keeps in QualiTrack: personal data and photo.
 */
class ProfileDomainTests {

    @Test
    void personalDataIsValidatedAndBlankOptionalValuesAreCleared() {
        var command = UpdateProfileCommand.of(4L, "  María   Pérez ", "45678912", " +51 (01) 555-1234 ", "  ");
        assertThat(command.fullName().value()).isEqualTo("María Pérez");
        assertThat(command.phoneNumber().value()).isEqualTo("+51 (01) 555-1234");
        assertThat(command.location()).isNull();

        assertThatThrownBy(() -> new Dni("4567891A")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Dni("1234567")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhoneNumber("12345")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhoneNumber("98765abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UpdateProfileCommand.of(4L, " ", null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(PersonName.tryParse("x")).isEmpty();
    }

    @Test
    void theProfileTellsWhenTheNameChanged() {
        var profile = new Profile(4L, new PersonName("Luis Rojas"));
        assertThat(profile.update(new PersonName("Luis Rojas"), new Dni("45678912"), null, null)).isFalse();
        assertThat(profile.update(new PersonName("Luis Rojas Vega"), null, null, null)).isTrue();
        assertThat(profile.getDni()).isNull();
        assertThat(profile.isStored()).isFalse();
    }

    @Test
    void onlyRealImagesUpToTwoMegabytesAreAcceptedAsPhotos() {
        var png = Arrays.copyOf(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 32);
        var jpeg = Arrays.copyOf(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 32);
        var webp = Arrays.copyOf("RIFF\0\0\0\0WEBPVP8 ".getBytes(), 32);
        assertThat(PhotoImage.of(png).contentType()).isEqualTo("image/png");
        assertThat(PhotoImage.of(jpeg).contentType()).isEqualTo("image/jpeg");
        assertThat(PhotoImage.of(webp).contentType()).isEqualTo("image/webp");

        assertThatThrownBy(() -> PhotoImage.of(new byte[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhotoImage.of("GIF89a....".getBytes())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhotoImage.of(Arrays.copyOf(png, PhotoImage.MAX_SIZE_BYTES + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhotoImage(png, "image/jpeg")).isInstanceOf(IllegalArgumentException.class);

        var profile = new Profile(4L, null);
        var uploadedAt = Instant.parse("2026-10-04T15:00:00Z");
        profile.changePhoto(PhotoImage.of(png), uploadedAt);
        assertThat(profile.getPhoto().contentType()).isEqualTo("image/png");
        assertThat(profile.getPhoto().sizeBytes()).isEqualTo(32);
        assertThat(profile.getPhoto().updatedAt()).isEqualTo(uploadedAt);
        assertThat(profile.removePhoto()).isTrue();
        assertThat(profile.removePhoto()).isFalse();
    }
}
