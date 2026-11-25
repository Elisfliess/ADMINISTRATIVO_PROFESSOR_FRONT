-- Snapshot consolidado do schema para instalação PostgreSQL local em banco vazio.
-- Fonte de verdade e histórico de evolução: as migrations Flyway V1, V2 e posteriores.
-- Mantenha este arquivo sincronizado com o conjunto completo de migrations.
-- Este arquivo não substitui o histórico do Flyway nem deve ser aplicado em banco já migrado.
-- Modelo inicial do Compasso conforme A14, versão 3.3.
-- UUIDs são gerados pela aplicação (UUIDv7); não há geração automática no banco.

CREATE TABLE usuario (
    id_usuario UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    email_verificado_em TIMESTAMPTZ,
    tentativas_login INTEGER NOT NULL DEFAULT 0,
    bloqueado_ate TIMESTAMPTZ,
    ultimo_acesso_em TIMESTAMPTZ,
    encerramento_solicitado_em TIMESTAMPTZ,
    encerrada_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = LOWER(email)),
    CONSTRAINT ck_usuario_tentativas_login CHECK (tentativas_login >= 0)
);

CREATE TABLE professor (
    id_professor UUID PRIMARY KEY,
    id_usuario UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    cnpj CHAR(14) NOT NULL,
    email_contato VARCHAR(254),
    telefone_contato VARCHAR(30),
    instagram_url VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_professor_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_professor_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_professor_cnpj CHECK (BTRIM(cnpj) ~ '^[0-9A-Z]{12}[0-9]{2}$'),
    CONSTRAINT fk_professor_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE TABLE sessao (
    id_sessao UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso_em TIMESTAMPTZ NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    encerrada_em TIMESTAMPTZ,
    CONSTRAINT uq_sessao_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_sessao_expiracao CHECK (expira_em > criada_em),
    CONSTRAINT fk_sessao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario)
);

CREATE TABLE versao_documento_legal (
    id_versao UUID PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    versao VARCHAR(20) NOT NULL,
    publicada_em TIMESTAMPTZ NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_versao_documento_tipo_versao UNIQUE (tipo, versao),
    CONSTRAINT ck_versao_documento_tipo
        CHECK (tipo IN ('termos_uso', 'politica_privacidade'))
);

CREATE TABLE aceite_documento_legal (
    id_aceite UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    versao_id UUID NOT NULL,
    aceito_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_aceite_usuario_versao UNIQUE (usuario_id, versao_id),
    CONSTRAINT fk_aceite_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_aceite_versao
        FOREIGN KEY (versao_id) REFERENCES versao_documento_legal (id_versao)
);

CREATE TABLE token_conta (
    id_token UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    usado_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_token_conta_hash UNIQUE (token_hash),
    CONSTRAINT ck_token_conta_tipo
        CHECK (tipo IN ('recuperacao_senha', 'verificacao_email')),
    CONSTRAINT fk_token_conta_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id_usuario)
);

CREATE TABLE aluno (
    id_aluno UUID PRIMARY KEY,
    professor_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    telefone VARCHAR(30),
    email VARCHAR(254),
    data_nascimento DATE,
    observacoes TEXT,
    responsavel_nome VARCHAR(150),
    responsavel_contato VARCHAR(254),
    responsavel_informado_em TIMESTAMPTZ,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    inativado_em TIMESTAMPTZ,
    anonimizado_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_aluno_id_professor UNIQUE (id_aluno, professor_id),
    CONSTRAINT ck_aluno_obrigatorios
        CHECK (anonimizado_em IS NOT NULL OR (telefone IS NOT NULL AND data_nascimento IS NOT NULL)),
    CONSTRAINT fk_aluno_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id_professor)
);

CREATE TABLE modalidade_aula (
    id_modalidade UUID PRIMARY KEY,
    professor_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    formato VARCHAR(30) NOT NULL,
    descricao TEXT,
    valor NUMERIC(10, 2),
    valor_atualizado_em TIMESTAMPTZ,
    duracao_minutos SMALLINT,
    publicada BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_modalidade_id_professor UNIQUE (id_modalidade, professor_id),
    CONSTRAINT ck_modalidade_formato
        CHECK (formato IN ('individual', 'grupo', 'personalizado')),
    CONSTRAINT ck_modalidade_valor CHECK (valor IS NULL OR valor >= 0),
    CONSTRAINT ck_modalidade_duracao
        CHECK (duracao_minutos IS NULL OR duracao_minutos > 0),
    CONSTRAINT fk_modalidade_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id_professor)
);

CREATE TABLE aula (
    id_aula UUID PRIMARY KEY,
    professor_id UUID NOT NULL,
    aluno_id UUID NOT NULL,
    modalidade_id UUID,
    inicio_em TIMESTAMPTZ NOT NULL,
    fim_em TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    observacoes TEXT,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_aula_id_professor UNIQUE (id_aula, professor_id),
    CONSTRAINT ck_aula_status
        CHECK (status IN ('agendada', 'dada', 'falta', 'cancelada')),
    CONSTRAINT ck_aula_fim_posterior_inicio
        CHECK (fim_em IS NULL OR fim_em > inicio_em),
    CONSTRAINT fk_aula_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id_professor),
    CONSTRAINT fk_aula_aluno_professor
        FOREIGN KEY (aluno_id, professor_id)
        REFERENCES aluno (id_aluno, professor_id),
    CONSTRAINT fk_aula_modalidade_professor
        FOREIGN KEY (modalidade_id, professor_id)
        REFERENCES modalidade_aula (id_modalidade, professor_id)
);

CREATE TABLE plano_pagamento (
    id_plano UUID PRIMARY KEY,
    professor_id UUID NOT NULL,
    aluno_id UUID NOT NULL,
    periodicidade VARCHAR(20) NOT NULL,
    valor NUMERIC(10, 2) NOT NULL,
    dia_vencimento SMALLINT,
    prazo_vencimento_dias SMALLINT,
    inicio_em DATE NOT NULL,
    encerrado_em DATE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_plano_id_aluno_professor
        UNIQUE (id_plano, aluno_id, professor_id),
    CONSTRAINT ck_plano_periodicidade
        CHECK (periodicidade IN ('mensal', 'por_aula')),
    CONSTRAINT ck_plano_valor CHECK (valor >= 0),
    CONSTRAINT ck_plano_dia_vencimento
        CHECK (dia_vencimento IS NULL OR dia_vencimento BETWEEN 1 AND 28),
    CONSTRAINT ck_plano_prazo_vencimento
        CHECK (prazo_vencimento_dias IS NULL OR prazo_vencimento_dias >= 0),
    CONSTRAINT ck_plano_vencimento_por_periodicidade
        CHECK (
            (periodicidade = 'mensal'
                AND dia_vencimento IS NOT NULL
                AND prazo_vencimento_dias IS NULL)
            OR
            (periodicidade = 'por_aula'
                AND dia_vencimento IS NULL
                AND prazo_vencimento_dias IS NOT NULL)
        ),
    CONSTRAINT ck_plano_encerramento
        CHECK (encerrado_em IS NULL OR encerrado_em >= inicio_em),
    CONSTRAINT fk_plano_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id_professor),
    CONSTRAINT fk_plano_aluno_professor
        FOREIGN KEY (aluno_id, professor_id)
        REFERENCES aluno (id_aluno, professor_id)
);

CREATE UNIQUE INDEX uq_plano_ativo_por_aluno
    ON plano_pagamento (aluno_id)
    WHERE encerrado_em IS NULL;

CREATE TABLE pagamento (
    id_pagamento UUID PRIMARY KEY,
    professor_id UUID NOT NULL,
    aluno_id UUID NOT NULL,
    plano_id UUID NOT NULL,
    aula_id UUID,
    valor NUMERIC(10, 2) NOT NULL,
    vencimento_em DATE NOT NULL,
    periodo_inicio DATE NOT NULL,
    periodo_fim DATE NOT NULL,
    data_pagamento DATE,
    status VARCHAR(20) NOT NULL,
    forma_pagamento VARCHAR(30),
    descricao VARCHAR(250),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pagamento_valor CHECK (valor >= 0),
    CONSTRAINT ck_pagamento_status
        CHECK (status IN ('pendente', 'pago', 'cancelado')),
    CONSTRAINT ck_pagamento_periodo
        CHECK (periodo_fim >= periodo_inicio),
    CONSTRAINT ck_pagamento_data_status
        CHECK (
            (status = 'pago' AND data_pagamento IS NOT NULL)
            OR
            (status IN ('pendente', 'cancelado') AND data_pagamento IS NULL)
        ),
    CONSTRAINT fk_pagamento_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id_professor),
    CONSTRAINT fk_pagamento_aluno_professor
        FOREIGN KEY (aluno_id, professor_id)
        REFERENCES aluno (id_aluno, professor_id),
    CONSTRAINT fk_pagamento_plano_aluno_professor
        FOREIGN KEY (plano_id, aluno_id, professor_id)
        REFERENCES plano_pagamento (id_plano, aluno_id, professor_id),
    CONSTRAINT fk_pagamento_aula_professor
        FOREIGN KEY (aula_id, professor_id)
        REFERENCES aula (id_aula, professor_id)
);

CREATE UNIQUE INDEX uq_pagamento_aluno_periodo_ativo
    ON pagamento (aluno_id, periodo_inicio, periodo_fim)
    WHERE status IN ('pendente', 'pago') AND aula_id IS NULL;

CREATE UNIQUE INDEX uq_pagamento_aula_ativo
    ON pagamento (aula_id)
    WHERE aula_id IS NOT NULL AND status IN ('pendente', 'pago');

CREATE TABLE perfil_publico (
    id_perfil UUID PRIMARY KEY,
    professor_id UUID NOT NULL,
    slug VARCHAR(120) NOT NULL,
    titulo VARCHAR(150),
    apresentacao TEXT,
    whatsapp_numero VARCHAR(20),
    whatsapp_mensagem VARCHAR(500),
    mostrar_valores BOOLEAN NOT NULL DEFAULT FALSE,
    publicado BOOLEAN NOT NULL DEFAULT FALSE,
    publicado_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_perfil_professor UNIQUE (professor_id),
    CONSTRAINT uq_perfil_slug UNIQUE (slug),
    CONSTRAINT uq_perfil_id_professor UNIQUE (id_perfil, professor_id),
    CONSTRAINT fk_perfil_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id_professor)
);

CREATE TABLE evento_analytics (
    id_evento UUID PRIMARY KEY,
    professor_id UUID NOT NULL,
    perfil_id UUID NOT NULL,
    tipo_evento VARCHAR(30) NOT NULL,
    data_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_evento_analytics_tipo
        CHECK (tipo_evento IN ('visualizacao', 'clique_whatsapp')),
    CONSTRAINT fk_evento_professor
        FOREIGN KEY (professor_id) REFERENCES professor (id_professor),
    CONSTRAINT fk_evento_perfil_professor
        FOREIGN KEY (perfil_id, professor_id)
        REFERENCES perfil_publico (id_perfil, professor_id)
);

CREATE INDEX idx_sessao_usuario ON sessao (usuario_id);
CREATE INDEX idx_aceite_usuario ON aceite_documento_legal (usuario_id);
CREATE INDEX idx_token_conta_usuario_tipo ON token_conta (usuario_id, tipo);
CREATE INDEX idx_aluno_professor_ativo_nome ON aluno (professor_id, ativo, nome);
CREATE INDEX idx_aula_professor_inicio ON aula (professor_id, inicio_em);
CREATE INDEX idx_aula_aluno_inicio ON aula (aluno_id, inicio_em);
CREATE INDEX idx_plano_professor_aluno ON plano_pagamento (professor_id, aluno_id);
CREATE INDEX idx_pagamento_professor_status_vencimento
    ON pagamento (professor_id, status, vencimento_em);
CREATE INDEX idx_pagamento_aluno_periodo
    ON pagamento (aluno_id, periodo_inicio, periodo_fim);
CREATE INDEX idx_evento_professor_data_hora
    ON evento_analytics (professor_id, data_hora);

CREATE OR REPLACE FUNCTION impedir_reabertura_aula_cancelada()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status = 'cancelada' AND NEW.status <> 'cancelada' THEN
        RAISE EXCEPTION 'Uma aula cancelada não pode voltar a outro status.'
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_aula_cancelada_final
    BEFORE UPDATE OF status ON aula
    FOR EACH ROW
    EXECUTE FUNCTION impedir_reabertura_aula_cancelada();

CREATE VIEW vw_pagamento_situacao AS
SELECT
    pagamento.*,
    CASE
        WHEN status = 'pendente'
            AND vencimento_em < (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date
            THEN 'em_atraso'
        ELSE status
    END AS situacao
FROM pagamento;

-- Permissões do papel de runtime. O papel é provisionado fora do Flyway.
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    usuario,
    professor,
    sessao,
    versao_documento_legal,
    aceite_documento_legal,
    token_conta,
    aluno,
    modalidade_aula,
    aula,
    plano_pagamento,
    pagamento,
    perfil_publico,
    evento_analytics
TO compasso_runtime;

GRANT SELECT ON TABLE vw_pagamento_situacao TO compasso_runtime;

-- Novas tabelas criadas pelo papel que executa esta migration
-- também ficam acessíveis ao runtime, sem conceder CREATE no schema.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO compasso_runtime;
-- Início das regras adicionais consolidadas da migration V2.
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
