package com.shkgroups.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

@Getter
public enum PlanId {

    AGENTE_IA_START("agente_ia_start", "AGENTE IA — Plano Start", new BigDecimal("99.90"),
            "start", "agente ia start", "agente-ia-start"),
    AGENTE_IA_PRO("agente_ia_pro", "AGENTE IA — Plano Pro", new BigDecimal("199.90"),
            "pro", "agente ia pro", "agente-ia-pro");

    private final String id;
    private final String title;
    private final BigDecimal price;
    private final Set<String> aliases;

    PlanId(String id, String title, BigDecimal price, String... aliases) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.aliases = Set.of(aliases);
    }

    private static final Map<String, PlanId> LOOKUP = buildLookup();

    private static Map<String, PlanId> buildLookup() {
        var map = new HashMap<String, PlanId>();
        for (var p : values()) {
            put(map, p, p.id);
            put(map, p, p.name());
            for (var a : p.aliases) put(map, p, a);
        }
        return Collections.unmodifiableMap(map);
    }

    private static void put(Map<String, PlanId> map, PlanId p, String raw) {
        if (raw == null) return;
        map.put(normalize(raw), p);
    }

    private static String normalize(String s) {
        if (s == null) return "";

        String v = s.strip().toLowerCase(Locale.ROOT);

        v = Normalizer.normalize(v, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        v = v.replace('_', '-');
        v = v.replaceAll("[\\p{Z}\\s]+", "-");
        v = v.replaceAll("[^a-z0-9-]", "");
        v = v.replaceAll("-{2,}", "-")
                .replaceAll("(^-)|(-$)", "");

        return v;
    }

    public static PlanId from(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("plan_required");
        var plan = LOOKUP.get(normalize(raw));
        if (plan == null) throw new IllegalArgumentException("invalid_plan");
        return plan;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PlanId json(String raw) {
        return from(raw);
    }

    @JsonValue
    public String jsonValue() {
        return id;
    }
}
