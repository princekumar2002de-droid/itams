package com.princekumar.itams.stats;

import com.princekumar.itams.stats.dto.CountByLabel;
import com.princekumar.itams.stats.dto.DashboardStatsResponse;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Single query-driven service that aggregates dashboard KPIs.
 *
 * <p>Uses native SQL rather than JPQL because we're doing group-bys and
 * joins across tables that don't all have JPA entities in this project
 * (license tables' entities land alongside this milestone). Native SQL
 * is fine here — it's read-only and shaped to Postgres 16.</p>
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private final EntityManager em;

    public StatsService(EntityManager em) { this.em = em; }

    public DashboardStatsResponse dashboard() {
        List<CountByLabel> byStatus = countByLabel(
            "SELECT status AS label, COUNT(*) AS c FROM asset GROUP BY status ORDER BY status");
        List<CountByLabel> byCategory = countByLabel("""
            SELECT c.name AS label, COUNT(a.*) AS c
              FROM asset_category c
              LEFT JOIN asset_model m ON m.category_id = c.id
              LEFT JOIN asset a ON a.model_id = m.id
             GROUP BY c.name
             ORDER BY c.name
            """);
        List<CountByLabel> byDept = countByLabel("""
            SELECT d.code AS label, COUNT(DISTINCT aa.asset_id) AS c
              FROM asset_assignment aa
              JOIN employee e ON e.person_id = aa.assignee_person_id
              JOIN department d ON d.id = e.department_id
             WHERE aa.actual_return_at IS NULL
             GROUP BY d.code
             ORDER BY d.code
            """);
        List<CountByLabel> ticketsByPriority = countByLabel(
            "SELECT priority AS label, COUNT(*) AS c FROM ticket WHERE status <> 'CLOSED' GROUP BY priority");
        List<CountByLabel> ticketsByStatus = countByLabel(
            "SELECT status AS label, COUNT(*) AS c FROM ticket GROUP BY status");

        long total   = sum(byStatus);
        long inStock = pick(byStatus, "IN_STOCK");
        long assigned = pick(byStatus, "ASSIGNED");
        long maint    = pick(byStatus, "UNDER_MAINTENANCE");
        long retired  = pick(byStatus, "RETIRED");

        long employees = single("SELECT COUNT(*) FROM employee");
        long openTickets = single("SELECT COUNT(*) FROM ticket WHERE status <> 'CLOSED'");
        long openAssignments = single("SELECT COUNT(*) FROM asset_assignment WHERE actual_return_at IS NULL");
        long expiringSoon = single(
            "SELECT COUNT(*) FROM software_license WHERE expires_on IS NOT NULL AND expires_on <= :cutoff",
            "cutoff", LocalDate.now().plusDays(60));

        return new DashboardStatsResponse(
            total, inStock, assigned, maint, retired,
            employees, openTickets, openAssignments, expiringSoon,
            byStatus, byCategory, byDept, ticketsByPriority, ticketsByStatus
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<CountByLabel> countByLabel(String sql) {
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        return rows.stream()
            .map(r -> new CountByLabel(String.valueOf(r[0]), ((Number) r[1]).longValue()))
            .toList();
    }

    private long single(String sql, Object... paramKV) {
        var q = em.createNativeQuery(sql);
        for (int i = 0; i + 1 < paramKV.length; i += 2) {
            q.setParameter((String) paramKV[i], paramKV[i + 1]);
        }
        return ((Number) q.getSingleResult()).longValue();
    }

    private static long sum(List<CountByLabel> buckets) {
        return buckets.stream().mapToLong(CountByLabel::count).sum();
    }

    private static long pick(List<CountByLabel> buckets, String label) {
        return buckets.stream().filter(b -> label.equals(b.label())).mapToLong(CountByLabel::count).findFirst().orElse(0L);
    }
}
