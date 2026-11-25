# SPEC — Usuário e autenticação

**Status:** primeira versão consolidada.  
**Módulo:** `usuario`  
**Rastreabilidade:** RF01–RF03, RF35–RF36, RF44, RF47; D-012; D-013; D-014; SDA-006; A10; A14.

## 1. Objetivo e responsabilidade

Responsável pela conta de acesso do professor, credenciais, autenticação, sessão, verificação de e-mail, recuperação de senha e registro de aceite dos documentos legais. O módulo não administra dados de negócio do professor, alunos ou agenda.

## 2. Funcionalidades do MVP

- Cadastro de professor/conta com e-mail e senha e dados iniciais necessários à criação do professor.
- Verificação obrigatória do e-mail por link antes de liberar acesso (D-013).
- Login, logout e consulta da identidade da sessão atual.
- Bloqueio após cinco tentativas consecutivas inválidas por 15 minutos, com resposta equivalente para e-mail existente e inexistente (RF01).
- Recuperação de senha por link de uso único e validade curta (RF02).
- Reenvio de recuperação respeitando espera de 45 segundos e limite de cinco por hora.
- Aceite dos Termos e da Política no primeiro acesso, com versão e data/hora (RF35).
- Exportação dos dados da conta em CSV após nova autenticação por senha.
- Encerramento de conta após exportação e confirmação explícita, coordenado com os módulos envolvidos (RF44), não como exclusão física simplista.

## 3. Regras de negócio

- Uma conta corresponde a um único professor (RN19).
- E-mail normalizado para minúsculas e único.
- Conta não verificada não pode iniciar sessão.
- D-013 substitui parcialmente D-012: confirmação por link é obrigatória e permanece o aviso de segurança ao dono do endereço quando há tentativa de cadastro de e-mail já existente.
- Respostas de cadastro, recuperação e reenvio não devem permitir enumeração de e-mails.
- Após cinco tentativas inválidas consecutivas, bloquear por 15 minutos. O contador deve ser reiniciado conforme o comportamento de autenticação bem-sucedida/expiração do bloqueio definido no RF01; não ampliar para bloqueios permanentes.
- Tokens de verificação e recuperação são de uso único, expiram e são armazenados de forma segura (hash conforme A14/A10). Nunca devolver token na resposta HTTP; entregar pelo e-mail.
- Senhas aceitas têm no mínimo 8 caracteres e no máximo 72 bytes em UTF-8, com ao menos uma letra e um dígito; não há exigência de caractere especial (decisão A1). O limite em bytes evita truncamento silencioso pelo BCrypt quando há caracteres multibyte.
- Tokens de verificação e de recuperação têm validade máxima de 1 hora (decisão A2). Emitir novo token do mesmo tipo invalida os pendentes.
- Reenvio de recuperação e de verificação: mínimo 45 segundos entre envios e máximo cinco por hora, por e-mail e tipo de token.
- Versões vigentes dos documentos legais são semeadas por migration (`termos_uso` e `politica_privacidade`, versão `1.0`, decisão B2); o aceite só é registrado para a versão vigente.
- Sessão stateful por cookie `JSESSIONID`, expiração por 30 minutos de inatividade, memória da instância (D-014, complementar à SDA-006). Logout invalida no servidor. Reinício encerra sessões.
- O login renova o token CSRF e devolve um novo cookie `XSRF-TOKEN`; o cliente deve usar esse novo par cookie/header nas operações mutáveis seguintes. O padrão double-submit não permite revogar server-side um cookie antigo, limitação aceita no MVP.
- A tabela física `SESSAO` permanece no schema por compatibilidade, mas não é consultada, gravada ou invalidada pela aplicação no MVP.
- Aceite registra versão e instante do servidor; cliente não escolhe timestamp.
- A opção “Não aceito: encerrar conta” direciona ao mesmo fluxo de encerramento, sem contornar exportação, reautenticação ou confirmação.
- `GET /auth/me` retorna somente id, email, emailVerificado e termosAceitos; dados do professor são consultados no módulo professor.
- JWT, refresh token, MFA e sessão distribuída não fazem parte do MVP.

## 4. Modelo e persistência

Referência A14 v3.4:
- `USUARIO`: credenciais e estado da conta, `email_verificado_em`, tentativas/bloqueio e datas de encerramento.
- `TOKEN_CONTA`: tokens de verificação e recuperação.
- `VERSAO_DOCUMENTO_LEGAL` e `ACEITE_DOCUMENTO_LEGAL`: versões e aceites.
- `SESSAO` é tabela legada e não é utilizada pela aplicação no MVP. Não persistir a sessão HTTP nela.

Não criar tabela de sessão alternativa nem guardar tokens em texto puro.

## 5. API

Contrato oficial em `docs/api/openapi.yaml`:
- `POST /auth/register`
- `POST /auth/verify-email`
- `POST /auth/verification/resend`
- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /auth/password-recovery`
- `POST /auth/password-recovery/confirm`
- `POST /account/exportacao`: exportação CSV após confirmação da senha.
- `POST /account/encerramento`: encerramento após nova autenticação e confirmação explícita.
- `POST /legal/acceptances`

Cadastro, verificação, login e recuperação são públicos; `logout`, `me`, exportação, encerramento e aceite exigem sessão. Exportação e encerramento devem continuar acessíveis mesmo quando o aceite legal estiver pendente, pois fazem parte da opção de recusa prevista no RF35. A exportação não encerra a conta; o encerramento invalida a sessão e solicita e-mail de confirmação após concluir a operação. CSRF permanece habilitado para operações mutáveis, inclusive login/logout conforme a configuração Spring Security adotada. Não inventar endpoint para expor token ou credenciais.

## 6. Validações e erros

- E-mail válido, até 254 caracteres e normalizado.
- Senha nunca retornada; aplicar hash seguro.
- Token inválido, expirado ou já usado gera erro de domínio sem revelar dados sensíveis.
- Falha de login usa mensagem genérica e não revela se e-mail existe.
- Usar Problem Details RFC 9457 e códigos estáveis.
- Códigos já publicados estão catalogados em `docs/api/README.md`; não renomear código publicado, apenas adicionar novos.
- 401 para sessão ausente/expirada; 403 para conta ainda não verificada ou operação proibida, sem vazar detalhes.
- 429 apenas para limites específicos aprovados.

## 7. Segurança e isolamento

- `JSESSIONID` HttpOnly; CSRF com `XSRF-TOKEN` e `X-XSRF-TOKEN`.
- CORS com origens explícitas e credentials; SameSite fica pendente da topologia frontend/backend.
- Tokens não aparecem em logs, respostas ou mensagens de erro.
- Não registrar senhas, cookies, conteúdo de autenticação ou dados pessoais desnecessários.
- Cadastro de e-mail já existente deve disparar aviso ao titular, conforme D-013, sem revelar a existência na resposta ao solicitante.

## 8. Transações e dependências

- Cadastro coordenado com módulo professor: conta e perfil inicial devem ser persistidos atomicamente; envio de e-mail somente após commit via `EmailSender`.
- Persistir token antes do envio. Falha no envio não desfaz a persistência; usuário pode solicitar reenvio.
- Aceite legal é persistido em transação própria.
- Exportação da conta exige reautenticação por senha e não altera estado.
- Encerramento exige reautenticação por senha e confirmação explícita; é caso transversal coordenado em `application`. Cada módulo mantém sua responsabilidade de anonimização/retensão. Ao concluir, invalidar a sessão e enviar e-mail de confirmação após o commit.
- `usuario` não depende de módulos de negócio. Não criar dependência de professor para autenticar; a coordenação de cadastro ocorre por application.

## 9. Testes e critérios de aceite

- Conta não verificada não autentica; após token válido, autentica.
- Token expirado/reutilizado não confirma e-mail nem redefine senha.
- Cadastro e recuperação não revelam existência de e-mail.
- Cinco falhas geram bloqueio por 15 minutos, independentemente da existência da conta.
- Reenvio de recuperação aplica 45 segundos e cinco por hora.
- Logout invalida sessão; sessão expira após 30 minutos inativa.
- Reinício da aplicação encerra sessões.
- `/auth/me` não retorna dados completos de professor.
- Exportação da conta exige senha e retorna CSV sem encerrar sessão.
- Encerramento exige senha e confirmação, invalida a sessão e envia confirmação por e-mail após a conclusão.
- Aceite registra versão e timestamp gerado pelo servidor.
- Enquanto o aceite estiver pendente, bloquear as demais funções administrativas, mantendo disponíveis leitura dos documentos, logout, exportação da conta e encerramento pelo fluxo “Não aceito: encerrar conta”.
- Testar CSRF, CORS e acesso sem sessão.

## 10. Pendências

- Provedor de e-mail e templates finais.
- Política final de SameSite/origens quando a topologia de hospedagem for definida.
- Detalhe de UX para token inválido/expirado, sem alterar a segurança definida.
- Expiração por 30 minutos de inatividade ainda não tem teste automatizado de tempo; validar quando houver estratégia de teste com relógio controlado.
- Bloqueio de login e limites de reenvio usam estado em memória, compatível apenas com a instância única do MVP; qualquer escala horizontal exige nova decisão.
