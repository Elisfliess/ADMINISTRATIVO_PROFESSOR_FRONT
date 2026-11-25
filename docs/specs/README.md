# Especificações dos módulos

As SPECs descrevem o comportamento que a implementação deve cumprir no MVP. Elas complementam o contrato OpenAPI, sem substituí-lo.

## Fontes de verdade

1. Notion: requisitos funcionais RF01–RF61, regras RN01–RN37, D-012 (MVP), D-013 (verificação de e-mail), D-014 (sessão HTTP) e modelo A14 vigente.
2. `docs/architecture/BACKEND-ARCHITECTURE.md`: decisões técnicas de arquitetura.
3. `docs/api/openapi.yaml`: contrato HTTP oficial.
4. SPECs desta pasta: comportamento, responsabilidades, dependências, transações e critérios de aceite.

Se houver conflito, não escolher silenciosamente. Identificar a fonte, interromper a implementação afetada e resolver a decisão antes de alterar código ou contrato.

## Módulos

| SPEC | Responsabilidade |
| --- | --- |
| [Usuário](usuario.md) | Conta, cadastro, verificação de e-mail, autenticação, sessão, recuperação e aceite legal |
| [Professor](professor.md) | Dados do titular e perfil administrativo do professor |
| [Aluno](aluno.md) | Cadastro, consulta, edição, histórico, inativação e reativação |
| [Agenda](agenda.md) | Aulas, recorrência, remarcação, cancelamento e status |
| [Financeiro](financeiro.md) | Planos, cobranças, vencimentos, baixas, cancelamentos e CSV |
| [Página pública](pagina-publica.md) | Conteúdo publicado, modalidades, publicação e analytics anônimo |

## Como implementar

1. Leia a arquitetura e a SPEC do módulo.
2. Consulte RFs/RNs e decisões citadas na SPEC.
3. Confira os endpoints e schemas correspondentes em `docs/api/openapi.yaml`.
4. Implemente dentro dos limites de dependência do módulo.
5. Atualize OpenAPI e SPEC na mesma PR se uma alteração aprovada mudar o contrato.
6. Execute os testes e critérios de aceite previstos.

## Status e liberdade técnica

- **Aprovado:** comportamento decidido pelo produto/equipe.
- **Contrato definido:** forma HTTP documentada no OpenAPI.
- **Detalhe técnico livre:** escolha de implementação dentro das restrições arquiteturais.
- **Pendente:** não especificado; não inventar comportamento.

Uma SPEC não exige abstrações, classes ou métodos específicos quando isso não agrega valor. O objetivo é padronizar o resultado, não engessar cada decisão interna.
