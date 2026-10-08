#version 330 core
in vec4 fColor;
in vec2 fTexCoords;
in float fTexId;

uniform sampler2D uTexture;

out vec4 color;

void main() {
    if (fTexId > 0.0) {
        color = fColor * texture(uTexture, fTexCoords);
    } else {
        color = fColor;
    }
}