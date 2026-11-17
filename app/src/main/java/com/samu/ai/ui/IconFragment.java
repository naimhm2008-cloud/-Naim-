package com.samu.ai.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.service.FloatingIconService;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Icon tab: pick any picture (gallery), any emoji (keyboard), any shape,
 * then the floating icon updates instantly.
 */
public class IconFragment extends Fragment {

    private static final int REQ_PICK = 21;

    private android.widget.FrameLayout preview;
    private EditText emojiInput;
    private String currentShape = "circle";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_icon, container, false);
        preview = v.findViewById(R.id.icon_preview);
        emojiInput = v.findViewById(R.id.icon_emoji_input);

        Button pick = v.findViewById(R.id.btn_icon_gallery);
        Button useEmoji = v.findViewById(R.id.btn_icon_emoji);
        Button reset = v.findViewById(R.id.btn_icon_reset);
        Button shapeCircle = v.findViewById(R.id.btn_shape_circle);
        Button shapeSquare = v.findViewById(R.id.btn_shape_square);
        Button shapeHeart = v.findViewById(R.id.btn_shape_heart);
        Button shapeStar = v.findViewById(R.id.btn_shape_star);
        Button shapeRobot = v.findViewById(R.id.btn_shape_robot);
        Button shapeFree = v.findViewById(R.id.btn_shape_free);

        pick.setOnClickListener(vg -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED
                    && android.os.Build.VERSION.SDK_INT >= 33) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQ_PICK);
                return;
            }
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, REQ_PICK);
        });

        useEmoji.setText(R.string.icon_use_emoji);
        useEmoji.setOnClickListener(vg -> {
            String emoji = emojiInput.getText().toString().trim();
            if (emoji.isEmpty()) {
                Toast.makeText(getContext(), R.string.icon_enter_emoji, Toast.LENGTH_SHORT).show();
                return;
            }
            saveIconData(o -> o.put("emoji", emoji));
            preview.removeAllViews();
            android.widget.TextView tv = new android.widget.TextView(requireContext());
            tv.setText(emoji);
            tv.setTextSize(48);
            tv.setGravity(android.view.Gravity.CENTER);
            preview.addView(tv);
            FloatingIconService.refresh(requireContext());
        });

        reset.setOnClickListener(vg -> {
            saveIconData(o -> { });
            showDefaultPreview();
            FloatingIconService.refresh(requireContext());
        });

        View.OnClickListener shapeClick = vg -> {
            currentShape = (String) ((Button) vg).getTag();
            saveIconData(o -> o.put("shape", currentShape));
            FloatingIconService.refresh(requireContext());
            Toast.makeText(getContext(), R.string.icon_shape_set, Toast.LENGTH_SHORT).show();
        };
        shapeCircle.setTag("circle");
        shapeSquare.setTag("square");
        shapeHeart.setTag("heart");
        shapeStar.setTag("star");
        shapeRobot.setTag("robot");
        shapeFree.setTag("none");
        for (Button b : new Button[]{shapeCircle, shapeSquare, shapeHeart, shapeStar, shapeRobot, shapeFree})
            b.setOnClickListener(shapeClick);

        showDefaultPreview();
        return v;
    }

    private void showDefaultPreview() {
        preview.removeAllViews();
        ImageView iv = new ImageView(requireContext());
        iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.samu_logo));
        iv.setAdjustViewBounds(true);
        preview.addView(iv);
    }

    private void saveIconData(JSONObjectEditor editor) {
        try {
            String data = SettingsStore.get(requireContext()).getIconData();
            JSONObject o = data.isEmpty() ? new JSONObject() : new JSONObject(data);
            editor.edit(o);
            SettingsStore.get(requireContext()).setIconData(o.toString());
        } catch (Exception ignored) { }
    }

    private interface JSONObjectEditor {
        void edit(JSONObject o) throws Exception;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK || resultCode != Activity.RESULT_OK
                || data == null || data.getData() == null) return;
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(data.getData());
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            if (bmp == null) return;

            File dir = new File(requireContext().getFilesDir(), "icons");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "floating_icon.png");
            FileOutputStream fo = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 95, fo);
            fo.close();

            String path = f.getAbsolutePath();
            saveIconData(o -> {
                o.remove("emoji");
                o.put("path", path);
            });
            preview.removeAllViews();
            ImageView iv = new ImageView(requireContext());
            iv.setImageBitmap(bmp);
            iv.setAdjustViewBounds(true);
            preview.addView(iv);
            FloatingIconService.refresh(requireContext());
            Toast.makeText(getContext(), R.string.icon_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.icon_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
