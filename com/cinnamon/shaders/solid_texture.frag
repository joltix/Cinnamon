#version 330 core

in vec2 Texcoord;
in vec4 outColors;
out vec4 outColor;

uniform sampler2D tex;

void main()
{
    outColor = texture(tex, Texcoord) * outColors;
}
