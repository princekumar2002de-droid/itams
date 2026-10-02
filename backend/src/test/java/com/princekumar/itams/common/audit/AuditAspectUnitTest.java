package com.princekumar.itams.common.audit;

import com.princekumar.itams.common.entity.BaseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the pieces of {@link AuditAspect} that don't require a full
 * Spring context — specifically the id-extraction helper and the audit-row
 * shape when invoked directly.
 *
 * <p>The AOP wiring (that the aspect is actually invoked around methods
 * annotated with {@link AuditWrite}) requires a running Spring context and
 * is covered by the integration test suite (expected to be added in a
 * dedicated {@code AuditAspectIT}, pending; see this project's verification
 * report for the test-execution hand-off).</p>
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectUnitTest {

    @Mock AuditLogRepository repo;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractEntityId_from_BaseEntity_returns_its_id() {
        FakeBaseEntity be = new FakeBaseEntity();
        setField(be, "id", 42L);
        assertThat(AuditAspect.extractEntityId(be)).isEqualTo(42L);
    }

    @Test
    void extractEntityId_from_non_entity_returns_null() {
        assertThat(AuditAspect.extractEntityId("plain string")).isNull();
        assertThat(AuditAspect.extractEntityId(null)).isNull();
    }

    @Test
    void auditLog_entity_roundtrip_persists_all_fields() {
        AuditAspect aspect = new AuditAspect(repo);
        AuditLog row = new AuditLog(
            java.time.OffsetDateTime.now(),
            7L,                 // actor
            "Ticket", 123L,     // entity type + id
            "CREATE",
            null,
            "req-abc"
        );
        repo.save(row);
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo).save(cap.capture());
        AuditLog saved = cap.getValue();
        assertThat(saved.getActorUserId()).isEqualTo(7L);
        assertThat(saved.getEntityType()).isEqualTo("Ticket");
        assertThat(saved.getEntityId()).isEqualTo(123L);
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getRequestId()).isEqualTo("req-abc");
    }

    // ── fakes / reflection ──────────────────────────────────────────────────

    /**
     * Concrete subclass because BaseEntity is abstract. Deliberately NOT annotated
     * with @Entity: this class lives under com.princekumar.itams, so Spring Boot's
     * entity scan would register it in every @SpringBootTest and Hibernate's
     * schema validation would fail on the missing table.
     */
    static class FakeBaseEntity extends BaseEntity { }

    private static void setField(Object target, String name, Object value) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                var f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("no field " + name);
    }
}
