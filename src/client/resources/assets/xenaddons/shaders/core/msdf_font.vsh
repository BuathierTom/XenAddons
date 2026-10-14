#version 330
//? if >=26.3 {
//#extension GL_ARB_separate_shader_objects : require
//?}

//? if <26.3 {
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec4 CustomData;
in vec4 Color2;
in vec4 ShadowProps;

out vec4 vertexColor;
out vec2 texCoord;
out float pxRange;
//?} else {
//#include <minecraft:dynamictransforms.glsl>
//#include <minecraft:projection.glsl>
//
//layout(location = 0) in vec3 Position;
//layout(location = 1) in vec4 Color;
//layout(location = 2) in vec2 UV0;
//layout(location = 3) in vec4 CustomData;
//layout(location = 4) in vec4 Color2;
//layout(location = 5) in vec4 ShadowProps;
//
//layout(location = 0) out vec4 vertexColor;
//layout(location = 1) out vec2 texCoord;
//layout(location = 2) out float pxRange;
//?}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * ColorModulator;
    texCoord = UV0;
    pxRange = CustomData.x; // Per-glyph pixel range for MSDF
}
