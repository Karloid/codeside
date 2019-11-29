package model

class LootBox {
    lateinit var position: model.Point2D
    lateinit var size: model.Point2D
    lateinit var item: model.Item
    constructor() {}
    constructor(position: model.Point2D, size: model.Point2D, item: model.Item) {
        this.position = position
        this.size = size
        this.item = item
    }
    companion object {

        fun readFrom(stream: java.io.InputStream): LootBox {
            val result = LootBox()
            result.position = model.Point2D.readFrom(stream)
            result.size = model.Point2D.readFrom(stream)
            result.item = model.Item.readFrom(stream)
            return result
        }
    }

    fun writeTo(stream: java.io.OutputStream) {
        position.writeTo(stream)
        size.writeTo(stream)
        item.writeTo(stream)
    }
}
