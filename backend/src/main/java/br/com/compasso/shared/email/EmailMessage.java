package br.com.compasso.shared.email;

/**
 * Mensagem de e-mail pronta para envio. O corpo nunca é registrado em log, pois
 * pode conter tokens de uso único.
 */
public record EmailMessage(String destinatario, String assunto, String corpo) {
}
