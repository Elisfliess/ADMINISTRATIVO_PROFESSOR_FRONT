# Arquitetura do Backend — Compasso

**Status:** decisões arquiteturais aprovadas para o MVP; implementação parcial.  
**Última revisão:** 15/11/2026.  
**Fonte complementar:** registros SDA-001 a SDA-009 e D-014 no Notion, vinculados na página de decisões do projeto.

## 1. Objetivo e princípios

Este documento é a referência técnica para implementar e revisar o backend. Requisitos funcionais, regras de negócio, MVP e decisões de produto continuam no Notion. Não ampliar o escopo com base em sugestões técnicas.

Princípios:
- Simplicidade adequada ao MVP; evitar overengineering.
- Monólito modular, sem microserviços.
- Regras de negócio pertencem aos módulos de domínio.
- Banco de dados protege integridade; aplicação executa os casos de uso.
- Decisões aprovadas não significam que a funcionalidade já esteja implementada.

## 2. Stack e persistência

- Java 21 LTS e Spring Boot 4.1.2, conforme stack aprovada no projeto.
- PostgreSQL 17 (ambiente Supabase Dev atualmente em 17.11).
- Flyway para evolução do schema.
- Hibernate com `ddl-auto=validate`.
- Maven Wrapper versionado no repositório.
- UUIDv7 gerado pela aplicação; não depender de função nativa do PostgreSQL.

### Credenciais e migrations

- `compasso_runtime`: operações normais da aplicação.
- `compasso_migrator`: aplicação de migrations.
- Profile padrão: Flyway desabilitado.
- Profile `migration`: habilita execução explícita com credenciais próprias.
- Nunca editar migration já aplicada; corrigir por nova migration.
- Não utilizar Hibernate `update` ou `create`.

## 3. Organização modular

Módulos de domínio:
- `usuario`
- `professor`
- `aluno`
- `agenda`
- `financeiro`
- `pagina_publica`

Módulo transversal:
- `shared`, sem dependência dos módulos de domínio.

Camada de aplicação:
- `application`, para orquestrar casos de uso que atravessam módulos.

Estrutura interna padrão de cada módulo: `controller`, `dto`, `entity`, `repository` e `service`. Criar mappers apenas quando necessário.

### Regras de dependência

- Módulos chamam funcionalidades de outros módulos por seus Services públicos, nunca acessando diretamente seus Repositories.
- Não criar dependências cíclicas; não usar `@Lazy` para mascarar ciclos.
- `usuario` não depende de módulos de negócio.
- `professor` pode consultar `usuario`; o inverso não deve ocorrer.
- `aluno`, `agenda`, `financeiro` e `pagina_publica` podem depender de `professor` quando necessário.
- `agenda` pode acionar `financeiro` para operações associadas a aulas; `financeiro` não chama `agenda`.
- `shared` não depende de módulos de domínio.
- `application` pode coordenar Services de vários módulos, mas não deve duplicar suas regras.

## 4. Camada application e transações

Casos de uso que atravessam módulos ficam na camada `application`. Exemplo: `InativarAlunoUseCase` coordena aluno, agenda e financeiro, enquanto cada módulo mantém a implementação de suas regras.

Cada caso de uso possui limite transacional explícito. Quando alterações em vários módulos formam uma única operação de negócio, todas são confirmadas juntas ou revertidas juntas.

A camada `application` coordena a transação. Services dos módulos participam da mesma unidade de trabalho, sem criar transações independentes para cada etapa.

Exemplos de operações transversais:
- Inativar aluno e tratar aulas, cobranças e plano relacionados.
- Remarcar aula e atualizar a cobrança pendente correspondente.
- Cancelar aula e cancelar cobrança pendente correspondente.
- Criar plano e primeira cobrança, quando aplicável.
- Criar aulas recorrentes e cobranças correspondentes, quando aplicável.
- Publicar a página pública: atualizar conteúdo de `PERFIL_PUBLICO` e modalidades em `pagina_publica` e canais públicos de contato armazenados em `PROFESSOR`, na mesma transação. A prévia é local no frontend; a publicação aprovada é a única operação que persiste essas alterações.

Respeitar a ordem de locks já definida no domínio, incluindo plano antes de aula. Não manter transação aberta durante chamadas externas.

## 5. Geração mensal de cobranças

- Agendamento diário com Spring `@Scheduled`.
- Fuso horário explícito: `America/Sao_Paulo`.
- O agendador apenas identifica trabalho devido e delega a um caso de uso da camada `application`.
- A rotina recupera períodos que ficaram sem processamento durante indisponibilidade.
- Cálculos e regras financeiras permanecem no domínio/caso de uso.
- Constraints específicas no PostgreSQL protegem contra duplicidade.
- Não adicionar Quartz, filas ou infraestrutura distribuída no MVP.

Detalhes operacionais do algoritmo e consultas serão definidos durante a implementação, respeitando as regras financeiras já aprovadas no Notion.

## 6. Integração de e-mail

- Definir contrato interno `EmailSender`.
- Implementar adaptador para o provedor escolhido; o provedor ainda será definido na implementação.
- Persistir token/dados primeiro e tentar enviar somente após o commit.
- Falha de envio não desfaz operação já persistida.
- Disponibilizar reenvio de verificação/recuperação.
- No MVP, não implementar broker, fila, Outbox ou retentativa automática.

Limitação aceita: a aplicação pode cair depois do commit e antes do envio, causando perda daquela tentativa. O usuário poderá solicitar novo envio.

## 7. Autenticação, sessão e segurança web

- Spring Security stateful com cookie `JSESSIONID`.
- Expiração após 30 minutos de inatividade.
- Sessões armazenadas em memória.
- Logout invalida a sessão; reiniciar a aplicação encerra sessões existentes.
- Uma única instância do backend no MVP.
- JWT, Redis, sessão compartilhada e escalabilidade horizontal fora do escopo atual.
- Se múltiplas instâncias forem necessárias, reavaliar a estratégia de sessão antes de adotá-las.

### CORS, CSRF e cookies

- CORS permite apenas origens explicitamente autorizadas.
- Credenciais habilitadas; nunca usar wildcard com credenciais.
- CSRF habilitado com `CookieCsrfTokenRepository`.
- Cookie CSRF: `XSRF-TOKEN`; header: `X-XSRF-TOKEN`.
- O login gera um novo token CSRF e o devolve em `Set-Cookie`; o cliente deve usar esse novo par nas operações seguintes.
- Limitação aceita no MVP: o padrão double-submit é stateless e não revoga server-side um cookie antigo. Bloqueio imediato de um par antigo exigiria armazenar o token vinculado à sessão, decisão arquitetural fora do escopo atual.
- `JSESSIONID` com `HttpOnly`.
- Definir `SameSite` quando a topologia de hospedagem frontend/backend for conhecida.
- HTTPS obrigatório antes do lançamento público.
- CSP será definida quando o frontend estiver estabelecido.
- Não incluir `Idempotency-Key` na allowlist CORS do MVP, pois idempotência genérica não foi aprovada.

## 8. Persistência e integridade

PostgreSQL é a fonte de verdade. Usar constraints para proteger invariantes estruturais (unicidade, chaves estrangeiras, obrigatoriedade e restrições de valores aplicáveis).

A aplicação executa os casos de uso e as regras de negócio. O banco pode aplicar constraints e proteções pontuais de integridade para impedir que dados persistidos violem invariantes já aprovadas. Não implementar fluxos de negócio ou orquestração de módulos em triggers.

### Exceção aprovada: aula cancelada definitiva

A migration V1 contém a função `impedir_reabertura_aula_cancelada()` e a trigger `trg_aula_cancelada_final`. Essa proteção impede que uma aula cujo status já seja `cancelada` volte a outro estado.

A trigger foi mantida deliberadamente como defesa de integridade no PostgreSQL, conforme decisão aprovada durante a consolidação das SPECs. Ela:

- Não substitui o caso de uso de cancelamento na aplicação.
- Não cancela cobranças nem executa operações em outros módulos.
- Não decide quando uma aula deve ser cancelada.
- Apenas rejeita uma atualização que viole a irreversibilidade do estado.
- Deve permanecer coberta por testes de integração PostgreSQL.

A aplicação deve validar a transição antes de persistir e traduzir eventual rejeição do banco em conflito de estado (409), sem expor SQL ou detalhes internos.

## 9. Testes

- Testes unitários para regras de negócio isoladas.
- Testes de integração para Services, Repositories, transações e constraints.
- Testes de API para autenticação, autorização e contratos HTTP.
- Testcontainers e CI validam PostgreSQL 17.11, alinhado ao ambiente Supabase Dev.
- Executar migrations Flyway desde um banco descartável limpo em PostgreSQL 17.
- Não utilizar H2 como substituto do PostgreSQL para testes de persistência.
- Não utilizar o Supabase de desenvolvimento como banco de testes automatizados.
- O workflow GitHub Actions está implementado e executa a validação do backend e o lint do contrato OpenAPI.

A CI atual não executa PostgreSQL 16; não declarar essa versão como validada. A execução local de Testcontainers exige Docker acessível ao processo Maven. Se indisponível, registrar a limitação; build sem esses testes não representa validação completa.

## 10. Isolamento e autorização por professor

A identidade do professor deve ser resolvida no backend a partir da sessão autenticada. Não confiar em `professor_id` enviado pelo frontend como prova de autorização. Recursos pertencentes a outro professor devem ser tratados como não encontrados (404), conforme a política de segurança aprovada.

## 11. Tratamento de erros e validação

- Bean Validation para formato, obrigatoriedade e validação de entrada.
- Regras de negócio nos Services/casos de uso.
- Tratamento centralizado por `@RestControllerAdvice`.
- Respostas de erro no formato `ProblemDetail` (RFC 9457), incluindo código de erro de domínio quando aplicável.
- Não expor stack traces, detalhes internos ou dados sensíveis ao cliente.

## 12. Decisões relacionadas e pendências

Registros detalhados: SDA-001 a SDA-009 e D-014 na pasta [02_DECISOES](https://www.notion.so/405f51667c7c818bbc4ec21a5d80ecff) do Notion. A [D-014 — sessão HTTP no MVP](https://www.notion.so/419f51667c7c817ba5fdd242ffef34a8) complementa a SDA-006: `JSESSIONID`, 30 minutos de inatividade, armazenamento em memória e uma instância; a tabela `SESSAO` é legada e não é usada pela aplicação.

Pendências deliberadas para etapas posteriores:
- Escolha do provedor de e-mail.
- `SameSite`, origens e configuração final de cookies após definição da hospedagem do frontend.
- Detalhamento operacional do algoritmo de recuperação do agendador.
- CSP quando o frontend estiver definido.

Essas pendências não autorizam antecipar ferramentas ou ampliar o escopo do MVP.
