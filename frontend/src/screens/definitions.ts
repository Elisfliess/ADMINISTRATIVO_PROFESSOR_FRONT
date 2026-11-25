import type { ActionDefinition, FieldDefinition, ScreenDefinition } from './types'

const action = (
  label: string,
  to: string,
  variant: ActionDefinition['variant'] = 'secondary',
): ActionDefinition => ({ label, to, variant })

const field = (
  name: string,
  label: string,
  extra: Omit<FieldDefinition, 'name' | 'label'> = {},
): FieldDefinition => ({ name, label, ...extra })

const abaAluno = [
  { slug: 'P13-ficha-do-aluno', label: 'Ficha' },
  { slug: 'P14-aba-historico-de-aulas', label: 'Histórico de aulas' },
  { slug: 'P15-aba-cobrancas', label: 'Cobranças' },
]

const abaPagina = [
  { slug: 'P33-editor-da-pagina-publica', label: 'Editor' },
  { slug: 'P34-aba-apresentacao', label: 'Apresentação' },
  { slug: 'P35-aba-modalidades-e-valores', label: 'Modalidades e valores' },
  { slug: 'P36-aba-contatos-e-whatsapp', label: 'Contatos e WhatsApp' },
]

const legalText = {
  privacidade: [
    '1. Coleta de dados. O Compasso registra apenas os dados necessários para organizar a agenda, os alunos e as cobranças do professor.',
    '2. Uso dos dados. As informações são utilizadas para executar as funcionalidades da conta e exibir, quando publicado, o perfil público do professor.',
    '3. Compartilhamento. Não há venda de dados. A página pública mostra somente os campos explicitamente publicados pelo professor.',
    '4. Analytics público. A página pública registra apenas contagens anônimas de visitas e cliques, sem IP, cookies, fingerprint ou identificador pessoal.',
    '5. Retenção e exclusão. O professor pode exportar seus dados e solicitar o encerramento da conta; alunos podem ser anonimizados preservando os registros financeiros.',
    '6. Segurança. A autenticação usa sessão HTTP com cookie HttpOnly e proteção CSRF. Nenhum token de acesso é exposto ao navegador.',
  ],
  termos: [
    '1. Aceite. O uso das funções administrativas depende da leitura e do aceite destes termos e da Política de Privacidade, registrados com versão e data/hora do servidor.',
    '2. Responsabilidades. O professor é responsável pela veracidade das informações cadastradas e pela comunicação com seus alunos.',
    '3. Agenda. Aulas sobrepostas são permitidas. Uma aula cancelada não pode ser reaberta; reposições são novas aulas, sem vínculo com a original.',
    '4. Financeiro. Planos são mensais ou por aula. Não há pagamento online, estorno, multa, juros ou suspensão por atraso neste serviço.',
    '5. Página pública. Somente o conteúdo publicado aparece para visitantes. Prévias não alteram a página publicada nem geram eventos.',
    '6. Encerramento. O encerramento de conta exige exportação prévia, nova autenticação e confirmação explícita.',
  ],
}

export const screenDefinitions: ScreenDefinition[] = [
  {
    id: 'G04-sessao-expirada',
    code: 'G04',
    title: 'Sessão expirada',
    description: 'A sessão HTTP de 30 minutos terminou ou foi invalidada pelo servidor.',
    group: 'Gerais',
    blocks: [
      {
        type: 'callout',
        tone: 'warning',
        title: 'Sua sessão terminou',
        text: 'Entre novamente para continuar. Nenhuma alteração pendente foi enviada ao servidor.',
      },
      {
        type: 'bullets',
        items: ['Use o mesmo e-mail da conta.', 'A recuperação de senha continua disponível.'],
      },
      {
        type: 'actions',
        items: [
          action('Voltar ao login', 'P01-login', 'primary'),
          action('Ler os termos', 'V04-termos-de-uso'),
        ],
      },
    ],
  },
  {
    id: 'P01-login',
    code: 'P01',
    title: 'Entrar',
    description: 'Acesse sua conta para organizar alunos, aulas e cobranças.',
    group: 'Usuário',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('email', 'E-mail', {
            type: 'email',
            placeholder: 'voce@exemplo.invalid',
            required: true,
          }),
          field('senha', 'Senha', {
            type: 'password',
            placeholder: 'Mínimo de 8 caracteres',
            required: true,
          }),
        ],
        submitLabel: 'Entrar',
        submitTo: 'P08-aceite-de-termos-e-politica',
        note: 'Se os dados não forem reconhecidos, você verá uma mensagem genérica. Confira o e-mail e a senha e tente novamente.',
      },
      {
        type: 'actions',
        items: [
          action('Criar conta', 'P02-cadastro'),
          action('Esqueci minha senha', 'P05-recuperar-senha'),
        ],
      },
    ],
  },
  {
    id: 'P02-cadastro',
    code: 'P02',
    title: 'Criar conta',
    description: 'Cadastro do professor. A conta só é liberada após a confirmação do e-mail.',
    group: 'Usuário',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('nome', 'Nome', { required: true, placeholder: 'Nome do professor' }),
          field('razaoSocial', 'Razão social', { required: true }),
          field('cnpj', 'CNPJ', {
            required: true,
            hint: '14 caracteres, sem pontuação.',
            defaultValue: '00000000000191',
          }),
          field('email', 'E-mail da conta', { type: 'email', required: true }),
          field('senha', 'Senha', {
            type: 'password',
            required: true,
            hint: '8 a 72 caracteres, com letra e número.',
          }),
          field('emailContato', 'E-mail de contato'),
          field('telefoneContato', 'Telefone de contato'),
        ],
        submitLabel: 'Cadastrar',
        submitTo: 'P03-verifique-seu-e-mail',
      },
      { type: 'actions', items: [action('Já tenho conta', 'P01-login')] },
    ],
  },
  {
    id: 'P03-verifique-seu-e-mail',
    code: 'P03',
    title: 'Verifique seu e-mail',
    description: 'Confirme seu e-mail pelo link que enviamos antes de acessar a conta.',
    group: 'Usuário',
    blocks: [
      {
        type: 'callout',
        tone: 'info',
        title: 'Enviamos um link de confirmação',
        text: 'Se o e-mail estiver cadastrado, você receberá o link em instantes.',
      },
      {
        type: 'fields',
        fields: [field('email', 'E-mail', { type: 'email', required: true })],
        submitLabel: 'Reenviar link',
        submitTo: 'P03-verifique-seu-e-mail',
        note: 'Você pode pedir um novo link após alguns minutos.',
      },
      { type: 'actions', items: [action('Voltar ao login', 'P01-login')] },
    ],
  },
  {
    id: 'P04-e-mail-confirmado-ou-link-invalido',
    code: 'P04',
    title: 'Confirmação de e-mail',
    description: 'Resultado do link de verificação: confirmado ou inválido/expirado.',
    group: 'Usuário',
    blocks: [
      {
        type: 'callout',
        tone: 'success',
        title: 'E-mail confirmado',
        text: 'A conta está pronta. Faça login para continuar.',
      },
      {
        type: 'callout',
        tone: 'error',
        title: 'Link inválido ou expirado',
        text: 'Solicite um novo link para continuar.',
      },
      {
        type: 'actions',
        items: [
          action('Entrar', 'P01-login', 'primary'),
          action('Solicitar novo link', 'P03-verifique-seu-e-mail'),
        ],
      },
    ],
  },
  {
    id: 'P05-recuperar-senha',
    code: 'P05',
    title: 'Recuperar senha',
    description:
      'Enviaremos um link para você definir uma nova senha. Ele é válido por até 1 hora.',
    group: 'Usuário',
    blocks: [
      {
        type: 'fields',
        fields: [field('email', 'E-mail', { type: 'email', required: true })],
        submitLabel: 'Enviar link',
        submitTo: 'P06-e-mail-de-recuperacao-enviado',
      },
      { type: 'actions', items: [action('Voltar ao login', 'P01-login')] },
    ],
  },
  {
    id: 'P06-e-mail-de-recuperacao-enviado',
    code: 'P06',
    title: 'E-mail de recuperação enviado',
    description: 'Confira as instruções que enviamos para o seu e-mail.',
    group: 'Usuário',
    blocks: [
      {
        type: 'callout',
        tone: 'success',
        text: 'Se o e-mail estiver cadastrado, você receberá um link válido por até 1 hora.',
      },
      { type: 'actions', items: [action('Voltar ao login', 'P01-login', 'primary')] },
    ],
  },
  {
    id: 'P07-redefinir-senha',
    code: 'P07',
    title: 'Redefinir senha',
    description: 'Defina uma nova senha usando o link recebido por e-mail.',
    group: 'Usuário',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('token', 'Link de recuperação', {
            required: true,
            hint: 'Cole aqui o link completo recebido por e-mail.',
          }),
          field('novaSenha', 'Nova senha', {
            type: 'password',
            required: true,
            hint: '8 a 72 caracteres, com letra e número.',
          }),
        ],
        submitLabel: 'Salvar nova senha',
        submitTo: 'P01-login',
      },
    ],
  },
  {
    id: 'P08-aceite-de-termos-e-politica',
    code: 'P08',
    title: 'Aceite de termos e política',
    description: 'Leia e aceite os documentos para começar a usar o Compasso.',
    group: 'Usuário',
    blocks: [
      {
        type: 'tabs',
        items: [
          { slug: 'V02-politica-de-privacidade', label: 'Política de Privacidade' },
          { slug: 'V04-termos-de-uso', label: 'Termos de Uso' },
        ],
      },
      {
        type: 'bullets',
        items: [
          'É possível ler os documentos sem aceitar.',
          'Até o aceite, a área administrativa fica bloqueada.',
          'Você ainda pode sair, exportar dados e encerrar a conta.',
        ],
      },
      {
        type: 'confirm',
        title: 'Declaro que li e aceito os documentos vigentes',
        text: 'Seu aceite será registrado com a versão vigente dos documentos.',
        checkboxLabel: 'Li e aceito os Termos de Uso e a Política de Privacidade (versão 1.0).',
        confirmLabel: 'Aceitar e continuar',
        confirmTo: 'P11-lista-de-alunos',
        cancelLabel: 'Não aceito: encerrar conta',
        cancelTo: 'P46-encerrar-conta',
      },
    ],
  },
  {
    id: 'P10-sair',
    code: 'P10',
    title: 'Sair',
    description: 'Ao sair, sua sessão é encerrada e você volta para a tela de login.',
    group: 'Usuário',
    blocks: [
      {
        type: 'callout',
        tone: 'info',
        text: 'Você será levado à tela de login. Dados não salvos nesta sessão serão perdidos.',
      },
      {
        type: 'actions',
        items: [
          action('Cancelar', 'P11-lista-de-alunos'),
          action('Sair da conta', 'P01-login', 'danger'),
        ],
      },
    ],
  },
  {
    id: 'P11-lista-de-alunos',
    code: 'P11',
    title: 'Alunos',
    description: 'A lista padrão mostra somente alunos ativos.',
    group: 'Alunos',
    blocks: [
      { type: 'actions', items: [action('Cadastrar aluno', 'P12-cadastrar-aluno', 'primary')] },
      { type: 'table', source: 'alunos', searchable: true, rowLink: 'P13-ficha-do-aluno' },
    ],
  },
  {
    id: 'P12-cadastrar-aluno',
    code: 'P12',
    title: 'Cadastrar aluno',
    description:
      'Nome, telefone e data de nascimento são obrigatórios. Para menores de 18 anos, informe o responsável legal.',
    group: 'Alunos',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('nome', 'Nome', { required: true }),
          field('telefone', 'Telefone', { required: true }),
          field('dataNascimento', 'Data de nascimento', { type: 'date', required: true }),
          field('email', 'E-mail'),
          field('responsavelNome', 'Nome do responsável legal', {
            hint: 'Obrigatório para menores de 18 anos.',
          }),
          field('responsavelContato', 'Contato do responsável legal'),
          field('observacoes', 'Observações', { type: 'textarea' }),
        ],
        submitLabel: 'Salvar aluno',
        submitTo: 'P13-ficha-do-aluno',
      },
      { type: 'actions', items: [action('Cancelar', 'P11-lista-de-alunos')] },
    ],
  },
  {
    id: 'P13-ficha-do-aluno',
    code: 'P13',
    title: 'Ficha do aluno',
    description: 'Dados cadastrais, histórico de aulas e cobranças do aluno selecionado.',
    group: 'Alunos',
    blocks: [
      {
        type: 'stats',
        items: [
          { label: 'Aluno', value: 'Aluno Exemplo 01' },
          { label: 'Situação', value: 'Ativo' },
          { label: 'Plano', value: 'Mensal · R$ 180,00' },
        ],
      },
      { type: 'tabs', items: abaAluno },
      {
        type: 'actions',
        items: [
          action('Editar', 'P16-editar-aluno', 'primary'),
          action('Inativar ou reativar', 'P17-inativar-ou-reativar'),
          action('Exportar dados', 'P18-exportar-dados'),
          action('Anonimizar', 'P19-excluir-ou-anonimizar', 'danger'),
        ],
      },
    ],
  },
  {
    id: 'P14-aba-historico-de-aulas',
    code: 'P14',
    title: 'Histórico de aulas',
    description: 'Aulas do aluno, incluindo status dada, falta e cancelada.',
    group: 'Alunos',
    blocks: [
      { type: 'tabs', items: abaAluno },
      { type: 'table', source: 'historicoAulas' },
    ],
  },
  {
    id: 'P15-aba-cobrancas',
    code: 'P15',
    title: 'Cobranças do aluno',
    description: 'Cobranças vinculadas ao aluno e suas situações.',
    group: 'Alunos',
    blocks: [
      { type: 'tabs', items: abaAluno },
      { type: 'table', source: 'cobrancas' },
      {
        type: 'actions',
        items: [
          action('Dar baixa', 'P30-dar-baixa-na-cobranca', 'primary'),
          action('Cancelar cobrança', 'P50-cancelar-cobranca'),
          action('Exportar recebimentos', 'P32-exportar-recebimentos'),
          action('Ver plano do aluno', 'P49-plano-encerrar-e-novo'),
        ],
      },
    ],
  },
  {
    id: 'P16-editar-aluno',
    code: 'P16',
    title: 'Editar aluno',
    description: 'Altere somente os dados que deseja atualizar.',
    group: 'Alunos',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('nome', 'Nome', { required: true, defaultValue: 'Aluno Exemplo 01' }),
          field('telefone', 'Telefone', { required: true, defaultValue: '11900010001' }),
          field('dataNascimento', 'Data de nascimento', {
            type: 'date',
            required: true,
            defaultValue: '1998-04-12',
          }),
          field('email', 'E-mail', { type: 'email', defaultValue: 'aluno01@exemplo.invalid' }),
          field('observacoes', 'Observações', { type: 'textarea' }),
        ],
        submitLabel: 'Salvar alterações',
        submitTo: 'P13-ficha-do-aluno',
      },
      { type: 'actions', items: [action('Cancelar', 'P13-ficha-do-aluno')] },
    ],
  },
  {
    id: 'P17-inativar-ou-reativar',
    code: 'P17',
    title: 'Inativar ou reativar aluno',
    description: 'Confira o que será afetado antes de inativar o aluno.',
    group: 'Alunos',
    blocks: [
      {
        type: 'callout',
        tone: 'warning',
        title: 'Prévia de inativação',
        text: '1 aula futura será cancelada; 1 cobrança pendente cancelável; 1 cobrança paga relacionada será preservada.',
      },
      {
        type: 'bullets',
        items: [
          'Reativar não recria aulas nem planos.',
          'Alunos anonimizados não podem ser reativados.',
        ],
      },
      {
        type: 'confirm',
        title: 'Confirmar inativação',
        text: 'Confirme para aplicar a inativação.',
        checkboxLabel: 'Confirmo a inativação após conferir a prévia.',
        confirmLabel: 'Inativar aluno',
        confirmTo: 'P13-ficha-do-aluno',
        cancelLabel: 'Cancelar',
        cancelTo: 'P13-ficha-do-aluno',
      },
      { type: 'actions', items: [action('Reativar aluno', 'P13-ficha-do-aluno')] },
    ],
  },
  {
    id: 'P18-exportar-dados',
    code: 'P18',
    title: 'Exportar dados do aluno',
    description: 'Informe sua senha para gerar o arquivo CSV do aluno.',
    group: 'Alunos',
    blocks: [
      {
        type: 'fields',
        fields: [field('senha', 'Senha', { type: 'password', required: true })],
        submitLabel: 'Exportar CSV',
        submitTo: 'P13-ficha-do-aluno',
      },
    ],
  },
  {
    id: 'P19-excluir-ou-anonimizar',
    code: 'P19',
    title: 'Excluir ou anonimizar aluno',
    description: 'Confira os efeitos antes de anonimizar. Esta ação é definitiva.',
    group: 'Alunos',
    blocks: [
      {
        type: 'bullets',
        items: [
          'Nome passa a ser “Aluno anonimizado”.',
          'Dados pessoais removidos: telefone, e-mail, data de nascimento, observações e responsável legal.',
          'Registros de aulas e cobranças são preservados.',
          'O estado é final: não é possível editar ou reativar.',
        ],
      },
      {
        type: 'confirm',
        title: 'Anonimizar definitivamente',
        text: 'Esta operação não pode ser desfeita.',
        checkboxLabel: 'Confirmo que li a prévia e solicito a anonimização.',
        confirmLabel: 'Anonimizar aluno',
        confirmTo: 'P13-ficha-do-aluno',
        cancelLabel: 'Cancelar',
        cancelTo: 'P13-ficha-do-aluno',
      },
      { type: 'actions', items: [action('Voltar à ficha', 'P13-ficha-do-aluno')] },
    ],
  },
  {
    id: 'P21-agenda',
    code: 'P21',
    title: 'Agenda',
    description: 'Visão por intervalo com prévia dos indicadores prioritários.',
    group: 'Agenda',
    blocks: [
      {
        type: 'stats',
        items: [
          { label: 'Aulas na semana', value: '4' },
          { label: 'Atrasos', value: '1' },
          { label: 'Pendentes', value: '2' },
        ],
      },
      {
        type: 'actions',
        items: [
          action('Nova aula', 'P22-criar-ou-editar-aula', 'primary'),
          action('Visão do mês', 'P28-visao-geral-do-mes'),
          action('Filtros', 'P29-filtros-em-atraso-e-pendentes'),
        ],
      },
      { type: 'table', source: 'aulas', rowLink: 'P23-detalhe-da-aula' },
    ],
  },
  {
    id: 'P22-criar-ou-editar-aula',
    code: 'P22',
    title: 'Criar ou editar aula',
    description:
      'A duração padrão é de 60 minutos. Informe o local da aula nas observações, quando necessário.',
    group: 'Agenda',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('alunoId', 'Aluno', {
            type: 'select',
            required: true,
            options: [
              { value: 'alu-001', label: 'Aluno Exemplo 01' },
              { value: 'alu-002', label: 'Aluno Exemplo 02' },
            ],
          }),
          field('modalidadeId', 'Modalidade', {
            type: 'select',
            required: true,
            options: [
              { value: 'mod-001', label: 'Violão individual' },
              { value: 'mod-002', label: 'Técnica vocal individual' },
            ],
          }),
          field('inicioEm', 'Início', {
            type: 'text',
            required: true,
            defaultValue: '2026-11-23T18:00',
          }),
          field('fimEm', 'Fim', {
            type: 'text',
            required: true,
            defaultValue: '2026-11-23T19:00',
            hint: 'Sugestão: início + 60 minutos; é editável.',
          }),
          field('repetirAte', 'Repetir semanalmente até', { type: 'date' }),
          field('observacoes', 'Observações', {
            type: 'textarea',
            hint: 'Informe o local da aula aqui, quando necessário.',
          }),
        ],
        submitLabel: 'Ver prévia e salvar',
        submitTo: 'P21-agenda',
      },
      {
        type: 'callout',
        tone: 'info',
        text: 'Antes de salvar, você verá quantas aulas e cobranças serão criadas.',
      },
    ],
  },
  {
    id: 'P23-detalhe-da-aula',
    code: 'P23',
    title: 'Detalhe da aula',
    description: 'Veja os dados desta aula e gerencie status, remarcação e cancelamento.',
    group: 'Agenda',
    blocks: [
      {
        type: 'stats',
        items: [
          { label: 'Aluno', value: 'Aluno Exemplo 01' },
          { label: 'Início', value: '23/11, 18:00' },
          { label: 'Status', value: 'AGENDADA' },
        ],
      },
      {
        type: 'callout',
        tone: 'info',
        text: 'Dada e falta só podem ser marcadas a partir do início da aula e podem ser trocadas ou desfeitas.',
      },
      {
        type: 'actions',
        items: [
          action('Editar aula', 'P22-criar-ou-editar-aula', 'primary'),
          action('Remarcar', 'P25-remarcar-aula'),
          action('Cancelar', 'P26-cancelar-aula', 'danger'),
          action('Marcar status', 'P27-marcar-status'),
        ],
      },
    ],
  },
  {
    id: 'P25-remarcar-aula',
    code: 'P25',
    title: 'Remarcar aula',
    description:
      'Remarcar altera o horário desta aula. Para criar uma reposição, use o botão correspondente.',
    group: 'Agenda',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('inicioEm', 'Novo início', { type: 'text', required: true }),
          field('fimEm', 'Novo fim', { type: 'text', required: true }),
        ],
        submitLabel: 'Salvar remarcação',
        submitTo: 'P21-agenda',
      },
      { type: 'actions', items: [action('Criar reposição', 'P22-criar-ou-editar-aula')] },
    ],
  },
  {
    id: 'P26-cancelar-aula',
    code: 'P26',
    title: 'Cancelar aula',
    description: 'O cancelamento é definitivo. Confira o impacto financeiro antes de continuar.',
    group: 'Agenda',
    blocks: [
      {
        type: 'callout',
        tone: 'warning',
        text: 'Prévia: 1 cobrança pendente relacionada será cancelada. A aula não poderá ser reaberta.',
      },
      {
        type: 'confirm',
        title: 'Cancelar aula definitivamente',
        text: 'A aula e a cobrança relacionada serão canceladas.',
        consequences: [
          'Aula passa a CANCELADA.',
          'Cobrança pendente relacionada é cancelada.',
          'Reagendamento da mesma aula não é permitido.',
        ],
        checkboxLabel: 'Confirmo o cancelamento após conferir a prévia.',
        confirmLabel: 'Cancelar aula',
        confirmTo: 'P21-agenda',
        cancelLabel: 'Voltar',
        cancelTo: 'P23-detalhe-da-aula',
      },
    ],
  },
  {
    id: 'P27-marcar-status',
    code: 'P27',
    title: 'Marcar status da aula',
    description: 'Escolha o status desta aula: dada ou falta.',
    group: 'Agenda',
    blocks: [
      {
        type: 'callout',
        tone: 'info',
        text: 'A troca entre dada e falta é permitida. A cobrança não muda quando a falta é trocada por dada.',
      },
      {
        type: 'actions',
        items: [
          action('Marcar como dada', 'P23-detalhe-da-aula', 'primary'),
          action('Marcar como falta', 'P23-detalhe-da-aula', 'danger'),
        ],
      },
    ],
  },
  {
    id: 'P28-visao-geral-do-mes',
    code: 'P28',
    title: 'Visão geral do mês',
    description: 'Resumo de aulas e cobranças do mês corrente.',
    group: 'Agenda',
    blocks: [
      {
        type: 'stats',
        items: [
          { label: 'Aulas', value: '18' },
          { label: 'Dadas', value: '12' },
          { label: 'Faltas', value: '3' },
          { label: 'Canceladas', value: '3' },
        ],
      },
      { type: 'table', source: 'aulas' },
      { type: 'actions', items: [action('Voltar à agenda', 'P21-agenda')] },
    ],
  },
  {
    id: 'P29-filtros-em-atraso-e-pendentes',
    code: 'P29',
    title: 'Filtros: atraso e pendentes',
    description: 'Filtros de apresentação sobre as cobranças do mês.',
    group: 'Agenda',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('situacao', 'Situação', {
            type: 'select',
            defaultValue: 'EM_ATRASO',
            options: [
              { value: 'TODAS', label: 'Todas' },
              { value: 'PENDENTE', label: 'Pendente' },
              { value: 'EM_ATRASO', label: 'Em atraso' },
            ],
          }),
          field('alunoId', 'Aluno', {
            type: 'select',
            options: [
              { value: '', label: 'Todos os alunos' },
              { value: 'alu-001', label: 'Aluno Exemplo 01' },
              { value: 'alu-002', label: 'Aluno Exemplo 02' },
            ],
          }),
        ],
        submitLabel: 'Aplicar filtros',
        submitTo: 'P29-filtros-em-atraso-e-pendentes',
      },
      { type: 'table', source: 'cobrancas', searchable: true },
    ],
  },
  {
    id: 'P30-dar-baixa-na-cobranca',
    code: 'P30',
    title: 'Dar baixa na cobrança',
    description: 'A baixa é integral, com data de recebimento e forma de pagamento opcional.',
    group: 'Financeiro',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('recebidoEm', 'Data de recebimento', {
            type: 'date',
            required: true,
            defaultValue: '2026-11-21',
          }),
          field('formaPagamento', 'Forma de pagamento', {
            type: 'select',
            options: [
              { value: 'Pix', label: 'Pix' },
              { value: 'Dinheiro', label: 'Dinheiro' },
              { value: 'Transferência', label: 'Transferência' },
            ],
          }),
        ],
        submitLabel: 'Confirmar baixa',
        submitTo: 'P15-aba-cobrancas',
        note: 'Não é possível usar uma data futura. Cobranças pagas ou canceladas não podem ser alteradas.',
      },
    ],
  },
  {
    id: 'P32-exportar-recebimentos',
    code: 'P32',
    title: 'Exportar recebimentos',
    description: 'Baixe o CSV com as cobranças pagas no mês selecionado.',
    group: 'Financeiro',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('mes', 'Mês', {
            type: 'text',
            required: true,
            defaultValue: '2026-11',
            hint: 'Formato YYYY-MM.',
          }),
          field('senha', 'Senha', { type: 'password', required: true }),
        ],
        submitLabel: 'Exportar CSV',
        submitTo: 'P15-aba-cobrancas',
      },
    ],
  },
  {
    id: 'P33-editor-da-pagina-publica',
    code: 'P33',
    title: 'Editor da página pública',
    description: 'Edite o conteúdo, confira a prévia e publique quando estiver pronto.',
    group: 'Página pública',
    blocks: [
      { type: 'tabs', items: abaPagina },
      {
        type: 'fields',
        fields: [
          field('slug', 'Endereço (slug)', { required: true, defaultValue: 'professor-exemplo' }),
          field('titulo', 'Título', { required: true, defaultValue: 'Música com calma e método' }),
          field('apresentacao', 'Apresentação', {
            type: 'textarea',
            required: true,
            defaultValue:
              'Aulas particulares de música para quem quer começar ou retomar o instrumento com consistência.',
          }),
          field('mostrarValores', 'Mostrar valores', {
            type: 'select',
            defaultValue: 'nao',
            options: [
              { value: 'nao', label: 'Não mostrar' },
              { value: 'sim', label: 'Mostrar valores' },
            ],
          }),
        ],
        submitLabel: 'Salvar alterações',
        submitTo: 'P40-previa-simples',
      },
      {
        type: 'actions',
        items: [
          action('Ver prévia', 'P40-previa-simples', 'primary'),
          action('Publicar', 'P41-confirmar-publicacao', 'danger'),
        ],
      },
    ],
  },
  {
    id: 'P34-aba-apresentacao',
    code: 'P34',
    title: 'Apresentação da página pública',
    description: 'Texto exibido no topo da página pública, sem login.',
    group: 'Página pública',
    blocks: [
      { type: 'tabs', items: abaPagina },
      {
        type: 'fields',
        fields: [
          field('titulo', 'Título', { required: true, defaultValue: 'Música com calma e método' }),
          field('apresentacao', 'Apresentação', {
            type: 'textarea',
            required: true,
            defaultValue:
              'Aulas particulares de música para quem quer começar ou retomar o instrumento com consistência.',
          }),
        ],
        submitLabel: 'Salvar alterações',
        submitTo: 'P33-editor-da-pagina-publica',
      },
    ],
  },
  {
    id: 'P35-aba-modalidades-e-valores',
    code: 'P35',
    title: 'Modalidades e valores',
    description: 'Escolha as modalidades e defina se os valores aparecerão na página pública.',
    group: 'Página pública',
    blocks: [
      { type: 'tabs', items: abaPagina },
      { type: 'table', source: 'modalidades' },
      {
        type: 'callout',
        tone: 'info',
        text: 'Quando “mostrar valores” estiver desligado, os valores não aparecem na página pública.',
      },
      { type: 'actions', items: [action('Voltar ao editor', 'P33-editor-da-pagina-publica')] },
    ],
  },
  {
    id: 'P36-aba-contatos-e-whatsapp',
    code: 'P36',
    title: 'Contatos e WhatsApp',
    description: 'Informe os canais de contato e a mensagem do botão de WhatsApp.',
    group: 'Página pública',
    blocks: [
      { type: 'tabs', items: abaPagina },
      {
        type: 'fields',
        fields: [
          field('emailContato', 'E-mail de contato', {
            type: 'email',
            defaultValue: 'contato@exemplo.invalid',
          }),
          field('telefoneContato', 'Telefone de contato', { defaultValue: '11999990000' }),
          field('instagramUrl', 'Instagram', {
            defaultValue: 'https://instagram.com/exemplo.invalid',
          }),
          field('whatsappNumero', 'WhatsApp', { required: true, defaultValue: '11999990000' }),
          field('whatsappMensagem', 'Mensagem do WhatsApp', {
            type: 'textarea',
            defaultValue: 'Olá! Gostaria de saber mais sobre as aulas.',
          }),
        ],
        submitLabel: 'Salvar alterações',
        submitTo: 'P33-editor-da-pagina-publica',
      },
    ],
  },
  {
    id: 'P40-previa-simples',
    code: 'P40',
    title: 'Prévia simples',
    description: 'Veja exatamente como a página ficará após a publicação.',
    group: 'Página pública',
    blocks: [
      { type: 'preview', audience: 'publico' },
      {
        type: 'actions',
        items: [
          action('Aprovar e publicar', 'P41-confirmar-publicacao', 'primary'),
          action('Voltar ao editor', 'P33-editor-da-pagina-publica'),
        ],
      },
    ],
  },
  {
    id: 'P41-confirmar-publicacao',
    code: 'P41',
    title: 'Confirmar publicação',
    description: 'Confirme para publicar esta versão da página.',
    group: 'Página pública',
    blocks: [
      {
        type: 'callout',
        tone: 'warning',
        text: 'Revise a prévia antes de confirmar: o conteúdo anterior será substituído.',
      },
      {
        type: 'confirm',
        title: 'Publicar esta versão da página',
        text: 'Modalidades removidas deixarão de aparecer na página pública.',
        consequences: [
          'A página pública passa a mostrar este conteúdo.',
          'A página passa a exibir a data da nova publicação.',
          'Não é possível restaurar a versão anterior.',
        ],
        checkboxLabel: 'Aprovo explicitamente a publicação desta versão.',
        confirmLabel: 'Aprovar e publicar',
        confirmTo: 'P42-dashboard-da-pagina-publica',
        cancelLabel: 'Rejeitar e voltar',
        cancelTo: 'P33-editor-da-pagina-publica',
      },
    ],
  },
  {
    id: 'P42-dashboard-da-pagina-publica',
    code: 'P42',
    title: 'Indicadores da página pública',
    description: 'Acompanhe visitas e cliques no WhatsApp da sua página.',
    group: 'Página pública',
    blocks: [
      {
        type: 'stats',
        items: [
          { label: 'Visitas', value: '128' },
          { label: 'Cliques no WhatsApp', value: '37' },
          { label: 'Conversão', value: '28,9%' },
        ],
      },
      {
        type: 'callout',
        tone: 'info',
        text: 'Os números são totais acumulados e não identificam visitantes.',
      },
      {
        type: 'actions',
        items: [
          action('Ver página pública', 'V01-pagina-publica-do-professor', 'primary'),
          action('Editar página', 'P33-editor-da-pagina-publica'),
        ],
      },
    ],
  },
  {
    id: 'P43-configuracoes-de-conta',
    code: 'P43',
    title: 'Configurações da conta',
    description:
      'Atualize seus dados e preferências. Contatos públicos mudam quando você publica a página.',
    group: 'Conta',
    blocks: [
      {
        type: 'fields',
        fields: [
          field('nome', 'Nome', { required: true, defaultValue: 'Professor Exemplo' }),
          field('razaoSocial', 'Razão social', {
            required: true,
            defaultValue: 'Professor Exemplo Música LTDA',
          }),
          field('cnpj', 'CNPJ', {
            defaultValue: '00000000000191',
            hint: 'Identificador da conta; não é editável nesta tela.',
          }),
        ],
        submitLabel: 'Salvar configurações',
        submitTo: 'P43-configuracoes-de-conta',
      },
      {
        type: 'actions',
        items: [
          action('Tema claro/escuro', 'P45-tema-claro-e-escuro'),
          action('Termos e privacidade', 'P48-termos-e-politica-leitura'),
          action('Encerrar conta', 'P46-encerrar-conta', 'danger'),
        ],
      },
    ],
  },
  {
    id: 'P45-tema-claro-e-escuro',
    code: 'P45',
    title: 'Tema claro e escuro',
    description: 'Escolha a aparência que prefere para o Compasso.',
    group: 'Conta',
    blocks: [
      {
        type: 'callout',
        tone: 'info',
        text: 'A escolha é salva neste navegador e aplicada a todas as telas, inclusive à página pública.',
      },
      { type: 'themeChooser' },
      {
        type: 'bullets',
        items: [
          'Claro: ideal para ambientes iluminados.',
          'Escuro: mais confortável em ambientes com pouca luz.',
          'A escolha vale para todo o aplicativo, inclusive a página pública.',
        ],
      },
    ],
  },
  {
    id: 'P46-encerrar-conta',
    code: 'P46',
    title: 'Encerrar conta',
    description: 'Antes de encerrar, exporte seus dados e confirme sua senha.',
    group: 'Conta',
    blocks: [
      {
        type: 'callout',
        tone: 'warning',
        text: 'Exporte seus dados antes de encerrar. A exportação não encerra a sessão.',
      },
      {
        type: 'bullets',
        items: [
          'A sessão é invalidada ao concluir.',
          'Um e-mail de confirmação é enviado após o encerramento.',
        ],
      },
      {
        type: 'fields',
        fields: [field('senha', 'Senha da conta', { type: 'password', required: true })],
        submitLabel: 'Validar senha',
        submitTo: 'P46-encerrar-conta',
      },
      {
        type: 'confirm',
        title: 'Encerrar definitivamente',
        text: 'Esta ação não pode ser desfeita.',
        checkboxLabel: 'Já exportei meus dados e confirmo o encerramento da conta.',
        confirmLabel: 'Encerrar conta',
        confirmTo: 'P01-login',
        cancelLabel: 'Exportar dados antes',
        cancelTo: 'P18-exportar-dados',
      },
    ],
  },
  {
    id: 'P48-termos-e-politica-leitura',
    code: 'P48',
    title: 'Termos e política para leitura',
    description: 'Documentos disponíveis mesmo quando o aceite está pendente.',
    group: 'Conta',
    blocks: [
      {
        type: 'tabs',
        items: [
          { slug: 'V02-politica-de-privacidade', label: 'Política de Privacidade' },
          { slug: 'V04-termos-de-uso', label: 'Termos de Uso' },
        ],
      },
      {
        type: 'paragraph',
        text: 'Versão vigente: 1.0. A leitura não substitui o registro de aceite feito no início.',
      },
      { type: 'actions', items: [action('Voltar às configurações', 'P43-configuracoes-de-conta')] },
    ],
  },
  {
    id: 'P49-plano-encerrar-e-novo',
    code: 'P49',
    title: 'Plano, encerramento e novo plano',
    description: 'Cada aluno tem um plano ativo, mensal ou por aula.',
    group: 'Financeiro',
    blocks: [
      {
        type: 'bullets',
        items: [
          'Mensal: valor fixo, adiantado, vencimento entre 1 e 28.',
          'Por aula: vencimento igual à data da aula mais o prazo configurado.',
          'Planos encerrados não são reativados; crie um novo plano.',
        ],
      },
      { type: 'table', source: 'planos' },
      {
        type: 'actions',
        items: [
          action('Voltar à ficha do aluno', 'P13-ficha-do-aluno'),
          action('Configurações da conta', 'P43-configuracoes-de-conta'),
        ],
      },
    ],
  },
  {
    id: 'P50-cancelar-cobranca',
    code: 'P50',
    title: 'Cancelar cobrança',
    description: 'Cobranças pagas e canceladas são estados finais e imutáveis.',
    group: 'Financeiro',
    blocks: [
      {
        type: 'callout',
        tone: 'warning',
        text: 'Uma cobrança mensal cancelada não é recriada automaticamente.',
      },
      {
        type: 'confirm',
        title: 'Cancelar esta cobrança',
        text: 'O cancelamento é permanente.',
        consequences: [
          'Status passa a CANCELADO.',
          'Nenhuma nova cobrança será gerada para o mesmo período.',
        ],
        checkboxLabel: 'Confirmo o cancelamento da cobrança.',
        confirmLabel: 'Cancelar cobrança',
        confirmTo: 'P15-aba-cobrancas',
        cancelLabel: 'Cancelar',
        cancelTo: 'P15-aba-cobrancas',
      },
    ],
  },
  {
    id: 'V01-pagina-publica-do-professor',
    code: 'V01',
    title: 'Página pública do professor',
    description: 'Conheça a página que seus alunos veem, sem precisar entrar.',
    group: 'Página pública',
    blocks: [
      { type: 'preview', audience: 'publico' },
      {
        type: 'actions',
        items: [
          { label: 'Abrir WhatsApp', href: 'https://wa.me/11999990000', variant: 'primary' },
          action('Voltar aos indicadores', 'P42-dashboard-da-pagina-publica'),
        ],
      },
    ],
  },
  {
    id: 'V02-politica-de-privacidade',
    code: 'V02',
    title: 'Política de Privacidade',
    description: 'Texto vigente, versão 1.0.',
    group: 'Legal',
    blocks: legalText.privacidade.map((text) => ({ type: 'paragraph' as const, text })),
  },
  {
    id: 'V03-pagina-nao-encontrada',
    code: 'V03',
    title: 'Página não encontrada',
    description: 'Rota desconhecida ou conteúdo não publicado.',
    group: 'Legal',
    blocks: [
      {
        type: 'callout',
        tone: 'error',
        title: '404',
        text: 'Não encontramos esta tela ou esta página pública.',
      },
      {
        type: 'actions',
        items: [action('Ir para o início', 'P01-login', 'primary'), action('Entrar', 'P01-login')],
      },
    ],
  },
  {
    id: 'V04-termos-de-uso',
    code: 'V04',
    title: 'Termos de Uso',
    description: 'Texto vigente, versão 1.0.',
    group: 'Legal',
    blocks: legalText.termos.map((text) => ({ type: 'paragraph' as const, text })),
  },
]

export const screenById = new Map(
  screenDefinitions.map((definition) => [definition.id, definition]),
)
