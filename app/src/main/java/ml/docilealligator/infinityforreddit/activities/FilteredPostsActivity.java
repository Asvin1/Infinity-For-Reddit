package ml.docilealligator.infinityforreddit.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.r0adkll.slidr.Slidr;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.docilealligator.infinityforreddit.ActivityToolbarInterface;
import ml.docilealligator.infinityforreddit.FragmentCommunicator;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.MarkPostAsReadInterface;
import ml.docilealligator.infinityforreddit.PostFilter;
import ml.docilealligator.infinityforreddit.PostFragmentContentScrollingInterface;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.RedditDataRoomDatabase;
import ml.docilealligator.infinityforreddit.SortType;
import ml.docilealligator.infinityforreddit.SortTypeSelectionCallback;
import ml.docilealligator.infinityforreddit.asynctasks.GetCurrentAccountAsyncTask;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.FilteredThingFABMoreOptionsBottomSheetFragment;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.PostLayoutBottomSheetFragment;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.SearchPostSortTypeBottomSheetFragment;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.SortTimeBottomSheetFragment;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.SortTypeBottomSheetFragment;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.UserThingSortTypeBottomSheetFragment;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.events.SwitchAccountEvent;
import ml.docilealligator.infinityforreddit.fragments.PostFragment;
import ml.docilealligator.infinityforreddit.post.Post;
import ml.docilealligator.infinityforreddit.post.PostDataSource;
import ml.docilealligator.infinityforreddit.readpost.InsertReadPost;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;

public class FilteredPostsActivity extends BaseActivity implements SortTypeSelectionCallback,
        PostLayoutBottomSheetFragment.PostLayoutSelectionCallback, ActivityToolbarInterface,
        MarkPostAsReadInterface, FilteredThingFABMoreOptionsBottomSheetFragment.FABOptionSelectionCallback,
        PostFragmentContentScrollingInterface {

    public static final String EXTRA_NAME = "ESN";
    public static final String EXTRA_QUERY = "EQ";
    public static final String EXTRA_FILTER = "EF";
    public static final String EXTRA_POST_TYPE = "EPT";
    public static final String EXTRA_USER_WHERE = "EUW";

    private static final String IS_IN_LAZY_MODE_STATE = "IILMS";
    private static final String NULL_ACCESS_TOKEN_STATE = "NATS";
    private static final String ACCESS_TOKEN_STATE = "ATS";
    private static final String ACCOUNT_NAME_STATE = "ANS";
    private static final String FRAGMENT_OUT_STATE = "FOS";
    private static final int CUSTOMIZE_POST_FILTER_ACTIVITY_REQUEST_CODE = 1000;

    @BindView(R.id.coordinator_layout_filtered_thing_activity)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.appbar_layout_filtered_posts_activity)
    AppBarLayout appBarLayout;
    @BindView(R.id.collapsing_toolbar_layout_filtered_posts_activity)
    CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.toolbar_filtered_posts_activity)
    Toolbar toolbar;
    @BindView(R.id.fab_filtered_thing_activity)
    FloatingActionButton fab;
    @Inject
    RedditDataRoomDatabase mRedditDataRoomDatabase;
    @Inject
    @Named("default")
    SharedPreferences mSharedPreferences;
    @Inject
    @Named("post_layout")
    SharedPreferences mPostLayoutSharedPreferences;
    @Inject
    CustomThemeWrapper mCustomThemeWrapper;
    private boolean isInLazyMode = false;
    private boolean mNullAccessToken = false;
    private String mAccessToken;
    private String mAccountName;
    private String name;
    private String userWhere;
    private int postType;
    private PostFragment mFragment;
    private Menu mMenu;
    private AppBarLayout.LayoutParams params;
    private SortTypeBottomSheetFragment bestSortTypeBottomSheetFragment;
    private SortTypeBottomSheetFragment popularAndAllSortTypeBottomSheetFragment;
    private SortTypeBottomSheetFragment subredditSortTypeBottomSheetFragment;
    private SortTypeBottomSheetFragment multiRedditSortTypeBottomSheetFragment;
    private UserThingSortTypeBottomSheetFragment userThingSortTypeBottomSheetFragment;
    private SearchPostSortTypeBottomSheetFragment searchPostSortTypeBottomSheetFragment;
    private SortTimeBottomSheetFragment sortTimeBottomSheetFragment;
    private PostLayoutBottomSheetFragment postLayoutBottomSheetFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((Infinity) getApplication()).getAppComponent().inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filtered_thing);

        ButterKnife.bind(this);

        EventBus.getDefault().register(this);

        applyCustomTheme();

        if (mSharedPreferences.getBoolean(SharedPreferencesUtils.SWIPE_RIGHT_TO_GO_BACK, true)) {
            Slidr.attach(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();

            if (isChangeStatusBarIconColor()) {
                addOnOffsetChangedListener(appBarLayout);
            }

            if (isImmersiveInterface()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    coordinatorLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                } else {
                    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                }
                adjustToolbar(toolbar);

                int navBarHeight = getNavBarHeight();
                if (navBarHeight > 0) {
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
                    params.bottomMargin += navBarHeight;
                    fab.setLayoutParams(params);
                }
            }
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarGoToTop(toolbar);

        params = (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();

        name = getIntent().getStringExtra(EXTRA_NAME);
        postType = getIntent().getIntExtra(EXTRA_POST_TYPE, PostDataSource.TYPE_FRONT_PAGE);
        int filter = getIntent().getIntExtra(EXTRA_FILTER, -1000);
        PostFilter postFilter = new PostFilter();
        switch (filter) {
            case Post.NSFW_TYPE:
                postFilter.onlyNSFW = true;
                break;
            case Post.TEXT_TYPE:
                postFilter.containsTextType = true;
                postFilter.containsLinkType = false;
                postFilter.containsImageType = false;
                postFilter.containsGifType = false;
                postFilter.containsVideoType = false;
                postFilter.containsGalleryType = false;
                break;
            case Post.LINK_TYPE:
                postFilter.containsTextType = false;
                postFilter.containsLinkType = true;
                postFilter.containsImageType = false;
                postFilter.containsGifType = false;
                postFilter.containsVideoType = false;
                postFilter.containsGalleryType = false;
                break;
            case Post.IMAGE_TYPE:
                postFilter.containsTextType = false;
                postFilter.containsLinkType = false;
                postFilter.containsImageType = true;
                postFilter.containsGifType = false;
                postFilter.containsVideoType = false;
                postFilter.containsGalleryType = false;
                break;
            case Post.GIF_TYPE:
                postFilter.containsTextType = false;
                postFilter.containsLinkType = false;
                postFilter.containsImageType = false;
                postFilter.containsGifType = true;
                postFilter.containsVideoType = false;
                postFilter.containsGalleryType = false;
                break;
            case Post.VIDEO_TYPE:
                postFilter.containsTextType = false;
                postFilter.containsLinkType = false;
                postFilter.containsImageType = false;
                postFilter.containsGifType = false;
                postFilter.containsVideoType = true;
                postFilter.containsGalleryType = false;
                break;
            case Post.GALLERY_TYPE:
                postFilter.containsTextType = false;
                postFilter.containsLinkType = false;
                postFilter.containsImageType = false;
                postFilter.containsGifType = false;
                postFilter.containsVideoType = false;
                postFilter.containsGalleryType = true;
                break;
        }

        if (postType == PostDataSource.TYPE_USER) {
            userWhere = getIntent().getStringExtra(EXTRA_USER_WHERE);
            if (userWhere != null && !PostDataSource.USER_WHERE_SUBMITTED.equals(userWhere) && mMenu != null) {
                mMenu.findItem(R.id.action_sort_filtered_thing_activity).setVisible(false);
            }
        }

        if (savedInstanceState != null) {
            isInLazyMode = savedInstanceState.getBoolean(IS_IN_LAZY_MODE_STATE);
            mNullAccessToken = savedInstanceState.getBoolean(NULL_ACCESS_TOKEN_STATE);
            mAccessToken = savedInstanceState.getString(ACCESS_TOKEN_STATE);
            mAccountName = savedInstanceState.getString(ACCOUNT_NAME_STATE);

            if (!mNullAccessToken && mAccessToken == null) {
                getCurrentAccountAndBindView(postFilter);
            } else {
                mFragment = (PostFragment) getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_OUT_STATE);
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_filtered_posts_activity, mFragment).commit();
                bindView(postFilter, false);
            }
        } else {
            getCurrentAccountAndBindView(postFilter);
        }

        postLayoutBottomSheetFragment = new PostLayoutBottomSheetFragment();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mFragment != null) {
            return ((FragmentCommunicator) mFragment).handleKeyDown(keyCode) || super.onKeyDown(keyCode, event);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public SharedPreferences getDefaultSharedPreferences() {
        return mSharedPreferences;
    }

    @Override
    protected CustomThemeWrapper getCustomThemeWrapper() {
        return mCustomThemeWrapper;
    }

    @Override
    protected void applyCustomTheme() {
        coordinatorLayout.setBackgroundColor(mCustomThemeWrapper.getBackgroundColor());
        applyAppBarLayoutAndToolbarTheme(appBarLayout, toolbar);
        applyFABTheme(fab);
    }

    private void getCurrentAccountAndBindView(PostFilter postFilter) {
        new GetCurrentAccountAsyncTask(mRedditDataRoomDatabase.accountDao(), account -> {
            if (account == null) {
                mNullAccessToken = true;
            } else {
                mAccessToken = account.getAccessToken();
                mAccountName = account.getUsername();
            }
            bindView(postFilter, true);
        }).execute();
    }

    private void bindView(PostFilter postFilter, boolean initializeFragment) {
        switch (postType) {
            case PostDataSource.TYPE_FRONT_PAGE:
                getSupportActionBar().setTitle(name);

                bestSortTypeBottomSheetFragment = new SortTypeBottomSheetFragment();
                Bundle bestBundle = new Bundle();
                bestBundle.putBoolean(SortTypeBottomSheetFragment.EXTRA_NO_BEST_TYPE, false);
                bestSortTypeBottomSheetFragment.setArguments(bestBundle);
                break;
            case PostDataSource.TYPE_SEARCH:
                getSupportActionBar().setTitle(R.string.search);

                searchPostSortTypeBottomSheetFragment = new SearchPostSortTypeBottomSheetFragment();
                Bundle searchBundle = new Bundle();
                searchPostSortTypeBottomSheetFragment.setArguments(searchBundle);
                break;
            case PostDataSource.TYPE_SUBREDDIT:
                if (name.equals("popular") || name.equals("all")) {
                    getSupportActionBar().setTitle(name.substring(0, 1).toUpperCase() + name.substring(1));

                    popularAndAllSortTypeBottomSheetFragment = new SortTypeBottomSheetFragment();
                    Bundle popularBundle = new Bundle();
                    popularBundle.putBoolean(SortTypeBottomSheetFragment.EXTRA_NO_BEST_TYPE, true);
                    popularAndAllSortTypeBottomSheetFragment.setArguments(popularBundle);
                } else {
                    String subredditNamePrefixed = "r/" + name;
                    getSupportActionBar().setTitle(subredditNamePrefixed);

                    subredditSortTypeBottomSheetFragment = new SortTypeBottomSheetFragment();
                    Bundle subredditSheetBundle = new Bundle();
                    subredditSheetBundle.putBoolean(SortTypeBottomSheetFragment.EXTRA_NO_BEST_TYPE, true);
                    subredditSortTypeBottomSheetFragment.setArguments(subredditSheetBundle);
                }
                break;
            case PostDataSource.TYPE_MULTI_REDDIT:
                String multiRedditName;
                if (name.endsWith("/")) {
                    multiRedditName = name.substring(0, name.length() - 1);
                    multiRedditName = multiRedditName.substring(multiRedditName.lastIndexOf("/") + 1);
                } else {
                    multiRedditName = name.substring(name.lastIndexOf("/") + 1);
                }
                getSupportActionBar().setTitle(multiRedditName);

                multiRedditSortTypeBottomSheetFragment = new SortTypeBottomSheetFragment();
                Bundle multiRedditBundle = new Bundle();
                multiRedditBundle.putBoolean(SortTypeBottomSheetFragment.EXTRA_NO_BEST_TYPE, true);
                multiRedditSortTypeBottomSheetFragment.setArguments(multiRedditBundle);
                break;
            case PostDataSource.TYPE_USER:
                String usernamePrefixed = "u/" + name;
                getSupportActionBar().setTitle(usernamePrefixed);

                userThingSortTypeBottomSheetFragment = new UserThingSortTypeBottomSheetFragment();
                break;
        }

        sortTimeBottomSheetFragment = new SortTimeBottomSheetFragment();

        if (initializeFragment) {
            mFragment = new PostFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(PostFragment.EXTRA_POST_TYPE, postType);
            bundle.putParcelable(PostFragment.EXTRA_FILTER, postFilter);
            bundle.putString(PostFragment.EXTRA_ACCESS_TOKEN, mAccessToken);
            bundle.putString(PostFragment.EXTRA_ACCOUNT_NAME, mAccountName);
            if (postType == PostDataSource.TYPE_USER) {
                bundle.putString(PostFragment.EXTRA_USER_NAME, name);
                bundle.putString(PostFragment.EXTRA_USER_WHERE, userWhere);
            } else if (postType == PostDataSource.TYPE_SUBREDDIT || postType == PostDataSource.TYPE_MULTI_REDDIT) {
                bundle.putString(PostFragment.EXTRA_NAME, name);
            } else if (postType == PostDataSource.TYPE_SEARCH) {
                bundle.putString(PostFragment.EXTRA_NAME, name);
                bundle.putString(PostFragment.EXTRA_QUERY, getIntent().getStringExtra(EXTRA_QUERY));
            }
            mFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout_filtered_posts_activity, mFragment).commit();
        }

        fab.setOnClickListener(view -> {
            Intent intent = new Intent(this, CustomizePostFilterActivity.class);
            if (mFragment != null) {
                intent.putExtra(CustomizePostFilterActivity.EXTRA_POST_FILTER, mFragment.getPostFilter());
            }
            startActivityForResult(intent, CUSTOMIZE_POST_FILTER_ACTIVITY_REQUEST_CODE);
        });

        if (mAccessToken != null) {
            fab.setOnLongClickListener(view -> {
                FilteredThingFABMoreOptionsBottomSheetFragment filteredThingFABMoreOptionsBottomSheetFragment
                        = new FilteredThingFABMoreOptionsBottomSheetFragment();
                filteredThingFABMoreOptionsBottomSheetFragment.show(getSupportFragmentManager(), filteredThingFABMoreOptionsBottomSheetFragment.getTag());
                return true;
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filtered_posts_activity, menu);
        applyMenuItemTheme(menu);
        mMenu = menu;
        MenuItem lazyModeItem = mMenu.findItem(R.id.action_lazy_mode_filtered_thing_activity);
        if (isInLazyMode) {
            lazyModeItem.setTitle(R.string.action_stop_lazy_mode);
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
            collapsingToolbarLayout.setLayoutParams(params);
        } else {
            lazyModeItem.setTitle(R.string.action_start_lazy_mode);
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
            collapsingToolbarLayout.setLayoutParams(params);
        }

        if (userWhere != null && !PostDataSource.USER_WHERE_SUBMITTED.equals(userWhere)) {
            mMenu.findItem(R.id.action_sort_filtered_thing_activity).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_sort_filtered_thing_activity:
                switch (postType) {
                    case PostDataSource.TYPE_FRONT_PAGE:
                        bestSortTypeBottomSheetFragment.show(getSupportFragmentManager(), bestSortTypeBottomSheetFragment.getTag());
                        break;
                    case PostDataSource.TYPE_SEARCH:
                        searchPostSortTypeBottomSheetFragment.show(getSupportFragmentManager(), searchPostSortTypeBottomSheetFragment.getTag());
                        break;
                    case PostDataSource.TYPE_SUBREDDIT:
                        if (name.equals("popular") || name.equals("all")) {
                            popularAndAllSortTypeBottomSheetFragment.show(getSupportFragmentManager(), popularAndAllSortTypeBottomSheetFragment.getTag());
                        } else {
                            subredditSortTypeBottomSheetFragment.show(getSupportFragmentManager(), subredditSortTypeBottomSheetFragment.getTag());
                        }
                        break;
                    case PostDataSource.TYPE_MULTI_REDDIT:
                        multiRedditSortTypeBottomSheetFragment.show(getSupportFragmentManager(), multiRedditSortTypeBottomSheetFragment.getTag());
                        break;
                    case PostDataSource.TYPE_USER:
                        userThingSortTypeBottomSheetFragment.show(getSupportFragmentManager(), userThingSortTypeBottomSheetFragment.getTag());
                }
                return true;
            case R.id.action_refresh_filtered_thing_activity:
                if (mMenu != null) {
                    mMenu.findItem(R.id.action_lazy_mode_filtered_thing_activity).setTitle(R.string.action_start_lazy_mode);
                }
                if (mFragment instanceof FragmentCommunicator) {
                    ((FragmentCommunicator) mFragment).refresh();
                }
                return true;
            case R.id.action_lazy_mode_filtered_thing_activity:
                MenuItem lazyModeItem = mMenu.findItem(R.id.action_lazy_mode_filtered_thing_activity);
                if (isInLazyMode) {
                    ((FragmentCommunicator) mFragment).stopLazyMode();
                    isInLazyMode = false;
                    lazyModeItem.setTitle(R.string.action_start_lazy_mode);
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                    collapsingToolbarLayout.setLayoutParams(params);
                } else {
                    if (((FragmentCommunicator) mFragment).startLazyMode()) {
                        isInLazyMode = true;
                        lazyModeItem.setTitle(R.string.action_stop_lazy_mode);
                        params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
                        collapsingToolbarLayout.setLayoutParams(params);
                    }
                }
                return true;
            case R.id.action_change_post_layout_filtered_post_activity:
                postLayoutBottomSheetFragment.show(getSupportFragmentManager(), postLayoutBottomSheetFragment.getTag());
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CUSTOMIZE_POST_FILTER_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            if (mFragment != null) {
                mFragment.changePostFilter(data.getParcelableExtra(CustomizePostFilterActivity.RETURN_EXTRA_POST_FILTER));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, FRAGMENT_OUT_STATE, mFragment);
        outState.putBoolean(IS_IN_LAZY_MODE_STATE, isInLazyMode);
        outState.putString(ACCESS_TOKEN_STATE, mAccessToken);
        outState.putString(ACCOUNT_NAME_STATE, mAccountName);
        outState.putBoolean(NULL_ACCESS_TOKEN_STATE, mNullAccessToken);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void sortTypeSelected(SortType sortType) {
        mFragment.changeSortType(sortType);
    }

    @Override
    public void postLayoutSelected(int postLayout) {
        if (mFragment != null) {
            switch (postType) {
                case PostDataSource.TYPE_FRONT_PAGE:
                    mPostLayoutSharedPreferences.edit().putInt(SharedPreferencesUtils.POST_LAYOUT_FRONT_PAGE_POST, postLayout).apply();
                    break;
                case PostDataSource.TYPE_SUBREDDIT:
                    mPostLayoutSharedPreferences.edit().putInt(SharedPreferencesUtils.POST_LAYOUT_SUBREDDIT_POST_BASE + name, postLayout).apply();
                    break;
                case PostDataSource.TYPE_USER:
                    mPostLayoutSharedPreferences.edit().putInt(SharedPreferencesUtils.POST_LAYOUT_USER_POST_BASE + name, postLayout).apply();
                    break;
                case PostDataSource.TYPE_SEARCH:
                    mPostLayoutSharedPreferences.edit().putInt(SharedPreferencesUtils.POST_LAYOUT_SEARCH_POST, postLayout).apply();
            }
            ((FragmentCommunicator) mFragment).changePostLayout(postLayout);
        }
    }

    @Override
    public void sortTypeSelected(String sortType) {
        Bundle bundle = new Bundle();
        bundle.putString(SortTimeBottomSheetFragment.EXTRA_SORT_TYPE, sortType);
        sortTimeBottomSheetFragment.setArguments(bundle);
        sortTimeBottomSheetFragment.show(getSupportFragmentManager(), sortTimeBottomSheetFragment.getTag());
    }

    @Subscribe
    public void onAccountSwitchEvent(SwitchAccountEvent event) {
        finish();
    }

    @Override
    public void onLongPress() {
        if (mFragment != null) {
            mFragment.goBackToTop();
        }
    }

    @Override
    public void markPostAsRead(Post post) {
        InsertReadPost.insertReadPost(mRedditDataRoomDatabase, mAccountName, post.getId());
    }

    @Override
    public void fabOptionSelected(int option) {
        if (option == FilteredThingFABMoreOptionsBottomSheetFragment.FAB_OPTION_FILTER) {

        } else if (option == FilteredThingFABMoreOptionsBottomSheetFragment.FAB_OPTION_HIDE_READ_POSTS) {
            if (mFragment != null) {
                mFragment.hideReadPosts();
            }
        }
    }

    @Override
    public void contentScrollUp() {
        fab.show();
    }

    @Override
    public void contentScrollDown() {
        fab.hide();
    }
}
