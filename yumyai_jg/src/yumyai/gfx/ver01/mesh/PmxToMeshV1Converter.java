package yumyai.gfx.ver01.mesh;

import org.apache.commons.io.FilenameUtils;
import yumyai.gfx.ver01.MaterialV1;
import yumyai.gfx.ver01.material.MmdMaterialV1;
import yumyai.gfx.ver01.material.PhongMaterialV1;
import yumyai.gfx.ver01.material.TexturedPhongMaterialV1;
import yumyai.mmd.pmx.PmxMaterial;
import yumyai.mmd.pmx.PmxModel;

import java.io.File;

public class PmxToMeshV1Converter {
    public static PmxMeshV1 createMesh(PmxModel model) {
        PmxMeshV1 mesh = new PmxMeshV1(model);

        for (int i = 0; i < model.getMaterialCount(); i++) {
            PmxMaterial pmxMaterial = model.getMaterial(i);
            MaterialV1 material = convertMaterial(model, pmxMaterial);
            mesh.addMaterial(material, pmxMaterial.vertexCount / 3);
        }

        return mesh;
    }

    /*
    protected static MaterialV1 convertMaterial(PmxModel model, PmxMaterial material) {
        if (material.textureIndex < 0) {
            PhongMaterialV1 result = new PhongMaterialV1();
            result.setAmbient(material.ambient);
            result.setDiffuse(material.diffuse);
            result.setSpecular(material.specular);
            result.setShininess(material.shininess);
            return result;
        } else {
            String textureFileName = model.getAbsoluteTextureFileName(material.textureIndex);
            File file = new File(textureFileName);
            if (!file.exists()) {
                textureFileName = FilenameUtils.removeExtension(textureFileName) + ".png";
            }
            TexturedPhongMaterialV1 result = new TexturedPhongMaterialV1();
            result.setAmbient(material.ambient);
            result.setDiffuse(material.diffuse);
            result.setSpecular(material.specular);
            result.setShininess(material.shininess);
            result.setTextureFileName(textureFileName);
            return result;
        }
    }
    */

    protected static String getTextureOrPngFileName(String textureFileName) {
        File file = new File(textureFileName);
        if (!file.exists()) {
            file = new File(textureFileName + ".png");
            if (file.exists()) {
                return file.getAbsolutePath();
            } else {
                return "";
            }
        } else {
            return file.getAbsolutePath();
        }
    }

    protected static MaterialV1 convertMaterial(PmxModel model, PmxMaterial material) {
        MmdMaterialV1 result = new MmdMaterialV1();
        result.setAmbient(material.ambient);
        result.setDiffuse(material.diffuse);
        result.setSpecular(material.specular);
        result.setShininess(material.shininess);

        if (material.textureIndex >= 0) {
            result.setTextureFileName(getTextureOrPngFileName(model.getAbsoluteTextureFileName(material.textureIndex)));
        } else {
            result.setTextureFileName("");
        }
        result.setUseTexture(!result.getTextureFileName().equals(""));

        if (material.toonTextureIndex >= 0) {
            if (material.useSharedToonTexture) {
                result.setToonTextureFileName("data/textures/mmd_toons/toon" + String.format("%02d.bmp", material.toonTextureIndex+1));
            } else {
                result.setToonTextureFileName(getTextureOrPngFileName(model.getAbsoluteTextureFileName(material.toonTextureIndex)));
            }
        } else {
            result.setToonTextureFileName("");
        }
        result.setUseToon(!result.getToonTextureFileName().equals(""));

        if (material.sphereMode > 0 && material.sphereTextureIndex >= 0) {
            result.setSphereMapTextureFileName(getTextureOrPngFileName(model.getAbsoluteTextureFileName(material.sphereTextureIndex)));
        } else {
            result.setSphereMapTextureFileName("");
        }
        result.setSphereMapMode(material.sphereMode);
        result.setUseSphereMap(!result.getSphereMapTextureFileName().equals(""));
        if (!result.isUseSphereMap())
            result.setSphereMapMode(PmxMaterial.NO_SPHERE_MAP);

        return result;
    }

}
