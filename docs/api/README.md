# Contrato da API — Compasso

**Status:** contrato MVP aprovado e vigente; mudanças futuras devem passar por revisão e atualização coordenada das SPECs.  
**Contrato formal:** [openapi.yaml](openapi.yaml) — OpenAPI 3.1.0.  
**Escopo:** MVP vigente, conforme D-012, D-013, D-014, RF01–RF61 e RN01–RN37.

## Autoridade e manutenção

O arquivo `openapi.yaml` é a fonte oficial do contrato HTTP. As SPECs dos módulos explicam comportamento e critérios de aceite; não devem contradizer o OpenAPI. Se uma SPEC exigir mudança de rota, método, schema, status ou segurança, atualizar o contrato na mesma alteração.

- Requisitos, regras de negócio, escopo e decisões de produto: Notion.
- Código, testes e referência técnica: este repositório.
- OpenAPI: fonte oficial dos contratos HTTP.
- SPECs: especificação de comportamento e implementação por módulo.

Decisões arquiteturais aprovadas não significam que as funcionalidades estejam implementadas.

## Convenções gerais

- Prefixo obrigatório: `/api/v1`.
- JSON em UTF-8; propriedades em `camelCase`.
- Recursos no plural; identificadores UUID em formato textual padrão.
- IDs são gerados pelo backend.
- Datas civis: `YYYY-MM-DD`. Instantes: ISO 8601 com offset. Persistência de instantes em UTC.
- Regras de calendário e vencimento usam `America/Sao_Paulo`.
- Sucesso sem envelope global: objeto ou array diretamente na raiz.
- Operação concluída sem representação: `204 No Content`, sem corpo.
- Coleções grandes usam paginação; coleções pequenas/estáticas podem retornar array direto quando indicado na operação.
- Paginação: `page` inicia em 0; `size` padrão 20, máximo 100; `sort=campo,direcao`.
- Ordenação padrão: `id,asc`, quando o endpoint aceitar ordenação; cada endpoint restringe os campos permitidos.
- UUID é usado como critério secundário para manter a ordenação estável quando necessário.
- Filtros são query params específicos por endpoint.
- Propriedades JSON desconhecidas são rejeitadas com `400 Bad Request`.
- Enums são strings `UPPER_SNAKE_CASE`, independentes dos nomes Java.
- DTOs de entrada e saída são distintos; Entities JPA nunca são expostas.

## HTTP

| Situação | Status |
| --- | --- |
| Consulta ou alteração com representação | 200 |
| Criação de recurso | 201 |
| Operação concluída sem conteúdo | 204 |
| Entrada inválida / JSON inválido / propriedade desconhecida | 400 |
| Ausência de sessão ou sessão expirada | 401 |
| Autenticado, mas sem permissão para a operação | 403 |
| Recurso inexistente ou pertencente a outro professor | 404 |
| Conflito com estado atual ou regra de unicidade | 409 |
| Limite específico excedido | 429 |
| Erro inesperado | 500 |

Não adotar 422 no MVP. Cada operação declara os status que realmente pode produzir; a tabela não significa que todo endpoint use todos eles.

## Erros — RFC 9457

Todos os erros são `application/problem+json`. Campos padrão: `type`, `title`, `status`, `detail`, `instance`; extensão `code` estável e, em erros de validação, `errors`.

```json
{
  "type": "about:blank",
  "title": "Erro de validação",
  "status": 400,
  "detail": "Um ou mais campos são inválidos.",
  "instance": "/api/v1/alunos",
  "code": "VALIDATION_ERROR",
  "requestId": "uuid",
  "errors": [
    {
      "field": "email",
      "code": "INVALID_FORMAT",
      "message": "Informe um endereço de e-mail válido."
    }
  ]
}
```

O servidor gera um identificador de correlação, devolve-o no header `X-Request-Id` e inclui o mesmo valor como `requestId` no Problem Details. Não aceitar identificador arbitrário do cliente como fonte confiável. Nunca incluir stack trace, SQL, classes internas, tokens ou credenciais.

### Catálogo de códigos de erro

Códigos já publicados são estáveis: podem ser adicionados, mas não renomeados. Esta tabela lista os códigos implementados e deve acompanhar a evolução dos módulos.

| `code` | Status | Situação |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Um ou mais campos inválidos; `errors` detalha cada campo. |
| `MALFORMED_JSON` | 400 | JSON ilegível ou propriedade desconhecida. |
| `METHOD_NOT_ALLOWED` | 405 | Método HTTP não suportado pelo recurso. |
| `UNAUTHORIZED` | 401 | Sessão ausente ou expirada. |
| `INVALID_CREDENTIALS` | 401 | E-mail ou senha inválidos, sem revelar existência da conta. |
| `FORBIDDEN` | 403 | Operação não permitida, incluindo falha de CSRF. |
| `EMAIL_NOT_VERIFIED` | 403 | Conta ainda não confirmou o e-mail. |
| `ACCOUNT_DISABLED` | 403 | Conta encerrada ou indisponível para acesso. |
| `LEGAL_ACCEPTANCE_REQUIRED` | 403 | Aceite dos documentos legais pendente. |
| `NOT_FOUND` | 404 | Recurso inexistente ou pertencente a outro professor. |
| `LEGAL_VERSION_NOT_FOUND` | 404 | Versão de documento legal não vigente. |
| `CONFLICT` | 409 | Conflito com o estado atual ou regra de unicidade. |
| `TOKEN_INVALID_OR_EXPIRED` | 409 | Token de conta inválido, expirado ou já utilizado. |
| `ACCOUNT_LOCKED` | 429 | Bloqueio por tentativas de login; acompanha `Retry-After`. |
| `RESEND_TOO_SOON` | 429 | Espera mínima de 45 segundos entre reenvios; acompanha `Retry-After`. |
| `RESEND_LIMIT_EXCEEDED` | 429 | Limite de cinco reenvios por hora; acompanha `Retry-After`. |
| `PASSWORD_TOO_SHORT`, `PASSWORD_TOO_LONG`, `PASSWORD_TOO_WEAK` | 400 | Política de senha: no mínimo 8 caracteres, no máximo 72 bytes em UTF-8, ao menos uma letra e um dígito. |
| `CNPJ_INVALID` | 400 | CNPJ com tamanho, formato ou dígitos verificadores inválidos. Desde 31/07/2026 o formato aceito é alfanumérico: 12 caracteres `0-9`/`A-Z` e 2 dígitos verificadores. |
| `INTERNAL_ERROR` | 500 | Falha inesperada; detalhes internos não são expostos. |

Em erros de validação, o campo `errors[].code` usa códigos de campo: `REQUIRED`, `INVALID_FORMAT`, `INVALID_LENGTH`, `OUT_OF_RANGE` e `INVALID_VALUE`.

## Paginação

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

Quando a operação for paginada, os metadados acima são obrigatórios. O critério UUID secundário garante ordenação estável entre páginas.

## Autenticação e sessão

- Spring Security stateful; cookie `JSESSIONID`, HttpOnly.
- Sessão em memória, expiração após 30 minutos de inatividade, uma instância no MVP (D-014; complementar à SDA-006). A tabela física `SESSAO` é mantida por compatibilidade do schema, mas não é usada pela aplicação.
- `POST /auth/logout` invalida a sessão.
- Cadastro com e-mail já existente retorna exatamente a mesma resposta genérica do cadastro novo; o titular recebe aviso de segurança conforme D-013.
- Reinício da aplicação encerra sessões existentes.
- JWT, Redis e sessões distribuídas estão fora do MVP.
- Endpoints administrativos exigem sessão, salvo exceção explicitamente pública.
- CSRF permanece habilitado: cookie `XSRF-TOKEN`, header `X-XSRF-TOKEN`.
- O cliente obtém o cookie `XSRF-TOKEN` em uma requisição `GET` (por exemplo `GET /auth/me`) e reenvia o valor no header `X-XSRF-TOKEN` em toda operação mutável. Sem o par cookie/header válido, a resposta é `403`.
- O login troca o identificador da sessão antes de autenticar, mitigando fixação de sessão, e renova o token CSRF. O cliente deve usar o par cookie/header devolvido na própria resposta de login nas operações seguintes.
- Como o CSRF usa o padrão double-submit stateless, um par cookie/header antigo ainda é aceito enquanto o cookie existir. Revogação server-side exigiria vincular o token à sessão, decisão fora do MVP.
- CORS permite origens explícitas e credenciais; nunca wildcard com credenciais.
- `SameSite` e origens definitivas dependem da topologia de hospedagem frontend/backend.
- HTTPS obrigatório antes do lançamento público.

## PATCH

Usar JSON Merge Patch (RFC 7396), com `Content-Type: application/merge-patch+json`.

- Propriedade ausente: manter valor atual.
- Propriedade com `null`: limpar quando o campo permitir.
- Arrays são substituídos integralmente.
- Cada schema de PATCH deve indicar quais propriedades podem ser limpas.
- A implementação deve distinguir propriedade ausente de propriedade presente com null; DTO simples que perde essa distinção não atende ao contrato.

## Publicação da página pública

- A prévia de edição é mantida exclusivamente no estado local do frontend; não existe rascunho persistido no MVP.
- Cancelar/rejeitar a prévia não realiza chamada de escrita e preserva a configuração publicada.
- `GET /publico/perfil` consulta a configuração editorial persistida (`PERFIL_PUBLICO` e `MODALIDADE_AULA`).
- `POST /publico/perfil/publicacao` recebe o snapshot completo aprovado (perfil e modalidades) e persiste/publica tudo em uma única transação.
- A resposta pública `GET /publico/{slug}` compõe dados públicos do professor (`nome`, `razaoSocial`, `cnpj`, `emailContato`, `telefoneContato`, `instagramUrl`), dados editoriais do perfil (`apresentacao`, `whatsappNumero`, `whatsappMensagem`) e modalidades publicadas.
- `GET /publico/perfil` fornece ao editor o snapshot persistido, incluindo os canais gerais armazenados em `PROFESSOR`; `GET /professor` fornece os dados de identificação do titular (nome, razão social e CNPJ) usados na prévia.
- A operação de publicação atualiza atomicamente `PERFIL_PUBLICO`, `MODALIDADE_AULA` e os canais públicos em `PROFESSOR`, com coordenação de `application`. `PATCH /professor` não aceita alterações dos canais públicos, evitando contornar a prévia e aprovação.
- Na A14 v3.4, modalidades usam `publicada` e `valor_atualizado_em`; a API apresenta `publicada` e `valorAtualizadoEm`. A data de atualização é controlada pelo backend.
- Não existe `PATCH /publico/perfil` nem endpoint de salvamento provisório.
- Se a publicação falhar, a transação é revertida e o conteúdo público anterior permanece disponível.

## Encerramento da conta

- `POST /account/exportacao` exige reautenticação por senha e retorna CSV sem encerrar a sessão.
- `POST /account/encerramento` exige nova autenticação e confirmação explícita; a camada `application` coordena exclusão/anonimização conforme retenção, invalida a sessão e solicita e-mail de confirmação após a conclusão.
- A opção “Não aceito: encerrar conta” do aceite legal usa o mesmo fluxo, sem pular exportação ou confirmação. As rotas de exportação e encerramento permanecem acessíveis mesmo com aceite pendente; as demais funções administrativas continuam bloqueadas.

## Agenda: prévias sem mutação

- `POST /aulas/previa` calcula a quantidade de aulas e cobranças previstas, incluindo valores e vencimentos para plano por aula, sem persistir.
- `GET /aulas/{aulaId}/cancelamento/previa` apresenta o impacto financeiro do cancelamento sem mutação.
- A operação efetiva sempre recalcula e valida as regras no backend; prévias não reservam nem garantem estado futuro.

## Geração de cobranças

- Não existe `POST /cobrancas` no MVP. A criação manual/avulsa está fora do escopo D-012.
- Cobranças são geradas pelos casos de uso de planos mensais e pelo agendamento de aulas em planos por aula. O professor pode listar, consultar, baixar ou cancelar cobranças existentes.

## Validação e responsabilidade

- Bean Validation: formato, presença, tamanho, limites e estrutura.
- Services/casos de uso: regras de negócio, transições de estado, isolamento e conflitos.
- Controllers: HTTP, binding e delegação.
- Frontend pode validar para UX, mas backend é fonte de verdade.
- Propriedades desconhecidas devem ser rejeitadas.

## Fora do contrato MVP

Não incluir endpoints de JWT/refresh, login de aluno, pagamentos online, estorno, MFA, upload de mídia, notificações automáticas, auditoria abrangente, Idempotency-Key genérico ou recursos de evoluções futuras.
