# Configuração do banco de desenvolvimento

## Ambiente padrão

O ambiente padrão do Backend é o projeto Supabase dedicado ao desenvolvimento da equipe. Ele contém PostgreSQL convencional e é acessado pela aplicação Spring Boot via JDBC.

O projeto de desenvolvimento deve ser separado de qualquer futuro banco de produção. Utilize exclusivamente dados fictícios.

## Obter os dados de conexão

Solicite à pessoa responsável pelo Supabase de desenvolvimento:

- Host e porta da conexão aprovada.
- Nome do banco.
- Nome do usuário correspondente ao método de conexão.
- Senha de runtime, distribuída por canal privado.
- Confirmação de que a conexão JDBC é permitida na rede utilizada.
- Versão principal do PostgreSQL do projeto.

O Supabase oferece conexão direta e poolers. A conexão direta é preferível quando a rede suporta IPv6. Em redes IPv4 ou restritas, use o Session Pooler quando indicado pelo painel do projeto.

Para a aplicação Java, a URL precisa ser JDBC:

```properties
DB_URL=jdbc:postgresql://HOST:PORTA/postgres?sslmode=require
DB_USERNAME=USUARIO_DE_RUNTIME
DB_PASSWORD=SENHA_DE_RUNTIME
```

Substitua os valores pelos dados exatos do painel. No Session Pooler, o formato do nome de usuário pode ser diferente do usuário da conexão direta. Não adivinhe esses valores.

Não use Transaction Pooler para executar migrations Flyway que dependam de comportamento de sessão.

## Configurar o Backend

O arquivo `backend/.env.enc` é versionado no Git e contém as variáveis de desenvolvimento criptografadas com `age`. A senha é compartilhada separadamente com integrantes autorizados. Consulte o README do Backend para os passos completos de geração, atualização e inicialização com os scripts PowerShell 7.

O script de inicialização descriptografa o arquivo em memória, define as variáveis no processo e inicia o Maven Wrapper sem criar `.env` em texto puro. Para configuração individual manual, copie `.env.example` para `.env` na pasta `backend`, preencha as variáveis e inicie a aplicação pela IDE ou pelo Maven Wrapper.

O Spring Boot carrega o arquivo `.env` local quando presente. Variáveis do ambiente da máquina/IDE têm precedência sobre os valores do arquivo local. O `.env` em texto puro é ignorado pelo Git e nunca deve ser enviado em commits. O `backend/.env.enc` é a exceção intencional: deve ser versionado.

**Build e execução:** use sempre o Maven Wrapper versionado em `backend/mvnw` e `backend/mvnw.cmd`, a partir da pasta `backend`. Não é necessário Maven global. O Wrapper baixa a distribuição Maven fixada no repositório, mas exige JDK 21 instalado e configurado.

## Migrations e banco compartilhado

A migration inicial está em `src/main/resources/db/migration/V1__create_initial_schema.sql`. Flyway é a fonte de verdade do schema; `schema-reference.sql` é apenas uma cópia de consulta para quem configurar PostgreSQL local.

A `V4__allow_alphanumeric_cnpj.sql` apenas substitui a constraint de formato de CNPJ para aceitar o padrão alfanumérico oficial (`^[0-9A-Z]{12}[0-9]{2}$`). A coluna `CHAR(14)` já comporta letras e não há reescrita de dados; CNPJs numéricos existentes continuam válidos. A conferência dos dígitos verificadores permanece na aplicação. Como qualquer migration, sua execução no banco compartilhado exige coordenação e aprovação prévias.

Por padrão, Flyway fica desativado ao iniciar a API. Isso evita que qualquer integrante altere o schema compartilhado sem coordenação.

A pessoa autorizada a aplicar migrations deve receber credenciais específicas de DDL, preencher localmente `FLYWAY_USERNAME` e `FLYWAY_PASSWORD` e ativar o perfil `migration` apenas durante a aplicação.

Execute os comandos a partir da pasta `backend`.

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

O perfil `migration` usa uma conexão própria do Flyway com as credenciais `FLYWAY_*`. A API mantém `DB_USERNAME` e `DB_PASSWORD` para runtime. Depois da execução, confira a tabela `flyway_schema_history` e não mantenha o perfil `migration` ativado para o uso diário.

A V1 concede ao papel `compasso_runtime` operações `SELECT`, `INSERT`, `UPDATE` e `DELETE` nas tabelas da aplicação. A V2 remove especificamente a permissão `DELETE` de `plano_pagamento` e `pagamento`, preservando planos encerrados e cobranças canceladas como registros históricos. O runtime mantém leitura da view `vw_pagamento_situacao`. A V1 também configura privilégios padrão para tabelas futuras criadas pelo papel que executa as migrations. O runtime não recebe `CREATE` no schema `public` nem acesso à tabela interna `flyway_schema_history`. Os papéis `compasso_runtime` e `compasso_migrator` precisam existir antes da execução; sua criação e as credenciais são responsabilidade da configuração administrativa do ambiente, não da migration.

Antes de aplicar uma migration no banco compartilhado:

- Confirme que a versão do PostgreSQL do Supabase está coberta pela CI atual (PostgreSQL 17.11). A validação de PostgreSQL 16 não faz parte da matriz atual; não presuma compatibilidade testada nessa versão.
- Revise o SQL e confira sua aderência à A14.
- Coordene a aplicação com a equipe.
- Confirme que a migration não apaga ou altera dados de forma destrutiva.
- Não altere arquivos de migration já aplicados; crie uma nova migration corretiva.
- Não execute testes que limpem ou recriem tabelas no Supabase compartilhado.

## Problemas comuns

- **Falha de autenticação:** confira usuário e senha, incluindo o formato de usuário exigido pelo pooler.
- **Falha de conexão/timeout:** confira host, porta, conectividade da rede e método de conexão.
- **Erro de SSL:** mantenha `sslmode=require` na URL JDBC quando exigido pelo Supabase.
- **Variável ausente:** confirme que o arquivo se chama exatamente `.env`, está na pasta `backend` e contém as variáveis obrigatórias.
- **Erro de schema:** não tente resolver criando tabelas manualmente; confira o estado das migrations e peça revisão.

## Segurança

Nunca:
- versione o arquivo `.env` em texto puro;
- compartilhe senha ou connection string em canais públicos;
- use dados pessoais reais;
- utilize credenciais de produção no ambiente de desenvolvimento;
- utilize credenciais administrativas como credenciais permanentes do Backend.
