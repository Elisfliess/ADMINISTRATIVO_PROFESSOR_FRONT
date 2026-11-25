package br.com.compasso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "DB_URL=jdbc:postgresql://localhost:5432/compasso_test",
        "DB_USERNAME=test",
        "DB_PASSWORD=test",
        "spring.flyway.enabled=true"
})
class CompassoApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse(
                    System.getenv().getOrDefault("POSTGRES_TEST_IMAGE", "postgres:17.11-alpine")
            )).withInitScript("db/create-test-roles.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void contextLoads() {
    }

    @Test
    void migrationCreatesExpectedTablesAndView() {
        assertThat(tableExists("usuario")).isTrue();
        assertThat(tableExists("professor")).isTrue();
        assertThat(tableExists("sessao")).isTrue();
        assertThat(tableExists("versao_documento_legal")).isTrue();
        assertThat(tableExists("aceite_documento_legal")).isTrue();
        assertThat(tableExists("token_conta")).isTrue();
        assertThat(tableExists("aluno")).isTrue();
        assertThat(tableExists("modalidade_aula")).isTrue();
        assertThat(tableExists("aula")).isTrue();
        assertThat(tableExists("plano_pagamento")).isTrue();
        assertThat(tableExists("pagamento")).isTrue();
        assertThat(tableExists("perfil_publico")).isTrue();
        assertThat(tableExists("evento_analytics")).isTrue();

        Integer viewCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.views
                WHERE table_schema = 'public'
                  AND table_name = 'vw_pagamento_situacao'
                """,
                Integer.class
        );

        assertThat(viewCount).isEqualTo(1);

        Integer indexCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname IN (
                    'uq_plano_ativo_por_aluno',
                    'uq_pagamento_aluno_periodo_ativo',
                    'uq_pagamento_aula_ativo',
                    'idx_sessao_usuario',
                    'idx_aceite_usuario',
                    'idx_token_conta_usuario_tipo',
                    'idx_aluno_professor_ativo_nome',
                    'idx_aula_professor_inicio',
                    'idx_aula_aluno_inicio',
                    'idx_plano_professor_aluno',
                    'idx_pagamento_professor_status_vencimento',
                    'idx_pagamento_aluno_periodo',
                    'idx_evento_professor_data_hora'
                  )
                """,
                Integer.class
        );

        assertThat(indexCount).isEqualTo(13);
    }


    @Test
    void runtimeRoleHasOnlyApplicationDataPrivileges() {
        assertThat(hasPrivilege("public.usuario", "SELECT")).isTrue();
        assertThat(hasPrivilege("public.usuario", "INSERT")).isTrue();
        assertThat(hasPrivilege("public.usuario", "UPDATE")).isTrue();
        assertThat(hasPrivilege("public.usuario", "DELETE")).isTrue();
        assertThat(hasPrivilege("public.plano_pagamento", "DELETE")).isFalse();
        assertThat(hasPrivilege("public.pagamento", "DELETE")).isFalse();
        assertThat(hasPrivilege("public.plano_pagamento", "SELECT")).isTrue();
        assertThat(hasPrivilege("public.plano_pagamento", "INSERT")).isTrue();
        assertThat(hasPrivilege("public.plano_pagamento", "UPDATE")).isTrue();
        assertThat(hasPrivilege("public.pagamento", "SELECT")).isTrue();
        assertThat(hasPrivilege("public.pagamento", "INSERT")).isTrue();
        assertThat(hasPrivilege("public.pagamento", "UPDATE")).isTrue();
        assertThat(hasPrivilege("public.vw_pagamento_situacao", "SELECT")).isTrue();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT has_schema_privilege('compasso_runtime', 'public', 'CREATE')",
                Boolean.class
        )).isFalse();

        assertThat(hasPrivilege("public.flyway_schema_history", "SELECT")).isFalse();

        jdbcTemplate.execute(
                "CREATE TABLE public.permission_future_test (id UUID PRIMARY KEY)"
        );
        try {
            assertThat(hasPrivilege("public.permission_future_test", "SELECT")).isTrue();
            assertThat(hasPrivilege("public.permission_future_test", "INSERT")).isTrue();
            assertThat(hasPrivilege("public.permission_future_test", "UPDATE")).isTrue();
            assertThat(hasPrivilege("public.permission_future_test", "DELETE")).isTrue();
        } finally {
            jdbcTemplate.execute("DROP TABLE public.permission_future_test");
        }
    }

    private boolean hasPrivilege(String table, String privilege) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT has_table_privilege('compasso_runtime', ?, ?)",
                Boolean.class,
                table,
                privilege
        ));
    }

    @Test
    void databaseRejectsRelationshipsAcrossDifferentProfessors() {
        UUID userA = uuid(1);
        UUID userB = uuid(2);
        UUID professorA = uuid(3);
        UUID professorB = uuid(4);
        UUID studentA = uuid(5);

        insertProfessor(userA, professorA, "prof-a@example.test", "00000000000001");
        insertProfessor(userB, professorB, "prof-b@example.test", "00000000000002");
        insertStudent(studentA, professorA);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO aula (id_aula, professor_id, aluno_id, inicio_em, status)
                VALUES (?, ?, ?, ?, 'agendada')
                """,
                uuid(6), professorB, studentA,
                Timestamp.from(Instant.parse("2026-11-12T12:00:00Z"))
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void databaseRejectsCrossProfessorReferencesInPlansPaymentsAndAnalytics() {
        UUID userA = uuid(50);
        UUID userB = uuid(51);
        UUID professorA = uuid(52);
        UUID professorB = uuid(53);
        UUID studentA = uuid(54);
        UUID planA = uuid(55);
        UUID profileA = uuid(56);

        insertProfessor(userA, professorA, "prof-integrity-a@example.test", "00000000000007");
        insertProfessor(userB, professorB, "prof-integrity-b@example.test", "00000000000008");
        insertStudent(studentA, professorA);
        insertMonthlyPlan(planA, professorA, studentA);

        jdbcTemplate.update(
                """
                INSERT INTO perfil_publico (id_perfil, professor_id, slug)
                VALUES (?, ?, 'perfil-integridade-a')
                """,
                profileA, professorA
        );

        assertThatThrownBy(() -> insertMonthlyPlan(uuid(57), professorB, studentA))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO pagamento (
                    id_pagamento, professor_id, aluno_id, plano_id, valor,
                    vencimento_em, periodo_inicio, periodo_fim, status
                )
                VALUES (?, ?, ?, ?, 100.00, DATE '2026-01-10',
                        DATE '2026-01-01', DATE '2026-01-31', 'pendente')
                """,
                uuid(58), professorB, studentA, planA
        )).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO evento_analytics (id_evento, professor_id, perfil_id, tipo_evento)
                VALUES (?, ?, ?, 'visualizacao')
                """,
                uuid(59), professorB, profileA
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void canceledClassCannotReturnToAnotherStatus() {
        UUID user = uuid(10);
        UUID professor = uuid(11);
        UUID student = uuid(12);
        UUID lesson = uuid(13);

        insertProfessor(user, professor, "prof-cancel@example.test", "00000000000003");
        insertStudent(student, professor);

        jdbcTemplate.update(
                """
                INSERT INTO aula (id_aula, professor_id, aluno_id, inicio_em, status)
                VALUES (?, ?, ?, ?, 'cancelada')
                """,
                lesson, professor, student,
                Timestamp.from(Instant.parse("2026-11-12T12:00:00Z"))
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE aula SET status = 'agendada' WHERE id_aula = ?",
                lesson
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void studentCannotHaveTwoActivePaymentPlans() {
        UUID user = uuid(20);
        UUID professor = uuid(21);
        UUID student = uuid(22);

        insertProfessor(user, professor, "prof-plan@example.test", "00000000000004");
        insertStudent(student, professor);
        insertMonthlyPlan(uuid(23), professor, student);

        assertThatThrownBy(() -> insertMonthlyPlan(uuid(24), professor, student))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void activeMonthlyChargeCannotDuplicateTheSameStudentPeriod() {
        UUID user = uuid(40);
        UUID professor = uuid(41);
        UUID student = uuid(42);
        UUID plan = uuid(43);

        insertProfessor(user, professor, "prof-period@example.test", "00000000000006");
        insertStudent(student, professor);
        insertMonthlyPlan(plan, professor, student);
        insertPendingMonthlyPayment(uuid(44), professor, student, plan);

        assertThatThrownBy(() ->
                insertPendingMonthlyPayment(uuid(45), professor, student, plan))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void overdueStatusIsCalculatedByTheDatabaseView() {
        UUID user = uuid(30);
        UUID professor = uuid(31);
        UUID student = uuid(32);
        UUID plan = uuid(33);
        UUID payment = uuid(34);

        insertProfessor(user, professor, "prof-payment@example.test", "00000000000005");
        insertStudent(student, professor);
        insertMonthlyPlan(plan, professor, student);

        jdbcTemplate.update(
                """
                INSERT INTO pagamento (
                    id_pagamento, professor_id, aluno_id, plano_id, valor,
                    vencimento_em, periodo_inicio, periodo_fim, status
                )
                VALUES (?, ?, ?, ?, 100.00, DATE '2026-01-10',
                        DATE '2026-01-01', DATE '2026-01-31', 'pendente')
                """,
                payment, professor, student, plan
        );

        String situacao = jdbcTemplate.queryForObject(
                "SELECT situacao FROM vw_pagamento_situacao WHERE id_pagamento = ?",
                String.class,
                payment
        );

        assertThat(situacao).isEqualTo("em_atraso");
    }


    @Test
    void monthlyPaymentMustMatchTheOriginalPlanCycleAndDueDate() {
        UUID user = uuid(100);
        UUID professor = uuid(101);
        UUID student = uuid(102);
        UUID plan = uuid(103);

        insertProfessor(user, professor, "prof-cycle@example.test", "00000000000009");
        insertStudent(student, professor);
        insertMonthlyPlan(plan, professor, student);
        jdbcTemplate.update(
                "UPDATE plano_pagamento SET inicio_em = DATE '2026-01-31' WHERE id_plano = ?",
                plan
        );

        jdbcTemplate.update(
                """
                INSERT INTO pagamento (
                    id_pagamento, professor_id, aluno_id, plano_id, valor,
                    vencimento_em, periodo_inicio, periodo_fim, status
                )
                VALUES (?, ?, ?, ?, 100.00, DATE '2026-02-10',
                        DATE '2026-02-28', DATE '2026-03-30', 'pendente')
                """,
                uuid(104), professor, student, plan
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO pagamento (
                    id_pagamento, professor_id, aluno_id, plano_id, valor,
                    vencimento_em, periodo_inicio, periodo_fim, status
                )
                VALUES (?, ?, ?, ?, 100.00, DATE '2026-02-11',
                        DATE '2026-02-28', DATE '2026-03-30', 'pendente')
                """,
                uuid(105), professor, student, plan
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void perLessonPaymentMustMatchItsLessonAndPlan() {
        UUID user = uuid(110);
        UUID professor = uuid(111);
        UUID student = uuid(112);
        UUID plan = uuid(113);
        UUID lesson = uuid(114);

        insertProfessor(user, professor, "prof-per-lesson@example.test", "00000000000010");
        insertStudent(student, professor);
        insertPerLessonPlan(plan, professor, student, 120.00, 3);
        insertLesson(lesson, professor, student, "2026-03-10T13:00:00Z");
        insertPerLessonPayment(uuid(115), professor, student, plan, lesson,
                120.00, "2026-03-10", "2026-03-13");

        UUID otherLesson = uuid(116);
        insertLesson(otherLesson, professor, student, "2026-03-11T13:00:00Z");

        assertThatThrownBy(() -> insertPerLessonPayment(uuid(117), professor, student,
                plan, otherLesson, 100.00, "2026-03-11", "2026-03-14"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void paymentCannotReferenceAnotherStudentsLesson() {
        UUID user = uuid(120);
        UUID professor = uuid(121);
        UUID studentA = uuid(122);
        UUID studentB = uuid(123);
        UUID planB = uuid(124);
        UUID lessonA = uuid(125);

        insertProfessor(user, professor, "prof-cross-student@example.test", "00000000000011");
        insertStudent(studentA, professor);
        insertStudent(studentB, professor);
        insertPerLessonPlan(planB, professor, studentB, 100.00, 2);
        insertLesson(lessonA, professor, studentA, "2026-03-10T13:00:00Z");

        assertThatThrownBy(() -> insertPerLessonPayment(uuid(126), professor, studentB,
                planB, lessonA, 100.00, "2026-03-10", "2026-03-12"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void reschedulingLessonAndPendingPaymentInOneTransactionSucceeds() {
        UUID user = uuid(130);
        UUID professor = uuid(131);
        UUID student = uuid(132);
        UUID plan = uuid(133);
        UUID lesson = uuid(134);
        UUID payment = uuid(135);

        insertProfessor(user, professor, "prof-reschedule@example.test", "00000000000012");
        insertStudent(student, professor);
        insertPerLessonPlan(plan, professor, student, 100.00, 3);
        insertLesson(lesson, professor, student, "2026-03-10T13:00:00Z");
        insertPerLessonPayment(payment, professor, student, plan, lesson,
                100.00, "2026-03-10", "2026-03-13");

        new TransactionTemplate(transactionManager).execute(status -> {
            jdbcTemplate.queryForObject(
                    "SELECT id_plano FROM plano_pagamento WHERE id_plano = ? FOR UPDATE",
                    UUID.class, plan
            );
            jdbcTemplate.queryForObject(
                    "SELECT id_aula FROM aula WHERE id_aula = ? FOR UPDATE",
                    UUID.class, lesson
            );
            jdbcTemplate.update(
                    "UPDATE aula SET inicio_em = ? WHERE id_aula = ?",
                    Timestamp.from(Instant.parse("2026-03-17T13:00:00Z")), lesson
            );
            jdbcTemplate.update(
                    """
                    UPDATE pagamento
                    SET periodo_inicio = DATE '2026-03-17',
                        periodo_fim = DATE '2026-03-17',
                        vencimento_em = DATE '2026-03-20'
                    WHERE id_pagamento = ?
                    """,
                    payment
            );
            return null;
        });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT vencimento_em FROM pagamento WHERE id_pagamento = ?",
                LocalDate.class, payment
        )).isEqualTo(LocalDate.parse("2026-03-20"));
    }

    @Test
    void reschedulingLessonWithoutUpdatingItsPendingPaymentIsRejectedAtCommit() {
        UUID user = uuid(140);
        UUID professor = uuid(141);
        UUID student = uuid(142);
        UUID plan = uuid(143);
        UUID lesson = uuid(144);

        insertProfessor(user, professor, "prof-reschedule-invalid@example.test", "00000000000013");
        insertStudent(student, professor);
        insertPerLessonPlan(plan, professor, student, 100.00, 3);
        insertLesson(lesson, professor, student, "2026-03-10T13:00:00Z");
        insertPerLessonPayment(uuid(145), professor, student, plan, lesson,
                100.00, "2026-03-10", "2026-03-13");

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> {
            jdbcTemplate.queryForObject(
                    "SELECT id_plano FROM plano_pagamento WHERE id_plano = ? FOR UPDATE",
                    UUID.class, plan
            );
            jdbcTemplate.queryForObject(
                    "SELECT id_aula FROM aula WHERE id_aula = ? FOR UPDATE",
                    UUID.class, lesson
            );
            jdbcTemplate.update(
                    "UPDATE aula SET inicio_em = ? WHERE id_aula = ?",
                    Timestamp.from(Instant.parse("2026-03-17T13:00:00Z")), lesson
            );
            return null;
        })).isInstanceOf(RuntimeException.class);
    }

    @Test
    void paidAndCanceledPaymentsRespectTheirFinalStates() {
        UUID user = uuid(150);
        UUID professor = uuid(151);
        UUID student = uuid(152);
        UUID plan = uuid(153);
        UUID paidPayment = uuid(154);
        UUID canceledPayment = uuid(155);

        insertProfessor(user, professor, "prof-final-payments@example.test", "00000000000014");
        insertStudent(student, professor);
        insertMonthlyPlan(plan, professor, student);
        insertPendingMonthlyPayment(paidPayment, professor, student, plan);

        jdbcTemplate.update(
                """
                UPDATE pagamento
                SET status = 'pago', data_pagamento = DATE '2026-01-10'
                WHERE id_pagamento = ?
                """,
                paidPayment
        );
        jdbcTemplate.update(
                "UPDATE pagamento SET descricao = 'Recebido', forma_pagamento = 'pix' WHERE id_pagamento = ?",
                paidPayment
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE pagamento SET valor = 200.00 WHERE id_pagamento = ?",
                paidPayment
        )).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE pagamento SET status = 'cancelado', data_pagamento = NULL WHERE id_pagamento = ?",
                paidPayment
        )).isInstanceOf(DataAccessException.class);

        jdbcTemplate.update(
                """
                INSERT INTO pagamento (
                    id_pagamento, professor_id, aluno_id, plano_id, valor,
                    vencimento_em, periodo_inicio, periodo_fim, status
                )
                VALUES (?, ?, ?, ?, 100.00, DATE '2026-02-10',
                        DATE '2026-02-01', DATE '2026-02-28', 'pendente')
                """,
                canceledPayment, professor, student, plan
        );
        jdbcTemplate.update(
                "UPDATE pagamento SET status = 'cancelado' WHERE id_pagamento = ?",
                canceledPayment
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE pagamento SET descricao = 'Não pode editar' WHERE id_pagamento = ?",
                canceledPayment
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void planConfigurationBecomesImmutableAfterFirstPaymentAndAfterClosing() {
        UUID user = uuid(160);
        UUID professor = uuid(161);
        UUID student = uuid(162);
        UUID plan = uuid(163);

        insertProfessor(user, professor, "prof-plan-immutable@example.test", "00000000000015");
        insertStudent(student, professor);
        insertMonthlyPlan(plan, professor, student);
        insertPendingMonthlyPayment(uuid(164), professor, student, plan);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE plano_pagamento SET valor = 150.00 WHERE id_plano = ?",
                plan
        )).isInstanceOf(DataAccessException.class);

        jdbcTemplate.update(
                "UPDATE plano_pagamento SET encerrado_em = DATE '2026-02-01' WHERE id_plano = ?",
                plan
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE plano_pagamento SET encerrado_em = NULL WHERE id_plano = ?",
                plan
        )).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE plano_pagamento SET inicio_em = DATE '2026-02-01' WHERE id_plano = ?",
                plan
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void activePlanWithoutPaymentsCanStillChangeItsConfiguration() {
        UUID user = uuid(170);
        UUID professor = uuid(171);
        UUID student = uuid(172);
        UUID plan = uuid(173);

        insertProfessor(user, professor, "prof-plan-editable@example.test", "00000000000016");
        insertStudent(student, professor);
        insertMonthlyPlan(plan, professor, student);

        int updated = jdbcTemplate.update(
                "UPDATE plano_pagamento SET valor = 125.00 WHERE id_plano = ?",
                plan
        );

        assertThat(updated).isEqualTo(1);
    }

    @Test
    void cancelingLessonRequiresCancelingItsPendingPaymentInSameTransaction() {
        UUID user = uuid(180);
        UUID professor = uuid(181);
        UUID student = uuid(182);
        UUID plan = uuid(183);
        UUID lesson = uuid(184);
        UUID payment = uuid(185);

        insertProfessor(user, professor, "prof-cancel-lesson@example.test", "00000000000017");
        insertStudent(student, professor);
        insertPerLessonPlan(plan, professor, student, 100.00, 3);
        insertLesson(lesson, professor, student, "2026-03-10T13:00:00Z");
        insertPerLessonPayment(payment, professor, student, plan, lesson,
                100.00, "2026-03-10", "2026-03-13");

        new TransactionTemplate(transactionManager).execute(status -> {
            jdbcTemplate.queryForObject(
                    "SELECT id_plano FROM plano_pagamento WHERE id_plano = ? FOR UPDATE",
                    UUID.class, plan
            );
            jdbcTemplate.queryForObject(
                    "SELECT id_aula FROM aula WHERE id_aula = ? FOR UPDATE",
                    UUID.class, lesson
            );
            jdbcTemplate.update(
                    "UPDATE aula SET status = 'cancelada' WHERE id_aula = ?",
                    lesson
            );
            jdbcTemplate.update(
                    "UPDATE pagamento SET status = 'cancelado' WHERE id_pagamento = ?",
                    payment
            );
            return null;
        });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM pagamento WHERE id_pagamento = ?",
                String.class, payment
        )).isEqualTo("cancelado");
    }

    @Test
    void canceledLessonCannotReceiveANewPendingPayment() {
        UUID user = uuid(190);
        UUID professor = uuid(191);
        UUID student = uuid(192);
        UUID plan = uuid(193);
        UUID lesson = uuid(194);

        insertProfessor(user, professor, "prof-no-charge-canceled@example.test", "00000000000018");
        insertStudent(student, professor);
        insertPerLessonPlan(plan, professor, student, 100.00, 3);
        insertLesson(lesson, professor, student, "2026-03-10T13:00:00Z");
        jdbcTemplate.update("UPDATE aula SET status = 'cancelada' WHERE id_aula = ?", lesson);

        assertThatThrownBy(() -> insertPerLessonPayment(uuid(195), professor, student,
                plan, lesson, 100.00, "2026-03-10", "2026-03-13"))
                .isInstanceOf(DataAccessException.class);
    }


    @Test
    void paidPerLessonPaymentDoesNotFollowLaterLessonRescheduling() {
        UUID user = uuid(200);
        UUID professor = uuid(201);
        UUID student = uuid(202);
        UUID plan = uuid(203);
        UUID lesson = uuid(204);
        UUID payment = uuid(205);

        insertProfessor(user, professor, "prof-paid-reschedule@example.test", "00000000000019");
        insertStudent(student, professor);
        insertPerLessonPlan(plan, professor, student, 100.00, 3);
        insertLesson(lesson, professor, student, "2026-03-10T13:00:00Z");
        insertPerLessonPayment(payment, professor, student, plan, lesson,
                100.00, "2026-03-10", "2026-03-13");
        jdbcTemplate.update(
                """
                UPDATE pagamento
                SET status = 'pago', data_pagamento = DATE '2026-03-13'
                WHERE id_pagamento = ?
                """,
                payment
        );

        new TransactionTemplate(transactionManager).execute(status -> {
            jdbcTemplate.queryForObject(
                    "SELECT id_plano FROM plano_pagamento WHERE id_plano = ? FOR UPDATE",
                    UUID.class, plan
            );
            jdbcTemplate.queryForObject(
                    "SELECT id_aula FROM aula WHERE id_aula = ? FOR UPDATE",
                    UUID.class, lesson
            );
            jdbcTemplate.update(
                    "UPDATE aula SET inicio_em = ? WHERE id_aula = ?",
                    Timestamp.from(Instant.parse("2026-03-17T13:00:00Z")), lesson
            );
            return null;
        });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT periodo_inicio FROM pagamento WHERE id_pagamento = ?",
                LocalDate.class, payment
        )).isEqualTo(LocalDate.parse("2026-03-10"));
    }

    @Test
    void closedPlanWithoutPaymentsCannotBeReactivatedOrReconfigured() {
        UUID user = uuid(210);
        UUID professor = uuid(211);
        UUID student = uuid(212);
        UUID plan = uuid(213);

        insertProfessor(user, professor, "prof-closed-empty-plan@example.test", "00000000000020");
        insertStudent(student, professor);
        insertMonthlyPlan(plan, professor, student);
        jdbcTemplate.update(
                "UPDATE plano_pagamento SET encerrado_em = DATE '2026-02-01' WHERE id_plano = ?",
                plan
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE plano_pagamento SET valor = 150.00 WHERE id_plano = ?",
                plan
        )).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE plano_pagamento SET encerrado_em = NULL WHERE id_plano = ?",
                plan
        )).isInstanceOf(DataAccessException.class);
    }


    @Test
    void concurrentMonthlyChargeCreationForSamePlanIsSerialized() throws Exception {
        UUID user = uuid(220);
        UUID professor = uuid(221);
        UUID student = uuid(222);
        UUID plan = uuid(223);

        insertProfessor(user, professor, "prof-concurrent-charge@example.test", "00000000000021");
        insertStudent(student, professor);
        insertMonthlyPlan(plan, professor, student);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Tempo esgotado aguardando início do teste concorrente.");
                }
                try {
                    new TransactionTemplate(transactionManager).execute(status -> {
                        insertPendingMonthlyPayment(uuid(224), professor, student, plan);
                        return null;
                    });
                    return true;
                } catch (DataIntegrityViolationException expectedDuplicate) {
                    return false;
                }
            });

            Future<Boolean> second = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Tempo esgotado aguardando início do teste concorrente.");
                }
                try {
                    new TransactionTemplate(transactionManager).execute(status -> {
                        insertPendingMonthlyPayment(uuid(225), professor, student, plan);
                        return null;
                    });
                    return true;
                } catch (DataIntegrityViolationException expectedDuplicate) {
                    return false;
                }
            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(java.util.List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            )).containsExactlyInAnyOrder(true, false);

            Integer activeCharges = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pagamento WHERE plano_id = ? AND status = 'pendente'",
                    Integer.class, plan
            );
            assertThat(activeCharges).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void databaseAcceptsAlphanumericCnpjAndRejectsInvalidFormats() {
        insertProfessor(uuid(300), uuid(301), "prof-cnpj-alfanumerico@example.test", "12ABC34501DE35");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT cnpj FROM professor WHERE id_professor = ?",
                String.class,
                uuid(301)
        )).isEqualTo("12ABC34501DE35");

        assertThatThrownBy(() -> insertProfessor(
                uuid(302), uuid(303), "prof-cnpj-minusculo@example.test", "12abc34501de35"))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> insertProfessor(
                uuid(304), uuid(305), "prof-cnpj-letra-final@example.test", "12ABC34501DE3A"))
                .isInstanceOf(DataAccessException.class);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name = ?
                """,
                Integer.class,
                tableName
        );

        return count != null && count == 1;
    }

    private void insertProfessor(UUID userId, UUID professorId, String email, String cnpj) {
        jdbcTemplate.update(
                "INSERT INTO usuario (id_usuario, email, senha_hash) VALUES (?, ?, ?)",
                userId, email, "test-hash"
        );

        jdbcTemplate.update(
                """
                INSERT INTO professor (id_professor, id_usuario, nome, razao_social, cnpj)
                VALUES (?, ?, 'Professor de teste', 'Razao social de teste', ?)
                """,
                professorId, userId, cnpj
        );
    }

    private void insertStudent(UUID studentId, UUID professorId) {
        jdbcTemplate.update(
                """
                INSERT INTO aluno (id_aluno, professor_id, nome, telefone, data_nascimento)
                VALUES (?, ?, 'Aluno ficticio', '11999990000', DATE '2000-01-01')
                """,
                studentId, professorId
        );
    }

    private void insertMonthlyPlan(UUID planId, UUID professorId, UUID studentId) {
        jdbcTemplate.update(
                """
                INSERT INTO plano_pagamento (
                    id_plano, professor_id, aluno_id, periodicidade, valor,
                    dia_vencimento, inicio_em
                )
                VALUES (?, ?, ?, 'mensal', 100.00, 10, DATE '2026-01-01')
                """,
                planId, professorId, studentId
        );
    }


    private void insertPerLessonPlan(
            UUID planId, UUID professorId, UUID studentId, double value, int dueDays) {
        jdbcTemplate.update(
                """
                INSERT INTO plano_pagamento (
                    id_plano, professor_id, aluno_id, periodicidade, valor,
                    prazo_vencimento_dias, inicio_em
                )
                VALUES (?, ?, ?, 'por_aula', ?, ?, DATE '2026-01-01')
                """,
                planId, professorId, studentId, value, dueDays
        );
    }

    private void insertLesson(
            UUID lessonId, UUID professorId, UUID studentId, String startInstant) {
        jdbcTemplate.update(
                """
                INSERT INTO aula (id_aula, professor_id, aluno_id, inicio_em, status)
                VALUES (?, ?, ?, ?, 'agendada')
                """,
                lessonId, professorId, studentId, Timestamp.from(Instant.parse(startInstant))
        );
    }

    private void insertPerLessonPayment(
            UUID paymentId, UUID professorId, UUID studentId, UUID planId, UUID lessonId,
            double value, String lessonDate, String dueDate) {
        jdbcTemplate.update(
                """
                INSERT INTO pagamento (
                    id_pagamento, professor_id, aluno_id, plano_id, aula_id, valor,
                    vencimento_em, periodo_inicio, periodo_fim, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?::date, ?::date, ?::date, 'pendente')
                """,
                paymentId, professorId, studentId, planId, lessonId, value,
                dueDate, lessonDate, lessonDate
        );
    }

    private void insertPendingMonthlyPayment(
            UUID paymentId, UUID professorId, UUID studentId, UUID planId) {
        jdbcTemplate.update(
                """
                INSERT INTO pagamento (
                    id_pagamento, professor_id, aluno_id, plano_id, valor,
                    vencimento_em, periodo_inicio, periodo_fim, status
                )
                VALUES (?, ?, ?, ?, 100.00, DATE '2026-01-10',
                        DATE '2026-01-01', DATE '2026-01-31', 'pendente')
                """,
                paymentId, professorId, studentId, planId
        );
    }

    private UUID uuid(int suffix) {
        return UUID.fromString(String.format(
                "018f0000-0000-7000-8000-%012d",
                suffix
        ));
    }
}
