package com.princekumar.itams.common.audit;

import com.princekumar.itams.auth.CurrentUser;
import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;

/**
 * AOP aspect that writes one {@code audit_log} row after every successful
 * invocation of a service method annotated with {@link AuditWrite}.
 *
 * <h3>Transaction semantics</h3>
 *
 * <p>By default in Spring, {@code @Transactional} runs at
 * {@link Ordered#LOWEST_PRECEDENCE} and is therefore the <b>innermost</b>
 * aspect. Any user-defined {@code @Around} aspect wraps outside it. That
 * means {@link #around(ProceedingJoinPoint)} executes <b>after</b> the
 * business transaction has committed, and {@link #saveAudit} runs in its
 * own, separate transaction.</p>
 *
 * <p>Consequence: a failed audit insert does <b>not</b> roll back the
 * already-committed business change. We accept that trade-off here (one
 * missing audit row is better than losing a legitimate business write),
 * log the failure at ERROR level, and keep the business change. A future
 * iteration can invert the ordering via
 * {@code @EnableTransactionManagement(order = …)} so that a change can never
 * be committed without its audit row.</p>
 *
 * <h3>What is recorded</h3>
 * <ul>
 *   <li>{@code actor_user_id} from {@link CurrentUser#id()} (falls back to
 *       the seeded system user when no auth is present).</li>
 *   <li>{@code entity_id} extracted from the return value when it is a
 *       {@link BaseEntity}; otherwise 0 with a WARN log line.</li>
 *   <li>{@code changes} is left null; before/after values are not recorded yet.</li>
 *   <li>{@code request_id} from the {@code X-Request-Id} HTTP header
 *       when the call is inside a request context.</li>
 * </ul>
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)  // OUTER of @Transactional; see JavaDoc.
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository repo;

    public AuditAspect(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Around("@annotation(com.princekumar.itams.common.audit.AuditWrite)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();     // business write + commit happen here

        try {
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();
            AuditWrite ann = method.getAnnotation(AuditWrite.class);
            if (ann == null) return result;   // defensive

            Long entityId = extractEntityId(result);
            if (entityId == null) {
                log.warn("Audit: no BaseEntity id for {}; recording as entity_id=0",
                    pjp.getSignature().toShortString());
            }
            saveAudit(
                ann.entity(),
                entityId != null ? entityId : 0L,
                ann.action(),
                CurrentUser.id(),
                currentRequestId()
            );
        } catch (RuntimeException ex) {
            // Do NOT re-throw — the business change already committed (aspect
            // runs outside @Transactional). Log and move on.
            log.error("Audit insert failed for {}; business change retained.",
                pjp.getSignature().toShortString(), ex);
        }

        return result;
    }

    /**
     * Save the audit row. {@link org.springframework.data.repository.CrudRepository#save}
     * on a Spring Data repository is itself {@code @Transactional} (declared on the
     * generated proxy), so this call opens its own fresh transaction when the
     * aspect runs outside of any active business transaction — see the class JavaDoc.
     */
    void saveAudit(String entityType, long entityId, String action,
                   Long actorUserId, String requestId) {
        repo.save(new AuditLog(
            OffsetDateTime.now(),
            actorUserId,
            entityType,
            entityId,
            action,
            null,
            requestId
        ));
    }

    /** Pull an id out of a return value if it's a BaseEntity; else null. */
    static Long extractEntityId(Object returnValue) {
        if (returnValue instanceof BaseEntity be) {
            return be.getId();
        }
        return null;
    }

    private static String currentRequestId() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            String h = req.getHeader("X-Request-Id");
            if (h != null && !h.isBlank()) {
                return h.length() > 60 ? h.substring(0, 60) : h;
            }
        }
        return null;
    }
}
