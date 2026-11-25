# Como contribuir

## Fluxo de trabalho

Adotamos GitHub Flow:
1. Atualize sua referência local de `main`.
2. Crie uma branch curta a partir de `main`.
3. Faça alterações pequenas e relacionadas.
4. Execute as verificações disponíveis.
5. Abra um Pull Request para `main`.
6. Aguarde revisão humana e CI verde.
7. Faça merge pelo GitHub e exclua a branch temporária.

## Nomes de branches

```text
feature/<descricao-curta>
fix/<descricao-curta>
hotfix/<descricao-curta>
docs/<descricao-curta>
refactor/<descricao-curta>
test/<descricao-curta>
chore/<descricao-curta>
```

Use kebab-case, sem nomes pessoais e sem acentos.

## Commits

Adote Conventional Commits:

```text
feat: add student registration
fix: prevent duplicate appointments
docs: describe local setup
test: cover appointment scheduling
refactor: simplify lesson service
chore: update dependencies
```

## Pull Requests
- Um PR deve resolver um objetivo principal.
- Descreva contexto, solução, como validar e riscos.
- Referencie requisitos/decisões do Notion quando aplicável.
- Inclua evidências visuais para mudanças relevantes de interface.
- Não solicite merge com CI falhando.
- Resolva ou responda todos os comentários de revisão.

## Revisão
Enquanto o plano GitHub Free não permitir proteção de branch neste repositório privado, estes controles são operacionais e dependem da colaboração da equipe. Não faça push direto em `main`; exija revisão de pelo menos outro integrante sempre que possível.
