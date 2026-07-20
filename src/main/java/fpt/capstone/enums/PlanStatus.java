package fpt.capstone.enums;

/**
 * Shared plan lifecycle for both money plans (FundUsagePlan) and item allocation plans.
 * DELETED is a soft-delete marker reachable only from PENDING_APPROVAL / REJECTED.
 */
public enum PlanStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    COMPLETED,
    CANCELLED,
    DELETED
}
