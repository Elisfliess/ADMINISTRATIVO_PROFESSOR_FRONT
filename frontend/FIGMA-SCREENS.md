# Telas do Compasso no frontend

Este documento registra como os frames do arquivo **PROFESSOR JOSÉ (Copy)** foram integrados ao frontend do Compasso. Os códigos dos frames continuam no código-fonte, nos nomes dos arquivos e nas rotas, mas **não aparecem na interface**: a aplicação se comporta como um produto, sem anotações de especificação, rótulos de protótipo ou referências a Figma.

## Estrutura

- `src/screens/<id-do-frame>.tsx`: um componente por frame, com o mesmo nome/código usado no Figma.
- `src/screens/definitions.ts`: conteúdo, textos, campos, tabelas e fluxos de cada tela.
- `src/screens/DynamicScreen.tsx`: renderizador compartilhado dos blocos de UI.
- `src/screens/registry.ts`: registro de navegação usado pelo roteador.
- `src/components/AppShell.tsx`: cabeçalho, navegação e rodapé, com variantes pública e administrativa.
- `src/theme/`: tokens A11 claros/escuros e persistência da escolha no navegador.
- `src/router/hashRouter.ts`: navegação por hash, sem dependência de React Router.
- `src/data/mock.ts`: dados exclusivamente fictícios para demonstração.
- `src/services/api/endpoints.ts`: paths oficiais do `docs/api/openapi.yaml`.

Os 92 frames do Figma (46 claros + 46 escuros) são representados por **46 componentes**, não por duplicatas. A variante escura é aplicada pelos tokens de tema em todas as telas.

## Navegação

A aplicação abre em `/`, que exibe o login. Antes do aceite dos documentos, as telas usam um cabeçalho público com links de entrada e cadastro. Depois do aceite, o cabeçalho passa a oferecer a navegação administrativa:

- **Agenda** → `/#/P21-agenda`
- **Alunos** → `/#/P11-lista-de-alunos`
- **Cobranças** → `/#/P15-aba-cobrancas`
- **Página pública** → `/#/P33-editor-da-pagina-publica`
- **Indicadores** → `/#/P42-dashboard-da-pagina-publica`
- **Configurações** → `/#/P43-configuracoes-de-conta`

O botão **Sair** leva a `/#/P10-sair`. O rodapé traz Política de Privacidade e Termos de Uso. Rotas desconhecidas abrem a página 404 (`V03`). O alternador de tema fica no cabeçalho e também na tela `P45-tema-claro-e-escuro`.

Os botões internos e as abas usam os mesmos IDs dos frames, por exemplo `/#/P13-ficha-do-aluno`, `/#/P40-previa-simples` e `/#/V01-pagina-publica-do-professor`.

## Frames implementados

`G04`, `P01`, `P02`, `P03`, `P04`, `P05`, `P06`, `P07`, `P08`, `P10`, `P11`, `P12`, `P13`, `P14`, `P15`, `P16`, `P17`, `P18`, `P19`, `P21`, `P22`, `P23`, `P25`, `P26`, `P27`, `P28`, `P29`, `P30`, `P32`, `P33`, `P34`, `P35`, `P36`, `P40`, `P41`, `P42`, `P43`, `P45`, `P46`, `P48`, `P49`, `P50`, `V01`, `V02`, `V03`, `V04`.

## Fluxos críticos cobertos

1. Login (`P01`) → aceite legal (`P08`) → área administrativa (`P11`), com confirmação explícita.
2. Cadastro (`P02`) → verificação de e-mail (`P03`) → confirmação (`P04`) → login (`P01`).
3. Recuperação (`P05`) → e-mail enviado (`P06`) → redefinição (`P07`).
4. Gestão de alunos (`P11`–`P19`) e financeiro (`P15`, `P30`, `P32`, `P49`, `P50`).
5. Edição da página pública (`P33`/`P34`/`P35`/`P36`) → prévia (`P40`) → aprovação (`P41`) → indicadores (`P42`).
6. Cancelamento e inativação com prévia e confirmação (`P17`, `P19`, `P26`, `P41`, `P46`, `P50`).
7. Tema claro/escuro aplicado globalmente (`P45` e alternador do cabeçalho).

## Integração HTTP

A integração é ativada por `VITE_API_BASE_URL` (veja `.env.example`). Sem essa variável, as telas usam dados fictícios e navegação local; com ela, `src/services/api/integration.ts` liga os formulários e confirmações aos endpoints implementados no backend: login/sessão, cadastro e verificação, recuperação de senha, aceite legal, saída, alunos, listagem de cobranças, baixa, cancelamento e exportação de recebimentos.

O cliente `src/services/api/client.ts` envia `credentials: 'include'`, obtém o cookie `XSRF-TOKEN` com uma leitura prévia em `GET /auth/me`, reenvia o header `X-XSRF-TOKEN` nas operações mutáveis e interpreta erros RFC 9457. A validação no navegador é apenas apoio de UX; a regra de negócio continua no backend.

Fluxos cujos endpoints ainda não existem no backend permanecem locais: agenda, publicação da página pública, indicadores, inativação/anonimização de alunos, exportação/encerramento de conta e edição de dados do professor. Nenhum caminho fora do contrato `docs/api/openapi.yaml` foi adicionado.

Não foi adicionado JWT, MFA, pagamento online, upload de mídia, rascunho persistido de publicação nem criação avulsa de cobrança.

## Validação executada

- `npm run typecheck`
- `npm run lint`
- `npm run test:run`
- `npm run build`
- `npm run format:check`
- `npm run test:e2e`

A validação visual pixel-perfect contra o Figma não foi automatizada; a interface segue os IDs dos frames, os tokens A11 e os requisitos das SPECs/OpenAPI disponíveis no repositório.
