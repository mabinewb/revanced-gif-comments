package app.revanced.gifcomments;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** No cookies, account credentials or hard-coded API keys. */
public final class TenorClient {
    public static final class Gif {
        public final String id, title, preview;
        public Gif(String id, String title, String preview) { this.id=id; this.title=title; this.preview=preview; }
    }
    public static List<Gif> search(String query) throws Exception {
        String slug = query.trim().toLowerCase(Locale.ROOT);
        if(slug.matches("[0-9]+")) {
            if(!isId(slug))throw new IllegalArgumentException("Invalid Tenor ID");
            return parseIdPage(new String(fetch("https://tenor.com/view/"+slug,12*1024*1024),StandardCharsets.UTF_8),slug);
        }
        String page = "https://tenor.com/";
        if (!slug.isEmpty()) {
            StringBuilder encoded = new StringBuilder();
            for (String word : slug.split("\\s+")) {
                if (encoded.length()>0) encoded.append('-');
                encoded.append(URLEncoder.encode(word,"UTF-8").replace("+","%20"));
            }
            page += "search/"+encoded+"-gifs";
        }
        return parsePage(new String(fetch(page, 12*1024*1024), StandardCharsets.UTF_8));
    }
    public static boolean isId(String id){return id!=null && id.matches("[1-9][0-9]{0,19}");}
    private static JSONObject store(String html) throws Exception {
        Matcher m=Pattern.compile("<script[^>]*id=\"store-cache\"[^>]*>([\\s\\S]*?)</script>").matcher(html);
        if(!m.find())throw new IllegalArgumentException("Tenor page format changed");
        return new JSONObject(m.group(1));
    }
    public static List<Gif> parseIdPage(String html,String id) throws Exception {
        if(!isId(id))throw new IllegalArgumentException("Invalid Tenor ID");
        JSONObject gifs=store(html).optJSONObject("gifs");
        JSONObject byId=gifs==null?null:gifs.optJSONObject("byId");
        JSONObject record=byId==null?null:byId.optJSONObject(id);
        List<Gif> matches=parseResults(record==null?null:record.optJSONArray("results"));
        List<Gif> exact=new ArrayList<Gif>();
        for(Gif gif:matches)if(gif.id.equals(id))exact.add(gif);
        return exact;
    }
    public static List<Gif> parsePage(String html) throws Exception {
        JSONObject store=store(html);
        JSONObject universal = store.optJSONObject("universal");
        JSONObject searches = universal==null ? null : universal.optJSONObject("search");
        JSONArray results = null;
        if (searches!=null) {
            Iterator<String> keys=searches.keys();
            if(keys.hasNext()) results=searches.getJSONObject(keys.next()).optJSONArray("results");
        }
        if(results==null) {
            JSONObject gifs=store.optJSONObject("gifs");
            JSONObject featured=gifs==null?null:gifs.optJSONObject("featured");
            results=featured==null?null:featured.optJSONArray("results");
        }
        if(results==null) throw new IllegalArgumentException("Tenor returned no readable results");
        return parseResults(results);
    }
    private static List<Gif> parseResults(JSONArray results) throws Exception {
        List<Gif> gifs=new ArrayList<Gif>();
        if(results==null)return gifs;
        for(int i=0;i<results.length() && gifs.size()<24;i++) {
            JSONObject r=results.optJSONObject(i);
            if(r==null)continue;
            String id=r.optString("id","");
            if(!id.matches("[1-9][0-9]{0,19}"))continue;
            String rating=r.optString("content_rating","");
            if(!rating.isEmpty() && !rating.equals("G"))continue;
            JSONArray flags=r.optJSONArray("flags");
            boolean excluded=false;
            if(flags!=null) for(int j=0;j<flags.length();j++) {
                String flag=flags.optString(j);
                if(flag.equals("sticker") || flag.equals("static"))excluded=true;
            }
            if(excluded)continue;
            JSONObject formats=r.optJSONObject("media_formats");
            JSONObject tiny=formats==null?null:formats.optJSONObject("tinygif");
            if(tiny==null)continue;
            String preview=tiny.optString("url","");
            if(!isMediaUrl(preview))continue;
            gifs.add(new Gif(id,r.optString("content_description","GIF"),preview));
        }
        return gifs;
    }
    public static boolean isMediaUrl(String url) {
        try { URL u=new URL(url);return u.getProtocol().equals("https") && u.getHost().matches("media[0-9]*\\.tenor\\.com") && u.getUserInfo()==null && (u.getPort()==-1 || u.getPort()==443); }
        catch(Exception e){return false;}
    }
    public static byte[] fetch(String address,int limit) throws Exception {
        for(int redirect=0;redirect<4;redirect++) {
            URL url=new URL(address);
            if(!isMediaUrl(address) && !(url.getProtocol().equals("https") && url.getHost().equals("tenor.com") && url.getUserInfo()==null && (url.getPort()==-1 || url.getPort()==443))) throw new IllegalArgumentException("Unexpected Tenor host");
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent","Mozilla/5.0 (Android) ReVancedGifComments/0.3.0");
            try {
                int status=connection.getResponseCode();
                if(status>=300 && status<400) {address=new URL(url,connection.getHeaderField("Location")).toString();continue;}
                if(status!=200)throw new IllegalStateException("Tenor HTTP "+status);
                if(connection.getContentLength()>limit)throw new IllegalStateException("Tenor response too large");
                try(InputStream input=connection.getInputStream();ByteArrayOutputStream output=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[8192];int n;
                    while((n=input.read(buffer))!=-1) {
                        if(output.size()+n>limit)throw new IllegalStateException("Tenor response too large");
                        output.write(buffer,0,n);
                    }
                    return output.toByteArray();
                }
            }finally{connection.disconnect();}
        }
        throw new IllegalStateException("Too many Tenor redirects");
    }
}
