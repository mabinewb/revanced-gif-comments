package app.revanced.gifcomments;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Versioned local metadata only; no comment text or account identity. */
public final class GifLibrary {
    private final List<TenorClient.Gif> recent=new ArrayList<TenorClient.Gif>();
    private final List<TenorClient.Gif> favorites=new ArrayList<TenorClient.Gif>();
    public List<TenorClient.Gif> recent(){return new ArrayList<TenorClient.Gif>(recent);}
    public List<TenorClient.Gif> favorites(){return new ArrayList<TenorClient.Gif>(favorites);}
    private static boolean valid(TenorClient.Gif gif){return gif!=null && TenorClient.isId(gif.id) && TenorClient.isMediaUrl(gif.preview);}
    private static boolean remove(List<TenorClient.Gif> list,String id){for(int i=0;i<list.size();i++)if(list.get(i).id.equals(id)){list.remove(i);return true;}return false;}
    public void remember(TenorClient.Gif gif){if(!valid(gif))return;remove(recent,gif.id);recent.add(0,gif);while(recent.size()>50)recent.remove(recent.size()-1);}
    public boolean isFavorite(String id){for(TenorClient.Gif gif:favorites)if(gif.id.equals(id))return true;return false;}
    public void toggleFavorite(TenorClient.Gif gif){if(!valid(gif))return;if(remove(favorites,gif.id))return;favorites.add(0,gif);while(favorites.size()>100)favorites.remove(favorites.size()-1);}
    public void clearRecent(){recent.clear();}
    private static JSONArray array(List<TenorClient.Gif> list) throws Exception {
        JSONArray result=new JSONArray();for(TenorClient.Gif gif:list)result.put(new JSONObject().put("id",gif.id).put("title",gif.title).put("preview",gif.preview));return result;
    }
    public String encode(){try{return new JSONObject().put("version",1).put("recent",array(recent)).put("favorites",array(favorites)).toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public static GifLibrary decode(String json){GifLibrary library=new GifLibrary();try{
        JSONObject data=new JSONObject(json);if(data.optInt("version")!=1)return library;
        read(data.optJSONArray("recent"),library.recent,50);read(data.optJSONArray("favorites"),library.favorites,100);
    }catch(Exception ignored){}return library;}
    private static void read(JSONArray data,List<TenorClient.Gif> target,int max){if(data==null)return;for(int i=0;i<data.length() && target.size()<max;i++){
        JSONObject item=data.optJSONObject(i);if(item==null)continue;
        TenorClient.Gif gif=new TenorClient.Gif(item.optString("id",""),item.optString("title","GIF"),item.optString("preview",""));
        if(!valid(gif))continue;boolean duplicate=false;for(TenorClient.Gif old:target)if(old.id.equals(gif.id))duplicate=true;if(!duplicate)target.add(gif);
    }}
}
