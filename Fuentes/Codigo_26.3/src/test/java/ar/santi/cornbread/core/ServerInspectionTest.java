package ar.santi.cornbread.core;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ServerInspectionTest {
    private ServerInspection.Result inspect(String json) {
        return ServerInspection.inspect(JsonParser.parseString(json).getAsJsonObject());
    }
    @Test void missingFieldsRemainUnknown() {
        var r=inspect("{}");
        assertEquals("Unknown",r.software()); assertEquals("Unknown",r.protocol());
        assertEquals("No mod list announced",r.mods());
    }
    @Test void paperIsOnlyAVersionTextHint() {
        var r=inspect("{\"version\":{\"name\":\"Paper 26.2\",\"protocol\":999}}");
        assertEquals("Paper 26.2",r.software()); assertEquals("999",r.protocol());
        assertEquals("Medium (version text)",r.confidence());
        assertEquals("Unknown",r.modLoader());
    }
    @Test void forgeAndNeoForgeAdvertisedModsAreParsed() {
        var r=inspect("{\"version\":{\"name\":\"NeoForge\"},\"modinfo\":{\"type\":\"neoforge\",\"modList\":[{\"modid\":\"moonlight\"}]},\"forgeData\":{\"mods\":[{\"modId\":\"example\"}]}}");
        assertEquals("NeoForge / Forge-family",r.modLoader());
        assertEquals("moonlight, example",r.mods());
        assertEquals("High (modded status payload)",r.confidence());
    }
    @Test void fabricAndProxyHintsAreRecognized() {
        assertEquals("Fabric",inspect("{\"version\":{\"name\":\"Fabric 26.3\"}}").modLoader());
        assertEquals("Velocity",inspect("{\"version\":{\"name\":\"Velocity\"}}").proxy());
    }
    @Test void malformedOptionalFieldsDoNotCrash() {
        var r=inspect("{\"version\":[],\"modinfo\":null,\"forgeData\":{\"mods\":[null,1,{}, {\"id\":\"test\"}]}}");
        assertEquals("Unknown",r.version()); assertEquals("test",r.mods());
    }
}
