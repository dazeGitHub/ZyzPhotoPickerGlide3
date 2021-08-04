package me.iwf.photopicker;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import me.iwf.photopicker.customui.ActivityData;
import me.iwf.photopicker.customui.ICustomMadeUi;
import me.iwf.photopicker.customui.ISelectedAction;
import me.iwf.photopicker.entity.Photo;
import me.iwf.photopicker.event.OnItemCheckListener;
import me.iwf.photopicker.fragment.ImagePagerFragment;
import me.iwf.photopicker.fragment.PhotoPickerFragment;

import static android.widget.Toast.LENGTH_LONG;

public class PhotoPickerActivity extends AppCompatActivity implements ISelectedAction {

    private PhotoPickerFragment pickerFragment;
    private ImagePagerFragment imagePagerFragment;
    private MenuItem menuDoneItem;

    private int maxCount = PhotoPicker.DEFAULT_MAX_COUNT;
    private int mAlreadySelectedCount = 0;
    private int mCurMaxCanSelectCount = 0;

    /**
     * to prevent multiple calls to inflate menu
     */
    private boolean menuIsInflated = false;

    private boolean showGif = false;
    private ArrayList<String> originalPhotos = null;
    private ICustomMadeUi iCustomeMadeUi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean showCamera = getIntent().getBooleanExtra(PhotoPicker.EXTRA_SHOW_CAMERA, true);
        boolean showGif = getIntent().getBooleanExtra(PhotoPicker.EXTRA_SHOW_GIF, false);
        boolean previewEnabled = getIntent().getBooleanExtra(PhotoPicker.EXTRA_PREVIEW_ENABLED, true);
        iCustomeMadeUi = ActivityData.INSTANCE.getCustomView();
        setShowGif(showGif);

        setContentView(R.layout.__picker_activity_photo_picker);

        setTitle();

        maxCount = getIntent().getIntExtra(PhotoPicker.EXTRA_MAX_COUNT, PhotoPicker.DEFAULT_MAX_COUNT);
        mAlreadySelectedCount = getIntent().getIntExtra(PhotoPicker.EXTRA_ALREADY_SELECTED_COUNT, 0);

        if(maxCount >= mAlreadySelectedCount){
            mCurMaxCanSelectCount = maxCount - mAlreadySelectedCount;
        }else{
            mCurMaxCanSelectCount = maxCount;
        }

        int columnNumber = getIntent().getIntExtra(PhotoPicker.EXTRA_GRID_COLUMN, PhotoPicker.DEFAULT_COLUMN_NUMBER);
        originalPhotos = getIntent().getStringArrayListExtra(PhotoPicker.EXTRA_ORIGINAL_PHOTOS);

        pickerFragment = (PhotoPickerFragment) getSupportFragmentManager().findFragmentByTag("tag");
        if (pickerFragment == null) {
            pickerFragment = PhotoPickerFragment
                    .newInstance(showCamera, showGif, previewEnabled, columnNumber, mCurMaxCanSelectCount, originalPhotos);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, pickerFragment, "tag")
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
        }

        pickerFragment.getPhotoGridAdapter().setOnItemCheckListener(new OnItemCheckListener() {
            @Override
            public boolean onItemCheck(int position, Photo photo, final int selectedItemCount) {

                if (menuDoneItem != null) {
                    menuDoneItem.setEnabled(selectedItemCount > 0);
                }

                if (mCurMaxCanSelectCount <= 1) {
                    List<String> photos = pickerFragment.getPhotoGridAdapter().getSelectedPhotos();
                    if (!photos.contains(photo.getPath())) {
                        photos.clear();
                        pickerFragment.getPhotoGridAdapter().notifyDataSetChanged();
                    }
                    return true;
                }

                if (selectedItemCount > mCurMaxCanSelectCount) {
                    Toast.makeText(getActivity(), getString(R.string.__picker_over_max_count_tips, maxCount),
                            LENGTH_LONG).show();
                    return false;
                }

                if (iCustomeMadeUi != null) {
                    iCustomeMadeUi.setTitleCount(mAlreadySelectedCount + selectedItemCount, maxCount);
                } else {
                    if (menuDoneItem != null) {
                        if (mCurMaxCanSelectCount > 1) {
                            menuDoneItem.setTitle(getString(R.string.__picker_done_with_count, mAlreadySelectedCount + selectedItemCount, maxCount));
                        } else {
                            menuDoneItem.setTitle(getString(R.string.__picker_done));
                        }
                    }
                }

                return true;

            }
        });

    }

    private void setTitle() {
        if (iCustomeMadeUi != null) {
            menuIsInflated = true;
            FrameLayout flTitleRoout = findViewById(R.id.fl_title_root);
            flTitleRoout.removeAllViews();
            flTitleRoout.addView(iCustomeMadeUi.titleLayout(this, this));
        } else {
            Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(mToolbar);
            setTitle(R.string.__picker_title);

            ActionBar actionBar = getSupportActionBar();
            assert actionBar != null;
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                actionBar.setElevation(25);
            }
        }
    }

    //刷新右上角按钮文案
    public void updateTitleDoneItem() {
        if (menuIsInflated) {
            if (pickerFragment != null && pickerFragment.isResumed()) {
                List<String> photos = pickerFragment.getPhotoGridAdapter().getSelectedPhotos();
                int size = photos == null ? 0 : photos.size();
                if (iCustomeMadeUi == null && menuDoneItem != null) {
                    menuDoneItem.setEnabled(size > 0);
                    if (maxCount > 1) {
                        menuDoneItem.setTitle(getString(R.string.__picker_done_with_count, size, maxCount));
                    } else {
                        menuDoneItem.setTitle(getString(R.string.__picker_done));
                    }
                } else {
                    if (iCustomeMadeUi != null) {
                        iCustomeMadeUi.setTitleCount(mAlreadySelectedCount + size, maxCount);
                    }
                }
            } else if (imagePagerFragment != null && imagePagerFragment.isResumed()) {
                //预览界面 完成总是可点的，没选就把默认当前图片
                if (menuDoneItem != null) {
                    menuDoneItem.setEnabled(true);
                }
            }

        }
    }

    /**
     * Overriding this method allows us to run our exit animation first, then exiting
     * the activity when it complete.
     */
    @Override
    public void onBackPressed() {
        if (imagePagerFragment != null && imagePagerFragment.isVisible()) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            }
        } else {
            super.onBackPressed();
        }
    }


    public void addImagePagerFragment(ImagePagerFragment imagePagerFragment) {
        this.imagePagerFragment = imagePagerFragment;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, this.imagePagerFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!menuIsInflated) {
            getMenuInflater().inflate(R.menu.__picker_menu_picker, menu);
            menuDoneItem = menu.findItem(R.id.done);
            if (originalPhotos != null && originalPhotos.size() > 0) {
                menuDoneItem.setEnabled(true);
                menuDoneItem.setTitle(
                        getString(R.string.__picker_done_with_count, originalPhotos.size(), maxCount));
            } else {
                menuDoneItem.setEnabled(false);
            }
            menuIsInflated = true;
            return true;
        }
        return false;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        if (item.getItemId() == R.id.done) {
            done();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityData.INSTANCE.setCustomView(null);
    }

    public PhotoPickerActivity getActivity() {
        return this;
    }

    public boolean isShowGif() {
        return showGif;
    }

    public void setShowGif(boolean showGif) {
        this.showGif = showGif;
    }

    @Override
    public void back() {
        onBackPressed();
    }

    @Override
    public void done() {
        Intent intent = new Intent();
        ArrayList<String> selectedPhotos = null;
        if (pickerFragment != null) {
            selectedPhotos = pickerFragment.getPhotoGridAdapter().getSelectedPhotoPaths();
        }
        //当在列表没有选择图片，又在详情界面时默认选择当前图片
        if (selectedPhotos.size() <= 0) {
            if (imagePagerFragment != null && imagePagerFragment.isResumed()) {
                // 预览界面
                selectedPhotos = imagePagerFragment.getCurrentPath();
            }
        }
        if (selectedPhotos != null && selectedPhotos.size() > 0) {
            intent.putStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS, selectedPhotos);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
