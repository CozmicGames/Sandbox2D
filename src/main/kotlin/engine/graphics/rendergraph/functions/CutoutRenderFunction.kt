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

abstract class CutoutRenderFunction(dependencyName: String, dependencyIndex: Int) : RenderFunction(), Disposable {
    private val colorInput = colorRenderTargetDependency(dependencyName, dependencyIndex)

    private val pipeline = PipelineDefinition(
        """
        #section uniforms
        sampler2D uTexture;
        vec4 uBorderColor;
        vec4 uCutout;
        
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
                
        float rectangle(vec2 uv, float x, float y, float width, float height) {
	        float t = 0.0;
	        if ((uv.x > x - width * 0.5) && (uv.x < x + width * 0.5) && (uv.y > y - height * 0.5) && (uv.y < y + height * 0.5))
		        t = 1.0;
	        return t;
        }
        
        void main() {
            float inside = rectangle(uv, uCutout.x, uCutout.y, uCutout.z, uCutout.w);
            
            outColor = texture(uTexture, vTexcoord);
            
            if (inside != 1.0)
                outColor *= uBorderColor;
        }
        
    """.trimIndent()
    ).createPipeline()

    val borderColor = Color(0.5f, 0.5f, 0.5f, 1.0f)
    var cutOutX = 0.5f
    var cutOutY = 0.5f
    var cutOutWidth = 1.0f
    var cutOutHeight = 1.0f

    private val borderColorUniform = requireNotNull(pipeline.getFloatUniform("uBorderColor"))
    private val cutoutUniform = requireNotNull(pipeline.getFloatUniform("uCutout"))

    override fun render(delta: Float) {
        Kore.graphics.clear(Color.BLACK)
        Kore.graphics.setPipeline(pipeline)
        borderColorUniform.update(borderColor)
        cutoutUniform.update(cutOutX, cutOutY, cutOutWidth, cutOutHeight)
        pipeline.getTexture2DUniform("uTexture")?.update(colorInput.texture)
        Kore.graphics.draw(Primitive.TRIANGLES, 3, 0)
    }

    override fun dispose() {
        pipeline.dispose()
    }
}