package yumyai.mmd.vmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public class VmdBoneMotion
{
    public String boneName = "";
    public final List<VmdBoneKeyframe> keyFrames = new ArrayList<VmdBoneKeyframe>();

    public VmdBoneMotion()
    {
        // NOP
    }

    void sortAndRemoveDuplicates()
    {
        if (keyFrames.size() <= 1)
        {
            return;
        }

        Collections.sort(keyFrames, new Comparator<VmdBoneKeyframe>()
        {
            @Override
            public int compare(VmdBoneKeyframe o1, VmdBoneKeyframe o2)
            {
                return o1.frameNumber - o2.frameNumber;
            }
        });

        ArrayList<VmdBoneKeyframe> filteredList = new ArrayList<VmdBoneKeyframe>();
        filteredList.add(keyFrames.get(0));
        for (int i = 1; i < keyFrames.size(); i++)
        {
            if (keyFrames.get(i).frameNumber != filteredList.get(filteredList.size() - 1).frameNumber)
            {
                filteredList.add(keyFrames.get(i));
            }
        }
        keyFrames.clear();
        keyFrames.addAll(filteredList);
    }

    public void evaluate(float t, int index, Vector3f displacement, Quat4f rotation)
    {
        if (index == keyFrames.size())
        {
            displacement.set(keyFrames.get(keyFrames.size() - 1).displacement);
            rotation.set(keyFrames.get(keyFrames.size() - 1).rotation);
        }
        else if (index == 0)
        {
            displacement.set(keyFrames.get(0).displacement);
            rotation.set(keyFrames.get(0).rotation);
        }
        else
        {
            VmdBoneKeyframe f0 = keyFrames.get(index - 1);
            VmdBoneKeyframe f1 = keyFrames.get(index);

            float alpha = (t - f0.frameNumber) / (f1.frameNumber - f0.frameNumber);

            displacement.set(f0.displacement);
            displacement.scale(1 - alpha);
            displacement.scaleAdd(alpha, f1.displacement, displacement);

            Quat4f r0 = f0.rotation;
            Quat4f r1 = f1.rotation;
            float dotProd = r0.x * r1.x + r0.y * r1.y + r0.z * r1.z + r0.w * r1.w;
            if (dotProd < 0)
            {
                rotation.set(r1);
                rotation.scale(-1);
            }
            else
            {
                rotation.set(r1);
            }
            rotation.interpolate(r0, rotation, alpha);
        }
    }

    public int getUpperBoundIndex(float t)
    {
        if (keyFrames.isEmpty())
        {
            return 0;
        }
        else if (t < keyFrames.get(0).frameNumber)
        {
            return 0;
        }
        else if (t >= keyFrames.get(keyFrames.size() - 1).frameNumber)
        {
            return keyFrames.size();
        }
        else
        {
            int left = 1;
            int right = keyFrames.size() - 1;
            while (left <= right)
            {
                int mid = (left + right) / 2;
                VmdBoneKeyframe f0 = keyFrames.get(mid - 1);
                VmdBoneKeyframe f1 = keyFrames.get(mid);
                if (f0.frameNumber <= t && t < f1.frameNumber)
                {
                    return mid;
                }
                else if (f0.frameNumber > t)
                {
                    right = mid - 1;
                }
                else
                {
                    left = mid + 1;
                }
            }
            return keyFrames.size();
        }
    }

    public void evaluate(float time, Vector3f displacement, Quat4f rotation)
    {
        int index = getUpperBoundIndex(time);
        evaluate(time, index, displacement, rotation);
    }

    public int getLastFrame()
    {
        if (keyFrames.size() == 0) {
            return 0;
        } else {
            return keyFrames.get(keyFrames.size()-1).frameNumber;
        }
    }
}
