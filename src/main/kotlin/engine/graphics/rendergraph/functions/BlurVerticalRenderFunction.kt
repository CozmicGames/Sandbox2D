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

class BlurVerticalRenderFunction(dependencyName: String, dependencyIndex: Int) : RenderFunction(), Disposable {
    private val colorInput = colorRenderTargetDependency(dependencyName, dependencyIndex)

    private val pipeline = PipelineDefinition(
        """
        #section uniforms
        sampler2D uTexture;
        float uSize;
        float uScale;
        
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
                
        void main() {
             float blurSize = uScale / uSize;
             vec4 sum = vec4(0.0);
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y - 4.0 * blurSize)) * 0.06;
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y - 3.0 * blurSize)) * 0.09;
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y - 2.0 * blurSize)) * 0.12;
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y - blurSize)) * 0.15;
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y)) * 0.16;
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y + blurSize)) * 0.15;
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y + 2.0 * blurSize)) * 0.12;
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y + 3.0 * blurSize)) * 0.09;
             sum += texture(uTexture, vec2(vTexcoord.x, vTexcoord.y + 4.0 * blurSize)) * 0.06;
             outColor = sum;
        }
        
    """.trimIndent()
    ).createPipeline()

    var scale = 1.0f

    private val textureUniform = requireNotNull(pipeline.getTexture2DUniform("uTexture"))
    private val sizeUniform = requireNotNull(pipeline.getFloatUniform("uSize"))
    private val scaleUniform = requireNotNull(pipeline.getFloatUniform("uScale"))

    override fun render(delta: Float) {
        Kore.graphics.clear(Color.BLACK)
        Kore.graphics.setPipeline(pipeline)
        sizeUniform.update(pass.height.toFloat())
        scaleUniform.update(scale)
        textureUniform.update(colorInput.texture)
        Kore.graphics.draw(Primitive.TRIANGLES, 3, 0)
    }

    override fun dispose() {
        pipeline.dispose()
    }
}
