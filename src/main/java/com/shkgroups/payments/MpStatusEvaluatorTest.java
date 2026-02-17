package com.shkgroups.payments;

import com.shkgroups.payments.domain.MpDecision;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MpStatusEvaluatorTest {

    @Test
    public void shouldReturnProvisionForApproved() {
        assertEquals(MpDecision.PROVISION, MpStatusEvaluator.decisionFor("approved"));
    }

    @Test
    public void shouldReturnWaitPaymentForPendingStatuses() {
        assertEquals(MpDecision.WAIT_PAYMENT, MpStatusEvaluator.decisionFor("pending"));
        assertEquals(MpDecision.WAIT_PAYMENT, MpStatusEvaluator.decisionFor("in_process"));
        assertEquals(MpDecision.WAIT_PAYMENT, MpStatusEvaluator.decisionFor("authorized"));
    }

    @Test
    public void shouldReturnDenyForFailedStatuses() {
        assertEquals(MpDecision.DENY, MpStatusEvaluator.decisionFor("rejected"));
        assertEquals(MpDecision.DENY, MpStatusEvaluator.decisionFor("cancelled"));
        assertEquals(MpDecision.DENY, MpStatusEvaluator.decisionFor("refunded"));
    }

    @Test
    public void shouldIgnoreUnknownOrBlankStatus() {
        assertEquals(MpDecision.IGNORE, MpStatusEvaluator.decisionFor(""));
        assertEquals(MpDecision.IGNORE, MpStatusEvaluator.decisionFor("something_else"));
        assertEquals(MpDecision.IGNORE, MpStatusEvaluator.decisionFor(null));
    }
}
