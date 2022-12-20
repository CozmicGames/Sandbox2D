package engine.physics

interface CollisionListener {
    fun onCollision(collisionPair: CollisionPair)
}