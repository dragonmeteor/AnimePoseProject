#version 120

uniform float sys_edgeSize;

void main()
{	
	vec3 p = (gl_ModelViewMatrix * gl_Vertex).xyz;
	vec3 n =  normalize(gl_NormalMatrix * gl_Normal);
	p += n* sys_edgeSize / 100;
	gl_Position = gl_ProjectionMatrix * vec4(p,1);	
}