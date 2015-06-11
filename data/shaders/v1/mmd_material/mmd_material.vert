#version 120

varying vec3 geom_worldPosition;
varying vec3 geom_worldNormal;
varying vec2 geom_texCoord;

void main()
{
	geom_worldPosition = (gl_ModelViewMatrix * gl_Vertex).xyz;
	geom_worldNormal =  normalize(gl_NormalMatrix * gl_Normal);
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	geom_texCoord = gl_MultiTexCoord0.xy;	
}