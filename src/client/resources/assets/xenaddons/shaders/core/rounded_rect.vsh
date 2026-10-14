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
in vec4 CustomData; // [RectSize.x, RectSize.y, Radius, BorderThickness]
in vec4 Color2;
in vec4 ShadowProps; // [ShadowBlur, ShadowOffsetX, ShadowOffsetY, _]

out vec4 vertexColor;
out vec4 vertexColor2;
out vec2 texCoord;
out vec2 rectSize;
out float radius;
out float borderThickness;
out vec4 shadowProps;
//?} else {
//#include <minecraft:dynamictransforms.glsl>
//#include <minecraft:projection.glsl>
//
//layout(location = 0) in vec3 Position;
//layout(location = 1) in vec4 Color;
//layout(location = 2) in vec2 UV0;
//layout(location = 3) in vec4 CustomData; // [RectSize.x, RectSize.y, Radius, BorderThickness]
//layout(location = 4) in vec4 Color2;
//layout(location = 5) in vec4 ShadowProps; // [ShadowBlur, ShadowOffsetX, ShadowOffsetY, _]
//
//layout(location = 0) out vec4 vertexColor;
//layout(location = 1) out vec4 vertexColor2;
//layout(location = 2) out vec2 texCoord;
//layout(location = 3) out vec2 rectSize;
//layout(location = 4) out float radius;
//layout(location = 5) out float borderThickness;
//layout(location = 6) out vec4 shadowProps;
//?}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    vertexColor2 = Color2;
    texCoord = UV0;
    rectSize = CustomData.xy;
    radius = CustomData.z;
    borderThickness = CustomData.w;
    shadowProps = ShadowProps;
}
