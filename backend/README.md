# Backend — Compasso API

> Para instruções completas de preparação do ambiente, execução pelo terminal e uso em Windows sem acesso de administrador, consulte o [guia de Setup Local](../docs/SETUP.md).

API REST do projeto Compasso, construída com Java 21 e Spring Boot 4.1.2.

## Requisitos

- Git.
- JDK 21.
- Acesso autorizado ao projeto Supabase de desenvolvimento.
- IDE compatível com Java (recomendada, mas não obrigatória).

O repositório inclui Maven Wrapper 3.3.4 e utiliza a distribuição Maven 3.9.9. **Não é necessário instalar Maven globalmente.** Os comandos abaixo usam sempre o Wrapper. Ele baixa a distribuição Maven na primeira execução.

> **Importante:** Maven Wrapper elimina a necessidade de instalar Maven na máquina, mas não substitui o JDK. Java 21 precisa estar instalado e disponível para a IDE/terminal. No IntelliJ, confira o Project SDK e o JRE usado pelo Maven Runner.

## Configuração do ambiente

O arquivo `backend/.env.enc` contém o ambiente de desenvolvimento criptografado com [age](https://age-encryption.org/) e **é versionado no Git** para que alterações nas variáveis sejam distribuídas junto com o código. O arquivo `.env` em texto puro nunca é versionado. A senha do arquivo criptografado não fica no repositório: compartilhe-a com integrantes autorizados por um canal separado.

### Preparação feita pela pessoa responsável

1. Mantenha o arquivo `.env` local em `backend/.env`, criado a partir de `.env.example`.
2. Instale o [age](https://age-encryption.org/) e o PowerShell 7 (`pwsh`).
3. Execute, a partir da raiz do repositório:

   ```powershell
   pwsh -File .\backend\scripts\encrypt-env.ps1
   ```

4. Informe uma senha forte quando o age solicitar. O age pedirá a confirmação.
5. Revise o arquivo gerado e faça commit de `backend/.env.enc` na branch de trabalho. Compartilhe a senha com os integrantes autorizados por um canal diferente do Git.

O script não sobrescreve um `.env.enc` existente. Para atualizar o ambiente, edite o `.env` local, remova a versão criptografada anterior, execute novamente o script e versione o novo `backend/.env.enc`. Assim, as mudanças de configuração acompanham o histórico do projeto sem expor os valores em texto puro.

### Configuração feita por cada integrante

1. Instale JDK 21, PowerShell 7 e [age](https://age-encryption.org/). Docker só é necessário para os testes de integração.
2. Atualize sua branch e confirme que `backend/.env.enc` está presente no repositório.
3. Execute na raiz do repositório:

   ```powershell
   pwsh -File .\backend\scripts\run-with-encrypted-env.ps1
   ```

4. Digite a senha quando o age solicitar. A aplicação será iniciada pelo Maven Wrapper.

O script descriptografa o conteúdo em memória e injeta no processo as variáveis com nomes válidos encontradas no arquivo, sem exigir alteração do script quando novas configurações forem adicionadas. Variáveis reservadas do sistema operacional, como `PATH`, `JAVA_HOME` e `TEMP`, são rejeitadas. **Ele não cria um arquivo `.env` em texto puro.** Ao encerrar a aplicação, restaura as variáveis de ambiente anteriores do processo PowerShell.

**Limite de segurança importante:** isso evita deixar uma cópia em texto puro no diretório do projeto, mas não cria isolamento contra processos executados com as mesmas permissões do usuário. Enquanto a aplicação estiver em execução, processos com acesso equivalente podem, dependendo do sistema, inspecionar variáveis de ambiente ou memória. Portanto, não execute o backend com esse fluxo enquanto uma IA local ou ferramenta não confiável tiver acesso irrestrito ao mesmo usuário/processo. A senha compartilhada também permite que qualquer integrante que a conheça descriptografe todas as versões históricas do arquivo. Ao remover alguém da equipe ou suspeitar de exposição, rotacione as credenciais, gere uma nova versão criptografada e troque a senha. Antes de tornar o repositório público, faça essa rotação também; versões antigas continuarão no histórico, mas os valores antigos precisam estar revogados.

### Alternativa: configuração local manual

Se necessário, também é possível copiar `.env.example` para `.env` na pasta `backend` e preencher as variáveis manualmente. Não compartilhe nem versione esse arquivo.

A URL JDBC deve seguir o formato:

```properties
DB_URL=jdbc:postgresql://HOST:PORTA/postgres?sslmode=require
DB_USERNAME=USUARIO_DO_BANCO
DB_PASSWORD=SENHA_DO_BANCO
```

Use os valores de host, porta e usuário correspondentes ao método de conexão escolhido no Supabase (conexão direta ou Session Pooler). Não suponha que o usuário do pooler seja igual ao usuário da conexão direta.

Consulte [Configuração do banco de desenvolvimento](docs/database/DATABASE-SETUP.md) para detalhes e cuidados.

### Variáveis opcionais da aplicação

| Variável | Padrão | Uso |
| --- | --- | --- |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origens permitidas, separadas por vírgula. Não usar wildcard com credenciais. |
| `EMAIL_PROVIDER` | `noop` | Provedor de e-mail. `noop` apenas registra o assunto, sem enviar. O adaptador Resend ainda não foi implementado. |
| `EMAIL_FROM` | `nao-responder@compasso.local` | Remetente das mensagens transacionais. |
| `APP_PUBLIC_URL` | `http://localhost:5173` | URL pública usada para montar os links de verificação e recuperação enviados por e-mail. |

Com `EMAIL_PROVIDER=noop`, tokens de verificação e recuperação não são expostos em resposta HTTP, log ou console: sem provedor real, o fluxo de confirmação por e-mail não pode ser concluído manualmente.

## Executar

Execute os comandos a partir da pasta `backend`.

Linux/macOS:
```bash
./mvnw spring-boot:run
```

Windows PowerShell:
```powershell
.\mvnw.cmd spring-boot:run
```

## Testes

Linux/macOS:
```bash
./mvnw test
```

Windows PowerShell:
```powershell
.\mvnw.cmd test
```

Os testes de integração utilizam PostgreSQL efêmero via Testcontainers. **Docker precisa estar instalado, em execução e acessível ao processo que executa Maven.** Se os testes forem iniciados pelo IntelliJ, o Docker também precisa estar corretamente configurado e acessível a partir do ambiente da IDE. Isso não é requisito para executar a aplicação no fluxo padrão com Supabase.

Se Docker não estiver disponível, é possível verificar apenas a compilação/empacotamento sem executar testes com:

```powershell
.\mvnw.cmd -DskipTests package
```

Esse comando **não valida os testes** e não deve ser tratado como substituto de `test` ou da CI. A CI executa os testes em runners com Docker disponível. Não use opções para ignorar testes como forma de declarar a validação concluída.

## Health check

Com a aplicação em execução:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

Apenas os endpoints `health` e `info` são expostos via HTTP. Detalhes internos de saúde não são expostos.

## Banco de dados e migrations

- PostgreSQL é o banco oficial do projeto.
- Flyway é a fonte versionada do schema em `src/main/resources/db/migration`.
- Hibernate usa `ddl-auto=validate`: não cria nem altera tabelas.
- Não faça alterações manuais de schema no painel do Supabase como procedimento normal.
- Não edite migrations já aplicadas; crie uma nova migration corretiva.
- Não execute testes automatizados destrutivos no banco compartilhado.

A cadeia versionada contém V1–V5: V1 cria o schema inicial; V2 adiciona garantias de integridade para planos, aulas e cobranças; V3 semeia as versões `1.0` dos Termos de Uso e da Política de Privacidade; V4 permite o formato alfanumérico de CNPJ; V5 garante a unicidade de cobranças mensais por plano e início do período. A V1 foi criada com base em uma versão anterior do modelo; a referência documental vigente é a A14 v3.4, e mudanças posteriores são registradas em novas migrations. A CI utiliza PostgreSQL 17.11 via Testcontainers. A última confirmação disponível do Supabase Dev registra V1 e V2 aplicadas; a aplicação de V3, V4 e V5 no banco compartilhado não está confirmada nesta revisão. Não deduza o estado do banco compartilhado apenas pela presença das migrations no Git.

Por padrão, Flyway não executa migrations ao iniciar a API. Para uma alteração aprovada no banco compartilhado, somente a pessoa autorizada deve configurar `FLYWAY_USERNAME` e `FLYWAY_PASSWORD` e iniciar uma vez com o perfil `migration`:

Linux/macOS:
```bash
SPRING_PROFILES_ACTIVE=migration ./mvnw spring-boot:run
```

Windows PowerShell:
```powershell
$env:SPRING_PROFILES_ACTIVE = "migration"
.\mvnw.cmd spring-boot:run
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

O perfil `migration` usa credenciais separadas para Flyway. A API continua usando `DB_USERNAME` e `DB_PASSWORD` para runtime.

## Segurança do ambiente

- Use somente dados fictícios/sintéticos.
- Nunca utilize credenciais de produção no desenvolvimento.
- Nunca versione `.env` em texto puro, senhas, tokens ou connection strings reais. O único arquivo de ambiente permitido no Git é `backend/.env.enc`, criptografado com `age`.
- Não use credenciais administrativas do Supabase como credenciais permanentes da aplicação.
- O Supabase de desenvolvimento deve permanecer separado de qualquer futuro ambiente de produção.

## PostgreSQL local (opcional)

Quem preferir executar PostgreSQL localmente pode seguir [PostgreSQL local opcional](docs/database/LOCAL-DATABASE.md). Essa configuração é individual e não é requisito para contribuir com o Backend.

## Estado do Backend

Implementado nesta etapa:

- **Fundação compartilhada:** Problem Details, validação, correlação por `X-Request-Id`, geração de UUIDv7, contrato de e-mail e envio após o commit.
- **Segurança e autenticação:** Spring Security stateful com `JSESSIONID`, CSRF, CORS restrito, bloqueio de login e limites de reenvio; módulo `usuario` com cadastro, verificação de e-mail, login/logout, `/auth/me`, recuperação de senha, reenvio e aceite legal.
- **Professor:** criação coordenada no cadastro e `GET /professor` para o titular autenticado. A edição `PATCH /professor` ainda não está implementada.
- **Alunos:** `GET /alunos` com filtro por nome/ativo e paginação, `POST /alunos`, `GET /alunos/{alunoId}`, `PATCH /alunos/{alunoId}` com JSON Merge Patch, reativação, exportação CSV e anonimização com senha/confirmação explícita.
- **Financeiro:** planos de pagamento por aluno (listar/criar/consultar/atualizar/encerrar); cobranças (listar/consultar/baixar/cancelar); geração mensal agendada; serviço público para geração de cobranças por aula; exportação CSV com reautenticação por senha. Implementação e correções integradas pelo PR #17.
- **Testes:** testes unitários e de API com PostgreSQL via Testcontainers.

Ainda não implementado ou incompleto:

- **Agenda:** o pacote existe como estrutura, mas os casos de uso e endpoints de aulas ainda não foram implementados.
- **Integração Agenda ↔ Financeiro:** o Financeiro oferece o serviço público de geração por aula, mas isso não representa a integração completa de criação/recorrência, remarcação e cancelamento de aulas com cobranças na mesma transação.
- **Alunos:** inativação (prévia e execução) e histórico de aulas dependem da integração com Agenda/Financeiro.
- **Professor:** `PATCH /professor`.
- **Página pública:** perfil, modalidades, prévia/publicação, página pública e analytics.
- **Conta:** exportação de dados do titular e encerramento de conta.
- **E-mail:** adaptador de provedor real (Resend) e templates definitivos. O padrão `noop` não envia mensagens reais.
- Outros fluxos do contrato OpenAPI que não estejam listados como implementados acima.

### Validação recente

O PR #17 registrou 39/39 testes direcionados do Financeiro e 157/157 testes da suíte backend aprovados localmente em PostgreSQL 17.11/Testcontainers. O workflow **Backend CI** também concluiu com sucesso no commit atual da `main` (`703ca21b80cbfb072fdc88939daf43d7cfe4b97f`). Isso não significa que todos os módulos do contrato estejam implementados.

A última confirmação disponível do Supabase Dev registra V1 e V2 aplicadas. A aplicação de V3–V5 no banco compartilhado ainda precisa ser verificada e coordenada antes de qualquer execução de migration.

O contrato HTTP oficial do MVP está em `docs/api/openapi.yaml`; sua existência não significa que todos os endpoints já estejam implementados. As próximas estruturas devem seguir o modelo de dados e os requisitos aprovados no Notion, sem inventar campos ou regras de negócio.
