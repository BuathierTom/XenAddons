#version 330
//? if >=26.3 {
//#extension GL_ARB_separate_shader_objects : require
//?}

uniform sampler2D Sampler0;

//? if <26.3 {
in vec4 vertexColor;
in vec2 texCoord;
in float pxRange;

out vec4 fragColor;
//?} else {
//layout(location = 0) in vec4 vertexColor;
//layout(location = 1) in vec2 texCoord;
//layout(location = 2) in float pxRange;
//
//layout(location = 0) out vec4 fragColor;
//?}

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec3 msd = texture(Sampler0, texCoord).rgb;
    float sd = median(msd.r, msd.g, msd.b);

    // Compute screen-space pixel range using fragment derivatives (fwidth).
    // This automatically adapts to any transform/scale applied to the quad,
    // giving perfectly crisp edges at every size and resolution.
    // pxRange = raw distanceRange from atlas (e.g. 8.0)
    vec2 msdfUnit = pxRange / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(texCoord);
    float screenPxRange = max(0.5 * dot(msdfUnit, screenTexSize), 1.0);

    float screenPxDistance = screenPxRange * (sd - 0.5);
    float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);

    fragColor = vec4(vertexColor.rgb, vertexColor.a * opacity);
    if (fragColor.a < 0.001) discard;
}
