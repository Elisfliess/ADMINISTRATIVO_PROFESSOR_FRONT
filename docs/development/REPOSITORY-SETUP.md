# Configuração inicial do repositório

## GitHub
- Nome: `compasso`
- Visibilidade: Private
- Modelo: monorepo
- Branch padrão: `main`
- Estratégia: GitHub Flow
- Plano de proteção: GitHub Free; controles operacionais até que branch protection esteja disponível.

## Configurações manuais após criar o repositório
1. Confirmar `main` como default branch.
2. Adicionar os três integrantes com o menor nível de acesso necessário.
3. Habilitar Issues e Pull Requests.
4. Habilitar Dependabot security updates/alerts, se disponíveis.
5. Habilitar secret scanning/push protection, se disponíveis no plano.
6. Desabilitar merge commit se a equipe optar por manter somente squash merge.
7. Configurar descrição e tópicos do repositório.
8. Conferir que nenhum segredo foi inserido no primeiro commit.

## Limitação importante
Este pacote não configura regras de branch no GitHub. Em repositório privado no plano Free, não conte com bloqueio técnico de push/merge. A equipe deve exigir PR, revisão e CI como procedimento.
