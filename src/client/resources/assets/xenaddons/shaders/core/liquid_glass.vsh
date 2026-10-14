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

out vec2 FragCoord;
out vec2 TexCoord;
out vec4 FragColor;
out vec4 vCustomData;
out vec4 vColor2;
out vec4 vShadowProps;
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
//layout(location = 0) out vec2 FragCoord;
//layout(location = 1) out vec2 TexCoord;
//layout(location = 2) out vec4 FragColor;
//layout(location = 3) out vec4 vCustomData;
//layout(location = 4) out vec4 vColor2;
//layout(location = 5) out vec4 vShadowProps;
//?}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + ModelOffset, 1.0);
    FragCoord = UV0;
    TexCoord = gl_Position.xy * 0.5 + 0.5;
    FragColor = Color * ColorModulator;
    vCustomData = CustomData;
    vColor2 = Color2;
    vShadowProps = ShadowProps;
}
