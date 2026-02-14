package com.shkgroups.common;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    private static final Map<String, PlanId> LOOKUP = new HashMap<>();

    static {
        for (var p : values()) {
            // chaves "oficiais"
            LOOKUP.put(normalize(p.id), p);
            LOOKUP.put(normalize(p.name()), p);

            // aliases curtos
            if (p == AGENTE_IA_START) {
                LOOKUP.put(normalize("start"), p);
                LOOKUP.put(normalize("agente ia start"), p);
                LOOKUP.put(normalize("agente_ia_start"), p);
                LOOKUP.put(normalize("agente-ia-start"), p);
            }
            if (p == AGENTE_IA_PRO) {
                LOOKUP.put(normalize("pro"), p);
                LOOKUP.put(normalize("agente ia pro"), p);
                LOOKUP.put(normalize("agente_ia_pro"), p);
                LOOKUP.put(normalize("agente-ia-pro"), p);
            }
        }
    }

    public static PlanId from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("plan_is_required");
        }
        var key = normalize(raw);
        var plan = LOOKUP.get(key);
        if (plan == null) {
            throw new IllegalArgumentException("invalid_plan: " + raw);
        }
        return plan;
    }

    private static String normalize(String s) {
        // padroniza: lower + troca separadores + remove lixo duplicado
        var v = s.trim().toLowerCase(Locale.ROOT);
        v = v.replace('-', '_').replace(' ', '_');
        while (v.contains("__")) v = v.replace("__", "_");
        return v;
    }
}
