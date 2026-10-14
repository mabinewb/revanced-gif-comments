package app.revanced.gifcomments;

import android.app.Dialog;
import android.app.Activity;
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
import android.view.ViewTreeObserver;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.widget.PopupWindow;
import android.util.Log;
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
    private static final WeakHashMap<EditText,Boolean> OBSERVED=new WeakHashMap<EditText,Boolean>();
    private static final WeakHashMap<View,Boolean> ROOTS=new WeakHashMap<View,Boolean>();
    public static void watchActivity(Activity activity){watchRoot(activity.getWindow().getDecorView());}
    public static void watchDialog(Dialog dialog){if(dialog.getWindow()!=null)watchRoot(dialog.getWindow().getDecorView());}
    private static void watchRoot(final View root){
        if(ROOTS.put(root,Boolean.TRUE)!=null)return;
        root.post(() -> scan(root));
        final ViewTreeObserver.OnGlobalFocusChangeListener focus=(oldView,newView) -> {
            observe(newView);
            if(newView instanceof EditText && isCommentEditor((EditText)newView))install(newView);
        };
        final ViewTreeObserver.OnGlobalLayoutListener layout=() -> {
            View focused=root.findFocus();observe(focused);
            if(focused instanceof EditText && isCommentEditor((EditText)focused))install(focused);
        };
        root.getViewTreeObserver().addOnGlobalFocusChangeListener(focus);
        root.getViewTreeObserver().addOnGlobalLayoutListener(layout);
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){root.getViewTreeObserver().addOnGlobalFocusChangeListener(focus);root.getViewTreeObserver().addOnGlobalLayoutListener(layout);root.post(() -> scan(root));}
            public void onViewDetachedFromWindow(View v){if(root.getViewTreeObserver().isAlive()){root.getViewTreeObserver().removeOnGlobalFocusChangeListener(focus);root.getViewTreeObserver().removeOnGlobalLayoutListener(layout);}}
        });
    }
    private static void scan(View view){
        if(view instanceof EditText){observe(view);if(isCommentEditor((EditText)view))install(view);}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)scan(group.getChildAt(i));}
    }
    public static void observe(View view) {
        if(!(view instanceof EditText))return;
        final EditText editor=(EditText)view;
        if(OBSERVED.put(editor,Boolean.TRUE)!=null)return;
        final ViewTreeObserver.OnGlobalLayoutListener listener=() -> {
            if(isCommentEditor(editor))install(editor);
            WeakReference<State> ref=STATES.get(editor);
            if(ref!=null && ref.get()!=null)ref.get().position();
        };
        editor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){editor.getViewTreeObserver().addOnGlobalLayoutListener(listener);editor.post(() -> listener.onGlobalLayout());}
            public void onViewDetachedFromWindow(View v){if(editor.getViewTreeObserver().isAlive())editor.getViewTreeObserver().removeOnGlobalLayoutListener(listener);}
        });
        if(editor.isAttachedToWindow()){editor.getViewTreeObserver().addOnGlobalLayoutListener(listener);editor.post(() -> listener.onGlobalLayout());}
    }
    private static boolean isCommentEditor(EditText editor) {
        if(!editor.isAttachedToWindow() || !editor.isShown())return false;
        View node=editor;
        for(int depth=0;node!=null && depth<12;depth++) {
            if(node.getId()!=View.NO_ID)try {
                String name=node.getResources().getResourceEntryName(node.getId());
                if(name.equals("comment") || name.equals("comment_dialog_root") || name.equals("comment_dialog_wrapper"))return true;
            }catch(RuntimeException ignored){}
            node=node.getParent() instanceof View?(View)node.getParent():null;
        }
        String hint=editor.getHint()==null?"":editor.getHint().toString().toLowerCase(java.util.Locale.ROOT);
        return hint.contains("댓글") || hint.contains("답글") || hint.contains("comment") || hint.contains("reply");
    }
    public static void install(View view) {
        if(!(view instanceof EditText))return;
        final EditText editor=(EditText)view;
        observe(editor);
        editor.post(() -> {
            if(!editor.isAttachedToWindow() || !editor.isShown())return;
            WeakReference<State> ref=STATES.get(editor);
            if(ref!=null && ref.get()!=null){ref.get().position();return;}
            try {
                State state=new State(editor);
                STATES.put(editor,new WeakReference<State>(state));
                state.position();
                Log.i("ReVancedGIF","0.2.1 comment editor attached: "+editor.getClass().getName());
            }catch(RuntimeException failure){Log.w("ReVancedGIF","Cannot attach GIF button",failure);}
        });
    }
    private static int dp(Context c,int n){return (int)(c.getResources().getDisplayMetrics().density*n+.5f);}
    private static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private static final class State {
        final EditText editor;
        final LinearLayout preview;
        final Animated previewImage;
        final TextView previewText;
        final PopupWindow buttonWindow,previewWindow;
        final Button button;
        int selectionGeneration;
        boolean changing,picking;
        String selectedId;
        State(EditText e) {
            editor=e;Context c=e.getContext();
            GifTransport.cancelAll();
            button=new Button(c);button.setText("GIF");button.setContentDescription("Tenor GIF 검색");button.setMinWidth(0);button.setMinimumWidth(0);button.setPadding(0,0,0,0);
            buttonWindow=new PopupWindow(button,dp(c,48),dp(c,48),false);
            buttonWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            buttonWindow.setClippingEnabled(true);
            editor.setPadding(editor.getPaddingLeft(),editor.getPaddingTop(),editor.getPaddingRight()+dp(c,48),editor.getPaddingBottom());
            button.setOnClickListener(v -> open());
            preview=new LinearLayout(c);preview.setGravity(Gravity.CENTER_VERTICAL);preview.setVisibility(View.GONE);
            previewImage=new Animated(c);preview.addView(previewImage,new LinearLayout.LayoutParams(dp(c,96),dp(c,72)));
            previewText=new TextView(c);previewText.setText("선택한 GIF");previewText.setTextColor(0xff111111);preview.addView(previewText,new LinearLayout.LayoutParams(0,dp(c,72),1));
            Button remove=new Button(c);remove.setText("제거");remove.setContentDescription("선택한 GIF 제거");preview.addView(remove);
            remove.setOnClickListener(v -> clear());
            preview.setBackgroundColor(0xffeeeeee);
            previewWindow=new PopupWindow(preview,dp(c,280),dp(c,80),false);
            previewWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
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
            e.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){GifTransport.cancel(State.this);selectedId=null;selectionGeneration++;buttonWindow.dismiss();previewWindow.dismiss();}});
            View send=e.getRootView().findViewById(0x7f0b1238);
            if(send!=null)send.setOnTouchListener((v,event) -> {
                if(event.getAction()==MotionEvent.ACTION_UP && selectedId!=null && event.getX()>=0 && event.getY()>=0 && event.getX()<v.getWidth() && event.getY()<v.getHeight()) {
                    GifTransport.arm(State.this,editor.getText().toString(),selectedId);
                    MAIN.postDelayed(() -> {if(GifTransport.outstanding(State.this)){GifTransport.cancel(State.this);Toast.makeText(editor.getContext(),"GIF 첨부 요청을 처리하지 못했습니다. 게시된 댓글을 확인하세요.",Toast.LENGTH_LONG).show();}},10000);
                }
                return false;
            });
        }
        void position(){
            Rect visible=new Rect();
            if(picking || !editor.isAttachedToWindow() || !editor.isShown() || !editor.getGlobalVisibleRect(visible) || visible.height()<dp(editor.getContext(),24)) {buttonWindow.dismiss();previewWindow.dismiss();return;}
            int[] xy=new int[2];editor.getLocationOnScreen(xy);
            int x=xy[0]+editor.getWidth()-dp(editor.getContext(),48),y=xy[1]+Math.max(0,(editor.getHeight()-dp(editor.getContext(),48))/2);
            try {
                if(buttonWindow.isShowing())buttonWindow.update(x,y,-1,-1);
                else buttonWindow.showAtLocation(editor,Gravity.TOP|Gravity.LEFT,x,y);
                if(selectedId!=null) {
                    int px=xy[0],py=Math.max(0,xy[1]-dp(editor.getContext(),80));
                    if(previewWindow.isShowing())previewWindow.update(px,py,-1,-1);
                    else previewWindow.showAtLocation(editor,Gravity.TOP|Gravity.LEFT,px,py);
                }else previewWindow.dismiss();
            }catch(RuntimeException failure){Log.w("ReVancedGIF","Cannot position GIF button",failure);}
        }
        void clear(){changing=true;try{if(editor.getText().toString().equals("\u200B"))editor.setText("");selectedId=null;GifTransport.cancel(this);preview.setVisibility(View.GONE);previewWindow.dismiss();selectionGeneration++;}finally{changing=false;}}
        void choose(final TenorClient.Gif gif){
            clear();changing=true;
            try{selectedId=gif.id;if(editor.length()==0)editor.setText("\u200B");GifTransport.armDraft(this,editor.getText().toString(),gif.id);}finally{changing=false;}
            previewText.setText("GIF 선택됨");previewImage.setMovie(null);preview.setVisibility(View.VISIBLE);position();
            final int generation=++selectionGeneration;
            NETWORK.execute(() -> {try{byte[] bytes=TenorClient.fetch(gif.preview,4*1024*1024);final Movie movie=Movie.decodeByteArray(bytes,0,bytes.length);MAIN.post(() -> {if(generation==selectionGeneration && preview.isAttachedToWindow())previewImage.setMovie(movie);});}catch(Exception ignored){MAIN.post(() -> {if(generation==selectionGeneration)previewText.setText("GIF 선택됨 · 미리보기 로드 실패");});}});
        }
        void open(){
            picking=true;buttonWindow.dismiss();previewWindow.dismiss();
            Context c=editor.getContext();final Dialog dialog=new Dialog(c);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            LinearLayout body=column(c);body.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));
            TextView title=new TextView(c);title.setText("Tenor GIF 검색");title.setTextSize(18);body.addView(title);
            final EditText search=new EditText(c);search.setSingleLine(true);search.setHint("검색어 입력");body.addView(search);
            final TextView status=new TextView(c);body.addView(status);
            ScrollView scroll=new ScrollView(c);final LinearLayout results=column(c);scroll.addView(results);body.addView(scroll,new LinearLayout.LayoutParams(-1,dp(c,320)));
            Button close=new Button(c);close.setText("닫기");body.addView(close);close.setOnClickListener(v -> dialog.dismiss());
            final Session session=new Session(dialog,results,status,this);
            dialog.setOnDismissListener(d -> {session.active=false;session.generation++;if(session.pending!=null)MAIN.removeCallbacks(session.pending);picking=false;position();});
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
        Animated(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setMovie(Movie m){movie=m;start=SystemClock.uptimeMillis();invalidate();}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);if(movie==null)return;int duration=movie.duration();movie.setTime((int)((SystemClock.uptimeMillis()-start)%(duration>0?duration:1000)));float scale=Math.min((float)getWidth()/Math.max(1,movie.width()),(float)getHeight()/Math.max(1,movie.height()));canvas.save();canvas.translate((getWidth()-movie.width()*scale)/2,(getHeight()-movie.height()*scale)/2);canvas.scale(scale,scale);movie.draw(canvas,0,0);canvas.restore();if(isShown())postInvalidateDelayed(60);}
    }
}
