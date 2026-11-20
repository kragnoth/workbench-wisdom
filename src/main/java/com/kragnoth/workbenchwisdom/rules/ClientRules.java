package com.kragnoth.workbenchwisdom.rules;

/** The rules the server last sent this client (on joining and after each {@code /reload}). */
public final class ClientRules {
    private ClientRules() {}

    private static volatile BenchRules rules = BenchRules.EMPTY;

    public static BenchRules get() {
        return rules;
    }

    static void set(BenchRules r) {
        rules = r;
    }
}
