# Checklist de segurança para desenvolvimento

- [ ] Nenhum segredo ou arquivo `.env` em texto puro versionado.
- [ ] `backend/.env.enc` contém somente valores criptografados com `age` e está versionado.
- [ ] Senha forte do `.env.enc` compartilhada apenas com integrantes autorizados e por canal separado do Git.
- [ ] Credenciais e senha do arquivo criptografado rotacionadas em caso de desligamento, suspeita de exposição ou antes de tornar o repositório público.
- [ ] Nenhum dado pessoal real em testes, fixtures, logs ou screenshots.
- [ ] Autenticação e autorização verificadas no servidor, não apenas na interface.
- [ ] Validação de entrada e tratamento seguro de erros.
- [ ] Menor privilégio para usuários, tokens e serviços.
- [ ] CORS, cookies, sessão e headers definidos conforme arquitetura.
- [ ] Dependências e imagens de container verificadas.
- [ ] Logs sem tokens, senhas ou dados pessoais desnecessários.
- [ ] Migrações revisadas e testadas em banco descartável.
- [ ] Fluxos críticos cobertos por testes.
- [ ] Backup, retenção e recuperação definidos antes de produção.
