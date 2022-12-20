package engine.graphics.shaders

object DefaultShader : Shader(
    """
    #section state
    blend add source_alpha one_minus_source_alpha
    
    #section common
    void vertexShader(inout Vertex v) {
    }
    
    vec4 fragmentShader(inout Fragment f) {
        return f.textureColor * f.color;
    }
""".trimIndent()
)
