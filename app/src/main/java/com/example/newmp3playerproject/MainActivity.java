package com.example.newmp3playerproject;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //listtview1,2_xml
    private DrawerLayout drawerLayout;
    private LinearLayout linearLayout1;
    private LinearLayout linearLayout2;
    private ListView listView1;
    private ListView listView2;
    private Button btn1_Close;
    private Button btn2_Close;

    //listview_item_xml
    private ImageView ivAlbum;
    private TextView tvTitle;
    private TextView tvArtist;

    //Main_xml
    private ImageView iv_Album;
    private ImageView iv_Shuffle;
    private ImageView iv_Previous;
    private ImageView iv_Play;
    private ImageView iv_Pause;
    private ImageView iv_Next;
    private ImageView iv_Star;
    private TextView tv_Title;
    private TextView tv_Artist;
    private SeekBar seekBar;

    //이미지 사이즈
    private static final int MAX_IMAGE_SIZE = 200;
    private static final BitmapFactory.Options options = new BitmapFactory.Options();

    //스레드 관련된 변수
    private boolean isPlaying = true;
    private boolean shuffleFlag = false;

    // 데이터 관련된 변수
    private int position;
    private MyDBHelper myDBHelper;
    private ArrayList<MusicData> arrayList = new ArrayList<MusicData>();
    private ArrayList<MusicData> likeList = new ArrayList<>();
    private ArrayList<MusicData> currentList = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private MenuItem menu_Item1;
    private MenuItem menu_Item2;
    private MusicData musicData;
    private MusicAdapter musicAdapter;
    private long backBtnTime = 0;
    private static final String MY_LIKE_YES = "yes";
    private static final String MY_LIKE_NO = "no";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //listtview1,2_xml 바인딩
        linearLayout1 = findViewById(R.id.linearLayout1);
        linearLayout2 = findViewById(R.id.linearLayout2);
        listView1 = findViewById(R.id.listView1);
        listView2 = findViewById(R.id.listView2);
        btn1_Close = findViewById(R.id.btn1_Close);
        btn2_Close = findViewById(R.id.btn2_Close);
        drawerLayout = findViewById(R.id.drawerLayout);

        //listview_item_xml 바인딩
        ivAlbum = findViewById(R.id.ivAlbum);
        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);

        //menu_xml 바인딩
        menu_Item1 = findViewById(R.id.menu_Item1);
        menu_Item2 = findViewById(R.id.menu_Item2);

        //main_xml 바인딩
        iv_Album = findViewById(R.id.iv_Album);
        iv_Shuffle = findViewById(R.id.iv_Shuffle);
        iv_Previous = findViewById(R.id.iv_Previous);
        iv_Play = findViewById(R.id.iv_Play);
        iv_Pause = findViewById(R.id.iv_Pause);
        iv_Next = findViewById(R.id.iv_Next);
        iv_Star = findViewById(R.id.iv_Star);
        tv_Title = findViewById(R.id.tv_Title);
        tv_Artist = findViewById(R.id.tv_Artist);
        seekBar = findViewById(R.id.seekBar);

        //listtview1,2_xml 이벤트 등록
        btn1_Close.setOnClickListener(this);
        btn2_Close.setOnClickListener(this);

        //main_xml
        iv_Shuffle.setOnClickListener(this);
        iv_Previous.setOnClickListener(this);
        iv_Play.setOnClickListener(this);
        iv_Pause.setOnClickListener(this);
        iv_Next.setOnClickListener(this);
        iv_Star.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
                if (seekBar.getProgress() > 0 && iv_Play.getVisibility() == View.GONE) {
                    mediaPlayer.start();
                }
            }
        });

        //Content Provider 통해서 음악파일을 가져와야 한다
        getMusicList();

        //DataBase에서 음악파일 insert를 진행한다.
        myDBHelper = new MyDBHelper(this);
        myDBHelper.insertMusicDataAll(myDBHelper.getWritableDatabase(), arrayList);

        //DataBase에서 모든 리스트와 좋아요 리스트를 가져온다
        arrayList = myDBHelper.getTableAllMusicList(myDBHelper.getReadableDatabase());
        //어댑터 장착
        musicAdapter = new MusicAdapter(this, arrayList);
        listView1.setAdapter(musicAdapter);
        musicAdapter.notifyDataSetChanged();

        //미디어 플레이어 객체를 가져옴
        mediaPlayer = new MediaPlayer();

        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                changeMusic(position);
            }
        });


        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                changeMusic(position);
            }
        });

        //시크바를 동시에 진행함.
        ProgressUpdate progressUpdate = new ProgressUpdate();
        progressUpdate.start();

        //미디어플레이어에서 노래가 끝나면 이벤트 처리하는 기능
        mediaPlayer.setOnCompletionListener(completionListener);

        //멈춤화면 안보이게 설정
        iv_Pause.setVisibility(View.GONE);

    }//end of on create

    //음악이 끝난후 다음곡을 실행시킨다.
    private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if (position + 1 < currentList.size()) {
                position++;
                playMusic(currentList.get(position));

                Bitmap bitmap = getAlbumImage(getApplicationContext(), Long.parseLong(currentList.get(position).getAlbumId()), MAX_IMAGE_SIZE);
                if (bitmap != null) {
                    iv_Album.setImageBitmap(bitmap);
                } else {
                    iv_Album.setImageResource(R.drawable.musicnull);
                }

            }
        }
    };

    //메뉴창 가져오기
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menulist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //메뉴아이템 이벤트 설정
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_Item1:
                drawerLayout.openDrawer(linearLayout1);
                drawerLayout.closeDrawer(linearLayout2);
                if (drawerLayout.isDrawerOpen(linearLayout1)) {
                    drawerLayout.closeDrawer(linearLayout1);
                }

                //어댑터 장착
                arrayList = myDBHelper.getTableAllMusicList(myDBHelper.getReadableDatabase());
                currentList = arrayList;
                musicAdapter = new MusicAdapter(this, currentList);
                listView1.setAdapter(musicAdapter);
                musicAdapter.notifyDataSetChanged();
                break;

            case R.id.menu_Item2:
                drawerLayout.openDrawer(linearLayout2);
                drawerLayout.closeDrawer(linearLayout1);
                if (drawerLayout.isDrawerOpen(linearLayout2)) {
                    drawerLayout.closeDrawer(linearLayout2);
                }

                likeList = myDBHelper.getTableLikeMusicList(myDBHelper.getReadableDatabase());
                currentList = likeList;
                musicAdapter = new MusicAdapter(this, currentList);
                listView2.setAdapter(musicAdapter);
                musicAdapter.notifyDataSetChanged();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);

    }//end of onCreateOptionsMenu


    //이벤트 설정
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1_Close:
                drawerLayout.closeDrawer(linearLayout1);
                break;

            case R.id.btn2_Close:
                drawerLayout.closeDrawer(linearLayout2);
                break;

            case R.id.iv_Shuffle:
                if (shuffleFlag == false) {
                    iv_Shuffle.setImageResource(R.drawable.shuffle1);
                    shuffleFlag = true;
                } else {
                    iv_Shuffle.setImageResource(R.drawable.shuffle);
                    shuffleFlag = false;
                }
                Bitmap bitmap = getAlbumImage(getApplicationContext(), Long.parseLong(currentList.get(position).getAlbumId()), MAX_IMAGE_SIZE);
                if (bitmap != null) {
                    iv_Album.setImageBitmap(bitmap);
                } else {
                    iv_Album.setImageResource(R.drawable.musicnull);
                }
                break;

            case R.id.iv_Previous:
                if (shuffleFlag == false) {
                    position--;
                    if(position == -1) {
                        position = currentList.size() - 1;
                    }
                    changeMusic(position);

                } else {
                    position = (int) (Math.random() * ((currentList.size() - 1) - 0 + 1) + 0);
                    changeMusic(position);
                }
                break;

            case R.id.iv_Play:
                playMusic(currentList.get(position));
                iv_Pause.setVisibility(View.VISIBLE);
                iv_Play.setVisibility(View.GONE);
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition());
                break;

            case R.id.iv_Pause:
                iv_Pause.setVisibility(View.GONE);
                iv_Play.setVisibility(View.VISIBLE);
                mediaPlayer.pause();
                break;

            case R.id.iv_Next:
                if (shuffleFlag == false) {
                    position++;
                    if (position == currentList.size()) {
                        position = 0;
                    }
                    changeMusic(position);

                } else {
                    position = (int) (Math.random() * ((currentList.size() - 1) - 0 + 1) + 0);
                    changeMusic(position);
                }
                break;
            case R.id.iv_Star:
                MyDBHelper myDBHelper = new MyDBHelper(this);
                if (currentList.get(position).getStar().equals(MY_LIKE_YES)) {
                    myDBHelper.updateMusicTBLMyLike(myDBHelper.getWritableDatabase(), currentList.get(position).getId(), MY_LIKE_NO);
                    currentList.get(position).setStar(MY_LIKE_NO);
                    iv_Star.setImageResource(R.drawable.emptystar);

                    Log.d("확인", "" + position);

                } else {
                    myDBHelper.updateMusicTBLMyLike(myDBHelper.getWritableDatabase(), currentList.get(position).getId(), MY_LIKE_YES);
                    currentList.get(position).setStar(MY_LIKE_YES);
                    iv_Star.setImageResource(R.drawable.fullstar);

                    Log.d("확인", "" + position);
                }
                break;
        }
    }//end of onClick

    //음악, 시크바, 뮤직 변경
    public void changeMusic(int position) {
        playMusic(currentList.get(position));

        Bitmap bitmap = getAlbumImage(getApplicationContext(), Long.parseLong(currentList.get(position).getAlbumId()), MAX_IMAGE_SIZE);
        if (bitmap != null) {
            iv_Album.setImageBitmap(bitmap);
        } else {
            iv_Album.setImageResource(R.drawable.musicnull);
        }

        seekBar.setProgress(0);
        if (currentList.get(position).getStar().equals(MY_LIKE_YES)) {
            iv_Star.setImageResource(R.drawable.fullstar);
        } else {
            iv_Star.setImageResource(R.drawable.emptystar);
        }

        this.position = position;
    }

    //이미지, 아티스트명 음악, 시크바등 관련함수 및 음악을 시작함.
    public void playMusic(MusicData musicData) {

        try {
            seekBar.setProgress(0);

            //선택된 노래 이미지 위치를 가져온다.
            //Long.parseLong(currentList.get(position).getAlbumId())로 해야한다 int안됨
            Bitmap bitmap = getAlbumImage(getApplicationContext(), Long.parseLong(currentList.get(position).getAlbumId()), MAX_IMAGE_SIZE);
            if (bitmap != null) {
                iv_Album.setImageBitmap(bitmap);
            } else {
                iv_Album.setImageResource(R.drawable.musicnull);
            }

            tv_Title.setText(musicData.getTitle());
            tv_Artist.setText(musicData.getArtist());

            if (musicData.getStar().equals(MY_LIKE_YES)) {
                iv_Star.setImageResource(R.drawable.fullstar);
            } else {
                iv_Star.setImageResource(R.drawable.emptystar);
            }

            Uri musicURI = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + musicData.getId());

            mediaPlayer.reset();

            //듣고자하는 파일을 프로바이더가 가져온다.
            mediaPlayer.setDataSource(this, musicURI);
            mediaPlayer.prepare();
            mediaPlayer.start();
            seekBar.setMax(mediaPlayer.getDuration());
            if (mediaPlayer.isPlaying()) {
                iv_Play.setVisibility(View.GONE);
                iv_Pause.setVisibility(View.VISIBLE);
            } else {
                iv_Play.setVisibility(View.VISIBLE);
                iv_Pause.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e("음악플레이어", "playMusic() 에러발생" + e.toString());
        }
    }

    //Content Provider에서 contentResolver를 이용해서 음악파일을 가져온다.
    //listView1에 musicData를 뿌려준다.
    public void getMusicList() {
        //퍼미션허용요청
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                MODE_PRIVATE);
        Cursor cursor = null;
        try {
            //contentResolver를 이용해서 음악파일을 가져온다(아이디,앨범아이디,타이틀,아티스트)
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.ALBUM_ID,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST}, MediaStore.Audio.Media.DATA + " like ? ", new String[]{"%mymusic%"}, MediaStore.Audio.Media.TITLE);
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                String albumId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

                MusicData musicData = new MusicData(id, albumId, title, artist, MY_LIKE_NO);
                arrayList.add(musicData);
            }
        } catch (Exception e) {
            Log.d("음악플레이어", "getMusicList() 외부에서 음악파일가져오기 오류" + e.toString());
        } finally {
            cursor.close();
        }
    }//end of getMusicList

    //이미지를 가져온다.
    private Bitmap getAlbumImage(Context context, long albumId, int maxImageSize) {
        //이미지를 가져올려면 contentresolver와 앨범 이미지 아이디를 통해서 Uri를 가져온다.
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://media/external/audio/albumart/" + albumId);
        if (uri != null) {
            //이미지를 가져오기 위해서
            ParcelFileDescriptor pfd = null;

            try {
                pfd = contentResolver.openFileDescriptor(uri, "r");
                Bitmap bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, options);

                if (bitmap != null) {
                    //비트맵 사이즈를 체크해서 내가 정한 가로크기와 세로크기가 아니면 다시 비트맵 크기 재설정해서 비트맵을 만든다
                    if (options.outHeight != maxImageSize || options.outWidth != maxImageSize) {
                        Bitmap tempBitmap = Bitmap.createScaledBitmap(bitmap, maxImageSize, maxImageSize, true);
                        bitmap.recycle();
                        bitmap = tempBitmap;
                    }
                }
                return bitmap;
            } catch (FileNotFoundException e) {
                Log.e("음악플레이어", "비트맵 이미지 변환에서 오류" + e.toString());
            } finally {
                if (pfd != null) {
                    try {
                        pfd.close();
                    } catch (IOException e) {
                        Log.e("음악플레이어", "ParcelFileDescriptor 닫기 오류" + e.toString());
                    }
                }
            }
        }
        return null;
    }//end of getAlbumImage

    //쓰래드 1번방식
    class ProgressUpdate extends Thread {
        @Override
        public void run() {
            while (isPlaying) {
                try {
                    Thread.sleep(300);
                    if (mediaPlayer.isPlaying()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("ProgressUpdate", e.getMessage());
                }
            }
        }
    }

    //두번 눌러서 종료
    @Override
    public void onBackPressed() {
        long curTime = System.currentTimeMillis();
        long gapTime = curTime - backBtnTime;

        if (0 <= gapTime && 2000 >= gapTime) {
            super.onBackPressed();
        } else {
            backBtnTime = curTime;
            Toast.makeText(this, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPlaying = false;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }//end of onDestroy
}