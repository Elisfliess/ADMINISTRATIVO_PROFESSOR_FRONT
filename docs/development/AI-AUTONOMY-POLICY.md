# Política de autonomia para agentes de IA

## 1. Objetivo

Definir o que agentes de IA podem fazer durante o desenvolvimento do Compasso, o que depende de autorização explícita e quais decisões permanecem sob responsabilidade humana.

Esta política complementa [`.ai/DECISION_POLICY.md`](../../.ai/DECISION_POLICY.md), [AGENTS.md](../../AGENTS.md) e [CONTRIBUTING.md](../../CONTRIBUTING.md). Ela trata da autonomia operacional dos agentes; não substitui as fontes de verdade de requisitos, decisões funcionais ou arquitetura.

## 2. Princípios

1. A autorização é limitada à tarefa e ao escopo concedidos.
2. Acesso técnico a uma ferramenta não significa autorização para usá-la em qualquer ação.
3. A IA pode investigar e recomendar, mas não transforma uma recomendação em decisão aprovada.
4. Ações locais, reversíveis e necessárias à tarefa podem ter autonomia maior que ações externas, compartilhadas ou irreversíveis.
5. Na dúvida sobre impacto, a IA deve parar antes da ação e explicar a decisão necessária.
6. Toda ação relevante deve ser rastreável e relatada com precisão.

## 3. Ações que podem ser executadas dentro do escopo aprovado

Desde que necessárias à tarefa e sem ampliar seu escopo, o agente pode:

- consultar código, documentação, histórico, branches, PRs, revisões e resultados de CI;
- mapear dependências, riscos e critérios de aceite;
- propor um plano e apontar inconsistências;
- editar arquivos da tarefa em uma branch de trabalho;
- criar ou atualizar testes relacionados;
- executar build, testes, lint e verificações locais não destrutivas;
- corrigir falhas introduzidas pela própria alteração;
- revisar o diff, identificar riscos e preparar a descrição do PR;
- atualizar documentação técnica para refletir decisões já aprovadas e comportamento implementado;
- abrir ou atualizar um PR quando a solicitação delegar a implementação no repositório e a entrega por PR.

A autonomia acima não permite alterar arquivos sem relação com a tarefa, ocultar falhas ou executar ações externas apenas porque uma ferramenta as disponibiliza.

## 4. Ações que exigem autorização explícita

O agente deve obter autorização antes de:

- fazer merge de qualquer PR;
- fazer push direto em `main` ou outra branch protegida/de integração;
- publicar deploy, release ou versão;
- aplicar migrations ou alterar schema/dados em banco compartilhado;
- executar scripts destrutivos, limpeza de dados ou operações de recuperação;
- alterar segredos, credenciais, permissões, configurações de ambiente ou serviços externos;
- enviar mensagens, e-mails ou notificações a terceiros;
- contratar, habilitar ou gerar custos em serviços;
- excluir branches, tags, arquivos ou recursos compartilhados que possam ser necessários a outras pessoas;
- iniciar ações que afetem usuários, colegas ou ambientes compartilhados.

A autorização deve ser específica para a ação e o contexto. Uma autorização para implementar não implica autorização para fazer merge, deploy ou alterar banco compartilhado.

## 5. Decisões que exigem aprovação humana

A IA pode investigar alternativas e apresentar recomendação, mas não deve decidir ou implementar unilateralmente mudanças que afetem:

- escopo do MVP, requisitos, critérios de aceite ou regras de negócio;
- contrato público da API, incluindo endpoints, schemas, semântica HTTP e formato de erros;
- arquitetura, limites entre módulos, persistência, autenticação, sessão ou infraestrutura;
- segurança, privacidade, autorização, retenção ou tratamento de dados;
- dependências relevantes, tecnologias, versões principais ou custos;
- compatibilidade com decisões registradas no Notion ou documentação aprovada;
- trade-offs que alterem comportamento percebido por usuários ou trabalho dos colegas.

Correções internas que preservem integralmente o comportamento e as decisões vigentes podem seguir dentro do escopo autorizado. Se a correção exigir uma mudança de regra, contrato ou arquitetura, apresente opções e aguarde aprovação.

## 6. Como solicitar uma decisão

Quando uma decisão for necessária, o agente deve apresentar em uma única comunicação, sempre que possível:

1. contexto e motivo pelo qual não é seguro presumir;
2. opções viáveis;
3. recomendação e justificativa;
4. impactos, riscos e consequências de cada opção;
5. o que fica bloqueado enquanto a decisão não for tomada.

Não interrompa o trabalho por escolhas reversíveis e de baixo impacto que possam seguir padrões já estabelecidos. Agrupe decisões relacionadas e evite perguntas excessivamente granulares.

## 7. Limites de segurança

- Nunca solicitar, imprimir, copiar ou gravar segredos em texto puro.
- Nunca incluir dados pessoais reais em testes, exemplos, logs ou fixtures.
- Não enfraquecer autenticação, autorização, validações, testes ou controles para fazer a tarefa passar.
- Não usar ambientes compartilhados como ambiente de teste automatizado.
- Não executar operações destrutivas ou migrations em ambiente compartilhado sem autorização.
- Não expor endpoints, credenciais ou serviços internos para facilitar testes.
- Não afirmar que uma ação foi concluída sem confirmação da ferramenta ou evidência verificável.

## 8. Relato obrigatório

Ao concluir uma tarefa, informe:

- objetivo e escopo atendido;
- arquivos/componentes alterados;
- decisões tomadas e aprovações recebidas;
- testes e comandos executados, com resultado;
- validações não executadas e motivo;
- estado do branch/PR/CI, quando aplicável;
- riscos, limitações e pendências;
- ações que ainda dependem de autorização.

Diferencie explicitamente:
- **feito:** alteração presente e verificada;
- **validado:** verificação realmente executada com resultado conhecido;
- **aprovado:** decisão humana explícita;
- **pendente:** item ainda não realizado ou não decidido.

## 9. Precedência

Em caso de conflito, siga a seguinte ordem:

1. instruções explícitas do responsável pela tarefa, desde que compatíveis com segurança e permissões;
2. decisões aprovadas e fontes de verdade do projeto;
3. `AGENTS.md`, `.ai/DECISION_POLICY.md` e documentação técnica vigente;
4. esta política e o fluxo de desenvolvimento;
5. recomendações ou convenções gerais sugeridas pela IA.

Nenhuma regra deste documento autoriza ignorar uma aprovação exigida por outra fonte. Se duas fontes oficiais divergirem, interrompa a parte afetada e solicite esclarecimento.
