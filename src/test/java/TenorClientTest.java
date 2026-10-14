import app.revanced.gifcomments.TenorClient;
import java.util.List;

public final class TenorClientTest {
    public static void main(String[] args) throws Exception {
        String valid="{\"id\":\"14112463059641248604\",\"content_description\":\"고양이\",\"media_formats\":{\"tinygif\":{\"url\":\"https://media.tenor.com/example/cat.gif\"}}}";
        String rejected=valid.replace("\"content_description\"","\"content_rating\":\"R\",\"content_description\"");
        String sticker=valid.replace("\"content_description\"","\"flags\":[\"sticker\"],\"content_description\"");
        String unsafe=valid.replace("media.tenor.com","media.tenor.com.evil.example");
        String store="{\"universal\":{\"search\":{\"happy\":{\"results\":["+valid+","+rejected+","+sticker+","+unsafe+"]}}}}";
        List<TenorClient.Gif> gifs=TenorClient.parsePage("<script id=\"store-cache\" type=\"text/x-cache\">"+store+"</script>");
        if(gifs.size()!=1)throw new AssertionError("Filter unsafe/rated/sticker results");
        if(!gifs.get(0).id.equals("14112463059641248604"))throw new AssertionError("Preserve large unsigned IDs as strings");
        if(!gifs.get(0).title.equals("고양이"))throw new AssertionError("Preserve Unicode");
        for(String url:new String[]{"http://media.tenor.com/a.gif","https://media.tenor.com@evil.example/a.gif","https://media.tenor.com:123/a.gif","file:///a.gif"})if(TenorClient.isMediaUrl(url))throw new AssertionError("Unsafe URL "+url);
        if(!TenorClient.isMediaUrl("https://media1.tenor.com/m/test.gif"))throw new AssertionError("Valid numbered media domain");
        try{TenorClient.parsePage("changed page");throw new AssertionError("Changed page format");}catch(IllegalArgumentException expected){}
        String featured="<script id=\"store-cache\">{\"gifs\":{\"featured\":{\"results\":["+valid+"]}}}</script>";
        if(TenorClient.parsePage(featured).size()!=1)throw new AssertionError("Featured results");
        if(args.length>0){List<TenorClient.Gif> live=TenorClient.search("happy");if(live.isEmpty())throw new AssertionError("Live search returned no GIFs");System.out.println("Live Tenor search: "+live.size()+" GIFs");}
        System.out.println("Tenor parser and URL checks passed");
    }
}
