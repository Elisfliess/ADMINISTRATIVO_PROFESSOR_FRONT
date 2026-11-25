import { useState } from 'react'

type Documento = 'Termos de Uso' | 'Política de Privacidade'

const CONTEUDO: Record<Documento, { titulo: string; texto: string }[]> = {
  'Termos de Uso': [
    {
      titulo: '1. Uso do Compasso',
      texto:
        'O Compasso organiza a agenda, os alunos e as cobranças do professor. O uso das funções administrativas depende da leitura e do aceite destes termos.',
    },
    {
      titulo: '2. Responsabilidades',
      texto:
        'O professor é responsável pela veracidade das informações cadastradas e pela comunicação com seus alunos. Aulas sobrepostas são permitidas.',
    },
    {
      titulo: '3. Cancelamento',
      texto:
        'Uma aula cancelada não pode ser reaberta. Reposições são novas aulas, sem vínculo com a original. Planos encerrados não são reativados.',
    },
  ],
  'Política de Privacidade': [
    {
      titulo: '1. Coleta de dados',
      texto:
        'O Compasso registra apenas os dados necessários para organizar a agenda, os alunos e as cobranças do professor.',
    },
    {
      titulo: '2. Uso dos dados',
      texto:
        'As informações são utilizadas para executar as funcionalidades da conta e exibir, quando publicado, o perfil público do professor.',
    },
    {
      titulo: '3. Analytics público',
      texto:
        'A página pública registra apenas contagens anônimas de visitas e cliques, sem IP, cookies, fingerprint ou identificador pessoal.',
    },
  ],
}

/** Frame do Figma: P48-termos-e-politica-leitura — claro / ... — escuro. */
export function P48TermosEPoliticaLeitura() {
  const [documento, setDocumento] = useState<Documento>('Termos de Uso')

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
          Termos e Política
        </h1>
        <p className="mt-1 text-xs font-medium uppercase tracking-wide" style={{ color: 'var(--muted)' }}>
          Leitura
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        {(Object.keys(CONTEUDO) as Documento[]).map((opcao) => {
          const ativa = documento === opcao
          return (
            <button
              key={opcao}
              type="button"
              onClick={() => setDocumento(opcao)}
              aria-pressed={ativa}
              className="rounded-full px-5 py-2.5 text-xs font-medium transition"
              style={{
                background: ativa ? '#5C3419' : '#EAE1D5',
                color: ativa ? '#FFFFFF' : '#3B281B',
              }}
            >
              {opcao}
            </button>
          )
        })}
      </div>

      <article
        className="space-y-6 rounded-2xl border p-6 sm:p-8"
        style={{
          background: '#FAF6F0',
          borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
        }}
      >
        {CONTEUDO[documento].map((secao) => (
          <div key={secao.titulo} className="space-y-2">
            <h2 className="text-base font-bold" style={{ color: '#2B1B10' }}>
              {secao.titulo}
            </h2>
            <p className="text-sm leading-relaxed" style={{ color: '#5A4A3F' }}>
              {secao.texto}
            </p>
          </div>
        ))}

        <p className="border-t pt-4 text-xs" style={{ borderColor: '#E5DDD2', color: '#6E5D52' }}>
          Versão vigente: 1.0.
        </p>
      </article>
    </section>
  )
}
