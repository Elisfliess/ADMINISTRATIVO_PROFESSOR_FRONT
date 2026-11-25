# Fluxo de desenvolvimento com apoio de IA

## 1. Objetivo

Estabelecer um processo repetível para que integrantes e agentes de IA possam desenvolver o Compasso com autonomia operacional, qualidade verificável e respeito às decisões do projeto.

O processo deve facilitar a colaboração, não criar burocracia. A profundidade das etapas e das validações deve ser proporcional ao risco e ao tipo de alteração.

## 2. Fontes de referência

Antes de trabalhar, identifique as fontes aplicáveis:

- **Notion:** requisitos, regras de negócio, escopo do MVP, UX e decisões funcionais.
- **Repositório:** código, testes, arquitetura técnica, instruções de desenvolvimento, SPECs técnicas e contrato OpenAPI.
- **GitHub:** estado real das branches, commits, PRs, revisões e CI.

Siga a matriz de leitura em [AGENTS.md](../../AGENTS.md). Não leia indiscriminadamente toda a documentação; consulte os documentos obrigatórios para o tipo de tarefa e amplie a leitura quando as dependências exigirem.

Em caso de conflito, lacuna ou fonte indisponível:
1. registre exatamente o que está em conflito ou faltando;
2. não escolha silenciosamente uma interpretação;
3. interrompa somente a parte que depende dessa definição;
4. solicite uma decisão humana quando ela puder alterar comportamento, contrato, arquitetura, segurança ou escopo.

Sugestões de IA não são decisões aprovadas. Diferencie sempre o que foi decidido, o que está implementado e o que é apenas uma proposta.

## 3. Etapas do fluxo

### 3.1. Entender a solicitação e o estado atual

Antes de editar:

1. Reescreva o objetivo em termos verificáveis.
2. Identifique módulo, camadas, arquivos e contratos potencialmente afetados.
3. Consulte a branch de destino e o estado atual do código; não presuma que o contexto de uma conversa ou execução anterior continua atualizado.
4. Leia requisitos, SPECs, decisões e documentação técnica aplicáveis.
5. Verifique alterações locais, commits e PRs existentes quando tiver acesso ao ambiente.
6. Identifique dependências, riscos, critérios de aceite e validações necessárias.

Se o pedido for amplo, divida-o em entregas coesas. Não amplie o escopo para corrigir problemas não relacionados sem comunicar e obter autorização.

### 3.2. Planejar

Apresente um plano proporcional à tarefa, contendo:

- resultado esperado e limites do escopo;
- arquivos ou componentes que provavelmente serão alterados;
- regras e decisões que precisam ser preservadas;
- riscos técnicos e de regressão;
- testes e verificações que demonstrarão o resultado;
- decisões pendentes, se houver.

Para tarefas pequenas e sem ambiguidade, o plano pode ser curto. Agrupe dúvidas relacionadas e pergunte apenas quando a resposta for necessária para avançar com segurança.

### 3.3. Implementar incrementalmente

- Faça mudanças pequenas, coesas e alinhadas ao escopo.
- Preserve padrões existentes antes de introduzir novos.
- Evite abstrações, dependências e infraestrutura sem necessidade demonstrável.
- Não altere contratos, regras, arquitetura ou requisitos por conveniência.
- Atualize testes junto com a implementação.
- Mantenha documentação e contrato coerentes com o comportamento aprovado.
- Não remova ou enfraqueça testes, validações ou controles para obter uma execução verde.

Quando surgir um problema fora do escopo, registre-o como observação ou pendência e não o incorpore automaticamente à entrega.

### 3.4. Testar e revisar durante a implementação

Execute verificações em ciclos curtos. Ao corrigir um defeito, crie um teste que represente o comportamento incorreto sempre que tecnicamente viável.

Para correções de bugs, o teste de regressão deve demonstrar, preferencialmente:
1. que o cenário reproduz a falha na implementação anterior;
2. que o teste falha antes da correção pelo motivo esperado;
3. que o teste passa após a correção;
4. que a suíte relacionada continua passando.

Se não for possível executar o teste contra a implementação anterior — por exemplo, por custo, risco ou dificuldade técnica — explique a limitação e demonstre por que o teste cobre o cenário. Não simule uma falha nem afirme que o teste foi executado antes da correção sem evidência.

Para novas funcionalidades, priorize critérios de aceite e cenários positivos, negativos, limites, autorização e concorrência quando relevantes. Para documentação ou alterações sem comportamento executável, adapte a validação ao artefato.

### 3.5. Validar a entrega

A validação é dividida em três grupos:

**Obrigatória para toda alteração aplicável**
- revisar o diff completo;
- executar build/compilação e testes automatizados relevantes;
- executar a suíte completa disponível quando a alteração puder afetar o conjunto;
- verificar lint, formatação ou validação estática já configurados e pertinentes;
- conferir que não foram introduzidos segredos, dados pessoais reais ou arquivos gerados indevidos;
- verificar documentação e referências afetadas.

**Condicional ao tipo de alteração**
- Persistência: testes de integração com PostgreSQL via Testcontainers, incluindo migrations desde um banco descartável limpo quando pertinente.
- API: validar o contrato OpenAPI quando endpoints, schemas, parâmetros, respostas ou erros forem afetados.
- Autenticação/autorização/segurança: testar os fluxos e casos de negação, isolamento e limites relevantes.
- Frontend: executar lint, testes e build disponíveis; validar fluxo visual/manual quando houver impacto de interface.
- Concorrência, agendamento ou tempo: usar testes determinísticos sempre que possível, evitando depender de espera real.
- Integrações externas: validar contratos e falhas com testes isolados; usar ambiente externo somente quando necessário e autorizado.

**Operacional e dependente de autorização**
- aplicar migrations em Supabase ou outro banco compartilhado;
- executar smoke tests em ambiente compartilhado;
- alterar dados, configurações, segredos ou serviços externos;
- publicar/deployar ou executar operações que afetem usuários e colegas.

Não se exige acesso ao banco compartilhado nem smoke test live em todo PR. Quando uma validação condicional não for necessária, registre-a como não aplicável; quando seria necessária mas não pôde ser executada, registre a limitação e seu impacto.

### 3.6. Revisão antes do Pull Request

Antes de abrir ou atualizar o PR, faça uma autorrevisão independente:

1. Leia o diff como revisor, não apenas como autor.
2. Compare cada mudança com o escopo e os critérios de aceite.
3. Procure regressões, caminhos não testados, tratamento de erros incompleto e problemas de segurança.
4. Verifique compatibilidade com arquitetura, SPECs, OpenAPI e convenções.
5. Confirme migrations, permissões, transações e isolamento quando aplicáveis.
6. Revise mensagens de commit, arquivos gerados e conteúdo sensível.
7. Confirme que os resultados relatados correspondem a comandos realmente executados.

O PR deve ter um objetivo principal, descrição clara, evidências úteis e pendências explícitas. Use o [modelo de Pull Request](../../.github/PULL_REQUEST_TEMPLATE.md).

### 3.7. Acompanhar revisão e CI

- Consulte os checks e resultados atuais do GitHub; não confie apenas em uma execução local.
- Analise cada comentário de revisão e responda com alteração, justificativa técnica ou pedido de esclarecimento.
- Corrija problemas dentro do escopo aprovado.
- Se uma revisão revelar mudança de escopo, contrato, arquitetura, segurança ou regra de negócio, interrompa essa parte e solicite aprovação.
- Após novos commits, reexecute as verificações afetadas e confirme o estado final da CI.
- Não considere um PR pronto apenas porque foi aberto ou porque uma execução anterior passou.

### 3.8. Encerrar a entrega

Antes de considerar o trabalho concluído:

- confirme o estado final do PR e dos checks;
- informe o que foi implementado e os arquivos principais;
- liste comandos e verificações realmente executados, com resultados;
- diferencie validação local, CI e validação em ambiente compartilhado;
- registre decisões tomadas, riscos, limitações e pendências;
- identifique claramente qualquer ação ainda dependente de autorização;
- não faça merge sem autorização humana explícita.

Uma pendência conhecida não bloqueia automaticamente o PR. Classifique-a como:
- **bloqueadora:** impede cumprir requisito, segurança, integridade ou critério de aceite;
- **não bloqueadora:** limitação conhecida e aceita, sem impedir a entrega atual;
- **fora do escopo:** deve ser tratada em outra tarefa.

## 4. Definition of Done

Os critérios objetivos de conclusão estão em [DEFINITION-OF-DONE.md](DEFINITION-OF-DONE.md). Use-o junto com este fluxo, não como substituto do planejamento e da revisão.

## 5. Autonomia e aprovações

Os limites de atuação de agentes estão em [AI-AUTONOMY-POLICY.md](AI-AUTONOMY-POLICY.md). Em caso de dúvida, prevalece a exigência de aprovação humana para ações irreversíveis ou decisões que alterem o produto.

## 6. Princípio final

O objetivo não é maximizar quantidade de testes, documentos ou etapas. É produzir mudanças pequenas, rastreáveis e verificáveis, com evidência proporcional ao risco e sem transformar hipóteses em decisões.
