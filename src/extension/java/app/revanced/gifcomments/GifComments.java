package app.revanced.gifcomments;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adds the native attachment field to the app's own comment request builder. */
public final class GifComments {
    private GifComments() {}
    private static final Pattern MARKER = Pattern.compile("\\[\\[gif:([1-9][0-9]{0,19})\\]\\]");
    private static final byte[] RESERVED = "[[gif".getBytes(StandardCharsets.UTF_8);

    public static Object rewrite(Object builder) {
        try {
            Object message = builder.getClass().getMethod("buildPartial").invoke(builder);
            byte[] bytes = (byte[]) message.getClass().getMethod("toByteArray").invoke(message);
            byte[] updated = rewritePayload(bytes);
            if (updated == bytes) return builder;
            // Resolve every method before mutating the builder. Unknown field 32 is
            // retained by the app's older protobuf implementation.
            Method clear = builder.getClass().getMethod("clear");
            Method merge = builder.getClass().getMethod("mergeFrom", byte[].class, int.class, int.class);
            clear.invoke(builder);
            merge.invoke(builder, updated, 0, updated.length);
            return builder;
        } catch (Exception failure) {
            // Never fall back to posting the reserved marker as plain text.
            throw new IllegalArgumentException("GIF comment preparation failed; request blocked", failure);
        }
    }

    public static byte[] rewritePayload(byte[] bytes) {
        if (!containsReserved(bytes)) return bytes;
        Cursor cursor = new Cursor(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String id = null;
        int textFields = 0;
        boolean alreadyAttached = false;
        while (cursor.offset < bytes.length) {
            int start = cursor.offset;
            long tag = cursor.varint();
            int field = (int) (tag >>> 3);
            int wire = (int) (tag & 7);
            if (field < 1 || field > 536870911) throw new IllegalArgumentException("Invalid protobuf tag");
            if (field == 32) alreadyAttached = true;
            byte[] value = cursor.value(wire);
            if (field == 3 && wire == 2) {
                textFields++;
                String text = new String(value, StandardCharsets.UTF_8);
                if (text.toLowerCase(java.util.Locale.ROOT).contains("[[gif")) {
                    Matcher match = MARKER.matcher(text);
                    if (!match.find()) throw new IllegalArgumentException("Use [[gif:NUMERIC_TENOR_ID]]");
                    id = match.group(1);
                    String remaining = (text.substring(0, match.start()) + text.substring(match.end())).trim();
                    if (remaining.toLowerCase(java.util.Locale.ROOT).contains("[[gif")) {
                        throw new IllegalArgumentException("Only one GIF marker is allowed");
                    }
                    // An empty text field is allowed alongside the attachment.
                    writeString(out, 3, remaining);
                    continue;
                }
            }
            out.write(bytes, start, cursor.offset - start);
        }
        if (id == null) return bytes; // Reserved bytes in a different field are unrelated.
        if (textFields != 1 || alreadyAttached) throw new IllegalArgumentException("Ambiguous GIF comment payload");
        writeString(out, 32, id);
        return out.toByteArray();
    }

    private static boolean containsReserved(byte[] bytes) {
        outer: for (int i = 0; i <= bytes.length - RESERVED.length; i++) {
            for (int j = 0; j < RESERVED.length; j++) {
                byte ch = bytes[i + j];
                if (ch >= 'A' && ch <= 'Z') ch += 32;
                if (ch != RESERVED[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static void writeString(ByteArrayOutputStream out, int field, String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        writeVarint(out, ((long) field << 3) | 2);
        writeVarint(out, encoded.length);
        out.write(encoded, 0, encoded.length);
    }

    private static void writeVarint(ByteArrayOutputStream out, long value) {
        while ((value & ~127L) != 0) {
            out.write(((int) value & 127) | 128);
            value >>>= 7;
        }
        out.write((int) value);
    }

    private static final class Cursor {
        final byte[] bytes;
        int offset;
        Cursor(byte[] bytes) { this.bytes = bytes; }
        long varint() {
            long value = 0;
            for (int shift = 0; shift < 64; shift += 7) {
                if (offset >= bytes.length) throw new IllegalArgumentException("Truncated protobuf varint");
                int next = bytes[offset++] & 255;
                if (shift == 63 && (next & 254) != 0) throw new IllegalArgumentException("Overflowed protobuf varint");
                value |= (long) (next & 127) << shift;
                if ((next & 128) == 0) return value;
            }
            throw new IllegalArgumentException("Overflowed protobuf varint");
        }
        byte[] value(int wire) {
            if (wire == 0) { varint(); return null; }
            long size;
            if (wire == 1) size = 8;
            else if (wire == 5) size = 4;
            else if (wire == 2) size = varint();
            else throw new IllegalArgumentException("Unsupported protobuf wire type");
            if (size < 0 || size > bytes.length - offset) throw new IllegalArgumentException("Truncated protobuf value");
            int begin = offset;
            offset += (int) size;
            return wire == 2 ? Arrays.copyOfRange(bytes, begin, offset) : null;
        }
    }
}
