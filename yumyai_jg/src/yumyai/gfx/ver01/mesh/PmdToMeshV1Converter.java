package yumyai.gfx.ver01.mesh;

import org.apache.commons.io.FilenameUtils;
import yumyai.gfx.ver01.MaterialV1;
import yumyai.gfx.ver01.material.MmdMaterialV1;
import yumyai.mmd.pmd.PmdMaterial;
import yumyai.mmd.pmd.PmdModel;

import javax.vecmath.Vector4f;
import java.io.File;

public class PmdToMeshV1Converter {
    public static PmdMeshV1 createMesh(PmdModel model) {
        PmdMeshV1 mesh = new PmdMeshV1(model);

        for (int i = 0; i < model.materials.size(); i++) {
            MaterialV1 material = convertMaterial(model.materials.get(i), model);
            mesh.addMaterial(material, model.materials.get(i).vertexCount / 3);
        }

        return mesh;
    }

    /*
    protected static MaterialV1 convertMaterial(PmdMaterial material)
    {
        if (material.textureFileName.equals(""))
        {
            PhongMaterialV1 result = new PhongMaterialV1();
            result.setAmbient(material.ambient);
            result.setDiffuse(new Vector4f(material.diffuse.x, material.diffuse.y, material.diffuse.z, material.alpha));
            result.setSpecular(material.specular);
            result.setShininess(material.shininess);
            return result;
        }
        else
        {
            TexturedPhongMaterialV1 result = new TexturedPhongMaterialV1();
            result.setAmbient(material.ambient);
            result.setDiffuse(new Vector4f(material.diffuse.x, material.diffuse.y, material.diffuse.z, material.alpha));
            result.setSpecular(material.specular);
            result.setShininess(material.shininess);
            File textureFile = new File(material.textureFileName);
            if (textureFile.exists())
                result.setTextureFileName(material.textureFileName);
            else {
                String newFileName = textureFile.getParent() + File.separator + FilenameUtils.getBaseName(material.textureFileName) + ".png";
                result.setTextureFileName(newFileName);
            }
            return result;
        }
    }
    */

    static String getFileName(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            return file.getAbsolutePath();
        } else {
            //fileName = file.getParent() + File.separator + FilenameUtils.getBaseName(fileName) + ".png";
            fileName = fileName + ".png";
            file = new File(fileName);
            if (file.exists()) {
                return file.getAbsolutePath();
            } else {
                return "";
            }
        }
    }

    protected static MaterialV1 convertMaterial(PmdMaterial material, PmdModel model) {
        MmdMaterialV1 result = new MmdMaterialV1();
        result.setAmbient(material.ambient);
        result.setDiffuse(new Vector4f(material.diffuse.x, material.diffuse.y, material.diffuse.z, material.alpha));
        result.setSpecular(material.specular);
        result.setShininess(material.shininess);

        // Compute the texture file name.
        if (!material.textureFileName.equals("")) {
            result.setTextureFileName(getFileName(material.textureFileName));
        } else {
            result.setTextureFileName("");
        }
        result.setUseTexture(!result.getTextureFileName().equals(""));

        // Compute the toon texture file name.
        if (material.toonIndex >= 0) {
            result.setToonTextureFileName(getFileName(model.toonFileNames.get(material.toonIndex)));
        } else {
            result.setToonTextureFileName("");
        }
        result.setUseToon(!result.getToonTextureFileName().equals(""));

        // Compute the sphere map texture file name.
        if (!material.sphereMapFileName.equals("")) {
            String fileName = getFileName(material.sphereMapFileName);
            result.setSphereMapTextureFileName(fileName);
        } else {
            result.setSphereMapTextureFileName("");
        }
        result.setSphereMapMode(material.sphereMapMode);
        result.setUseSphereMap(!result.getSphereMapTextureFileName().equals(""));
        if (!result.isUseSphereMap()) {
            result.setSphereMapMode(PmdMaterial.NO_SPHERE_MAP);
        }

        result.setHasEdge(material.edgeFlag != 0);

        return result;
    }
}
