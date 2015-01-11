uniform mat4 u_MVPMatrix;

attribute vec4 a_Position;
attribute vec4 a_Color;
attribute vec3 a_Normal;

varying vec3 v_Position;
varying vec4 v_Color;
varying vec3 v_Normal;

// The entry point for our vertex shader.
void main()
{
	v_Color = a_Color;
	gl_Position = u_MVPMatrix * a_Position;
}