package com.yunxian.extoolbar;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.view.CollapsibleActionView;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuPresenter;
import android.support.v7.view.menu.MenuView;
import android.support.v7.view.menu.SubMenuBuilder;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.DecorToolbar;
import android.support.v7.widget.TintTypedArray;
import android.support.v7.widget.ToolbarWidgetWrapper;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.yunxian.extoolbar.widget.ActionMenuPresenter;
import com.yunxian.extoolbar.widget.ActionMenuView;

import java.util.ArrayList;
import java.util.List;

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;

/**
 * 扩展Support-V7包中的Toolbar。开放更多的接口便于更好的定制化
 *
 * @author A Shuai
 * @email ls1110924@gmail.com
 * @date 2016/12/3 17:39
 */

public class Toolbar extends ViewGroup {

    private static final String TAG = Toolbar.class.getSimpleName();

    private ActionMenuView mMenuView;
    private TextView mTitleTextView;
    private TextView mSubtitleTextView;
    private ImageButton mNavButtonView;
    private ImageView mLogoView;

    private Drawable mCollapseIcon;
    private CharSequence mCollapseDescription;
    ImageButton mCollapseButtonView;
    View mExpandedActionView;

    /**
     * Context against which to inflate popup menus.
     */
    private Context mPopupContext;

    /**
     * Theme resource against which to inflate popup menus.
     */
    private int mPopupTheme;

    private int mTitleTextAppearance;
    private int mSubtitleTextAppearance;

    int mButtonGravity;

    private int mMaxButtonHeight;

    private int mTitleMarginStart;
    private int mTitleMarginEnd;
    private int mTitleMarginTop;
    private int mTitleMarginBottom;

    private RtlSpacingHelper mContentInsets;
    private int mContentInsetStartWithNavigation;
    private int mContentInsetEndWithActions;

    private int mGravity = GravityCompat.START | Gravity.CENTER_VERTICAL;

    private CharSequence mTitleText;
    private CharSequence mSubtitleText;

    private int mTitleTextColor;
    private int mSubtitleTextColor;

    private boolean mEatingTouch;
    private boolean mEatingHover;

    // Clear me after use.
    private final ArrayList<View> mTempViews = new ArrayList<View>();

    // Used to hold views that will be removed while we have an expanded action view.
    private final ArrayList<View> mHiddenViews = new ArrayList<>();

    private final int[] mTempMargins = new int[2];

    Toolbar.OnMenuItemClickListener mOnMenuItemClickListener;

    private final ActionMenuView.OnMenuItemClickListener mMenuViewItemClickListener =
            new ActionMenuView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mOnMenuItemClickListener != null) {
                        return mOnMenuItemClickListener.onMenuItemClick(item);
                    }
                    return false;
                }
            };

    private ToolbarWidgetWrapper mWrapper;
    private ActionMenuPresenter mOuterActionMenuPresenter;
    private ExpandedActionViewMenuPresenter mExpandedMenuPresenter;
    private MenuPresenter.Callback mActionMenuPresenterCallback;
    private MenuBuilder.Callback mMenuBuilderCallback;

    private boolean mCollapsible;

    private final Runnable mShowOverflowMenuRunnable = new Runnable() {
        @Override
        public void run() {
//            showOverflowMenu();
        }
    };

    public Toolbar(Context context) {
        this(context, null);
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.extoolbar_toolbarStyle);
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Toolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.extoolbar_Toolbar, defStyleAttr, defStyleRes);

        mTitleTextAppearance = a.getResourceId(R.styleable.extoolbar_Toolbar_extoolbar_titleTextAppearance, 0);
        mSubtitleTextAppearance = a.getResourceId(R.styleable.extoolbar_Toolbar_extoolbar_subtitleTextAppearance, 0);
        mGravity = a.getInteger(R.styleable.extoolbar_Toolbar_android_gravity, mGravity);
        mButtonGravity = a.getInteger(R.styleable.extoolbar_Toolbar_extoolbar_buttonGravity, Gravity.TOP);

        // 第一次先读取正确的属性
        int titleMargin = a.getDimensionPixelOffset(R.styleable.extoolbar_Toolbar_extoolbar_titleMargin, 0);
        // 然后判断是否有兼容属性，如果设置了兼容属性，则尝试读取兼容属性
        if (a.hasValue(R.styleable.extoolbar_Toolbar_extoolbar_titleMargins)) {
            titleMargin = a.getDimensionPixelOffset(R.styleable.extoolbar_Toolbar_extoolbar_titleMargins, titleMargin);
        }
        mTitleMarginStart = mTitleMarginEnd = mTitleMarginTop = mTitleMarginBottom = titleMargin;

        // 依次读取单个设置的各个方向的留白
        final int marginStart = a.getDimensionPixelOffset(R.styleable.extoolbar_Toolbar_extoolbar_titleMarginStart, -1);
        if (marginStart >= 0) {
            mTitleMarginStart = marginStart;
        }
        final int marginEnd = a.getDimensionPixelOffset(R.styleable.extoolbar_Toolbar_extoolbar_titleMarginEnd, -1);
        if (marginEnd >= 0) {
            mTitleMarginEnd = marginEnd;
        }
        final int marginTop = a.getDimensionPixelOffset(R.styleable.extoolbar_Toolbar_extoolbar_titleMarginTop, -1);
        if (marginTop >= 0) {
            mTitleMarginTop = marginTop;
        }
        final int marginBottom = a.getDimensionPixelOffset(R.styleable.extoolbar_Toolbar_extoolbar_titleMarginBottom, -1);
        if (marginBottom >= 0) {
            mTitleMarginBottom = marginBottom;
        }

        mMaxButtonHeight = a.getDimensionPixelSize(R.styleable.extoolbar_Toolbar_extoolbar_maxButtonHeight, -1);

        final int contentInsetStart = a.getDimensionPixelOffset(R.styleable.extoolbar_Toolbar_extoolbar_contentInsetStart, RtlSpacingHelper.UNDEFINED);
        final int contentInsetEnd = a.getDimensionPixelOffset(R.styleable.extoolbar_Toolbar_extoolbar_contentInsetEnd, RtlSpacingHelper.UNDEFINED);
        final int contentInsetLeft = a.getDimensionPixelSize(R.styleable.extoolbar_Toolbar_extoolbar_contentInsetLeft, 0);
        final int contentInsetRight = a.getDimensionPixelSize(R.styleable.extoolbar_Toolbar_extoolbar_contentInsetRight, 0);

        ensureContentInsets();
        mContentInsets.setAbsolute(contentInsetLeft, contentInsetRight);
        if (contentInsetStart != RtlSpacingHelper.UNDEFINED || contentInsetEnd != RtlSpacingHelper.UNDEFINED) {
            mContentInsets.setRelative(contentInsetStart, contentInsetEnd);
        }

        mContentInsetStartWithNavigation = a.getDimensionPixelOffset(
                R.styleable.extoolbar_Toolbar_extoolbar_contentInsetStartWithNavigation, RtlSpacingHelper.UNDEFINED);
        mContentInsetEndWithActions = a.getDimensionPixelOffset(
                R.styleable.extoolbar_Toolbar_extoolbar_contentInsetEndWithActions, RtlSpacingHelper.UNDEFINED);

        mCollapseIcon = a.getDrawable(R.styleable.extoolbar_Toolbar_extoolbar_collapseIcon);
        mCollapseDescription = a.getText(R.styleable.extoolbar_Toolbar_extoolbar_collapseContentDescription);

        final CharSequence title = a.getText(R.styleable.extoolbar_Toolbar_extoolbar_title);
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        }

        final CharSequence subtitle = a.getText(R.styleable.extoolbar_Toolbar_extoolbar_subtitle);
        if (!TextUtils.isEmpty(subtitle)) {
            setSubtitle(subtitle);
        }

        // Set the default context, since setPopupTheme() may be a no-op.
        mPopupContext = getContext();
        setPopupTheme(a.getResourceId(R.styleable.extoolbar_Toolbar_extoolbar_popupTheme, 0));

        final Drawable navIcon = a.getDrawable(R.styleable.extoolbar_Toolbar_extoolbar_navigationIcon);
        if (navIcon != null) {
            setNavigationIcon(navIcon);
        }
        final CharSequence navDesc = a.getText(R.styleable.extoolbar_Toolbar_extoolbar_navigationContentDescription);
        if (!TextUtils.isEmpty(navDesc)) {
            setNavigationContentDescription(navDesc);
        }

        final Drawable logo = a.getDrawable(R.styleable.extoolbar_Toolbar_extoolbar_logo);
        if (logo != null) {
            setLogo(logo);
        }

        final CharSequence logoDesc = a.getText(R.styleable.extoolbar_Toolbar_extoolbar_logoDescription);
        if (!TextUtils.isEmpty(logoDesc)) {
            setLogoDescription(logoDesc);
        }

        if (a.hasValue(R.styleable.extoolbar_Toolbar_extoolbar_titleTextColor)) {
            setTitleTextColor(a.getColor(R.styleable.extoolbar_Toolbar_extoolbar_titleTextColor, Color.WHITE));
        }

        if (a.hasValue(R.styleable.extoolbar_Toolbar_extoolbar_subtitleTextColor)) {
            setSubtitleTextColor(a.getColor(R.styleable.extoolbar_Toolbar_extoolbar_subtitleTextColor, Color.WHITE));
        }

        a.recycle();
    }

    /**
     * Specifies the theme to use when inflating popup menus. By default, uses
     * the same theme as the toolbar itself.
     *
     * @param resId theme used to inflate popup menus
     * @see #getPopupTheme()
     */
    public void setPopupTheme(@StyleRes int resId) {
        if (mPopupTheme != resId) {
            mPopupTheme = resId;
            if (resId == 0) {
                mPopupContext = getContext();
            } else {
                mPopupContext = new ContextThemeWrapper(getContext(), resId);
            }
        }
    }

    /**
     * @return resource identifier of the theme used to inflate popup menus, or
     * 0 if menus are inflated against the toolbar theme
     * @see #setPopupTheme(int)
     */
    public int getPopupTheme() {
        return mPopupTheme;
    }

    /**
     * Sets the title margin.
     *
     * @param start  the starting title margin in pixels
     * @param top    the top title margin in pixels
     * @param end    the ending title margin in pixels
     * @param bottom the bottom title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMargin
     * @see #getTitleMarginStart()
     * @see #getTitleMarginTop()
     * @see #getTitleMarginEnd()
     * @see #getTitleMarginBottom()
     */
    public void setTitleMargin(int start, int top, int end, int bottom) {
        mTitleMarginStart = start;
        mTitleMarginTop = top;
        mTitleMarginEnd = end;
        mTitleMarginBottom = bottom;

        requestLayout();
    }

    /**
     * @return the starting title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMarginStart
     * @see #setTitleMarginStart(int)
     */
    public int getTitleMarginStart() {
        return mTitleMarginStart;
    }

    /**
     * Sets the starting title margin in pixels.
     *
     * @param margin the starting title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMarginStart
     * @see #getTitleMarginStart()
     */
    public void setTitleMarginStart(int margin) {
        mTitleMarginStart = margin;

        requestLayout();
    }

    /**
     * @return the top title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMarginTop
     * @see #setTitleMarginTop(int)
     */
    public int getTitleMarginTop() {
        return mTitleMarginTop;
    }

    /**
     * Sets the top title margin in pixels.
     *
     * @param margin the top title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMarginTop
     * @see #getTitleMarginTop()
     */
    public void setTitleMarginTop(int margin) {
        mTitleMarginTop = margin;

        requestLayout();
    }

    /**
     * @return the ending title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMarginEnd
     * @see #setTitleMarginEnd(int)
     */
    public int getTitleMarginEnd() {
        return mTitleMarginEnd;
    }

    /**
     * Sets the ending title margin in pixels.
     *
     * @param margin the ending title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMarginEnd
     * @see #getTitleMarginEnd()
     */
    public void setTitleMarginEnd(int margin) {
        mTitleMarginEnd = margin;

        requestLayout();
    }

    /**
     * @return the bottom title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMarginBottom
     * @see #setTitleMarginBottom(int)
     */
    public int getTitleMarginBottom() {
        return mTitleMarginBottom;
    }

    /**
     * Sets the bottom title margin in pixels.
     *
     * @param margin the bottom title margin in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_titleMarginBottom
     * @see #getTitleMarginBottom()
     */
    public void setTitleMarginBottom(int margin) {
        mTitleMarginBottom = margin;
        requestLayout();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (Build.VERSION.SDK_INT >= 17) {
            super.onRtlPropertiesChanged(layoutDirection);
        }

        ensureContentInsets();
        mContentInsets.setDirection(layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL);
    }

    /**
     * Set a logo drawable from a resource id.
     * <p>
     * <p>This drawable should generally take the place of title text. The logo cannot be
     * clicked. Apps using a logo should also supply a description using
     * {@link #setLogoDescription(int)}.</p>
     *
     * @param resId ID of a drawable resource
     */
    public void setLogo(@DrawableRes int resId) {
        setLogo(AppCompatResources.getDrawable(getContext(), resId));
    }

    /**
     * 是否可以展示溢出菜单
     *
     * @return true为可以展示，false为不会展示
     */
    public boolean canShowOverflowMenu() {
        return getVisibility() == VISIBLE && mMenuView != null && mMenuView.isOverflowReserved();
    }

    /**
     * Check whether the overflow menu is currently showing. This may not reflect
     * a pending show operation in progress.
     *
     * @return true if the overflow menu is currently showing
     */
    public boolean isOverflowMenuShowing() {
        return mMenuView != null && mMenuView.isOverflowMenuShowing();
    }

    public boolean isOverflowMenuShowPending() {
        return mMenuView != null && mMenuView.isOverflowMenuShowPending();
    }

    /**
     * Show the overflow items from the associated menu.
     *
     * @return true if the menu was able to be shown, false otherwise
     */
    public boolean showOverflowMenu() {
        return mMenuView != null && mMenuView.showOverflowMenu();
    }

    /**
     * Hide the overflow items from the associated menu.
     *
     * @return true if the menu was able to be hidden, false otherwise
     */
    public boolean hideOverflowMenu() {
        return mMenuView != null && mMenuView.hideOverflowMenu();
    }

    public void setMenu(@Nullable MenuBuilder menu, ActionMenuPresenter outerPresenter) {
        if (menu == null && mMenuView == null) {
            return;
        }

        ensureMenuView();
        final MenuBuilder oldMenu = mMenuView.peekMenu();
        if (oldMenu == menu) {
            return;
        }

        if (oldMenu != null) {
            oldMenu.removeMenuPresenter(mOuterActionMenuPresenter);
            oldMenu.removeMenuPresenter(mExpandedMenuPresenter);
        }

        if (mExpandedMenuPresenter == null) {
            mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
        }

        outerPresenter.setExpandedActionViewsExclusive(true);
        if (menu != null) {
            menu.addMenuPresenter(outerPresenter, mPopupContext);
            menu.addMenuPresenter(mExpandedMenuPresenter, mPopupContext);
        } else {
            outerPresenter.initForMenu(mPopupContext, null);
            mExpandedMenuPresenter.initForMenu(mPopupContext, null);
            outerPresenter.updateMenuView(true);
            mExpandedMenuPresenter.updateMenuView(true);
        }
        mMenuView.setPopupTheme(mPopupTheme);
        mMenuView.setPresenter(outerPresenter);
        mOuterActionMenuPresenter = outerPresenter;
    }

    /**
     * Dismiss all currently showing popup menus, including overflow or submenus.
     */
    public void dismissPopupMenus() {
        if (mMenuView != null) {
            mMenuView.dismissPopupMenus();
        }
    }


    public boolean isTitleTruncated() {
        if (mTitleTextView == null) {
            return false;
        }

        final Layout titleLayout = mTitleTextView.getLayout();
        if (titleLayout == null) {
            return false;
        }

        final int lineCount = titleLayout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            if (titleLayout.getEllipsisCount(i) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set a logo drawable.
     * <p>
     * <p>This drawable should generally take the place of title text. The logo cannot be
     * clicked. Apps using a logo should also supply a description using
     * {@link #setLogoDescription(int)}.</p>
     *
     * @param drawable Drawable to use as a logo
     */
    public void setLogo(Drawable drawable) {
        if (drawable != null) {
            ensureLogoView();
            if (!isChildOrHidden(mLogoView)) {
                addSystemView(mLogoView, true);
            }
        } else if (mLogoView != null && isChildOrHidden(mLogoView)) {
            removeView(mLogoView);
            mHiddenViews.remove(mLogoView);
        }
        if (mLogoView != null) {
            mLogoView.setImageDrawable(drawable);
        }
    }

    /**
     * Return the current logo drawable.
     *
     * @return The current logo drawable
     * @see #setLogo(int)
     * @see #setLogo(Drawable)
     */
    @Nullable
    public Drawable getLogo() {
        return mLogoView != null ? mLogoView.getDrawable() : null;
    }

    /**
     * Set a description of the toolbar's logo.
     * <p>
     * <p>This description will be used for accessibility or other similar descriptions
     * of the UI.</p>
     *
     * @param resId String resource id
     */
    public void setLogoDescription(@StringRes int resId) {
        setLogoDescription(getContext().getText(resId));
    }

    /**
     * Set a description of the toolbar's logo.
     * <p>
     * <p>This description will be used for accessibility or other similar descriptions
     * of the UI.</p>
     *
     * @param description Description to set
     */
    public void setLogoDescription(CharSequence description) {
        if (!TextUtils.isEmpty(description)) {
            ensureLogoView();
        }
        if (mLogoView != null) {
            mLogoView.setContentDescription(description);
        }
    }

    /**
     * Return the description of the toolbar's logo.
     *
     * @return A description of the logo
     */
    @Nullable
    public CharSequence getLogoDescription() {
        return mLogoView != null ? mLogoView.getContentDescription() : null;
    }

    private void ensureLogoView() {
        if (mLogoView == null) {
            mLogoView = new AppCompatImageView(getContext());
        }
    }

    /**
     * Check whether this Toolbar is currently hosting an expanded action view.
     * <p>
     * <p>An action view may be expanded either directly from the
     * {@link MenuItem MenuItem} it belongs to or by user action. If the Toolbar
     * has an expanded action view it can be collapsed using the {@link #collapseActionView()}
     * method.</p>
     *
     * @return true if the Toolbar has an expanded action view
     */
    public boolean hasExpandedActionView() {
        return mExpandedMenuPresenter != null && mExpandedMenuPresenter.mCurrentExpandedItem != null;
    }

    /**
     * Collapse a currently expanded action view. If this Toolbar does not have an
     * expanded action view this method has no effect.
     * <p>
     * <p>An action view may be expanded either directly from the
     * {@link MenuItem MenuItem} it belongs to or by user action.</p>
     *
     * @see #hasExpandedActionView()
     */
    public void collapseActionView() {
        final MenuItemImpl item = mExpandedMenuPresenter == null ? null :
                mExpandedMenuPresenter.mCurrentExpandedItem;
        if (item != null) {
            item.collapseActionView();
        }
    }

    /**
     * Returns the title of this toolbar.
     *
     * @return The current title.
     */
    public CharSequence getTitle() {
        return mTitleText;
    }

    /**
     * Set the title of this toolbar.
     * <p>
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.</p>
     *
     * @param resId Resource ID of a string to set as the title
     */
    public void setTitle(@StringRes int resId) {
        setTitle(getContext().getText(resId));
    }

    /**
     * Set the title of this toolbar.
     * <p>
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.</p>
     *
     * @param title Title to set
     */
    public void setTitle(CharSequence title) {
        if (!TextUtils.isEmpty(title)) {
            if (mTitleTextView == null) {
                final Context context = getContext();
                mTitleTextView = new AppCompatTextView(context);
                mTitleTextView.setSingleLine();
                mTitleTextView.setEllipsize(TextUtils.TruncateAt.END);
                if (mTitleTextAppearance != 0) {
                    mTitleTextView.setTextAppearance(context, mTitleTextAppearance);
                }
                if (mTitleTextColor != 0) {
                    mTitleTextView.setTextColor(mTitleTextColor);
                }
            }
            if (!isChildOrHidden(mTitleTextView)) {
                addSystemView(mTitleTextView, true);
            }
        } else if (mTitleTextView != null && isChildOrHidden(mTitleTextView)) {
            removeView(mTitleTextView);
            mHiddenViews.remove(mTitleTextView);
        }
        if (mTitleTextView != null) {
            mTitleTextView.setText(title);
        }
        mTitleText = title;
    }

    /**
     * Return the subtitle of this toolbar.
     *
     * @return The current subtitle
     */
    public CharSequence getSubtitle() {
        return mSubtitleText;
    }

    /**
     * Set the subtitle of this toolbar.
     * <p>
     * <p>Subtitles should express extended information about the current content.</p>
     *
     * @param resId String resource ID
     */
    public void setSubtitle(@StringRes int resId) {
        setSubtitle(getContext().getText(resId));
    }

    /**
     * Set the subtitle of this toolbar.
     * <p>
     * <p>Subtitles should express extended information about the current content.</p>
     *
     * @param subtitle Subtitle to set
     */
    public void setSubtitle(CharSequence subtitle) {
        if (!TextUtils.isEmpty(subtitle)) {
            if (mSubtitleTextView == null) {
                final Context context = getContext();
                mSubtitleTextView = new AppCompatTextView(context);
                mSubtitleTextView.setSingleLine();
                mSubtitleTextView.setEllipsize(TextUtils.TruncateAt.END);
                if (mSubtitleTextAppearance != 0) {
                    mSubtitleTextView.setTextAppearance(context, mSubtitleTextAppearance);
                }
                if (mSubtitleTextColor != 0) {
                    mSubtitleTextView.setTextColor(mSubtitleTextColor);
                }
            }
            if (!isChildOrHidden(mSubtitleTextView)) {
                addSystemView(mSubtitleTextView, true);
            }
        } else if (mSubtitleTextView != null && isChildOrHidden(mSubtitleTextView)) {
            removeView(mSubtitleTextView);
            mHiddenViews.remove(mSubtitleTextView);
        }
        if (mSubtitleTextView != null) {
            mSubtitleTextView.setText(subtitle);
        }
        mSubtitleText = subtitle;
    }

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     */
    public void setTitleTextAppearance(Context context, @StyleRes int resId) {
        mTitleTextAppearance = resId;
        if (mTitleTextView != null) {
            mTitleTextView.setTextAppearance(context, resId);
        }
    }

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     */
    public void setSubtitleTextAppearance(Context context, @StyleRes int resId) {
        mSubtitleTextAppearance = resId;
        if (mSubtitleTextView != null) {
            mSubtitleTextView.setTextAppearance(context, resId);
        }
    }

    /**
     * Sets the text color of the title, if present.
     *
     * @param color The new text color in 0xAARRGGBB format
     */
    public void setTitleTextColor(@ColorInt int color) {
        mTitleTextColor = color;
        if (mTitleTextView != null) {
            mTitleTextView.setTextColor(color);
        }
    }

    /**
     * Sets the text color of the subtitle, if present.
     *
     * @param color The new text color in 0xAARRGGBB format
     */
    public void setSubtitleTextColor(@ColorInt int color) {
        mSubtitleTextColor = color;
        if (mSubtitleTextView != null) {
            mSubtitleTextView.setTextColor(color);
        }
    }

    /**
     * Retrieve the currently configured content description for the navigation button view.
     * This will be used to describe the navigation action to users through mechanisms such
     * as screen readers or tooltips.
     *
     * @return The navigation button's content description
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_navigationContentDescription
     */
    @Nullable
    public CharSequence getNavigationContentDescription() {
        return mNavButtonView != null ? mNavButtonView.getContentDescription() : null;
    }

    /**
     * Set a content description for the navigation button if one is present. The content
     * description will be read via screen readers or other accessibility systems to explain
     * the action of the navigation button.
     *
     * @param resId Resource ID of a content description string to set, or 0 to
     *              clear the description
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_navigationContentDescription
     */
    public void setNavigationContentDescription(@StringRes int resId) {
        setNavigationContentDescription(resId != 0 ? getContext().getText(resId) : null);
    }

    /**
     * Set a content description for the navigation button if one is present. The content
     * description will be read via screen readers or other accessibility systems to explain
     * the action of the navigation button.
     *
     * @param description Content description to set, or <code>null</code> to
     *                    clear the content description
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_navigationContentDescription
     */
    public void setNavigationContentDescription(@Nullable CharSequence description) {
        if (!TextUtils.isEmpty(description)) {
            ensureNavButtonView();
        }
        if (mNavButtonView != null) {
            mNavButtonView.setContentDescription(description);
        }
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     * <p>
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.</p>
     * <p>
     * <p>If you use a navigation icon you should also set a description for its action using
     * {@link #setNavigationContentDescription(int)}. This is used for accessibility and
     * tooltips.</p>
     *
     * @param resId Resource ID of a drawable to set
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_navigationIcon
     */
    public void setNavigationIcon(@DrawableRes int resId) {
        setNavigationIcon(AppCompatResources.getDrawable(getContext(), resId));
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     * <p>
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.</p>
     * <p>
     * <p>If you use a navigation icon you should also set a description for its action using
     * {@link #setNavigationContentDescription(int)}. This is used for accessibility and
     * tooltips.</p>
     *
     * @param icon Drawable to set, may be null to clear the icon
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_navigationIcon
     */
    public void setNavigationIcon(@Nullable Drawable icon) {
        if (icon != null) {
            ensureNavButtonView();
            if (!isChildOrHidden(mNavButtonView)) {
                addSystemView(mNavButtonView, true);
            }
        } else if (mNavButtonView != null && isChildOrHidden(mNavButtonView)) {
            removeView(mNavButtonView);
            mHiddenViews.remove(mNavButtonView);
        }
        if (mNavButtonView != null) {
            mNavButtonView.setImageDrawable(icon);
        }
    }

    /**
     * Return the current drawable used as the navigation icon.
     *
     * @return The navigation icon drawable
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_navigationIcon
     */
    @Nullable
    public Drawable getNavigationIcon() {
        return mNavButtonView != null ? mNavButtonView.getDrawable() : null;
    }

    /**
     * Set a listener to respond to navigation events.
     * <p>
     * <p>This listener will be called whenever the user clicks the navigation button
     * at the start of the toolbar. An icon must be set for the navigation button to appear.</p>
     *
     * @param listener Listener to set
     * @see #setNavigationIcon(Drawable)
     */
    public void setNavigationOnClickListener(OnClickListener listener) {
        ensureNavButtonView();
        mNavButtonView.setOnClickListener(listener);
    }

    /**
     * Return the Menu shown in the toolbar.
     * <p>
     * <p>Applications that wish to populate the toolbar's menu can do so from here. To use
     * an XML menu resource, use {@link #inflateMenu(int)}.</p>
     *
     * @return The toolbar's Menu
     */
    public Menu getMenu() {
        ensureMenu();
        return mMenuView.getMenu();
    }

    /**
     * Set the icon to use for the overflow button.
     *
     * @param icon Drawable to set, may be null to clear the icon
     */
    public void setOverflowIcon(@Nullable Drawable icon) {
        ensureMenu();
        mMenuView.setOverflowIcon(icon);
    }

    /**
     * Return the current drawable used as the overflow icon.
     *
     * @return The overflow icon drawable
     */
    @Nullable
    public Drawable getOverflowIcon() {
        ensureMenu();
        return mMenuView.getOverflowIcon();
    }

    private void ensureMenu() {
        ensureMenuView();
        if (mMenuView.peekMenu() == null) {
            // Initialize a new menu for the first time.
            final MenuBuilder menu = (MenuBuilder) mMenuView.getMenu();
            if (mExpandedMenuPresenter == null) {
                mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
            }
            mMenuView.setExpandedActionViewsExclusive(true);
            menu.addMenuPresenter(mExpandedMenuPresenter, mPopupContext);
        }
    }

    private void ensureMenuView() {
        if (mMenuView == null) {
            mMenuView = new ActionMenuView(getContext());
            mMenuView.setPopupTheme(mPopupTheme);
            mMenuView.setOnMenuItemClickListener(mMenuViewItemClickListener);
            mMenuView.setMenuCallbacks(mActionMenuPresenterCallback, mMenuBuilderCallback);
            final LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.END | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
            mMenuView.setLayoutParams(lp);
            addSystemView(mMenuView, false);
        }
    }

    private MenuInflater getMenuInflater() {
        return new SupportMenuInflater(getContext());
    }

    /**
     * Inflate a menu resource into this toolbar.
     * <p>
     * <p>Inflate an XML menu resource into this toolbar. Existing items in the menu will not
     * be modified or removed.</p>
     *
     * @param resId ID of a menu resource to inflate
     */
    public void inflateMenu(@MenuRes int resId) {
        getMenuInflater().inflate(resId, getMenu());
    }

    /**
     * Set a listener to respond to menu item click events.
     * <p>
     * <p>This listener will be invoked whenever a user selects a menu item from
     * the action buttons presented at the end of the toolbar or the associated overflow.</p>
     *
     * @param listener Listener to set
     */
    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        mOnMenuItemClickListener = listener;
    }

    /**
     * Sets the content insets for this toolbar relative to layout direction.
     * <p>
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @param contentInsetStart Content inset for the toolbar starting edge
     * @param contentInsetEnd   Content inset for the toolbar ending edge
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetEnd
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetStart
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetEnd()
     * @see #getContentInsetLeft()
     * @see #getContentInsetRight()
     */
    public void setContentInsetsRelative(int contentInsetStart, int contentInsetEnd) {
        ensureContentInsets();
        mContentInsets.setRelative(contentInsetStart, contentInsetEnd);
    }

    /**
     * Gets the starting content inset for this toolbar.
     * <p>
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @return The starting content inset for this toolbar
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetStart
     * @see #setContentInsetsRelative(int, int)
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetEnd()
     * @see #getContentInsetLeft()
     * @see #getContentInsetRight()
     */
    public int getContentInsetStart() {
        return mContentInsets != null ? mContentInsets.getStart() : 0;
    }

    /**
     * Gets the ending content inset for this toolbar.
     * <p>
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @return The ending content inset for this toolbar
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetEnd
     * @see #setContentInsetsRelative(int, int)
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetLeft()
     * @see #getContentInsetRight()
     */
    public int getContentInsetEnd() {
        return mContentInsets != null ? mContentInsets.getEnd() : 0;
    }

    /**
     * Sets the content insets for this toolbar.
     * <p>
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @param contentInsetLeft  Content inset for the toolbar's left edge
     * @param contentInsetRight Content inset for the toolbar's right edge
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetLeft
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetRight
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetEnd()
     * @see #getContentInsetLeft()
     * @see #getContentInsetRight()
     */
    public void setContentInsetsAbsolute(int contentInsetLeft, int contentInsetRight) {
        ensureContentInsets();
        mContentInsets.setAbsolute(contentInsetLeft, contentInsetRight);
    }

    /**
     * Gets the left content inset for this toolbar.
     * <p>
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @return The left content inset for this toolbar
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetLeft
     * @see #setContentInsetsRelative(int, int)
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetEnd()
     * @see #getContentInsetRight()
     */
    public int getContentInsetLeft() {
        return mContentInsets != null ? mContentInsets.getLeft() : 0;
    }

    /**
     * Gets the right content inset for this toolbar.
     * <p>
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @return The right content inset for this toolbar
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetRight
     * @see #setContentInsetsRelative(int, int)
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetEnd()
     * @see #getContentInsetLeft()
     */
    public int getContentInsetRight() {
        return mContentInsets != null ? mContentInsets.getRight() : 0;
    }

    /**
     * Gets the start content inset to use when a navigation button is present.
     * <p>
     * <p>Different content insets are often called for when additional buttons are present
     * in the toolbar, as well as at different toolbar sizes. The larger value of
     * {@link #getContentInsetStart()} and this value will be used during layout.</p>
     *
     * @return the start content inset used when a navigation icon has been set in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetStartWithNavigation
     * @see #setContentInsetStartWithNavigation(int)
     */
    public int getContentInsetStartWithNavigation() {
        return mContentInsetStartWithNavigation != RtlSpacingHelper.UNDEFINED
                ? mContentInsetStartWithNavigation
                : getContentInsetStart();
    }

    /**
     * Sets the start content inset to use when a navigation button is present.
     * <p>
     * <p>Different content insets are often called for when additional buttons are present
     * in the toolbar, as well as at different toolbar sizes. The larger value of
     * {@link #getContentInsetStart()} and this value will be used during layout.</p>
     *
     * @param insetStartWithNavigation the inset to use when a navigation icon has been set
     *                                 in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetStartWithNavigation
     * @see #getContentInsetStartWithNavigation()
     */
    public void setContentInsetStartWithNavigation(int insetStartWithNavigation) {
        if (insetStartWithNavigation < 0) {
            insetStartWithNavigation = RtlSpacingHelper.UNDEFINED;
        }
        if (insetStartWithNavigation != mContentInsetStartWithNavigation) {
            mContentInsetStartWithNavigation = insetStartWithNavigation;
            if (getNavigationIcon() != null) {
                requestLayout();
            }
        }
    }

    /**
     * Gets the end content inset to use when action buttons are present.
     * <p>
     * <p>Different content insets are often called for when additional buttons are present
     * in the toolbar, as well as at different toolbar sizes. The larger value of
     * {@link #getContentInsetEnd()} and this value will be used during layout.</p>
     *
     * @return the end content inset used when a menu has been set in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetEndWithActions
     * @see #setContentInsetEndWithActions(int)
     */
    public int getContentInsetEndWithActions() {
        return mContentInsetEndWithActions != RtlSpacingHelper.UNDEFINED
                ? mContentInsetEndWithActions
                : getContentInsetEnd();
    }

    /**
     * Sets the start content inset to use when action buttons are present.
     * <p>
     * <p>Different content insets are often called for when additional buttons are present
     * in the toolbar, as well as at different toolbar sizes. The larger value of
     * {@link #getContentInsetEnd()} and this value will be used during layout.</p>
     *
     * @param insetEndWithActions the inset to use when a menu has been set in pixels
     * @attr ref android.support.v7.appcompat.R.styleable#Toolbar_contentInsetEndWithActions
     * @see #getContentInsetEndWithActions()
     */
    public void setContentInsetEndWithActions(int insetEndWithActions) {
        if (insetEndWithActions < 0) {
            insetEndWithActions = RtlSpacingHelper.UNDEFINED;
        }
        if (insetEndWithActions != mContentInsetEndWithActions) {
            mContentInsetEndWithActions = insetEndWithActions;
            if (getNavigationIcon() != null) {
                requestLayout();
            }
        }
    }

    /**
     * Gets the content inset that will be used on the starting side of the bar in the current
     * toolbar configuration.
     *
     * @return the current content inset start in pixels
     * @see #getContentInsetStartWithNavigation()
     */
    public int getCurrentContentInsetStart() {
        return getNavigationIcon() != null
                ? Math.max(getContentInsetStart(), Math.max(mContentInsetStartWithNavigation, 0))
                : getContentInsetStart();
    }

    /**
     * Gets the content inset that will be used on the ending side of the bar in the current
     * toolbar configuration.
     *
     * @return the current content inset end in pixels
     * @see #getContentInsetEndWithActions()
     */
    public int getCurrentContentInsetEnd() {
        boolean hasActions = false;
        if (mMenuView != null) {
            final MenuBuilder mb = mMenuView.peekMenu();
            hasActions = mb != null && mb.hasVisibleItems();
        }
        return hasActions
                ? Math.max(getContentInsetEnd(), Math.max(mContentInsetEndWithActions, 0))
                : getContentInsetEnd();
    }

    /**
     * Gets the content inset that will be used on the left side of the bar in the current
     * toolbar configuration.
     *
     * @return the current content inset left in pixels
     * @see #getContentInsetStartWithNavigation()
     * @see #getContentInsetEndWithActions()
     */
    public int getCurrentContentInsetLeft() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
                ? getCurrentContentInsetEnd()
                : getCurrentContentInsetStart();
    }

    /**
     * Gets the content inset that will be used on the right side of the bar in the current
     * toolbar configuration.
     *
     * @return the current content inset right in pixels
     * @see #getContentInsetStartWithNavigation()
     * @see #getContentInsetEndWithActions()
     */
    public int getCurrentContentInsetRight() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
                ? getCurrentContentInsetStart()
                : getCurrentContentInsetEnd();
    }

    private void ensureNavButtonView() {
        if (mNavButtonView == null) {
            mNavButtonView = new AppCompatImageButton(getContext(), null,
                    R.attr.toolbarNavigationButtonStyle);
            final LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.START | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
            mNavButtonView.setLayoutParams(lp);
        }
    }

    void ensureCollapseButtonView() {
        if (mCollapseButtonView == null) {
            mCollapseButtonView = new AppCompatImageButton(getContext(), null,
                    R.attr.toolbarNavigationButtonStyle);
            mCollapseButtonView.setImageDrawable(mCollapseIcon);
            mCollapseButtonView.setContentDescription(mCollapseDescription);
            final LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.START | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
            lp.mViewType = LayoutParams.EXPANDED;
            mCollapseButtonView.setLayoutParams(lp);
            mCollapseButtonView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    collapseActionView();
                }
            });
        }
    }

    private void addSystemView(View v, boolean allowHide) {
        final ViewGroup.LayoutParams vlp = v.getLayoutParams();
        final LayoutParams lp;
        if (vlp == null) {
            lp = generateDefaultLayoutParams();
        } else if (!checkLayoutParams(vlp)) {
            lp = generateLayoutParams(vlp);
        } else {
            lp = (LayoutParams) vlp;
        }
        lp.mViewType = LayoutParams.SYSTEM;

        if (allowHide && mExpandedActionView != null) {
            v.setLayoutParams(lp);
            mHiddenViews.add(v);
        } else {
            addView(v, lp);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());

        if (mExpandedMenuPresenter != null && mExpandedMenuPresenter.mCurrentExpandedItem != null) {
            state.expandedMenuItemId = mExpandedMenuPresenter.mCurrentExpandedItem.getItemId();
        }

        state.isOverflowOpen = isOverflowMenuShowing();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        final Menu menu = mMenuView != null ? mMenuView.peekMenu() : null;
        if (ss.expandedMenuItemId != 0 && mExpandedMenuPresenter != null && menu != null) {
            final MenuItem item = menu.findItem(ss.expandedMenuItemId);
            if (item != null) {
                MenuItemCompat.expandActionView(item);
            }
        }

        if (ss.isOverflowOpen) {
            postShowOverflowMenu();
        }
    }

    private void postShowOverflowMenu() {
        removeCallbacks(mShowOverflowMenuRunnable);
        post(mShowOverflowMenuRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mShowOverflowMenuRunnable);
    }

    //TODO
    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {

    }

    private int getViewListMeasuredWidth(List<View> views, int[] collapsingMargins) {
        int collapseLeft = collapsingMargins[0];
        int collapseRight = collapsingMargins[1];
        int width = 0;
        final int count = views.size();
        for (int i = 0; i < count; i++) {
            final View v = views.get(i);
            final Toolbar.LayoutParams lp = (Toolbar.LayoutParams) v.getLayoutParams();
            final int l = lp.leftMargin - collapseLeft;
            final int r = lp.rightMargin - collapseRight;
            final int leftMargin = Math.max(0, l);
            final int rightMargin = Math.max(0, r);
            collapseLeft = Math.max(0, -l);
            collapseRight = Math.max(0, -r);
            width += leftMargin + v.getMeasuredWidth() + rightMargin;
        }
        return width;
    }

    private int layoutChildLeft(View child, int left, int[] collapsingMargins,
                                int alignmentHeight) {
        final Toolbar.LayoutParams lp = (Toolbar.LayoutParams) child.getLayoutParams();
        final int l = lp.leftMargin - collapsingMargins[0];
        left += Math.max(0, l);
        collapsingMargins[0] = Math.max(0, -l);
        final int top = getChildTop(child, alignmentHeight);
        final int childWidth = child.getMeasuredWidth();
        child.layout(left, top, left + childWidth, top + child.getMeasuredHeight());
        left += childWidth + lp.rightMargin;
        return left;
    }

    private int layoutChildRight(View child, int right, int[] collapsingMargins,
                                 int alignmentHeight) {
        final Toolbar.LayoutParams lp = (Toolbar.LayoutParams) child.getLayoutParams();
        final int r = lp.rightMargin - collapsingMargins[1];
        right -= Math.max(0, r);
        collapsingMargins[1] = Math.max(0, -r);
        final int top = getChildTop(child, alignmentHeight);
        final int childWidth = child.getMeasuredWidth();
        child.layout(right - childWidth, top, right, top + child.getMeasuredHeight());
        right -= childWidth + lp.leftMargin;
        return right;
    }

    private int getChildTop(View child, int alignmentHeight) {
        final Toolbar.LayoutParams lp = (Toolbar.LayoutParams) child.getLayoutParams();
        final int childHeight = child.getMeasuredHeight();
        final int alignmentOffset = alignmentHeight > 0 ? (childHeight - alignmentHeight) / 2 : 0;
        switch (getChildVerticalGravity(lp.gravity)) {
            case Gravity.TOP:
                return getPaddingTop() - alignmentOffset;

            case Gravity.BOTTOM:
                return getHeight() - getPaddingBottom() - childHeight
                        - lp.bottomMargin - alignmentOffset;

            default:
            case Gravity.CENTER_VERTICAL:
                final int paddingTop = getPaddingTop();
                final int paddingBottom = getPaddingBottom();
                final int height = getHeight();
                final int space = height - paddingTop - paddingBottom;
                int spaceAbove = (space - childHeight) / 2;
                if (spaceAbove < lp.topMargin) {
                    spaceAbove = lp.topMargin;
                } else {
                    final int spaceBelow = height - paddingBottom - childHeight -
                            spaceAbove - paddingTop;
                    if (spaceBelow < lp.bottomMargin) {
                        spaceAbove = Math.max(0, spaceAbove - (lp.bottomMargin - spaceBelow));
                    }
                }
                return paddingTop + spaceAbove;
        }
    }

    private int getChildVerticalGravity(int gravity) {
        final int vgrav = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        switch (vgrav) {
            case Gravity.TOP:
            case Gravity.BOTTOM:
            case Gravity.CENTER_VERTICAL:
                return vgrav;
            default:
                return mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        }
    }

    /**
     * Prepare a list of non-SYSTEM child views. If the layout direction is RTL
     * this will be in reverse child order.
     *
     * @param views   List to populate. It will be cleared before use.
     * @param gravity Horizontal gravity to match against
     */
    private void addCustomViewsWithGravity(List<View> views, int gravity) {
        final boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
        final int childCount = getChildCount();
        final int absGrav = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));

        views.clear();

        if (isRtl) {
            for (int i = childCount - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                final Toolbar.LayoutParams lp = (Toolbar.LayoutParams) child.getLayoutParams();
                if (lp.mViewType == Toolbar.LayoutParams.CUSTOM && shouldLayout(child) && getChildHorizontalGravity(lp.gravity) == absGrav) {
                    views.add(child);
                }
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final Toolbar.LayoutParams lp = (Toolbar.LayoutParams) child.getLayoutParams();
                if (lp.mViewType == Toolbar.LayoutParams.CUSTOM && shouldLayout(child) && getChildHorizontalGravity(lp.gravity) == absGrav) {
                    views.add(child);
                }
            }
        }
    }

    private int getChildHorizontalGravity(int gravity) {
        final int ld = ViewCompat.getLayoutDirection(this);
        final int absGrav = GravityCompat.getAbsoluteGravity(gravity, ld);
        final int hGrav = absGrav & Gravity.HORIZONTAL_GRAVITY_MASK;
        switch (hGrav) {
            case Gravity.LEFT:
            case Gravity.RIGHT:
            case Gravity.CENTER_HORIZONTAL:
                return hGrav;
            default:
                return ld == ViewCompat.LAYOUT_DIRECTION_RTL ? Gravity.RIGHT : Gravity.LEFT;
        }
    }

    private boolean shouldLayout(View view) {
        return view != null && view.getParent() == this && view.getVisibility() != GONE;
    }

    private int getHorizontalMargins(View v) {
        final MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return MarginLayoutParamsCompat.getMarginStart(mlp) +
                MarginLayoutParamsCompat.getMarginEnd(mlp);
    }

    private int getVerticalMargins(View v) {
        final MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return mlp.topMargin + mlp.bottomMargin;
    }

    @Override
    public Toolbar.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new Toolbar.LayoutParams(getContext(), attrs);
    }

    @Override
    protected Toolbar.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof Toolbar.LayoutParams) {
            return new Toolbar.LayoutParams((Toolbar.LayoutParams) p);
        } else if (p instanceof ActionBar.LayoutParams) {
            return new Toolbar.LayoutParams((ActionBar.LayoutParams) p);
        } else if (p instanceof MarginLayoutParams) {
            return new Toolbar.LayoutParams((MarginLayoutParams) p);
        } else {
            return new Toolbar.LayoutParams(p);
        }
    }

    @Override
    protected Toolbar.LayoutParams generateDefaultLayoutParams() {
        return new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p) && p instanceof Toolbar.LayoutParams;
    }

    private static boolean isCustomView(View child) {
        return ((Toolbar.LayoutParams) child.getLayoutParams()).mViewType == Toolbar.LayoutParams.CUSTOM;
    }


    public DecorToolbar getWrapper() {
        if (mWrapper == null) {
            mWrapper = new ToolbarWidgetWrapper(this, true);
        }
        return mWrapper;
    }

    void removeChildrenForExpandedActionView() {
        final int childCount = getChildCount();
        // Go backwards since we're removing from the list
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            final Toolbar.LayoutParams lp = (Toolbar.LayoutParams) child.getLayoutParams();
            if (lp.mViewType != Toolbar.LayoutParams.EXPANDED && child != mMenuView) {
                removeViewAt(i);
                mHiddenViews.add(child);
            }
        }
    }

    void addChildrenForExpandedActionView() {
        final int count = mHiddenViews.size();
        // Re-add in reverse order since we removed in reverse order
        for (int i = count - 1; i >= 0; i--) {
            addView(mHiddenViews.get(i));
        }
        mHiddenViews.clear();
    }

    private boolean isChildOrHidden(View child) {
        return child.getParent() == this || mHiddenViews.contains(child);
    }

    /**
     * Force the toolbar to collapse to zero-height during measurement if
     * it could be considered "empty" (no visible elements with nonzero measured size)
     */
    public void setCollapsible(boolean collapsible) {
        mCollapsible = collapsible;
        requestLayout();
    }

    /**
     * Must be called before the menu is accessed
     */
    public void setMenuCallbacks(MenuPresenter.Callback pcb, MenuBuilder.Callback mcb) {
        mActionMenuPresenterCallback = pcb;
        mMenuBuilderCallback = mcb;
        if (mMenuView != null) {
            mMenuView.setMenuCallbacks(pcb, mcb);
        }
    }

    private void ensureContentInsets() {
        if (mContentInsets == null) {
            mContentInsets = new RtlSpacingHelper();
        }
    }

    /**
     * Interface responsible for receiving menu item click events if the items themselves
     * do not have individual item click listeners.
     */
    public interface OnMenuItemClickListener {
        /**
         * This method will be invoked when a menu item is clicked if the item itself did
         * not already handle the event.
         *
         * @param item {@link MenuItem} that was clicked
         * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
         */
        boolean onMenuItemClick(MenuItem item);
    }

    /**
     * Layout information for child views of Toolbars.
     * <p>
     * <p>Toolbar.LayoutParams extends ActionBar.LayoutParams for compatibility with existing
     * ActionBar API. See
     * {@link android.support.v7.app.AppCompatActivity#setSupportActionBar(android.support.v7.widget.Toolbar)
     * ActionBarActivity.setActionBar}
     * for more info on how to use a Toolbar as your Activity's ActionBar.</p>
     */
    public static class LayoutParams extends ActionBar.LayoutParams {
        static final int CUSTOM = 0;
        static final int SYSTEM = 1;
        static final int EXPANDED = 2;

        int mViewType = CUSTOM;

        public LayoutParams(@NonNull Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = Gravity.CENTER_VERTICAL | GravityCompat.START;
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(int gravity) {
            this(WRAP_CONTENT, MATCH_PARENT, gravity);
        }

        public LayoutParams(LayoutParams source) {
            super(source);

            mViewType = source.mViewType;
        }

        public LayoutParams(ActionBar.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            // ActionBar.LayoutParams doesn't have a MarginLayoutParams constructor.
            // Fake it here and copy over the relevant data.
            copyMarginsFromCompat(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        void copyMarginsFromCompat(MarginLayoutParams source) {
            this.leftMargin = source.leftMargin;
            this.topMargin = source.topMargin;
            this.rightMargin = source.rightMargin;
            this.bottomMargin = source.bottomMargin;
        }
    }

    public static class SavedState extends AbsSavedState {
        int expandedMenuItemId;
        boolean isOverflowOpen;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            expandedMenuItemId = source.readInt();
            isOverflowOpen = source.readInt() != 0;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(expandedMenuItemId);
            out.writeInt(isOverflowOpen ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                });
    }

    private class ExpandedActionViewMenuPresenter implements MenuPresenter {
        MenuBuilder mMenu;
        MenuItemImpl mCurrentExpandedItem;

        ExpandedActionViewMenuPresenter() {
        }

        @Override
        public void initForMenu(Context context, MenuBuilder menu) {
            // Clear the expanded action view when menus change.
            if (mMenu != null && mCurrentExpandedItem != null) {
                mMenu.collapseItemActionView(mCurrentExpandedItem);
            }
            mMenu = menu;
        }

        @Override
        public MenuView getMenuView(ViewGroup root) {
            return null;
        }

        @Override
        public void updateMenuView(boolean cleared) {
            // Make sure the expanded item we have is still there.
            if (mCurrentExpandedItem != null) {
                boolean found = false;

                if (mMenu != null) {
                    final int count = mMenu.size();
                    for (int i = 0; i < count; i++) {
                        final MenuItem item = mMenu.getItem(i);
                        if (item == mCurrentExpandedItem) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    // The item we had expanded disappeared. Collapse.
                    collapseItemActionView(mMenu, mCurrentExpandedItem);
                }
            }
        }

        @Override
        public void setCallback(Callback cb) {
        }

        @Override
        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        @Override
        public boolean flagActionItems() {
            return false;
        }

        @Override
        public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
            // TODO
//            ensureCollapseButtonView();
            if (mCollapseButtonView.getParent() != Toolbar.this) {
                addView(mCollapseButtonView);
            }
            mExpandedActionView = item.getActionView();
            mCurrentExpandedItem = item;
            if (mExpandedActionView.getParent() != Toolbar.this) {
                final LayoutParams lp = generateDefaultLayoutParams();
                lp.gravity = GravityCompat.START | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
                lp.mViewType = LayoutParams.EXPANDED;
                mExpandedActionView.setLayoutParams(lp);
                addView(mExpandedActionView);
            }

            // TODO
//            removeChildrenForExpandedActionView();
            requestLayout();
            item.setActionViewExpanded(true);

            if (mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) mExpandedActionView).onActionViewExpanded();
            }

            return true;
        }

        @Override
        public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
            // Do this before detaching the actionview from the hierarchy, in case
            // it needs to dismiss the soft keyboard, etc.
            if (mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) mExpandedActionView).onActionViewCollapsed();
            }

            removeView(mExpandedActionView);
            removeView(mCollapseButtonView);
            mExpandedActionView = null;

            // TODO
//            addChildrenForExpandedActionView();
            mCurrentExpandedItem = null;
            requestLayout();
            item.setActionViewExpanded(false);

            return true;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            return null;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {
        }
    }

}
