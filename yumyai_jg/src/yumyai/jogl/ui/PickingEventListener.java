package yumyai.jogl.ui;

import java.util.EventListener;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public interface PickingEventListener extends EventListener
{
    public void objectPicked(Object source, int objectId, Vector3f pickLocation, Vector2f mousePosition);

    public void startPickingMode(Object source);

    public void stopPickingMode(Object source);
}
