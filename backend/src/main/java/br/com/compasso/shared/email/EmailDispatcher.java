package br.com.compasso.shared.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Agenda o envio para depois do commit da transação corrente, conforme a
 * arquitetura: os dados são persistidos primeiro e a falha de envio não desfaz
 * a operação. Sem transação ativa, o envio ocorre imediatamente.
 */
@Component
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final EmailSender emailSender;

    public EmailDispatcher(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void enviarAposCommit(EmailMessage mensagem) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviarComSeguranca(mensagem);
                }
            });
            return;
        }

        enviarComSeguranca(mensagem);
    }

    private void enviarComSeguranca(EmailMessage mensagem) {
        try {
            emailSender.enviar(mensagem);
        } catch (RuntimeException ex) {
            // A operação já foi confirmada; o usuário poderá solicitar novo envio.
            log.error("Falha ao enviar e-mail com assunto '{}'", mensagem.assunto(), ex);
        }
    }
}
