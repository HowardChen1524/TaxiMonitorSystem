package com.example.obd_app;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "Main";
    private static final boolean D = true;
    private ListView mListView;
    private ArrayList<ListItem> mList;
    private APPSetting mAPPSetting;
    private int mAdapter_choose;
    private int mCar_choose;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_settings);
        mAPPSetting = (APPSetting)getApplication();
        mListView = findViewById(R.id.listView_settings);

        // 获取Resources对象
        Resources res = this.getResources();

        mList = new ArrayList<>();

        // 初始化data，装载八组数据到数组链表mList中
        ListItem item = new ListItem();
        item.setImage(res.getDrawable(R.drawable.icon_obdii));
        item.setTitle("OBD Adapter Setting");
        mList.add(item);

        item = new ListItem();
        item.setImage(res.getDrawable(R.drawable.icon_car_type));
        item.setTitle("Car Setting");
        mList.add(item);

        final SettingsListViewAdapter adapter = new SettingsListViewAdapter();
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position,long id) {
                // parent: 識別哪個listview
                // view: 當前listview裡item的view的布局，可以用這個view獲取裡面控件的id的id後操作該控件
                // position: 當前item在listview adapter的位置
                // id: 當前item在listview裡的第幾行的位置(0開始)
                // TODO Auto-generated method stub
                switch((int)id){
                    case 0:
                        String[] OBD_items = mAPPSetting.getOBDAdapterNameList();
                        AlertDialog.Builder mOBDAdapter_builder = new AlertDialog.Builder(SettingsActivity. this)
                                .setTitle("OBD Adapter Setting")
                                .setIcon(R.drawable.icon_obdii);
                        mOBDAdapter_builder.setSingleChoiceItems(OBD_items, mAPPSetting.getOBDAdapter(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAdapter_choose = which;
                            }
                        });
                        mOBDAdapter_builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAPPSetting.setOBDAdapter(mAdapter_choose);//把預設值改成選擇的
                                System.out.println(mAPPSetting.getOBDAdapter());
                                dialog.dismiss();//結束對話框
                            }
                        });
                        mOBDAdapter_builder.show();
                        break;
                    case 1:
                        String[] Car_items = mAPPSetting.getCarTypeList();
                        AlertDialog.Builder mCar_builder = new AlertDialog.Builder(SettingsActivity.this)
                                .setTitle("Car Setting")
                                .setIcon(R.drawable.icon_car_type);
                        mCar_builder.setSingleChoiceItems(Car_items, mAPPSetting.getCar(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mCar_choose = which;
                            }
                        });
                        mCar_builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAPPSetting.setCar(mCar_choose);//把預設值改成選擇的
                                System.out.println(mAPPSetting.getCar());
                                dialog.dismiss();//結束對話框
                            }
                        });
                        mCar_builder.show();
                        break;
                    default:
                        showToast("Error");
                        break;
                }
            }
        });
    }

    class SettingsListViewAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListItemView listItemView;
            if (convertView == null) {
                convertView = LayoutInflater.from(SettingsActivity.this).inflate(R.layout.item_settings, null);

                listItemView = new ListItemView();
                listItemView.imageView = convertView.findViewById(R.id.image);
                listItemView.textView = convertView.findViewById(R.id.title);

                convertView.setTag(listItemView);
            } else {
                listItemView = (ListItemView) convertView.getTag();
            }

            Drawable img = mList.get(position).getImage();
            String title = mList.get(position).getTitle();

            listItemView.imageView.setImageDrawable(img);
            listItemView.textView.setText(title);

            return convertView;
        }
    }

    class ListItemView {
        ImageView imageView;
        TextView textView;
    }

    class ListItem {
        private Drawable image;
        private String title;

        public Drawable getImage() {
            return image;
        }

        public void setImage(Drawable image) {
            this.image = image;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    //Toast message
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

