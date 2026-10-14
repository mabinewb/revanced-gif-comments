import app.revanced.gifcomments.GifLibrary;
import app.revanced.gifcomments.TenorClient;
public final class GifLibraryTest {
    static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);}
    public static void main(String[] args){
        GifLibrary library=new GifLibrary();
        TenorClient.Gif a=new TenorClient.Gif("14083772942551242087","한국어 😺","https://media.tenor.com/test.gif");
        TenorClient.Gif b=new TenorClient.Gif("2","Two","https://media1.tenor.com/m/test.gif");
        library.remember(a);library.remember(b);library.remember(a);
        check(library.recent().size()==2 && library.recent().get(0).id.equals(a.id),"Deduplicate and move reuse to front");
        library.toggleFavorite(a);library.toggleFavorite(b);
        GifLibrary loaded=GifLibrary.decode(library.encode());
        check(loaded.recent().get(0).id.equals(a.id) && loaded.recent().get(0).title.equals(a.title),"Persist large ID and Unicode");
        check(loaded.isFavorite(a.id) && loaded.isFavorite(b.id),"Persist favorites");
        loaded.clearRecent();check(loaded.recent().isEmpty() && loaded.isFavorite(a.id),"Clearing recent preserves favorites");
        loaded.toggleFavorite(a);check(!loaded.isFavorite(a.id) && loaded.isFavorite(b.id),"Remove one favorite");
        loaded.remember(new TenorClient.Gif("1","bad","https://evil.example/test.gif"));check(loaded.recent().isEmpty(),"Reject unsafe persisted media");
        for(int i=1;i<=120;i++){TenorClient.Gif g=new TenorClient.Gif(""+i,"title","https://media.tenor.com/test.gif");library.remember(g);if(!library.isFavorite(g.id))library.toggleFavorite(g);}
        check(library.recent().size()==50 && library.favorites().size()==100,"Bound stored metadata");
        check(GifLibrary.decode(library.encode()).recent().size()==50,"Bounded library roundtrip");
        check(GifLibrary.decode("garbage").recent().isEmpty(),"Recover corrupt storage");
        check(GifLibrary.decode("{\"version\":99,\"recent\":[]}").favorites().isEmpty(),"Ignore unknown schema");
        check(GifLibrary.decode("{\"version\":1,\"recent\":[{\"id\":\"1\",\"preview\":\"https://evil.example/a.gif\"}]}").recent().isEmpty(),"Validate metadata on load");
        System.out.println("GIF library persistence, ordering, limits and independent favorites passed");
    }
}
