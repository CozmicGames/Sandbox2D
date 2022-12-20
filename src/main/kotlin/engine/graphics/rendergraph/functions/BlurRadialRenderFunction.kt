package engine.graphics.rendergraph.functions

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.graphics.Primitive
import com.cozmicgames.graphics.gpu.getFloatUniform
import com.cozmicgames.graphics.gpu.getTexture2DUniform
import com.cozmicgames.graphics.gpu.pipeline.PipelineDefinition
import com.cozmicgames.graphics.gpu.update
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import engine.graphics.rendergraph.RenderFunction
import engine.graphics.rendergraph.colorRenderTargetDependency

class BlurRadialRenderFunction(dependencyName: String, dependencyIndex: Int) : RenderFunction(), Disposable {
    private val colorInput = colorRenderTargetDependency(dependencyName, dependencyIndex)

    private val pipeline = PipelineDefinition(
        """
        #section uniforms
        sampler2D uTexture;
        float uSampleDistance;
        float uSampleStrength;
        vec2 uOrigin;
        
        #section vertex
        out vec2 vTexcoord;

        void main() {
            float x = -1.0 + float((gl_VertexID & 1) << 2);
            float y = -1.0 + float((gl_VertexID & 2) << 1);
            vTexcoord.x = (x + 1.0) * 0.5;
            vTexcoord.y = (y + 1.0) * 0.5;
            gl_Position = vec4(x, y, 0, 1);
        }
        
        #section fragment
        in vec2 vTexcoord;
                            
        out vec4 outColor;
                
        const float cInvSqrtPi = 0.39894228040143267793994605993439;
        const float cInvPi = 0.31830988618379067153776752674503;

        void main() {
             const float cSamples[10] = float[](-0.08, -0.05, -0.03, -0.02, -0.01, 0.01, 0.02, 0.03, 0.05, 0.08);

             vec2 direction = uOrigin - vTexcoord;
             float distance = sqrt(direction.x * direction.x + direction.y * direction.y);
             
             direction = direction / distance;
             
             vec4 color = texture(uTexture, vTexcoord);
             
             vec4 sum = color;
             
             for (int i = 0; i < 10; i++) {
                 sum += texture(uTexture, vTexcoord + direction * cSamples[i] * uSampleDistance);
             }
             
             sum /= 11.0;
             
             float t = distance * uSampleStrength;
             t = clamp(t, 0.0, 1.0);
             
             outColor = mix(color, sum, t);
        }
        
    """.trimIndent()
    ).createPipeline()

    var sampleDistance = 1.0f
    var sampleStrength = 2.2f
    var originX = 0.5f
    var originY = 0.5f

    private val textureUniform = requireNotNull(pipeline.getTexture2DUniform("uTexture"))
    private val sampleDistanceUniform = requireNotNull(pipeline.getFloatUniform("uSampleDistance"))
    private val sampleStrengthUniform = requireNotNull(pipeline.getFloatUniform("uSampleStrength"))
    private val originUniform = requireNotNull(pipeline.getFloatUniform("uOrigin"))

    override fun render(delta: Float) {
        Kore.graphics.clear(Color.BLACK)
        Kore.graphics.setPipeline(pipeline)
        sampleDistanceUniform.update(sampleDistance)
        sampleStrengthUniform.update(sampleStrength)
        originUniform.update {
            it[0] = originX
            it[1] = originY
        }
        textureUniform.update(colorInput.texture)
        Kore.graphics.draw(Primitive.TRIANGLES, 3, 0)
    }

    override fun dispose() {
        pipeline.dispose()
    }
}