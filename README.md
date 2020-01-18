# codeside
Kotlin, Finals-26, Round2-16, Sandbox-24


# points of interest 📍

core of simulation: https://github.com/Karloid/codeside/blob/master/src/main/kotlin/sim/Simulator.kt#L52

set of strategies to put into simulation: https://github.com/Karloid/codeside/blob/master/src/main/kotlin/core/MyStrategy.kt#L322

simulation's `eval` function: https://github.com/Karloid/codeside/blob/master/src/main/kotlin/core/MyStrategy.kt#L431

deciding if shoot or not : https://github.com/Karloid/codeside/blob/master/src/main/kotlin/strats/SmartGuyStrategy.kt#L347

`SmartGuy` behavior: https://github.com/Karloid/codeside/blob/master/src/main/kotlin/strats/SmartGuyStrategy.kt#L37

struggling with decision between `SmartGuy` and `Simulation` :D : https://github.com/Karloid/codeside/blob/master/src/main/kotlin/core/MyStrategy.kt#L69  

strategy in most cases behaves as `SmartGuy` to save some time, but in case of danger or if it's stuck the simulation starts

self explosion with mine: https://github.com/Karloid/codeside/blob/master/src/main/kotlin/strats/SmartGuyStrategy.kt#L234

pathfinding: calc next near move point on the way to the destination: https://github.com/Karloid/codeside/blob/673327ee36b83565cbf9293860791082ad349a47/src/main/kotlin/core/Path.kt#L161

pathfinding: calc distance considering obstacles: https://github.com/Karloid/codeside/blob/673327ee36b83565cbf9293860791082ad349a47/src/main/kotlin/core/Path.kt#L29
