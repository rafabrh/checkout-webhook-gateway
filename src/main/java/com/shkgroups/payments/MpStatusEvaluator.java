package com.shkgroups.payments;

final class MpStatusEvaluator {

    private MpStatusEvaluator() {

    }

    static MpDecision decisionFor(String mpStatus) {
        if (mpStatus == null || mpStatus.isEmpty()) {
            return MpDecision.IGNORE;
        }

        return switch (mpStatus.toLowerCase()) {
            case "approved" -> MpDecision.PROVISION;
            case "pending", "in_process", "authorized" -> MpDecision.WAIT_PAYMENT;
            case "rejected", "cancelled", "refunded", "charged_back" -> MpDecision.DENY;
            default -> MpDecision.IGNORE;

        };
    }
}