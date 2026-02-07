package com.shkgroups.common;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum PlanId {

    AGENTE_IA_START("agente_ia_start", "AGENTE IA — Plano Start", new BigDecimal("99.90")),
    AGENTE_IA_PRO("agente_ia_pro", "AGENTE IA — Plano Pro", new BigDecimal("199.90"));

    private final String id;
    private final String title;
    private final BigDecimal price;

    PlanId(String id, String title, BigDecimal price) {
        this.id = id;
        this.title = title;
        this.price = price;
    }

    public static PlanId from(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("plan_is_required");

        String v = raw.trim().toLowerCase();

        if (v.equals("start") || v.equals("agente_ia_star") || v.equals("agente-ia-start")) v = "agente_ia_start";
        if (v.equals("pro") || v.equals("agente-ia-pro")) v = "agente_ia_pro";

        for (var p : values()) {
            if (p.id.equalsIgnoreCase(v)) return p;
        }
        throw new IllegalArgumentException("invalid_plan: " + raw);
    }
}
