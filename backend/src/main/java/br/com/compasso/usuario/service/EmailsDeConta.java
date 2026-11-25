package br.com.compasso.usuario.service;

import br.com.compasso.shared.email.EmailMessage;

/**
 * Conteúdo mínimo dos e-mails transacionais. Os templates definitivos
 * dependem do provedor escolhido e permanecem pendentes.
 */
public final class EmailsDeConta {

    private EmailsDeConta() {
    }

    public static EmailMessage verificacao(String destinatario, String urlPublica, String token) {
        String corpo = """
                Olá!

                Confirme seu endereço de e-mail para liberar o acesso ao Compasso:
                %s/verificar-email?token=%s

                Este link é de uso único e expira em 1 hora.
                Se você não solicitou o cadastro, ignore esta mensagem.
                """.formatted(urlPublica, token);

        return new EmailMessage(destinatario, "Confirme seu e-mail no Compasso", corpo);
    }

    public static EmailMessage recuperacaoDeSenha(String destinatario, String urlPublica, String token) {
        String corpo = """
                Olá!

                Recebemos um pedido de redefinição de senha. Acesse:
                %s/redefinir-senha?token=%s

                Este link é de uso único e expira em 1 hora.
                Se você não solicitou a alteração, ignore esta mensagem.
                """.formatted(urlPublica, token);

        return new EmailMessage(destinatario, "Recuperação de senha do Compasso", corpo);
    }

    public static EmailMessage avisoEmailJaCadastrado(String destinatario, String urlPublica) {
        String corpo = """
                Olá!

                Recebemos uma tentativa de cadastro usando este endereço de e-mail,
                que já possui uma conta no Compasso. Se foi você, utilize a opção de
                recuperação de senha em %s.

                Se você não fez essa solicitação, nenhuma ação é necessária.
                """.formatted(urlPublica);

        return new EmailMessage(destinatario, "Aviso de segurança do Compasso", corpo);
    }
}
