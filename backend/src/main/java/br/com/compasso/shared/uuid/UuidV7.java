package br.com.compasso.shared.uuid;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Gerador de UUID versão 7 conforme RFC 9562.
 *
 * <p>O identificador é sempre gerado pela aplicação; o PostgreSQL não gera
 * chaves. Os 48 bits mais significativos armazenam o instante em
 * milissegundos, seguidos da versão 7, de 12 bits aleatórios e da variante
 * RFC, garantindo ordenação aproximada por data de criação.</p>
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        return generate(System.currentTimeMillis());
    }

    static UUID generate(long epochMilli) {
        byte[] aleatorio = new byte[10];
        RANDOM.nextBytes(aleatorio);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putShort((short) (epochMilli >>> 32));
        buffer.putInt((int) epochMilli);
        buffer.put((byte) (0x70 | (aleatorio[0] & 0x0F)));
        buffer.put(aleatorio[1]);
        buffer.put((byte) (0x80 | (aleatorio[2] & 0x3F)));
        buffer.put(aleatorio, 3, 7);

        return new UUID(buffer.getLong(0), buffer.getLong(8));
    }
}
