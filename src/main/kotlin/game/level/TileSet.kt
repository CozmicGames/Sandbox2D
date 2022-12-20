package game.level

import com.cozmicgames.utils.Disposable
import com.cozmicgames.utils.Properties
import com.cozmicgames.utils.UUID
import com.cozmicgames.utils.extensions.enumValueOfOrNull
import com.cozmicgames.utils.extensions.pathWithoutExtension
import engine.Game
import engine.graphics.Material
import engine.assets.managers.getMaterial
import engine.assets.managers.materials
import engine.assets.remove
import game.components.GridComponent
import game.components.getCellType

class TileSet(val name: String) : Disposable {
    companion object {
        private fun createMaterial(tileSetName: String): String {
            val name = "${tileSetName.pathWithoutExtension}/${UUID.randomUUID()}.material"
            val material = Material()
            material.colorTexturePath = "internal/images/empty_tiletype.png"
            Game.assets.materials?.add(name, material)
            return name
        }
    }

    class TileType(val tileSet: TileSet) : Disposable {
        sealed class Dependency(val type: Type) {
            enum class Type {
                SOLID,
                EMPTY,
                TILE,
                TILE_EXCLUSIVE
            }
        }

        object SolidDependency : Dependency(Type.SOLID)

        object EmptyDependency : Dependency(Type.EMPTY)

        class TileTypeDependency(vararg types: String) : Dependency(Type.TILE) {
            var tileTypes = hashSetOf(*types)
        }

        class ExclusiveTileTypeDependency(vararg types: String) : Dependency(Type.TILE_EXCLUSIVE) {
            var tileTypes = hashSetOf(*types)
        }

        inner class Rule : Disposable {
            val material = createMaterial(tileSet.name)

            var dependencyTopLeft: Dependency? = null
            var dependencyTopCenter: Dependency? = null
            var dependencyTopRight: Dependency? = null
            var dependencyCenterLeft: Dependency? = null
            var dependencyCenterRight: Dependency? = null
            var dependencyBottomLeft: Dependency? = null
            var dependencyBottomCenter: Dependency? = null
            var dependencyBottomRight: Dependency? = null

            fun read(properties: Properties) {
                dependencyTopLeft = null
                dependencyTopCenter = null
                dependencyTopRight = null
                dependencyCenterLeft = null
                dependencyCenterRight = null
                dependencyBottomLeft = null
                dependencyBottomCenter = null
                dependencyBottomRight = null

                properties.getProperties("material")?.let {
                    val material = Game.assets.getMaterial(this.material)
                    material?.clear()
                    material?.set(it)
                }

                fun readDependencyProperties(properties: Properties): Dependency? {
                    val typeName = properties.getString("type") ?: return null
                    val type = enumValueOfOrNull<Dependency.Type>(typeName) ?: return null

                    return when (type) {
                        Dependency.Type.SOLID -> SolidDependency
                        Dependency.Type.EMPTY -> EmptyDependency
                        Dependency.Type.TILE -> {
                            TileTypeDependency().also { dependency ->
                                properties.getStringArray("tileTypes")?.let { dependency.tileTypes.addAll(it) }
                            }
                        }
                        Dependency.Type.TILE_EXCLUSIVE -> {
                            ExclusiveTileTypeDependency().also { dependency ->
                                properties.getStringArray("tileTypes")?.let { dependency.tileTypes.addAll(it) }
                            }
                        }
                    }
                }

                properties.getProperties("topLeft")?.let { dependencyTopLeft = readDependencyProperties(it) }
                properties.getProperties("topCenter")?.let { dependencyTopCenter = readDependencyProperties(it) }
                properties.getProperties("topRight")?.let { dependencyTopRight = readDependencyProperties(it) }
                properties.getProperties("centerLeft")?.let { dependencyCenterLeft = readDependencyProperties(it) }
                properties.getProperties("centerRight")?.let { dependencyCenterRight = readDependencyProperties(it) }
                properties.getProperties("bottomLeft")?.let { dependencyBottomLeft = readDependencyProperties(it) }
                properties.getProperties("bottomCenter")?.let { dependencyBottomCenter = readDependencyProperties(it) }
                properties.getProperties("bottomRight")?.let { dependencyBottomRight = readDependencyProperties(it) }
            }

            fun write(properties: Properties) {
                properties.setProperties("material", Game.assets.getMaterial(material) ?: Material().also {
                    it.colorTexturePath = "assets/internal/images/empty_tiletype.png"
                })

                fun writeDependencyProperties(dependency: Dependency?): Properties {
                    val dependencyProperties = Properties()
                    dependency?.let {
                        dependencyProperties.setString("type", it.type.name)
                        if (it is TileTypeDependency)
                            dependencyProperties.setStringArray("tileTypes", it.tileTypes.toTypedArray())
                        if (it is ExclusiveTileTypeDependency)
                            dependencyProperties.setStringArray("tileTypes", it.tileTypes.toTypedArray())
                    }
                    return dependencyProperties
                }

                properties.setProperties("topLeft", writeDependencyProperties(dependencyTopLeft))
                properties.setProperties("topCenter", writeDependencyProperties(dependencyTopCenter))
                properties.setProperties("topRight", writeDependencyProperties(dependencyTopRight))
                properties.setProperties("centerLeft", writeDependencyProperties(dependencyCenterLeft))
                properties.setProperties("centerRight", writeDependencyProperties(dependencyCenterRight))
                properties.setProperties("bottomLeft", writeDependencyProperties(dependencyBottomLeft))
                properties.setProperties("bottomCenter", writeDependencyProperties(dependencyBottomCenter))
                properties.setProperties("bottomRight", writeDependencyProperties(dependencyBottomRight))
            }

            override fun dispose() {
                Game.assets.remove(material)
            }
        }

        private val rulesInternal = arrayListOf<Rule>()

        val rules get() = ArrayList(rulesInternal).toList()

        var defaultMaterial = createMaterial(tileSet.name)
        var width = 1.0f
        var height = 1.0f

        fun getMaterial(gridComponent: GridComponent, cellX: Int, cellY: Int): String {
            if (rulesInternal.isEmpty())
                return defaultMaterial

            val topLeft = gridComponent.getCellType(cellX - 1, cellY + 1)
            val topCenter = gridComponent.getCellType(cellX, cellY + 1)
            val topRight = gridComponent.getCellType(cellX + 1, cellY + 1)
            val centerLeft = gridComponent.getCellType(cellX - 1, cellY)
            val centerRight = gridComponent.getCellType(cellX + 1, cellY)
            val bottomLeft = gridComponent.getCellType(cellX - 1, cellY - 1)
            val bottomCenter = gridComponent.getCellType(cellX, cellY - 1)
            val bottomRight = gridComponent.getCellType(cellX + 1, cellY - 1)

            return getMaterial(topLeft, topCenter, topRight, centerLeft, centerRight, bottomLeft, bottomCenter, bottomRight)
        }

        fun getMaterial(topLeft: String? = null, topCenter: String? = null, topRight: String? = null, centerLeft: String? = null, centerRight: String? = null, bottomLeft: String? = null, bottomCenter: String? = null, bottomRight: String? = null): String {
            if (rulesInternal.isEmpty())
                return defaultMaterial

            fun checkDependency(dependency: Dependency?, tileType: String?): Boolean {
                if (dependency == null)
                    return true

                return when (dependency.type) {
                    Dependency.Type.EMPTY -> tileType == null
                    Dependency.Type.SOLID -> tileType != null
                    Dependency.Type.TILE -> tileType in (dependency as TileTypeDependency).tileTypes
                    Dependency.Type.TILE_EXCLUSIVE -> tileType == null || tileType !in (dependency as ExclusiveTileTypeDependency).tileTypes
                }
            }

            var material = defaultMaterial

            for (rule in rulesInternal) {
                if (!checkDependency(rule.dependencyTopLeft, topLeft))
                    continue

                if (!checkDependency(rule.dependencyTopCenter, topCenter))
                    continue

                if (!checkDependency(rule.dependencyTopRight, topRight))
                    continue

                if (!checkDependency(rule.dependencyCenterLeft, centerLeft))
                    continue

                if (!checkDependency(rule.dependencyCenterRight, centerRight))
                    continue

                if (!checkDependency(rule.dependencyBottomLeft, bottomLeft))
                    continue

                if (!checkDependency(rule.dependencyBottomCenter, bottomCenter))
                    continue

                if (!checkDependency(rule.dependencyBottomRight, bottomRight))
                    continue

                material = rule.material
                break
            }

            return material
        }

        fun addRule(): Rule {
            val rule = Rule()
            rulesInternal += rule
            return rule
        }

        fun removeRule(rule: Rule) {
            if (rulesInternal.remove(rule))
                rule.dispose()
        }

        fun read(properties: Properties) {
            rulesInternal.clear()

            properties.getProperties("defaultMaterial")?.let {
                val material = Game.assets.getMaterial(defaultMaterial)
                material?.clear()
                material?.set(it)
            }

            properties.getPropertiesArray("rules")?.let {
                it.forEach {
                    val rule = addRule()
                    rule.read(it)
                }
            }
        }

        fun write(properties: Properties) {
            val rulesProperties = arrayListOf<Properties>()

            rulesInternal.forEach {
                val ruleProperties = Properties()
                it.write(ruleProperties)
                rulesProperties += ruleProperties
            }

            properties.setProperties("defaultMaterial", Game.assets.getMaterial(defaultMaterial) ?: (Material().also {
                it.colorTexturePath = "internal/images/empty_tiletype.png"
            }))

            properties.setPropertiesArray("rules", rulesProperties.toTypedArray())
        }

        override fun dispose() {
            Game.assets.remove(defaultMaterial)

            rulesInternal.forEach {
                it.dispose()
            }
        }
    }

    private val types = hashMapOf<String, TileType>()

    val tileTypeNames get() = types.keys.toList()

    operator fun get(id: String) = types[id]

    operator fun contains(id: String) = id in types

    fun addType(): String {
        val id = UUID.randomUUID().toString()
        this[id] = TileType(this)
        return id
    }

    operator fun set(name: String, type: TileType) {
        types[name] = type
    }

    fun remove(name: String): Boolean {
        val type = types.remove(name)
        return if (type != null) {
            type.dispose()
            true
        } else
            false
    }

    fun clear() {
        types.clear()
    }

    fun set(tileSet: TileSet) {
        tileSet.types.forEach { (name, type) ->
            this[name] = TileType(this).also { dest ->
                dest.defaultMaterial = type.defaultMaterial
                dest.width = type.width
                dest.height = type.height

                type.rules.forEach {
                    val rule = dest.addRule()

                    Game.assets.getMaterial(it.material)?.let {
                        Game.assets.getMaterial(rule.material)?.set(it)
                    }

                    fun copyDependency(src: TileType.Dependency?) = when (src?.type) {
                        TileType.Dependency.Type.EMPTY -> TileType.EmptyDependency
                        TileType.Dependency.Type.SOLID -> TileType.EmptyDependency
                        TileType.Dependency.Type.TILE -> TileType.TileTypeDependency().also {
                            it.tileTypes.addAll((src as TileType.TileTypeDependency).tileTypes)
                        }
                        TileType.Dependency.Type.TILE_EXCLUSIVE -> TileType.ExclusiveTileTypeDependency().also {
                            it.tileTypes.addAll((src as TileType.ExclusiveTileTypeDependency).tileTypes)
                        }
                        else -> null
                    }

                    rule.dependencyTopLeft = copyDependency(it.dependencyTopLeft)
                    rule.dependencyTopCenter = copyDependency(it.dependencyTopCenter)
                    rule.dependencyTopRight = copyDependency(it.dependencyTopRight)
                    rule.dependencyCenterLeft = copyDependency(it.dependencyCenterLeft)
                    rule.dependencyCenterRight = copyDependency(it.dependencyCenterRight)
                    rule.dependencyBottomLeft = copyDependency(it.dependencyBottomLeft)
                    rule.dependencyBottomCenter = copyDependency(it.dependencyBottomCenter)
                    rule.dependencyBottomRight = copyDependency(it.dependencyBottomRight)
                }
            }
        }
    }

    fun read(properties: Properties) {
        types.clear()

        properties.getPropertiesArray("types")?.let {
            for (typeProperties in it) {
                val name = typeProperties.getString("name") ?: continue
                val tileType = TileType(this)
                tileType.width = typeProperties.getFloat("width") ?: 1.0f
                tileType.height = typeProperties.getFloat("height") ?: 1.0f
                tileType.read(typeProperties)
                types[name] = tileType
            }
        }
    }

    fun write(properties: Properties) {
        val typesProperties = arrayListOf<Properties>()

        types.forEach { (name, tileType) ->
            val typeProperties = Properties()

            typeProperties.setString("name", name)
            typeProperties.setFloat("width", tileType.width)
            typeProperties.setFloat("height", tileType.height)
            tileType.write(typeProperties)

            typesProperties += typeProperties
        }

        properties.setPropertiesArray("types", typesProperties.toTypedArray())
    }

    override fun dispose() {
        types.forEach { (_, type) ->
            type.dispose()
        }
    }
}