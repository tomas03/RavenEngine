#version 330 core
in vec4 fColor;
in vec2 fTexCoords;
in float fTexId;

uniform sampler2D uTextures[8];

out vec4 color;

void main() {
    if (fTexId > 0.0) {
        int id = int(fTexId);
        vec4 texColor = vec4(1.0);

        switch (id) {
            case 1:
                texColor = texture(uTextures[0], fTexCoords);
                break;
            case 2:
                texColor = texture(uTextures[1], fTexCoords);
                break;
            case 3:
                texColor = texture(uTextures[2], fTexCoords);
                break;
            case 4:
                texColor = texture(uTextures[3], fTexCoords);
                break;
            case 5:
                texColor = texture(uTextures[4], fTexCoords);
                break;
            case 6:
                texColor = texture(uTextures[5], fTexCoords);
                break;
            case 7:
                texColor = texture(uTextures[6], fTexCoords);
                break;
            case 8:
                texColor = texture(uTextures[7], fTexCoords);
                break;
        }

        color = fColor * texColor;
    } else {
        color = fColor;
    }
}