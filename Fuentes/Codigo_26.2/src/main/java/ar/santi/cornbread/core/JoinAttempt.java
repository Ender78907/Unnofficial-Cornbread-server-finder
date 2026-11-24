package ar.santi.cornbread.core;

import java.text.Normalizer;
import java.util.Locale;

/** Tracks only the initial join, using its initiating screen as an identity token. */
public final class JoinAttempt {
    public record Failure(ServerEntry server, boolean retryAutomatically) {}
    private ServerEntry pending;
    private Object parent;
    private boolean automatic;

    public void begin(ServerEntry server, Object parent, boolean automatic) {
        this.pending = server.copy(); this.parent = parent; this.automatic = automatic;
    }
    public ServerEntry pending() { return pending; }
    public Object parent() { return parent; }
    public void cancel() { pending = null; parent = null; automatic = false; }

    /** Consumes the attempt once. Blocking is deferred until the cancellable retry runs. */
    public Failure failed(FinderStore store, Object parent, String reason, boolean whitelistKey) {
        if (pending == null || this.parent != parent) return null;
        ServerEntry server = pending;
        boolean retry = automatic && store.data.retryFailedAutoJoin
            && (store.data.retryWhitelist || !(whitelistKey || isWhitelist(reason)));
        cancel();
        store.failed(server, reason);
        return new Failure(server, retry);
    }

    /** Text fallback for literal plugin messages; vanilla translations are checked by key. */
    public static boolean isWhitelist(String text) {
        if (text == null) return false;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKD)
            .toLowerCase(Locale.ROOT).replaceAll("§.", "")
            .replaceAll("[\\p{M}\\s_-]", "");
        return normalized.contains("whitelist") || normalized.contains("allowlist")
            || normalized.contains("listablanca") || normalized.contains("listadepermitidos")
            || normalized.contains("listadeadmitidos");
    }
}
