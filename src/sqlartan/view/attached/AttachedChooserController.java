package sqlartan.view.attached;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sqlartan.view.SqlartanController;
import sqlartan.view.util.Popup;
import java.io.File;

/**
 * Created by julien on 02.05.16.
 */
public class AttachedChooserController {

	public Button ok;
	public Button cancel;
	public Button browse;
	public TextField path;
	public Pane attachedPane;
	public TextField dbName;

	private SqlartanController sqlartanController;

	File file = null;


	@FXML
	private void initialize()
	{
	}

	@FXML
	private void close()
	{
		((Stage)attachedPane.getScene().getWindow()).close();
	}

	public void setSqlartanController(SqlartanController controller)
	{
		sqlartanController = controller;
	}

	@FXML
	private void validate()
	{
		file = new File(path.getText());

		if (!file.getPath().isEmpty() && !dbName.getText().isEmpty()) {
			sqlartanController.attachDatabase(file, dbName.getText());
			close();
		}
		else
		{
			Popup.error("Invalid Entry","The informations for the path and/or the db name are empty");
		}

	}

	@FXML
	private void browse()
	{
		String oldPath = path.getText();
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Selecte database to attached");
		file = fileChooser.showOpenDialog(attachedPane.getScene().getWindow());

		if (file != null) {
			if (dbName.getText().isEmpty() || (!oldPath.isEmpty() && fileName(new File(oldPath)).equals(dbName.getText())))
			{
				dbName.setText(fileName(file));
			}

			path.setText(file.getPath());
		}
	}

	private String fileName(File file)
	{
		return file.getName().split("\\.")[0];
	}
}
