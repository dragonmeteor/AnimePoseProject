#version 120

varying vec3 geom_worldPosition;
varying vec3 geom_worldNormal;
varying vec2 geom_texCoord;

uniform vec3  sys_lightPosition;
uniform vec3  sys_lightAmbient;
uniform vec3  sys_lightDiffuse;
uniform vec3  sys_lightSpecular;

uniform vec3 sys_materialAmbient;
uniform vec4 sys_materialDiffuse;
uniform vec3 sys_materialSpecular;
uniform float sys_materialShininess;

uniform vec3  sys_eyePosition;

uniform sampler2D sys_materialTexture;

void main()
{
	vec3 n = normalize(geom_worldNormal);
	vec3 l = normalize(sys_lightPosition - geom_worldPosition);	

	vec4 textureColor = texture2D(sys_materialTexture, geom_texCoord);
	if (textureColor.a < 0.0)
	{
		discard;
	}
	else
	{
		float dotProd = dot(n, l);
		if (dotProd > 0)	
		{	
			vec3 diffuse  = sys_lightDiffuse * sys_materialDiffuse.rgb * textureColor.rgb * dotProd;

			vec3 e = sys_eyePosition;
			vec3 v = normalize(e - geom_worldPosition);
			vec3 h = normalize(l + v);

			vec3 specular = pow(max(dot(n, h), 0.0), sys_materialShininess) * dotProd * sys_lightSpecular * sys_materialSpecular;

			gl_FragColor = vec4(diffuse + specular + sys_lightAmbient*sys_materialAmbient*textureColor.rgb, textureColor.a*sys_materialDiffuse.a);
		}
		else
		{
			gl_FragColor = vec4(sys_lightAmbient*sys_materialAmbient*textureColor.rgb, textureColor.a*sys_materialDiffuse.a);
		}	
	}	
}