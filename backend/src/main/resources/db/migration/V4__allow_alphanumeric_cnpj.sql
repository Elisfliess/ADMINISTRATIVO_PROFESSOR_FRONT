-- Adequa a constraint de CNPJ ao formato alfanumérico da Receita Federal,
-- vigente a partir de 31/07/2026: as 12 primeiras posições aceitam 0-9 ou A-Z
-- e as 2 últimas continuam sendo dígitos verificadores numéricos.
--
-- A coluna CHAR(14) já comporta letras; esta migration apenas substitui a
-- constraint de formato. Não há reescrita nem alteração de dados, e todos os
-- CNPJs numéricos legados continuam válidos. A conferência dos dígitos
-- verificadores permanece na aplicação (CnpjValidator), como antes.
ALTER TABLE professor DROP CONSTRAINT ck_professor_cnpj;

ALTER TABLE professor ADD CONSTRAINT ck_professor_cnpj
    CHECK (BTRIM(cnpj) ~ '^[0-9A-Z]{12}[0-9]{2}$');
