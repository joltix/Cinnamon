#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in ivec2 texFlip;
layout(location = 3) in vec4 colors;
layout(location = 4) in mat4 projection;
layout(location = 8) in mat4 translation;
layout(location = 12) in vec2 scale;
layout(location = 13) in float rotation;

out vec4 outColors;
out vec2 Texcoord;

void main()
{
    // Compute half width and half height
    float halfW = scale.x / 2;
    float halfH = scale.y / 2;

    // Compute scaled coords and translate to origin (for rotation)
    float scaledX = (position.x * scale.x) - halfW;
    float scaledY = (position.y * scale.y) - halfH;

    // Compute rotation trig
    float rotSin = sin(rotation);
    float rotCos = cos(rotation);

    // Apply rotation and translate back from rotation origin
    float x = ((scaledX * rotCos) + (scaledY * -rotSin)) + halfW;
    float y = ((scaledX * rotSin) + (scaledY * rotCos)) + halfH;

    // Apply projection and translation matrix
    gl_Position = projection * translation * vec4(x, y, position.z, 1);
    outColors = colors;

    // Apply horizontal flip if desired
    if (texFlip.x == 1) {
        Texcoord.x = 1f - texCoord.x;
    } else {
        Texcoord.x = texCoord.x;
    }

    // Apply vertical flip if desired
    if (texFlip.y == 1) {
        Texcoord.y = 1f - texCoord.y;
    } else {
        Texcoord.y = texCoord.y;
    }
}