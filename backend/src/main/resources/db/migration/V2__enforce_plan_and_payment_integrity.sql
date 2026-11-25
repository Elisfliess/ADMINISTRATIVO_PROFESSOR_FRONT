-- Regras de integridade de planos, aulas e cobranças.
-- A V1 permanece imutável porque já pode ter sido aplicada em ambientes existentes.

-- O runtime não pode excluir planos ou cobranças; os estados finais são preservados.
REVOKE DELETE ON TABLE plano_pagamento, pagamento FROM compasso_runtime;



-- Permite que uma cobrança referencie a aula, o aluno e o professor em conjunto.
ALTER TABLE aula
    ADD CONSTRAINT uq_aula_id_aluno_professor
    UNIQUE (id_aula, aluno_id, professor_id);

ALTER TABLE pagamento
    DROP CONSTRAINT fk_pagamento_aula_professor;

ALTER TABLE pagamento
    ADD CONSTRAINT fk_pagamento_aula_aluno_professor
    FOREIGN KEY (aula_id, aluno_id, professor_id)
    REFERENCES aula (id_aula, aluno_id, professor_id);

-- Protege os dados financeiros de planos que já originaram cobranças.
-- Um plano encerrado não pode ser reativado; sua data de encerramento pode ser corrigida.
CREATE OR REPLACE FUNCTION proteger_configuracao_plano()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_possui_cobrancas BOOLEAN;
BEGIN
    IF OLD.encerrado_em IS NOT NULL THEN
        IF NEW.encerrado_em IS NULL THEN
            RAISE EXCEPTION 'Um plano encerrado não pode ser reativado.'
                USING ERRCODE = 'check_violation';
        END IF;

        IF ROW(
            NEW.periodicidade,
            NEW.valor,
            NEW.dia_vencimento,
            NEW.prazo_vencimento_dias,
            NEW.inicio_em
        ) IS DISTINCT FROM ROW(
            OLD.periodicidade,
            OLD.valor,
            OLD.dia_vencimento,
            OLD.prazo_vencimento_dias,
            OLD.inicio_em
        ) THEN
            RAISE EXCEPTION 'As configurações de um plano encerrado são imutáveis.'
                USING ERRCODE = 'check_violation';
        END IF;

        RETURN NEW;
    END IF;

    IF ROW(
        NEW.periodicidade,
        NEW.valor,
        NEW.dia_vencimento,
        NEW.prazo_vencimento_dias,
        NEW.inicio_em
    ) IS DISTINCT FROM ROW(
        OLD.periodicidade,
        OLD.valor,
        OLD.dia_vencimento,
        OLD.prazo_vencimento_dias,
        OLD.inicio_em
    ) THEN
        SELECT EXISTS (
            SELECT 1
            FROM pagamento p
            WHERE p.plano_id = OLD.id_plano
        )
        INTO v_possui_cobrancas;

        IF v_possui_cobrancas THEN
            RAISE EXCEPTION 'As configurações de um plano com cobranças não podem ser alteradas. Encerre-o e crie outro plano.'
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_plano_proteger_configuracao
    BEFORE UPDATE ON plano_pagamento
    FOR EACH ROW
    EXECUTE FUNCTION proteger_configuracao_plano();

-- Valida e protege cobranças. O bloqueio do plano serializa a geração de cobranças
-- com alterações nas configurações do próprio plano.
CREATE OR REPLACE FUNCTION validar_e_proteger_pagamento()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_plano plano_pagamento%ROWTYPE;
    v_aula aula%ROWTYPE;
    v_data_aula DATE;
    v_diferenca_meses INTEGER;
    v_inicio_ciclo DATE;
    v_proximo_ciclo DATE;
    v_fim_ciclo DATE;
    v_vencimento DATE;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF OLD.status = 'cancelado' THEN
            RAISE EXCEPTION 'Uma cobrança cancelada é imutável.'
                USING ERRCODE = 'check_violation';
        END IF;

        IF OLD.status = 'pago' THEN
            IF ROW(
                NEW.id_pagamento,
                NEW.professor_id,
                NEW.aluno_id,
                NEW.plano_id,
                NEW.aula_id,
                NEW.valor,
                NEW.vencimento_em,
                NEW.periodo_inicio,
                NEW.periodo_fim,
                NEW.data_pagamento,
                NEW.status,
                NEW.criado_em
            ) IS DISTINCT FROM ROW(
                OLD.id_pagamento,
                OLD.professor_id,
                OLD.aluno_id,
                OLD.plano_id,
                OLD.aula_id,
                OLD.valor,
                OLD.vencimento_em,
                OLD.periodo_inicio,
                OLD.periodo_fim,
                OLD.data_pagamento,
                OLD.status,
                OLD.criado_em
            ) THEN
                RAISE EXCEPTION 'Uma cobrança paga só permite alterar descrição e forma de pagamento.'
                    USING ERRCODE = 'check_violation';
            END IF;

            RETURN NEW;
        END IF;

        IF ROW(
            NEW.id_pagamento,
            NEW.professor_id,
            NEW.aluno_id,
            NEW.plano_id,
            NEW.aula_id,
            NEW.valor,
            NEW.criado_em
        ) IS DISTINCT FROM ROW(
            OLD.id_pagamento,
            OLD.professor_id,
            OLD.aluno_id,
            OLD.plano_id,
            OLD.aula_id,
            OLD.valor,
            OLD.criado_em
        ) THEN
            RAISE EXCEPTION 'Os vínculos e o valor de uma cobrança não podem ser alterados.'
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;

    -- A aplicação deve adquirir os bloqueios nesta ordem: plano, aula e cobrança.
    SELECT *
    INTO v_plano
    FROM plano_pagamento
    WHERE id_plano = NEW.plano_id
      AND aluno_id = NEW.aluno_id
      AND professor_id = NEW.professor_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'O plano informado não pertence ao aluno e professor da cobrança.'
            USING ERRCODE = 'foreign_key_violation';
    END IF;

    IF NEW.valor IS DISTINCT FROM v_plano.valor THEN
        RAISE EXCEPTION 'O valor da cobrança deve corresponder ao valor do plano de origem.'
            USING ERRCODE = 'check_violation';
    END IF;

    IF v_plano.periodicidade = 'mensal' THEN
        IF NEW.aula_id IS NOT NULL THEN
            RAISE EXCEPTION 'Uma cobrança mensal não pode estar vinculada a uma aula.'
                USING ERRCODE = 'check_violation';
        END IF;

        IF TG_OP = 'UPDATE'
           AND OLD.status = 'pendente'
           AND ROW(NEW.vencimento_em, NEW.periodo_inicio, NEW.periodo_fim)
               IS DISTINCT FROM
               ROW(OLD.vencimento_em, OLD.periodo_inicio, OLD.periodo_fim) THEN
            RAISE EXCEPTION 'As datas de uma cobrança mensal não podem ser alteradas após sua criação.'
                USING ERRCODE = 'check_violation';
        END IF;

        v_diferenca_meses :=
            (EXTRACT(YEAR FROM NEW.periodo_inicio)::INTEGER
                - EXTRACT(YEAR FROM v_plano.inicio_em)::INTEGER) * 12
            + EXTRACT(MONTH FROM NEW.periodo_inicio)::INTEGER
                - EXTRACT(MONTH FROM v_plano.inicio_em)::INTEGER;

        IF v_diferenca_meses < 0 THEN
            RAISE EXCEPTION 'O período da cobrança mensal não pode começar antes do plano.'
                USING ERRCODE = 'check_violation';
        END IF;

        -- Sempre calcula a partir do início original do plano, evitando deriva
        -- em ciclos iniciados no dia 29, 30 ou 31.
        v_inicio_ciclo :=
            (v_plano.inicio_em + make_interval(months => v_diferenca_meses))::DATE;
        v_proximo_ciclo :=
            (v_plano.inicio_em + make_interval(months => v_diferenca_meses + 1))::DATE;
        v_fim_ciclo := v_proximo_ciclo - 1;

        v_vencimento := make_date(
            EXTRACT(YEAR FROM v_inicio_ciclo)::INTEGER,
            EXTRACT(MONTH FROM v_inicio_ciclo)::INTEGER,
            v_plano.dia_vencimento
        );

        v_vencimento := GREATEST(v_plano.inicio_em, v_vencimento);

        IF NEW.periodo_inicio IS DISTINCT FROM v_inicio_ciclo
           OR NEW.periodo_fim IS DISTINCT FROM v_fim_ciclo
           OR NEW.vencimento_em IS DISTINCT FROM v_vencimento THEN
            RAISE EXCEPTION 'O período ou vencimento da cobrança mensal não corresponde ao ciclo definido pelo plano.'
                USING ERRCODE = 'check_violation';
        END IF;
    ELSIF v_plano.periodicidade = 'por_aula' THEN
        IF NEW.aula_id IS NULL THEN
            RAISE EXCEPTION 'Uma cobrança por aula precisa estar vinculada a uma aula.'
                USING ERRCODE = 'check_violation';
        END IF;

        SELECT *
        INTO v_aula
        FROM aula
        WHERE id_aula = NEW.aula_id
          AND aluno_id = NEW.aluno_id
          AND professor_id = NEW.professor_id
        FOR UPDATE;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'A aula da cobrança não pertence ao mesmo aluno e professor.'
                USING ERRCODE = 'foreign_key_violation';
        END IF;

        IF v_aula.status = 'cancelada' AND NEW.status IN ('pendente', 'pago') THEN
            IF TG_OP = 'INSERT' THEN
                RAISE EXCEPTION 'Uma aula cancelada não pode receber nem reativar uma cobrança.'
                    USING ERRCODE = 'check_violation';
            ELSIF OLD.status = 'pendente' THEN
                RAISE EXCEPTION 'Uma aula cancelada não pode receber nem reativar uma cobrança.'
                    USING ERRCODE = 'check_violation';
            END IF;
        END IF;

        v_data_aula := (v_aula.inicio_em AT TIME ZONE 'America/Sao_Paulo')::DATE;
        v_vencimento := v_data_aula + v_plano.prazo_vencimento_dias;

        IF NEW.periodo_inicio IS DISTINCT FROM v_data_aula
           OR NEW.periodo_fim IS DISTINCT FROM v_data_aula
           OR NEW.vencimento_em IS DISTINCT FROM v_vencimento THEN
            RAISE EXCEPTION 'O período e vencimento da cobrança devem corresponder à data local da aula e ao prazo do plano.'
                USING ERRCODE = 'check_violation';
        END IF;
    ELSE
        RAISE EXCEPTION 'Periodicidade de plano não reconhecida.'
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_pagamento_validar_integridade
    BEFORE INSERT OR UPDATE ON pagamento
    FOR EACH ROW
    EXECUTE FUNCTION validar_e_proteger_pagamento();

-- A aplicação atualiza aula e cobrança pendente na mesma transação.
-- Esta verificação é adiada até o commit, permitindo que ambas as atualizações
-- sejam concluídas antes de conferir o estado final.
CREATE OR REPLACE FUNCTION verificar_integridade_aula_pagamento()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_inicio_em TIMESTAMPTZ;
    v_status VARCHAR(20);
    v_data_aula DATE;
    v_pagamento_inconsistente UUID;
BEGIN
    SELECT inicio_em, status
    INTO v_inicio_em, v_status
    FROM aula
    WHERE id_aula = NEW.id_aula;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    IF v_status = 'cancelada' THEN
        SELECT p.id_pagamento
        INTO v_pagamento_inconsistente
        FROM pagamento p
        WHERE p.aula_id = NEW.id_aula
          AND p.status = 'pendente'
        LIMIT 1;

        IF FOUND THEN
            RAISE EXCEPTION 'A cobrança pendente da aula precisa ser cancelada na mesma transação.'
                USING ERRCODE = 'check_violation';
        END IF;

        RETURN NULL;
    END IF;

    v_data_aula := (v_inicio_em AT TIME ZONE 'America/Sao_Paulo')::DATE;

    SELECT p.id_pagamento
    INTO v_pagamento_inconsistente
    FROM pagamento p
    JOIN plano_pagamento pl ON pl.id_plano = p.plano_id
    WHERE p.aula_id = NEW.id_aula
      AND p.status = 'pendente'
      AND pl.periodicidade = 'por_aula'
      AND (
          p.periodo_inicio IS DISTINCT FROM v_data_aula
          OR p.periodo_fim IS DISTINCT FROM v_data_aula
          OR p.vencimento_em IS DISTINCT FROM
              (v_data_aula + pl.prazo_vencimento_dias)
      )
    LIMIT 1;

    IF FOUND THEN
        RAISE EXCEPTION 'A remarcação da aula e a atualização da cobrança pendente devem ocorrer na mesma transação.'
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_aula_integridade_pagamento
    AFTER UPDATE OF inicio_em, status ON aula
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION verificar_integridade_aula_pagamento();
