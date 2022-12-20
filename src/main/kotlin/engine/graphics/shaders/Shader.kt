package engine.graphics.shaders

import com.cozmicgames.files.FileHandle
import com.cozmicgames.files.readToString
import com.cozmicgames.graphics.gpu.VertexLayout
import com.cozmicgames.graphics.gpu.pipeline.PipelineDefinition
import com.cozmicgames.utils.extensions.removeComments
import engine.graphics.Material

open class Shader(source: String) {
    companion object {
        private fun loadSource(file: FileHandle): String {
            val text = file.readToString()
            return text.removeComments()
        }

        val VERTEX_LAYOUT = VertexLayout {
            vec2("position")
            vec2("texcoord")
            vec2("uBounds")
            vec2("vBounds")
            vec4("color", true, VertexLayout.AttributeType.BYTE)
        }
    }

    constructor(file: FileHandle) : this(loadSource(file))

    private val definition = PipelineDefinition()

    init {
        definition.parse(
            """
            $source
                
            #section layout
            vec2 position
            vec2 texcoord
            vec2 uBounds
            vec2 vBounds
            vec4 color normalized byte
            
            #section uniforms
            mat4 uCameraTransform;
            bool uFlipX;
            bool uFlipY;
            sampler2D uTexture;
            bool uUseSamplePointAntialiased;
            
            #section types
            struct Vertex {
                vec2 position;
                vec2 texcoord;
                vec4 color;
            }
            
            struct Fragment {
                vec4 textureColor;
                vec4 color;
                vec2 texcoord;
            }
            
            #section vertex
            #define uMin (uBounds.x)
            #define vMin (vBounds.x)
            #define uMax (uBounds.y)
            #define vMax (vBounds.y)
            #define uRange (uMax - uMin)
            #define vRange (vMax - vMin)

            out vec2 vTexcoord;
            out vec2 vNormalizedTexcoord;
            out vec4 vColor;

            void main() {
                vec2 tc;
                
                if (uFlipX) {
                    float normalizedU = (texcoord.x - uMin) / uRange;
                    float flippedNormalizedU = 1.0 - normalizedU;
                    float flippedU = uMin + flippedNormalizedU * uRange;
                    tc.x = flippedU;
                } else
                    tc.x = texcoord.x;
                
                if (uFlipY) {
                    float normalizedV = (texcoord.y - vMin) / vRange;
                    float flippedNormalizedV = 1.0 - normalizedV;
                    float flippedV = vMin + flippedNormalizedV * vRange;
                    tc.y = flippedV;
                } else
                    tc.y = texcoord.y;
                
                
                Vertex v;
                v.position = position;
                v.texcoord = tc;
                v.color = color;
                vertexShader(v);
                 
                gl_Position = uCameraTransform * vec4(v.position, 0.0, 1.0);
                vTexcoord = v.texcoord;
                vNormalizedTexcoord = (v.texcoord - vec2(uMin, vMin)) / vec2(uRange, vRange);
                vColor = v.color;
            }
            
            #section fragment
            in vec2 vTexcoord;
            in vec2 vNormalizedTexcoord;
            in vec4 vColor;
                                
            out vec4 outColor;
            
            void main() {
                Fragment f;
                f.textureColor = texture(uTexture, vTexcoord);
                f.color = vColor;
                f.texcoord = vNormalizedTexcoord;
                outColor = fragmentShader(f);
            }
            
        """.trimIndent()
        )
    }

    fun createPipeline() = definition.createPipeline()

    open fun setMaterial(material: Material) {}
}