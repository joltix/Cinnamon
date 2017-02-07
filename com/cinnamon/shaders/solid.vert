#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texcoord;
layout(location = 2) in vec4 colors;
layout(location = 3) in mat4 projection;
layout(location = 7) in mat4 translation;

out vec4 outColors;
out vec2 Texcoord;

void main()
{
    gl_Position = projection * translation * vec4(position, 1);
    outColors = colors;
    Texcoord = texcoord;
}
