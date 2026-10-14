package app.revanced.gifcomments;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.MotionEvent;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Attached to the native comment editor, not to a process-global selected GIF. */
public final class GifPicker {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final ExecutorService NETWORK=Executors.newFixedThreadPool(3);
    private static final WeakHashMap<EditText,WeakReference<State>> STATES=new WeakHashMap<EditText,WeakReference<State>>();
    private GifPicker(){}
    public static void install(View view) {
        if(!(view instanceof EditText))return;
        final EditText editor=(EditText)view;
        editor.post(new Runnable(){public void run(){
            try {
                WeakReference<State> ref=STATES.get(editor);
                if(ref!=null && ref.get()!=null)return;
                if(!(editor.getParent() instanceof LinearLayout))return;
                ViewGroup previewParent=(ViewGroup)editor.getParent();
                View row=previewParent;
                // The native editor lives inside MaxHeightScrollView inside a horizontal row.
                while(row.getParent() instanceof ViewGroup) {
                    if(row.getParent() instanceof LinearLayout && ((LinearLayout)row.getParent()).getOrientation()==LinearLayout.HORIZONTAL)break;
                    row=(View)row.getParent();
                }
                if(!(row.getParent() instanceof LinearLayout))return;
                State state=new State(editor,previewParent,(LinearLayout)row.getParent());
                STATES.put(editor,new WeakReference<State>(state));
            }catch(RuntimeException ignored){ /* Unsupported layout: leave the native editor usable. */ }
        }});
    }
    private static int dp(Context c,int n){return (int)(c.getResources().getDisplayMetrics().density*n+.5f);}
    private static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private static final class State {
        final EditText editor;
        final LinearLayout preview;
        final Animated previewImage;
        final TextView previewText;
        int selectionGeneration;
        boolean changing;
        String selectedId;
        State(EditText e,ViewGroup previewParent,LinearLayout row) {
            editor=e;Context c=e.getContext();
            GifTransport.cancelAll();
            Button button=new Button(c);button.setText("GIF");button.setContentDescription("Tenor GIF 검색");button.setMinWidth(0);button.setMinimumWidth(0);button.setPadding(0,0,0,0);
            row.addView(button,new LinearLayout.LayoutParams(dp(c,48),dp(c,48)));
            button.setOnClickListener(v -> open());
            preview=new LinearLayout(c);preview.setGravity(Gravity.CENTER_VERTICAL);preview.setVisibility(View.GONE);
            previewImage=new Animated(c);preview.addView(previewImage,new LinearLayout.LayoutParams(dp(c,96),dp(c,72)));
            previewText=new TextView(c);previewText.setText("선택한 GIF");preview.addView(previewText,new LinearLayout.LayoutParams(0,dp(c,72),1));
            Button remove=new Button(c);remove.setText("제거");remove.setContentDescription("선택한 GIF 제거");preview.addView(remove);
            remove.setOnClickListener(v -> clear());
            previewParent.addView(preview,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(c,80)));
            e.addTextChangedListener(new TextWatcher(){
                public void beforeTextChanged(CharSequence s,int start,int count,int after){}
                public void onTextChanged(CharSequence s,int start,int before,int count){}
                public void afterTextChanged(Editable text){
                    if(changing)return;
                    if(selectedId!=null) {
                        if(text.length()==0){selectedId=null;preview.setVisibility(View.GONE);selectionGeneration++;GifTransport.cancel(State.this);}
                        else GifTransport.armDraft(State.this,text.toString(),selectedId);
                    }
                }
            });
            e.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){GifTransport.cancel(State.this);selectedId=null;selectionGeneration++;}});
            View send=e.getRootView().findViewById(0x7f0b1238);
            if(send!=null)send.setOnTouchListener((v,event) -> {
                if(event.getAction()==MotionEvent.ACTION_UP && selectedId!=null && event.getX()>=0 && event.getY()>=0 && event.getX()<v.getWidth() && event.getY()<v.getHeight()) {
                    GifTransport.arm(State.this,editor.getText().toString(),selectedId);
                    MAIN.postDelayed(() -> {if(GifTransport.outstanding(State.this)){GifTransport.cancel(State.this);Toast.makeText(editor.getContext(),"GIF 첨부 요청을 처리하지 못했습니다. 게시된 댓글을 확인하세요.",Toast.LENGTH_LONG).show();}},10000);
                }
                return false;
            });
        }
        void clear(){changing=true;try{if(editor.getText().toString().equals("\u200B"))editor.setText("");selectedId=null;GifTransport.cancel(this);preview.setVisibility(View.GONE);selectionGeneration++;}finally{changing=false;}}
        void choose(final TenorClient.Gif gif){
            clear();changing=true;
            try{selectedId=gif.id;if(editor.length()==0)editor.setText("\u200B");GifTransport.armDraft(this,editor.getText().toString(),gif.id);}finally{changing=false;}
            previewText.setText("GIF 선택됨");previewImage.setMovie(null);preview.setVisibility(View.VISIBLE);
            final int generation=++selectionGeneration;
            NETWORK.execute(() -> {try{byte[] bytes=TenorClient.fetch(gif.preview,4*1024*1024);final Movie movie=Movie.decodeByteArray(bytes,0,bytes.length);MAIN.post(() -> {if(generation==selectionGeneration && preview.isAttachedToWindow())previewImage.setMovie(movie);});}catch(Exception ignored){MAIN.post(() -> {if(generation==selectionGeneration)previewText.setText("GIF 선택됨 · 미리보기 로드 실패");});}});
        }
        void open(){
            Context c=editor.getContext();final Dialog dialog=new Dialog(c);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            LinearLayout body=column(c);body.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));
            TextView title=new TextView(c);title.setText("Tenor GIF 검색");title.setTextSize(18);body.addView(title);
            final EditText search=new EditText(c);search.setSingleLine(true);search.setHint("검색어 입력");body.addView(search);
            final TextView status=new TextView(c);body.addView(status);
            ScrollView scroll=new ScrollView(c);final LinearLayout results=column(c);scroll.addView(results);body.addView(scroll,new LinearLayout.LayoutParams(-1,dp(c,320)));
            Button close=new Button(c);close.setText("닫기");body.addView(close);close.setOnClickListener(v -> dialog.dismiss());
            final Session session=new Session(dialog,results,status,this);
            dialog.setOnDismissListener(d -> {session.active=false;session.generation++;if(session.pending!=null)MAIN.removeCallbacks(session.pending);});
            search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable t){session.schedule(t.toString());}});
            dialog.setContentView(body);dialog.show();Window window=dialog.getWindow();if(window!=null){window.setLayout(-1,-2);window.setGravity(Gravity.BOTTOM);}
            session.schedule("");
        }
    }
    private static final class Session {
        final Dialog dialog;final LinearLayout results;final TextView status;final State state;
        volatile boolean active=true;volatile int generation;Runnable pending;
        Session(Dialog d,LinearLayout r,TextView s,State owner){dialog=d;results=r;status=s;state=owner;}
        void schedule(final String query){if(pending!=null)MAIN.removeCallbacks(pending);final int current=++generation;pending=() -> {status.setText("검색 중…");results.removeAllViews();NETWORK.execute(() -> {try{List<TenorClient.Gif> gifs=TenorClient.search(query);MAIN.post(() -> {if(active && current==generation)show(gifs,current);});}catch(Exception failure){MAIN.post(() -> {if(active && current==generation)status.setText("검색 실패 · 검색어를 바꿔 다시 시도하세요");});}});};MAIN.postDelayed(pending,400);}
        void show(List<TenorClient.Gif> gifs,int current){
            status.setText(gifs.isEmpty()?"검색 결과 없음":"GIF를 선택하세요 · 일부 GIF는 YouTube에서 거절될 수 있습니다");
            LinearLayout row=null;Context c=results.getContext();
            for(int i=0;i<gifs.size();i++) {
                final TenorClient.Gif gif=gifs.get(i);
                if(i%2==0){row=new LinearLayout(c);results.addView(row);}
                LinearLayout card=column(c);card.setPadding(dp(c,4),dp(c,4),dp(c,4),dp(c,4));
                final Animated image=new Animated(c);image.setContentDescription(gif.title);card.addView(image,new LinearLayout.LayoutParams(-1,dp(c,112)));
                TextView caption=new TextView(c);caption.setText(gif.title);caption.setMaxLines(2);card.addView(caption);
                row.addView(card,new LinearLayout.LayoutParams(0,dp(c,158),1));
                card.setOnClickListener(v -> {if(editorReady()){state.choose(gif);dialog.dismiss();}});
                NETWORK.execute(() -> {if(!active || current!=generation)return;try{byte[] bytes=TenorClient.fetch(gif.preview,4*1024*1024);final Movie movie=Movie.decodeByteArray(bytes,0,bytes.length);MAIN.post(() -> {if(active && current==generation)image.setMovie(movie);});}catch(Exception ignored){}});
            }
        }
        boolean editorReady(){if(state.editor.isAttachedToWindow())return true;dialog.dismiss();return false;}
    }
    private static final class Animated extends View {
        Movie movie;long start;
        Animated(Context c){super(c);}
        void setMovie(Movie m){movie=m;start=SystemClock.uptimeMillis();invalidate();}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);if(movie==null)return;int duration=movie.duration();movie.setTime((int)((SystemClock.uptimeMillis()-start)%(duration>0?duration:1000)));float scale=Math.min((float)getWidth()/Math.max(1,movie.width()),(float)getHeight()/Math.max(1,movie.height()));canvas.save();canvas.translate((getWidth()-movie.width()*scale)/2,(getHeight()-movie.height()*scale)/2);canvas.scale(scale,scale);movie.draw(canvas,0,0);canvas.restore();if(isShown())postInvalidateDelayed(60);}
    }
}
