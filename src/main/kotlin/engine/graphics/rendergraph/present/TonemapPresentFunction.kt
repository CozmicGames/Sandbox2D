package engine.graphics.rendergraph.present

class TonemapPresentFunction(type: Type = Type.REINHARD, dependencyName: String, dependencyIndex: Int) : PresentFunction(
    """
        vec4 effect(vec2 position) {
            ${
        when (type) {
            Type.LINEAR -> """
            float exposure = 1.0;
            float gamma = 2.2;
            vec4 color = getColor(position);
	        color.rgb = clamp(exposure * color.rgb, 0.0, 1.0);
	        color.rgb = pow(color.rgb, vec3(1.0 / gamma));
	        return color;    
        """
            Type.REINHARD -> """
            vec4 color = getColor(position);
            return vec4(color.rgb / (color.rgb + vec3(1.0)), color.a);
        """
            Type.ACES_FILM -> """
            const float a = 2.51;
            const float b = 0.03;
            const float c = 2.43;
            const float d = 0.59;
            const float e = 0.14;
            vec4 color = getColor(position);
            return vec4(clamp((color.rgb * (a * color.rgb + b)) / (color.rgb * (c * color.rgb + d ) + e), 0.0, 1.0), color.a);
        """
            Type.FILMIC -> """
            vec4 color = getColor(position);
            vec3 x = max(vec3(0.0), color.rgb - 0.004);
	        return vec4((x * (6.2 * x + 0.5)) / (x * (6.2 * x + 1.7) + 0.06), color.a);
        """
            Type.LUMA_BASED_REINHARD -> """
            float gamma = 2.2;
            vec4 color = getColor(position);
	        float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
	        float toneMappedLuma = luma / (1.0 + luma);
            color.rgb *= toneMappedLuma / luma;
	        color.rgb = pow(color.rgb, vec3(1.0 / gamma));
	        return color;
        """
            Type.WHITE_PRESERVING_LUMA_BASED_REINHARD -> """
            float gamma = 2.2;
            vec4 color = getColor(position);
            float white = 2.0;
	        float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
	        float toneMappedLuma = luma * (1.0 + luma / (white * white)) / (1.0 + luma);
	        color.rgb *= toneMappedLuma / luma;
	        color.rgb = pow(color.rgb, vec3(1.0 / gamma));
	        return color;
        """
            Type.ROM_BIN_DA_HOUSE -> """
            float gamma = 2.2;
            vec4 color = getColor(position);
            color.rgb = exp(-1.0 / (2.72 * color.rgb + 0.15));
	        color.rgb = pow(color.rgb, vec3(1.0 / gamma));
	        return color;
        """
        }
    }
    }
    """, dependencyName, dependencyIndex
) {
    enum class Type {
        LINEAR,
        REINHARD,
        ACES_FILM,
        FILMIC,
        LUMA_BASED_REINHARD,
        WHITE_PRESERVING_LUMA_BASED_REINHARD,
        ROM_BIN_DA_HOUSE
    }
}