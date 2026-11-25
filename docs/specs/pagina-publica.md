# SPEC — Página pública

**Status:** primeira versão consolidada.  
**Módulo:** `pagina_publica`  
**Rastreabilidade:** RF04–RF10, RF26–RF34; RN31–RN36; D-012; A14; A09/A10/A11.

## 1. Objetivo e responsabilidade

Administrar conteúdo explicitamente destinado à divulgação pública do professor, publicação da página e analytics anônimo. Não é uma API de consulta irrestrita ao perfil interno do professor.

## 2. Funcionalidades do MVP

- Editar apresentação, modalidades, formatos, valores opcionais e dados específicos da página. Os canais gerais de contato (e-mail, telefone e Instagram) são editados no módulo `professor`.
- Pré-visualizar alterações antes de publicar.
- Aprovar/publicar alterações; rejeitar ou sair mantém versão publicada.
- Consultar página pública por link/slug sem login.
- Registrar visitas e cliques no CTA de WhatsApp de forma anônima.
- Consultar totais agregados de visitas e cliques, sem filtro temporal.

## 3. Regras de negócio

- Página apresenta modalidades previstas: violão individual, técnica vocal individual, aulas em grupo e personalizadas (RN31).
- Valores são opcionais; chave mostrar valores desligada por padrão. Se desligada, não exibir valores nem data de atualização. Se ligada, mostrar valores e data de atualização (RN32).
- Exibir nome ou razão social, CNPJ e canais de contato conforme RN33/RF32. `email_contato`, `telefone_contato` e `instagram_url` pertencem a `PROFESSOR` e são editados no módulo `professor`.
- CTA abre conversa de WhatsApp com `whatsapp_numero` e `whatsapp_mensagem` configurados em `PERFIL_PUBLICO` (RF07/RN34).
- Página pública não exige login e deve funcionar desde 320 px.
- Somente conteúdo efetivamente publicado é visível ao visitante. Prévia não altera a versão pública e não conta no analytics.
- Não publicar dados de alunos, responsáveis, menores, agenda, pagamentos, credenciais ou dados internos.
- Analytics não usa cookies nem IP e não identifica origem do visitante. Registrar apenas tipo e momento do evento; indicadores mostram totais e conversão sem filtro temporal (RN35–RN36).
- Sem upload de mídia, personalização livre de cores/fontes, rascunho/publicado avançado ou recursos fora do MVP.

## 4. Modelo e persistência

A14 v3.4:
- `PROFESSOR`: `nome`, `razao_social`, `cnpj`, `email_contato`, `telefone_contato`, `instagram_url`. Os canais gerais de contato são mantidos pelo módulo `professor`; esta SPEC não duplica esses campos.
- `PERFIL_PUBLICO`: `slug`, `titulo`, `apresentacao`, `whatsapp_numero`, `whatsapp_mensagem`, `mostrar_valores`, `publicado`, `publicado_em`.
- `MODALIDADE_AULA`: `nome`, `formato`, `descricao`, `valor`, `valor_atualizado_em`, `duracao_minutos`, `publicada`. O campo da A14 é `publicada`, não `ativa`.
- `EVENTO_ANALYTICS`: `tipo_evento`, `data_hora`, associado ao professor/perfil, sem IP, cookies ou identificadores.
- Não existe entidade/tabela de rascunho no MVP. A prévia é estado temporário do frontend e não é persistida.
- A página pública é um DTO composto por allowlist: dados públicos de `PROFESSOR` + conteúdo de `PERFIL_PUBLICO` + modalidades publicadas de `MODALIDADE_AULA`. Não serializar Entities.
## 5. API

Contrato oficial em `docs/api/openapi.yaml`:
- `GET /publico/perfil`: consulta autenticada da configuração editorial, modalidades e canais públicos de contato lidos de `PROFESSOR`. O frontend também consulta `GET /professor` para obter nome, razão social e CNPJ que compõem a prévia.
- `POST /publico/perfil/publicacao`: recebe o snapshot completo aprovado (perfil, modalidades e canais públicos de contato) e persiste/publica tudo em uma única operação transacional coordenada por `application`.
- `PATCH /professor` não altera os canais públicos de contato; evita contornar a aprovação da página. `GET /professor` pode retorná-los para consulta, mas a escrita é feita pelo caso de uso de publicação.
- `GET /publico/{slug}`: consulta pública composta: nome/razão social/CNPJ e canais gerais vêm de `PROFESSOR`; apresentação, slug e CTA de WhatsApp vêm de `PERFIL_PUBLICO`; modalidades vêm de `MODALIDADE_AULA`.
- `POST /publico/{slug}/eventos`: registrar visita ou clique anônimo.
- `GET /publico/indicadores`: totais autenticados.

Não existe `PATCH /publico/perfil` nem endpoint de salvamento de rascunho. O corpo de publicação substitui integralmente a configuração editorial e representa o snapshot completo das modalidades e dos canais públicos de contato gerenciados no editor. IDs informados identificam modalidades existentes do mesmo professor; modalidades sem ID são novas. Modalidades existentes omitidas do snapshot deixam de ser publicadas (`publicada=false`), sem exclusão física, pois podem estar referenciadas por aulas. Os canais gerais de contato ficam armazenados em `PROFESSOR`, mas só são alterados por esta operação de publicação.

## 6. Validações e erros

- Slug único.
- Validar formato e tamanho de URLs, e-mail, telefone, mensagem e texto.
- Valores não negativos; publicação de valores somente quando mostrarValores=true.
- Rejeitar propriedades desconhecidas.
- Página não publicada/inexistente retorna 404.
- Analytics aceita somente tipos de evento previstos.
- 400 para entrada inválida; 401 para indicadores/admin sem sessão; 404 para página inexistente.
- Problem Details para erros.

## 7. Segurança e isolamento

- Edição, publicação e indicadores exigem sessão.
- Página pública permite acesso somente ao DTO público explicitamente aprovado.
- Sem dados pessoais de alunos ou menores.
- Analytics não registra IP, cookies, fingerprint ou identificador do visitante.
- Sanitizar conteúdo e evitar XSS; URLs de contato devem usar esquemas permitidos.

## 8. Transações e dependências

- Módulo pode consultar dados de professor via Service público.
- Não expor repositórios de professor.
- O frontend mantém localmente os valores editados e monta a prévia sem persistência. Rejeitar a prévia, cancelar a edição ou sair do editor descarta esse estado sem chamada de escrita.
- Os canais públicos (`email_contato`, `telefone_contato`, `instagram_url`) permanecem armazenados em `PROFESSOR`, mas são atualizados somente pela operação de publicação para não contornar RF09/RF10.
- Ao aprovar, o frontend envia o snapshot completo de perfil, modalidades e canais públicos para `POST /publico/perfil/publicacao`. A camada `application` coordena, em uma única transação, a persistência do perfil e das modalidades no módulo `pagina_publica` e dos canais públicos no módulo `professor`. Se qualquer etapa falhar, tudo é revertido e a página anterior permanece disponível.
- Se a validação ou persistência falhar, a transação inteira é revertida e a versão pública anterior permanece disponível.
- Modalidades removidas do snapshot completo são despublicadas, não excluídas fisicamente, para preservar vínculos históricos com aulas.
- Não existe histórico de versões nem restauração de publicação no MVP.
- Registro de analytics é operação curta e independente da resposta da página; não bloquear renderização por falha de indicador além do comportamento definido na implementação.

## 9. Testes e critérios de aceite

- Página pública acessível sem sessão; administração exige sessão.
- Página contém apenas dados permitidos.
- Mostrar valores desligado oculta preço e data de atualização.
- Prévia usa somente estado local do frontend: não chama endpoint de escrita, não altera o banco e não gera evento.
- Rejeitar/cancelar a prévia mantém intacta a configuração persistida e a página pública.
- A publicação envia o snapshot completo; sucesso persiste perfil e modalidades atomicamente e substitui o conteúdo visível publicamente.
- Falha na publicação não deixa perfil e modalidades parcialmente atualizados.
- Eventos não persistem IP/cookies/identificador do visitante.
- Indicadores retornam totais agregados sem filtro temporal.
- Layout e contrato atendem às telas a partir de 320 px (validação visual no frontend).
- Testar isolamento entre professores e slug.

## 10. Pendências

- Durante a implementação, conferir limites e constraints diretamente na A14 v3.4 e nas migrations vigentes.
- A A14 v3.4 já possui `whatsapp_numero` e `whatsapp_mensagem` em `PERFIL_PUBLICO`, além de `email_contato`, `telefone_contato` e `instagram_url` em `PROFESSOR`; não adicionar campos nem duplicar esses dados. A `MODALIDADE_AULA` usa `publicada` e `valor_atualizado_em`; não substituir por `ativa` nem permitir que o frontend defina a data de atualização do valor.
- A prévia temporária no frontend e a publicação transacional são decisões aprovadas para o MVP; não introduzir tabela de rascunho, versionamento ou endpoint de salvamento provisório sem nova decisão da equipe.
