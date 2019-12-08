@file:Suppress("NOTHING_TO_INLINE")

import model.Game
import model.Point2D
import model.Unit


var emptySimCount: Long = 0
var fullSimTickCount: Long = 0

class Simulator(val game: Game, val mStrt : MyStrategy) {

    @JvmField
    var ticksCacled: Int = 0

    @JvmField
    var robotBallTouches = IntArray(8)

    var resultGoal: Boolean? = null

    @JvmField
    var microTicks: Int = 10

    var customRenderArray: MutableList<Any>? = null

    val metainfo: SimMetaInfo by lazy { SimMetaInfo() }

    val prop = game.properties


    fun move(e: Unit, delta_time: Float) {
       /* e.velocity = e.velocity.clamp(copyRules.MAX_ENTITY_SPEED)
        e.position += e.velocity * delta_time
        e.position.y -= copyRules.GRAVITY * delta_time * delta_time / 2
        e.velocity.y -= copyRules.GRAVITY * delta_time*/
    }


    fun tick() {
        ticksCacled++


        var delta_time = 1f / game.properties.ticksPerSecond
        for (i in 0 until microTicks) {
            update(delta_time / microTicks)
        }
    }

    private fun update(delta_time: Double) {                     //TODO optimzie
     /*   if (fullRobotSim) {
            val j = random.nextInt(4)
            val a = units[0]
            units[0] = units[j]
            units[j] = a
        }*/
/*

        game.units.forEach {  robot ->
            if (fullRobotSim) {
                if (robot.touch) {
                    var target_velocity = robot.action.target_velocity.clamp(copyRules.ROBOT_MAX_GROUND_SPEED)
                    target_velocity -= robot.touch_normal * robot.touch_normal.dotProduct(target_velocity)
                    val target_velocity_change = target_velocity - robot.velocity
                    if (target_velocity_change.length() > 0) {
                        val acceleration = copyRules.ROBOT_ACCELERATION * Math.max(0.0, robot.touch_normal.y)
                        robot.velocity += (target_velocity_change.normalize() * acceleration * delta_time).clamp(
                            target_velocity_change.length()
                        )
                    }
                }


                if (robot.action.use_nitro) {
                    val target_velocity_change =
                        (robot.action.target_velocity - robot.velocity).clamp(robot.nitro_amount * rules.NITRO_POINT_VELOCITY_CHANGE)
                    if (target_velocity_change.length() > 0) {
                        val acceleration = target_velocity_change.normalize() * rules.ROBOT_NITRO_ACCELERATION

                        val velocity_change = (acceleration * delta_time).clamp(target_velocity_change.length())
                        robot.velocity += velocity_change
                        robot.nitro_amount -= velocity_change.length() / rules.NITRO_POINT_VELOCITY_CHANGE
                    }
                }
            }

            move(robot, delta_time)
            robot.radius =
                copyRules.ROBOT_MIN_RADIUS + (copyRules.ROBOT_MAX_RADIUS - copyRules.ROBOT_MIN_RADIUS) * robot.action.jump_speed / copyRules.ROBOT_MAX_JUMP_SPEED
            robot.radius_change_speed = robot.action.jump_speed
        }

        move(ball, delta_time)

        for (i in 0 until units.size)
            for (j in 0 until i)
                collideEntities(units[i], units[j])

        units.fori { robot ->
            collideEntities(robot, ball)
            if (fullRobotSim) {
                var collision_normal = collide_with_arena(robot)
                if (collision_normal == null) {
                    robot.touch = false
                } else {
                    robot.touch = true
                    robot.touch_normal = collision_normal
                }
            }
        }
        collide_with_arena(ball)

        if (Math.abs(ball.position.z) > Arena.depth / 2 + ball.radius) {
            resultGoal = ball.position.z > 0
        }
*/

    }

/*
    fun collideEntities(a: Entity, b: Entity) {
        val deltaPosition = b.position - a.position
        val distance = deltaPosition.length()
        val penetration = a.radius + b.radius - distance
        if (penetration > 0) {
            val k_a = (1 / a.mass) / ((1 / a.mass) + (1 / b.mass))
            val k_b = (1 / b.mass) / ((1 / a.mass) + (1 / b.mass))
            val normal = deltaPosition.normalize()
            a.position -= normal * penetration * k_a
            b.position += normal * penetration * k_b
            val delta_velocity =
                (b.velocity - a.velocity).dotProduct(normal) - b.radius_change_speed - a.radius_change_speed
            if (delta_velocity < 0) {
                val impulse: Point3D = normal * (1 + random(copyRules.MIN_HIT_E, copyRules.MAX_HIT_E)) * delta_velocity
                a.velocity += impulse * k_a
                b.velocity -= impulse * k_b
            }

            if (b.robotId == null) {
                robotBallTouches[a.robotId!!]++
            }
        }
    }
*/

/*
    fun collide_with_arena(e: Entity): Point3D? {
        val dan = dan_to_arena(e.position, e.radius) ?: return null

        val (distance, normal) = dan

        val penetration = e.radius - distance
        if (penetration > 0) {
            e.position += normal * penetration
            val velocity = e.velocity.dotProduct(normal) - e.radius_change_speed
            if (velocity < 0) {
                e.velocity -= normal * (1 + e.arena_e) * velocity
                return normal
            }
        }
        return null
    }
*/

    @Suppress("NOTHING_TO_INLINE")
    inline fun clamp(x: Double, min: Double, max: Double): Double {
        if (x < min) return min
        return if (x > max) max else x
    }

    private inline fun min(dan: Dan?, dan2: Dan?): Dan? {
        if (dan2 == null) {
            return dan
        }

        if (dan == null) {
            return dan2
        }

        return if (dan.distance < dan2.distance) dan else dan2
    }
}

class Dan(@JvmField var distance: Double, @JvmField var normal: Point2D) {
    operator fun component1() = distance
    operator fun component2() = normal
}

/*inline class Dan(val a: Array<Any>) {
    constructor(dist: Double, normalize: Point3D) : this(arrayOf(dist, normalize))

    val distance: Double get() = a[0] as Double
    val normal: Point3D get() = a[1] as Point3D

    operator fun component1() = distance
    operator fun component2() = normal
}*/
