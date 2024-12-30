package pro.sketchware.xml.resources.editors.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import pro.sketchware.xml.resources.editors.ResourcesEditorsActivity;
import pro.sketchware.xml.resources.editors.fragments.StylesEditor;

public class EditorsAdapter extends FragmentStateAdapter {

    private final ResourcesEditorsActivity activity;

    public EditorsAdapter(@NonNull ResourcesEditorsActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> activity.stringEditor;
            case 1 -> activity.colorsEditor;
            case 2 -> activity.stylesEditor;
            case 3 -> activity.themesEditor;
            default -> throw new IllegalArgumentException("Invalid position");
        };
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
