package engine.graphics.shaders

object SDFShader : Shader(
    """
    #section state
    blend add source_alpha one_minus_source_alpha
    
    #section uniforms
    float uSmoothing
    float uOutlineSize
    vec4 uOutlineColor
    
    #section common
    void vertexShader(inout Vertex v) {
    }
    
    vec4 fragmentShader(inout Fragment f) {
        float outerEdgeCenter = 0.5 - uOutlineSize;
        float distance = f.textureColor.a;
        float alpha = smoothstep(outerEdgeCenter - uSmoothing, outerEdgeCenter + uSmoothing, distance);
        float border = smoothstep(0.5 - uSmoothing, 0.5 + uSmoothing, distance);
        return vec4(mix(uOutlineColor.rgb, f.color.rgb, border), alpha);
    }
""".trimIndent()
)
