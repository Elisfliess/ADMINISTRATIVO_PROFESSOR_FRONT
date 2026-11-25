# SPEC — Aluno

**Status:** primeira versão consolidada.  
**Módulo:** `aluno`  
**Rastreabilidade:** RF11–RF15, RF37, RF39–RF40; RN15–RN19; A09; A14; D-012.

## 1. Objetivo e responsabilidade

Administrar o cadastro e o ciclo de vida dos alunos do professor, incluindo consulta e histórico de aulas. Aluno não possui conta, login ou área própria no MVP.

## 2. Funcionalidades do MVP

- Cadastrar, listar, buscar e consultar aluno.
- Editar dados cadastrais e responsável legal.
- Consultar histórico de aulas com data e status.
- Inativar e reativar aluno.
- Exportar dados do aluno em CSV com confirmação de senha (RF39).
- Excluir/anonimizar dados do aluno manualmente, apresentando resumo e exigindo confirmação de senha (RF40), respeitando retenções legais e registros financeiros.

## 3. Regras de negócio

- Nome, telefone/WhatsApp e data de nascimento são obrigatórios (RN15).
- CPF e RG não são exigidos; não solicitar dados sensíveis desnecessários.
- Menor de 18 anos exige nome e contato do responsável legal (RN16). Validação considera data de nascimento no momento da operação.
- Aluno pertence a um único professor; identidade do professor vem da sessão.
- A listagem padrão considera somente alunos ativos; inativos são consultados com `ativo=false`.
- Inativar: ocultar da lista ativa, registrar data, cancelar aulas futuras e cobranças pendentes ligadas a essas aulas, encerrar o plano. Cobranças pagas ligadas a aulas futuras não são alteradas; demais cobranças pendentes continuam disponíveis para baixa/cancelamento (RN17, RF15).
- Antes de confirmar inativação, apresentar resumo de quantas aulas/cobranças serão canceladas e avisar sobre cobranças pagas relacionadas (RF15). A operação efetiva deve revalidar os dados dentro da transação.
- Reativar não recria aulas canceladas nem reativa plano encerrado.
- Reativação limpa a data de inativação e exige que o aluno esteja inativo e não anonimizado; caso contrário retorna 409.
- Exportação CSV (RF39) exige a senha do professor e inclui `id`, `nome`, `telefone`, `email`, `dataNascimento`, `observacoes`, `responsavelNome`, `responsavelContato`, `ativo`, `inativadoEm`, `anonimizadoEm`, `criadoEm` e `atualizadoEm`, em UTF-8, com terminador `\r\n` e escaping RFC 4180. Valores iniciados por `=`, `+`, `-` ou `@`, ignorando espaços e caracteres de controle anteriores, recebem um apóstrofo apenas na exportação para evitar injeção de fórmula; o conteúdo persistido não é alterado. Senha inválida retorna 403.
- Anonimização (RF40) exige senha e confirmação explícita. Substitui o nome por `Aluno anonimizado`, limpa telefone, e-mail, data de nascimento, observações, responsável legal e `responsavel_informado_em`, preenche `anonimizado_em` e marca o aluno como inativo. A prévia lista os campos que serão removidos, incluindo `responsavelLegal.informadoEm`, e os registros preservados. Preserva identificador, professor, aulas, planos e cobranças; é estado final e bloqueia edição, reativação e nova anonimização com 409.
- Histórico de aulas é consultado em `AULA`; não criar tabela de histórico.
- Aluno não pode acessar a API administrativa.

## 4. Modelo e persistência

A14 v3.4:
- `ALUNO` pertence a `PROFESSOR` por FK composta (aluno + professor).
- Dados do responsável são condicionais para menores.
- Anonimização preserva registros financeiros/aulas necessários sem identificação, conforme retenção.
- A estrutura exata de campos deve seguir A14 e migrations; não acrescentar CPF/RG ou credenciais.

## 5. API

- `GET /alunos`: lista paginada e filtro por nome/ativo; sem o parâmetro `ativo`, retorna somente alunos ativos.
- `POST /alunos`: cadastro.
- `GET /alunos/{alunoId}`: detalhe.
- `PATCH /alunos/{alunoId}`: atualização parcial (JSON Merge Patch, RFC 7396); campos omitidos preservam o valor atual, inclusive dentro de `responsavelLegal`, e `null` limpa o campo quando permitido, revalidando RN16.
- `GET /alunos/{alunoId}/historico-aulas`: histórico paginado.
- `GET /alunos/{alunoId}/inativacao/previa`: apresentar contagens de aulas futuras, cobranças pendentes canceláveis e cobranças pagas relacionadas; não altera dados.
- `POST /alunos/{alunoId}/inativacao`: executar inativação; retorno informa contagens efetivamente processadas. A operação revalida os dados dentro da transação.
- `POST /alunos/{alunoId}/reativacao`: reativar aluno sem recriar aulas ou reativar plano.
- `POST /alunos/{alunoId}/exportacao`: recebe confirmação de senha e retorna CSV.
- `GET /alunos/{alunoId}/anonimizacao/previa`: apresenta resumo dos dados pessoais a anonimizar e registros que serão preservados.
- `POST /alunos/{alunoId}/anonimizacao`: recebe senha e confirmação explícita para executar anonimização manual.

## 6. Validações e erros

- Nome, telefone e data de nascimento obrigatórios.
- Responsável obrigatório para menor de 18 anos.
- Datas civis em `YYYY-MM-DD`.
- Rejeitar propriedades desconhecidas.
- 404 para aluno inexistente ou de outro professor.
- 409 para conflitos de estado; 400 para validações.
- Problem Details RFC 9457.

## 7. Segurança e isolamento

- Todas as operações administrativas exigem sessão.
- Escopo de consulta sempre inclui professor da sessão.
- Não aceitar `professorId` em payload, query ou rota como autorização.
- CSV e anonimização exigem confirmação de senha conforme RF39/RF40.
- Evitar dados reais em logs, fixtures e exemplos.

## 8. Transações e dependências

- Cadastro/edição simples em transação do módulo.
- Inativação é operação transversal coordenada por `application`: aluno, agenda e financeiro devem confirmar ou reverter juntos.
- Módulo aluno chama Services públicos de agenda/financeiro apenas conforme direção arquitetural aprovada; orquestração indivisível deve ficar em application.
- Histórico consulta o Service público de agenda; não acessar Repository de agenda.
- Reativação altera somente estado do aluno.

## 9. Testes e critérios de aceite

- Menor sem responsável não pode ser salvo.
- Um professor não consulta, altera, exporta ou anonimiza aluno de outro professor (404).
- Inativação cancela exatamente aulas futuras e cobranças pendentes vinculadas; preserva cobranças pagas e outras cobranças pendentes.
- Falha em qualquer etapa da inativação reverte a operação inteira.
- Reativação não recria aulas nem plano.
- Histórico reflete aulas e status reais.
- Prévia de inativação não altera dados; a execução revalida contagens e vínculos.
- Reativação não recria aulas nem plano.
- Exportação retorna CSV somente após confirmação de senha.
- Prévia de anonimização não altera dados; execução exige senha e confirmação explícita, preservando registros sujeitos à retenção.

## 10. Pendências

- Confirmar nomes, nulabilidade e limites finais dos campos do aluno diretamente na A14 e nas migrations ao implementar DTOs.
- Inativação (prévia e execução) e histórico de aulas dependem dos Services públicos de Financeiro (etapa 5) e Agenda (etapa 6); não implementar versões parciais com contagens vazias.
- O conteúdo exportável e as regras de anonimização foram definidos e aprovados para o MVP; a revisão legal final permanece pendente antes do lançamento público.
