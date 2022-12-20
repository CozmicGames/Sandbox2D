package engine.graphics.rendergraph.present

import com.cozmicgames.Kore
import com.cozmicgames.graphics
import com.cozmicgames.graphics.Primitive
import com.cozmicgames.graphics.gpu.getTexture2DUniform
import com.cozmicgames.graphics.gpu.pipeline.PipelineDefinition
import com.cozmicgames.graphics.gpu.update
import com.cozmicgames.utils.Color
import com.cozmicgames.utils.Disposable
import engine.graphics.rendergraph.RenderFunction
import engine.graphics.rendergraph.colorRenderTargetDependency

abstract class PresentFunction(effectSource: String, dependencyName: String, dependencyIndex: Int) : RenderFunction(), Disposable {
    private val colorInput = colorRenderTargetDependency(dependencyName, dependencyIndex)

    private val pipeline = PipelineDefinition(
        """
        #section uniforms
        sampler2D uTexture;
        
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
                
        vec4 getColor(vec2 position) {
            return texture(uTexture, position);
        }
    
        $effectSource
    
        void main() {
             outColor = effect(vTexcoord);
        }
        
    """.trimIndent()
    ).createPipeline()

    override fun render(delta: Float) {
        Kore.graphics.clear(Color.BLACK)
        Kore.graphics.setPipeline(pipeline)
        pipeline.getTexture2DUniform("uTexture")?.update(colorInput.texture)
        Kore.graphics.draw(Primitive.TRIANGLES, 3, 0)
    }

    override fun dispose() {
        pipeline.dispose()
    }
}