package yondoko.util;

import java.util.ArrayList;
import java.util.List;

public class CheckedStack<T>
{
    private final List<T> elements = new ArrayList<T>();
    private int current;
    
    public CheckedStack(int size, Factory<T> factory)
    {
        for (int i = 0; i < size; i++)
        {
            elements.add(factory.create());
        }
        current = elements.size()-1;
    }
    
    public T pop()
    {
        if (current < 0)
        {
            throw new RuntimeException("no more element to to spare");
        }
        else
        {
            T result = elements.get(current);
            current--;
            return result;
        }
    }
    
    public void push(T value)
    {
        if (current == elements.size()-1)
        {
            throw new RuntimeException("no element has been used");
        }
        else
        {
            if (value != elements.get(current+1))
            {
                throw new RuntimeException("pushed element not matched");
            }
            else
            {
                current++;                
            }
        }
    }           
}
