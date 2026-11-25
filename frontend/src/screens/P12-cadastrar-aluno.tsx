import { navigate } from '../router/hashRouter'
import { ALUNOS_MOCK, iniciaisDe, type AlunoFicha } from './alunos/dados'
import { FormularioAluno, type DadosFormularioAluno } from './alunos/FormularioAluno'

/** Frame do Figma: P12-cadastrar-aluno — claro / P12-cadastrar-aluno — escuro. */
export function P12CadastrarAluno() {
  function salvar(dados: DadosFormularioAluno) {
    const novo: AlunoFicha = {
      id: `aluno-${Date.now()}`,
      nome: dados.nome,
      iniciais: iniciaisDe(dados.nome),
      status: 'Ativo',
      email: dados.email || '—',
      telefone: dados.telefone,
      responsavel: dados.responsavel,
      dataNascimento: dados.dataNascimento,
      planoTitulo: dados.plano,
      planoDetalhe: `${dados.valorMensalidade || 'R$ 0,00'}, vence dia ${dados.diaVencimento}`,
      valorMensalidade: dados.valorMensalidade || 'R$ 0,00',
      diaVencimento: dados.diaVencimento,
      lgpdAutorizado: dados.lgpdAutorizado,
      proximaAulaDataHora: 'Sem aula marcada',
      proximaAulaTipo: '—',
      cobrancaStatus: 'Pendente',
      historico: [],
      cobrancas: [],
    }
    ALUNOS_MOCK.push(novo)
    navigate('/P11-lista-de-alunos')
  }

  return (
    <FormularioAluno
      titulo="Cadastrar aluno"
      onSalvar={salvar}
      onCancelar={() => navigate('/P11-lista-de-alunos')}
    />
  )
}
