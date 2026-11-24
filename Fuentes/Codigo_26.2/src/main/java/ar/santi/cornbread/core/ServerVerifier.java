package ar.santi.cornbread.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Bounded parallel verification. Fast replies are delivered without waiting for slow peers. */
public final class ServerVerifier {
    @FunctionalInterface public interface Ping { ServerEntry check(ServerEntry server, int protocol) throws Exception; }
    public record Progress(int checked, int total, ServerEntry match) {}
    public static List<ServerEntry> verify(ExecutorService executor, Ping ping, List<ServerEntry> entries,
            Filters filters, int protocol, Set<String> blocked, BooleanSupplier cancelled,
            Consumer<Progress> progress) throws InterruptedException {
        List<ServerEntry> candidates = entries.stream().filter(s -> !blocked.contains(s.address()))
            .collect(java.util.stream.Collectors.toMap(ServerEntry::address, s -> s, (a,b) -> a, LinkedHashMap::new))
            .values().stream().limit(200).toList();
        CompletionService<ServerEntry> completed = new ExecutorCompletionService<>(executor);
        List<Future<ServerEntry>> tasks = new ArrayList<>();
        List<ServerEntry> result = new ArrayList<>();
        try {
            for (ServerEntry s : candidates) tasks.add(completed.submit(() -> {
                if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) return null;
                try {
                    ServerEntry live = ping.check(s.copy(), protocol);
                    return filters.matches(live) && (!filters.compatibleOnly || live.protocol == protocol) ? live : null;
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
                catch (Exception e) { return null; }
            }));
            for (int i=0;i<tasks.size();i++) {
                if (cancelled.getAsBoolean()) throw new InterruptedException();
                ServerEntry match;
                try { match = completed.take().get(); } catch (ExecutionException e) { match = null; }
                if (cancelled.getAsBoolean()) throw new InterruptedException();
                if (match != null) result.add(match);
                progress.accept(new Progress(i+1,tasks.size(),match));
            }
            return result;
        } finally { for (Future<?> task : tasks) if (!task.isDone()) task.cancel(true); }
    }
}
