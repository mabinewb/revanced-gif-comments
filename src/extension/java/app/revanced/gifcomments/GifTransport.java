package app.revanced.gifcomments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.json.JSONObject;

/** The only armed attachment is the draft whose native send button was pressed. */
public final class GifTransport {
    private static volatile Pending pending;
    private static final class Pending {
        final Object owner;final String text,id;final long deadline;
        Pending(Object owner,String text,String id,long deadline){this.owner=owner;this.text=text;this.id=id;this.deadline=deadline;}
    }
    private static long now(){return System.nanoTime()/1000000;}
    public static void arm(Object owner,String text,String id){pending=new Pending(owner,text,id,now()+15000);}
    public static void armDraft(Object owner,String text,String id){pending=new Pending(owner,text,id,Long.MAX_VALUE);}
    public static void cancelAll(){pending=null;}
    public static boolean outstanding(Object owner){Pending p=pending;return p!=null && p.owner==owner;}
    public static void cancel(Object owner){if(outstanding(owner))pending=null;}
    public static Map<?,?> headers(Map<?,?> original,byte[] body){
        if(original==null || body==null)return original;
        for(Object key:original.keySet())if(key instanceof String && ((String)key).equalsIgnoreCase("Content-Length")){
            Map<Object,Object> copy=new HashMap<Object,Object>(original);copy.put(key,Integer.toString(body.length));return copy;
        }
        return original;
    }
    public static byte[] rewrite(Object request,Map<?,?> headers,byte[] body) {
        if(body==null)return body;
        String url=url(request);
        if(!url.contains("/comment/"))return body;
        boolean create=url.contains("/comment/create_comment?") || url.endsWith("/comment/create_comment") || url.contains("/comment/create_comment_reply?") || url.endsWith("/comment/create_comment_reply");
        try {
            boolean compressed=body.length>2 && (body[0]&255)==31 && (body[1]&255)==139;
            byte[] raw=body;
            if(compressed){try(GZIPInputStream input=new GZIPInputStream(new ByteArrayInputStream(body));ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=input.read(b))!=-1){if(output.size()+n>4*1024*1024)throw new IllegalArgumentException("Comment request too large");output.write(b,0,n);}raw=output.toByteArray();}}
            if(GifComments.hasReserved(raw))throw new IllegalArgumentException("Manual GIF marker blocked: use the GIF button");
            Pending p=pending;
            if(!create || p==null || now()>p.deadline)return body;
            byte[] updated=raw;
            String json=new String(raw,StandardCharsets.UTF_8).trim();
            if(json.startsWith("{")) {
                JSONObject object=new JSONObject(json);
                String text=object.optString("commentText",null);
                if(text!=null && text.replace("\u200B","").trim().equals(p.text.replace("\u200B","").trim())) {
                    String clean=text.replace("\u200B","");if(clean.isEmpty())object.remove("commentText");else object.put("commentText",clean);object.put("tenorId",p.id);
                    updated=object.toString().getBytes(StandardCharsets.UTF_8);
                }
            } else updated=GifComments.rewriteSelectedPayload(raw,p.text,p.id);
            if(updated==raw)return body;
            if(pending==p)pending=null;
            if(compressed){ByteArrayOutputStream output=new ByteArrayOutputStream();try(GZIPOutputStream gzip=new GZIPOutputStream(output)){gzip.write(updated);}updated=output.toByteArray();}
            return updated;
        }catch(Exception failure){throw new IllegalArgumentException("GIF attachment request blocked",failure);}
    }
    private static String url(Object request) {
        if(request instanceof String)return request.toString();
        if(request==null)return "";
        // YouTube 20.40.45 qon.bG calls abcb.v() for the actual Cronet URL.
        try{java.lang.reflect.Method method=request.getClass().getMethod("v");method.setAccessible(true);Object value=method.invoke(request);return value==null?"":value.toString();}catch(Exception ignored){}
        return "";
    }
}
