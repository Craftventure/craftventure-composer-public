@file:JvmName("App")

package net.craftventure.composer

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.reactivex.subjects.BehaviorSubject
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.input.KeyCode
import javafx.scene.layout.GridPane
import javafx.stage.FileChooser
import javafx.stage.Stage
import net.craftventure.composer.controller.PlaybackController
import net.craftventure.composer.extension.doubleFormatting
import net.craftventure.composer.fixture.Fixture
import net.craftventure.composer.scene.ShowScene
import net.craftventure.composer.timeline.KeyFrame
import net.craftventure.composer.timeline.KeyFrameEasing
import net.craftventure.composer.view.PreviewView
import tornadofx.selectedItem


class App : Application() {
    private var stages = mutableListOf<Stage>()
    private var stage: Stage? = null

    override fun start(primaryStage: Stage) {
        this.stage = primaryStage
        instance = this
//        initDebugScene()

        primaryStage.minWidth = 1000.0
        primaryStage.minHeight = 600.0

        val loader = FXMLLoader(javaClass.getResource("/app.fxml"))
        val root = loader.load<Parent>()

//        val controller = loader.getController<MainWindowController>()

        val scene = Scene(root, 0.0, 0.0)
        primaryStage.title = "Craftventure Composer"
        primaryStage.scene = scene

        scene.setOnKeyPressed {
            if (it.code == KeyCode.LEFT)
                playbackController.sync(0.0)

            if (it.code == KeyCode.SPACE || it.code == KeyCode.P)
                playbackController.toggle()
        }

        primaryStage.show()
        primaryStage.setOnCloseRequest {
            stages.forEach { it.close() }
        }
    }

    fun open3dView() {
        val stage = Stage()
        stage.title = "Preview"
        stage.scene = PreviewView.newInstance()
        stage.show()
        stages.add(stage)
    }

    fun openKeyFrameEditor(keyFrame: KeyFrame) {
        println("openKeyFrameEditor for ${keyFrame.at} ${keyFrame.keyValue}")

        val dialog = Dialog<Boolean>()
        dialog.title = "KeyFrame editor"
//        dialog.headerText = "Lets edit dis frame bruv"

        val saveButtonType = ButtonType("Save", ButtonData.OK_DONE)
        dialog.dialogPane.buttonTypes.addAll(saveButtonType, ButtonType.CANCEL)

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(20.0, 150.0, 10.0, 10.0)

        val at = TextField()
        at.doubleFormatting()
        at.text = (keyFrame.at / 1000.0).toString()
        val value = TextField()
        value.doubleFormatting()
        value.text = keyFrame.keyValue.toString()

        val easingIn = ComboBox<String>()
        easingIn.selectionModel.select(keyFrame.inEasing.name)
        easingIn.items = FXCollections.observableArrayList(KeyFrameEasing.values().map { it.name })
        val easingOut = ComboBox<String>()
        easingOut.selectionModel.select(keyFrame.outEasing.name)
        easingOut.items = FXCollections.observableArrayList(KeyFrameEasing.values().map { it.name })

        grid.add(Label("Easing (in)"), 0, 0)
        grid.add(easingIn, 1, 0)
        grid.add(Label("Easing (out)"), 0, 1)
        grid.add(easingOut, 1, 1)
        grid.add(Label("At (time in seconds)"), 0, 2)
        grid.add(at, 1, 2)
        grid.add(Label("Value"), 0, 3)
        grid.add(value, 1, 3)

//        val loginButton = dialog.dialogPane.lookupButton(loginButtonType)
//        loginButton.isDisable = true
//
//        username.textProperty().addListener { observable, oldValue, newValue ->
//            loginButton.isDisable = newValue.trim().isEmpty()
//        }

        dialog.dialogPane.content = grid

        dialog.setResultConverter({ dialogButton ->
            if (dialogButton === saveButtonType) {
                keyFrame.inEasing = KeyFrameEasing.valueOf(easingIn.selectedItem!!)
                keyFrame.outEasing = KeyFrameEasing.valueOf(easingOut.selectedItem!!)
                keyFrame.at = (at.text.toDouble() * 1000L).toLong()
                keyFrame.keyValue = value.text.toDouble()
            }
            null
        })

        /*  val result = */
        dialog.showAndWait()

//        result.ifPresent({ usernamePassword ->
//            System.out.println("Username=" + usernamePassword.first + ", Password=" + usernamePassword.second)
//        })
    }

    fun openFixtureEditor(fixture: Fixture) {
        println("openFixtureEditor ${fixture.name}")
    }

    fun openSceneLoadDialog() {
        val fileChooser = FileChooser()
        fileChooser.title = "Open Scene File"
//        fileChooser.initialDirectory = File("C:\\git\\craftventure-composer/show.json").parentFile
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Show JSON files", "*.json"))
        val result = fileChooser.showOpenDialog(stage!!)
        if (result != null) {
            try {
                mainScene.onNext(ShowScene().load(result))
            } catch (e: Exception) {
                e.printStackTrace()
                ExceptionDialog(
                        content = "Opening failed",
                        exception = e
                ).show()
            }
        }
    }

    fun openSceneSaveDialog() {
        val fileChooser = FileChooser()
        fileChooser.title = "Save Scene File"
//        fileChooser.initialDirectory = File("C:\\git\\craftventure-composer/show.json").parentFile
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Show JSON files", "*.json"))
        val result = fileChooser.showSaveDialog(stage!!)
        if (result != null) {
            try {
                App.mainScene.value!!.save(result)
            } catch (e: Exception) {
                e.printStackTrace()
                ExceptionDialog(
                        content = "Saving failed",
                        exception = e
                ).show()
            }
        }
    }

    fun openAudioPicker() {
        val fileChooser = FileChooser()
        fileChooser.title = "Load Audio File"
//        fileChooser.initialDirectory = File("C:\\git\\craftventure-composer/show.json").parentFile
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Audio files", "*.mp3"))
        val result = fileChooser.showOpenDialog(stage!!)
        try {
            playbackController.setTrack(result)
        } catch (e: Exception) {
            e.printStackTrace()
            ExceptionDialog(
                    content = "Failed to set audio",
                    exception = e
            ).show()
        }
    }

    companion object {
        val mainScene = BehaviorSubject.createDefault(ShowScene())
        val playbackController = PlaybackController()
        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()

        lateinit var instance: App
            private set

        @JvmStatic
        fun main(args: Array<String>) {
//            System.out.println("[${args.joinToString(",")}]")
            launch(App::class.java)
        }
    }
}

