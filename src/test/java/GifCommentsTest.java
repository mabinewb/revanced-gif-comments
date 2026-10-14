import app.revanced.gifcomments.GifComments;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.UnknownFieldSet;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class GifCommentsTest {
    static int tests;
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        tests++;
    }
    static byte[] payload(String text) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CodedOutputStream coded = CodedOutputStream.newInstance(out);
        coded.writeString(2, "opaque-params");
        coded.writeString(3, text);
        coded.writeUInt64(40, -1L);
        coded.writeFixed32(41, 12345);
        coded.writeFixed64(42, 9876543210L);
        coded.flush();
        return out.toByteArray();
    }
    static String field(byte[] bytes, int number) throws Exception {
        return UnknownFieldSet.parseFrom(bytes).getField(number).getLengthDelimitedList().get(0).toStringUtf8();
    }
    static void rejects(String text) throws Exception {
        try { GifComments.rewritePayload(payload(text)); }
        catch (IllegalArgumentException expected) { tests++; return; }
        throw new AssertionError("Must reject: " + text);
    }
    public static void main(String[] args) throws Exception {
        byte[] plain = payload("일반 댓글 🙂");
        require(GifComments.rewritePayload(plain) == plain, "Ordinary comments must be byte-identical");
        byte[] before = payload("안녕하세요 🙂 [[gif:123456789]]");
        byte[] after = GifComments.rewritePayload(before);
        require(field(after, 3).equals("안녕하세요 🙂"), "Preserve Korean and emoji");
        require(field(after, 32).equals("123456789"), "Attachment field 32");
        require(field(after, 2).equals("opaque-params"), "Preserve opaque params");
        UnknownFieldSet oldFields = UnknownFieldSet.parseFrom(before);
        UnknownFieldSet newFields = UnknownFieldSet.parseFrom(after);
        for (int number : new int[]{40, 41, 42}) {
            require(oldFields.getField(number).equals(newFields.getField(number)), "Preserve wire field " + number);
        }
        require(field(GifComments.rewritePayload(payload("[[gif:123]]")), 3).isEmpty(), "GIF-only comment");
        rejects("[[gif:0]]");
        rejects("[[gif:abc]]");
        rejects("[[gif:123]");
        rejects("[[GIF:123]]");
        rejects("[[gif:123]] [[gif:456]]");
        rejects("[[gif:123456789012345678901]]");
        ByteArrayOutputStream duplicate = new ByteArrayOutputStream();
        duplicate.write(before);
        CodedOutputStream duplicateCoded = CodedOutputStream.newInstance(duplicate);
        duplicateCoded.writeString(32, "existing");
        duplicateCoded.flush();
        try { GifComments.rewritePayload(duplicate.toByteArray()); throw new AssertionError("Duplicate attachment"); }
        catch (IllegalArgumentException expected) { tests++; }
        // The old message schema does not know field 3 or 32. Exercise real protobuf
        // parsing, reflection, clear/merge and unknown-field serialization.
        Any.Builder oldBuilder = Any.newBuilder().mergeFrom(before);
        require(GifComments.rewrite(oldBuilder) == oldBuilder, "Native builder instance preserved");
        byte[] roundtrip = oldBuilder.buildPartial().toByteArray();
        require(field(roundtrip, 32).equals("123456789"), "Unknown attachment survives protobuf round trip");
        require(field(roundtrip, 3).equals("안녕하세요 🙂"), "Marker absent after builder round trip");
        Any.Builder normalBuilder = Any.newBuilder().mergeFrom(plain);
        byte[] normalBefore = normalBuilder.buildPartial().toByteArray();
        GifComments.rewrite(normalBuilder);
        require(Arrays.equals(normalBefore, normalBuilder.buildPartial().toByteArray()), "Normal builder unchanged");
        require(oldBuilder.getValue().equals(ByteString.copyFromUtf8("opaque-params")), "Known protobuf fields preserved");
        byte[] selected=GifComments.rewriteSelectedPayload(payload("안녕"),"안녕","14083772942551242087");
        require(field(selected,32).equals("14083772942551242087"),"Selected draft protobuf attachment");
        require(field(selected,3).equals("안녕"),"Selected draft protobuf text preserved");
        byte[] other=payload("다른 초안");require(GifComments.rewriteSelectedPayload(other,"안녕","123")==other,"Other draft protobuf unchanged");
        byte[] gifOnly=GifComments.rewriteSelectedPayload(payload("[GIF]"),"[GIF]","123",true);
        require(field(gifOnly,3).equals("") && field(gifOnly,32).equals("123"),"GIF-only reply removes UI placeholder in protobuf");
        require(field(GifComments.rewriteSelectedPayload(payload("[GIF]"),"[GIF]","123",false),3).equals("[GIF]"),"User-written same text preserved without GIF-only state");
        System.out.println("Passed " + tests + " assertions");
    }
}
