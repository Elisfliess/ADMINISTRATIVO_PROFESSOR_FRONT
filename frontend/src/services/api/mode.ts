/**
 * A integração HTTP é ativada quando o frontend é apontado explicitamente para um backend
 * (por exemplo, VITE_API_BASE_URL=http://localhost:8080/api/v1). Sem essa configuração,
 * as telas funcionam em modo demonstração com dados fictícios e navegação local.
 */
export const backendEnabled = (() => {
  const base = import.meta.env.VITE_API_BASE_URL
  return typeof base === 'string' && base.trim().length > 0
})()
