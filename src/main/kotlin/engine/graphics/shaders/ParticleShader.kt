package engine.graphics.shaders

object ParticleShader : Shader(
    """
    #section state
    blend add one one
    
    #section common
    void vertexShader(inout Vertex v) {
    }
    
    vec4 fragmentShader(inout Fragment f) {
        return f.textureColor * f.color;
    }
""".trimIndent()
) {
    override fun toString(): String {
        return "Particle"
    }
}
