-- Semeia a primeira versão publicada dos documentos legais exigidos no primeiro
-- acesso (RF35). A versão é referenciada pelo fluxo de aceite em
-- POST /api/v1/legal/acceptances. Novas versões devem ser inseridas por nova
-- migration, nunca editando esta.
INSERT INTO versao_documento_legal (id_versao, tipo, versao, publicada_em)
VALUES
    ('0199c9d4-8a00-7000-8000-000000000001', 'termos_uso', '1.0', TIMESTAMPTZ '2026-11-15 00:00:00+00'),
    ('0199c9d4-8a00-7000-8000-000000000002', 'politica_privacidade', '1.0', TIMESTAMPTZ '2026-11-15 00:00:00+00');
