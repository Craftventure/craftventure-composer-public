package net.craftventure.composer.controller

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.ListView
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import net.craftventure.composer.App
import net.craftventure.composer.extension.orElse
import net.craftventure.composer.extension.toObservable
import net.craftventure.composer.extension.toOptional
import net.craftventure.composer.fixture.Fixture
import net.craftventure.composer.scene.ShowScene
import net.craftventure.composer.view.KeyframeEditor
import net.craftventure.composer.view.SceneView
import tornadofx.onDoubleClick

class MainWindowController {
    @FXML
    lateinit var mainMenu: MenuBar
    @FXML
    lateinit var topDownPreview: SceneView
    @FXML
    lateinit var keyframeEditor: KeyframeEditor
    @FXML
    lateinit var objectList: ListView<String>

    @FXML
    lateinit var load: MenuItem
    @FXML
    lateinit var save: MenuItem
    @FXML
    lateinit var newPreview: MenuItem
    @FXML
    lateinit var loadAudio: MenuItem

    private var scene: ShowScene? = null

    @FXML
    fun initialize() {
        objectList.items = FXCollections.observableArrayList()
        objectList.selectionModel.selectedItemProperty().addListener({ observable, oldValue, newValue ->
            scene?.let {
                it.selectedFixture.onNext(it.fixtures.firstOrNull { it.name == newValue }.toOptional())
            }
        })
        objectList.onDoubleClick {
            objectList.selectionModel.selectedItem?.let { selectedItem ->
                val fixture = scene?.fixtures?.firstOrNull { it.name == selectedItem }
                if (fixture != null)
                    App.instance.openFixtureEditor(fixture)
            }
        }

        load.setOnAction {
            App.instance.openSceneLoadDialog()
        }

        save.setOnAction {
            App.instance.openSceneSaveDialog()
        }

        loadAudio.setOnAction {
            App.instance.openAudioPicker()
        }

        newPreview.setOnAction {
            App.instance.open3dView()
            it.consume()
        }

        App.mainScene
                .doOnNext {
                    scene = it
                    topDownPreview.updateScene(it)
                }
                .flatMap { it.fixtures.toObservable() }
                .subscribe({
                    updateList(it)
                    topDownPreview.updateFixtures()
                })

        App.mainScene
                .flatMap { it.selectedFixture }
                .subscribe({
                    updateListSelect(it.orElse())
                    topDownPreview.updateSelectedFixture()
                    keyframeEditor.setFixture(it.orElse())
                })
    }

    private fun updateListSelect(fixture: Fixture?) {
        objectList.selectionModel.select(fixture?.name)
    }

    private fun updateList(fixtures: List<Fixture>) {
        val items = FXCollections.observableArrayList<String>()
        items.addAll(fixtures.map { it.name })
        objectList.items = items
    }
}