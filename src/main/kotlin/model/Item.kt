package model

import util.StreamUtil

abstract class Item {

    abstract fun writeTo(stream: java.io.OutputStream)
    companion object {

        fun readFrom(stream: java.io.InputStream): Item {
            when (StreamUtil.readInt(stream)) {
                HealthPack.TAG -> return HealthPack.readFrom(stream)
                Weapon.TAG -> return Weapon.readFrom(stream)
                Mine.TAG -> return Mine.readFrom(stream)
                else -> throw java.io.IOException("Unexpected discriminant value")
            }
        }
    }

    class HealthPack : Item {
        var health: Int = 0
        constructor() {}
        constructor(health: Int) {
            this.health = health
        }
        companion object {
            val TAG = 0

            fun readFrom(stream: java.io.InputStream): HealthPack {
                val result = HealthPack()
                result.health = StreamUtil.readInt(stream)
                return result
            }
        }

        override fun writeTo(stream: java.io.OutputStream) {
            StreamUtil.writeInt(stream, TAG)
            StreamUtil.writeInt(stream, health)
        }
    }

    class Weapon : Item {
        lateinit var weaponType: model.WeaponType
        constructor() {}
        constructor(weaponType: model.WeaponType) {
            this.weaponType = weaponType
        }
        companion object {
            val TAG = 1

            fun readFrom(stream: java.io.InputStream): Weapon {
                val result = Weapon()
                when (StreamUtil.readInt(stream)) {
                0 ->result.weaponType = WeaponType.PISTOL
                1 ->result.weaponType = model.WeaponType.ASSAULT_RIFLE
                2 ->result.weaponType = model.WeaponType.ROCKET_LAUNCHER
                else -> throw java.io.IOException("Unexpected discriminant value")
                }
                return result
            }
        }
        override fun writeTo(stream: java.io.OutputStream) {
            StreamUtil.writeInt(stream, TAG)
            StreamUtil.writeInt(stream, weaponType.discriminant)
        }
    }

    class Mine : Item {
        constructor() {}
        companion object {
            val TAG = 2

            fun readFrom(stream: java.io.InputStream): Mine {
                val result = Mine()
                return result
            }
        }

        override fun writeTo(stream: java.io.OutputStream) {
            StreamUtil.writeInt(stream, TAG)
        }
    }
}
