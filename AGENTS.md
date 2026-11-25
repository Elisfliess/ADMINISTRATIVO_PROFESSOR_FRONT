# Instruções para agentes de desenvolvimento

Este arquivo é a entrada principal para qualquer agente de IA que trabalhe neste repositório. As instruções específicas de ferramentas devem apontar para cá, evitando regras duplicadas e divergentes.

## Antes de agir

1. Leia [o fluxo de desenvolvimento com IA](docs/development/AI-DEVELOPMENT-WORKFLOW.md) e [a política de autonomia](docs/development/AI-AUTONOMY-POLICY.md).
2. Leia `.ai/PROJECT_CONTEXT.md`, `.ai/DEVELOPMENT_RULES.md` e `docs/development/TECH-STACK.md`.
3. Identifique o tipo de tarefa e consulte os documentos obrigatórios da matriz abaixo antes de propor ou editar arquivos.
4. Consulte o Notion para requisitos, escopo do MVP, regras de negócio, UX e decisões funcionais vigentes.
5. Se uma fonte estiver indisponível, truncada ou em conflito, informe a limitação e peça esclarecimento antes de assumir uma regra.

## Matriz de leitura por tarefa

| Tipo de trabalho | Leitura obrigatória |
| --- | --- |
| Qualquer tarefa | Este arquivo, `.ai/PROJECT_CONTEXT.md`, `.ai/DEVELOPMENT_RULES.md`, `docs/development/TECH-STACK.md` |
| Backend / arquitetura | `.ai/ARCHITECTURE.md` e `docs/architecture/BACKEND-ARCHITECTURE.md` |
| Endpoint, DTO, status HTTP, autenticação ou contrato | `docs/api/README.md` e `docs/api/openapi.yaml` |
| Regra ou funcionalidade de módulo | `docs/specs/README.md` e SPEC específica em `docs/specs/`; confira RFs/RNs e decisões citadas no Notion |
| Sequência e dependências de implementação | `docs/development/DEVELOPMENT-ORDER.md` |
| Setup, execução ou variáveis de ambiente | `docs/SETUP.md` e `backend/README.md`; para banco, também `backend/docs/database/DATABASE-SETUP.md` |
| Migrations, schema ou persistência | `backend/docs/database/DATABASE-SETUP.md`, `backend/docs/database/INTEGRITY-RULES.md`, migrations vigentes e arquitetura |
| Frontend | `frontend/AGENTS.md`, `frontend/README.md` e contrato OpenAPI quando houver integração HTTP |
| CI ou GitHub Actions | `docs/development/CI-SETUP.md`, `.github/workflows/README.md` e workflow relacionado |
| Segurança | `docs/security/SECURITY-CHECKLIST.md`, arquitetura, SPEC de usuário e decisões relacionadas |

Não leia indiscriminadamente todos os documentos em toda tarefa; siga a matriz e amplie a leitura quando as dependências do trabalho exigirem.

## Autoridade e rastreabilidade

- O Notion é a fonte de verdade para requisitos, regras de negócio, escopo, UX e decisões funcionais.
- O GitHub é a fonte de verdade para código, testes, contrato OpenAPI versionado e documentação técnica de implementação.
- O contrato HTTP oficial está em `docs/api/openapi.yaml`; as SPECs descrevem comportamento e critérios de aceite.
- Decisões relevantes incluem D-012 (MVP), D-013 (verificação de e-mail), D-014 (sessão HTTP no MVP) e SDA-001 a SDA-009. Consulte os registros originais no Notion; não trate decisões substituídas como vigentes.
- Não transforme sugestões em decisões aprovadas. Não confunda decisão arquitetural aprovada com funcionalidade implementada.
- Ao implementar requisito, referencie os IDs e a decisão aplicável no PR.

## Desenvolvimento

- Faça mudanças pequenas, coesas e testáveis.
- Não altere arquivos fora do escopo solicitado.
- Não adicione dependências sem justificar necessidade, manutenção, licença e impacto.
- Não introduza arquitetura, padrões ou abstrações sem necessidade demonstrável.
- Nunca use dados pessoais reais em testes, exemplos ou fixtures.

## Ferramentas de build do Backend

- Sempre prefira o Maven Wrapper versionado no repositório.
- Execute comandos a partir de `backend`: `./mvnw` em Linux/macOS/Git Bash e `.\mvnw.cmd` no Windows PowerShell.
- O Backend exige JDK 21 disponível e configurado na IDE/terminal.
- Testes Testcontainers exigem Docker acessível ao Maven. Se indisponível, informe a limitação e diferencie build de validação completa.

## Segurança

- Nunca solicite, exponha ou grave tokens, senhas, chaves, cookies ou arquivos `.env` em texto puro.
- `backend/.env.enc` é intencionalmente versionado; a senha do age deve ser distribuída separadamente somente a integrantes autorizados.
- Não desative testes, validações ou controles de segurança para fazer uma alteração passar.
- Não execute migrações destrutivas nem altere dados persistentes sem autorização explícita.

## Entrega

- Execute verificações disponíveis e informe exatamente o que foi executado.
- Liste limitações, riscos e pendências.
- Não afirme que algo foi testado se não foi executado.
