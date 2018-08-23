package com.xxun.xunlauncher.utils;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.xxun.xunlauncher.R;

import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;


/**
 * Created by guohongcheng on 2018/8/23.
 * 逻辑：
 * 1.输入密码，正确时进入支付宝，并且重置错误次数（WRONG_COUNT）标志位
 * 2.输入密码错误，连续4次将会锁定10分钟
 * 3.10分钟后，取消锁定
 */

public class PayDialogFragment extends DialogFragment implements PwdEditText.OnTextInputListener {

    private static final String TAG = "PayDialogFragment";
    private PwdEditText editText;
    private int mWrongCount = 0;

    private TextView mLockView;

    private static final String FLAG_ZHIFUBAO_PWD = "password_zhifubao";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String WRONG_COUNT = "WrongCount_SP";
    private static final String WRONG_LOCKTIME = "WrongTime_SP";

    private static final int DEFAULT_WRONG_TIME = 4;

    private static final long MIN_10 = 10;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        //去掉dialog的标题，需要在setContentView()之前
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        View view = inflater.inflate(R.layout.layout_pay_dialog, null);

        // 逻辑处理内容
        sharedPreferences = getActivity().getSharedPreferences(FLAG_ZHIFUBAO_PWD, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        mWrongCount = sharedPreferences.getInt(WRONG_COUNT, 0);
        Log.d(TAG, "mWrongCount: " + mWrongCount);

//        ImageView exitImgView = (ImageView) view.findViewById(R.id.iv_exit);
//        exitImgView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                PayDialogFragment.this.dismiss();
//            }
//        });
        editText = (PwdEditText) view.findViewById(R.id.et_input);
        editText.setOnTextInputListener(this);
        PwdKeyboardView keyboardView = (PwdKeyboardView) view.findViewById(R.id.key_board);
//        keyboardView.setPreviewEnabled(true);
        keyboardView.setOnKeyListener(new PwdKeyboardView.OnKeyListener() {
            @Override
            public void onInput(String text) {
                Log.d(TAG, "onInput: text = " + text);
                editText.append(text);
                String content = editText.getText().toString();
                Log.d(TAG, "onInput: content = " + content);
            }

            @Override
            public void onDelete() {
                Log.d(TAG, "onDelete: ");
                String content = editText.getText().toString();
                if (content.length() > 0) {
                    editText.setText(content.substring(0, content.length() - 1));
                }
            }
        });

        // 重要逻辑：
        // 当进入此界面时，要先判断是否锁定
        mLockView = (TextView) view.findViewById(R.id.lockview);
        // 密码错误次数超过4次
        if (mWrongCount >= DEFAULT_WRONG_TIME) {
            // 与上次密码输入错误的时间差：是否已经超过10分钟
            if (isInLockTime()) {
                mLockView.setVisibility(View.VISIBLE);
            } else {
                mLockView.setVisibility(View.GONE);

                // 重置密码输入错误次数：0次
                mWrongCount = 0;
                editor.putInt(WRONG_COUNT, mWrongCount);
                editor.commit();
            }
        } else {
            mLockView.setVisibility(View.GONE);
        }
        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.windowAnimations = R.style.DialogFragmentAnimation;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        //设置dialog的位置在底部
        lp.gravity = Gravity.BOTTOM;
        window.setAttributes(lp);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onComplete(String result) {
        Log.d(TAG, "onComplete: result = " + result);
        Toast.makeText(getContext(), "input complete : " + result, Toast.LENGTH_SHORT).show();
        String content = editText.getText().toString();
        if (content.equals("1234")) {
            Toast.makeText(getContext(), "成功", Toast.LENGTH_SHORT).show();
        } else {
            editText.setText(null);
            mWrongCount++;

            // 将密码输入失败次数持久存储
            editor.putInt(WRONG_COUNT, mWrongCount);
            editor.commit();

            // 密码输入次数超过4次
            if (mWrongCount >= DEFAULT_WRONG_TIME) {
                mLockView.setVisibility(View.VISIBLE);

                // 输入错误4次，将最后一次输入错误的时间持久化存储
                SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String date = sDateFormat.format(new java.util.Date());
                editor.putString(WRONG_LOCKTIME, date);
                editor.commit();
            } else {
                mLockView.setVisibility(View.GONE);
            }
        }
    }

    private boolean isInLockTime() {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String timeLast = sharedPreferences.getString(WRONG_LOCKTIME, "2006-05-26 12:00:00");
            String timeNow = format.format(new java.util.Date());
            Log.d(TAG, "timeLast " + timeLast + " timeNow " + timeNow);

            Date lastTime = format.parse(timeLast);
            Date nowTime = format.parse(timeNow);

            long diff = nowTime.getTime() - lastTime.getTime(); //两时间差，精确到毫秒
            long minDiff = diff / (60 * 1000);

            Log.d(TAG, " minDiff " + minDiff);
            return minDiff < MIN_10;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
