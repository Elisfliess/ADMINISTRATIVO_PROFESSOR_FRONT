# PostgreSQL local (opcional)

O uso de PostgreSQL local é uma alternativa individual. Não é necessário para desenvolver o Backend: o ambiente padrão da equipe é o Supabase de desenvolvimento.

## Requisitos

- Uma instalação PostgreSQL compatível com a versão principal adotada pelo projeto.
- Um banco vazio criado localmente.
- Acesso administrativo ao PostgreSQL local para provisionar os papéis iniciais.
- Credenciais locais próprias para runtime e migration.
- JDK 21 instalado e configurado. Não é necessário instalar Maven globalmente: use o Maven Wrapper versionado no repositório.

## Criar o banco e os papéis

Crie um banco local chamado `compasso` (ou ajuste o nome na URL JDBC). Antes de executar a primeiras migrations, os papéis `compasso_runtime` e `compasso_migrator` precisam existir.

Conectado ao PostgreSQL local com um usuário administrador, crie os papéis com senhas locais próprias:

```sql
CREATE ROLE compasso_runtime LOGIN PASSWORD 'SENHA_LOCAL_RUNTIME';
CREATE ROLE compasso_migrator LOGIN PASSWORD 'SENHA_LOCAL_MIGRATOR';

GRANT CONNECT ON DATABASE compasso
    TO compasso_runtime, compasso_migrator;
```

Depois, conecte-se ao banco `compasso` como administrador e conceda ao papel de migration permissão para criar os objetos no schema `public`, além de permitir que o runtime utilize o schema:

```sql
GRANT USAGE ON SCHEMA public TO compasso_runtime;
GRANT USAGE, CREATE ON SCHEMA public TO compasso_migrator;
```

Esses comandos são apenas para provisionar os papéis e permissões do PostgreSQL local. Não os execute no Supabase compartilhado como substituto do procedimento administrativo documentado em DATABASE-SETUP.md. Não conceda `CREATE` ao papel de runtime.

## Configuração

Crie um arquivo `.env` na pasta `backend` com os dados locais:

```properties
DB_URL=jdbc:postgresql://localhost:5432/compasso
DB_USERNAME=compasso_runtime
DB_PASSWORD=SENHA_LOCAL_RUNTIME

FLYWAY_USERNAME=compasso_migrator
FLYWAY_PASSWORD=SENHA_LOCAL_MIGRATOR
FLYWAY_ENABLED=false
```

Substitua as senhas de exemplo pelas senhas locais definidas ao criar os papéis. Não reutilize credenciais do Supabase ou de produção.

## Executar a primeira migration

Em banco local individual e vazio, execute a aplicação uma vez com o perfil `migration`. Esse perfil ativa o Flyway com as credenciais separadas de migration, enquanto a conexão principal da aplicação continua usando as credenciais de runtime.

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

Após a execução bem-sucedida, confira se a tabela `flyway_schema_history` foi criada e se as migrations V1 e V2 constam como aplicadas. Encerre a aplicação e mantenha `FLYWAY_ENABLED=false` no uso diário. Não deixe o perfil `migration` ativado para iniciar a API normalmente.

## Observações

- O Maven Wrapper usa a versão de Maven fixada em `backend/.mvn/wrapper/maven-wrapper.properties`; não é necessário instalar Maven globalmente.
- O Wrapper não instala Java: JDK 21 continua sendo requisito e precisa estar configurado na IDE/terminal.
- A versão principal do PostgreSQL local deve corresponder à versão definida para o projeto.
- Não crie ou altere o schema manualmente; use migrations versionadas.
- Não use dados reais.
- Se houver divergência de comportamento entre o PostgreSQL local e o Supabase, reproduza o problema na versão oficial de PostgreSQL usada pelo projeto.
- Não execute testes automatizados destrutivos no banco compartilhado; os testes de integração utilizam bancos efêmeros via Testcontainers.
