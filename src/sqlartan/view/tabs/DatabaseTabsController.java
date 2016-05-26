package sqlartan.view.tabs;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import sqlartan.Sqlartan;
import sqlartan.core.Database;
import sqlartan.core.PersistentStructure;
import sqlartan.core.Table;
import sqlartan.core.View;
import sqlartan.view.tabs.structureTab.StructureTab;
import sqlartan.view.util.Popup;
import java.io.IOException;
import static sqlartan.util.Matching.match;
import static sqlartan.view.util.ActionButtons.actionButton;

/**
 * Created by julien on 30.04.16.
 */
public class DatabaseTabsController extends TabsController<DatabaseTabsController.DatabaseStructureTab> {

	@FXML
	private TableColumn<DatabaseStructureTab, Number> colLignes;
	@FXML
	private TableColumn<DatabaseStructureTab, String> colRename;
	@FXML
	private TableColumn<DatabaseStructureTab, String> colDelete;
	@FXML
	private TableView<DatabaseStructureTab> structureTable;
	private Database database;
	private ObservableList<DatabaseStructureTab> dbStructs = FXCollections.observableArrayList();
	@FXML
	protected void initialize() throws IOException {
		super.initialize();

		tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
			if (newTab == structureTab) {
				displayStructure();
			}
		});

		colLignes.setCellValueFactory(param -> param.getValue().lignesProperty());

		colRename.setCellFactory(actionButton("Rename", (self, event) -> {
			DatabaseStructureTab dbStruct = self.getTableView().getItems().get(self.getIndex());
			String structName = dbStruct.nameProperty().get();
			Popup.input("Rename", "Rename " + structName + " into : ", structName).ifPresent(name -> {
				if (name.length() > 0 && !structName.equals(name)) {
					database.structure(structName).ifPresent(s -> Sqlartan.getInstance().getController().renameStructure(s, name));
				}
			});
		}));

		colDelete.setCellFactory(actionButton("Drop", (self, event) -> {
			DatabaseStructureTab dbStruct = self.getTableView().getItems().get(self.getIndex());
			database.structure(dbStruct.nameProperty().get()).ifPresent(s -> Sqlartan.getInstance().getController().dropStructure(s));
		}));

		tabPane.getSelectionModel().clearSelection();
	}
	/**
	 * Display the structure of the database
	 */
	protected void displayStructure() {
		dbStructs.clear();
		dbStructs.addAll(database.structures()
		                         .sorted((a, b) -> a.name().compareTo(b.name()))
		                         .map(DatabaseStructureTab::new)
		                         .toList());
		structureTable.setItems(dbStructs);
	}
	/**
	 * Set the database
	 *
	 * @param database
	 */
	public void setDatabase(Database database) {
		this.database = database;
	}

	/**
	 * Represent the structure tab of a database
	 */
	public class DatabaseStructureTab extends StructureTab {
		private LongProperty lignes;

		private DatabaseStructureTab(PersistentStructure<?> structure) {
			super(structure.name(), match(structure)
				.when(Table.class, t -> "Table")
				.when(View.class, v -> "View")
				.orElse("Unknown"));
			this.lignes = new SimpleLongProperty(structure.selectAll().count());
		}

		private LongProperty lignesProperty() {
			return lignes;
		}
	}
}
