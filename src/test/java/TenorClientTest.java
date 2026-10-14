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
        String exact="<script id=\"store-cache\">{\"gifs\":{\"byId\":{\"14112463059641248604\":{\"results\":["+valid+"]}}}}</script>";
        if(TenorClient.parseIdPage(exact,"14112463059641248604").size()!=1)throw new AssertionError("Exact numeric ID lookup");
        if(!TenorClient.parseIdPage(exact,"123").isEmpty())throw new AssertionError("Do not fall back to unrelated GIFs");
        if(!TenorClient.parseIdPage(exact.replace(valid,rejected),"14112463059641248604").isEmpty())throw new AssertionError("ID lookup uses rating filter");
        for(String id:new String[]{"0","01","-1","1.2","123456789012345678901"})if(TenorClient.isId(id))throw new AssertionError("Invalid ID "+id);
        if(args.length>0){List<TenorClient.Gif> live=TenorClient.search("happy");if(live.isEmpty())throw new AssertionError("Live search returned no GIFs");System.out.println("Live Tenor search: "+live.size()+" GIFs");List<TenorClient.Gif> direct=TenorClient.search("14083772942551242087");if(direct.size()!=1 || !direct.get(0).id.equals("14083772942551242087"))throw new AssertionError("Live ID lookup");System.out.println("Live numeric ID lookup passed");}
        System.out.println("Tenor parser and URL checks passed");
    }
}
