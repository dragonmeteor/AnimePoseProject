package yumyai.mmd.vmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VmdMorphMotion
{
    public String morphName = "";
    public final List<VmdMorphKeyframe> keyFrames = new ArrayList<VmdMorphKeyframe>();

    public VmdMorphMotion()
    {
        // NOP
    }

    void sortAndRemoveDuplicates()
    {
        if (keyFrames.size() <= 1)
        {
            return;
        }

        Collections.sort(keyFrames, new Comparator<VmdMorphKeyframe>()
        {
            @Override
            public int compare(VmdMorphKeyframe o1, VmdMorphKeyframe o2)
            {
                return o1.frameNumber - o2.frameNumber;
            }
        });

        ArrayList<VmdMorphKeyframe> filteredList = new ArrayList<VmdMorphKeyframe>();
        filteredList.add(keyFrames.get(0));
        for (int i = 1; i < keyFrames.size(); i++)
        {
            if (keyFrames.get(i).frameNumber != filteredList.get(filteredList.size()-1).frameNumber)
            {
                filteredList.add(keyFrames.get(i));
            }
        }
        keyFrames.clear();
        keyFrames.addAll(filteredList);
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
                VmdMorphKeyframe f0 = keyFrames.get(mid - 1);
                VmdMorphKeyframe f1 = keyFrames.get(mid);
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

    public float evaluate(float t, int index)
    {
        if (index == keyFrames.size())
        {
            return keyFrames.get(keyFrames.size() - 1).weight;
        }
        else if (index == 0)
        {
            return keyFrames.get(0).weight;
        }
        else
        {
            VmdMorphKeyframe f0 = keyFrames.get(index - 1);
            VmdMorphKeyframe f1 = keyFrames.get(index);
            float alpha = (t - f0.frameNumber) / (f1.frameNumber - f0.frameNumber);
            return f0.weight * (1-alpha) + f1.weight * alpha;
        }
    }

    public float evaluate(float time)
    {
        int index = getUpperBoundIndex(time);
        return evaluate(time, index);
    }

    public int getLastFrame()
    {
        return keyFrames.get(keyFrames.size()-1).frameNumber;
    }
}
