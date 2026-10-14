import app.revanced.gifcomments.GifTransport;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.json.JSONObject;

public final class GifTransportTest {
    public static final class Request {public String v(){return "https://www.youtube.com/youtubei/v1/comment/create_comment?key=example";}}
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    static JSONObject read(byte[] b)throws Exception{return new JSONObject(new String(b,StandardCharsets.UTF_8));}
    static void require(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args)throws Exception{
        Object owner=new Object();byte[] original=bytes("{\"commentText\":\"안녕\",\"context\":{\"client\":{\"clientName\":\"ANDROID\"}}}");
        GifTransport.arm(owner,"안녕","14083772942551242087");
        byte[] rewritten=GifTransport.rewrite(new Request(),Collections.emptyMap(),original);
        require(read(rewritten).getString("tenorId").equals("14083772942551242087"));
        require(read(rewritten).getString("commentText").equals("안녕"));
        require(read(rewritten).getJSONObject("context").getJSONObject("client").getString("clientName").equals("ANDROID"));
        require(!GifTransport.outstanding(owner));
        java.util.Map<String,String> oldHeaders=Collections.singletonMap("Content-Length","1");require(GifTransport.headers(oldHeaders,rewritten).get("Content-Length").equals(Integer.toString(rewritten.length)));require(oldHeaders.get("Content-Length").equals("1"));
        GifTransport.arm(owner,"다른 댓글","123");require(GifTransport.rewrite(new Request(),Collections.emptyMap(),original)==original);GifTransport.cancel(owner);
        GifTransport.arm(owner,"\u200B","123");JSONObject only=read(GifTransport.rewrite(new Request(),Collections.emptyMap(),bytes("{\"commentText\":\"\u200B\"}")));require(!only.has("commentText") && only.getString("tenorId").equals("123"));
        String reply="https://www.youtube.com/youtubei/v1/comment/create_comment_reply";
        GifTransport.arm(owner,"[GIF]","14083772942551242087",true);
        JSONObject replyOnly=read(GifTransport.rewrite(reply,Collections.emptyMap(),bytes("{\"commentText\":\"[GIF]\",\"replyParams\":\"opaque\"}")));
        require(!replyOnly.has("commentText") && replyOnly.getString("tenorId").equals("14083772942551242087") && replyOnly.getString("replyParams").equals("opaque"));
        GifTransport.arm(owner,"[GIF]","123",false);
        require(read(GifTransport.rewrite(reply,Collections.emptyMap(),bytes("{\"commentText\":\"[GIF]\"}"))).getString("commentText").equals("[GIF]"));
        GifTransport.arm(owner,"[GIF]","123",true);
        byte[] different=bytes("{\"commentText\":\"실제 텍스트\"}");require(GifTransport.rewrite(reply,Collections.emptyMap(),different)==different);GifTransport.cancel(owner);
        ByteArrayOutputStream compressed=new ByteArrayOutputStream();try(GZIPOutputStream gzip=new GZIPOutputStream(compressed)){gzip.write(original);}
        GifTransport.arm(owner,"안녕","123");byte[] response=GifTransport.rewrite("https://www.youtube.com/youtubei/v1/comment/create_comment_reply",Collections.emptyMap(),compressed.toByteArray());
        ByteArrayOutputStream plain=new ByteArrayOutputStream();try(GZIPInputStream input=new GZIPInputStream(new ByteArrayInputStream(response))){byte[] buffer=new byte[1000];int n;while((n=input.read(buffer))!=-1)plain.write(buffer,0,n);}
        require(read(plain.toByteArray()).getString("tenorId").equals("123"));
        try{GifTransport.rewrite(new Request(),Collections.emptyMap(),bytes("{\"commentText\":\"[[gif:123]]\"}"));throw new AssertionError("Marker must never be sent");}catch(IllegalArgumentException expected){}
        GifTransport.arm(owner,"안녕","123");require(GifTransport.rewrite("https://www.youtube.com/youtubei/v1/player",Collections.emptyMap(),original)==original);GifTransport.cancel(owner);
        System.out.println("Transport JSON, gzip, draft isolation and marker blocking checks passed");
    }
}
