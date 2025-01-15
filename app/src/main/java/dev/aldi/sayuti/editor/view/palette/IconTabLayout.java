package dev.aldi.sayuti.editor.view.palette;

import android.content.Context;
import android.view.ViewGroup;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.palette.IconBase;
import mod.agus.jcoderz.beans.ViewBeans;
import pro.sketchware.R;

public class IconTabLayout extends IconBase {

  public IconTabLayout(Context context) {
    super(context);
    setWidgetImage(R.drawable.ic_mtrl_tabs);
    setWidgetName("TabLayout");
  }

  @Override
  public ViewBean getBean() {
    ViewBean viewBean = new ViewBean();
    viewBean.type = ViewBeans.VIEW_TYPE_LAYOUT_TABLAYOUT;
    viewBean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
    viewBean.layout.backgroundColor = 0xff008dcd;
    viewBean.convert = "com.google.android.material.tabs.TabLayout";
    viewBean.inject =
        "app:tabGravity=\"fill\"\n"
            + "app:tabMode=\"fixed\"\n"
            + "app:tabIndicatorHeight=\"3dp\"\n"
            + "app:tabIndicatorColor=\"@android:color/white\"\n"
            + "app:tabSelectedTextColor=\"@android:color/white\"\n"
            + "app:tabTextColor=\"@android:color/white\"\n"
            + "app:tabTextAppearance=\"@android:style/TextAppearance.Widget.TabWidget\"";
    return viewBean;
  }
}
