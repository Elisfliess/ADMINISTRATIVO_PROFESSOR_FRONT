/** Tokens A11 do Figma: mesmo inventário e mesmos valores das collections Claro/Escuro. */
export type ThemeMode = 'claro' | 'escuro'

export const themeTokens = {
  claro: {
    fundo: '#FAF7F0',
    superficie: '#FFFFFF',
    textoPrincipal: '#443831',
    tintaDestaque: '#2B1E17',
    textoSecundario: '#5B4A3F',
    linkAcao: '#6B3A1F',
    bordaCampo: '#7D6B5E',
    focoExterno: '#0B57D0',
    sucesso: '#1E6B3A',
    erro: '#A32020',
    aviso: '#7A4E00',
    botaoFundo: '#EAE3E0',
    botaoTexto: '#6B3A1F',
  },
  escuro: {
    fundo: '#0B0D12',
    superficie: '#14181F',
    textoPrincipal: '#ECE8DE',
    tintaDestaque: '#ECE8DE',
    textoSecundario: '#B4B9C6',
    linkAcao: '#A5BBD2',
    bordaCampo: '#7F8797',
    focoExterno: '#7FB0FF',
    sucesso: '#6FCF97',
    erro: '#FF8F85',
    aviso: '#E5B94E',
    botaoFundo: '#A5BBD2',
    botaoTexto: '#0B0D12',
  },
} as const

export type ThemeToken = keyof (typeof themeTokens)['claro']
