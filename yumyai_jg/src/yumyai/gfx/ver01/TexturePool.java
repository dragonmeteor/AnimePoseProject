package yumyai.gfx.ver01;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javax.media.opengl.GL2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yumyai.jogl.Texture;
import yumyai.jogl.Texture2D;

public class TexturePool
{
    public Map<String, Texture> textures = new HashMap<String, Texture>();    
    public Map<String, Long> lastUsedTime = new HashMap<String, Long>();
    public boolean disposed = false;
    private int capacity = 128;
    /**
     * The logger.
     */
    private Logger logger;
        
    public TexturePool()
    {
        logger = LoggerFactory.getLogger(getClass());
    }

    public int getCapacity()
    {
        return capacity;
    }

    public void setCapacity(int capacity)
    {
        this.capacity = capacity;
    }        
    
    public Texture getTexture(GL2 gl, String fileName)
    {
        if (textures.containsKey(fileName))
        {
            lastUsedTime.put(fileName, System.nanoTime());
            return textures.get(fileName);
        }
        else
        {
            try
            {
                logger.debug("Created texture " + fileName);                
                Texture texture = new Texture2D(gl, fileName);
                texture.wrapR = GL2.GL_REPEAT;
                texture.wrapS = GL2.GL_REPEAT;
                texture.wrapT = GL2.GL_REPEAT;
                textures.put(fileName, texture);
                lastUsedTime.put(fileName, System.nanoTime());                
                return texture;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e.getMessage());
            }
        }
    }
    
    public void garbageCollect()
    {        
        if (textures.size() >= capacity)
        {
            String[] textureNames = new String[textures.size()];
            int count = 0;
            for (String textureName : textures.keySet())
            {
                textureNames[count] = textureName;
                count++;
            }
            Arrays.sort(textureNames, 0, count, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2)
                {
                    long t1 = lastUsedTime.get(o1);
                    long t2 = lastUsedTime.get(o2);
                    if (t1 < t2)
                    {
                        return -1;
                    }
                    else if (t1 == t2)
                    {
                        return 0;
                    }
                    else
                    {
                        return 1;
                    }
                }
            });
            for (int i = 0; i < capacity/2; i++)
            {
                String name = textureNames[i];
                Texture texture = textures.get(name);
                texture.unuse();
                texture.dispose();
                textures.remove(name);
                lastUsedTime.remove(name);
                logger.debug("Disposed texture " + name);                
            }
        }
    }
    
    public void disposeTexture(Texture texture)
    {
        if (textures.containsValue(texture))
        {
            String key = null;
            for (Map.Entry<String, Texture> entry : textures.entrySet())
            {
                if (entry.getValue() == texture)
                {
                    key = entry.getKey();
                }
            }
            textures.remove(key);
            texture.unuse();
            texture.dispose();
        }
    }
       
    public void dispose()
    {
        if (!disposed)
        {
            for (Texture texture : textures.values())
            {
                texture.unuse();
                texture.dispose();                
            }
            textures.clear();
            disposed = true;
        }
    }
    
    @Override
    protected void finalize() throws Throwable
    {
        dispose();
        super.finalize();
    }
}
