import { createElement, type ComponentType } from 'react'
import { screenDefinitions } from './definitions'
import type { ScreenDefinition } from './types'
import { G04SessaoExpirada } from './G04-sessao-expirada'
import { P01Login } from './P01-login'
import { P02Cadastro } from './P02-cadastro'
import { P03VerifiqueSeuEMail } from './P03-verifique-seu-e-mail'
import { P04EMailConfirmadoOuLinkInvalido } from './P04-e-mail-confirmado-ou-link-invalido'
import { P05RecuperarSenha } from './P05-recuperar-senha'
import { P06EMailDeRecuperacaoEnviado } from './P06-e-mail-de-recuperacao-enviado'
import { P07RedefinirSenha } from './P07-redefinir-senha'
import { P08AceiteDeTermosEPolitica } from './P08-aceite-de-termos-e-politica'
import { P10Sair } from './P10-sair'
import { P11ListaDeAlunos } from './P11-lista-de-alunos'
import { P12CadastrarAluno } from './P12-cadastrar-aluno'
import { P13FichaDoAluno } from './P13-ficha-do-aluno'
import { P14AbaHistoricoDeAulas } from './P14-aba-historico-de-aulas'
import { P15AbaCobrancas } from './P15-aba-cobrancas'
import { P16EditarAluno } from './P16-editar-aluno'
import { P17InativarOuReativar } from './P17-inativar-ou-reativar'
import { P18ExportarDados } from './P18-exportar-dados'
import { P19ExcluirOuAnonimizar } from './P19-excluir-ou-anonimizar'
import { P21Agenda } from './P21-agenda'
import { P22CriarOuEditarAula } from './P22-criar-ou-editar-aula'
import { P23DetalheDaAula } from './P23-detalhe-da-aula'
import { P25RemarcarAula } from './P25-remarcar-aula'
import { P26CancelarAula } from './P26-cancelar-aula'
import { P27MarcarStatus } from './P27-marcar-status'
import { P28VisaoGeralDoMes } from './P28-visao-geral-do-mes'
import { P29FiltrosEmAtrasoEPendentes } from './P29-filtros-em-atraso-e-pendentes'
import { P30DarBaixaNaCobranca } from './P30-dar-baixa-na-cobranca'
import { P32ExportarRecebimentos } from './P32-exportar-recebimentos'
import { P33EditorDaPaginaPublica } from './P33-editor-da-pagina-publica'
import { P40PreviaSimples } from './P40-previa-simples'
import { P42DashboardDaPaginaPublica } from './P42-dashboard-da-pagina-publica'
import { P43ConfiguracoesDeConta } from './P43-configuracoes-de-conta'
import { P45TemaClaroEEscuro } from './P45-tema-claro-e-escuro'
import { P46EncerrarConta } from './P46-encerrar-conta'
import { P48TermosEPoliticaLeitura } from './P48-termos-e-politica-leitura'
import { P49PlanoEncerrarENovo } from './P49-plano-encerrar-e-novo'
import { P50CancelarCobranca } from './P50-cancelar-cobranca'
import { V01PaginaPublicaDoProfessor } from './V01-pagina-publica-do-professor'
import { V02PoliticaDePrivacidade } from './V02-politica-de-privacidade'
import { V03PaginaNaoEncontrada } from './V03-pagina-nao-encontrada'
import { V04TermosDeUso } from './V04-termos-de-uso'

// As rotas P34, P35, P36 e P41 são abas/modais do editor (P33) no Figma.
// Renderizá-las pelo próprio editor mantém o chrome (abas, status, prévia) coerente.
function P34AbaApresentacaoRota() {
  return createElement(P33EditorDaPaginaPublica, { abaInicial: 'Apresentação' })
}

function P35AbaModalidadesEValoresRota() {
  return createElement(P33EditorDaPaginaPublica, { abaInicial: 'Modalidades e valores' })
}

function P36AbaContatosEWhatsappRota() {
  return createElement(P33EditorDaPaginaPublica, { abaInicial: 'Contatos e WhatsApp' })
}

function P41ConfirmarPublicacaoRota() {
  return createElement(P33EditorDaPaginaPublica, { abrirPublicacaoInicial: true })
}


export interface ScreenEntry extends Pick<
  ScreenDefinition,
  'id' | 'code' | 'title' | 'description' | 'group'
> {
  Component: ComponentType
}

const components: Record<string, ComponentType> = {
  'G04-sessao-expirada': G04SessaoExpirada,
  'P01-login': P01Login,
  'P02-cadastro': P02Cadastro,
  'P03-verifique-seu-e-mail': P03VerifiqueSeuEMail,
  'P04-e-mail-confirmado-ou-link-invalido': P04EMailConfirmadoOuLinkInvalido,
  'P05-recuperar-senha': P05RecuperarSenha,
  'P06-e-mail-de-recuperacao-enviado': P06EMailDeRecuperacaoEnviado,
  'P07-redefinir-senha': P07RedefinirSenha,
  'P08-aceite-de-termos-e-politica': P08AceiteDeTermosEPolitica,
  'P10-sair': P10Sair,
  'P11-lista-de-alunos': P11ListaDeAlunos,
  'P12-cadastrar-aluno': P12CadastrarAluno,
  'P13-ficha-do-aluno': P13FichaDoAluno,
  'P14-aba-historico-de-aulas': P14AbaHistoricoDeAulas,
  'P15-aba-cobrancas': P15AbaCobrancas,
  'P16-editar-aluno': P16EditarAluno,
  'P17-inativar-ou-reativar': P17InativarOuReativar,
  'P18-exportar-dados': P18ExportarDados,
  'P19-excluir-ou-anonimizar': P19ExcluirOuAnonimizar,
  'P21-agenda': P21Agenda,
  'P22-criar-ou-editar-aula': P22CriarOuEditarAula,
  'P23-detalhe-da-aula': P23DetalheDaAula,
  'P25-remarcar-aula': P25RemarcarAula,
  'P26-cancelar-aula': P26CancelarAula,
  'P27-marcar-status': P27MarcarStatus,
  'P28-visao-geral-do-mes': P28VisaoGeralDoMes,
  'P29-filtros-em-atraso-e-pendentes': P29FiltrosEmAtrasoEPendentes,
  'P30-dar-baixa-na-cobranca': P30DarBaixaNaCobranca,
  'P32-exportar-recebimentos': P32ExportarRecebimentos,
  'P33-editor-da-pagina-publica': P33EditorDaPaginaPublica,
  'P34-aba-apresentacao': P34AbaApresentacaoRota,
  'P35-aba-modalidades-e-valores': P35AbaModalidadesEValoresRota,
  'P36-aba-contatos-e-whatsapp': P36AbaContatosEWhatsappRota,
  'P40-previa-simples': P40PreviaSimples,
  'P41-confirmar-publicacao': P41ConfirmarPublicacaoRota,
  'P42-dashboard-da-pagina-publica': P42DashboardDaPaginaPublica,
  'P43-configuracoes-de-conta': P43ConfiguracoesDeConta,
  'P45-tema-claro-e-escuro': P45TemaClaroEEscuro,
  'P46-encerrar-conta': P46EncerrarConta,
  'P48-termos-e-politica-leitura': P48TermosEPoliticaLeitura,
  'P49-plano-encerrar-e-novo': P49PlanoEncerrarENovo,
  'P50-cancelar-cobranca': P50CancelarCobranca,
  'V01-pagina-publica-do-professor': V01PaginaPublicaDoProfessor,
  'V02-politica-de-privacidade': V02PoliticaDePrivacidade,
  'V03-pagina-nao-encontrada': V03PaginaNaoEncontrada,
  'V04-termos-de-uso': V04TermosDeUso,
}

export const screenRegistry: ScreenEntry[] = screenDefinitions.flatMap((definition) => {
  const Component = components[definition.id]
  if (!Component) return []
  return [
    {
      id: definition.id,
      code: definition.code,
      title: definition.title,
      description: definition.description,
      group: definition.group,
      Component,
    },
  ]
})

export const screenRegistryById = new Map(screenRegistry.map((entry) => [entry.id, entry]))
