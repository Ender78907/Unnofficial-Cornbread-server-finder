package ar.santi.cornbread.core;

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.*;
import java.util.function.*;

/** Continue paging until 40 unique verified matches, cancellation or an empty source page. */
public final class MatchedSearch {
    @FunctionalInterface public interface Source { List<ServerEntry> get(int skip,int limit) throws IOException,InterruptedException; }
    @FunctionalInterface public interface Verify { void run(List<ServerEntry> entries,Consumer<ServerVerifier.Progress> progress) throws InterruptedException; }
    @FunctionalInterface public interface Pause { void waitFor(long millis) throws InterruptedException; }
    public record Result(List<ServerEntry> matches,int nextSkip,boolean exhausted) {}
    public record Progress(int checked,int matched,ServerEntry match) {}
    public static Result run(Source source,Verify verify,int skip,Set<String> excluded,BooleanSupplier cancelled,
            Consumer<Progress> progress,IntConsumer cursor,IntConsumer retry,Pause pause) throws IOException,InterruptedException {
        List<ServerEntry> matches=new ArrayList<>();Set<String> seen=new HashSet<>(excluded);
        int[] checked={0};int attempt=0;
        while(matches.size()<40) {
            check(cancelled);
            int limit=40-matches.size();List<ServerEntry> page;
            try {page=source.get(skip,limit);attempt=0;}
            catch(HttpTimeoutException|SocketTimeoutException timeout) {
                check(cancelled);retry.accept(++attempt);pause.waitFor(Math.min(10000,1000L*attempt));continue;
            }
            check(cancelled);
            if(page.isEmpty())return new Result(List.copyOf(matches),skip,true);
            List<ServerEntry> candidates=new ArrayList<>();
            for(ServerEntry s:page)if(seen.add(s.address()))candidates.add(s);
            verify.run(candidates,p->{
                checked[0]++;
                ServerEntry match=p.match();
                if(match!=null && matches.size()<40)matches.add(match);else match=null;
                progress.accept(new Progress(checked[0],matches.size(),match));
            });
            check(cancelled);skip=Math.addExact(skip,limit);cursor.accept(skip);
            if(matches.size()<40)pause.waitFor(500);
        }
        return new Result(List.copyOf(matches),skip,false);
    }
    private static void check(BooleanSupplier cancelled) throws InterruptedException {
        if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedException();
    }
}
