package yumyai.jogl;

import java.util.ArrayList;
import javax.media.opengl.GL2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class VertexShader extends Shader {
    
    private ArrayList< Pair< String, Integer > > attributes = 
            new ArrayList<Pair<String, Integer>>();
    
    public VertexShader(GL2 glContext, String srcFile)
            throws GlslException {
        super(GL2.GL_VERTEX_SHADER, glContext, srcFile);
    }
    
    public int getAttributeCount()
    {
        return attributes.size();
    }
    
    public String getAttributeName(int index)
    {
        return attributes.get(index).getKey();
    }
    
    public int getAttributeLocation(int index)
    {
        return attributes.get(index).getValue();
    }
    
    public void addAttribute(String name, int location)
    {
        attributes.add(new ImmutablePair<String, Integer>(name, location));
    }
}
