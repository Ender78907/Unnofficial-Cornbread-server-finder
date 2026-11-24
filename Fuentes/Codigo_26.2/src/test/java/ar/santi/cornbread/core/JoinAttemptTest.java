package ar.santi.cornbread.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class JoinAttemptTest {
    @TempDir Path temp;
    private final Object screen = new Object();
    private ServerEntry server() { var s=new ServerEntry();s.host="127.0.0.1";s.port=25565;return s; }
    private FinderStore store(boolean enabled,boolean whitelist) {
        var store=new FinderStore(temp.resolve("state.json"));
        store.data.retryFailedAutoJoin=enabled;store.data.retryWhitelist=whitelist;return store;
    }
    private JoinAttempt attempt(boolean automatic) {
        var attempt=new JoinAttempt();attempt.begin(server(),screen,automatic);return attempt;
    }
    @Test void disabledByDefaultAndOldConfigurationStillLoads() throws Exception {
        Path path=temp.resolve("old.json");Files.writeString(path,"{\"language\":\"es\"}");
        var store=new FinderStore(path);
        assertFalse(store.data.retryFailedAutoJoin);assertFalse(store.data.retryWhitelist);
        assertFalse(attempt(true).failed(store,screen,"Connection refused",false).retryAutomatically());
    }
    @Test void optionsSurviveRestart() {
        var store=store(true,true);store.save();var loaded=new FinderStore(store.path);
        assertTrue(loaded.data.retryFailedAutoJoin);assertTrue(loaded.data.retryWhitelist);
    }
    @Test void failedAutoJoinRetriesButDoesNotBlockBeforeCountdown() {
        var store=store(true,false);var attempt=attempt(true);
        var failure=attempt.failed(store,screen,"Connection timed out",false);
        assertTrue(failure.retryAutomatically());assertEquals(server().address(),failure.server().address());
        assertFalse(store.isBlocked(server()));assertTrue(store.data.history.isEmpty());
        assertEquals(1,store.data.failures.get(server().address()).count);
        assertNull(attempt.pending());assertNull(attempt.parent());
    }
    @Test void manualJoinDoesNotRetryEvenWithBothOptionsEnabled() {
        assertFalse(attempt(false).failed(store(true,true),screen,"Connection refused",false).retryAutomatically());
    }
    @ParameterizedTest @ValueSource(strings={"You are not white-listed on this server!","Not whitelisted",
        "Whitelist required", "Not on the allow list", "§cNo estás en la lista blanca",
        "No estás en la lista de permitidos", "No estás en la lista de admitidos"})
    void whitelistRejectionsAreExemptByDefault(String reason) {
        var store=store(true,false);
        assertFalse(attempt(true).failed(store,screen,reason,false).retryAutomatically());
        assertFalse(store.isBlocked(server()));assertEquals(1,store.data.failures.size());
    }
    @Test void translatedWhitelistKeyProtectsUnrecognizedLanguage() {
        assertFalse(attempt(true).failed(store(true,false),screen,"非公開サーバー",true).retryAutomatically());
    }
    @Test void optInAllowsWhitelistRejection() {
        assertTrue(attempt(true).failed(store(true,true),screen,"Not whitelisted",true).retryAutomatically());
    }
    @Test void whitelistOptionAloneDoesNotEnableRetry() {
        assertFalse(attempt(true).failed(store(false,true),screen,"Not whitelisted",true).retryAutomatically());
    }
    @Test void unrelatedFailureCannotConsumeTheActiveAttempt() {
        var store=store(true,false);var attempt=attempt(true);
        assertNull(attempt.failed(store,new Object(),"Connection refused",false));
        assertNotNull(attempt.pending());assertTrue(store.data.failures.isEmpty());
        assertTrue(attempt.failed(store,screen,"Connection refused",false).retryAutomatically());
    }
    @Test void repeatedScreenInitializationDoesNotRepeatFailure() {
        var store=store(true,false);var attempt=attempt(true);
        assertNotNull(attempt.failed(store,screen,"Bad packet",false));
        assertNull(attempt.failed(store,screen,"Bad packet",false));
        assertEquals(1,store.data.failures.get(server().address()).count);
    }
    @Test void cancellationOrCompletedJoinDisarmsRecovery() {
        var store=store(true,true);var attempt=attempt(true);attempt.cancel();
        assertNull(attempt.failed(store,screen,"Not whitelisted",true));
        assertTrue(store.data.failures.isEmpty());assertTrue(store.data.blocked.isEmpty());
    }
    @Test void newManualAttemptDoesNotInheritAutomaticMode() {
        var store=store(true,true);var attempt=attempt(true);attempt.begin(server(),screen,false);
        assertFalse(attempt.failed(store,screen,"Disconnected",false).retryAutomatically());
    }
    @Test void attemptKeepsItsOwnServerSnapshot() {
        var server=server();var attempt=new JoinAttempt();attempt.begin(server,screen,true);server.port=25566;
        assertEquals(25565,attempt.failed(store(true,false),screen,"Refused",false).server().port);
    }
    @ParameterizedTest @ValueSource(strings={"Timed out", "Connection refused", "Unknown custom payload moonlight:map_marker"})
    void nonWhitelistFailuresRemainEligible(String reason) {
        assertTrue(attempt(true).failed(store(true,false),screen,reason,false).retryAutomatically());
    }
}
