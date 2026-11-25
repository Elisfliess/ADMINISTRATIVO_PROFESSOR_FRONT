# SPEC — [Nome do módulo]

**Status:** primeira versão — revisar junto ao contrato.  
**Módulo:** `[nome]`  
**Fontes:** RFs, RNs, decisões e A14 aplicáveis.

## 1. Objetivo e responsabilidade

Descrever a responsabilidade do módulo e o que não pertence a ele.

## 2. Funcionalidades do MVP

Listar somente comportamentos aprovados, com rastreabilidade para RF/RN/decisão.

## 3. Regras de negócio

Descrever pré-condições, estados, transições, invariantes e resultados. Distinguir decisões aprovadas de detalhes técnicos livres.

## 4. Modelo de domínio e persistência

Referenciar entidades/campos da A14. Não criar atributos, tabelas ou relacionamentos sem decisão/modelagem aprovada. Flyway é a fonte do schema.

## 5. API

Referenciar operações e schemas em `docs/api/openapi.yaml`. A SPEC não deve criar um contrato HTTP paralelo.

## 6. Validações e erros

Indicar validações estruturais, regras de negócio, códigos de erro relevantes e status HTTP. Seguir Problem Details global.

## 7. Segurança e isolamento

Descrever autenticação, autorização, escopo por professor, dados públicos/confidenciais e operações sensíveis.

## 8. Transações e concorrência

Definir limites transacionais e dependências. Operações transversais indivisíveis são coordenadas por `application`. Documentar ordem de locks quando aplicável.

## 9. Dependências entre módulos

Listar apenas chamadas por Services públicos, respeitando a arquitetura. Não acessar Repositories de outros módulos.

## 10. Testes e critérios de aceite

Cobrir cenários positivos, validações, conflitos, isolamento, transações e invariantes PostgreSQL relevantes.

## 11. Pontos pendentes

Registrar o que não está definido. Não preencher lacunas com suposições.
