package yumyai.jogl.ui;

public interface ViewsCoordinator
{
    void setViewUpdated(int viewId);

    void resetUpdatedStatus();

    boolean checkAllViewsUpdated();
}
