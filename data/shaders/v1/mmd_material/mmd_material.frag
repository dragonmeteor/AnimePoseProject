#version 120

#define MULTIPLY_SPHERE_MAP 1
#define ADD_SPHERE_MAP 2

varying vec3 geom_worldPosition;
varying vec3 geom_worldNormal;
varying vec2 geom_texCoord;

uniform vec3 sys_lightDirection;
uniform vec3 sys_lightAmbient;
uniform vec3 sys_lightDiffuse;
uniform vec3 sys_lightSpecular;

uniform vec3 sys_materialAmbient;
uniform vec4 sys_materialDiffuse;
uniform vec3 sys_materialSpecular;
uniform float sys_materialShininess;

uniform vec3  sys_eyePosition;

uniform bool sys_useTexture;
uniform sampler2D sys_materialTexture;

uniform bool sys_useToon;
uniform sampler2D sys_toonTexture;

uniform bool sys_useSphereMap;
uniform sampler2D sys_sphereMapTexture;
uniform int sys_sphereMapMode;
uniform mat4 sys_viewMatrixInverse;

void main()
{	
	vec4 color = vec4(sys_materialAmbient*sys_lightAmbient,0) + sys_materialDiffuse * vec4(sys_lightDiffuse,1);
	if (sys_useTexture)
	{		
		vec4 textureColor = texture2D(sys_materialTexture, geom_texCoord);		
		color *= textureColor;
	}

	vec3 n = normalize(geom_worldNormal);	
	vec3 l = normalize(-sys_lightDirection);
	float dotProd = dot(n,l);	

	if (sys_useToon) 
	{		
		vec4 toonColor = texture2D(sys_toonTexture, vec2(0.5,0.5+0.5*dotProd));
		toonColor.w = 1.0;
		color *= toonColor;
	}

	if (sys_useSphereMap)
	{
		vec2 t = (sys_viewMatrixInverse * vec4(n, 0)).xy;		
		t.x = t.x*0.5 + 0.5;
		t.y = t.y*0.5 + 0.5;
		vec4 sphereMapColor = texture2D(sys_sphereMapTexture, t);
		if (sys_sphereMapMode == MULTIPLY_SPHERE_MAP)
		{
			color.rgb *= sphereMapColor.rgb;
		}
		else if (sys_sphereMapMode == ADD_SPHERE_MAP)
		{
			color.rgb += sphereMapColor.rgb;
		}
	}

	vec3 e = sys_eyePosition;
	vec3 v = normalize(e - geom_worldPosition);
	vec3 h = normalize(l + v);
	vec3 specular = pow(max(0.00001, dot(h,n)), sys_materialShininess) * sys_materialSpecular * sys_lightSpecular;
	color.rgb += specular;
	
	gl_FragColor = color;
}