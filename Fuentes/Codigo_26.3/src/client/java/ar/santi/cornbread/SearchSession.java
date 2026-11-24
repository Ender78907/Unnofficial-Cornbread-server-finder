package ar.santi.cornbread;

import static ar.santi.cornbread.core.Texts.t;

import ar.santi.cornbread.core.*;
import net.minecraft.client.Minecraft;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** One bounded job per screen. All callbacks and storage changes run on the game thread. */
final class SearchSession {
    private static final ExecutorService WORK = Executors.newFixedThreadPool(2, Thread.ofPlatform().daemon().name("cornbread-search-", 0).factory());
    private static final ExecutorService PINGS = Executors.newFixedThreadPool(12, Thread.ofPlatform().daemon().name("cornbread-ping-", 0).factory());
    private final AtomicInteger generation = new AtomicInteger();
    private final CornbreadApi api = new CornbreadApi();
    private Future<?> job;
    boolean busy;
    private long nextSearch;
    void retryNow() { nextSearch=0; }
    void cancel() { generation.incrementAndGet(); if (job != null) job.cancel(true); busy = false; }
    private void deliver(int token, Runnable r) {
        Minecraft.getInstance().execute(() -> { if (generation.get() == token) r.run(); });
    }
    void search(Filters filters, int protocol, int skip, Set<String> alreadyShown, Consumer<MatchedSearch.Result> success,
            Consumer<String> status, Consumer<MatchedSearch.Progress> progress, java.util.function.IntConsumer cursor) {
        if (busy) return;
        if (System.currentTimeMillis() < nextSearch) { status.accept(t("Esperá unos segundos entre búsquedas.", "Wait a few seconds between searches.")); return; }
        nextSearch = System.currentTimeMillis() + 5000;
        int token = generation.incrementAndGet(); busy = true;
        Set<String> excluded = new HashSet<>(FinderClient.STORE.data.blocked.keySet());excluded.addAll(alreadyShown);
        job = WORK.submit(() -> {
            try {
                deliver(token, () -> status.accept(t("Buscando servidores que coincidan…", "Searching for matching servers…")));
                MatchedSearch.Result result=MatchedSearch.run(
                    (offset,limit)->api.search(filters,offset,protocol,limit),
                    (entries,update)->ServerVerifier.verify(PINGS,StatusPing::check,entries,filters,protocol,excluded,
                        ()->generation.get()!=token,update),
                    skip,excluded,()->generation.get()!=token,
                    update->deliver(token,()->progress.accept(update)),
                    offset->deliver(token,()->cursor.accept(offset)),
                    attempt->deliver(token,()->status.accept(t("Consulta vencida; reintentando (", "Query timed out; retrying (")+attempt+")…")),
                    Thread::sleep);
                deliver(token, () -> { busy = false; success.accept(result); });
            } catch (InterruptedException cancelled) { Thread.currentThread().interrupt(); }
            catch (Exception e) {
                deliver(token, () -> { busy = false; FinderClient.STORE.save();status.accept(e.getMessage() == null ? t("Falló la búsqueda.", "Search failed.") : e.getMessage()); });
            }
        });
    }
    void refresh(List<ServerEntry> entries, Filters filters, int protocol, Consumer<List<ServerEntry>> success, Consumer<String> status, Consumer<ServerVerifier.Progress> progress) {
        if (busy) return;
        int token = generation.incrementAndGet(); busy = true;
        Set<String> excluded = Set.copyOf(FinderClient.STORE.data.blocked.keySet());
        job = WORK.submit(() -> {
            try {
                List<ServerEntry> live = verify(entries, filters, protocol, token, excluded, progress);
                deliver(token, () -> { busy = false; success.accept(live); });
            } catch (Exception e) { deliver(token, () -> { busy = false; status.accept(t("No se pudo actualizar la lista.", "Could not refresh the list.")); }); }
        });
    }
    void checkOne(ServerEntry server, int protocol, Consumer<ServerEntry> success, Consumer<String> failure) {
        if (busy) return;
        int token = generation.incrementAndGet(); busy = true;
        job = WORK.submit(() -> {
            Future<ServerEntry> ping = PINGS.submit(() -> StatusPing.check(server.copy(), protocol));
            try {
                ServerEntry result = ping.get();
                deliver(token, () -> { busy = false; success.accept(result); });
            } catch (Exception e) { deliver(token, () -> { busy = false; failure.accept(t("El servidor no responde. Probá otro.", "The server is not responding. Try another.")); }); }
            finally { if (!ping.isDone()) ping.cancel(true); }
        });
    }
    private List<ServerEntry> verify(List<ServerEntry> entries, Filters f, int protocol, int token,
            Set<String> excluded, Consumer<ServerVerifier.Progress> progress) throws InterruptedException {
        return ServerVerifier.verify(PINGS, StatusPing::check, entries, f, protocol, excluded,
            () -> generation.get() != token,
            update -> deliver(token, () -> progress.accept(update)));
    }
}
