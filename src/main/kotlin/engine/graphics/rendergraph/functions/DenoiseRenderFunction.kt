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

class DenoiseRenderFunction(dependencyName: String, dependencyIndex: Int) : RenderFunction(), Disposable {
    private val colorInput = colorRenderTargetDependency(dependencyName, dependencyIndex)

    private val pipeline = PipelineDefinition(
        """
        #section uniforms
        sampler2D uTexture;
        float uSigma;
        float uThreshold;
        float uSigmaCoefficient;
        
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
             float radius = round(uSigma * uSigmaCoefficient);
             float radiusSquared = radius * radius;
             
             float invSigmaQx2 = 0.5 / (uSigma * uSigma);
             float invSigmaQx2PI = cInvPi * invSigmaQx2;
            
             float invThresholdSqx2 = 0.5 / (uThreshold * uThreshold);
             float invThresholdSqrt2PI = cInvSqrtPi / uThreshold;
            
             vec4 centrPx = texture(uTexture, vTexcoord); 
            
             float zBuff = 0.0;
             vec4 aBuff = vec4(0.0);
             vec2 size = vec2(textureSize(uTexture, 0));

             vec2 d;
             for (d.x = -radius; d.x <= radius; d.x++) {
                 float pt = sqrt(radiusSquared - d.x * d.x);
                 for (d.y = -pt; d.y <= pt; d.y++) {
                     float blurFactor = exp(-dot(d, d) * invSigmaQx2) * invSigmaQx2PI;
            
                     vec4 walkPx =  texture(uTexture, vTexcoord + d / size);
                     vec4 dC = walkPx - centrPx;
                     float deltaFactor = exp(-dot(dC, dC) * invThresholdSqx2) * invThresholdSqrt2PI * blurFactor;
            
                     zBuff += deltaFactor;
                     aBuff += deltaFactor * walkPx;
                 }
             }
             
             outColor = aBuff / zBuff;
        }
        
    """.trimIndent()
    ).createPipeline()

    var sigma = 5.0f
    var threshold = 0.1f
    var sigmaCoefficient = 2.0f

    private val textureUniform = requireNotNull(pipeline.getTexture2DUniform("uTexture"))
    private val sigmaUniform = requireNotNull(pipeline.getFloatUniform("uSigma"))
    private val thresholdUniform = requireNotNull(pipeline.getFloatUniform("uThreshold"))
    private val sigmaCoefficientUniform = requireNotNull(pipeline.getFloatUniform("uSigmaCoefficient"))

    override fun render(delta: Float) {
        Kore.graphics.clear(Color.BLACK)
        Kore.graphics.setPipeline(pipeline)
        sigmaUniform.update(sigma)
        thresholdUniform.update(threshold)
        sigmaCoefficientUniform.update(sigmaCoefficient)
        textureUniform.update(colorInput.texture)
        Kore.graphics.draw(Primitive.TRIANGLES, 3, 0)
    }

    override fun dispose() {
        pipeline.dispose()
    }
}