package ar.santi.cornbread.core;
import java.nio.ByteBuffer;
import java.util.Base64;
public final class ServerIcon {
    private ServerIcon() {}
    public static byte[] decode(String data) {
        if(data==null || !data.startsWith("data:image/png;base64,") || data.length()>65536) return null;
        try {
            byte[] bytes=Base64.getDecoder().decode(data.substring(22));
            if(bytes.length<24) return null;
            ByteBuffer b=ByteBuffer.wrap(bytes);
            if(b.getLong()!=0x89504e470d0a1a0aL || b.getInt()!=13 || b.getInt()!=0x49484452 || b.getInt()!=64 || b.getInt()!=64)return null;
            return bytes;
        } catch(IllegalArgumentException e) { return null; }
    }
}
