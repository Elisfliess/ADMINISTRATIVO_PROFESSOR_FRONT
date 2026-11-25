# Política de segurança

## Reportar vulnerabilidades

Não publique detalhes de vulnerabilidades, credenciais ou dados pessoais em Issues públicas.

Como o repositório é privado, comunique imediatamente os responsáveis pelo projeto por um canal privado acordado pela equipe. Inclua:
- descrição e impacto potencial;
- passos mínimos para reproduzir, sem dados reais;
- componente/versão afetada;
- mitigação conhecida, se houver.

## Regras obrigatórias
- Nunca commitar `.env` em texto puro, tokens, senhas, chaves privadas, certificados ou credenciais.
- O arquivo `backend/.env.enc` é a única exceção: pode e deve ser versionado, pois contém apenas o ambiente criptografado com `age`.
- Proteger o `.env.enc` com uma senha forte e de alta entropia, compartilhada com integrantes autorizados por canal separado do Git.
- Ao remover alguém da equipe ou suspeitar de exposição da senha/arquivo, rotacionar as credenciais do Supabase, gerar um novo `.env.enc` e trocar a senha.
- Antes de tornar o repositório público, rotacionar todas as credenciais contidas no ambiente, gerar e versionar um novo `.env.enc` com uma nova senha forte. A rotação invalida os valores antigos mesmo que versões criptografadas permaneçam no histórico.
- Nunca incluir dados pessoais reais de alunos, responsáveis ou professores em código, logs, fixtures ou screenshots.
- Usar variáveis de ambiente/secret manager nos ambientes.
- Aplicar menor privilégio em tokens, contas e serviços.
- Manter dependências atualizadas e revisar alertas.
- Não registrar payloads sensíveis em logs.
- Em caso de segredo exposto: revogar/rotacionar imediatamente; remover do histórico não substitui a rotação.

## Escopo
Esta política será complementada quando autenticação, hospedagem, ambientes e tratamento de dados estiverem formalizados.
