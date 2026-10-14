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
in vec4 CustomData;  // [rectWidth, rectHeight, radius, unused]
in vec4 Color2;      // unused
in vec4 ShadowProps; // [uMin, vMin, uMax, vMax]

out vec4 vertexColor;
out vec2 texCoord;     // Lokalne współrzędne quada (0..1) dla SDF
out vec2 rectSize;     // Wymiary prostokąta w pikselach
out float radius;      // Promień zaokrąglenia
out vec4 uvBounds;     // [uMin, vMin, uMax, vMax] zakres UV dla croppowania
//?} else {
//#include <minecraft:dynamictransforms.glsl>
//#include <minecraft:projection.glsl>
//
//layout(location = 0) in vec3 Position;
//layout(location = 1) in vec4 Color;
//layout(location = 2) in vec2 UV0;
//layout(location = 3) in vec4 CustomData;  // [rectWidth, rectHeight, radius, unused]
//layout(location = 4) in vec4 Color2;      // unused
//layout(location = 5) in vec4 ShadowProps; // [uMin, vMin, uMax, vMax]
//
//layout(location = 0) out vec4 vertexColor;
//layout(location = 1) out vec2 texCoord;     // Lokalne współrzędne quada (0..1) dla SDF
//layout(location = 2) out vec2 rectSize;     // Wymiary prostokąta w pikselach
//layout(location = 3) out float radius;      // Promień zaokrąglenia
//layout(location = 4) out vec4 uvBounds;     // [uMin, vMin, uMax, vMax] zakres UV dla croppowania
//?}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * ColorModulator;
    texCoord = UV0;
    rectSize = CustomData.xy;
    radius = CustomData.z;
    uvBounds = ShadowProps;
}
