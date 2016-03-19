package com.ui.jerry;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;


/**
 * camera 相关控件
 *
 * @author jerry
 * @date 2015-09-02
 */
public class CameraView extends RelativeLayout {

    private ImageView m_ivTop;
    private ImageView m_ivBottom;

    private int mWidth;
    private int mHeight;

    public static final int PIC_HEIGHT_FROM_CENTER = 744;   //从中心点开始的高度  px
    public static final int OFFSET = 100;  //中心点  偏移
    public static float SCALE_Y ;

    private boolean isChildAdded = false;

    private Animation anim_up100;
    private Animation anim_down100;
    private Animation anim_upAll;
    private Animation anim_downAll;

    private Context mContext;

    private Animation.AnimationListener animationListener;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        m_ivTop = new ImageView(context);
        m_ivBottom = new ImageView(context);

        m_ivTop.setImageResource(R.drawable.camera_icon_shot_top);
        m_ivBottom.setImageResource(R.drawable.camera_icon_shot_bottom);

        m_ivTop.setScaleType(ImageView.ScaleType.FIT_XY);
        m_ivBottom.setScaleType(ImageView.ScaleType.FIT_XY);

        setWillNotDraw(false);

        anim_upAll = AnimationUtils.loadAnimation(context, R.anim.slide_up_all);
        anim_downAll = AnimationUtils.loadAnimation(context, R.anim.slide_down_all);

        anim_upAll.setFillAfter(true);
        anim_downAll.setFillAfter(true);

        anim_up100 = AnimationUtils.loadAnimation(mContext,R.anim.slide_up100);
        anim_down100 =  AnimationUtils.loadAnimation(mContext,R.anim.slide_down100);

        anim_up100.setFillAfter(true);
        anim_down100.setFillAfter(true);
    }

    public Animation.AnimationListener getAnimationListener() {
        return animationListener;
    }

    public void setAnimationListener(Animation.AnimationListener animationListener) {
        this.animationListener = animationListener;

        anim_down100.setAnimationListener(animationListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidth = measure(widthMeasureSpec);
        mHeight = mWidth;

        //保证cameraView是正方形
        setMeasuredDimension(mWidth, mHeight);

        if(!isChildAdded) {
            //设置尺寸
            SCALE_Y = (float) mHeight / (PIC_HEIGHT_FROM_CENTER * 2);
            int slideHeight =(int) (mHeight / 2 + OFFSET * SCALE_Y);

            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,slideHeight);
            addView(m_ivTop, params);
            RelativeLayout.LayoutParams paramsBottom = new RelativeLayout.LayoutParams(mWidth,slideHeight);
            paramsBottom.topMargin = mHeight - slideHeight;
            addView(m_ivBottom, paramsBottom);

            isChildAdded =true;
        }
    }

    private int measure(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        // Default size if no limits are specified.
        int result = 0;
        if (specMode == MeasureSpec.AT_MOST) {

            // Calculate the ideal size of your
            // control within this maximum size.
            // If your control fills the available
            // space return the outer bound.

            result = specSize;
        } else if (specMode == MeasureSpec.EXACTLY) {

            // If your control can fit within these bounds return that value.
            result = specSize;
        }
        Environment.getExternalStorageDirectory();
        return result;
    }

    public void closeTo100(){
        m_ivTop.startAnimation(anim_down100);
        m_ivBottom.startAnimation(anim_up100);
    }

    public void openAll(){
        m_ivTop.startAnimation(anim_upAll);
        m_ivBottom.startAnimation(anim_downAll);
    }

    public void init(){
        openAll();
    }
}
