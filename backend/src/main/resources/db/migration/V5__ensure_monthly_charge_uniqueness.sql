-- Garante unicidade estrutural de cobranças mensais por plano e início de período.
-- Complementa uq_pagamento_aluno_periodo_ativo (aluno/período, apenas ativas) tornando
-- explícita a invariável por plano para qualquer status (incluindo cancelado), conforme
-- geração idempotente mensal. Cobranças por aula permanecem identificadas por aula_id.

CREATE UNIQUE INDEX IF NOT EXISTS uq_pagamento_plano_periodo_inicio_mensal
    ON pagamento (plano_id, periodo_inicio)
    WHERE aula_id IS NULL;
