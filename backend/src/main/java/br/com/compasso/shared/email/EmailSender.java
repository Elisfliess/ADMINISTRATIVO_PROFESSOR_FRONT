package br.com.compasso.shared.email;

/**
 * Contrato interno de envio de e-mail. O adaptador do provedor escolhido deve
 * implementar esta interface; a aplicação nunca depende do provedor
 * diretamente.
 */
public interface EmailSender {

    void enviar(EmailMessage mensagem);
}
