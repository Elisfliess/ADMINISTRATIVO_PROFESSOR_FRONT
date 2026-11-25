package br.com.compasso.shared.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador temporário usado enquanto o provedor definitivo (Resend) não é
 * integrado. Não envia mensagens e não registra destinatário, corpo ou tokens.
 */
@Component
@ConditionalOnProperty(name = "compasso.email.provider", havingValue = "noop", matchIfMissing = true)
public class NoOpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailSender.class);

    @Override
    public void enviar(EmailMessage mensagem) {
        log.info("Envio de e-mail desativado (provedor noop). Assunto: '{}'", mensagem.assunto());
    }
}
