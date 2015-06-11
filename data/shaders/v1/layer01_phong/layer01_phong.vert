#version 120

varying vec3 geom_worldPosition;
varying vec3 geom_worldNormal;

void main()
{
	geom_worldPosition = (gl_ModelViewMatrix * gl_Vertex).xyz;
	geom_worldNormal =  normalize(gl_NormalMatrix * gl_Normal);
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}