package app.revanced.gifcomments;
import android.content.Context;
import android.content.SharedPreferences;
final class GifStore {
    private final SharedPreferences preferences;
    final GifLibrary library;
    GifStore(Context context){preferences=context.getApplicationContext().getSharedPreferences("revanced_gif_library",Context.MODE_PRIVATE);library=GifLibrary.decode(preferences.getString("library","{}"));}
    void save(){preferences.edit().putString("library",library.encode()).apply();}
}
