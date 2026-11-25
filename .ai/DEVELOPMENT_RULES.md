# Regras de desenvolvimento

## Princípios
1. Simplicidade adequada ao escopo acadêmico; evitar overengineering.
2. Separação clara de responsabilidades, sem camadas artificiais.
3. Código legível, nomes explícitos e funções/classes com responsabilidade coesa.
4. Validar entradas na fronteira do sistema.
5. Tratar erros de forma previsível, sem vazar detalhes internos ou dados sensíveis.
6. Considerar acessibilidade, responsividade e privacidade desde o início.

## Qualidade
- Testar regras de negócio e fluxos críticos.
- Preferir testes determinísticos e independentes de serviços externos.
- Testes de integração devem usar ambiente isolado e dados fictícios.
- Não reduzir cobertura ou remover testes sem justificativa documentada.
- Não ignorar avisos de compilação, lint ou segurança sem explicar.

## Ferramentas de build do Backend
- Usar sempre o Maven Wrapper versionado em `backend/mvnw` e `backend/mvnw.cmd`; não exigir Maven instalado globalmente.
- Executar comandos a partir da pasta `backend`: `./mvnw` em Linux/macOS/Git Bash e `.\mvnw.cmd` no Windows PowerShell.
- O Wrapper baixa a distribuição Maven definida em `backend/.mvn/wrapper/maven-wrapper.properties` na primeira execução.
- O Wrapper não instala o JDK. É necessário ter JDK 21 instalado e configurado na IDE/terminal.
- Testes de integração utilizam Testcontainers e exigem Docker acessível ao processo que executa Maven. Se Docker não estiver disponível, registrar essa limitação; não apresentar build sem testes como validação completa.

## Dependências
Antes de adicionar uma dependência, avaliar se a funcionalidade já existe na plataforma ou stack. Registrar a justificativa quando o impacto for relevante.

## Banco de dados
- Mudanças de schema devem ser versionadas por migrações.
- Validar persistência com PostgreSQL via Testcontainers; a CI atual utiliza PostgreSQL 17.11. Não declarar compatibilidade testada com outra versão sem executar validação específica.
- Não editar schema de produção manualmente.
- Não incluir dumps com dados reais no Git.
- O schema inicial já possui migrations Flyway V1 e V2 aplicadas no Supabase Dev; novas alterações devem ser migrations incrementais, sem editar migrations aplicadas.
- Para contrato HTTP, consultar `docs/api/openapi.yaml`; para comportamento, consultar a SPEC do módulo em `docs/specs/`.

## Commits e branches
Seguir Conventional Commits e os padrões descritos em `CONTRIBUTING.md`.
