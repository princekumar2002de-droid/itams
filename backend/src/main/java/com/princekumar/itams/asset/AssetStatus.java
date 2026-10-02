package com.princekumar.itams.asset;

/**
 * Lifecycle status of a physical asset. Enforced by a CHECK constraint on
 * the DB column {@code asset.status} and by state-machine logic in
 * {@link AssetService}.
 *
 * <pre>
 *   IN_STOCK  ──assign──►  ASSIGNED  ──return──►  IN_STOCK
 *                                  └─return+flag─►  UNDER_MAINTENANCE
 *                                                        │
 *                                    ◄─maintenance done──┘
 *   IN_STOCK ─retire──►  RETIRED   (terminal)
 *   IN_STOCK ─mark lost►  LOST      (terminal)
 * </pre>
 */
public enum AssetStatus {
    IN_STOCK,
    ASSIGNED,
    UNDER_MAINTENANCE,
    RETIRED,
    LOST
}
