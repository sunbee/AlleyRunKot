package com.example.alleyrun

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.SurfaceView
import android.view.SurfaceHolder
import android.widget.TextView
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceControlViewHost
import android.view.View
import android.widget.Button
import org.w3c.dom.Text

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var spriteRunner: Runner
    private val spritesGarbageBag = mutableListOf<Obstacle>()
    private val spritesGoldCoin = mutableListOf<Obstacle>()
    private val spritesBone = mutableListOf<Obstacle>()
    private lateinit var spriteProRunner: ProRunner
    private lateinit var spriteProDogs: ProDogs
    private var score = 0
    private var message = "Press START!"
    private lateinit var mySensorManager: SensorManager
    private lateinit var myAccelerometer: Sensor
    private lateinit var gameCanvas: SurfaceView
    private lateinit var progressBarCanvas: SurfaceView
    private var isGameRunning = false
    private var gameLoopThread: Thread? = null
    private var gameCanvasWidth: Int = 500
    private var gameCanvasHeight: Int = 500
    private lateinit var xyz_display: TextView
    private lateinit var msg_display: TextView
    private lateinit var score_display: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get canvas elements from View
        gameCanvas = findViewById(R.id.gameCanvasView)
        progressBarCanvas = findViewById(R.id.progressBarView)

        xyz_display = findViewById<TextView>(R.id.xyzTextView)
        msg_display = findViewById<TextView>(R.id.messageTextView)
        score_display = findViewById<TextView>(R.id.scoreTextView)

        // Initialize the sensor manager
        mySensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        // Get the accelerometer sensor
        myAccelerometer = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setUpGame()

        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.quitButton)

        startButton.setOnClickListener { it: View? ->
            startGameLoop()
        }

        stopButton.setOnClickListener { it: View? ->
            msg_display.text = "Got STOP!"
            stopGameLoop()
        }
    }

    override fun onResume() {
        super.onResume()

        mySensorManager.registerListener(this, myAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()

        mySensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nothing to do here
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val acceleration = event.values[1]
            spriteRunner.y += acceleration * 20
            val xyz = "x${event.values[0]}, y${event.values[1]}, z${event.values[2]}"
            xyz_display.text = xyz
        }
    }

    private fun setUpGame() {
        score = 0

        val holder = gameCanvas.holder

        val canvasWidth = 500 // holder.surfaceFrame.width()
        val canvasHeight = 500 // holder.surfaceFrame.height()

        val runnerImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.runner_guy)
        val proRunnerImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.runner_guy_mini)
        val proDogsImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.runner_blonde_mini)

        spriteRunner = Runner(runnerImage, runnerImage.width, runnerImage.height)
        spriteProRunner = ProRunner(proRunnerImage, proRunnerImage.width, proRunnerImage.height)
        spriteProDogs = ProDogs(proDogsImage, proDogsImage.width, proDogsImage.height)

        spriteRunner.setPositionInitial(canvasWidth, canvasHeight)
        spriteRunner.setPositionInitial(canvasWidth, canvasHeight)
        spriteProDogs.setPositionInitial(canvasWidth, canvasHeight)

        initializeObstacleSprites(6, "garbage_bag", canvasWidth, canvasHeight)
        initializeObstacleSprites(6, "gold_coin", canvasWidth, canvasHeight)
        initializeObstacleSprites(6, "doggie_bone", canvasWidth, canvasHeight)
    }

    public fun game_frame(
        gameCanvas: SurfaceView,
        spriteRunner: Runner,
        spritesGarbageBag: List<Obstacle>,
        spritesGoldCoin: List<Obstacle>,
        spritesBone: List<Obstacle>
    ) {
        val holder = gameCanvas.holder
        val canvas = holder.lockCanvas()

        if (canvas != null) {
            // Clear canvas
            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)

            // Render background
            val gameCanvasImage =
                BitmapFactory.decodeResource(resources, R.drawable.game_background)
            //canvas.drawBitmap(gameCanvasImage, 0f, 0f, null)
            val srcRect = Rect(
                0,
                0,
                gameCanvasImage.width,
                gameCanvasImage.height
            ) // Source rectangle covering the entire image
            val dstRect = Rect(
                0,
                0,
                canvas.width,
                canvas.height
            ) // Destination rectangle covering the entire canvas
            canvas.drawBitmap(gameCanvasImage, srcRect, dstRect, null)

            // Render runner sprite
            msg_display.text = "Drawing Runner ${spriteRunner.x},${spriteRunner.y}"
            spriteRunner.draw(canvas)

            // Render garbage bag sprites
            spritesGarbageBag.forEach { spriteGarbageBag ->
                if (spriteGarbageBag.isAtEdge) {
                    spriteGarbageBag.setPositionReset(gameCanvas.width, gameCanvas.height)
                }
                spriteGarbageBag.setPositionAdvanceOneStep()
                spriteGarbageBag.draw(canvas)
            }

            // Render gold coin sprites
            spritesGoldCoin.forEach { spriteGoldCoin ->
                if (spriteGoldCoin.isAtEdge) {
                    spriteGoldCoin.setPositionReset(gameCanvas.width, gameCanvas.height)
                }
                spriteGoldCoin.setPositionAdvanceOneStep()
                spriteGoldCoin.draw(canvas)
            }

            // Render bone sprites
            spritesBone.forEach { spriteBone ->
                if (spriteBone.isAtEdge) {
                    spriteBone.setPositionReset(gameCanvas.width, gameCanvas.height)
                }
                spriteBone.setPositionAdvanceOneStep()
                spriteBone.draw(canvas)
            }

            // Detect collisions and handle events
            detectCollisionsWithRunner()
            score_display.text = score.toString()
        } // end IF canvas
        holder.unlockCanvasAndPost(canvas)
    }

    public fun game_loop( // DEPRECATED
        gameCanvas: SurfaceView,
        spriteRunner: Runner,
        spritesGarbageBag: List<Obstacle>,
        spritesGoldCoin: List<Obstacle>,
        spritesBone: List<Obstacle>
    ) {
        val fps = 60 // Frames per second
        val frameTimeMillis = (1000 / fps).toLong() // Time per frame in milliseconds
        var previousTimeMillis = System.currentTimeMillis()

        while (isGameRunning) {

            val currentTimeMillis = System.currentTimeMillis()
            val elapsedTimeMillis = currentTimeMillis - previousTimeMillis

            if (elapsedTimeMillis >= frameTimeMillis) {
                previousTimeMillis = currentTimeMillis

                game_frame(gameCanvas, spriteRunner, spritesGarbageBag, spritesGoldCoin, spritesBone)
            } // end IF elapsed
        } // end WHILE running
    }

    private fun startGameLoop() {
        if (isGameRunning) {
            // Game loop is already running, no need to start again
            return
        }

        isGameRunning = true

        gameLoopThread = Thread {
            val fps = 5 // Frames per second
            val frameTimeMillis = (1000 / fps).toLong() // Time per frame in milliseconds

            var previousTimeMillis = System.currentTimeMillis()

            while (isGameRunning) {
                val currentTimeMillis = System.currentTimeMillis()
                val elapsedTimeMillis = currentTimeMillis - previousTimeMillis

                if (elapsedTimeMillis >= frameTimeMillis) {
                    previousTimeMillis = currentTimeMillis

                    runOnUiThread {
                        game_frame(gameCanvas, spriteRunner, spritesGarbageBag, spritesGoldCoin, spritesBone)
                    }
                }

                // Add any necessary delay or timing mechanism here
                // Add a delay of 16 milliseconds (approx. 60 FPS)
                try {
                    Thread.sleep(16)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        gameLoopThread?.start()
    }

    private fun stopGameLoop() {
        Log.d("Stop", "Score: $score")
        isGameRunning = false

        // Wait for the game loop thread to finish
        try {
            gameLoopThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun initializeObstacleSprites(n: Int, t: String, wCanvas: Int, hCanvas: Int) {
        val obstacleTypeBitmaps = mapOf(
            "garbage_bag" to BitmapFactory.decodeResource(resources, R.drawable.garbage_bags),
            "gold_coin" to BitmapFactory.decodeResource(resources, R.drawable.gold_coin),
            "doggie_bone" to BitmapFactory.decodeResource(resources, R.drawable.doggie_bone)
        )

        val obstacleTypeArrays = mapOf(
            "garbage_bag" to spritesGarbageBag,
            "gold_coin" to spritesGoldCoin,
            "doggie_bone" to spritesBone
        )

        val im: Bitmap = obstacleTypeBitmaps[t]!!
        val w: Int = obstacleTypeBitmaps[t]!!.width // Set the width of the garbage bag image
        val h: Int = obstacleTypeBitmaps[t]!!.height // Set the height of the garbage bag image

        for (i in 0 until n) {
            val obstacle = Obstacle(im, w, h, t)
            obstacle.setPositionInitial(wCanvas, hCanvas) // Set the initial position
            obstacleTypeArrays[t]!!.add(obstacle)
        }
    }

    private fun detectCollisionsWithRunner() {
        spritesGarbageBag.forEach { sprite ->
            if (isCollision(spriteRunner, sprite)) {
                msg_display.text = "OUCH!!"
                sprite.setPositionReset(gameCanvas.width, gameCanvas.height)
                spriteProRunner.penalize()
            }
        }

        spritesGoldCoin.forEach { sprite ->
            if (isCollision(spriteRunner, sprite)) {
                msg_display.text = "HUZZAH!"
                sprite.setPositionReset(gameCanvas.width, gameCanvas.height)
                score += 25
            }
        }

        spritesBone.forEach { sprite ->
            if (isCollision(spriteRunner, sprite)) {
                msg_display.text = "DOGGONE!"
                sprite.setPositionReset(gameCanvas.width, gameCanvas.height)
                spriteProDogs.penalize()
            }
        }
    }

    private fun isCollision(spriteA: Sprite, spriteB: Sprite): Boolean {
        return (spriteA.x < spriteB.x + spriteB.width &&
                spriteA.x + spriteA.width > spriteB.x &&
                spriteA.y < spriteB.y + spriteB.height &&
                spriteA.y + spriteA.height > spriteB.y)
    }
}

