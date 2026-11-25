# Setup local — Compasso

Este guia prepara o **backend** para execução pelo terminal, inclusive em computadores Windows da faculdade sem privilégios de administrador. Os comandos Windows usam PowerShell. Salvo indicação contrária, execute-os na raiz do repositório.

## 1. Pré-requisitos e versões

| Ferramenta | Versão / requisito | Obrigatória? |
|---|---|---|
| JDK | **Java 21** | Sim |
| Maven | Maven **3.9.9**, obtido pelo Maven Wrapper 3.3.4 | Não instalar globalmente |
| Git | Versão compatível com o sistema | Para clonar/atualizar |
| Docker Engine | Docker Desktop ou equivalente compatível | Apenas para testes de integração |
| PowerShell | **7+** (`pwsh`) | Para os scripts de ambiente criptografado |
| age | Versão disponível para o sistema | Para usar `backend/.env.enc` |

A versão do Java está definida em `backend/pom.xml` (`java.version=21`). A versão do Wrapper e a distribuição Maven estão fixadas em `backend/.mvn/wrapper/maven-wrapper.properties`: Wrapper 3.3.4 e Maven 3.9.9.

**Não é necessário instalar Maven globalmente.** O Wrapper baixa e utiliza a versão fixada pelo projeto:

- `mvn ...`: executa um Maven instalado globalmente e disponível no `PATH`.
- `./mvnw ...`: executa o Wrapper no Linux/macOS.
- `.\mvnw.cmd ...`: executa o Wrapper no Windows PowerShell. O arquivo versionado no Windows é `mvnw.cmd`; no Git Bash, pode-se usar `./mvnw`.

O Wrapper **não instala Java**. O JDK 21 precisa estar instalado e acessível no terminal. No Windows, o script `mvnw.cmd` exige que `JAVA_HOME` esteja definido e aponte para a raiz do JDK. Na primeira execução, o Wrapper também precisa baixar Maven; redes restritas podem bloquear esse download.

### Verificar ferramentas

No PowerShell:

```powershell
java -version
javac -version
.\backend\mvnw.cmd -version
pwsh --version
age --version
docker version
docker ps
```

`pwsh` e `age` são necessários para o fluxo de ambiente criptografado. Docker é necessário para os testes de integração. `docker version` deve mostrar também a seção **Server**; ter apenas o cliente instalado não significa que o Docker Engine esteja ativo.

## 2. Windows sem acesso de administrador

Não é necessário instalar ferramentas em `Program Files` nem alterar variáveis do sistema. Prefira instalações no perfil do próprio usuário.

### 2.1. Obter um JDK 21 sem administrador

Se o IntelliJ IDEA já estiver instalado e tiver baixado um JDK pelo gerenciador de SDKs, é possível reutilizá-lo. O IntelliJ costuma armazenar esses JDKs em:

```text
%USERPROFILE%\.jdks\
```

Localize a pasta correspondente ao JDK 21. `JAVA_HOME` deve apontar para a raiz do JDK (a pasta que contém `bin\java.exe`), não para `bin`.

Se não houver JDK 21 disponível, baixe um pacote ZIP de JDK 21 (por exemplo, uma distribuição Temurin), extraia-o em uma pasta do usuário, como `%USERPROFILE%\tools\jdk-21\`, e use essa pasta como `JAVA_HOME`. Extrair um ZIP não exige instalação administrativa.

### 2.2. Configuração permanente no perfil do usuário

Para abrir diretamente a tela de variáveis de ambiente do usuário, execute:

```powershell
rundll32.exe sysdm.cpl,EditEnvironmentVariables
```

Na seção **Variáveis de usuário**, configure:

- `JAVA_HOME`: caminho raiz do JDK 21, por exemplo `C:\Users\SEU_USUARIO\.jdks\jdk-21`.
- `MAVEN_HOME` (opcional): caminho da distribuição Maven embutida no IntelliJ, por exemplo `C:\Program Files\JetBrains\IntelliJ IDEA\plugins\maven\lib\maven3`.

O caminho do Maven pode variar conforme edição, versão e local de instalação do IntelliJ. Localize a pasta `plugins\maven\lib\maven3` dentro da instalação efetiva da IDE. **MAVEN_HOME é opcional e não é usado pelo Maven Wrapper**; serve apenas se quiser executar `mvn` globalmente com essa distribuição.

Edite o `Path` das **variáveis de usuário** e adicione os caminhos completos:

- `C:\Users\SEU_USUARIO\.jdks\jdk-21\bin`
- `C:\Program Files\JetBrains\IntelliJ IDEA\plugins\maven\lib\maven3\bin` (somente se configurou Maven global)

Substitua os exemplos pelos caminhos reais. Prefira inserir caminhos absolutos diretamente no `Path`, sem depender de entradas aninhadas como `%JAVA_HOME%\bin` ou `%MAVEN_HOME%\bin`. Não apague entradas existentes. Salve e abra um **novo terminal**.

Valide:

```powershell
$env:JAVA_HOME
java -version
javac -version
```

Se configurou Maven global:

```powershell
$env:MAVEN_HOME
mvn -version
```

### 2.3. Configuração temporária por sessão

Ideal em computadores que resetam as configurações ao reiniciar. Abra PowerShell e ajuste os caminhos abaixo:

```powershell
$jdk = 'C:\Users\SEU_USUARIO\.jdks\jdk-21'
$maven = 'C:\Program Files\JetBrains\IntelliJ IDEA\plugins\maven\lib\maven3'

$env:JAVA_HOME = $jdk
$env:MAVEN_HOME = $maven
$env:Path = "$jdk\bin;$maven\bin;$env:Path"

java -version
javac -version
```

Se não precisar de Maven global, remova as linhas de `$maven`, `MAVEN_HOME` e a entrada `$maven\bin` do `Path`. O Maven Wrapper continuará funcionando.

As alterações feitas com `$env:` duram somente enquanto aquela sessão do PowerShell estiver aberta. Em um novo terminal, repita o bloco.

### 2.4. Ferramentas portáteis e escopo do usuário

- **JDK:** use o ZIP e extraia-o em uma pasta do perfil, como mostrado acima.
- **age:** baixe o arquivo compactado da página oficial de releases, extraia o executável em uma pasta do usuário (por exemplo, `%USERPROFILE%\tools\age\`) e adicione essa pasta ao `Path` do usuário ou temporariamente ao `$env:Path`.
- **WinGet:** quando disponível e permitido pela política da instituição, procure o identificador com `winget search <nome>` e instale com `winget install --id <ID_DO_PACOTE> --scope user`. A disponibilidade do escopo de usuário depende do pacote. Se WinGet estiver bloqueado, prefira uma distribuição ZIP portátil.
- **Docker:** exige um Docker Engine ativo e permissão para acessá-lo. Em computadores gerenciados pela faculdade, consulte o suporte de TI; não tente contornar as políticas da máquina. Sem Docker, ainda é possível compilar/empacotar sem executar testes, conforme a seção 4.

## 3. Variáveis de ambiente e profiles do Spring Boot

### 3.1. Variáveis esperadas

A configuração está em `backend/src/main/resources/application.properties`, `application-migration.properties` e `backend/.env.example`.

| Variável | Obrigatória? | Uso |
|---|---|---|
| `DB_URL` | **Sim** | URL JDBC do PostgreSQL da aplicação |
| `DB_USERNAME` | **Sim** | Usuário PostgreSQL de runtime |
| `DB_PASSWORD` | **Sim** | Senha do usuário de runtime |
| `FLYWAY_USERNAME` | Somente no profile `migration` | Usuário separado para migrations |
| `FLYWAY_PASSWORD` | Somente no profile `migration` | Senha do usuário de migrations |
| `FLYWAY_ENABLED` | Não | Ativação do Flyway na configuração padrão; o padrão é `false` |
| `PORT` | Não | Porta HTTP; padrão `8080` |

Exemplo de URL:

```properties
DB_URL=jdbc:postgresql://HOST:PORTA/postgres?sslmode=require
DB_USERNAME=USUARIO_DE_RUNTIME
DB_PASSWORD=SENHA_DE_RUNTIME
```

Não use uma URL HTTP do painel do Supabase. O valor precisa começar com `jdbc:postgresql://`; uma URL ausente ou incorreta causa falhas de inicialização, incluindo mensagens de que a URL deve começar com `jdbc`.

As credenciais `FLYWAY_*` são distintas das credenciais `DB_*`. Obtenha os dados autorizados com a pessoa responsável pelo ambiente. Não publique senhas ou connection strings.

### 3.2. Criar e carregar o arquivo .env

O projeto já contém:

```properties
spring.config.import=optional:file:.env[.properties]
```

O Spring Boot carrega `.env` do **diretório de trabalho atual** quando o arquivo existe. Execute os comandos dentro da pasta `backend`.

No PowerShell, a partir da raiz do repositório:

```powershell
Copy-Item .\backend\.env.example .\backend\.env
notepad .\backend\.env
Set-Location .\backend
.\mvnw.cmd spring-boot:run
```

Preencha os valores reais antes de iniciar. `backend/.env` é local, ignorado pelo Git e não deve ser commitado nem compartilhado. `backend/.env.example` contém apenas placeholders.

No Linux/macOS:

```bash
cp backend/.env.example backend/.env
# Edite backend/.env e preencha as credenciais autorizadas.
cd backend
./mvnw spring-boot:run
```

Não é necessário exportar manualmente as variáveis: o Spring Boot importa o arquivo. Variáveis já presentes no ambiente do processo (por exemplo, definidas no terminal) têm precedência sobre os valores importados.

**IntelliJ e terminal são ambientes separados:** variáveis configuradas em *Run/Debug Configurations > Environment variables* pertencem ao processo iniciado pela IDE e não são automaticamente herdadas por um terminal externo. Para executar pelo terminal, use o `.env` acima ou defina as variáveis naquela sessão.

### 3.3. Usar o ambiente criptografado da equipe

O repositório também distribui `backend/.env.enc`, criptografado com `age`. A senha é compartilhada separadamente com integrantes autorizados. Esse fluxo evita criar um `.env` em texto puro.

Com PowerShell 7 e `age` disponíveis, execute a partir da raiz:

```powershell
pwsh -NoProfile -File .\backend\scripts\run-with-encrypted-env.ps1
```

Digite a senha quando solicitado. O script descriptografa em memória, valida `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, e inicia o backend pelo Maven Wrapper. Ele exige PowerShell 7+; Windows PowerShell 5.1 não é compatível. Consulte o [README do backend](../backend/README.md) para detalhes de atualização e limites de segurança.

### 3.4. Profiles do Spring Boot

A execução diária usa o profile padrão, sem ativar migrations automaticamente. Para uma migration aprovada, somente a pessoa autorizada deve ativar o profile `migration`, que habilita Flyway e usa `FLYWAY_USERNAME` e `FLYWAY_PASSWORD`.

No Windows PowerShell, a partir da pasta `backend`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=migration"
```

No Linux/macOS:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=migration
```

As variáveis `FLYWAY_USERNAME` e `FLYWAY_PASSWORD` precisam estar disponíveis no ambiente ou no `.env`. Não mantenha o profile `migration` ativo no uso diário nem execute migrations no banco compartilhado sem coordenação e autorização. Consulte [Configuração do banco de desenvolvimento](../backend/docs/database/DATABASE-SETUP.md).

## 4. Testes e troubleshooting

### 4.1. Testes com Testcontainers

Os testes de integração utilizam PostgreSQL efêmero via Testcontainers. O **Docker Engine precisa estar instalado, iniciado e acessível** antes dos testes. Docker Desktop (ou alternativa compatível) deve estar em execução.

A partir da pasta `backend`:

```powershell
docker ps
.\mvnw.cmd test
```

Se a máquina não tiver Docker disponível e você precisar apenas compilar e empacotar, use:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Esse comando pula a execução dos testes. **Não substitui** a validação dos testes de integração nem a CI. Não use o banco compartilhado como alternativa aos containers de teste.

Linux/macOS:

```bash
docker ps
./mvnw test
./mvnw clean package -DskipTests
```

### 4.2. Checklist antes de iniciar

Execute a partir da raiz:

```powershell
java -version
javac -version
Set-Location .\backend
.\mvnw.cmd -version
Test-Path .env
Test-Path .env.enc
docker ps
```

- `java -version` e `javac -version` devem indicar Java 21.
- `.\mvnw.cmd -version` deve indicar Maven 3.9.9 e Java 21.
- Deve existir uma forma válida de fornecer as variáveis: `.env` local ou `.env.enc` com `pwsh` e `age`.
- `docker ps` precisa funcionar para os testes; não é requisito para iniciar a API normalmente com o Supabase.

### 4.3. Problemas comuns

| Sintoma | Verificação / solução |
|---|---|
| `JAVA_HOME` ausente ou inválido | Aponte para a raiz do JDK 21, não para `bin`. Abra um terminal novo após alterar variáveis permanentes. |
| `java` não é reconhecido | Confira se o diretório `bin` do JDK está no `Path` do usuário ou na sessão atual. |
| Maven/Wrapper não inicia | Use `.\mvnw.cmd` no PowerShell e execute dentro de `backend`. O Wrapper exige JDK 21 e acesso à internet na primeira execução. |
| `'url' must start with "jdbc"` ou erro de datasource | Confira `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`; a URL deve começar com `jdbc:postgresql://`. Confira também a localização do `.env` e o diretório atual. |
| Falha de autenticação/conexão PostgreSQL | Confira host, porta, SSL, usuário e senha do método de conexão Supabase escolhido. |
| Testcontainers não encontra Docker | Inicie Docker Desktop/Engine e valide com `docker ps`. Sem acesso ao Engine, use `-DskipTests` apenas para compilar/empacotar e execute os testes depois em ambiente compatível. |
| Script criptografado informa PowerShell incompatível | Execute com `pwsh` (PowerShell 7+), não com `powershell.exe` (Windows PowerShell 5.1). |
| `age` não é reconhecido | Confira se o executável está no `Path` da sessão/usuário e abra um terminal novo após alterar o Path. |

## Referências

- [README do backend](../backend/README.md)
- [Configuração do banco de desenvolvimento](../backend/docs/database/DATABASE-SETUP.md)
- [PostgreSQL local opcional](../backend/docs/database/LOCAL-DATABASE.md)
- [Stack tecnológica](development/TECH-STACK.md)
