#version 330 core

in vec2 Texcoord;
in vec4 outColors;
out vec4 outColor;

void main()
{
    outColor = outColors;
}