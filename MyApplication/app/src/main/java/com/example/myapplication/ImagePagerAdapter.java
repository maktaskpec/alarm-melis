package com.example.myapplication;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;
import android.media.MediaPlayer;
public class ImagePagerAdapter extends PagerAdapter {
    private Context context;
    private int[] imageArray = {R.drawable.havali_horoz, R.drawable.yumusak_horoz, R.drawable.alpha_horoz, R.drawable.futbolcu_civciv, R.drawable.kutuphaneci_tavuk};
    private int[] soundArray = {R.raw.horoz_ses1, R.raw.horoz_ses2, R.raw.horoz_ses3, R.raw.horoz_sesi4, R.raw.horoz_ses1};
    private MediaPlayer mediaPlayer;

    public ImagePagerAdapter(MainActivity context) {
        this.context = context;
        this.mediaPlayer = new MediaPlayer();
    }

    @Override
    public int getCount() {
        return imageArray.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.viewpager_item, container, false);

        ImageView imageView = itemView.findViewById(R.id.imageViewh);
        imageView.setImageResource(imageArray[position]);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                context.getResources().getDimensionPixelSize(R.dimen.new_width),
                context.getResources().getDimensionPixelSize(R.dimen.new_height)
        );

        imageView.setLayoutParams(layoutParams);

        container.addView(itemView);


        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
    public int getSoundResource(int position) {
        return soundArray[position];
    }
}
