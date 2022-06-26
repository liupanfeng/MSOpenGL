package com.meishe.msopengl.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * @author : lpf
 * @FileName: MSRecordButton
 * @Date: 2022/6/26 21:46
 * @Description: 按住录制View
 */
public class MSRecordButton extends AppCompatTextView {

    private OnRecordListener mListener;


    public MSRecordButton(@NonNull Context context) {
        this(context, null);
    }

    public MSRecordButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MSRecordButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mListener == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                mListener.onStartRecording();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                mListener.onStopRecording();
                break;
        }
        return true;
    }

    public void setOnRecordListener(OnRecordListener mListener) {
        this.mListener = mListener;
    }

    public interface OnRecordListener {
        void onStartRecording();

        void onStopRecording();
    }

}
