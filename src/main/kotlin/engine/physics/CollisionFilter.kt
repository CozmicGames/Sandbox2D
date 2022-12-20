package engine.physics

interface CollisionFilter {
    fun shouldCollide(a: Fixture<*>, b: Fixture<*>): Boolean
}