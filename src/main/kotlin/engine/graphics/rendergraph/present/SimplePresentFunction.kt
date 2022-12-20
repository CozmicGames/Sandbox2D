package engine.graphics.rendergraph.present

class SimplePresentFunction(dependencyName: String, dependencyIndex: Int) : PresentFunction(
    """
        vec4 effect(vec2 position) {
            return getColor(position);
        }
    """, dependencyName, dependencyIndex
)