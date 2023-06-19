package com.example.alleyrun

import android.graphics.Bitmap
import android.graphics.Canvas

open class Sprite(
    val image: Bitmap,
    var width: Int,
    var height: Int,
    var x: Float,
    var y: Float
) {
    fun draw(canvas: Canvas) {
        canvas.drawBitmap(image, x, y, null)
    }

    open fun setPositionInitial(canvasWidth: Int, canvasHeight: Int) {
        x = (Math.random() * (canvasWidth - width)).toFloat()
        y = (Math.random() * (canvasHeight - height)).toFloat()
    }

    fun isMouseOver(x: Float, y: Float): Boolean {
        return (x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height)
    }

    fun isTouchOver(x: Float, y: Float): Boolean {
        return (x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height)
    }

    fun speak() {
        // Implement audio playback logic here
    }
}

open class Runner(
    image: Bitmap,
    width: Int,
    height: Int
) : Sprite(image, width, height, 0f, 0f) {
    override fun setPositionInitial(canvasWidth: Int, canvasHeight: Int) {
        x = 0f
        y = (Math.random() * (canvasHeight - height)).toFloat()
    }
}

class Obstacle(
    image: Bitmap,
    width: Int,
    height: Int,
    val spriteType: String
) : Sprite(image, width, height, 0f, 0f) {
    var reward: Int = 10
    var speed: Int = -15
    var isAtEdge: Boolean = false

    override fun setPositionInitial(canvasWidth: Int, canvasHeight: Int) {
        x = (Math.random() * (canvasWidth - width)).toFloat()
        y = (Math.random() * (canvasHeight - height)).toFloat()
        isAtEdge = false
    }

    fun setPositionAdvanceOneStep() {
        if (x + speed > 0) {
            x += speed
            isAtEdge = false
        } else {
            isAtEdge = true
        }
    }

    fun setPositionReset(canvasWidth: Int, canvasHeight: Int) {
        x = (Math.random() * (canvasWidth - width)).toFloat()
        y = (Math.random() * (canvasHeight - height)).toFloat()
        isAtEdge = false
    }
}

open class ProRunner(
    image: Bitmap,
    width: Int,
    height: Int
) : Runner(image, width, height) {
    var speed: Int = 5
    var isAtEdge: Boolean = false

    override fun setPositionInitial(canvasWidth: Int, canvasHeight: Int) {
        x = 0.3f * (canvasWidth - width)
        y = 0.5f * (canvasHeight - height)
        isAtEdge = false
    }

    fun setPositionAdvanceOneStep(canvasWidth: Int, canvasHeight: Int) {
        if (x + speed < 0.7f * (canvasWidth - width)) {
            x += speed
            isAtEdge = false
        } else {
            isAtEdge = true
        }
    }

    fun penalize() {
        if (speed > 1) {
            speed -= 1
        }
    }
}

class ProDogs(
    image: Bitmap,
    width: Int,
    height: Int
) : ProRunner(image, width, height) {
    override fun setPositionInitial(canvasWidth: Int, canvasHeight: Int) {
        x = 0f
        y = 0.5f * (canvasHeight - height)
        isAtEdge = false
    }
}
