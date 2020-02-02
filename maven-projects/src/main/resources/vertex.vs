#version 330
layout (location=0) in vec3 position;
layout (location=1) in vec3 inColour;
out vec3 exColour;
uniform mat4 transform;
void main()
{
	gl_Position = transform * vec4( position,1.0 );
	exColour = inColour;
}