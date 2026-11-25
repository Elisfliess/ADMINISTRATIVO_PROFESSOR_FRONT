package br.com.compasso.shared.uuid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void geraVersaoEVarianteConformeRfc9562() {
        UUID uuid = UuidV7.generate();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void codificaOTimestampNos48BitsMaisSignificativos() {
        long epochMilli = 1_700_000_000_123L;

        UUID uuid = UuidV7.generate(epochMilli);

        long timestamp = uuid.getMostSignificantBits() >>> 16;
        assertThat(timestamp).isEqualTo(epochMilli);
    }

    @Test
    void mantemOrdenacaoPorInstanteDeGeracao() {
        UUID anterior = UuidV7.generate(1_700_000_000_000L);
        UUID posterior = UuidV7.generate(1_700_000_001_000L);

        assertThat(anterior.compareTo(posterior)).isNegative();
    }

    @Test
    void naoRepeteIdentificadores() {
        Set<UUID> gerados = new HashSet<>();

        for (int i = 0; i < 10_000; i++) {
            gerados.add(UuidV7.generate());
        }

        assertThat(gerados).hasSize(10_000);
    }
}
