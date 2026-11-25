# SPEC — Professor

**Status:** primeira versão consolidada.  
**Módulo:** `professor`  
**Rastreabilidade:** RF04–RF08, RF32–RF34; RN31–RN34; A14.

## 1. Objetivo e responsabilidade

Manter os dados cadastrais e de identificação do titular da conta. Conteúdo editorial da página pública pertence ao módulo `pagina_publica`; credenciais e sessão pertencem a `usuario`.

## 2. Funcionalidades do MVP

- Consultar dados do professor autenticado.
- Editar dados cadastrais permitidos.
- Fornecer e permitir editar os canais públicos de contato definidos na A14: e-mail, telefone/WhatsApp e Instagram.
- Manter vínculo único entre usuário e professor.

## 3. Regras de negócio

- Cada conta possui um único professor (RN19).
- O professor autenticado é identificado pela sessão; nunca aceitar `professorId` do cliente para escolher o titular dos dados.
- O CNPJ é obrigatório, único e normalizado para 14 caracteres sem pontuação. Desde 31/07/2026 o formato oficial é alfanumérico: as 12 primeiras posições aceitam `0-9` ou `A-Z` (maiúsculas) e as 2 últimas são dígitos verificadores numéricos, calculados pelo módulo 11 com valor ASCII − 48 por caractere, conforme A14 e a especificação da Receita Federal. CNPJs numéricos legados continuam válidos.
- Nome, razão social e CNPJ são dados do titular; e-mail/telefone de contato e Instagram são opcionais conforme A14.
- Na A14 v3.4, `email_contato`, `telefone_contato` e `instagram_url` são descritos como canais destinados à exibição pública (RF08). Esses campos permanecem armazenados em `PROFESSOR`, mas sua alteração é feita somente pelo caso de uso de publicação da página pública, para que a prévia e a aprovação do RF09/RF10 protejam também esses dados. O módulo `pagina_publica` consulta e atualiza esses campos por Service público; o frontend não os altera diretamente por `PATCH /professor`.
- A exposição pública é restrita aos campos explicitamente previstos em RF32/RN33 e ao conteúdo de contato configurado; não serializar a entidade `Professor` nem expor atributos internos.
- O módulo não gerencia modalidades, publicação, slug ou analytics.

## 4. Modelo e persistência

A14 v3.4:
- `PROFESSOR`: `id_professor`, `id_usuario`, `nome`, `razao_social`, `cnpj`, `email_contato`, `telefone_contato`, `instagram_url`, timestamps.
- `id_usuario` é FK única para `USUARIO`.
- UUIDv7 gerado na aplicação.
Não adicionar campos sem atualização aprovada da A14.

## 5. API

- `GET /professor`: dados do professor da sessão, incluindo canais de contato.
- `PATCH /professor`: atualização parcial dos dados cadastrais diretamente editáveis via JSON Merge Patch. Não aceita alterações de `email_contato`, `telefone_contato` ou `instagram_url`.
- Não há parâmetro de professor na rota; identidade vem da sessão.

Contrato detalhado em `docs/api/openapi.yaml`.

## 6. Validações e erros

- Validar tamanho, formato de e-mail/URL e CNPJ, incluindo o formato alfanumérico e os dígitos verificadores.
- Campos opcionais podem ser limpos por null somente quando permitido pelo schema. Os canais públicos de contato são validados na operação de publicação, não no PATCH cadastral.
- Rejeitar propriedades desconhecidas.
- 401 sem sessão; 400 para entrada inválida; 409 para conflito de unicidade; 500 genérico para falhas inesperadas.
- Problem Details é o formato único de erro.

## 7. Segurança e isolamento

- Todas as operações são autenticadas.
- O professor retornado é sempre o associado à sessão.
- Não retornar senha, hash, tokens, campos de segurança ou dados de outros professores.
- Dados públicos são montados por `pagina_publica` com allowlist explícita, não serializando a entidade.

## 8. Transações e dependências

- Módulo pode consultar `usuario` por seu Service público.
- `usuario` não depende de `professor`; cadastro inicial que cria ambos é coordenado por `application`.
- Outros módulos podem consultar professor por Services públicos.
- Alterações cadastrais simples usam transação local.
- `pagina_publica` pode consultar os dados de contato por Service público; `professor` não depende de `pagina_publica`.
- A publicação da página é coordenada por `application`: em uma única transação, atualiza o conteúdo editorial e as modalidades em `pagina_publica` e os canais públicos de contato em `professor`. Cada módulo mantém sua própria persistência e validações.

## 9. Testes e critérios de aceite

- Conta autenticada consulta e atualiza somente seu próprio professor.
- Alterar ID de usuário/professor no request não é possível pelo contrato.
- CNPJ inválido (formato, caracteres ou dígitos verificadores) ou duplicado é rejeitado.
- `PATCH /professor` não altera canais públicos de contato; esses valores só mudam após publicação aprovada da página.
- Campos opcionais podem ser limpos apenas quando permitidos.
- Resposta não contém dados internos de usuário.

## 10. Pendências

- Confirmar com a equipe se o cadastro inicial de professor mantém exatamente os campos e regras da A14; a primeira versão do contrato assume a criação coordenada junto ao cadastro de usuário.
